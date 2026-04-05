# JavaVibePlatformer

A lightweight 2D side-scrolling platformer written in pure Java, built to run smoothly on a **Raspberry Pi 5**. No game engine, no external graphics libraries — just Java AWT and direct Linux framebuffer rendering.

## What is this game?

You control a rolling ball through a platformer level. Collect all coins, then reach the exit door to complete the level.

- **Ball rotation** is physically animated — it rolls based on your actual horizontal and vertical velocity
- **Double jump** — you get two jumps before you must land again
- **Smooth camera** — follows you both horizontally and vertically with LERP easing
- **Coins** are only placed above platforms you can actually reach

---

## Controls

| Key | Action |
|-----|--------|
| `A` / `←` | Move left |
| `D` / `→` | Move right |
| `Space` / `W` / `↑` | Jump (press again in air for double jump) |
| `F3` | Toggle debug overlay (FPS, physics stats, heap, GC) |
| `ESC` | Quit |

---

## Requirements

- Java 17+ (OpenJDK recommended)
- Maven 3.6+
- Linux (Raspberry Pi OS, Ubuntu, Debian, etc.)

```bash
sudo apt install openjdk-17-jdk maven
```

---

## How to run

### 1. Clone the repository

```bash
git clone https://github.com/szefo94/JavaVibePlatformer.git
cd JavaVibePlatformer
```

### 2. Build

```bash
mvn package -q
```

### 3. Run — Desktop mode (X11/Wayland window)

```bash
./run.sh
```

This opens a normal desktop window at 854×480.

### 4. Run — Framebuffer mode (Raspberry Pi TTY, no desktop needed)

For best performance on Pi, bypass the desktop entirely and render directly to `/dev/fb0`:

```bash
# Switch to a TTY first (press Ctrl+Alt+F2 in desktop)
./run.sh --fb
```

Requirements for framebuffer mode:
- Must be run from a TTY, **not** inside a desktop session
- Your user must be in the `video` group: `sudo usermod -aG video $USER`
- Your user must be in the `input` group: `sudo usermod -aG input $USER`
- Log out and back in after adding groups

### 5. Optional — Fix CPU throttling on Pi (reduces frame jitter)

```bash
sudo cpupower frequency-set -g performance
# Reset after playing:
sudo cpupower frequency-set -g ondemand
```

---

## Running tests

```bash
mvn test
```

55 tests covering physics, player mechanics, camera, level loading, and coin behaviour.

---

## Project structure

```
src/main/java/com/jvplatformer/
├── Main.java                  # Entry point, selects AWT or framebuffer mode
├── GameConstants.java         # All tuning constants (speeds, colors, screen size)
├── GamePanel.java             # AWT game loop (desktop mode)
├── InputHandler.java          # Keyboard input via AWT KeyListener
├── IInputHandler.java         # Input interface shared by AWT and evdev backends
├── engine/
│   ├── Camera.java            # Smooth LERP follow camera (X and Y)
│   ├── DebugStats.java        # Stats snapshot passed to overlay and logger
│   └── PerfLogger.java        # Background-thread performance log (perf.log)
├── entity/
│   ├── Entity.java            # Abstract base: position, velocity, bounds
│   ├── Player.java            # Ball with rotation, double jump, LUT sin/cos
│   ├── Platform.java          # Static solid tile
│   ├── Coin.java              # Collectible with bobbing animation
│   └── LevelGoal.java         # Exit door
├── physics/
│   └── PhysicsEngine.java     # Gravity + two-pass AABB collision (float-based)
├── level/
│   ├── Level.java             # Level data bag
│   └── LevelLoader.java       # ASCII tile map parser
├── renderer/
│   ├── Renderer.java          # All Graphics2D draw calls
│   └── DebugOverlay.java      # F3 debug panel
└── fb/
    ├── FrameBuffer.java       # Memory-mapped /dev/fb0, RGB→RGB565 conversion
    ├── EvdevInput.java        # Raw keyboard input from /dev/input/eventX
    └── FbGameLoop.java        # Full game loop with no AWT dependency

src/main/resources/levels/
└── level1.txt                 # ASCII tile map ('#'=platform, 'P'=spawn, '*'=coin, 'E'=exit)
```

---

## Level format

Levels are plain text files in `src/main/resources/levels/`. Each character is a 48×48 pixel tile:

| Character | Meaning |
|-----------|---------|
| `#` | Platform (solid) |
| `P` | Player spawn point |
| `*` | Coin (only placed if a platform is within jump reach below) |
| `E` | Exit door (level goal) |
| `.` | Empty space |

---

## Performance notes

The game is tuned specifically for Raspberry Pi 5:

- **ZGC** garbage collector — sub-millisecond GC pauses
- **300-iteration JIT warmup** before gameplay starts
- **Hybrid sleep loop** — coarse `Thread.sleep` + busy-spin last 2ms for precise frame pacing
- **Sin/cos lookup tables** — avoids `Math.sin`/`Math.cos` on ARM every frame
- **Index-based loops** — no `Iterator` allocations in hot paths
- **Flat color rendering** — no gradients, no antialiasing, ovals replaced with rects where possible
- **Background perf logger** — file I/O never blocks the game loop thread

Performance data is written to `perf.log` during play. Open it after a session to see FPS, frame jitter, GC activity, and heap growth per second.
