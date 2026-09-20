package com.waleedissaa.leaderboard.demo;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.waleedissaa.leaderboard.ranking.RankingService;
import com.waleedissaa.leaderboard.ranking.SubmitScoreRequest;

/**
 * Keeps the public demo alive by submitting scores for a pool of bot players.
 * Enabled only when app.demo.enabled=true, so it never runs locally or in tests.
 */
@Component
@ConditionalOnProperty(name = "app.demo.enabled", havingValue = "true")
public class DemoTrafficGenerator {

    private static final Logger log = LoggerFactory.getLogger(DemoTrafficGenerator.class);
    private static final String SLUG = "weekly-sprint";

    private static final List<String> BOTS = List.of(
            "nova", "kite", "ember", "juno", "atlas", "wren", "pike", "sable");

    private final RankingService ranking;
    private final JdbcClient jdbc;

    public DemoTrafficGenerator(RankingService ranking, JdbcClient jdbc) {
        this.ranking = ranking;
        this.jdbc = jdbc;
    }

    /** Creates the bot players once, if they are not already there. */
    @Scheduled(initialDelay = 2_000, fixedDelay = Long.MAX_VALUE)
    public void seedBots() {
        for (String name : BOTS) {
            jdbc.sql("""
                    INSERT INTO players (username, display_name)
                    VALUES (:name, :display)
                    ON CONFLICT (username) DO NOTHING
                    """)
                    .param("name", name)
                    .param("display", name.substring(0, 1).toUpperCase() + name.substring(1))
                    .update();
        }
        log.info("Demo mode on: {} bot players ready", BOTS.size());
    }

    /** Every few seconds, one random bot submits a score. */
    @Scheduled(initialDelay = 5_000, fixedDelayString = "${app.demo.interval-ms:4000}")
    public void submitRandomScore() {
        String name = BOTS.get(ThreadLocalRandom.current().nextInt(BOTS.size()));
        Long playerId = jdbc.sql("SELECT id FROM players WHERE username = :name")
                .param("name", name)
                .query(Long.class)
                .optional()
                .orElse(null);
        if (playerId == null) {
            return; // seeding has not finished yet
        }

        // Most scores are ordinary; roughly 1 in 5 is big enough to shake up the top.
        int value = ThreadLocalRandom.current().nextInt(100, 1000);
        if (ThreadLocalRandom.current().nextInt(5) == 0) {
            value = ThreadLocalRandom.current().nextInt(1000, 2000);
        }

        try {
            ranking.submit(SLUG, new SubmitScoreRequest(playerId, value));
        } catch (RuntimeException e) {
            log.warn("Demo submission failed: {}", e.getMessage());
        }
    }
}
