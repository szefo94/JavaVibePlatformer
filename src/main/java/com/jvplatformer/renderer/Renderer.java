package com.jvplatformer.renderer;

import com.jvplatformer.GameConstants;
import com.jvplatformer.engine.Camera;
import com.jvplatformer.engine.DebugStats;
import com.jvplatformer.entity.Coin;
import com.jvplatformer.entity.Entity;
import com.jvplatformer.entity.Platform;
import com.jvplatformer.level.Level;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.List;

public class Renderer {
    private final DebugOverlay debugOverlay = new DebugOverlay();

    private static final Color GOAL_LOCKED_TINT = new Color(0, 0, 0, 120);

    // Cached HUD strings — only rebuilt when values change
    private int    cachedScore = -1, cachedTotal = -1;
    private long   cachedFps   = -1;
    private String hudCoins = "", hudFps = "";

    public void render(Graphics2D g, Level level, List<Entity> dynamics,
                       Camera camera, DebugStats stats, boolean debugEnabled,
                       boolean levelComplete, int score) {

        int w = GameConstants.SCREEN_WIDTH;
        int h = GameConstants.SCREEN_HEIGHT;

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,    RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,       RenderingHints.VALUE_RENDER_SPEED);
        g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_SPEED);

        // 1. Sky — flat fill is much faster than gradient on Pi (no per-pixel blending)
        g.setColor(GameConstants.COLOR_SKY_TOP);
        g.fillRect(0, 0, w, h);

        // 2. Platforms
        List<Platform> platforms = level.platforms;
        for (int i = 0, n = platforms.size(); i < n; i++) {
            platforms.get(i).render(g, camera);
        }

        // 3. Coins
        List<Coin> coins = level.coins;
        for (int i = 0, n = coins.size(); i < n; i++) {
            coins.get(i).render(g, camera);
        }

        // 4. Level goal
        if (level.goal != null) {
            boolean unlocked = (score >= level.coins.size());
            // Draw locked/unlocked indicator
            if (!unlocked) {
                // Draw greyed-out overlay on the goal to indicate locked state
                int drawX = (int) level.goal.x - camera.getOffsetX();
                int drawY = (int) level.goal.y - camera.getOffsetY();
                level.goal.render(g, camera);
                g.setColor(GOAL_LOCKED_TINT);
                g.fillRect(drawX, drawY, level.goal.width, level.goal.height);
                g.setColor(GameConstants.COLOR_HUD);
                g.setFont(GameConstants.HUD_FONT);
                g.drawString("!", drawX + 18, drawY + level.goal.height / 2 + 6);
            } else {
                level.goal.render(g, camera);
            }
        }

        // 5. Dynamic entities (player)
        for (int i = 0, n = dynamics.size(); i < n; i++) {
            Entity e = dynamics.get(i);
            if (e.active) e.render(g, camera);
        }

        // 6. HUD — rebuild strings only when values change to avoid per-frame allocation
        int total = level.coins.size();
        if (score != cachedScore || total != cachedTotal) {
            cachedScore = score; cachedTotal = total;
            hudCoins = "Coins: " + score + " / " + total;
        }
        if (stats.fps != cachedFps) {
            cachedFps = stats.fps;
            hudFps = "FPS: " + stats.fps;
        }
        g.setColor(GameConstants.COLOR_HUD);
        g.setFont(GameConstants.HUD_FONT);
        g.drawString(hudCoins, w / 2 - 50, 18);
        g.drawString(hudFps, w - 80, 18);
        g.drawString("WASD/Arrows=move  Space/Up=jump  ESC=quit  F3=debug", 8, h - 8);

        // 7. Level complete overlay
        if (levelComplete) {
            drawLevelComplete(g, w, h, score, total);
        }

        // 8. Debug overlay (on top of everything)
        if (debugEnabled) {
            debugOverlay.render(g, stats);
        }
    }

    private void drawLevelComplete(Graphics2D g, int w, int h, int score, int total) {
        g.setColor(GameConstants.COLOR_COMPLETE_BG);
        g.fillRect(0, 0, w, h);

        g.setFont(GameConstants.COMPLETE_FONT);
        g.setColor(GameConstants.COLOR_COMPLETE_TEXT);
        String title = "LEVEL COMPLETE!";
        FontMetrics fm = g.getFontMetrics();
        g.drawString(title, (w - fm.stringWidth(title)) / 2, h / 2 - 30);

        g.setFont(GameConstants.COMPLETE_SUB);
        g.setColor(GameConstants.COLOR_COMPLETE_SUB);
        String sub = "Score: " + score + " / " + total + " coins";
        fm = g.getFontMetrics();
        g.drawString(sub, (w - fm.stringWidth(sub)) / 2, h / 2 + 20);

        String prompt = "Press SPACE or ENTER to play again";
        g.drawString(prompt, (w - fm.stringWidth(prompt)) / 2, h / 2 + 60);
    }
}
