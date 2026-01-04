package com.foptron.game.ai;

import com.foptron.game.engine.GameSession;
import com.foptron.game.entity.EnemyCycle;
import com.foptron.game.model.Direction;
import com.foptron.game.model.Pos;

import java.util.List;
import java.util.Random;

public final class Brains {
    private Brains() {}

    public static EnemyBrain forEnemyId(String enemyId) {
        return switch (enemyId.toUpperCase()) {
            case "KOURA" -> Brains::koura;
            case "SARK" -> Brains::sark;
            case "RINZLER" -> Brains::rinzler;
            case "CLU" -> Brains::clu;
            default -> Brains::sark;
        };
    }

    private static EnemyAction koura(GameSession session, EnemyCycle enemy) {
        // Easy: mostly random, minimal avoidance.
        Direction dir = enemy.dir();

        if (session.isManualStepMode()) {
            long t = session.stepIndex() + Math.floorMod(enemy.id().hashCode(), 7);
            if (t % 6 == 0) dir = turnLeft(dir);
            if (t % 10 == 0) dir = turnRight(dir);
            boolean throwDisc = (t % 18 == 0) && session.isPlayerInDiscLine(enemy, 3);
            return new EnemyAction(dir, throwDisc);
        }

        Random rnd = session.rng();
        if (rnd.nextDouble() < 0.25) {
            dir = randomTurn(rnd, dir);
        }
        return new EnemyAction(dir, rnd.nextDouble() < 0.03);
    }

    private static EnemyAction sark(GameSession session, EnemyCycle enemy) {
        // Medium: predictable wall-following (turn right when blocked).
        Direction dir = enemy.dir();
        Pos next = enemy.pos().add(dir);
        if (session.willCollide(enemy, next)) {
            Direction right = turnRight(dir);
            if (!session.willCollide(enemy, enemy.pos().add(right))) {
                dir = right;
            } else {
                Direction left = turnLeft(dir);
                if (!session.willCollide(enemy, enemy.pos().add(left))) {
                    dir = left;
                } else {
                    dir = opposite(dir);
                }
            }
        }
        if (session.isManualStepMode()) {
            long t = session.stepIndex() + Math.floorMod(enemy.id().hashCode(), 11);
            boolean throwDisc = (t % 14 == 0) && session.isPlayerInDiscLine(enemy, 3);
            return new EnemyAction(dir, throwDisc);
        }

        return new EnemyAction(dir, session.rng().nextDouble() < 0.05);
    }

    private static EnemyAction rinzler(GameSession session, EnemyCycle enemy) {
        // Hard: chase player, basic avoidance.
        Direction dir = steerToward(session, enemy);
        if (session.willCollide(enemy, enemy.pos().add(dir))) {
            dir = safeAlternative(session, enemy, dir);
        }
        boolean throwDisc;
        if (session.isManualStepMode()) {
            long t = session.stepIndex() + Math.floorMod(enemy.id().hashCode(), 13);
            throwDisc = (t % 9 == 0) && session.isPlayerInDiscLine(enemy, 3);
        } else {
            throwDisc = session.rng().nextDouble() < 0.08 && session.isPlayerInDiscLine(enemy, 3);
        }
        return new EnemyAction(dir, throwDisc);
    }

    private static EnemyAction clu(GameSession session, EnemyCycle enemy) {
        // Impossible: anticipates player's next cell and prefers cutting paths.
        Direction dir = steerTowardAnticipated(session, enemy);
        if (session.willCollide(enemy, enemy.pos().add(dir))) {
            dir = safeAlternative(session, enemy, dir);
        }
        boolean throwDisc;
        if (session.isManualStepMode()) {
            long t = session.stepIndex() + Math.floorMod(enemy.id().hashCode(), 17);
            throwDisc = (t % 6 == 0) && session.isPlayerInDiscLine(enemy, 3);
        } else {
            throwDisc = session.rng().nextDouble() < 0.15 && session.isPlayerInDiscLine(enemy, 3);
        }
        return new EnemyAction(dir, throwDisc);
    }

    private static Direction safeAlternative(GameSession session, EnemyCycle enemy, Direction preferred) {
        List<Direction> candidates = List.of(preferred, turnLeft(preferred), turnRight(preferred), opposite(preferred));
        for (Direction d : candidates) {
            if (!session.willCollide(enemy, enemy.pos().add(d))) {
                return d;
            }
        }
        return preferred;
    }

    private static Direction steerToward(GameSession session, EnemyCycle enemy) {
        Pos p = session.player().pos();
        Pos e = enemy.pos();
        int dx = Integer.compare(p.x, e.x);
        int dy = Integer.compare(p.y, e.y);

        Direction horizontal = dx < 0 ? Direction.LEFT : Direction.RIGHT;
        Direction vertical = dy < 0 ? Direction.UP : Direction.DOWN;

        if (Math.abs(p.x - e.x) >= Math.abs(p.y - e.y)) {
            return dx == 0 ? vertical : horizontal;
        }
        return dy == 0 ? horizontal : vertical;
    }

    private static Direction steerTowardAnticipated(GameSession session, EnemyCycle enemy) {
        Pos predicted = session.player().pos().add(session.player().dir());
        Pos e = enemy.pos();
        int dx = Integer.compare(predicted.x, e.x);
        int dy = Integer.compare(predicted.y, e.y);
        Direction horizontal = dx < 0 ? Direction.LEFT : Direction.RIGHT;
        Direction vertical = dy < 0 ? Direction.UP : Direction.DOWN;
        if (Math.abs(predicted.x - e.x) >= Math.abs(predicted.y - e.y)) {
            return dx == 0 ? vertical : horizontal;
        }
        return dy == 0 ? horizontal : vertical;
    }

    private static Direction randomTurn(Random rnd, Direction current) {
        return rnd.nextBoolean() ? turnLeft(current) : turnRight(current);
    }

    private static Direction turnLeft(Direction d) {
        return switch (d) {
            case UP -> Direction.LEFT;
            case DOWN -> Direction.RIGHT;
            case LEFT -> Direction.DOWN;
            case RIGHT -> Direction.UP;
        };
    }

    private static Direction turnRight(Direction d) {
        return switch (d) {
            case UP -> Direction.RIGHT;
            case DOWN -> Direction.LEFT;
            case LEFT -> Direction.UP;
            case RIGHT -> Direction.DOWN;
        };
    }

    private static Direction opposite(Direction d) {
        return switch (d) {
            case UP -> Direction.DOWN;
            case DOWN -> Direction.UP;
            case LEFT -> Direction.RIGHT;
            case RIGHT -> Direction.LEFT;
        };
    }
}
