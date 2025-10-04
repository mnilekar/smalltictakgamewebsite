package com.tictac.game.service;

import com.tictac.game.dto.*;
import com.tictac.game.engine.TicTacEngine;
import com.tictac.game.model.Game;
import com.tictac.game.model.GameStatus;
import com.tictac.game.model.Mode;
import com.tictac.game.repo.GameRepository;
import com.tictac.game.security.JwtUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@Service
public class GameService {

    private final GameRepository repo;

    public GameService(GameRepository repo) { this.repo = repo; }

    private static Instant newDeadline() {
        return Instant.now().plus(20, ChronoUnit.SECONDS);
    }

    @Transactional
    public GameStateResponse startSelf(JwtUser user, StartSelfRequest req) {
        char first = req.firstPlayer().charAt(0);
        Game g = new Game();
        g.mode = Mode.SELF;
        g.playerXId = user.userId();
        g.playerOId = user.userId();
        g.currentTurn = first;
        g.status = GameStatus.IN_PROGRESS;
        g.board = ".........";
        g.deadlineAt = newDeadline();
        g.startedBy = user.userId();

        long id = repo.create(g);
        return toResp(id, g.board, g.currentTurn, g.status, g.deadlineAt, "BOTH", null, Mode.SELF);
    }

    @Transactional
    public GameStateResponse startVsSystem(JwtUser user, StartVsSystemRequest req) {
        char you = req.youPlayAs().charAt(0);
        char turn = 'X'; // game starts with X
        String board = ".........";

        Game g = new Game();
        g.mode = Mode.VS_SYSTEM;
        g.playerXId = (you=='X') ? user.userId() : null;
        g.playerOId = (you=='O') ? user.userId() : null;
        g.currentTurn = turn;
        g.status = GameStatus.IN_PROGRESS;
        g.board = board;
        g.deadlineAt = newDeadline();
        g.startedBy = user.userId();

        long id = repo.create(g);

        Map<String,Object> systemMove = null;

        // If user chose 'O', system (as X) must move first
        if (you=='O') {
            int[] m = TicTacEngine.bestSystemMove(board, 'X');
            if (m != null) {
                board = TicTacEngine.applyMove(board, m[0], m[1], 'X');
                var w = TicTacEngine.winner(board);
                GameStatus st = (w=='X') ? GameStatus.X_WON : TicTacEngine.isFull(board) ? GameStatus.TIE : GameStatus.IN_PROGRESS;
                turn = (st==GameStatus.IN_PROGRESS) ? 'O' : '-';
                Instant dl = (st==GameStatus.IN_PROGRESS) ? newDeadline() : Instant.now();
                repo.insertMove(id, 1, null, 'X', m[0], m[1]);
                repo.updateState(id, board, turn, st, dl);
                g.currentTurn = turn; g.status = st; g.board = board; g.deadlineAt = dl;
                systemMove = Map.of("row", m[0], "col", m[1], "mark", "X");
            }
        }

        return toResp(id, g.board, g.currentTurn, g.status, g.deadlineAt, String.valueOf(you), systemMove, Mode.VS_SYSTEM);
    }

    @Transactional
    public GameStateResponse move(JwtUser user, long gameId, MoveRequest req) {
        Game g = repo.find(gameId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found"));
        if (g.status != GameStatus.IN_PROGRESS) throw new ResponseStatusException(HttpStatus.CONFLICT, "Game finished");
        if (g.deadlineAt != null && Instant.now().isAfter(g.deadlineAt)) {
            // Mark forfeit by current player
            g.status = GameStatus.FORFEIT;
            g.currentTurn = '-';
            repo.updateState(g.gameId, g.board, g.currentTurn, g.status, Instant.now());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Move deadline exceeded");
        }

        // Authorization: check it's user's turn
        char myMark = markForUser(g, user);
        if (myMark != g.currentTurn && g.mode != Mode.SELF) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your turn");
        }

        // Apply player's move
        String board = TicTacEngine.applyMove(g.board, req.row(), req.col(), g.currentTurn);
        var w = TicTacEngine.winner(board);
        GameStatus st = switch (w) {
            case 'X' -> GameStatus.X_WON;
            case 'O' -> GameStatus.O_WON;
            default -> TicTacEngine.isFull(board) ? GameStatus.TIE : GameStatus.IN_PROGRESS;
        };
        char nextTurn = (st==GameStatus.IN_PROGRESS) ? (g.currentTurn=='X'?'O':'X') : '-';
        Instant dl = (st==GameStatus.IN_PROGRESS) ? newDeadline() : Instant.now();

        int nextMoveNo = repo.maxMoveNo(g.gameId) + 1;
        repo.insertMove(g.gameId, nextMoveNo, user.userId(), g.currentTurn, req.row(), req.col());
        repo.updateState(g.gameId, board, nextTurn, st, dl);

        g.board = board; g.currentTurn = nextTurn; g.status = st; g.deadlineAt = dl;

        Map<String,Object> systemMove = null;

        // If VS_SYSTEM and still in progress and now it's system's turn, make system move
        if (g.mode==Mode.VS_SYSTEM && g.status==GameStatus.IN_PROGRESS) {
            char systemMark = (g.playerXId == null) ? 'X' : 'O';
            if (g.currentTurn == systemMark) {
                int[] m = TicTacEngine.bestSystemMove(g.board, systemMark);
                if (m != null) {
                    board = TicTacEngine.applyMove(g.board, m[0], m[1], systemMark);
                    var w2 = TicTacEngine.winner(board);
                    GameStatus st2 = switch (w2) {
                        case 'X' -> GameStatus.X_WON;
                        case 'O' -> GameStatus.O_WON;
                        default -> TicTacEngine.isFull(board) ? GameStatus.TIE : GameStatus.IN_PROGRESS;
                    };
                    char next2 = (st2==GameStatus.IN_PROGRESS) ? (systemMark=='X'?'O':'X') : '-';
                    Instant dl2 = (st2==GameStatus.IN_PROGRESS) ? newDeadline() : Instant.now();
                    int sysMoveNo = repo.maxMoveNo(g.gameId) + 1;
                    repo.insertMove(g.gameId, sysMoveNo, null, systemMark, m[0], m[1]);
                    repo.updateState(g.gameId, board, next2, st2, dl2);
                    g.board = board; g.currentTurn = next2; g.status = st2; g.deadlineAt = dl2;
                    systemMove = Map.of("row", m[0], "col", m[1], "mark", String.valueOf(systemMark));
                }
            }
        }

        String youAre = (g.mode==Mode.SELF) ? "BOTH" : String.valueOf(markForUser(g, user));
        return toResp(g.gameId, g.board, g.currentTurn, g.status, g.deadlineAt, youAre, systemMove, g.mode);
    }

    public GameStateResponse get(long gameId, JwtUser user) {
        Game g = repo.find(gameId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found"));
        String youAre = (g.mode==Mode.SELF) ? "BOTH" : String.valueOf(markForUser(g, user));
        return toResp(g.gameId, g.board, g.currentTurn, g.status, g.deadlineAt, youAre, null, g.mode);
    }

    private static GameStateResponse toResp(long id, String board, char turn, GameStatus status,
                                            Instant deadline, String youAre, Map<String,Object> systemMove, Mode mode) {
        return new GameStateResponse(id, mode.name(), board, String.valueOf(turn), status.name(), deadline, youAre, systemMove);
    }

    private static char markForUser(Game g, JwtUser user) {
        if (g.mode==Mode.SELF) return g.currentTurn; // same user plays both sides; controller already checks flow
        if (g.playerXId != null && g.playerXId == user.userId()) return 'X';
        if (g.playerOId != null && g.playerOId == user.userId()) return 'O';
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a participant");
    }
}
