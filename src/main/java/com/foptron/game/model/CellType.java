package com.foptron.game.model;

public enum CellType {
    EMPTY('.'),
    WALL('#'),
    OBSTACLE('X'),
    RAMP('R');

    public final char symbol;

    CellType(char symbol) {
        this.symbol = symbol;
    }

    public static CellType fromSymbol(char c) {
        for (CellType t : values()) {
            if (t.symbol == c) return t;
        }
        return EMPTY;
    }
}
