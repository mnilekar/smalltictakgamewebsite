package com.tictac.game.dto;

public record GlobalRankingEntry(
        long rank,
        String username,
        long played,
        long wins,
        long losses,
        long ties,
        long forfeits,
        double winRate
) {}
