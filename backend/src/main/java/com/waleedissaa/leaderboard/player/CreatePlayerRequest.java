package com.waleedissaa.leaderboard.player;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreatePlayerRequest(
        @NotBlank @Size(min = 3, max = 32)
        @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "only letters, numbers, and underscores")
        String username,

        @NotBlank @Size(max = 64)
        String displayName) {}