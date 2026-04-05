package com.jvplatformer.entity;

import com.jvplatformer.GameConstants;
import com.jvplatformer.IInputHandler;
import com.jvplatformer.engine.Camera;

import java.awt.Graphics2D;

public class Coin extends Entity {
    private static final int SIZE = 20;
    // Offset so hitbox is centred on the tile
    private static final int TILE_OFFSET = (GameConstants.TILE_SIZE - SIZE) / 2;

    private double bobOffset = 0;
    private double bobTime;            // accumulated seconds, staggered per coin

    public Coin(int tileCol, int tileRow) {
        super(tileCol * GameConstants.TILE_SIZE + TILE_OFFSET,
              tileRow * GameConstants.TILE_SIZE + TILE_OFFSET,
              SIZE, SIZE);
        this.bobTime = (tileCol * 137 + tileRow * 29) % 628 / 100.0; // staggered start phase
    }

    // sin LUT shared with Player — 360 entries, indexed by integer degree
    private static final float[] SIN_LUT = new float[360];
    static {
        for (int i = 0; i < 360; i++) SIN_LUT[i] = (float) Math.sin(Math.toRadians(i));
    }

    @Override
    public void update(double dt, IInputHandler input) {
        bobTime += dt * 90.0;  // ~0.25 Hz bob (90 degrees/second)
        int idx = ((int) bobTime) % 360;
        if (idx < 0) idx += 360;
        bobOffset = SIN_LUT[idx] * 4.0;
    }

    @Override
    public void render(Graphics2D g, Camera camera) {
        if (!active) return;
        int drawX = (int) x - camera.getOffsetX();
        int drawY = (int) (y + bobOffset) - camera.getOffsetY();

        if (drawX + width < 0 || drawX > GameConstants.SCREEN_WIDTH) return;

        // Coin body — rects are ~10x faster than ovals in software rendering
        g.setColor(GameConstants.COLOR_COIN_RING);
        g.fillRect(drawX - 2, drawY - 2, SIZE + 4, SIZE + 4);
        g.setColor(GameConstants.COLOR_COIN);
        g.fillRect(drawX, drawY, SIZE, SIZE);
        g.setColor(GameConstants.COLOR_COIN_SHINE);
        g.fillRect(drawX + 4, drawY + 3, 6, 6);
    }
}
