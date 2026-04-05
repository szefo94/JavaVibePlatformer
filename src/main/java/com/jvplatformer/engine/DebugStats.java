package com.jvplatformer.engine;

/** Mutable snapshot of per-frame performance data. Populated by GamePanel each tick. */
public class DebugStats {
    public long fps;

    // Loop timing (nanoseconds, converted to us for display)
    public long updateTimeNs;
    public long renderTimeNs;

    // Memory (bytes, converted to MB for display)
    public long heapUsed;
    public long heapCommitted;
    public long heapMax;

    // GC
    public long gcCount;
    public long gcTimeMs;

    // Entities
    public int dynamicCount;
    public int platformCount;
    public int coinTotal;
    public int coinCollected;

    // Player state
    public double playerX;
    public double playerY;
    public double playerVelX;
    public double playerVelY;
    public boolean playerOnGround;

    // Camera
    public int cameraOffsetX;
    public int cameraOffsetY;

    // Render sub-timings (nanoseconds)
    public long drawTimeNs;   // actual Graphics2D calls
    public long showTimeNs;   // bs.show()
    public long syncTimeNs;   // Toolkit.sync()

    // Frame jitter — worst single frame in the last second
    public long worstFrameNs;

    // Input
    public int    keyEventsPerSec;
    public String heldKeys = "none";
}
