package com.foptron.game.data;

import com.foptron.game.model.Arena;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ArenaLoaderTest {

    @Test
    void predefinedArenasAre40x40() {
        Arena a1 = ArenaLoader.load("ARENA1", "data/arenas/arena1.txt");
        Arena a2 = ArenaLoader.load("ARENA2", "data/arenas/arena2.txt");
        Arena a3 = ArenaLoader.load("ARENA3", "data/arenas/arena3.txt");

        assertEquals(40, a1.grid().length);
        assertEquals(40, a2.grid().length);
        assertEquals(40, a3.grid().length);

        assertEquals(40, a1.grid()[0].length);
        assertEquals(40, a2.grid()[0].length);
        assertEquals(40, a3.grid()[0].length);
    }
}
