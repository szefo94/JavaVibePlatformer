package com.jvplatformer;

import com.jvplatformer.entity.Entity;
import com.jvplatformer.entity.Platform;
import com.jvplatformer.engine.Camera;
import com.jvplatformer.physics.PhysicsEngine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.Graphics2D;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PhysicsEngineTest {

    // Minimal concrete Entity for tests — no rendering needed
    static class Box extends Entity {
        Box(double x, double y) {
            super(x, y, 32, 32);
        }
        @Override public void update(double dt, IInputHandler input) {}
        @Override public void render(Graphics2D g, Camera camera) {}
    }

    private PhysicsEngine physics;

    @BeforeEach
    void setUp() {
        physics = new PhysicsEngine();
    }

    // ── Gravity ──────────────────────────────────────────────────────────────

    @Test
    void gravity_acceleratesEntityDownward() {
        Box box = new Box(0, 0);
        double initialVelY = box.velY;
        physics.update(List.of(box), List.of(), 1.0 / 60);
        assertTrue(box.velY > initialVelY, "velY should increase (downward) due to gravity");
    }

    @Test
    void gravity_doesNotExceedMaxFallSpeed() {
        Box box = new Box(0, 0);
        // Simulate a long fall
        for (int i = 0; i < 500; i++) {
            physics.update(List.of(box), List.of(), 1.0 / 60);
        }
        assertTrue(box.velY <= GameConstants.MAX_FALL_SPEED,
                "velY must not exceed MAX_FALL_SPEED");
    }

    // ── Landing ──────────────────────────────────────────────────────────────

    @Test
    void entity_landsOnTopOfPlatform() {
        // Box falling onto a platform at y=200
        Box box = new Box(0, 100);
        box.velY = 200;
        Platform floor = new Platform(0, 200, 200, 48);

        // Simulate until landed
        for (int i = 0; i < 100; i++) {
            physics.update(List.of(box), List.of(floor), 1.0 / 60);
            if (box.onGround) break;
        }

        assertTrue(box.onGround, "Entity should land on platform");
        assertEquals((double)(floor.y - box.height), box.y, 1.0,
                "Entity bottom should align with platform top");
        assertEquals(0.0, box.velY, "velY should be zeroed on landing");
    }

    @Test
    void entity_stopsOnGround_notSinking() {
        Box box = new Box(50, 0);
        Platform floor = new Platform(0, 300, 200, 48);

        for (int i = 0; i < 200; i++) {
            physics.update(List.of(box), List.of(floor), 1.0 / 60);
        }

        assertEquals((double)(floor.y - box.height), box.y, 1.0,
                "Entity must not sink through platform over time");
    }

    @Test
    void entity_onGroundFlag_falseAfterWalkingOffEdge() {
        Box box = new Box(0, 268); // sitting on platform top
        box.velX = 200;
        Platform floor = new Platform(0, 300, 50, 48); // narrow platform

        // Walk off
        for (int i = 0; i < 20; i++) {
            physics.update(List.of(box), List.of(floor), 1.0 / 60);
        }

        assertFalse(box.onGround, "onGround must be false once entity walks off the edge");
    }

    // ── Ceiling collision ─────────────────────────────────────────────────────

    @Test
    void entity_bouncesOffCeiling() {
        Box box = new Box(50, 200);
        box.velY = -400; // moving up
        Platform ceiling = new Platform(0, 100, 200, 48);

        for (int i = 0; i < 30; i++) {
            physics.update(List.of(box), List.of(ceiling), 1.0 / 60);
            if (box.velY >= 0) break;
        }

        assertTrue(box.velY >= 0, "velY must be >= 0 after hitting ceiling");
        assertTrue(box.y >= ceiling.y + ceiling.height - 1,
                "Entity must not pass through ceiling");
    }

    // ── Horizontal wall collision ─────────────────────────────────────────────

    @Test
    void entity_stopsAtRightWall() {
        Box box = new Box(0, 68); // sitting on floor (bottom=100)
        box.velX = 300;
        // Floor MUST be first — Y-pass processes in order; floor resolves landing before
        // the wall's Y range can be misinterpreted as a ceiling.
        Platform floor = new Platform(0,   100, 500, 48);
        Platform wall  = new Platform(200, 0,   48, 200);

        for (int i = 0; i < 60; i++) {
            physics.update(List.of(box), List.of(floor, wall), 1.0 / 60);
        }

        assertTrue(box.x <= wall.x, "Entity must not pass through wall on right");
    }

    @Test
    void entity_stopsAtLeftWall() {
        Box box = new Box(300, 68); // sitting on floor (bottom=100)
        box.velX = -300;
        Platform floor = new Platform(0,   100, 500, 48);
        Platform wall  = new Platform(100, 0,   48, 200);

        for (int i = 0; i < 60; i++) {
            physics.update(List.of(box), List.of(floor, wall), 1.0 / 60);
        }

        assertTrue(box.x >= wall.x + wall.width, "Entity must not pass through wall on left");
    }

    // ── Inactive entity ───────────────────────────────────────────────────────

    @Test
    void inactiveEntity_isNotUpdated() {
        Box box = new Box(0, 0);
        box.active = false;
        double startY = box.y;
        physics.update(List.of(box), List.of(), 1.0 / 60);
        assertEquals(startY, box.y, "Inactive entity must not be moved by physics");
    }
}
