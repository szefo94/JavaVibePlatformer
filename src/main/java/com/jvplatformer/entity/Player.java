package com.jvplatformer.entity;

import com.jvplatformer.GameConstants;
import com.jvplatformer.IInputHandler;
import com.jvplatformer.engine.Camera;

import java.awt.Graphics2D;
import java.awt.event.KeyEvent;

public class Player extends Entity {
    private int    jumpsLeft = 0;
    private double angle     = 0.0;

    // Sin/cos lookup table — 360 entries, avoids Math.sin/cos every frame on ARM
    private static final int    LUT_SIZE = 360;
    private static final float[] SIN_LUT = new float[LUT_SIZE];
    private static final float[] COS_LUT = new float[LUT_SIZE];
    static {
        for (int i = 0; i < LUT_SIZE; i++) {
            double a = Math.toRadians(i);
            SIN_LUT[i] = (float) Math.sin(a);
            COS_LUT[i] = (float) Math.cos(a);
        }
    }

    private static float lutSin(double radians) {
        int i = ((int) Math.toDegrees(radians) % LUT_SIZE + LUT_SIZE) % LUT_SIZE;
        return SIN_LUT[i];
    }
    private static float lutCos(double radians) {
        int i = ((int) Math.toDegrees(radians) % LUT_SIZE + LUT_SIZE) % LUT_SIZE;
        return COS_LUT[i];
    }

    public Player(double x, double y) {
        super(x, y, GameConstants.PLAYER_WIDTH, GameConstants.PLAYER_HEIGHT);
    }

    @Override
    public void update(double dt, IInputHandler input) {
        boolean left  = input.isHeld(KeyEvent.VK_LEFT)  || input.isHeld(KeyEvent.VK_A);
        boolean right = input.isHeld(KeyEvent.VK_RIGHT) || input.isHeld(KeyEvent.VK_D);
        boolean jump  = input.isJustPressed(KeyEvent.VK_SPACE)
                     || input.isJustPressed(KeyEvent.VK_UP)
                     || input.isJustPressed(KeyEvent.VK_W);

        // Horizontal movement
        double friction = onGround ? GameConstants.GROUND_FRICTION : GameConstants.AIR_FRICTION;

        if (right) velX += GameConstants.MOVE_FORCE;
        if (left)  velX -= GameConstants.MOVE_FORCE;

        velX *= friction;
        velX = Math.max(-GameConstants.MAX_WALK_SPEED, Math.min(GameConstants.MAX_WALK_SPEED, velX));

        // Snap tiny velocities to zero to prevent drift
        if (Math.abs(velX) < 0.5) velX = 0;

        // Rotation: rolling from X, partial spin from Y when airborne
        double radius = width / 2.0;
        angle += (velX / radius) * dt;
        if (!onGround) angle += (velY / radius) * dt * 0.4;

        // Refill jumps on landing
        if (onGround) jumpsLeft = 2;

        // Jump (double jump: up to 2 times before landing)
        if (jump && jumpsLeft > 0) {
            velY = GameConstants.JUMP_VELOCITY;
            onGround = false;
            jumpsLeft--;
        }
    }

    @Override
    public void render(Graphics2D g, Camera camera) {
        int drawX = (int) x - camera.getOffsetX();
        int drawY = (int) y - camera.getOffsetY();

        // Ball body
        g.setColor(GameConstants.COLOR_PLAYER);
        g.fillOval(drawX, drawY, width, height);

        // Seam lines — two perpendicular diameters rotated by current angle
        int cx = drawX + width / 2;
        int cy = drawY + height / 2;
        int r  = width / 2 - 2;

        float cos0 = lutCos(angle);
        float sin0 = lutSin(angle);
        float cos1 = lutCos(angle + Math.PI / 2);
        float sin1 = lutSin(angle + Math.PI / 2);

        g.setColor(GameConstants.COLOR_PLAYER_EYE);
        // Seam 1
        g.drawLine(cx - (int)(cos0 * r), cy - (int)(sin0 * r),
                   cx + (int)(cos0 * r), cy + (int)(sin0 * r));
        // Seam 2 (perpendicular)
        g.drawLine(cx - (int)(cos1 * r), cy - (int)(sin1 * r),
                   cx + (int)(cos1 * r), cy + (int)(sin1 * r));

        // Surface dot — makes rotation direction obvious
        g.setColor(GameConstants.COLOR_PLAYER_EYE);
        g.fillRect(cx + (int)(cos0 * (r - 3)) - 3, cy + (int)(sin0 * (r - 3)) - 3, 6, 6);
    }

    public void respawn(double spawnX, double spawnY) {
        x = spawnX;
        y = spawnY;
        velX  = 0;
        velY  = 0;
        angle = 0;
        onGround  = false;
        jumpsLeft = 0;
    }
}
