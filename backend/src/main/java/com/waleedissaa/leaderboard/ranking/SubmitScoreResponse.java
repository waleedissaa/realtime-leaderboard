package com.waleedissaa.leaderboard.ranking;

public record SubmitScoreResponse(boolean newBest, int bestScore, long rank) {}
