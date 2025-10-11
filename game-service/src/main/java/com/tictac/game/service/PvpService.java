package com.tictac.game.service;

import com.tictac.game.model.Game;
import com.tictac.game.model.GameStatus;
import com.tictac.game.model.Mode;
import com.tictac.game.repo.GameRepository;
import com.tictac.game.ws.GameEvent;
import com.tictac.game.ws.GameEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class PvpService {

    private final GameRepository repo;
    private final GameEventPublisher publisher;

    public PvpService(GameRepository repo, GameEventPublisher publisher) {
        this.repo = repo;
        this.publisher = publisher;
    }

    private static Instant nextDeadline() { return Instant.now().plus(20, ChronoUnit.SECONDS); }
    private static String emptyBoard() { return "........."; } // '.' = empty

    @Transactional
    public GameStateDto start(long userId) {
        Game g = new Game();
        g.mode = Mode.PVP;
        g.playerXId = userId;
        g.playerOId = null;
        g.currentTurn = 'X';
        g.status = GameStatus.IN_PROGRESS;
        g.board = emptyBoard();
        g.deadlineAt = nextDeadline();
        g.startedBy = userId;

        long id = repo.create(g);
        return GameStateDto.of(id, g, "X");
    }

    @Transactional
    public GameStateDto join(long gameId, long userId) {
        var g = repo.find(gameId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found"));
        if (g.mode != Mode.PVP) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not a PVP game");
        if (g.playerXId != null && g.playerXId == userId) throw new ResponseStatusException(HttpStatus.CONFLICT, "Already X");
        if (!repo.setPlayerOIfVacant(gameId, userId)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Game full");

        // fresh read
        g = repo.find(gameId).orElseThrow();
        return GameStateDto.of(gameId, g, "O");
    }

    @Transactional
    public GameStateDto move(long gameId, long userId, int row, int col) {
        var g = repo.find(gameId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found"));
        if (g.mode != Mode.PVP) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not a PVP game");
        if (g.status != GameStatus.IN_PROGRESS) throw new ResponseStatusException(HttpStatus.CONFLICT, "Game finished");

        // Turn ownership
        if (g.currentTurn == 'X' && (g.playerXId == null || g.playerXId != userId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your turn");
        if (g.currentTurn == 'O' && (g.playerOId == null || g.playerOId != userId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your turn");

        // Validate coords
        if (row < 0 || row > 2 || col < 0 || col > 2) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bad cell");
        int idx = row * 3 + col;
        if (g.board.charAt(idx) != '.') throw new ResponseStatusException(HttpStatus.CONFLICT, "Cell occupied");

        // Apply move
        char mark = g.currentTurn;
        StringBuilder sb = new StringBuilder(g.board);
        sb.setCharAt(idx, mark);
        String newBoard = sb.toString();

        var status = evaluate(newBoard);
        char nextTurn = (status == GameStatus.IN_PROGRESS) ? (mark == 'X' ? 'O' : 'X') : '-';
        Instant deadline = (status == GameStatus.IN_PROGRESS) ? nextDeadline() : g.deadlineAt;

        // persist
        int moveNo = repo.maxMoveNo(gameId) + 1;
        repo.insertMove(gameId, moveNo, userId, mark, row, col);
        repo.updateState(gameId, newBoard, nextTurn, status, deadline);

        var dto = new GameStateDto(gameId, "PVP", g.playerXId, g.playerOId, String.valueOf(nextTurn), status.name(), newBoard, deadline.toString(), null);

        // Broadcast
        publisher.broadcast(new GameEvent(gameId, newBoard, nextTurn, status, deadline.toString()));

        return dto;
    }

    // winner detection
    private static GameStatus evaluate(String b) {
        int[][] lines = {
                {0,1,2},{3,4,5},{6,7,8}, // rows
                {0,3,6},{1,4,7},{2,5,8}, // cols
                {0,4,8},{2,4,6}          // diags
        };
        for (int[] L: lines) {
            char a = b.charAt(L[0]), c = b.charAt(L[1]), d = b.charAt(L[2]);
            if (a != '.' && a == c && c == d) return (a == 'X') ? GameStatus.X_WON : GameStatus.O_WON;
        }
        if (b.indexOf('.') == -1) return GameStatus.TIE;
        return GameStatus.IN_PROGRESS;
    }

    public record GameStateDto(
            long gameId, String mode, Long playerXId, Long playerOId,
            String turn, String status, String board, String deadlineAt, String youAre
    ) {
        static GameStateDto of(long id, Game g, String youAre) {
            return new GameStateDto(id, g.mode.name(), g.playerXId, g.playerOId,
                    String.valueOf(g.currentTurn), g.status.name(), g.board,
                    g.deadlineAt != null ? g.deadlineAt.toString() : null, youAre);
        }
    }
}
