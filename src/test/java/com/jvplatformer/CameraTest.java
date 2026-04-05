package com.jvplatformer;

import com.jvplatformer.engine.Camera;
import com.jvplatformer.entity.Platform;
import com.jvplatformer.entity.Player;
import com.jvplatformer.physics.PhysicsEngine;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CameraTest {

    private static final int W = GameConstants.SCREEN_WIDTH;
    private static final int H = GameConstants.SCREEN_HEIGHT;

    @Test
    void camera_startsAtZero() {
        Camera cam = new Camera();
        assertEquals(0, cam.getOffsetX());
        assertEquals(0, cam.getOffsetY());
    }

    @Test
    void camera_reset_returnsToZero() {
        Camera cam = new Camera();
        Player p = new Player(5000, 5000);
        cam.update(p, 10000, 10000);
        cam.reset();
        assertEquals(0, cam.getOffsetX());
        assertEquals(0, cam.getOffsetY());
    }

    @Test
    void camera_doesNotScrollNegative() {
        Camera cam = new Camera();
        Player p = new Player(0, 0); // at origin
        // Converge camera over many ticks
        for (int i = 0; i < 300; i++) cam.update(p, 10000, 10000);
        assertTrue(cam.getOffsetX() >= 0, "Camera X must not go negative");
        assertTrue(cam.getOffsetY() >= 0, "Camera Y must not go negative");
    }

    @Test
    void camera_doesNotScrollBeyondLevelWidth() {
        Camera cam = new Camera();
        int levelW = W + 100; // level just slightly wider than screen
        Player p = new Player(levelW, 0); // player at far right
        for (int i = 0; i < 300; i++) cam.update(p, levelW, 10000);
        assertTrue(cam.getOffsetX() <= levelW - W,
                "Camera must not scroll past the right edge of the level");
    }

    @Test
    void camera_followsPlayerHorizontally() {
        Camera cam = new Camera();
        Player p = new Player(2000, 100);
        for (int i = 0; i < 300; i++) cam.update(p, 10000, 10000);
        // Camera should have scrolled significantly rightward
        assertTrue(cam.getOffsetX() > 500, "Camera should follow player to the right");
    }

    @Test
    void camera_followsPlayerVertically() {
        Camera cam = new Camera();
        Player p = new Player(100, 3000);
        for (int i = 0; i < 300; i++) cam.update(p, 10000, 10000);
        assertTrue(cam.getOffsetY() > 500, "Camera should follow player downward");
    }

    @Test
    void camera_lerps_notInstant() {
        Camera cam = new Camera();
        Player p = new Player(5000, 5000);
        cam.update(p, 20000, 20000); // single tick
        // After one tick with LERP=0.18, camera should have moved but not fully arrived
        int expectedTarget = (int)(p.x - W / GameConstants.CAMERA_LEAD_FACTOR);
        assertTrue(cam.getOffsetX() < expectedTarget,
                "Camera should LERP toward target, not teleport");
    }
}
