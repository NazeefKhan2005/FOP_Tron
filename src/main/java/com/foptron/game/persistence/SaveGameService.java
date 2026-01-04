package com.foptron.game.persistence;

import com.foptron.game.engine.GameSession;
import com.foptron.game.entity.PlayerCycle;
import com.foptron.web.ws.dto.StartRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

@Service
public class SaveGameService {

    private final ObjectMapper mapper;

    public SaveGameService(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public Path saveDir() {
        return Path.of("saves");
    }

    public SaveGameData toSaveData(GameSession session) {
        PlayerCycle p = session.player();
        String characterId = p.characterId().name();
        return new SaveGameData(
                session.playerName(),
                characterId,
                p.level(),
                p.xp(),
                List.copyOf(session.achievements()),
                Instant.now()
        );
    }

    public void save(GameSession session) {
        try {
            Files.createDirectories(saveDir());
            SaveGameData data = toSaveData(session);
            Path file = saveDir().resolve(safeFileName(data.playerName()) + ".json");
            mapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), data);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to save game", e);
        }
    }

    public SaveGameData load(String playerName) {
        try {
            Path file = saveDir().resolve(safeFileName(playerName) + ".json");
            if (!Files.exists(file)) {
                return null;
            }
            return mapper.readValue(file.toFile(), SaveGameData.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load save", e);
        }
    }

    public StartRequest toStartRequest(SaveGameData save, String arenaId) {
        return new StartRequest(save.playerName(), save.characterId(), arenaId, false);
    }

    private static String safeFileName(String s) {
        if (s == null || s.isBlank()) return "Player";
        return s.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
