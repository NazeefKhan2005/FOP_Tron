package com.foptron.web.ws.dto;

public record EnemyDto(
        String id,
        String name,
        String color,
        int x,
        int y,
        String direction,
        double lives,
        int discSlots,
        int activeDiscs,
        String type
) {
}
