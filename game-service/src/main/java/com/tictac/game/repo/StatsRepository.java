package com.tictac.game.repo;

import com.tictac.game.dto.GlobalRankingEntry;
import com.tictac.game.dto.HistoryEntry;
import com.tictac.game.dto.MyStatsResponse;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class StatsRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public StatsRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<HistoryEntry> findHistory(long userId, List<String> modes, LocalDate from, LocalDate to, int limit, int offset) {
        List<String> modeList = (modes == null || modes.isEmpty())
                ? List.of("SELF", "VS_SYSTEM", "PVP")
                : modes;
        int modesEmpty = (modes == null || modes.isEmpty()) ? 1 : 0;
        Timestamp fromTs = null;
        Timestamp toTs = null;
        if (from != null) {
            fromTs = Timestamp.valueOf(from.atStartOfDay());
        }
        if (to != null) {
            LocalDateTime endExclusive = to.plusDays(1).atStartOfDay();
            toTs = Timestamp.valueOf(endExclusive);
        }

        String sql = """
            SELECT
                g.GAME_ID,
                g.GAME_MODE,
                g.GAME_STATUS,
                g.WINNER,
                g.CREATED_AT,
                g.ENDED_AT,
                CASE
                  WHEN g.GAME_MODE = 'SELF' THEN 'SELF'
                  WHEN g.GAME_MODE = 'VS_SYSTEM' THEN 'SYSTEM'
                  WHEN g.GAME_MODE = 'PVP' THEN NVL(u.USERNAME, 'UNKNOWN')
                  ELSE 'UNKNOWN'
                END AS PLAYED_AGAINST,
                CASE
                  WHEN g.ENDED_AT IS NULL THEN NULL
                  ELSE ROUND((CAST(g.ENDED_AT AS DATE) - CAST(g.CREATED_AT AS DATE)) * 86400)
                END AS DURATION_SECONDS
            FROM GAMES g
            LEFT JOIN AUTH.USERS u ON u.USER_ID = CASE
                WHEN g.GAME_MODE = 'PVP' AND g.PLAYER_X_ID = :userId THEN g.PLAYER_O_ID
                WHEN g.GAME_MODE = 'PVP' AND g.PLAYER_O_ID = :userId THEN g.PLAYER_X_ID
                ELSE NULL
            END
            WHERE (:userId = g.PLAYER_X_ID OR :userId = g.PLAYER_O_ID)
              AND (:fromDate IS NULL OR g.CREATED_AT >= :fromDate)
              AND (:toDate IS NULL OR g.CREATED_AT < :toDate)
              AND (:modesEmpty = 1 OR g.GAME_MODE IN (:modes))
            ORDER BY g.CREATED_AT DESC
            OFFSET :offset ROWS FETCH NEXT :limit ROWS ONLY
            """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("fromDate", fromTs)
                .addValue("toDate", toTs)
                .addValue("modesEmpty", modesEmpty)
                .addValue("modes", modeList)
                .addValue("limit", limit)
                .addValue("offset", offset);

        return jdbc.query(sql, params, (rs, rowNum) -> {
            Timestamp createdAt = rs.getTimestamp("CREATED_AT");
            Timestamp endedAt = rs.getTimestamp("ENDED_AT");
            Long duration = rs.getObject("DURATION_SECONDS", Long.class);
            return new HistoryEntry(
                    rs.getLong("GAME_ID"),
                    rs.getString("GAME_MODE"),
                    rs.getString("PLAYED_AGAINST"),
                    rs.getString("GAME_STATUS"),
                    rs.getString("WINNER"),
                    duration,
                    createdAt != null ? createdAt.toInstant() : null,
                    endedAt != null ? endedAt.toInstant() : null
            );
        });
    }

    public MyStatsResponse findMyStats(long userId) {
        String sql = """
            SELECT
                COUNT(*) AS PLAYED,
                SUM(CASE
                      WHEN WINNER = 'X' AND PLAYER_X_ID = :userId THEN 1
                      WHEN WINNER = 'O' AND PLAYER_O_ID = :userId THEN 1
                      ELSE 0
                END) AS WINS,
                SUM(CASE
                      WHEN WINNER = 'X' AND PLAYER_O_ID = :userId THEN 1
                      WHEN WINNER = 'O' AND PLAYER_X_ID = :userId THEN 1
                      ELSE 0
                END) AS LOSSES,
                SUM(CASE WHEN GAME_STATUS = 'TIE' OR WINNER = 'TIE' THEN 1 ELSE 0 END) AS TIES,
                SUM(CASE
                      WHEN GAME_STATUS = 'FORFEIT'
                       AND WINNER IN ('X','O')
                       AND ((WINNER = 'X' AND PLAYER_O_ID = :userId) OR (WINNER = 'O' AND PLAYER_X_ID = :userId))
                      THEN 1
                      ELSE 0
                END) AS FORFEITS,
                SUM(ROUND((CAST(NVL(ENDED_AT, SYSTIMESTAMP) AS DATE) - CAST(CREATED_AT AS DATE)) * 86400)) AS TOTAL_TIME_SECONDS
            FROM GAMES
            WHERE GAME_STATUS IN ('X_WON','O_WON','TIE','FORFEIT')
              AND (:userId = PLAYER_X_ID OR :userId = PLAYER_O_ID)
            """;

        MapSqlParameterSource params = new MapSqlParameterSource().addValue("userId", userId);

        return jdbc.queryForObject(sql, params, (rs, rowNum) -> {
            long played = rs.getLong("PLAYED");
            long wins = rs.getLong("WINS");
            long losses = rs.getLong("LOSSES");
            long ties = rs.getLong("TIES");
            long forfeits = rs.getLong("FORFEITS");
            long totalTime = rs.getLong("TOTAL_TIME_SECONDS");
            double avgWinRate = played > 0 ? (double) wins / (double) played : 0.0;
            return new MyStatsResponse(played, wins, losses, ties, forfeits, avgWinRate, totalTime);
        });
    }

    public List<GlobalRankingEntry> findGlobalRanking(int minMatches, int limit, int offset) {
        String sql = """
            WITH participants AS (
                SELECT GAME_ID, PLAYER_X_ID AS USER_ID, 'X' AS MARK
                FROM GAMES
                WHERE PLAYER_X_ID IS NOT NULL
                UNION ALL
                SELECT GAME_ID, PLAYER_O_ID AS USER_ID, 'O' AS MARK
                FROM GAMES
                WHERE PLAYER_O_ID IS NOT NULL AND PLAYER_O_ID <> PLAYER_X_ID
            ),
            finished AS (
                SELECT p.USER_ID, p.MARK, g.GAME_STATUS, g.WINNER, g.ENDED_AT
                FROM participants p
                JOIN GAMES g ON g.GAME_ID = p.GAME_ID
                WHERE g.GAME_STATUS IN ('X_WON','O_WON','TIE','FORFEIT')
            ),
            agg AS (
                SELECT u.USERNAME AS USERNAME,
                       COUNT(*) AS PLAYED,
                       SUM(CASE WHEN f.WINNER = f.MARK THEN 1 ELSE 0 END) AS WINS,
                       SUM(CASE WHEN f.WINNER IN ('X','O') AND f.WINNER <> f.MARK THEN 1 ELSE 0 END) AS LOSSES,
                       SUM(CASE WHEN f.GAME_STATUS = 'TIE' OR f.WINNER = 'TIE' THEN 1 ELSE 0 END) AS TIES,
                       SUM(CASE WHEN f.GAME_STATUS = 'FORFEIT' AND f.WINNER IN ('X','O') AND f.WINNER <> f.MARK THEN 1 ELSE 0 END) AS FORFEITS,
                       MAX(f.ENDED_AT) AS LAST_ENDED_AT
                FROM finished f
                JOIN AUTH.USERS u ON u.USER_ID = f.USER_ID
                GROUP BY u.USERNAME
            ),
            ranked AS (
                SELECT
                    ROW_NUMBER() OVER (
                        ORDER BY
                            CASE WHEN PLAYED = 0 THEN 0 ELSE WINS / PLAYED END DESC,
                            PLAYED DESC,
                            LAST_ENDED_AT DESC
                    ) AS RN,
                    USERNAME,
                    PLAYED,
                    WINS,
                    LOSSES,
                    TIES,
                    FORFEITS,
                    CASE WHEN PLAYED = 0 THEN 0 ELSE ROUND(WINS / PLAYED, 4) END AS WIN_RATE
                FROM agg
                WHERE PLAYED >= :minMatches
            )
            SELECT RN AS RANK, USERNAME, PLAYED, WINS, LOSSES, TIES, FORFEITS, WIN_RATE
            FROM ranked
            WHERE RN > :offset AND RN <= :offset + :limit
            ORDER BY RN
            """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("minMatches", minMatches)
                .addValue("limit", limit)
                .addValue("offset", offset);

        return jdbc.query(sql, params, (rs, rowNum) -> new GlobalRankingEntry(
                rs.getLong("RANK"),
                rs.getString("USERNAME"),
                rs.getLong("PLAYED"),
                rs.getLong("WINS"),
                rs.getLong("LOSSES"),
                rs.getLong("TIES"),
                rs.getLong("FORFEITS"),
                rs.getDouble("WIN_RATE")
        ));
    }
}
