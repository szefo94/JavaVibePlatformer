package com.jvplatformer;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class InputHandler extends KeyAdapter implements IInputHandler {
    private final boolean[] held        = new boolean[256];
    private final boolean[] justPressed = new boolean[256];

    // Counts key-down events per second for PerfLogger
    private int keyEventCount = 0;

    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        if (code < held.length) {
            if (!held[code]) {
                justPressed[code] = true;
                keyEventCount++;
            }
            held[code] = true;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int code = e.getKeyCode();
        if (code < held.length) {
            held[code] = false;
        }
    }

    public boolean isHeld(int keyCode) {
        return keyCode < held.length && held[keyCode];
    }

    /** Returns true once per press; consuming the event. */
    public boolean isJustPressed(int keyCode) {
        if (keyCode < justPressed.length && justPressed[keyCode]) {
            justPressed[keyCode] = false;
            return true;
        }
        return false;
    }

    /** Call once per update tick to clear consumed just-pressed states. */
    public void clearJustPressed() {
        java.util.Arrays.fill(justPressed, false);
    }

    /** Returns presses-per-second count and resets it. Call once per second. */
    public int drainKeyEventCount() {
        int c = keyEventCount;
        keyEventCount = 0;
        return c;
    }

    /** Snapshot of currently held game keys as a short string, e.g. "R,SPC". */
    public String heldSummary() {
        StringBuilder sb = new StringBuilder();
        if (held[KeyEvent.VK_LEFT]  || held[KeyEvent.VK_A])     append(sb, "L");
        if (held[KeyEvent.VK_RIGHT] || held[KeyEvent.VK_D])     append(sb, "R");
        if (held[KeyEvent.VK_SPACE] || held[KeyEvent.VK_UP]
                                     || held[KeyEvent.VK_W])    append(sb, "JMP");
        return sb.length() == 0 ? "none" : sb.toString();
    }

    private static void append(StringBuilder sb, String key) {
        if (sb.length() > 0) sb.append(',');
        sb.append(key);
    }
}
