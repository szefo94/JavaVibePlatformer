package com.jvplatformer.fb;

import com.jvplatformer.GameConstants;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Writes game frames directly to /dev/fb0, bypassing X11 entirely.
 *
 * Flow each frame:
 *   1. Game draws to a BufferedImage via Graphics2D (same renderer code unchanged)
 *   2. flip() reads pixel data, converts RGB888 -> RGB565, writes to memory-mapped fb0
 *
 * RGB565 = 16bpp: 5 bits red | 6 bits green | 5 bits blue (little-endian)
 * Requires user to be in the 'video' group.
 * Must be run from a TTY (Ctrl+Alt+F2), not from inside X11/Wayland.
 */
public class FrameBuffer implements AutoCloseable {

    private static final String FB_PATH     = "/dev/fb0";
    private static final String SYSFS_SIZE  = "/sys/class/graphics/fb0/virtual_size";
    private static final String SYSFS_STRIDE = "/sys/class/graphics/fb0/stride";

    private final int fbWidth;
    private final int fbHeight;
    private final int stride;          // bytes per row in framebuffer
    private final int startX;          // pixel offset: center game horizontally
    private final int startY;          // pixel offset: center game vertically

    private final BufferedImage      canvas;
    private final int[]              pixels;   // ARGB pixels from canvas
    private final byte[]             row565;   // one row converted to RGB565

    private final RandomAccessFile   fbFile;
    private final MappedByteBuffer   fbMap;

    public FrameBuffer() throws IOException {
        // Read screen geometry from sysfs
        String[] dims = Files.readString(Paths.get(SYSFS_SIZE)).trim().split(",");
        fbWidth  = Integer.parseInt(dims[0]);
        fbHeight = Integer.parseInt(dims[1]);
        stride   = Integer.parseInt(Files.readString(Paths.get(SYSFS_STRIDE)).trim());

        // Center the game viewport on the physical screen
        startX = (fbWidth  - GameConstants.SCREEN_WIDTH)  / 2;
        startY = (fbHeight - GameConstants.SCREEN_HEIGHT) / 2;

        // Offscreen canvas — game renderer draws here
        canvas = new BufferedImage(GameConstants.SCREEN_WIDTH, GameConstants.SCREEN_HEIGHT,
                                   BufferedImage.TYPE_INT_RGB);
        pixels = new int[GameConstants.SCREEN_WIDTH * GameConstants.SCREEN_HEIGHT];
        row565 = new byte[GameConstants.SCREEN_WIDTH * 2];

        // Memory-map the entire framebuffer — writes go directly to VRAM, no syscalls
        fbFile = new RandomAccessFile(FB_PATH, "rw");
        fbMap  = fbFile.getChannel().map(FileChannel.MapMode.READ_WRITE,
                                         0, (long) stride * fbHeight);

        clearScreen();
        System.out.println("[FB] " + fbWidth + "x" + fbHeight + " stride=" + stride
                + " bpp=16  game at (" + startX + "," + startY + ")");
    }

    /** Returns a Graphics2D for the offscreen canvas. Caller must dispose() it. */
    public Graphics2D createGraphics() {
        Graphics2D g = canvas.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,    RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,       RenderingHints.VALUE_RENDER_SPEED);
        g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_SPEED);
        return g;
    }

    /**
     * Convert canvas pixels to RGB565 and write to the mapped framebuffer.
     * Called once per frame.
     */
    public void flip() {
        canvas.getRGB(0, 0, GameConstants.SCREEN_WIDTH, GameConstants.SCREEN_HEIGHT,
                      pixels, 0, GameConstants.SCREEN_WIDTH);

        for (int row = 0; row < GameConstants.SCREEN_HEIGHT; row++) {
            // Convert this row to RGB565
            int base = row * GameConstants.SCREEN_WIDTH;
            for (int col = 0; col < GameConstants.SCREEN_WIDTH; col++) {
                int rgb = pixels[base + col];
                int r   = (rgb >> 16) & 0xFF;
                int g   = (rgb >>  8) & 0xFF;
                int b   =  rgb        & 0xFF;
                // Pack to RGB565 little-endian
                int c565 = ((r >> 3) << 11) | ((g >> 2) << 5) | (b >> 3);
                row565[col * 2]     = (byte) ( c565       & 0xFF);
                row565[col * 2 + 1] = (byte) ((c565 >> 8) & 0xFF);
            }

            // Write row directly into mapped VRAM
            int fbPos = (startY + row) * stride + startX * 2;
            fbMap.position(fbPos);
            fbMap.put(row565, 0, row565.length);
        }
    }

    /** Blank the entire screen black. */
    private void clearScreen() {
        byte[] black = new byte[stride];
        for (int row = 0; row < fbHeight; row++) {
            fbMap.position(row * stride);
            fbMap.put(black);
        }
    }

    @Override
    public void close() {
        clearScreen();
        try { fbFile.close(); } catch (IOException ignored) {}
    }
}
