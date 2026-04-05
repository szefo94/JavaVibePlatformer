package com.jvplatformer.entity;

import com.jvplatformer.GameConstants;
import com.jvplatformer.IInputHandler;
import com.jvplatformer.engine.Camera;

import java.awt.Graphics2D;

public class Platform extends Entity {

    public Platform(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    @Override
    public void update(double dt, IInputHandler input) {
        // Static - no update needed
    }

    @Override
    public void render(Graphics2D g, Camera camera) {
        int drawX = (int) x - camera.getOffsetX();
        int drawY = (int) y - camera.getOffsetY();

        // Only draw if on screen
        if (drawX + width < 0 || drawX > GameConstants.SCREEN_WIDTH) return;
        if (drawY + height < 0 || drawY > GameConstants.SCREEN_HEIGHT) return;

        g.setColor(GameConstants.COLOR_PLATFORM);
        g.fillRect(drawX, drawY, width, height);
        // Top-edge highlight: 1 fillRect instead of 4-line drawRect
        g.setColor(GameConstants.COLOR_PLATFORM_B);
        g.fillRect(drawX, drawY, width, 3);
    }
}
