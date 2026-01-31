package com.tictac.game.dto;

public record MyStatsResponse(
        long played,
        long wins,
        long losses,
        long ties,
        long forfeits,
        double avgWinRate,
        long totalTimeSeconds
) {}
