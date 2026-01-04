package com.foptron.game.ai;

import com.foptron.game.engine.GameSession;
import com.foptron.game.entity.EnemyCycle;

public interface EnemyBrain {
    EnemyAction decide(GameSession session, EnemyCycle enemy);
}
