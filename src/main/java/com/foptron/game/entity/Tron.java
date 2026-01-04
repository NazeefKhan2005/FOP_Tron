package com.foptron.game.entity;

import com.foptron.game.data.CharacterDefinition;

public final class Tron extends PlayerCycle {

    public Tron(String id, CharacterDefinition def) {
        super(id, PlayerCharacterId.TRON, def);
    }

    @Override
    protected void onLevelUp() {
        // Tron: speed increases gradually every level.
        baseSpeed += 0.030;
        handling += 0.010;
    }
}
