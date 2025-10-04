package com.tictac.game.repo;

import com.tictac.game.model.Game;
import com.tictac.game.model.GameStatus;
import java.time.Instant;
import java.util.Optional;

public interface GameRepository {
    long create(Game g); // returns GAME_ID
    Optional<Game> find(long gameId);
    void insertMove(long gameId, int moveNo, Long byUserId, char mark, int row, int col);
    void updateState(long gameId, String board, char currentTurn, GameStatus status, Instant deadlineAt);
    int maxMoveNo(long gameId);
}
