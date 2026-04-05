package com.jvplatformer.engine;

import com.jvplatformer.GameConstants;
import com.jvplatformer.entity.Player;

public class Camera {
    private double x;
    private double y;

    public void update(Player player, int levelWidthPx, int levelHeightPx) {
        double targetX = player.x - GameConstants.SCREEN_WIDTH / GameConstants.CAMERA_LEAD_FACTOR;
        targetX = Math.max(0, Math.min(targetX, levelWidthPx - GameConstants.SCREEN_WIDTH));
        x += (targetX - x) * GameConstants.CAMERA_LERP;

        double targetY = player.y - GameConstants.SCREEN_HEIGHT / 2.0;
        targetY = Math.max(0, Math.min(targetY, levelHeightPx - GameConstants.SCREEN_HEIGHT));
        y += (targetY - y) * GameConstants.CAMERA_LERP;
    }

    public int getOffsetX() { return (int) x; }
    public int getOffsetY() { return (int) y; }

    public void reset() {
        x = 0;
        y = 0;
    }
}
