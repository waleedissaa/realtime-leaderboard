package com.waleedissaa.leaderboard.ranking;

import java.util.List;

public record RankingsPage(String slug, long version, long totalPlayers,
                           int limit, int offset, List<RankingEntry> entries) {}
