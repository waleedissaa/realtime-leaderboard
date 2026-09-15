package com.waleedissaa.leaderboard.player;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class PlayerRepository {

    private static final RowMapper<Player> MAPPER = (rs, rowNum) -> new Player(
            rs.getLong("id"),
            rs.getString("username"),
            rs.getString("display_name"),
            rs.getObject("created_at", OffsetDateTime.class));

    private final JdbcClient jdbc;

    public PlayerRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Player insert(String username, String displayName) {
        return jdbc.sql("""
                INSERT INTO players (username, display_name)
                VALUES (:username, :displayName)
                RETURNING id, username, display_name, created_at
                """)
                .param("username", username)
                .param("displayName", displayName)
                .query(MAPPER)
                .single();
    }

    public Optional<Player> findById(long id) {
        return jdbc.sql("SELECT id, username, display_name, created_at FROM players WHERE id = :id")
                .param("id", id)
                .query(MAPPER)
                .optional();
    }
}