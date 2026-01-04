package com.foptron.game.engine;

import com.foptron.game.data.CharacterDefinition;
import com.foptron.game.data.DataRepository;
import com.foptron.game.entity.PlayerCycle;
import com.foptron.game.entity.Kevin;
import com.foptron.game.entity.Tron;
import com.foptron.game.persistence.LeaderboardService;
import com.foptron.game.persistence.SaveGameData;
import com.foptron.game.model.Direction;
import com.foptron.web.ws.dto.InputMessage;
import com.foptron.web.ws.dto.ChoiceMessage;
import com.foptron.web.ws.dto.StartRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
@SuppressWarnings("Nullness")
public class GameSessionManager {

    private final DataRepository data;
    private final SimpMessagingTemplate messaging;
    private final LeaderboardService leaderboard;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    private volatile GameSession session;
    private volatile boolean leaderboardRecorded;

    public GameSessionManager(DataRepository data, SimpMessagingTemplate messaging, LeaderboardService leaderboard) {
        this.data = data;
        this.messaging = messaging;
        this.leaderboard = leaderboard;

        scheduler.scheduleAtFixedRate(this::tickAndBroadcast, 0, 100, TimeUnit.MILLISECONDS);
    }

    public synchronized void start(StartRequest req) {
        String characterKey = (req.characterId() == null ? "TRON" : req.characterId()).toUpperCase();
        String arenaMode = (req.arenaId() == null || req.arenaId().isBlank() ? "AUTO" : req.arenaId()).toUpperCase();
        boolean manualStep = Boolean.TRUE.equals(req.manualStep());

        CharacterDefinition def = data.characters().get(characterKey);
        if (def == null) {
            def = data.characters().values().stream().findFirst().orElseThrow();
        }

        PlayerCycle player = switch (characterKey) {
            case "KEVIN" -> new Kevin("P1", def);
            default -> new Tron("P1", def);
        };

        session = new GameSession(
                "S1",
                req.playerName(),
                arenaMode,
                player,
                data.arenas(),
                data.enemies(),
                data.story(),
            data.achievements(),
            manualStep
        );
        leaderboardRecorded = false;
        broadcast(session);
    }

    public GameSession currentSession() {
        return session;
    }

    public synchronized void startFromSave(SaveGameData save, boolean manualStep) {
        Objects.requireNonNull(save, "save");

        String characterKey = (save.characterId() == null ? "TRON" : save.characterId()).toUpperCase();
        CharacterDefinition def = data.characters().get(characterKey);
        if (def == null) {
            def = data.characters().values().stream().findFirst().orElseThrow();
        }

        PlayerCycle player = switch (characterKey) {
            case "KEVIN" -> new Kevin("P1", def);
            default -> new Tron("P1", def);
        };

        GameSession newSession = new GameSession(
                "S1",
                save.playerName(),
                "AUTO",
                player,
                data.arenas(),
                data.enemies(),
                data.story(),
            data.achievements(),
            manualStep
        );

        newSession.restoreFromSave(save.level(), save.xp(), save.achievements());
        session = newSession;
        leaderboardRecorded = false;
        broadcast(session);
    }

    public synchronized void startFromSave(SaveGameData save) {
        startFromSave(save, false);
    }

    public void input(InputMessage msg) {
        GameSession s = session;
        if (s == null) return;

        Direction dir = null;
        if (msg.direction() != null && !msg.direction().isBlank()) {
            try {
                dir = Direction.valueOf(msg.direction().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                dir = null;
            }
        }

        boolean manual = Boolean.TRUE.equals(msg.manualStep()) || s.isManualStepMode();
        if (manual) {
            // In manual mode: one keypress = one step. Player only moves if a direction is provided.
            if (dir == null && !msg.throwDisc()) {
                return;
            }
            s.manualStep(dir, msg.throwDisc());
        } else {
            s.applyInput(dir, msg.throwDisc());
        }

        maybeRecordLeaderboard(s);
        broadcast(s);
    }

    public void choice(ChoiceMessage msg) {
        GameSession s = session;
        if (s == null) return;
        s.applyChoice(msg.option());

        maybeRecordLeaderboard(s);
        broadcast(s);
    }

    private void tickAndBroadcast() {
        GameSession s = session;
        if (s == null) return;

        if (s.isManualStepMode()) {
            return;
        }

        s.tick();

        maybeRecordLeaderboard(s);
        broadcast(s);
    }

    private void maybeRecordLeaderboard(GameSession s) {
        if (!s.isRunning() && !leaderboardRecorded) {
            leaderboard.record(s.playerName(), s.highestLevelAchieved(), s.totalScore());
            leaderboardRecorded = true;
        }
    }

    private void broadcast(GameSession s) {
        Object payload = Objects.requireNonNull(GameStateMapper.toDto(s), "state");
        messaging.convertAndSend("/topic/state", payload);
    }

    // Spawning and level flow are managed by GameSession.
}
