package com.foptron.web.api;

import com.foptron.game.engine.GameSession;
import com.foptron.game.engine.GameSessionManager;
import com.foptron.game.persistence.LeaderboardEntry;
import com.foptron.game.persistence.LeaderboardService;
import com.foptron.game.persistence.SaveGameData;
import com.foptron.game.persistence.SaveGameService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class PersistenceController {

    private final GameSessionManager manager;
    private final SaveGameService saveGame;
    private final LeaderboardService leaderboard;

    public PersistenceController(GameSessionManager manager, SaveGameService saveGame, LeaderboardService leaderboard) {
        this.manager = manager;
        this.saveGame = saveGame;
        this.leaderboard = leaderboard;
    }

    @PostMapping("/api/save")
    public Map<String, Object> save() {
        GameSession s = manager.currentSession();
        if (s == null) {
            return Map.of("saved", false, "message", "No active session");
        }
        saveGame.save(s);
        return Map.of("saved", true);
    }

    @GetMapping("/api/load")
    public SaveGameData load(@RequestParam String playerName) {
        return saveGame.load(playerName);
    }

    @PostMapping("/api/load/start")
    public Map<String, Object> loadAndStart(@RequestParam String playerName, @RequestParam(required = false) Boolean manualStep) {
        SaveGameData save = saveGame.load(playerName);
        if (save == null) {
            return Map.of("started", false, "message", "No save found for playerName='" + playerName + "'");
        }

        manager.startFromSave(save, Boolean.TRUE.equals(manualStep));
        return Map.of("started", true);
    }

    @GetMapping("/api/leaderboard")
    public List<LeaderboardEntry> leaderboard() {
        return leaderboard.top10();
    }
}
