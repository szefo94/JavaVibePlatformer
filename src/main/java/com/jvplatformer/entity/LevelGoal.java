package com.jvplatformer.entity;

import com.jvplatformer.GameConstants;
import com.jvplatformer.IInputHandler;
import com.jvplatformer.engine.Camera;

import java.awt.Graphics2D;

public class LevelGoal extends Entity {

    // Two tiles tall so it's clearly visible
    public LevelGoal(int tileCol, int tileRow) {
        super(tileCol * GameConstants.TILE_SIZE,
              (tileRow - 1) * GameConstants.TILE_SIZE,   // one tile above its tile row
              GameConstants.TILE_SIZE,
              GameConstants.TILE_SIZE * 2);
    }

    @Override
    public void update(double dt, IInputHandler input) {}

    @Override
    public void render(Graphics2D g, Camera camera) {
        int drawX = (int) x - camera.getOffsetX();
        int drawY = (int) y - camera.getOffsetY();

        if (drawX + width < 0 || drawX > GameConstants.SCREEN_WIDTH) return;

        // Door frame
        g.setColor(GameConstants.COLOR_GOAL_FRAME);
        g.fillRect(drawX, drawY, width, height);

        // Door interior
        g.setColor(GameConstants.COLOR_GOAL_INNER);
        g.fillRect(drawX + 4, drawY + 4, width - 8, height - 8);

        // Star / arrow symbol in centre
        g.setColor(GameConstants.COLOR_GOAL_SYMBOL);
        int cx = drawX + width / 2;
        int cy = drawY + height / 2;
        // Simple arrow pointing right
        int[] xs = { cx - 10, cx + 14, cx - 10 };
        int[] ys = { cy - 10, cy,      cy + 10  };
        g.fillPolygon(xs, ys, 3);
        g.fillRect(cx - 14, cy - 4, 18, 8);
    }
}
