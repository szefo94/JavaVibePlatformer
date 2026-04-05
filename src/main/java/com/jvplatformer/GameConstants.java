package com.jvplatformer;

import java.awt.Color;
import java.awt.Font;

public final class GameConstants {
    private GameConstants() {}

    // Window
    public static final int SCREEN_WIDTH  = 854;
    public static final int SCREEN_HEIGHT = 480;
    public static final int TARGET_UPS    = 60;

    // Tile
    public static final int TILE_SIZE = 48;

    // Physics
    public static final double GRAVITY        = 1800.0;
    public static final double MAX_FALL_SPEED = 900.0;
    public static final double JUMP_VELOCITY  = -720.0;

    // Player movement
    public static final double MAX_WALK_SPEED   = 320.0;
    public static final double MOVE_FORCE       = 60.0;
    public static final double GROUND_FRICTION  = 0.75;
    public static final double AIR_FRICTION     = 0.90;

    // Player size
    public static final int PLAYER_WIDTH  = 36;
    public static final int PLAYER_HEIGHT = 36;

    // Camera
    public static final double CAMERA_LERP        = 0.18;
    public static final double CAMERA_LEAD_FACTOR = 3.0; // player at 1/LEAD_FACTOR from left

    // Colors (static final to avoid GC pressure)
    public static final Color COLOR_SKY_TOP    = new Color(100, 150, 220);
    public static final Color COLOR_SKY_BOT    = new Color(180, 210, 255);
    public static final Color COLOR_PLATFORM   = new Color(80,  160,  60);
    public static final Color COLOR_PLATFORM_B = new Color(120,  80,  40);
    public static final Color COLOR_PLAYER     = new Color(255, 200,  60);
    public static final Color COLOR_PLAYER_EYE = new Color(50,   50,  50);
    public static final Color COLOR_HUD        = Color.WHITE;

    // Coin
    public static final Color COLOR_COIN       = new Color(255, 215,   0);
    public static final Color COLOR_COIN_RING  = new Color(200, 160,   0);
    public static final Color COLOR_COIN_SHINE = new Color(255, 255, 180);

    // Level goal (exit door)
    public static final Color COLOR_GOAL_FRAME  = new Color( 60,  60, 200);
    public static final Color COLOR_GOAL_INNER  = new Color( 30, 180, 255);
    public static final Color COLOR_GOAL_SYMBOL = Color.WHITE;

    // Level complete overlay
    public static final Color COLOR_COMPLETE_BG   = new Color(0, 0, 0, 180);
    public static final Color COLOR_COMPLETE_TEXT  = new Color(255, 230, 50);
    public static final Color COLOR_COMPLETE_SUB   = Color.WHITE;

    // Fonts
    public static final Font HUD_FONT      = new Font("Monospaced", Font.BOLD, 14);
    public static final Font COMPLETE_FONT = new Font("Monospaced", Font.BOLD, 48);
    public static final Font COMPLETE_SUB  = new Font("Monospaced", Font.PLAIN, 22);
}
