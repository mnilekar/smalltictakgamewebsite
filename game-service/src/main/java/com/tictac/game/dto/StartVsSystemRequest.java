package com.tictac.game.dto;

import jakarta.validation.constraints.Pattern;

public record StartVsSystemRequest(
        @Pattern(regexp = "^[XO]$") String youPlayAs,
        @Pattern(regexp = "^(EASY|MEDIUM)$") String difficulty
) {}
