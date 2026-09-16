package com.waleedissaa.leaderboard.ranking;

public record RankingEntry(long rank, long playerId, String username, String displayName, int score) {}
