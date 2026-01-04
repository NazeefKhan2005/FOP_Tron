package com.foptron.web.ws.dto;

public record InputMessage(
        String direction,
        boolean throwDisc,
        Boolean manualStep
) {
}
