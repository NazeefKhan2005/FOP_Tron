package com.foptron.web.ws.dto;

public record PlayerDto(
        String playerName,
        String characterName,
        String color,
        int x,
        int y,
        String direction,
        double lives,
        int level,
        long xp,
        long xpForNextLevel,
        int discSlots,
        int activeDiscs
) {
}
