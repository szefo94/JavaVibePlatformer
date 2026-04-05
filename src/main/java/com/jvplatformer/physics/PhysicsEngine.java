package com.jvplatformer.physics;

import com.jvplatformer.GameConstants;
import com.jvplatformer.entity.Entity;
import com.jvplatformer.entity.Platform;

import java.util.List;

public class PhysicsEngine {

    public void update(List<Entity> dynamics, List<Platform> platforms, double dt) {
        for (int i = 0, n = dynamics.size(); i < n; i++) {
            Entity e = dynamics.get(i);
            if (!e.active) continue;
            integrate(e, dt);
            resolveCollisions(e, platforms);
        }
    }

    private void integrate(Entity e, double dt) {
        // Apply gravity
        e.velY += GameConstants.GRAVITY * dt;
        e.velY = Math.min(e.velY, GameConstants.MAX_FALL_SPEED);

        e.x += e.velX * dt;
        e.y += e.velY * dt;

        e.onGround = false;
    }

    private void resolveCollisions(Entity e, List<Platform> platforms) {
        int n = platforms.size();

        // --- Y-axis pass (float-based to avoid int-truncation floor-miss bug) ---
        for (int i = 0; i < n; i++) {
            Platform p = platforms.get(i);
            // Skip if no X overlap
            if (e.x + e.width <= p.x || e.x >= p.x + p.width) continue;
            // Skip if no Y overlap
            if (e.y + e.height <= p.y || e.y >= p.y + p.height) continue;

            if (e.velY > 0) {
                // Falling: land on top
                e.y = p.y - e.height;
                e.velY = 0;
                e.onGround = true;
            } else if (e.velY < 0) {
                // Rising: hit ceiling
                e.y = p.y + p.height;
                e.velY = 0;
            }
        }

        // --- X-axis pass (float-based) ---
        for (int i = 0; i < n; i++) {
            Platform p = platforms.get(i);
            // Skip if no Y overlap
            if (e.y + e.height <= p.y || e.y >= p.y + p.height) continue;
            // Skip if no X overlap
            if (e.x + e.width <= p.x || e.x >= p.x + p.width) continue;

            if (e.velX > 0) {
                e.x = p.x - e.width;
            } else if (e.velX < 0) {
                e.x = p.x + p.width;
            }
            e.velX = 0;
        }
    }
}
