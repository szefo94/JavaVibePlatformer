package com.jvplatformer.level;

import com.jvplatformer.GameConstants;
import com.jvplatformer.entity.Coin;
import com.jvplatformer.entity.LevelGoal;
import com.jvplatformer.entity.Platform;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class LevelLoader {

    public static Level load(String resourcePath) {
        InputStream is = LevelLoader.class.getResourceAsStream(resourcePath);
        if (is == null) {
            throw new RuntimeException("Level resource not found: " + resourcePath);
        }

        List<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read level: " + resourcePath, e);
        }

        return loadFromLines(lines);
    }

    /** Load a level from a list of strings — used by tests without classpath resources. */
    public static Level loadFromLines(List<String> lines) {
        int rows = lines.size();
        int cols = lines.stream().mapToInt(String::length).max().orElse(0);
        int t    = GameConstants.TILE_SIZE;

        List<Platform> platforms = new ArrayList<>();
        List<Coin>     coins     = new ArrayList<>();
        LevelGoal      goal      = null;
        double spawnX = t, spawnY = t;

        // Max tiles a player can jump upward (v²/2g = 720²/3600 ≈ 144 px = 3 tiles)
        final int MAX_JUMP_TILES = 3;

        for (int row = 0; row < rows; row++) {
            String line = lines.get(row);
            for (int col = 0; col < line.length(); col++) {
                char ch = line.charAt(col);
                int px = col * t;
                int py = row * t;

                switch (ch) {
                    case '#' -> {
                        // Merge horizontally: extend previous platform or create new one
                        if (!platforms.isEmpty()) {
                            Platform last = platforms.get(platforms.size() - 1);
                            if ((int) last.y == py && (int)(last.x + last.width) == px) {
                                last.width += t;
                                continue;
                            }
                        }
                        platforms.add(new Platform(px, py, t, t));
                    }
                    case 'P' -> {
                        spawnX = px;
                        spawnY = py;
                    }
                    case '*' -> {
                        // Only place coin if there is a platform within jump reach below it
                        boolean hasGround = false;
                        for (int below = row + 1; below <= Math.min(row + MAX_JUMP_TILES, rows - 1); below++) {
                            String belowLine = lines.get(below);
                            if (col < belowLine.length() && belowLine.charAt(col) == '#') {
                                hasGround = true;
                                break;
                            }
                        }
                        if (hasGround) coins.add(new Coin(col, row));
                    }
                    case 'E' -> goal = new LevelGoal(col, row);
                }
            }
        }

        return new Level(platforms, coins, goal, spawnX, spawnY, cols * t, rows * t);
    }
}
