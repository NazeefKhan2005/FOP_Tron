package com.foptron.game.entity;

import com.foptron.game.data.CharacterDefinition;

public final class Kevin extends PlayerCycle {

    public Kevin(String id, CharacterDefinition def) {
        super(id, PlayerCharacterId.KEVIN, def);
    }

    @Override
    protected void onLevelUp() {
        // Kevin: slightly improves control, and disc cooldown decreases each level.
        handling += 0.020;
        discCooldownReductionMs += 35;
    }
}
