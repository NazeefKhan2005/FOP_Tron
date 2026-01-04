package com.foptron.game.entity;

import com.foptron.game.data.EnemyDefinition;

public final class EnemyCycle extends Cycle {

    private final EnemyDefinition def;

    public EnemyCycle(String id, EnemyDefinition def) {
        super(id, CycleType.ENEMY, def.displayName(), def.color());
        this.def = def;

        this.baseSpeed = def.speed();
        this.handling = def.handling();
        this.lives = 1.0;
        this.discSlots = 1;
        this.level = 1;
        this.xp = 0;
    }

    public EnemyDefinition definition() {
        return def;
    }

    public double aggression() {
        return def.aggression();
    }

    @Override
    public void tryLevelUp() {
        // Enemies do not level in this assignment baseline.
    }

    @Override
    public long xpForNextLevel() {
        return Long.MAX_VALUE;
    }
}
