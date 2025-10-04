package com.tictac.game.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record MoveRequest(
        @Min(0) @Max(2) int row,
        @Min(0) @Max(2) int col
) {}
