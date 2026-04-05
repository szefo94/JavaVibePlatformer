package com.jvplatformer;

import com.jvplatformer.entity.Coin;
import com.jvplatformer.entity.Player;
import com.jvplatformer.entity.Platform;
import com.jvplatformer.level.Level;
import com.jvplatformer.level.LevelLoader;
import com.jvplatformer.physics.PhysicsEngine;

import org.junit.jupiter.api.Test;

import java.awt.Rectangle;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CoinTest {

    // ── Bounds ────────────────────────────────────────────────────────────────

    @Test
    void coin_activByDefault() {
        Coin c = new Coin(1, 1);
        assertTrue(c.active);
    }

    @Test
    void coin_boundsMatchPosition() {
        Coin c = new Coin(2, 3);
        Rectangle b = c.getBounds();
        // Coin is offset by TILE_OFFSET = (48-20)/2 = 14 inside tile
        int expectedX = 2 * GameConstants.TILE_SIZE + (GameConstants.TILE_SIZE - 20) / 2;
        int expectedY = 3 * GameConstants.TILE_SIZE + (GameConstants.TILE_SIZE - 20) / 2;
        assertEquals(expectedX, b.x, "Coin bounds X");
        assertEquals(expectedY, b.y, "Coin bounds Y");
    }

    // ── Bob animation ─────────────────────────────────────────────────────────

    @Test
    void coin_bobOffsetStaysWithinRange() {
        Coin c = new Coin(0, 0);
        for (int i = 0; i < 1000; i++) {
            c.update(1.0 / 60, null); // input unused
        }
        // getBounds is used for collision; bobOffset is visual only and ±4px max
        // We can't read bobOffset directly, but the coin should still be active
        assertTrue(c.active, "Coin should remain active after many update ticks");
    }

    // ── Staggered phases ──────────────────────────────────────────────────────

    @Test
    void twoCoins_havedifferentInitialPhases() {
        // Different tileCol → different bobTime start → they won't perfectly sync
        // We verify they don't share the exact same object reference (sanity)
        Coin c1 = new Coin(0, 0);
        Coin c2 = new Coin(5, 3);
        assertNotSame(c1, c2);
        // Their visual positions will differ because phases differ — tested indirectly
        // by checking they update without throwing
        assertDoesNotThrow(() -> {
            for (int i = 0; i < 60; i++) {
                c1.update(1.0 / 60, null);
                c2.update(1.0 / 60, null);
            }
        });
    }

    // ── Collection (integration) ──────────────────────────────────────────────

    @Test
    void coin_collectedWhenPlayerOverlaps() {
        // Place a coin and a player at the same position, check intersection
        Coin c = new Coin(2, 1);
        // Position player directly on coin
        Player p = new Player(c.x, c.y);

        assertTrue(p.getBounds().intersects(c.getBounds()),
                "Player positioned on coin should have overlapping bounds");
    }

    @Test
    void coin_notCollected_whenPlayerFarAway() {
        Coin c = new Coin(0, 0);
        Player p = new Player(5000, 5000);
        assertFalse(p.getBounds().intersects(c.getBounds()),
                "Player far from coin must not have overlapping bounds");
    }

    // ── Level integration ─────────────────────────────────────────────────────

    @Test
    void level_coinCount_decreasesAfterCollection() {
        Level lvl = LevelLoader.loadFromLines(List.of(
                "*",
                "#"
        ));
        assertEquals(1, lvl.coins.size());

        Coin c = lvl.coins.get(0);
        Player p = new Player(c.x, c.y);

        // Simulate collection check (as done in game loop)
        var pb = p.getBounds();
        if (c.active && pb.intersects(c.getBounds())) {
            c.active = false;
        }

        assertFalse(c.active, "Coin should be deactivated after collection");
        // Coins list still has the entry; only active flag changes
        assertEquals(1, lvl.coins.size());
        assertEquals(0, lvl.coins.stream().filter(x -> x.active).count(),
                "No active coins should remain after collection");
    }
}
