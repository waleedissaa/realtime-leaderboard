package com.waleedissaa.leaderboard;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest
public abstract class AbstractIntegrationTest {

    // One container is shared by every test class in the run.
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    protected JdbcClient jdbc;

    @BeforeEach
    void resetData() {
        // Wipe rows between tests; the schema stays as Flyway built it.
        jdbc.sql("TRUNCATE scores, leaderboard_entries, players RESTART IDENTITY CASCADE").update();
        jdbc.sql("UPDATE leaderboards SET version = 0").update();
    }

    protected long createPlayer(String username) {
        return jdbc.sql("""
                INSERT INTO players (username, display_name) VALUES (:u, :u) RETURNING id
                """)
                .param("u", username)
                .query(Long.class)
                .single();
    }

    protected long versionOf(String slug) {
        return jdbc.sql("SELECT version FROM leaderboards WHERE slug = :slug")
                .param("slug", slug)
                .query(Long.class)
                .single();
    }
}
