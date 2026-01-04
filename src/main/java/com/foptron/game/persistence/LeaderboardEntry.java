package com.foptron.game.persistence;

import java.time.LocalDate;

public record LeaderboardEntry(
        String playerName,
        int highestLevel,
        long totalScore,
        LocalDate date
) {
}
