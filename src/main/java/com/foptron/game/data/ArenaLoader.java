package com.foptron.game.data;

import com.foptron.game.model.Arena;
import com.foptron.game.model.CellType;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Random;

public final class ArenaLoader {
    private ArenaLoader() {}

    public static Arena load(String fallbackId, String classpathResource) {
        try (var in = ArenaLoader.class.getClassLoader().getResourceAsStream(classpathResource)) {
            if (in == null) {
                throw new IllegalStateException("Missing arena resource: " + classpathResource);
            }
            try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String first = br.readLine();
                String id = fallbackId;
                String name = fallbackId;
                boolean open = false;

                if (first == null || !first.startsWith("#ARENA")) {
                    throw new IllegalStateException("Arena file must start with #ARENA header: " + classpathResource);
                }

                id = attr(first, "id", fallbackId);
                name = attr(first, "name", fallbackId);
                open = Boolean.parseBoolean(attr(first, "open", "false"));

                CellType[][] grid = new CellType[Arena.SIZE][Arena.SIZE];
                for (int y = 0; y < Arena.SIZE; y++) {
                    String line = br.readLine();
                    if (line == null) {
                        throw new IllegalStateException("Arena grid too short: " + classpathResource);
                    }
                    if (line.length() < Arena.SIZE) {
                        throw new IllegalStateException("Arena line too short at y=" + y + ": " + classpathResource);
                    }
                    for (int x = 0; x < Arena.SIZE; x++) {
                        grid[y][x] = CellType.fromSymbol(line.charAt(x));
                    }
                }

                return new Arena(id, name, open, grid);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load arena: " + classpathResource, e);
        }
    }

    public static Arena random(String id, String name, boolean open, Random rng) {
        Objects.requireNonNull(rng, "rng");
        CellType[][] grid = new CellType[Arena.SIZE][Arena.SIZE];

        for (int y = 0; y < Arena.SIZE; y++) {
            for (int x = 0; x < Arena.SIZE; x++) {
                boolean boundary = (x == 0 || y == 0 || x == Arena.SIZE - 1 || y == Arena.SIZE - 1);
                if (!open && boundary) {
                    grid[y][x] = CellType.WALL;
                    continue;
                }

                double r = rng.nextDouble();
                if (r < 0.05) {
                    grid[y][x] = CellType.OBSTACLE;
                } else if (r < 0.07) {
                    grid[y][x] = CellType.RAMP;
                } else {
                    grid[y][x] = CellType.EMPTY;
                }
            }
        }

        // Carve a central safe zone.
        for (int y = 16; y <= 23; y++) {
            for (int x = 16; x <= 23; x++) {
                grid[y][x] = CellType.EMPTY;
            }
        }

        return new Arena(id, name, open, grid);
    }

    private static String attr(String header, String key, String defaultValue) {
        String[] parts = header.replace("#ARENA", "").trim().split(" ");
        for (String p : parts) {
            if (p.startsWith(key + "=")) {
                return p.substring((key + "=").length()).replace('"', ' ').trim();
            }
        }
        return defaultValue;
    }
}
