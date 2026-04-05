package com.jvplatformer;

import com.jvplatformer.fb.FbGameLoop;

import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) throws Exception {
        boolean fbMode = args.length > 0 && args[0].equals("--fb");

        if (fbMode) {
            // Framebuffer mode: no X11, direct /dev/fb0 + /dev/input
            // Must be run from a TTY (Ctrl+Alt+F2), not inside desktop
            new FbGameLoop().start();
        } else {
            // AWT/Swing mode: normal desktop window via X11
            SwingUtilities.invokeLater(() -> {
                GameWindow window = new GameWindow();
                window.start();
            });
        }
    }
}
