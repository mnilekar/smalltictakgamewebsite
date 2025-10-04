package com.tictac.game.dto;

import java.time.Instant;
import java.util.Map;

public record GameStateResponse(
        long gameId,
        String mode,         // SELF | VS_SYSTEM
        String board,        // 9 chars, '.' empty
        String turn,         // "X" | "O" | "-" (finished)
        String status,       // IN_PROGRESS | X_WON | O_WON | TIE | FORFEIT
        Instant deadlineAt,
        String youAre,       // X or O (for VS_SYSTEM), or BOTH (for SELF)
        Map<String,Object> systemMove // null or {row,col,mark}
) {}
