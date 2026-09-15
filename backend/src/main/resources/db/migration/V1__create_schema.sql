CREATE TABLE players (
  id           BIGSERIAL PRIMARY KEY,
  username     VARCHAR(32)  NOT NULL UNIQUE,
  display_name VARCHAR(64)  NOT NULL,
  created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE leaderboards (
  id         BIGSERIAL PRIMARY KEY,
  slug       VARCHAR(64)  NOT NULL UNIQUE,
  name       VARCHAR(128) NOT NULL,
  version    BIGINT       NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE scores (
  id             BIGSERIAL PRIMARY KEY,
  leaderboard_id BIGINT  NOT NULL REFERENCES leaderboards(id),
  player_id      BIGINT  NOT NULL REFERENCES players(id),
  value          INTEGER NOT NULL CHECK (value >= 0),
  submitted_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE leaderboard_entries (
  leaderboard_id BIGINT  NOT NULL REFERENCES leaderboards(id),
  player_id      BIGINT  NOT NULL REFERENCES players(id),
  best_score     INTEGER NOT NULL,
  achieved_at    TIMESTAMPTZ NOT NULL,
  PRIMARY KEY (leaderboard_id, player_id)
);

CREATE INDEX idx_entries_ranking
  ON leaderboard_entries (leaderboard_id, best_score DESC, achieved_at ASC);

CREATE INDEX idx_scores_history
  ON scores (player_id, leaderboard_id, submitted_at DESC);