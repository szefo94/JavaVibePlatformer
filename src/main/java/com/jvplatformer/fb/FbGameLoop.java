package com.jvplatformer.fb;

import com.jvplatformer.GameConstants;
import com.jvplatformer.engine.Camera;
import com.jvplatformer.engine.DebugStats;
import com.jvplatformer.engine.PerfLogger;
import com.jvplatformer.entity.Coin;
import com.jvplatformer.entity.Entity;
import com.jvplatformer.entity.Player;
import com.jvplatformer.level.Level;
import com.jvplatformer.level.LevelLoader;
import com.jvplatformer.physics.PhysicsEngine;
import com.jvplatformer.renderer.Renderer;

import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.ArrayList;
import java.util.List;

/**
 * Game loop that runs without any AWT/Swing/X11 dependency.
 *
 * Rendering: BufferedImage -> RGB565 -> /dev/fb0 via memory-mapped file
 * Input:     /dev/input/event0, event1 (raw evdev, no display server)
 *
 * Must be launched from a TTY (Ctrl+Alt+F2), not from inside X11/Wayland.
 */
public class FbGameLoop {

    private static final long NS_PER_UPDATE =
            1_000_000_000L / GameConstants.TARGET_UPS;

    // Keyboard: event0 = Logitech, event1 = Dell (both detected as keyboards)
    private final EvdevInput    input   = new EvdevInput("/dev/input/event0",
                                                          "/dev/input/event1");
    private final PhysicsEngine physics = new PhysicsEngine();
    private final Camera        camera  = new Camera();
    private final Renderer      renderer = new Renderer();
    private final DebugStats    stats   = new DebugStats();
    private final PerfLogger    perfLog = new PerfLogger();
    private final MemoryMXBean  memBean = ManagementFactory.getMemoryMXBean();
    private final List<GarbageCollectorMXBean> gcBeans =
            ManagementFactory.getGarbageCollectorMXBeans();

    private FrameBuffer fb;

    private Level        level;
    private Player       player;
    private List<Entity> dynamics;

    private boolean debugEnabled  = false;
    private boolean levelComplete = false;
    private int     score         = 0;
    private long    fps           = 0;

    public void start() throws IOException {
        fb = new FrameBuffer();

        loadLevel("/levels/level1.txt");
        warmup();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            perfLog.close();
            fb.close();
            input.stop();
        }));

        gameLoop();
    }

    private void loadLevel(String path) {
        level         = LevelLoader.load(path);
        player        = level.createPlayer();
        dynamics      = new ArrayList<>();
        dynamics.add(player);
        score         = 0;
        levelComplete = false;
        camera.reset();
    }

    /** Spin update+render 300x to push JIT through C1→C2 tiers before game starts. */
    private void warmup() {
        for (int i = 0; i < 300; i++) {
            physics.update(dynamics, level.platforms, 1.0 / GameConstants.TARGET_UPS);
            render();
        }
    }

    private void gameLoop() {
        long lastTime   = System.nanoTime();
        long delta      = 0;
        long fpsTimer   = System.nanoTime();  // updates FPS display every 250ms
        long statsTimer = System.nanoTime();  // heavy stats/log every 1s
        long frameCount = 0;
        long worstFrame = 0;

        while (true) {
            long frameStart = System.nanoTime();
            long now        = frameStart;
            delta += now - lastTime;
            lastTime = now;

            while (delta >= NS_PER_UPDATE) {
                long t0 = System.nanoTime();
                update();
                stats.updateTimeNs = System.nanoTime() - t0;
                delta -= NS_PER_UPDATE;
            }

            long t0 = System.nanoTime();
            render();
            stats.renderTimeNs = System.nanoTime() - t0;
            frameCount++;

            long frameTotal = System.nanoTime() - frameStart;
            if (frameTotal > worstFrame) worstFrame = frameTotal;

            // Update FPS display every 250ms so HUD reflects current throughput quickly
            if (now - fpsTimer >= 250_000_000L) {
                long elapsed = now - fpsTimer;
                fps        = frameCount * 1_000_000_000L / elapsed;
                frameCount = 0;
                fpsTimer   = now;
            }

            // Heavy stats sampling + perf log once per second
            if (now - statsTimer >= 1_000_000_000L) {
                statsTimer            = now;
                stats.worstFrameNs    = worstFrame;
                worstFrame            = 0;
                stats.keyEventsPerSec = input.drainKeyEventCount();
                stats.heldKeys        = input.heldSummary();
                refreshDebugStats();
                perfLog.tick(stats);
            }

            // Hybrid sleep: coarse sleep + busy-spin last 2ms
            long deadline  = lastTime + NS_PER_UPDATE;
            long remaining = deadline - System.nanoTime();
            if (remaining > 2_000_000L) {
                try { Thread.sleep((remaining - 2_000_000L) / 1_000_000L); }
                catch (InterruptedException ignored) {}
            }
            while (System.nanoTime() < deadline) { Thread.onSpinWait(); }
        }
    }

    private void update() {
        if (input.isJustPressed(KeyEvent.VK_ESCAPE)) {
            perfLog.close();
            fb.close();
            input.stop();
            System.exit(0);
        }
        if (input.isJustPressed(KeyEvent.VK_F3)) debugEnabled = !debugEnabled;

        if (levelComplete) {
            if (input.isJustPressed(KeyEvent.VK_SPACE)
                    || input.isJustPressed(KeyEvent.VK_ENTER)) {
                loadLevel("/levels/level1.txt");
            }
            input.clearJustPressed();
            return;
        }

        physics.update(dynamics, level.platforms, 1.0 / GameConstants.TARGET_UPS);

        double dt = 1.0 / GameConstants.TARGET_UPS;
        for (int i = 0, n = dynamics.size(); i < n; i++) {
            dynamics.get(i).update(dt, input);
        }

        List<Coin> coins = level.coins;
        for (int i = 0, n = coins.size(); i < n; i++) {
            Coin c = coins.get(i);
            if (c.active) c.update(dt, input);
        }

        camera.update(player, level.widthPx, level.heightPx);

        var playerBounds = player.getBounds();
        for (int i = 0, n = coins.size(); i < n; i++) {
            Coin c = coins.get(i);
            if (c.active && playerBounds.intersects(c.getBounds())) {
                c.active = false;
                score++;
            }
        }

        if (level.goal != null) {
            boolean allCoins = (score >= level.coins.size());
            if (allCoins && playerBounds.intersects(level.goal.getBounds())) {
                levelComplete = true;
            }
        }

        if (player.y > level.heightPx + 200) {
            player.respawn(level.spawnX, level.spawnY);
            camera.reset();
        }

        input.clearJustPressed();
    }

    private void render() {
        stats.fps           = fps;
        stats.dynamicCount  = dynamics.size();
        stats.platformCount = level.platforms.size();
        stats.coinTotal     = level.coins.size();
        stats.coinCollected = score;
        stats.playerX       = player.x;
        stats.playerY       = player.y;
        stats.playerVelX    = player.velX;
        stats.playerVelY    = player.velY;
        stats.playerOnGround = player.onGround;
        stats.cameraOffsetX = camera.getOffsetX();
        stats.cameraOffsetY = camera.getOffsetY();

        long t0 = System.nanoTime();
        Graphics2D g = fb.createGraphics();
        try {
            renderer.render(g, level, dynamics, camera, stats, debugEnabled, levelComplete, score);
        } finally {
            g.dispose();
        }
        stats.drawTimeNs = System.nanoTime() - t0;

        t0 = System.nanoTime();
        fb.flip();
        stats.showTimeNs = System.nanoTime() - t0;
        stats.syncTimeNs = 0;
    }

    private void refreshDebugStats() {
        var heap = memBean.getHeapMemoryUsage();
        stats.heapUsed      = heap.getUsed();
        stats.heapCommitted = heap.getCommitted();
        stats.heapMax       = heap.getMax();

        long count = 0, time = 0;
        for (GarbageCollectorMXBean gc : gcBeans) {
            long c = gc.getCollectionCount();
            long t = gc.getCollectionTime();
            if (c >= 0) count += c;
            if (t >= 0) time  += t;
        }
        stats.gcCount  = count;
        stats.gcTimeMs = time;
    }
}
