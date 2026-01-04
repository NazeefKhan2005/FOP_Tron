package com.foptron.game.data;

public record CharacterDefinition(
        String id,
        String displayName,
        String color,
        double speed,
        double handling,
        double lives,
        int discsOwned,
        long experiencePoints
) {
}
