package com.jvplatformer.entity;

import com.jvplatformer.IInputHandler;
import com.jvplatformer.engine.Camera;

import java.awt.Graphics2D;
import java.awt.Rectangle;

public abstract class Entity {
    public double x, y;
    public double velX, velY;
    public int width, height;
    public boolean onGround;
    public boolean active = true;

    // Cached bounds — updated in-place, never reallocated
    private final Rectangle bounds = new Rectangle();

    public Entity(double x, double y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        bounds.setBounds((int) x, (int) y, width, height);
    }

    public abstract void update(double dt, IInputHandler input);
    public abstract void render(Graphics2D g, Camera camera);

    public Rectangle getBounds() {
        bounds.x = (int) x;
        bounds.y = (int) y;
        bounds.width  = width;
        bounds.height = height;
        return bounds;
    }
}
