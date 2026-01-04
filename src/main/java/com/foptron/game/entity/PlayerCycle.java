package com.foptron.game.entity;

import com.foptron.game.data.CharacterDefinition;

public abstract class PlayerCycle extends Cycle {

    private final PlayerCharacterId characterId;
    private final CharacterDefinition def;

    private final double baseSpeedAtLevel1;
    private final double handlingAtLevel1;
    private final double maxLivesAtLevel1;
    private final int discSlotsAtLevel1;

    private long xpForNextLevel;

    protected PlayerCycle(String id, PlayerCharacterId characterId, CharacterDefinition def) {
        super(id, CycleType.PLAYER, def.displayName(), def.color());
        this.characterId = characterId;
        this.def = def;

        this.baseSpeed = def.speed();
        this.handling = def.handling();
        this.lives = def.lives();
        this.maxLives = def.lives();
        this.discSlots = def.discsOwned();
        this.xp = def.experiencePoints();
        this.level = 1;

        this.baseSpeedAtLevel1 = def.speed();
        this.handlingAtLevel1 = def.handling();
        this.maxLivesAtLevel1 = def.lives();
        this.discSlotsAtLevel1 = def.discsOwned();
    }

    public PlayerCharacterId characterId() {
        return characterId;
    }

    public CharacterDefinition definition() {
        return def;
    }

    public void setXpForNextLevel(long xpForNextLevel) {
        this.xpForNextLevel = Math.max(1, xpForNextLevel);
    }

    @Override
    public long xpForNextLevel() {
        if (level >= 99) return Long.MAX_VALUE;
        return Math.max(1, xpForNextLevel);
    }

    @Override
    public final void tryLevelUp() {
        while (level < 99 && xp >= xpForNextLevel()) {
            xp -= xpForNextLevel();
            level++;

            onLevelUp();

            if (level % 10 == 0) {
                increaseMaxLives(1.0);
            }

            if (level % 15 == 0) {
                discSlots += 1;
            }
        }
    }

    protected abstract void onLevelUp();

    public void restoreProgress(int targetLevel, long targetXp) {
        int clampedLevel = Math.max(1, Math.min(99, targetLevel));

        // Reset to level 1 baseline.
        this.baseSpeed = baseSpeedAtLevel1;
        this.handling = handlingAtLevel1;
        this.maxLives = maxLivesAtLevel1;
        this.lives = maxLivesAtLevel1;
        this.discSlots = discSlotsAtLevel1;
        this.activeDiscs = 0;
        this.discCooldownRemainingMs = 0;
        this.turnCooldownRemainingMs = 0;
        this.discCooldownReductionMs = 0;

        this.level = 1;
        this.xp = 0;

        // Apply level-up effects up to target.
        for (int i = 2; i <= clampedLevel; i++) {
            level = i;
            onLevelUp();
            if (level % 10 == 0) {
                // Keep maxLives consistent.
                maxLives += 1.0;
            }
            if (level % 15 == 0) {
                discSlots += 1;
            }
        }

        // After rebuilding, restore lives to max.
        this.lives = this.maxLives;
        this.xp = Math.max(0, targetXp);
    }
}
