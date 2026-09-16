package com.waleedissaa.leaderboard.ranking;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.waleedissaa.leaderboard.common.BadRequestException;
import com.waleedissaa.leaderboard.common.NotFoundException;
import com.waleedissaa.leaderboard.player.PlayerService;

@Service
public class RankingService {

    private static final int MAX_LIMIT = 100;

    private final LeaderboardRepository leaderboards;
    private final ScoreRepository scores;
    private final RankingReadRepository rankingReads;
    private final PlayerService players;

    public RankingService(LeaderboardRepository leaderboards, ScoreRepository scores,
                          RankingReadRepository rankingReads, PlayerService players) {
        this.leaderboards = leaderboards;
        this.scores = scores;
        this.rankingReads = rankingReads;
        this.players = players;
    }

    @Transactional
    public SubmitScoreResponse submit(String slug, SubmitScoreRequest request) {
        Leaderboard board = findBoard(slug);
        players.get(request.playerId()); // 404 if the player doesn't exist

        scores.insertScore(board.id(), request.playerId(), request.value());
        boolean newBest = scores.upsertBest(board.id(), request.playerId(), request.value());
        if (newBest) {
            leaderboards.incrementVersion(board.id());
        }

        int best = scores.findBest(board.id(), request.playerId()).orElseThrow();
        long rank = scores.countAhead(board.id(), best) + 1;
        return new SubmitScoreResponse(newBest, best, rank);
    }

    @Transactional(readOnly = true)
    public PlayerStanding standing(String slug, long playerId) {
        Leaderboard board = findBoard(slug);
        int best = scores.findBest(board.id(), playerId)
                .orElseThrow(() -> new NotFoundException(
                        "Player " + playerId + " has no score on '" + slug + "'"));
        long rank = scores.countAhead(board.id(), best) + 1;
        return new PlayerStanding(playerId, best, rank, scores.countPlayers(board.id()));
    }

    @Transactional(readOnly = true)
    public RankingsPage rankings(String slug, int limit, int offset) {
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new BadRequestException("limit must be between 1 and " + MAX_LIMIT);
        }
        if (offset < 0) {
            throw new BadRequestException("offset must be 0 or greater");
        }
        Leaderboard board = findBoard(slug);
        List<RankingEntry> entries = rankingReads.findTop(board.id(), limit, offset);
        return new RankingsPage(board.slug(), board.version(), scores.countPlayers(board.id()),
                limit, offset, entries);
    }

    private Leaderboard findBoard(String slug) {
        return leaderboards.findBySlug(slug)
                .orElseThrow(() -> new NotFoundException("Leaderboard '" + slug + "' not found"));
    }
}
