package com.foptron.web.ws.dto;

import java.util.List;

public record GameStateDto(
        int size,
        String arenaName,
        boolean openArena,
        List<String> arena,
        PlayerDto player,
        List<EnemyDto> enemies,
        List<DiscDto> discs,
        List<TrailCellDto> trails,
        List<String> events,
        StoryDto story,
        List<String> achievements,
        boolean awaitingChoice,
        boolean running,
        boolean victory
) {
}
