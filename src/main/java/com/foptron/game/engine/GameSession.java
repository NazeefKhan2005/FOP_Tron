package com.foptron.game.engine;

import com.foptron.game.ai.Brains;
import com.foptron.game.ai.EnemyAction;
import com.foptron.game.ai.EnemyBrain;
import com.foptron.game.combat.Disc;
import com.foptron.game.data.AchievementDefinition;
import com.foptron.game.data.ArenaLoader;
import com.foptron.game.data.EnemyDefinition;
import com.foptron.game.data.StoryChapter;
import com.foptron.game.entity.Cycle;
import com.foptron.game.entity.EnemyCycle;
import com.foptron.game.entity.PlayerCycle;
import com.foptron.game.model.Arena;
import com.foptron.game.model.Direction;
import com.foptron.game.model.Pos;

import java.time.Instant;
import java.util.*;

public final class GameSession {

    private final String sessionId;
    private final String playerName;
    private final Random rng;

    private Arena arena;
    private final PlayerCycle player;
    private final List<EnemyCycle> enemies = new ArrayList<>();

    private final Map<String, Arena> arenasById;
    private final Map<String, EnemyDefinition> enemiesById;
    private final Map<String, AchievementDefinition> achievementsById;
    private final String arenaMode;
    private final boolean manualStepMode;

    private long stepIndex;

    private final Map<String, EnemyBrain> brainsByEnemyId = new HashMap<>();

    private final Map<Pos, String> trailColors = new HashMap<>();
    private final Map<String, Disc> discsById = new HashMap<>();

    private final Deque<String> events = new ArrayDeque<>();

    private final Set<String> achievementTitles = new LinkedHashSet<>();
    private final Set<String> achievedIds = new HashSet<>();
    private final Set<String> unlockedChapters = new LinkedHashSet<>();

    private final List<StoryChapter> chapters;

    private long lastTickMs;
    private boolean running;
    private boolean victory;

    private int highestLevelAchieved;
    private long totalScore;

    private StoryChapter currentStory;
    private boolean awaitingEndingChoice;

    public GameSession(
            String sessionId,
            String playerName,
            String arenaMode,
            PlayerCycle player,
            Map<String, Arena> arenasById,
            Map<String, EnemyDefinition> enemiesById,
            List<StoryChapter> chapters,
            Map<String, AchievementDefinition> achievementsById,
            boolean manualStepMode
    ) {
        this.sessionId = sessionId;
        this.playerName = playerName == null || playerName.isBlank() ? "Player" : playerName.trim();
        this.player = player;
        this.arenasById = arenasById;
        this.enemiesById = enemiesById;
        this.chapters = chapters;
        this.achievementsById = achievementsById;
        this.arenaMode = arenaMode == null || arenaMode.isBlank() ? "AUTO" : arenaMode.trim().toUpperCase();
        this.manualStepMode = manualStepMode;

        // In manual mode, keep behavior deterministic across runs.
        this.rng = manualStepMode
            ? new Random(Objects.hash(sessionId, this.playerName, this.arenaMode))
            : new Random(Objects.hash(sessionId, Instant.now().toEpochMilli()));
        this.lastTickMs = System.currentTimeMillis();
        this.running = true;
        this.highestLevelAchieved = player.level();
        this.totalScore = 0;

        this.stepIndex = 0;

        startLevel(true);
    }

    public boolean isManualStepMode() {
        return manualStepMode;
    }

    public long stepIndex() {
        return stepIndex;
    }

    public String playerName() {
        return playerName;
    }

    public String sessionId() {
        return sessionId;
    }

    public Random rng() {
        return rng;
    }

    public Arena arena() {
        return arena;
    }

    public PlayerCycle player() {
        return player;
    }

    public List<EnemyCycle> enemies() {
        return enemies;
    }

    public Collection<Disc> discs() {
        return discsById.values();
    }

    public Map<Pos, String> trailColors() {
        return trailColors;
    }

    public Deque<String> events() {
        return events;
    }

    public Set<String> achievements() {
        return achievementTitles;
    }

    public StoryChapter currentStory() {
        return currentStory;
    }

    public boolean isAwaitingEndingChoice() {
        return awaitingEndingChoice;
    }

    public void applyChoice(int option) {
        if (!awaitingEndingChoice) return;
        if (option != 1 && option != 2) return;

        awaitingEndingChoice = false;
        if (option == 1) {
            setStoryById("END_RESTORE");
            awardAchievementTitle("Ending: Restore the Grid");
            logSys("Choice made: Restore the Grid.");
        } else {
            setStoryById("END_REWRITE");
            awardAchievementTitle("Ending: Rewrite the System");
            logSys("Choice made: Rewrite the System.");
        }
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isVictory() {
        return victory;
    }

    public int highestLevelAchieved() {
        return highestLevelAchieved;
    }

    public long totalScore() {
        return totalScore;
    }

    public void applyInput(Direction direction, boolean throwDisc) {
        if (!running) return;

        if (direction != null && !direction.isOpposite(player.dir())) {
            if (!manualStepMode) {
                if (player.canTurn()) {
                    player.setTurnCooldown(turnCooldownMsFromHandling(player.handling()));
                    player.setPosition(player.pos(), direction);
                }
            } else {
                // In manual step mode, turning should feel immediate and deterministic.
                player.setPosition(player.pos(), direction);
            }
        }

        if (throwDisc && player.canThrowDisc()) {
            if (spawnDisc(player)) {
                logPlayer(player.displayName() + " throws a disc.");
            } else {
                logSys("Disc throw blocked.");
            }
        }
    }

    public void restoreFromSave(int level, long xp, Collection<String> achievementTitles) {
        if (level < 1) level = 1;
        if (level > 99) level = 99;

        player.restoreProgress(level, xp);
        highestLevelAchieved = Math.max(highestLevelAchieved, player.level());

        if (achievementTitles != null) {
            for (String t : achievementTitles) {
                if (t == null || t.isBlank()) continue;
                this.achievementTitles.add(t.trim());
            }
        }

        startLevel(true);
    }

    public void tick() {
        long now = System.currentTimeMillis();
        long dt = Math.min(250, Math.max(16, now - lastTickMs));
        lastTickMs = now;

        if (!running) return;

        stepIndex++;

        player.tickTimers(dt);
        for (EnemyCycle e : enemies) {
            e.tickTimers(dt);
        }

        // AI decisions.
        for (EnemyCycle e : enemies) {
            if (!e.isAlive()) continue;
            EnemyBrain brain = brainsByEnemyId.get(e.id());
            EnemyAction action = brain.decide(this, e);

            if (action.direction() != null && e.canTurn() && !action.direction().isOpposite(e.dir())) {
                e.setTurnCooldown(turnCooldownMsFromHandling(e.handling()));
                e.setPosition(e.pos(), action.direction());
            }
            if (action.throwDisc() && e.canThrowDisc()) {
                spawnDisc(e);
            }
        }

        // Move discs first.
        tickDiscs();

        // Move cycles.
        tickCycle(player, dt);
        for (EnemyCycle e : enemies) {
            tickCycle(e, dt);
        }

        enemies.removeIf(e -> !e.isAlive());
        if (enemies.isEmpty() && running) {
            onLevelCompleted();
        }
        if (!player.isAlive() && running) {
            onLevelFailed();
        }

        unlockStoryForLevel(player.level());
        updateAchievements();
    }

    public void stepOnce() {
        if (!running) return;

        stepIndex++;

        long dt = 100;

        player.tickTimers(dt);
        for (EnemyCycle e : enemies) {
            e.tickTimers(dt);
        }

        // AI decisions (one decision per step).
        for (EnemyCycle e : enemies) {
            if (!e.isAlive()) continue;
            EnemyBrain brain = brainsByEnemyId.get(e.id());
            EnemyAction action = brain.decide(this, e);

            if (action.direction() != null && !action.direction().isOpposite(e.dir())) {
                e.setPosition(e.pos(), action.direction());
            }
            if (action.throwDisc() && e.canThrowDisc()) {
                spawnDisc(e);
            }
        }

        // Discs advance one tile per step.
        tickDiscs();

        // Cycles advance one tile per step.
        advanceCycleOneCell(player);
        for (EnemyCycle e : enemies) {
            advanceCycleOneCell(e);
        }

        enemies.removeIf(e -> !e.isAlive());
        if (enemies.isEmpty() && running) {
            onLevelCompleted();
        }
        if (!player.isAlive() && running) {
            onLevelFailed();
        }

        unlockStoryForLevel(player.level());
        updateAchievements();
    }

    public void manualStep(Direction playerMove, boolean throwDisc) {
        if (!running) return;

        stepIndex++;

        // In manual mode, the player only moves when a direction is provided.
        if (playerMove != null) {
            player.setPosition(player.pos(), playerMove);
        }

        if (throwDisc && player.canThrowDisc()) {
            if (spawnDisc(player)) {
                logPlayer(player.displayName() + " throws a disc.");
            } else {
                logSys("Disc throw blocked.");
            }
        }

        // Timers still tick (cooldowns, respawn protection).
        long dt = 100;
        player.tickTimers(dt);
        for (EnemyCycle e : enemies) {
            e.tickTimers(dt);
        }

        // Enemies decide and move one cell per manual step.
        for (EnemyCycle e : enemies) {
            if (!e.isAlive()) continue;
            EnemyBrain brain = brainsByEnemyId.get(e.id());
            EnemyAction action = brain.decide(this, e);

            if (action.direction() != null) {
                e.setPosition(e.pos(), action.direction());
            }
            if (action.throwDisc() && e.canThrowDisc()) {
                spawnDisc(e);
            }
        }

        // Discs move one tile per step.
        tickDiscs();

        // Player moves at most one tile, only if a direction key was pressed.
        if (playerMove != null) {
            advanceCycleOneCell(player);
        }

        for (EnemyCycle e : enemies) {
            advanceCycleOneCell(e);
        }

        enemies.removeIf(e -> !e.isAlive());
        if (enemies.isEmpty() && running) {
            onLevelCompleted();
        }
        if (!player.isAlive() && running) {
            onLevelFailed();
        }

        unlockStoryForLevel(player.level());
        updateAchievements();
    }

    private enum CollisionKind { NONE, SOLID, TRAIL }

    private CollisionKind collisionKind(Pos next) {
        if (!arena.inBounds(next)) return CollisionKind.SOLID;
        if (arena.isSolid(next)) return CollisionKind.SOLID;
        if (trailColors.containsKey(next)) return CollisionKind.TRAIL;
        return CollisionKind.NONE;
    }

    private boolean cycleCollisionAt(Cycle mover, Pos next) {
        if (next.equals(player.pos()) && mover != player) return true;
        for (EnemyCycle e : enemies) {
            if (e == mover) continue;
            if (next.equals(e.pos())) return true;
        }
        return false;
    }

    private void tickCycle(Cycle cycle, long dtMs) {
        if (!cycle.isAlive()) return;

        cycle.addMoveProgress(dtMs);
        while (cycle.hasMoveStep() && cycle.isAlive() && running) {
            cycle.consumeMoveStep();
            if (!advanceCycleOneCell(cycle)) {
                return;
            }
        }
    }

    private boolean advanceCycleOneCell(Cycle cycle) {
        if (!cycle.isAlive()) return false;
        if (!running) return false;

        Pos next = cycle.pos().add(cycle.dir());

        if (!arena.inBounds(next)) {
            if (arena.isOpen()) {
                if (cycle == player) {
                    logSys(player.displayName() + " falls off the open Grid (all lives lost)!");
                    cycle.applyDamage(cycle.lives());
                } else {
                    cycle.applyDamage(cycle.lives());
                    logEnemy(cycle.displayName() + " falls off the open Grid (derez)!");
                }
                return false;
            }

            if (cycle == player) {
                applyWallCollision(player, "boundary wall", false);
            } else {
                cycle.setPosition(cycle.pos(), opposite(cycle.dir()));
            }
            return false;
        }

        CollisionKind kind = collisionKind(next);
        boolean cycleHit = cycleCollisionAt(cycle, next);
        if (kind != CollisionKind.NONE || cycleHit) {
            if (cycle == player) {
                if (kind == CollisionKind.TRAIL) {
                    applyWallCollision(player, "jetwall", true);
                } else if (kind == CollisionKind.SOLID) {
                    applyWallCollision(player, "wall", false);
                } else {
                    applyCycleCollisionAsPlayer(next);
                }
            } else {
                EnemyCycle enemy = (EnemyCycle) cycle;
                if (kind == CollisionKind.TRAIL) {
                    enemy.applyDamage(0.1);
                    logEnemy(enemy.displayName() + " clips a jetwall (-0.1 lives)!");
                    clearJetwalls();
                    if (enemy.isAlive()) respawnEnemy(enemy);
                } else if (kind == CollisionKind.SOLID) {
                    enemy.applyDamage(0.1);
                    logEnemy(enemy.displayName() + " scrapes a wall (-0.1 lives)!");
                    if (enemy.isAlive()) respawnEnemy(enemy);
                } else {
                    applyCycleCollisionAsEnemy(enemy, next);
                }

                if (!enemy.isAlive()) {
                    onEnemyDefeated(enemy);
                }
            }
            return false;
        }

        // Leave jetwall where you were.
        trailColors.put(cycle.pos(), cycle.color());
        cycle.trail().add(cycle.pos());

        // Move.
        cycle.setPosition(next, cycle.dir());

        if (arena.isRamp(next)) {
            cycle.applyRampBoost(0.15, 3.0);
        }

        recaptureIfPossible(cycle);
        return true;
    }

    private void applyWallCollision(PlayerCycle p, String reason, boolean jetwall) {
        if (p.isRespawnProtected()) return;

        p.applyDamage(0.5);
        logPlayer(p.displayName() + " collides with " + reason + " (-0.5 lives)!");
        if (jetwall) {
            clearJetwalls();
        }
        if (p.isAlive()) {
            respawnPlayer();
        }
    }

    private void applyCycleCollisionAsPlayer(Pos next) {
        if (player.isRespawnProtected()) return;

        player.applyDamage(0.5);
        logPlayer(player.displayName() + " collides with an enemy (-0.5 lives)!");

        for (EnemyCycle e : enemies) {
            if (next.equals(e.pos()) && e.isAlive()) {
                e.applyDamage(0.1);
                logEnemy(e.displayName() + " bumps the player (-0.1 lives)!");
                if (!e.isAlive()) {
                    onEnemyDefeated(e);
                } else {
                    respawnEnemy(e);
                }
                break;
            }
        }

        if (player.isAlive()) {
            respawnPlayer();
        }
    }

    private void applyCycleCollisionAsEnemy(EnemyCycle enemy, Pos next) {
        if (!enemy.isAlive()) return;
        if (enemy.isRespawnProtected()) return;

        if (next.equals(player.pos()) && player.isAlive() && !player.isRespawnProtected()) {
            player.applyDamage(0.5);
            enemy.applyDamage(0.1);
            logEnemy(enemy.displayName() + " collides with the player (-0.1 lives)!");
            logPlayer(player.displayName() + " is hit by an enemy (-0.5 lives)!");
            if (player.isAlive()) respawnPlayer();
        } else {
            enemy.applyDamage(0.1);
            logEnemy(enemy.displayName() + " collides (-0.1 lives)!");
        }

        if (enemy.isAlive()) {
            respawnEnemy(enemy);
        }
    }

    public boolean willCollide(Cycle cycle, Pos next) {
        if (!arena.inBounds(next)) {
            return !arena.isOpen();
        }
        if (arena.isSolid(next)) {
            return true;
        }
        if (trailColors.containsKey(next)) {
            return true;
        }
        return false;
    }

    public boolean isPlayerInDiscLine(EnemyCycle enemy, int range) {
        Pos p = player.pos();
        Pos e = enemy.pos();
        Direction d = enemy.dir();
        for (int i = 1; i <= range; i++) {
            Pos step = new Pos(e.x + d.dx * i, e.y + d.dy * i);
            if (!arena.inBounds(step)) return false;
            if (arena.isSolid(step) || trailColors.containsKey(step)) return false;
            if (step.equals(p)) return true;
        }
        return false;
    }

    private void tickDiscs() {
        List<Disc> discs = new ArrayList<>(discsById.values());
        for (Disc disc : discs) {
            if (!disc.isFlying()) {
                continue;
            }

            Pos next = disc.pos().add(disc.dir());
            if (!arena.inBounds(next) || arena.isSolid(next) || trailColors.containsKey(next)) {
                disc.land();
                continue;
            }

            disc.setPos(next);
            disc.decRange();

            if (next.equals(player.pos()) && !player.isRespawnProtected()) {
                player.applyDamage(1.0);
                logPlayer(player.displayName() + " is struck by a disc (-1 life)!");
                disc.land();
                if (player.isAlive()) {
                    respawnPlayer();
                }
                continue;
            }

            for (EnemyCycle e : enemies) {
                if (next.equals(e.pos()) && e.isAlive()) {
                    e.applyDamage(1.0);
                    logEnemy(e.displayName() + " is struck by a disc (-1 life)!");
                    disc.land();
                    if (!e.isAlive()) {
                        onEnemyDefeated(e);
                    } else {
                        respawnEnemy(e);
                    }
                    break;
                }
            }

            if (disc.remainingRange() <= 0) {
                disc.land();
            }
        }
    }

    private void recaptureIfPossible(Cycle cycle) {
        for (Disc disc : discsById.values()) {
            if (disc.isFlying()) continue;
            if (!disc.pos().equals(cycle.pos())) continue;

            if (cycle == player) {
                if (!disc.ownerColor().equalsIgnoreCase(cycle.color()) && !disc.ownerCycleId().equals(cycle.id())) {
                    continue;
                }
            } else {
                if (!disc.ownerCycleId().equals(cycle.id())) {
                    continue;
                }
            }

            discsById.remove(disc.id());
            cycle.onDiscRecaptured();
            if (cycle == player) {
                logPlayer(player.displayName() + " recaptures a disc — energy restored!");
            }
            break;
        }
    }

    private boolean spawnDisc(Cycle cycle) {
        Pos first = cycle.pos().add(cycle.dir());
        if (!arena.inBounds(first)) return false;
        if (arena.isSolid(first) || trailColors.containsKey(first)) return false;

        // Prevent enemies from throwing into other enemies.
        if (cycle != player) {
            for (EnemyCycle other : enemies) {
                if (other == cycle) continue;
                if (first.equals(other.pos())) {
                    return false;
                }
            }
        }

        String discId = "D-" + UUID.randomUUID();
        Disc disc = new Disc(discId, cycle.id(), cycle.color(), first, cycle.dir(), 2);
        discsById.put(discId, disc);
        cycle.onDiscThrown();

        // Immediate hit if a target is directly in front.
        if (cycle == player) {
            for (EnemyCycle e : enemies) {
                if (first.equals(e.pos()) && e.isAlive()) {
                    e.applyDamage(1.0);
                    logEnemy(e.displayName() + " is struck by a disc (-1 life)!");
                    disc.land();
                    if (!e.isAlive()) {
                        onEnemyDefeated(e);
                    } else {
                        respawnEnemy(e);
                    }
                    break;
                }
            }
        } else {
            if (first.equals(player.pos()) && player.isAlive() && !player.isRespawnProtected()) {
                player.applyDamage(1.0);
                logPlayer(player.displayName() + " is struck by a disc (-1 life)!");
                disc.land();
                if (player.isAlive()) {
                    respawnPlayer();
                }
            }
        }

        return true;
    }

    private void respawnPlayer() {
        Pos spawn = findSpawnFor("P1");
        player.setPosition(spawn, spawnDirectionFor("P1"));
        player.giveRespawnProtection(600);
    }

    private void respawnEnemy(EnemyCycle enemy) {
        enemy.setPosition(findSpawnFor(enemy.id()), spawnDirectionFor(enemy.id()));
        enemy.giveRespawnProtection(400);
    }

    private void clearJetwalls() {
        if (trailColors.isEmpty()) return;
        trailColors.clear();
        player.trail().clear();
        for (EnemyCycle e : enemies) {
            e.trail().clear();
        }
        logSys("Jetwalls cleared.");
    }

    private Pos findSpawnFor(String salt) {
        if (!manualStepMode) {
            for (int tries = 0; tries < 5000; tries++) {
                int x = rng.nextInt(Arena.SIZE);
                int y = rng.nextInt(Arena.SIZE);
                Pos p = new Pos(x, y);
                if (isOccupiedOrBlocked(p)) continue;
                return p;
            }
            return new Pos(Arena.SIZE / 2, Arena.SIZE / 2);
        }

        // Deterministic scan with a salt-based offset.
        int start = Math.floorMod(Objects.hash(salt, player.level(), arena.name()), Arena.SIZE * Arena.SIZE);
        for (int i = 0; i < Arena.SIZE * Arena.SIZE; i++) {
            int idx = (start + i) % (Arena.SIZE * Arena.SIZE);
            int x = idx % Arena.SIZE;
            int y = idx / Arena.SIZE;
            Pos p = new Pos(x, y);
            if (isOccupiedOrBlocked(p)) continue;
            return p;
        }
        return new Pos(Arena.SIZE / 2, Arena.SIZE / 2);
    }

    private boolean isOccupiedOrBlocked(Pos p) {
        if (arena.isSolid(p)) return true;
        if (trailColors.containsKey(p)) return true;
        if (p.equals(player.pos())) return true;
        for (EnemyCycle e : enemies) {
            if (p.equals(e.pos())) return true;
        }
        return false;
    }

    private Direction spawnDirectionFor(String salt) {
        if (!manualStepMode) {
            return switch (rng.nextInt(4)) {
                case 0 -> Direction.UP;
                case 1 -> Direction.DOWN;
                case 2 -> Direction.LEFT;
                default -> Direction.RIGHT;
            };
        }
        int v = Math.floorMod(Objects.hash(salt, player.level()), 4);
        return switch (v) {
            case 0 -> Direction.UP;
            case 1 -> Direction.RIGHT;
            case 2 -> Direction.DOWN;
            default -> Direction.LEFT;
        };
    }

    private Direction opposite(Direction d) {
        return switch (d) {
            case UP -> Direction.DOWN;
            case DOWN -> Direction.UP;
            case LEFT -> Direction.RIGHT;
            case RIGHT -> Direction.LEFT;
        };
    }

    private long turnCooldownMsFromHandling(double handling) {
        double h = Math.max(0.1, Math.min(2.0, handling));
        return (long) (350 / h);
    }

    private void onEnemyDefeated(EnemyCycle enemy) {
        EnemyDefinition d = enemy.definition();
        logSys(d.displayName() + " derezzed.");

        if ("SARK".equalsIgnoreCase(d.id())) awardAchievement("ACH_BEAT_SARK");
        if ("RINZLER".equalsIgnoreCase(d.id())) awardAchievement("ACH_BEAT_RINZLER");
        if ("CLU".equalsIgnoreCase(d.id())) awardAchievement("ACH_BEAT_CLU");

        if ("CLU".equalsIgnoreCase(d.id())) {
            unlockStoryOnDefeat("CLU");
            awaitingEndingChoice = true;
        }
    }

    private void onLevelCompleted() {
        int completedLevel = player.level();

        if (completedLevel >= 99) {
            victory = true;
            running = false;
            onRoundEnd(true);
            return;
        }

        long levelXp = xpThresholdForLevel(completedLevel);
        player.setXpForNextLevel(levelXp);
        player.addXp(levelXp);
        player.tryLevelUp();

        highestLevelAchieved = Math.max(highestLevelAchieved, player.level());
        totalScore += levelXp * 100L;

        awardAchievement("ACH_FIRST_WIN");
        logSys("Level " + completedLevel + " complete. +" + levelXp + " XP.");

        startLevel(false);
    }

    private void onLevelFailed() {
        logSys("Derezzed. Restarting level " + player.level() + ".");

        discsById.clear();
        clearJetwalls();
        enemies.clear();
        brainsByEnemyId.clear();

        player.restoreLivesToMax();
        player.resetDiscState();
        player.giveRespawnProtection(700);

        spawnPlayerAndEnemiesForCurrentLevel(false);
    }

    private void startLevel(boolean firstStart) {
        int level = player.level();
        this.arena = selectArenaForLevel(level);

        discsById.clear();
        trailColors.clear();
        player.trail().clear();

        enemies.clear();
        brainsByEnemyId.clear();

        awaitingEndingChoice = false;
        victory = false;

        player.resetDiscState();

        spawnPlayerAndEnemiesForCurrentLevel(firstStart);

        long xpNeed = xpThresholdForLevel(level);
        player.setXpForNextLevel(xpNeed);

        logSys(titleForLevelStart(level));
        unlockStoryForLevel(level);
    }

    private void spawnPlayerAndEnemiesForCurrentLevel(boolean firstStart) {
        int level = player.level();

        int playerQuadrant = manualStepMode
            ? Math.floorMod(Objects.hash(playerName, level, arena.name()), 4)
            : rng.nextInt(4);
        player.setPosition(spawnInQuadrant(arena, playerQuadrant), Direction.RIGHT);
        player.giveRespawnProtection(firstStart ? 900 : 700);

        List<EnemyDefinition> defs = enemyDefsForLevel(level);
        for (int i = 0; i < defs.size(); i++) {
            EnemyDefinition ed = defs.get(i);
            EnemyCycle enemy = new EnemyCycle("E" + (i + 1), ed);
            int q = (playerQuadrant + 1 + (i % 3)) % 4;
            enemy.setPosition(spawnInQuadrant(arena, q), spawnDirectionFor(enemy.id()));
            enemies.add(enemy);
            brainsByEnemyId.put(enemy.id(), Brains.forEnemyId(enemy.definition().id()));
        }
    }

    private Pos spawnInQuadrant(Arena arena, int quadrant) {
        int minX = quadrant == 0 || quadrant == 2 ? 2 : 22;
        int maxX = quadrant == 0 || quadrant == 2 ? 17 : 37;
        int minY = quadrant == 0 || quadrant == 1 ? 2 : 22;
        int maxY = quadrant == 0 || quadrant == 1 ? 17 : 37;

        if (!manualStepMode) {
            for (int tries = 0; tries < 5000; tries++) {
                int x = minX + rng.nextInt(maxX - minX + 1);
                int y = minY + rng.nextInt(maxY - minY + 1);
                Pos p = new Pos(x, y);
                if (!arena.inBounds(p)) continue;
                if (arena.isSolid(p)) continue;
                if (trailColors.containsKey(p)) continue;
                return p;
            }
        } else {
            int start = Math.floorMod(Objects.hash("Q", quadrant, player.level(), arena.name()), (maxX - minX + 1) * (maxY - minY + 1));
            int w = (maxX - minX + 1);
            int h = (maxY - minY + 1);
            for (int i = 0; i < w * h; i++) {
                int idx = (start + i) % (w * h);
                int x = minX + (idx % w);
                int y = minY + (idx / w);
                Pos p = new Pos(x, y);
                if (!arena.inBounds(p)) continue;
                if (arena.isSolid(p)) continue;
                if (trailColors.containsKey(p)) continue;
                return p;
            }
        }
        return new Pos(Arena.SIZE / 2, Arena.SIZE / 2);
    }

    private Arena selectArenaForLevel(int level) {
        if (!"AUTO".equalsIgnoreCase(arenaMode)) {
            Arena chosen = arenasById.get(arenaMode);
            return chosen != null ? chosen : arenasById.getOrDefault("ARENA1", arenasById.values().stream().findFirst().orElseThrow());
        }

        int inChapter = ((level - 1) % 25) + 1;
        if (inChapter <= 6) {
            return arenasById.getOrDefault("ARENA1", arenasById.values().stream().findFirst().orElseThrow());
        }
        if (inChapter <= 12) {
            return arenasById.getOrDefault("ARENA3", arenasById.values().stream().findFirst().orElseThrow());
        }
        if (inChapter <= 18) {
            return arenasById.getOrDefault("ARENA2", arenasById.values().stream().findFirst().orElseThrow());
        }

        long seed = Objects.hash(sessionId, level);
        return ArenaLoader.random("PROCEDURAL_" + level, "Procedural", false, new Random(seed));
    }

    private List<EnemyDefinition> enemyDefsForLevel(int level) {
        List<EnemyDefinition> all = new ArrayList<>(enemiesById.values());
        if (all.isEmpty()) {
            throw new IllegalStateException("No enemies loaded from data/enemies.txt");
        }

        EnemyDefinition koura = enemiesById.getOrDefault("KOURA", all.get(0));
        EnemyDefinition sark = enemiesById.getOrDefault("SARK", all.get(0));
        EnemyDefinition rinzler = enemiesById.getOrDefault("RINZLER", all.get(0));
        EnemyDefinition clu = enemiesById.getOrDefault("CLU", all.get(0));

        int chapter = ((level - 1) / 25) + 1;
        EnemyDefinition preferred = switch (chapter) {
            case 1 -> koura;
            case 2 -> sark;
            case 3 -> rinzler;
            default -> clu;
        };

        List<EnemyDefinition> pool = new ArrayList<>();
        pool.add(koura);
        pool.add(sark);
        pool.add(rinzler);
        pool.add(clu);

        while (pool.size() < 7) {
            pool.add(preferred);
        }
        Collections.shuffle(pool, rng);
        return pool.subList(0, 7);
    }

    private long xpThresholdForLevel(int level) {
        int chapter = ((level - 1) / 25) + 1;
        String id = switch (chapter) {
            case 1 -> "KOURA";
            case 2 -> "SARK";
            case 3 -> "RINZLER";
            default -> "CLU";
        };
        EnemyDefinition def = enemiesById.get(id);
        return def != null ? Math.max(1, def.xpReward()) : 100;
    }

    private String titleForLevelStart(int level) {
        int chapter = ((level - 1) / 25) + 1;
        int inChapter = ((level - 1) % 25) + 1;
        return "Level Start: L" + level + " (Chapter " + chapter + ", stage " + inChapter + ") — " + arena.name();
    }

    private void setStoryById(String id) {
        for (StoryChapter c : chapters) {
            if (c.id().equalsIgnoreCase(id)) {
                currentStory = c;
                return;
            }
        }
    }

    private void onRoundEnd(boolean won) {
        if (won) {
            logSys("System: Run complete.");
        } else {
            logSys("System: Exited to main menu.");
        }
    }

    private void logSys(String message) {
        log("[SYS] " + message);
    }

    private void logPlayer(String message) {
        log("[P] " + message);
    }

    private void logEnemy(String message) {
        log("[E] " + message);
    }

    private void log(String message) {
        events.addFirst(message);
        while (events.size() > 12) {
            events.removeLast();
        }
    }

    private void unlockStoryForLevel(int level) {
        for (StoryChapter c : chapters) {
            if (c.unlockLevel() <= level && unlockedChapters.add(c.id())) {
                currentStory = c;
                logSys("Story unlocked: " + c.title());
            }
        }
    }

    private void unlockStoryOnDefeat(String enemyId) {
        for (StoryChapter c : chapters) {
            if (c.unlockOnDefeat() != null && c.unlockOnDefeat().equalsIgnoreCase(enemyId) && unlockedChapters.add(c.id())) {
                currentStory = c;
                logSys("Story unlocked: " + c.title());
            }
        }
    }

    private void updateAchievements() {
        int level = player.level();
        if (level >= 5) awardAchievement("ACH_LEVEL_5");
        if (level >= 10) awardAchievement("ACH_LEVEL_10");
        if (level >= 25) awardAchievement("ACH_LEVEL_25");
        if (level >= 50) awardAchievement("ACH_LEVEL_50");
        if (level >= 75) awardAchievement("ACH_LEVEL_75");
        if (level >= 99) awardAchievement("ACH_LEVEL_99");
    }

    private void awardAchievement(String id) {
        if (id == null) return;
        String key = id.toUpperCase();
        if (!achievedIds.add(key)) return;

        AchievementDefinition def = achievementsById != null ? achievementsById.get(key) : null;
        if (def != null) {
            achievementTitles.add(def.title());
            log("[ACH] Achievement unlocked: " + def.title());
        } else {
            achievementTitles.add(id);
            log("[ACH] Achievement unlocked: " + id);
        }
    }

    private void awardAchievementTitle(String title) {
        if (title == null || title.isBlank()) return;
        achievementTitles.add(title.trim());
        log("[ACH] Achievement unlocked: " + title.trim());
    }
}
