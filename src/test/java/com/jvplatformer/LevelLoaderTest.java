package com.jvplatformer;

import com.jvplatformer.entity.Platform;
import com.jvplatformer.level.Level;
import com.jvplatformer.level.LevelLoader;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LevelLoaderTest {

    private static final int T = GameConstants.TILE_SIZE; // 48

    // ── Spawn ─────────────────────────────────────────────────────────────────

    @Test
    void spawn_parsedFromP() {
        Level lvl = LevelLoader.loadFromLines(List.of(
                "P..",
                "###"
        ));
        assertEquals(0.0, lvl.spawnX);
        assertEquals(0.0, lvl.spawnY);
    }

    @Test
    void spawn_defaultsToOneTile_whenMissing() {
        Level lvl = LevelLoader.loadFromLines(List.of("###"));
        assertEquals(T, lvl.spawnX);
        assertEquals(T, lvl.spawnY);
    }

    // ── Platforms ─────────────────────────────────────────────────────────────

    @Test
    void singleTile_createsSinglePlatform() {
        Level lvl = LevelLoader.loadFromLines(List.of("#"));
        assertEquals(1, lvl.platforms.size());
        Platform p = lvl.platforms.get(0);
        assertEquals(0,  (int) p.x);
        assertEquals(0,  (int) p.y);
        assertEquals(T,  p.width);
        assertEquals(T,  p.height);
    }

    @Test
    void adjacentTiles_mergedIntoOnePlatform() {
        Level lvl = LevelLoader.loadFromLines(List.of("####"));
        assertEquals(1, lvl.platforms.size(), "Four adjacent tiles must merge into one platform");
        assertEquals(4 * T, lvl.platforms.get(0).width);
    }

    @Test
    void nonAdjacentTiles_createSeparatePlatforms() {
        Level lvl = LevelLoader.loadFromLines(List.of("#.#"));
        assertEquals(2, lvl.platforms.size(), "Tiles separated by '.' must not merge");
    }

    @Test
    void tilesOnDifferentRows_createSeparatePlatforms() {
        Level lvl = LevelLoader.loadFromLines(List.of(
                "##",
                "##"
        ));
        assertEquals(2, lvl.platforms.size(), "Tiles on different rows must not merge");
    }

    @Test
    void platform_positionedCorrectly_forRowAndCol() {
        Level lvl = LevelLoader.loadFromLines(List.of(
                "...",
                ".#."
        ));
        Platform p = lvl.platforms.get(0);
        assertEquals(T,     (int) p.x, "Platform col=1 -> x=T");
        assertEquals(T,     (int) p.y, "Platform row=1 -> y=T");
    }

    // ── Coins ─────────────────────────────────────────────────────────────────

    @Test
    void coin_placedWhenPlatformWithinJumpReach() {
        // '*' at row 0, '#' at row 1 — within MAX_JUMP_TILES=3
        Level lvl = LevelLoader.loadFromLines(List.of(
                "*",
                "#"
        ));
        assertEquals(1, lvl.coins.size(), "Coin should be placed above reachable platform");
    }

    @Test
    void coin_notPlaced_whenNoPlatformBelow() {
        Level lvl = LevelLoader.loadFromLines(List.of(
                "*",
                ".."
        ));
        assertEquals(0, lvl.coins.size(), "Coin must be rejected if no platform within jump reach");
    }

    @Test
    void coin_notPlaced_whenPlatformTooFarBelow() {
        // MAX_JUMP_TILES = 3; platform at row 5 is too far below row 0
        Level lvl = LevelLoader.loadFromLines(List.of(
                "*",
                ".",
                ".",
                ".",
                ".",
                "#"
        ));
        assertEquals(0, lvl.coins.size(), "Coin must be rejected when platform is > 3 tiles below");
    }

    @Test
    void multipleCoinRows_allFiltered() {
        Level lvl = LevelLoader.loadFromLines(List.of(
                "*.*",   // col 0 has ground, col 2 has no ground
                "#.."
        ));
        assertEquals(1, lvl.coins.size(), "Only the coin above a platform should be created");
    }

    // ── Goal ─────────────────────────────────────────────────────────────────

    @Test
    void goal_parsedFromE() {
        Level lvl = LevelLoader.loadFromLines(List.of(
                "PE",
                "##"
        ));
        assertNotNull(lvl.goal, "Goal must be created for 'E' tile");
    }

    @Test
    void goal_isNull_whenNoEInLevel() {
        Level lvl = LevelLoader.loadFromLines(List.of(
                "P#",
                "##"
        ));
        assertNull(lvl.goal, "Goal must be null when 'E' is absent");
    }

    // ── Dimensions ────────────────────────────────────────────────────────────

    @Test
    void levelDimensions_matchTileGridSize() {
        Level lvl = LevelLoader.loadFromLines(List.of(
                "###",   // 3 cols
                "###",   // 2 rows
                "###"
        ));
        assertEquals(3 * T, lvl.widthPx);
        assertEquals(3 * T, lvl.heightPx);
    }

    // ── Real level resource ───────────────────────────────────────────────────

    @Test
    void realLevel1_loadsWithoutException() {
        assertDoesNotThrow(() -> LevelLoader.load("/levels/level1.txt"),
                "level1.txt must parse cleanly");
    }

    @Test
    void realLevel1_hasAtLeastOnePlatformAndSpawn() {
        Level lvl = LevelLoader.load("/levels/level1.txt");
        assertFalse(lvl.platforms.isEmpty(), "level1 must have at least one platform");
        assertTrue(lvl.spawnX >= 0 && lvl.spawnY >= 0, "level1 spawn must be non-negative");
    }
}
