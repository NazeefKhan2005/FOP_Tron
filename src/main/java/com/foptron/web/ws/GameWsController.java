package com.foptron.web.ws;

import com.foptron.game.engine.GameSessionManager;
import com.foptron.web.ws.dto.ChoiceMessage;
import com.foptron.web.ws.dto.InputMessage;
import com.foptron.web.ws.dto.StartRequest;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
public class GameWsController {

    private final GameSessionManager manager;

    public GameWsController(GameSessionManager manager) {
        this.manager = manager;
    }

    @MessageMapping("/start")
    public void start(StartRequest request) {
        manager.start(request);
    }

    @MessageMapping("/input")
    public void input(InputMessage msg) {
        manager.input(msg);
    }

    @MessageMapping("/choice")
    public void choice(ChoiceMessage msg) {
        manager.choice(msg);
    }
}
