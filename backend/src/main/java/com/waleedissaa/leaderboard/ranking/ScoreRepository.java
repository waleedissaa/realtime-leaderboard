package com.waleedissaa.leaderboard.ranking;

import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class ScoreRepository {

    private final JdbcClient jdbc;

    public ScoreRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void insertScore(long leaderboardId, long playerId, int value) {
        jdbc.sql("""
                INSERT INTO scores (leaderboard_id, player_id, value)
                VALUES (:lb, :player, :value)
                """)
                .param("lb", leaderboardId)
                .param("player", playerId)
                .param("value", value)
                .update();
    }

    /** Returns true only if this score became the player's new best. */
    public boolean upsertBest(long leaderboardId, long playerId, int value) {
        int rows = jdbc.sql("""
                INSERT INTO leaderboard_entries (leaderboard_id, player_id, best_score, achieved_at)
                VALUES (:lb, :player, :value, now())
                ON CONFLICT (leaderboard_id, player_id) DO UPDATE
                  SET best_score = EXCLUDED.best_score, achieved_at = EXCLUDED.achieved_at
                  WHERE leaderboard_entries.best_score < EXCLUDED.best_score
                  
                """)
                .param("lb", leaderboardId)
                .param("player", playerId)
                .param("value", value)
                .update();
        return rows == 1;
    }

    public Optional<Integer> findBest(long leaderboardId, long playerId) {
        return jdbc.sql("""
                SELECT best_score FROM leaderboard_entries
                WHERE leaderboard_id = :lb AND player_id = :player
                """)
                .param("lb", leaderboardId)
                .param("player", playerId)
                .query(Integer.class)
                .optional();
    }

    public long countAhead(long leaderboardId, int score) {
        return jdbc.sql("""
                SELECT COUNT(*) FROM leaderboard_entries
                WHERE leaderboard_id = :lb AND best_score > :score
                """)
                .param("lb", leaderboardId)
                .param("score", score)
                .query(Long.class)
                .single();
    }

    public long countPlayers(long leaderboardId) {
        return jdbc.sql("SELECT COUNT(*) FROM leaderboard_entries WHERE leaderboard_id = :lb")
                .param("lb", leaderboardId)
                .query(Long.class)
                .single();
    }
}
