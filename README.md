# Real-Time Leaderboard

A leaderboard that ranks players as scores arrive. Spring Boot REST API over
PostgreSQL, React frontend that polls for updates and highlights rank changes.

**Stack:** Java 21 · Spring Boot · PostgreSQL · React (Vite) · Docker · Testcontainers

## Design decisions

**Ranks are computed, not stored.** A player's rank is one plus the number of
players with a higher score, read from a composite index on
`(leaderboard_id, best_score DESC, achieved_at ASC)`. Storing ranks would mean a
single score change rewriting every row below it — with 10,000 players, a jump from
rank 5,000 to rank 1 rewrites 4,999 rows. Here it updates one.

This scales to tens of thousands of players. Beyond that, a Redis sorted set
would be the next step.

**Writes are a single conditional upsert.** Submitting a score inserts into the
append-only `scores` history, then upserts the player's best:

```sql
INSERT INTO leaderboard_entries (leaderboard_id, player_id, best_score, achieved_at)
VALUES (:lb, :player, :value, now())
ON CONFLICT (leaderboard_id, player_id) DO UPDATE
  SET best_score = EXCLUDED.best_score, achieved_at = EXCLUDED.achieved_at
  WHERE leaderboard_entries.best_score < EXCLUDED.best_score
```

The comparison happens inside the database while the row is locked. A
read-then-write in application code would lose updates: two concurrent
submissions of 950 and 900 could both read the old best of 800, and whichever
committed last would win. The row count returned also tells the service whether
this was a new best, which is what decides if the leaderboard version is bumped.

**Polling with ETags.** The client polls every 3 seconds. Each leaderboard has a
`version` that increments only when rankings actually change, and the rankings
endpoint returns it as an ETag (scoped to the page, so a different `limit` or
`offset` is a different tag). Unchanged polls return `304 Not Modified` with no
body after a single primary-key lookup, and the frontend skips the re-render.

**Schema.** `scores` is append-only history; `leaderboard_entries` holds one row
per player with their current best. The second table is a deliberate
denormalization — computing rankings from the full history would aggregate every
submission ever made on each poll.

## API

| Method | Path | |
|---|---|---|
| POST | `/api/players` | Create a player (409 if the username is taken) |
| POST | `/api/leaderboards/{slug}/scores` | Submit a score; returns whether it was a new best, and the new rank |
| GET | `/api/leaderboards/{slug}/rankings?limit=&offset=` | Ranked page; true ranks survive pagination |
| GET | `/api/leaderboards/{slug}/players/{id}` | One player's standing |

Errors use RFC 9457 `application/problem+json`.

## Tests

Integration tests run against real PostgreSQL via Testcontainers, covering the
ranking rules (ties share a rank, a lower score never replaces a best, the
version only moves when rankings move, pagination keeps true ranks) and a
6-thread concurrency test that submits simultaneously for one player and asserts
the highest score survives regardless of commit order.

Removing the `WHERE` clause from the upsert above makes three of these tests
fail, which is the point of them.

```bash
cd backend && ./mvnw test
```

## Running locally

Requires Docker, Java 21+, and Node 18+.

```bash
docker compose up -d                       # PostgreSQL on :5432
cd backend && ./mvnw spring-boot:run       # API on :8080
cd frontend && npm install && npm run dev  # UI on :5173
```

Flyway applies the schema on startup and seeds two leaderboards.

Set `DEMO_MODE=true` on the backend to run a scheduled job that submits bot
scores every few seconds, so the leaderboard visibly moves.
