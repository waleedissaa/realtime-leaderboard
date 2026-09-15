package com.waleedissaa.leaderboard.player;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import com.waleedissaa.leaderboard.common.NotFoundException;

@Service
public class PlayerService {

    private final PlayerRepository players;

    public PlayerService(PlayerRepository players) {
        this.players = players;
    }

    public Player create(CreatePlayerRequest request) {
        try {
            return players.insert(request.username(), request.displayName());
        } catch (DuplicateKeyException e) {
            throw new UsernameTakenException(request.username());
        }
    }

    public Player get(long id) {
        return players.findById(id)
                .orElseThrow(() -> new NotFoundException("Player " + id + " not found"));
    }
}
