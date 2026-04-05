package com.jvplatformer.engine;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Writes performance events to perf.log only when anomalies occur.
 * Silent (zero I/O) during normal play — no impact on frame timing.
 *
 * Thresholds:
 *   FPS drop  : fps < TARGET_FPS * 0.8  (below 80% of target)
 *   Slow frame : update or render > 8 ms
 *   GC spike   : GC count increased since last check
 */
public class PerfLogger implements AutoCloseable {

    private static final int    TARGET_FPS      = 60;
    private static final double FPS_THRESHOLD   = TARGET_FPS * 0.8;  // 48 fps
    private static final long   FRAME_WARN_NS   = 16_000_000L;       // 16 ms (half of 16.67ms budget)

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private final BufferedWriter writer;

    // Background writer — game loop puts lines here, writer thread drains it
    private static final String POISON = "__STOP__";
    private final BlockingQueue<String> queue = new ArrayBlockingQueue<>(256);
    private final Thread writerThread;

    // State tracked between calls
    private long prevGcCount   = 0;
    private long prevGcTimeMs  = 0;
    private long prevHeapUsed  = 0;
    private int  fpsDipCount   = 0;
    private long secondsLogged = 0;

    public PerfLogger() {
        BufferedWriter w = null;
        try {
            w = new BufferedWriter(new FileWriter("perf.log", false));
            w.write("=== PerfLogger started " + LocalDateTime.now() + " ==="); w.newLine();
            w.write("Target FPS: " + TARGET_FPS
                    + "  FPS warn threshold: " + (int) FPS_THRESHOLD
                    + "  Frame warn: " + (FRAME_WARN_NS / 1_000_000) + " ms"); w.newLine();
            w.write("---"); w.newLine();
            w.flush();
        } catch (IOException e) {
            System.err.println("[PerfLogger] Could not open perf.log: " + e.getMessage());
        }
        writer = w;

        // Background thread drains the queue and does all file I/O
        // so the game loop thread never blocks on disk writes
        writerThread = new Thread(() -> {
            try {
                while (true) {
                    String line = queue.take();
                    if (line == POISON) break;
                    if (writer != null) {
                        try { writer.write(line); writer.newLine(); writer.flush(); }
                        catch (IOException ignored) {}
                    }
                }
            } catch (InterruptedException ignored) {}
        }, "perf-logger");
        writerThread.setDaemon(true);
        writerThread.setPriority(Thread.MIN_PRIORITY);
        writerThread.start();
    }

    /**
     * Call once per second from the game loop (already in the 1-Hz refresh block).
     * Logs nothing if everything is within normal range.
     */
    public void tick(DebugStats s) {
        if (writer == null) return;
        secondsLogged++;

        boolean anyEvent = false;
        StringBuilder sb = new StringBuilder();
        String ts = LocalDateTime.now().format(FMT);
        sb.append("[").append(ts).append("] ");

        // --- FPS ---
        if (s.fps < FPS_THRESHOLD) {
            fpsDipCount++;
            sb.append("FPS_DROP fps=").append(s.fps)
              .append(" (dip#").append(fpsDipCount).append(") | ");
            anyEvent = true;
        } else {
            if (fpsDipCount > 0) {
                // Recovery — log that we recovered
                sb.append("FPS_RECOVER after ").append(fpsDipCount).append("s | ");
                anyEvent = true;
            }
            fpsDipCount = 0;
        }

        // --- Slow frame (update) ---
        if (s.updateTimeNs > FRAME_WARN_NS) {
            sb.append("SLOW_UPDATE ").append(s.updateTimeNs / 1_000_000).append("ms | ");
            anyEvent = true;
        }

        // --- Slow frame (render) ---
        if (s.renderTimeNs > FRAME_WARN_NS) {
            sb.append("SLOW_RENDER ").append(s.renderTimeNs / 1_000_000).append("ms | ");
            anyEvent = true;
        }

        // --- Frame jitter: worst single frame this second ---
        if (s.worstFrameNs > FRAME_WARN_NS) {
            sb.append("JITTER worst=").append(s.worstFrameNs / 1_000_000).append("ms | ");
            anyEvent = true;
        }

        // --- GC activity ---
        long gcDeltaCount = s.gcCount  - prevGcCount;
        long gcDeltaMs    = s.gcTimeMs - prevGcTimeMs;
        if (gcDeltaCount > 0) {
            sb.append("GC count+").append(gcDeltaCount)
              .append(" time+").append(gcDeltaMs).append("ms | ");
            anyEvent = true;
        }
        prevGcCount  = s.gcCount;
        prevGcTimeMs = s.gcTimeMs;

        // --- Heap growth rate ---
        long heapGrowthKb = (s.heapUsed - prevHeapUsed) / 1024;
        prevHeapUsed = s.heapUsed;

        // --- Append snapshot only when something was flagged ---
        if (anyEvent) {
            sb.append(" [heap=").append(mb(s.heapUsed)).append("MB")
              .append(" +").append(heapGrowthKb).append("KB/s")
              .append(" worst=").append(s.worstFrameNs / 1_000_000).append("ms")
              .append(" upd=").append(s.updateTimeNs / 1000).append("us")
              .append(" draw=").append(s.drawTimeNs / 1000).append("us")
              .append(" show=").append(s.showTimeNs / 1000).append("us")
              .append(" plat=").append(s.platformCount)
              .append(" coins=").append(s.coinCollected).append("/").append(s.coinTotal)
              .append(" px=").append((int) s.playerX).append(",").append((int) s.playerY)
              .append(" keys=[").append(s.heldKeys).append("]")
              .append(" kps=").append(s.keyEventsPerSec)
              .append("]");
            writeLine(sb.toString());
        }

        // --- Periodic summary every 30 s so the file isn't empty on a stable run ---
        if (secondsLogged % 30 == 0) {
            writeLine("[" + LocalDateTime.now().format(FMT) + "] SUMMARY"
                    + " fps=" + s.fps
                    + " heap=" + mb(s.heapUsed) + "MB"
                    + " heap_growth=" + heapGrowthKb + "KB/s"
                    + " worst=" + s.worstFrameNs / 1_000_000 + "ms"
                    + " gcTotal=" + s.gcCount + "x/" + s.gcTimeMs + "ms"
                    + " upd=" + s.updateTimeNs / 1000 + "us"
                    + " draw=" + s.drawTimeNs / 1000 + "us"
                    + " show=" + s.showTimeNs / 1000 + "us"
                    + " keys=[" + s.heldKeys + "]"
                    + " kps=" + s.keyEventsPerSec);
        }
    }
    

    @Override
    public void close() {
        writeLine("=== PerfLogger stopped after " + secondsLogged + "s ===");
        queue.offer(POISON);  // signal writer thread to stop
        try { writerThread.join(2000); } catch (InterruptedException ignored) {}
        if (writer != null) try { writer.close(); } catch (IOException ignored) {}
    }

    // Game loop thread just offers to the queue — never blocks on disk
    private void writeLine(String line) {
        queue.offer(line);  // drops silently if queue full (256 lines buffer)
    }

    private static long mb(long bytes) {
        return bytes / (1024 * 1024);
    }
}
