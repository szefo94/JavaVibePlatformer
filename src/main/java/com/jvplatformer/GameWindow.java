package com.jvplatformer;

import javax.swing.JFrame;
import java.awt.Dimension;

public class GameWindow {
    private final JFrame frame;
    private final GamePanel gamePanel;

    public GameWindow() {
        frame = new JFrame("JavaVibePlatformer");
        gamePanel = new GamePanel();

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.add(gamePanel);

        Dimension size = new Dimension(GameConstants.SCREEN_WIDTH, GameConstants.SCREEN_HEIGHT);
        gamePanel.setPreferredSize(size);

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        gamePanel.requestFocusInWindow();
    }

    public void start() {
        gamePanel.startGameLoop();
    }
}
