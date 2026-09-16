package com.waleedissaa.leaderboard.ranking;

import java.util.List;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class RankingReadRepository {

    private final JdbcClient jdbc;

    public RankingReadRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<RankingEntry> findTop(long leaderboardId, int limit, int offset) {
        return jdbc.sql("""
                SELECT RANK() OVER (ORDER BY e.best_score DESC) AS rank,
                       e.player_id, p.username, p.display_name, e.best_score
                FROM leaderboard_entries e
                JOIN players p ON p.id = e.player_id
                WHERE e.leaderboard_id = :lb
                ORDER BY e.best_score DESC, e.achieved_at ASC
                LIMIT :limit OFFSET :offset
                """)
                .param("lb", leaderboardId)
                .param("limit", limit)
                .param("offset", offset)
                .query((rs, rowNum) -> new RankingEntry(
                        rs.getLong("rank"),
                        rs.getLong("player_id"),
                        rs.getString("username"),
                        rs.getString("display_name"),
                        rs.getInt("best_score")))
                .list();
    }
}
