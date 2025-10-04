package com.tictac.game.dto;

import jakarta.validation.constraints.Pattern;

public record StartSelfRequest(
        @Pattern(regexp = "^[XO]$") String firstPlayer
) {}
