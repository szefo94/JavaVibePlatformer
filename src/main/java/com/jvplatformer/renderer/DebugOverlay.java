package com.jvplatformer.renderer;

import com.jvplatformer.engine.DebugStats;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

/**
 * Semi-transparent debug panel (toggle F3).
 * Shows timing, memory, GC, entity counts and player state.
 */
public class DebugOverlay {
    private static final Font  FONT       = new Font("Monospaced", Font.PLAIN, 12);
    private static final Color BG         = new Color(0, 0, 0, 160);
    private static final Color TEXT       = new Color(200, 255, 200);
    private static final Color LABEL      = new Color(140, 200, 255);
    private static final Color WARN       = new Color(255, 160, 60);
    private static final Color BAR_UPDATE = new Color(80, 200, 80);
    private static final Color BAR_RENDER = new Color(80, 160, 255);
    private static final Color BAR_GC     = new Color(255, 80, 80);
    private static final Color BAR_BG     = new Color(40, 40, 40);

    private static final int PAD    = 8;
    private static final int W      = 280;
    private static final int LINE_H = 16;

    public void render(Graphics2D g, DebugStats s) {
        g.setFont(FONT);

        // Build lines
        String[] labels = {
            "FPS",
            "Update",
            "Render",
            "GC count",
            "GC time",
            "Heap used",
            "Heap commit",
            "Heap max",
            "Entities",
            "Platforms",
            "Coins",
            "Player X",
            "Player Y",
            "Vel X",
            "Vel Y",
            "On ground",
            "Camera X",
        };
        String[] values = {
            String.valueOf(s.fps),
            us(s.updateTimeNs),
            us(s.renderTimeNs),
            String.valueOf(s.gcCount),
            s.gcTimeMs + " ms",
            mb(s.heapUsed)    + " MB",
            mb(s.heapCommitted) + " MB",
            mb(s.heapMax)     + " MB",
            String.valueOf(s.dynamicCount),
            String.valueOf(s.platformCount),
            s.coinCollected + " / " + s.coinTotal,
            String.format("%.1f", s.playerX),
            String.format("%.1f", s.playerY),
            String.format("%.1f", s.playerVelX),
            String.format("%.1f", s.playerVelY),
            s.playerOnGround ? "yes" : "no",
            String.valueOf(s.cameraOffsetX),
        };

        int rows = labels.length;
        // +3 rows: header + bar chart (2 rows)
        int totalH = PAD * 2 + LINE_H * (rows + 3) + PAD;

        // Background panel — fillRect is faster than fillRoundRect in software rendering
        g.setColor(BG);
        g.fillRect(PAD, PAD, W, totalH);

        int y = PAD * 2 + LINE_H;

        // Header
        g.setColor(LABEL);
        g.drawString("[ DEBUG  F3=toggle ]", PAD * 2, y);
        y += LINE_H + 2;

        // Text rows
        for (int i = 0; i < labels.length; i++) {
            // Highlight slow frame
            if (i == 1 && s.updateTimeNs > 8_000_000L) g.setColor(WARN);
            else if (i == 2 && s.renderTimeNs > 8_000_000L) g.setColor(WARN);
            else g.setColor(TEXT);

            g.drawString(labels[i], PAD * 2, y);
            g.drawString(values[i], PAD * 2 + 120, y);
            y += LINE_H;
        }

        y += PAD;

        // Timing bar: update vs render vs gc vs idle
        long frameNs = 1_000_000_000L / Math.max(s.fps, 1);
        long gcNs    = s.gcTimeMs * 1_000_000L / Math.max(s.fps, 1); // rough per-frame share
        int barW     = W - PAD * 4;
        int barH     = 10;

        g.setColor(LABEL);
        g.drawString("Frame budget", PAD * 2, y);
        y += LINE_H - 2;

        g.setColor(BAR_BG);
        g.fillRect(PAD * 2, y, barW, barH);

        int uW = fraction(s.updateTimeNs, frameNs, barW);
        int rW = fraction(s.renderTimeNs, frameNs, barW);
        int gW = fraction(gcNs, frameNs, barW);

        g.setColor(BAR_UPDATE);
        g.fillRect(PAD * 2, y, uW, barH);
        g.setColor(BAR_RENDER);
        g.fillRect(PAD * 2 + uW, y, rW, barH);
        g.setColor(BAR_GC);
        g.fillRect(PAD * 2 + uW + rW, y, gW, barH);

        y += barH + 4;
        g.setColor(BAR_UPDATE); g.fillRect(PAD * 2,       y, 10, 8);
        g.setColor(TEXT);       g.drawString("update", PAD * 2 + 13, y + 8);
        g.setColor(BAR_RENDER); g.fillRect(PAD * 2 + 80,  y, 10, 8);
        g.setColor(TEXT);       g.drawString("render", PAD * 2 + 93, y + 8);
        g.setColor(BAR_GC);     g.fillRect(PAD * 2 + 160, y, 10, 8);
        g.setColor(TEXT);       g.drawString("gc",     PAD * 2 + 173, y + 8);
    }

    private static String us(long ns) {
        return (ns / 1000) + " µs";
    }

    private static long mb(long bytes) {
        return bytes / (1024 * 1024);
    }

    private static int fraction(long part, long total, int barWidth) {
        if (total <= 0) return 0;
        return (int) Math.min(barWidth, (long) barWidth * part / total);
    }
}
