package com.jvplatformer.level;

import com.jvplatformer.entity.Coin;
import com.jvplatformer.entity.LevelGoal;
import com.jvplatformer.entity.Platform;
import com.jvplatformer.entity.Player;

import java.util.List;

public class Level {
    public final List<Platform> platforms;
    public final List<Coin>     coins;
    public final LevelGoal      goal;        // may be null if level has no exit yet
    public final double         spawnX;
    public final double         spawnY;
    public final int            widthPx;
    public final int            heightPx;

    public Level(List<Platform> platforms, List<Coin> coins, LevelGoal goal,
                 double spawnX, double spawnY, int widthPx, int heightPx) {
        this.platforms = platforms;
        this.coins     = coins;
        this.goal      = goal;
        this.spawnX    = spawnX;
        this.spawnY    = spawnY;
        this.widthPx   = widthPx;
        this.heightPx  = heightPx;
    }

    public Player createPlayer() {
        return new Player(spawnX, spawnY);
    }
}
