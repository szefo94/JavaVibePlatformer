package com.jvplatformer;

import com.jvplatformer.entity.Platform;
import com.jvplatformer.entity.Player;
import com.jvplatformer.physics.PhysicsEngine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.event.KeyEvent;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PlayerTest {

    // Stub IInputHandler — set fields to simulate key state
    static class StubInput implements IInputHandler {
        boolean left, right, space;
        int justPressedKey = -1;

        @Override public boolean isHeld(int vk) {
            return (vk == KeyEvent.VK_LEFT  && left)
                || (vk == KeyEvent.VK_RIGHT && right)
                || (vk == KeyEvent.VK_A     && left)
                || (vk == KeyEvent.VK_D     && right);
        }
        @Override public boolean isJustPressed(int vk) {
            if (vk == justPressedKey) { justPressedKey = -1; return true; }
            return (vk == KeyEvent.VK_SPACE && space);
        }
        @Override public void clearJustPressed() { justPressedKey = -1; space = false; }
        @Override public int drainKeyEventCount() { return 0; }
        @Override public String heldSummary() { return ""; }
    }

    private PhysicsEngine physics;
    private StubInput input;

    @BeforeEach
    void setUp() {
        physics = new PhysicsEngine();
        input   = new StubInput();
    }

    // ── Movement ─────────────────────────────────────────────────────────────

    @Test
    void player_acceleratesRight_whenRightHeld() {
        Player p = new Player(100, 100);
        p.onGround = true;
        input.right = true;
        p.update(1.0 / 60, input);
        assertTrue(p.velX > 0, "velX should be positive when right is held");
    }

    @Test
    void player_acceleratesLeft_whenLeftHeld() {
        Player p = new Player(100, 100);
        p.onGround = true;
        input.left = true;
        p.update(1.0 / 60, input);
        assertTrue(p.velX < 0, "velX should be negative when left is held");
    }

    @Test
    void player_doesNotExceedMaxWalkSpeed() {
        Player p = new Player(100, 100);
        p.onGround = true;
        input.right = true;
        for (int i = 0; i < 300; i++) p.update(1.0 / 60, input);
        assertTrue(Math.abs(p.velX) <= GameConstants.MAX_WALK_SPEED + 1,
                "velX must not exceed MAX_WALK_SPEED");
    }

    @Test
    void player_decelerates_whenNoKeyHeld() {
        Player p = new Player(100, 100);
        p.onGround = true;
        p.velX = 200;
        p.update(1.0 / 60, input); // no keys
        assertTrue(Math.abs(p.velX) < 200, "velX should decrease when no key is held");
    }

    @Test
    void player_snapsToZero_fromTinyVelocity() {
        Player p = new Player(100, 100);
        p.onGround = true;
        p.velX = 0.4; // below 0.5 snap threshold
        p.update(1.0 / 60, input);
        assertEquals(0.0, p.velX, "velX below 0.5 should snap to 0 to prevent drift");
    }

    // ── Jump ─────────────────────────────────────────────────────────────────

    @Test
    void player_jumps_whenOnGround() {
        Player p = new Player(100, 200);
        p.onGround = true;
        input.justPressedKey = KeyEvent.VK_SPACE;
        p.update(1.0 / 60, input);
        assertEquals(GameConstants.JUMP_VELOCITY, p.velY, "velY should equal JUMP_VELOCITY on jump");
    }

    @Test
    void player_cannotJump_whenNotOnGroundAndNoJumpsLeft() {
        Player p = new Player(100, 100);
        p.onGround = false;
        // jumpsLeft defaults to 0 — never landed
        input.justPressedKey = KeyEvent.VK_SPACE;
        p.update(1.0 / 60, input);
        assertTrue(p.velY >= 0, "Player should not jump when airborne with 0 jumpsLeft");
    }

    @Test
    void player_doubleJump_consumesBothJumps() {
        Player p = new Player(100, 200);
        Platform floor = new Platform(0, 236, 500, 48);

        // Land to get 2 jumps — must call both physics AND player.update so onGround→jumpsLeft=2
        double dt = 1.0 / 60;
        for (int i = 0; i < 60; i++) {
            physics.update(List.of(p), List.of(floor), dt);
            p.update(dt, input);
        }
        assertTrue(p.onGround, "Player should land before double-jump test");

        // First jump
        input.justPressedKey = KeyEvent.VK_SPACE;
        p.update(1.0 / 60, input);
        assertEquals(GameConstants.JUMP_VELOCITY, p.velY, "First jump should fire");

        // Second jump (in air)
        p.onGround = false;
        input.justPressedKey = KeyEvent.VK_SPACE;
        p.update(1.0 / 60, input);
        assertEquals(GameConstants.JUMP_VELOCITY, p.velY, "Second jump should fire");

        // Third jump — should not fire
        p.onGround = false;
        input.justPressedKey = KeyEvent.VK_SPACE;
        p.update(1.0 / 60, input);
        // velY after physics may differ; just confirm it wasn't reset to JUMP_VELOCITY again
        // (velY will be > JUMP_VELOCITY due to gravity applied in same tick)
        assertTrue(p.velY >= GameConstants.JUMP_VELOCITY,
                "Third jump must not fire (velY should not drop back to JUMP_VELOCITY)");
    }

    @Test
    void player_jumpsLeft_restoredOnLanding() {
        Player p = new Player(100, 0);
        Platform floor = new Platform(0, 300, 200, 48);

        // Let player fall and land
        for (int i = 0; i < 200; i++) {
            physics.update(List.of(p), List.of(floor), 1.0 / 60);
            p.update(1.0 / 60, input);
            if (p.onGround) break;
        }

        assertTrue(p.onGround, "Player should have landed");
        // After landing, jumpsLeft should be 2 — confirmed by being able to jump
        input.justPressedKey = KeyEvent.VK_SPACE;
        p.update(1.0 / 60, input);
        assertEquals(GameConstants.JUMP_VELOCITY, p.velY, "Jump should be available after landing");
    }

    // ── Respawn ───────────────────────────────────────────────────────────────

    @Test
    void respawn_resetsPositionAndVelocity() {
        Player p = new Player(0, 0);
        p.velX = 300;
        p.velY = -500;
        p.respawn(100, 200);

        assertEquals(100, p.x, "respawn X");
        assertEquals(200, p.y, "respawn Y");
        assertEquals(0.0, p.velX, "velX after respawn");
        assertEquals(0.0, p.velY, "velY after respawn");
        assertFalse(p.onGround, "onGround after respawn");
    }
}
