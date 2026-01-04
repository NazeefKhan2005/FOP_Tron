package com.foptron.game.persistence;

import java.time.Instant;
import java.util.List;

public record SaveGameData(
        String playerName,
        String characterId,
        int level,
        long xp,
        List<String> achievements,
        Instant savedAt
) {
}
