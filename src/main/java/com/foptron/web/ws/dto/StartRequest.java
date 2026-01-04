package com.foptron.web.ws.dto;

public record StartRequest(
        String playerName,
        String characterId,
        String arenaId,
        Boolean manualStep
) {
}
