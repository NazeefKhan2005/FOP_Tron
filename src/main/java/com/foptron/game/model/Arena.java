package com.foptron.game.model;

public final class Arena {
    public static final int SIZE = 40;

    private final String id;
    private final String name;
    private final boolean open;
    private final CellType[][] grid;

    public Arena(String id, String name, boolean open, CellType[][] grid) {
        this.id = id;
        this.name = name;
        this.open = open;
        this.grid = grid;
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public boolean isOpen() {
        return open;
    }

    public boolean inBounds(Pos pos) {
        return pos.x >= 0 && pos.x < SIZE && pos.y >= 0 && pos.y < SIZE;
    }

    public CellType cellAt(Pos pos) {
        return grid[pos.y][pos.x];
    }

    public boolean isSolid(Pos pos) {
        CellType cell = cellAt(pos);
        return cell == CellType.WALL || cell == CellType.OBSTACLE;
    }

    public boolean isRamp(Pos pos) {
        return cellAt(pos) == CellType.RAMP;
    }

    public CellType[][] grid() {
        return grid;
    }
}
