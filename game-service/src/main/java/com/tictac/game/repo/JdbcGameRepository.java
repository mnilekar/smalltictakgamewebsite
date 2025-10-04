package com.tictac.game.repo;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.jdbc.support.GeneratedKeyHolder;

import com.tictac.game.model.Game;
import com.tictac.game.model.GameStatus;
import com.tictac.game.model.Mode;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.*;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Repository
public class JdbcGameRepository implements GameRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcGameRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public long create(Game g) {
        String sql = """
  INSERT INTO GAMES (GAME_MODE, PLAYER_X_ID, PLAYER_O_ID, CURRENT_TURN, GAME_STATUS,
                     BOARD_STATE, DEADLINE_AT, STARTED_BY, CREATED_AT, UPDATED_AT)
  VALUES (:mode, :px, :po, :turn, :status, :board, :deadline, :startedBy, SYSTIMESTAMP, SYSTIMESTAMP)
  """;

        MapSqlParameterSource ps = new MapSqlParameterSource()
                .addValue("mode", g.mode.name())
                .addValue("px", g.playerXId)
                .addValue("po", g.playerOId)
                .addValue("turn", String.valueOf(g.currentTurn))
                .addValue("status", g.status.name())
                .addValue("board", g.board)
                .addValue("deadline", java.sql.Timestamp.from(g.deadlineAt))
                .addValue("startedBy", g.startedBy);

        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(sql, ps, kh, new String[]{"GAME_ID"});
        Number key = kh.getKey();
        return (key != null) ? key.longValue() : fetchLastId();
    }

    private long fetchLastId() {
        Long id = jdbc.getJdbcTemplate().queryForObject("SELECT MAX(GAME_ID) FROM GAMES", Long.class);
        return id != null ? id : -1L;
    }

    @Override
    public Optional<Game> find(long gameId) {
        try {
            Game g = jdbc.getJdbcTemplate().queryForObject(
                    "SELECT GAME_ID, GAME_MODE, PLAYER_X_ID, PLAYER_O_ID, CURRENT_TURN, GAME_STATUS, BOARD_STATE, DEADLINE_AT, STARTED_BY " +
                            "FROM GAMES WHERE GAME_ID = ?",
                    (rs, rowNum) -> map(rs), gameId);
            return Optional.ofNullable(g);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    private static Game map(ResultSet rs) throws SQLException {
        Game g = new Game();
        g.gameId = rs.getLong("GAME_ID");
        g.mode = Mode.valueOf(rs.getString("GAME_MODE"));
        long px = rs.getLong("PLAYER_X_ID");
        g.playerXId = rs.wasNull() ? null : px;
        long po = rs.getLong("PLAYER_O_ID");
        g.playerOId = rs.wasNull() ? null : po;
        g.currentTurn = rs.getString("CURRENT_TURN").charAt(0);
        g.status = GameStatus.valueOf(rs.getString("GAME_STATUS"));
        g.board = rs.getString("BOARD_STATE");
        var ts = rs.getTimestamp("DEADLINE_AT");
        g.deadlineAt = (ts != null) ? ts.toInstant() : null;
        g.startedBy = rs.getLong("STARTED_BY");
        return g;
    }

    @Override
    public void insertMove(long gameId, int moveNo, Long byUserId, char mark, int row, int col) {
        String sql = """
      INSERT INTO MOVES (GAME_ID, MOVE_NO, BY_USER_ID, MARK, ROW_INDEX, COL_INDEX, MADE_AT)
      VALUES (:gameId, :moveNo, :byUserId, :mark, :row, :col, SYSTIMESTAMP)
      """;
        MapSqlParameterSource ps = new MapSqlParameterSource()
                .addValue("gameId", gameId)
                .addValue("moveNo", moveNo)
                .addValue("byUserId", byUserId)
                .addValue("mark", String.valueOf(mark))
                .addValue("row", row)
                .addValue("col", col);
        jdbc.update(sql, ps);
    }

    @Override
    public void updateState(long gameId, String board, char currentTurn, GameStatus status, Instant deadlineAt) {
        String sql = """
  UPDATE GAMES
     SET BOARD_STATE = :board,
         CURRENT_TURN = :turn,
         GAME_STATUS  = :status,
         DEADLINE_AT  = :deadline,
         UPDATED_AT   = SYSTIMESTAMP
   WHERE GAME_ID = :id
  """;
        Map<String,Object> params = Map.of(
                "board", board,
                "turn", String.valueOf(currentTurn),
                "status", status.name(),
                "deadline", java.sql.Timestamp.from(deadlineAt),
                "id", gameId
        );
        jdbc.update(sql, params);
    }

    @Override
    public int maxMoveNo(long gameId) {
        Integer m = jdbc.getJdbcTemplate().queryForObject(
                "SELECT COALESCE(MAX(MOVE_NO),0) FROM MOVES WHERE GAME_ID = ?", Integer.class, gameId);
        return (m != null) ? m : 0;
    }
}
