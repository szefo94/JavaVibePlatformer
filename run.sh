#!/bin/bash
set -e
cd "$(dirname "$0")"

# PERFORMANCE TIP: Set CPU governor to performance mode for stable frame pacing.
# Without this the Pi's ondemand governor starts at 600 MHz and ramps up slowly,
# causing the first few seconds to stutter and the busy-spin to overshoot.
#   sudo cpupower frequency-set -g performance
#   (reset with: sudo cpupower frequency-set -g ondemand)

JAR="target/JavaVibePlatformer-1.0-SNAPSHOT-jar-with-dependencies.jar"

if [ ! -f "$JAR" ]; then
    echo "JAR not found. Building first..."
    mvn package -q
fi

# Common JVM flags for both modes:
#   UseZGC                 — zero concurrent-phase interference vs G1's background threads
#   Xmx128m               — more headroom so GC fires half as often (~14s vs ~7s interval)
#   UseThreadPriorities    — map Java thread priorities to OS priorities (game loop = MAX)
#   ThreadPriorityPolicy=1 — on Linux, allow priorities above normal without root
#   ReservedCodeCacheSize  — pre-allocate JIT code cache; prevents post-GC recompilation waves
#   TieredCompilation      — C1 → C2 upgrade; CompileThreshold=100 triggers early
JVM_FLAGS=(
    -server
    -Xms48m -Xmx128m
    -XX:+UseZGC
    -XX:+UseThreadPriorities
    -XX:ThreadPriorityPolicy=1
    -XX:ReservedCodeCacheSize=32m
    -XX:+TieredCompilation
    -XX:CompileThreshold=100
)

if [ "$1" = "--fb" ]; then
    # Framebuffer mode: bypasses X11 entirely, writes directly to /dev/fb0
    # Requirements:
    #   - Run from a TTY, not inside desktop (Ctrl+Alt+F2 to switch)
    #   - User must be in 'video' group (for /dev/fb0)
    #   - User must be in 'input' group (for /dev/input/event*)
    if [ -n "$DISPLAY" ] || [ -n "$WAYLAND_DISPLAY" ]; then
        echo "WARNING: You appear to be running inside a desktop session."
        echo "Framebuffer mode works best from a TTY (press Ctrl+Alt+F2)."
        echo "The game may draw behind the compositor. Press Enter to continue anyway, or Ctrl+C to cancel."
        read -r
    fi

    exec java \
        "${JVM_FLAGS[@]}" \
        -Djava.awt.headless=true \
        -jar "$JAR" --fb
else
    # AWT/Swing mode: normal desktop window
    exec java \
        "${JVM_FLAGS[@]}" \
        -Dsun.java2d.xrender=true \
        -jar "$JAR"
fi
