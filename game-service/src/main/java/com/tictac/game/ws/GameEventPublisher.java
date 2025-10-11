package com.tictac.game.ws;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class GameEventPublisher {
    private final SimpMessagingTemplate broker;

    public GameEventPublisher(SimpMessagingTemplate broker) {
        this.broker = broker;
    }

    public void broadcast(GameEvent evt) {
        broker.convertAndSend("/topic/game." + evt.gameId(), evt);
    }
}
