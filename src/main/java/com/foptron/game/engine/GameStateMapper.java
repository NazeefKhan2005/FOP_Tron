package com.foptron.game.engine;

import com.foptron.game.combat.Disc;
import com.foptron.game.entity.EnemyCycle;
import com.foptron.game.model.Arena;
import com.foptron.game.model.CellType;
import com.foptron.game.model.Pos;
import com.foptron.web.ws.dto.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class GameStateMapper {
    private GameStateMapper() {}

    public static GameStateDto toDto(GameSession session) {
        Arena arena = session.arena();

        List<String> arenaRows = new ArrayList<>(Arena.SIZE);
        CellType[][] g = arena.grid();
        for (int y = 0; y < Arena.SIZE; y++) {
            StringBuilder sb = new StringBuilder(Arena.SIZE);
            for (int x = 0; x < Arena.SIZE; x++) {
                sb.append(g[y][x].symbol);
            }
            arenaRows.add(sb.toString());
        }

        var player = session.player();
        PlayerDto playerDto = new PlayerDto(
            session.playerName(),
            player.displayName(),
                player.color(),
                player.pos().x,
                player.pos().y,
                player.dir().name(),
                player.lives(),
                player.level(),
                player.xp(),
                player.xpForNextLevel(),
                player.discSlots(),
                player.activeDiscs()
        );

        List<EnemyDto> enemies = new ArrayList<>();
        for (EnemyCycle e : session.enemies()) {
            enemies.add(new EnemyDto(
                    e.id(),
                    e.displayName(),
                    e.color(),
                    e.pos().x,
                    e.pos().y,
                    e.dir().name(),
                    e.lives(),
                    e.discSlots(),
                    e.activeDiscs(),
                    e.definition().id()
            ));
        }

        List<DiscDto> discs = new ArrayList<>();
        for (Disc d : session.discs()) {
            discs.add(new DiscDto(d.id(), d.ownerColor(), d.pos().x, d.pos().y, d.isFlying()));
        }

        List<TrailCellDto> trails = new ArrayList<>();
        for (Map.Entry<Pos, String> e : session.trailColors().entrySet()) {
            trails.add(new TrailCellDto(e.getKey().x, e.getKey().y, e.getValue()));
        }

        StoryDto story = null;
        if (session.currentStory() != null) {
            story = new StoryDto(session.currentStory().id(), session.currentStory().title(), session.currentStory().text());
        }

        return new GameStateDto(
                Arena.SIZE,
                arena.name(),
                arena.isOpen(),
            arenaRows,
                playerDto,
                enemies,
                discs,
                trails,
                List.copyOf(session.events()),
                story,
                List.copyOf(session.achievements()),
            session.isAwaitingEndingChoice(),
                session.isRunning(),
                session.isVictory()
        );
    }
}
