package com.foptron.game.data;

public record EnemyDefinition(
        String id,
        String displayName,
        String color,
        String difficulty,
        long xpReward,
        double speed,
        double handling,
        double aggression,
        String intelligence
) {
}
