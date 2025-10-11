package com.tictac.game.ws;

import com.tictac.game.model.GameStatus;

public record GameEvent(
        long gameId,
        String board,
        char turn,
        GameStatus status,
        String deadlineAt // ISO-8601 string
) {}
