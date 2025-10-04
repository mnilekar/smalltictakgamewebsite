package com.tictac.game.model;

import java.time.Instant;

public class Game {
    public long gameId;
    public Mode mode;
    public Long playerXId; // null for system
    public Long playerOId; // null for system
    public char currentTurn; // 'X' or 'O'
    public GameStatus status;
    public String board; // 9 chars '.' empty
    public Instant deadlineAt;
    public long startedBy;
}
