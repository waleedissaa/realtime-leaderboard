package com.waleedissaa.leaderboard.ranking;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.waleedissaa.leaderboard.AbstractIntegrationTest;

class ConcurrentSubmissionTest extends AbstractIntegrationTest {

    private static final String SLUG = "weekly-sprint";

    @Autowired
    RankingService service;

    @Test
    @DisplayName("simultaneous submissions keep the highest score, whatever the order")
    void concurrentSubmissionsKeepTheHighest() throws Exception {
        long ana = createPlayer("ana");
        service.submit(SLUG, new SubmitScoreRequest(ana, 800));

        int[] values = {950, 900, 1000, 870, 1000, 640};
        CyclicBarrier startLine = new CyclicBarrier(values.length);

        try (ExecutorService pool = Executors.newFixedThreadPool(values.length)) {
            List<Callable<SubmitScoreResponse>> tasks = new ArrayList<>();
            for (int value : values) {
                tasks.add(() -> {
                    startLine.await(); // release every thread at the same moment
                    return service.submit(SLUG, new SubmitScoreRequest(ana, value));
                });
            }

            for (Future<SubmitScoreResponse> future : pool.invokeAll(tasks)) {
                future.get(); // fails the test if any submission threw
            }
        }

        Integer best = jdbc.sql("SELECT best_score FROM leaderboard_entries WHERE player_id = :p")
                .param("p", ana)
                .query(Integer.class)
                .single();

        assertThat(best).isEqualTo(1000);

        long history = jdbc.sql("SELECT COUNT(*) FROM scores").query(Long.class).single();
        assertThat(history).isEqualTo(values.length + 1);

        // One row per player, even under concurrent inserts.
        long entries = jdbc.sql("SELECT COUNT(*) FROM leaderboard_entries").query(Long.class).single();
        assertThat(entries).isEqualTo(1);
    }
}
