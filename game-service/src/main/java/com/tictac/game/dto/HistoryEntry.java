package com.tictac.game.dto;

import java.time.Instant;

public record HistoryEntry(
        long gameId,
        String mode,
        String playedAgainst,
        String status,
        String winner,
        Long durationSeconds,
        Instant createdAt,
        Instant endedAt
) {}
