package com.waleedissaa.leaderboard.ranking;

import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class LeaderboardRepository {

    private final JdbcClient jdbc;

    public LeaderboardRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Leaderboard> findBySlug(String slug) {
        return jdbc.sql("SELECT id, slug, name, version FROM leaderboards WHERE slug = :slug")
                .param("slug", slug)
                .query((rs, rowNum) -> new Leaderboard(
                        rs.getLong("id"), rs.getString("slug"),
                        rs.getString("name"), rs.getLong("version")))
                .optional();
    }

    public void incrementVersion(long leaderboardId) {
        jdbc.sql("UPDATE leaderboards SET version = version + 1 WHERE id = :id")
                .param("id", leaderboardId)
                .update();
    }
}
