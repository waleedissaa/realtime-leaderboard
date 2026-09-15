package com.waleedissaa.leaderboard.player;

import java.time.OffsetDateTime;

public record Player(long id, String username, String displayName, OffsetDateTime createdAt) {}