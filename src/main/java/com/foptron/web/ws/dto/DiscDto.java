package com.foptron.web.ws.dto;

public record DiscDto(
        String id,
        String ownerColor,
        int x,
        int y,
        boolean flying
) {
}
