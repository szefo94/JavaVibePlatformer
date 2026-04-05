package com.jvplatformer.fb;

import com.jvplatformer.IInputHandler;

import java.awt.event.KeyEvent;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * Reads keyboard events directly from Linux /dev/input/eventX devices.
 * No X11, no AWT — pure kernel input.
 *
 * Linux input_event struct (64-bit ARM, 24 bytes):
 *   u64 sec    (8 bytes)
 *   u64 usec   (8 bytes)
 *   u16 type   (2 bytes)   EV_KEY = 1
 *   u16 code   (2 bytes)   evdev key code
 *   s32 value  (4 bytes)   1=down, 0=up, 2=repeat
 *
 * Key codes are translated to Java KeyEvent.VK_* so the rest of the game
 * (Player.java, GamePanel etc.) works completely unchanged.
 *
 * Requires user to be in the 'input' group.
 */
public class EvdevInput implements IInputHandler {

    private static final int EVENT_SIZE = 24;   // 64-bit Linux input_event
    private static final int EV_KEY     = 1;
    private static final int KEY_DOWN   = 1;
    private static final int KEY_UP     = 0;

    // Translate evdev key codes -> Java KeyEvent VK codes
    // Only the keys the game actually uses
    private static final int[] EVDEV_TO_VK = new int[256];
    static {
        EVDEV_TO_VK[1]   = KeyEvent.VK_ESCAPE;  // KEY_ESC
        EVDEV_TO_VK[17]  = KeyEvent.VK_W;        // KEY_W
        EVDEV_TO_VK[30]  = KeyEvent.VK_A;        // KEY_A
        EVDEV_TO_VK[32]  = KeyEvent.VK_D;        // KEY_D
        EVDEV_TO_VK[28]  = KeyEvent.VK_ENTER;    // KEY_ENTER
        EVDEV_TO_VK[57]  = KeyEvent.VK_SPACE;    // KEY_SPACE
        EVDEV_TO_VK[61]  = KeyEvent.VK_F3;       // KEY_F3
        EVDEV_TO_VK[103] = KeyEvent.VK_UP;       // KEY_UP
        EVDEV_TO_VK[105] = KeyEvent.VK_LEFT;     // KEY_LEFT
        EVDEV_TO_VK[106] = KeyEvent.VK_RIGHT;    // KEY_RIGHT
        EVDEV_TO_VK[108] = KeyEvent.VK_DOWN;     // KEY_DOWN
    }

    // Indexed by Java VK code — same interface as InputHandler
    private final boolean[] held        = new boolean[256];
    private final boolean[] justPressed = new boolean[256];
    private int keyEventCount = 0;

    private volatile boolean running = true;

    public EvdevInput(String... devicePaths) {
        for (String path : devicePaths) {
            Thread t = new Thread(() -> readLoop(path), "evdev:" + path);
            t.setDaemon(true);
            t.setPriority(Thread.MAX_PRIORITY - 1);
            t.start();
        }
    }

    private void readLoop(String path) {
        try (FileInputStream fis = new FileInputStream(path)) {
            byte[] buf = new byte[EVENT_SIZE];
            ByteBuffer bb = ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN);

            while (running) {
                int read = 0;
                while (read < EVENT_SIZE) {
                    int n = fis.read(buf, read, EVENT_SIZE - read);
                    if (n < 0) return;
                    read += n;
                }

                bb.position(16);            // skip 16-byte timeval
                int type  = bb.getShort() & 0xFFFF;
                int evCode = bb.getShort() & 0xFFFF;
                int value = bb.getInt();

                if (type == EV_KEY && evCode < EVDEV_TO_VK.length) {
                    int vk = EVDEV_TO_VK[evCode];
                    if (vk != 0 && vk < held.length) {
                        synchronized (this) {
                            if (value == KEY_DOWN) {
                                if (!held[vk]) {
                                    justPressed[vk] = true;
                                    keyEventCount++;
                                }
                                held[vk] = true;
                            } else if (value == KEY_UP) {
                                held[vk] = false;
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("[Evdev] Error on " + path + ": " + e.getMessage());
        }
    }

    @Override
    public synchronized boolean isHeld(int vk) {
        return vk < held.length && held[vk];
    }

    @Override
    public synchronized boolean isJustPressed(int vk) {
        if (vk < justPressed.length && justPressed[vk]) {
            justPressed[vk] = false;
            return true;
        }
        return false;
    }

    @Override
    public synchronized void clearJustPressed() {
        Arrays.fill(justPressed, false);
    }

    @Override
    public synchronized int drainKeyEventCount() {
        int c = keyEventCount;
        keyEventCount = 0;
        return c;
    }

    @Override
    public synchronized String heldSummary() {
        StringBuilder sb = new StringBuilder();
        if (held[KeyEvent.VK_LEFT]  || held[KeyEvent.VK_A])   app(sb, "L");
        if (held[KeyEvent.VK_RIGHT] || held[KeyEvent.VK_D])   app(sb, "R");
        if (held[KeyEvent.VK_SPACE] || held[KeyEvent.VK_UP]
                                     || held[KeyEvent.VK_W])  app(sb, "JMP");
        return sb.length() == 0 ? "none" : sb.toString();
    }

    public void stop() { running = false; }

    private static void app(StringBuilder sb, String s) {
        if (sb.length() > 0) sb.append(',');
        sb.append(s);
    }
}
