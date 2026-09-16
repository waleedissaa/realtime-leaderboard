package com.waleedissaa.leaderboard.ranking;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SubmitScoreRequest(
        @NotNull @Positive Long playerId,
        @NotNull @Min(0) @Max(1_000_000_000) Integer value) {}
