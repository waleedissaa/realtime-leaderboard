package com.waleedissaa.leaderboard.ranking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.waleedissaa.leaderboard.AbstractIntegrationTest;
import com.waleedissaa.leaderboard.common.BadRequestException;
import com.waleedissaa.leaderboard.common.NotFoundException;

class RankingServiceTest extends AbstractIntegrationTest {

    private static final String SLUG = "weekly-sprint";

    @Autowired
    RankingService service;

    private SubmitScoreResponse submit(long playerId, int value) {
        return service.submit(SLUG, new SubmitScoreRequest(playerId, value));
    }

    @Test
    @DisplayName("a lower score does not replace the player's best")
    void lowerScoreDoesNotReplaceBest() {
        long ana = createPlayer("ana");

        assertThat(submit(ana, 1200).newBest()).isTrue();

        SubmitScoreResponse worse = submit(ana, 900);
        assertThat(worse.newBest()).isFalse();
        assertThat(worse.bestScore()).isEqualTo(1200);

        // Both submissions are kept in history even though the best did not change.
        long history = jdbc.sql("SELECT COUNT(*) FROM scores").query(Long.class).single();
        assertThat(history).isEqualTo(2);
    }

    @Test
    @DisplayName("equal scores share a rank, earliest first")
    void tiesShareRankAndOrderByAchievedAt() {
        long ana = createPlayer("ana");
        long ben = createPlayer("ben");
        long cruz = createPlayer("cruz");

        submit(ana, 1200);
        submit(cruz, 1200);
        submit(ben, 1300);

        List<RankingEntry> entries = service.rankings(SLUG, 10, 0).entries();

        assertThat(entries).extracting(RankingEntry::username)
                .containsExactly("ben", "ana", "cruz");
        assertThat(entries).extracting(RankingEntry::rank)
                .containsExactly(1L, 2L, 2L);
    }

    @Test
    @DisplayName("the version only changes when the rankings change")
    void versionTracksRankingChangesOnly() {
        long ana = createPlayer("ana");

        submit(ana, 1200);
        assertThat(versionOf(SLUG)).isEqualTo(1);

        submit(ana, 900); // not a new best
        assertThat(versionOf(SLUG)).isEqualTo(1);

        submit(ana, 1500);
        assertThat(versionOf(SLUG)).isEqualTo(2);
    }

    @Test
    @DisplayName("pagination keeps true ranks")
    void paginationKeepsTrueRanks() {
        for (int i = 1; i <= 5; i++) {
            submit(createPlayer("p" + i), i * 100);
        }

        RankingsPage page = service.rankings(SLUG, 2, 2);

        assertThat(page.entries()).extracting(RankingEntry::rank).containsExactly(3L, 4L);
        assertThat(page.totalPlayers()).isEqualTo(5);
    }

    @Test
    @DisplayName("bad input is rejected")
    void rejectsBadInput() {
        assertThatThrownBy(() -> service.rankings(SLUG, 0, 0))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.rankings(SLUG, 500, 0))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.rankings("nope", 10, 0))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> submit(999L, 100))
                .isInstanceOf(NotFoundException.class);
    }
}
