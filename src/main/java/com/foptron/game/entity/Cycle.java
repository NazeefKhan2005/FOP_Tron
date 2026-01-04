package com.foptron.game.entity;

import com.foptron.game.model.Direction;
import com.foptron.game.model.Pos;

import java.util.HashSet;
import java.util.Set;

public abstract class Cycle {

    private final String id;
    private final CycleType type;
    private final String displayName;
    private final String color;

    protected double baseSpeed;
    protected double handling;
    protected double lives;
    protected double maxLives;
    protected int discSlots;

    protected int level;
    protected long xp;

    protected Pos pos;
    protected Direction dir;

    protected final Set<Pos> trail = new HashSet<>();

    private double moveAccumulator;
    protected long discCooldownRemainingMs;
    protected long discCooldownReductionMs;
    protected long turnCooldownRemainingMs;
    protected long respawnProtectionMs;

    protected int activeDiscs;

    protected Cycle(String id, CycleType type, String displayName, String color) {
        this.id = id;
        this.type = type;
        this.displayName = displayName;
        this.color = color;
    }

    public String id() {
        return id;
    }

    public CycleType type() {
        return type;
    }

    public String displayName() {
        return displayName;
    }

    public String color() {
        return color;
    }

    public double speed() {
        return baseSpeed;
    }

    public double handling() {
        return handling;
    }

    public double lives() {
        return lives;
    }

    public double maxLives() {
        return maxLives;
    }

    public int discSlots() {
        return discSlots;
    }

    public int level() {
        return level;
    }

    public long xp() {
        return xp;
    }

    public Pos pos() {
        return pos;
    }

    public Direction dir() {
        return dir;
    }

    public Set<Pos> trail() {
        return trail;
    }

    public boolean isAlive() {
        return lives > 0;
    }

    public boolean canTurn() {
        return turnCooldownRemainingMs <= 0;
    }

    public boolean canThrowDisc() {
        return discCooldownRemainingMs <= 0 && activeDiscs < discSlots;
    }

    public void onDiscThrown() {
        activeDiscs++;
        discCooldownRemainingMs = Math.max(1500, computeDiscCooldownMs());
    }

    protected long computeDiscCooldownMs() {
        // Higher handling => shorter cooldown.
        long cooldown = (long) (5000 - (handling * 800));
        cooldown -= Math.max(0, discCooldownReductionMs);
        return cooldown;
    }

    public void onDiscRecaptured() {
        if (activeDiscs > 0) activeDiscs--;
    }

    public int activeDiscs() {
        return activeDiscs;
    }

    public void resetDiscState() {
        activeDiscs = 0;
        discCooldownRemainingMs = 0;
    }

    public void applyDamage(double amount) {
        lives = Math.max(0, lives - amount);
    }

    public void restoreLivesToMax() {
        lives = maxLives;
    }

    protected void increaseMaxLives(double amount) {
        if (amount <= 0) return;
        maxLives += amount;
        lives += amount;
    }

    public void setPosition(Pos pos, Direction dir) {
        this.pos = pos;
        this.dir = dir;
    }

    public void tickTimers(long dtMs) {
        discCooldownRemainingMs = Math.max(0, discCooldownRemainingMs - dtMs);
        turnCooldownRemainingMs = Math.max(0, turnCooldownRemainingMs - dtMs);
        respawnProtectionMs = Math.max(0, respawnProtectionMs - dtMs);
    }

    public void addMoveProgress(long dtMs) {
        moveAccumulator += speed() * (dtMs / 1000.0);
    }

    public boolean hasMoveStep() {
        return moveAccumulator >= 1.0;
    }

    public void consumeMoveStep() {
        moveAccumulator = Math.max(0, moveAccumulator - 1.0);
    }

    public void applyRampBoost(double delta, double maxSpeed) {
        baseSpeed = Math.min(maxSpeed, baseSpeed + delta);
    }

    public boolean isRespawnProtected() {
        return respawnProtectionMs > 0;
    }

    public void giveRespawnProtection(long ms) {
        respawnProtectionMs = ms;
    }

    public void setTurnCooldown(long ms) {
        turnCooldownRemainingMs = ms;
    }

    public void addXp(long amount) {
        xp = Math.max(0, xp + amount);
    }

    public abstract void tryLevelUp();

    public abstract long xpForNextLevel();
}
