package com.foptron.game.combat;

import com.foptron.game.model.Direction;
import com.foptron.game.model.Pos;

public final class Disc {
    private final String id;
    private final String ownerCycleId;
    private final String ownerColor;

    private Pos pos;
    private Direction dir;
    private int remainingRange;

    private boolean flying;

    public Disc(String id, String ownerCycleId, String ownerColor, Pos start, Direction dir, int range) {
        this.id = id;
        this.ownerCycleId = ownerCycleId;
        this.ownerColor = ownerColor;
        this.pos = start;
        this.dir = dir;
        this.remainingRange = range;
        this.flying = true;
    }

    public String id() {
        return id;
    }

    public String ownerCycleId() {
        return ownerCycleId;
    }

    public String ownerColor() {
        return ownerColor;
    }

    public Pos pos() {
        return pos;
    }

    public void setPos(Pos pos) {
        this.pos = pos;
    }

    public Direction dir() {
        return dir;
    }

    public int remainingRange() {
        return remainingRange;
    }

    public void decRange() {
        remainingRange = Math.max(0, remainingRange - 1);
    }

    public boolean isFlying() {
        return flying;
    }

    public void land() {
        flying = false;
    }
}
