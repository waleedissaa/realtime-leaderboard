import { useCallback, useEffect, useRef, useState } from "react";
import { createPlayer, fetchRankings, submitScore } from "./api";
import "./App.css";

const SLUG = "weekly-sprint";
const POLL_MS = 3000;

export default function App() {
  const [page, setPage] = useState(null);
  const [error, setError] = useState(null);
  const [moved, setMoved] = useState({});

  const prevRanks = useRef(new Map());
  const flashTimer = useRef(null);

  const load = useCallback(async () => {
    try {
      const data = await fetchRankings(SLUG);
      setPage(data);
      setError(null);

      const changes = {};
      for (const entry of data.entries) {
        const before = prevRanks.current.get(entry.playerId);
        if (before !== undefined && before !== entry.rank) {
          changes[entry.playerId] = before > entry.rank ? "up" : "down";
        }
      }
      prevRanks.current = new Map(data.entries.map((e) => [e.playerId, e.rank]));

      if (Object.keys(changes).length > 0) {
        setMoved(changes);
        clearTimeout(flashTimer.current);
        flashTimer.current = setTimeout(() => setMoved({}), 1500);
      }
    } catch (e) {
      setError(e.message);
    }
  }, []);

  useEffect(() => {
    let cancelled = false;
    const tick = () => {
      if (document.hidden || cancelled) return;
      load();
    };
    tick();
    const id = setInterval(tick, POLL_MS);
    return () => {
      cancelled = true;
      clearInterval(id);
      clearTimeout(flashTimer.current);
    };
  }, [load]);

  return (
    <main>
      <header>
        <h1>Weekly Sprint</h1>
        <p className="sub">
          Live standings, refreshed every {POLL_MS / 1000}s
          {page && <> · {page.totalPlayers} players · v{page.version}</>}
        </p>
      </header>

      {error && <p className="error">{error}</p>}

      <table>
        <thead>
          <tr>
            <th className="rank">#</th>
            <th>Player</th>
            <th className="score">Score</th>
          </tr>
        </thead>
        <tbody>
          {page?.entries.map((entry) => (
            <tr key={entry.playerId} className={moved[entry.playerId] ?? ""}>
              <td className="rank">{entry.rank}</td>
              <td>
                {entry.displayName} <span className="handle">@{entry.username}</span>
              </td>
              <td className="score">{entry.score.toLocaleString()}</td>
            </tr>
          ))}
          {page?.entries.length === 0 && (
            <tr>
              <td colSpan="3" className="empty">No scores yet</td>
            </tr>
          )}
        </tbody>
      </table>

      <Controls onDone={load} />
    </main>
  );
}

function Controls({ onDone }) {
  const [username, setUsername] = useState("");
  const [playerId, setPlayerId] = useState("");
  const [value, setValue] = useState("");
  const [status, setStatus] = useState(null);

  const run = async (action) => {
    setStatus(null);
    try {
      setStatus(await action());
      await onDone();
    } catch (e) {
      setStatus(e.message);
    }
  };

  return (
    <section className="controls">
      <form
        onSubmit={(e) => {
          e.preventDefault();
          run(async () => {
            const player = await createPlayer(username, username);
            setPlayerId(String(player.id));
            setUsername("");
            return `Created ${player.displayName} with id ${player.id}`;
          });
        }}
      >
        <input
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          placeholder="new username"
          required
        />
        <button type="submit">Add player</button>
      </form>

      <form
        onSubmit={(e) => {
          e.preventDefault();
          run(async () => {
            const result = await submitScore(SLUG, playerId, value);
            setValue("");
            return result.newBest
              ? `New best ${result.bestScore} — now rank ${result.rank}`
              : `Kept previous best of ${result.bestScore}`;
          });
        }}
      >
        <input
          value={playerId}
          onChange={(e) => setPlayerId(e.target.value)}
          placeholder="player id"
          required
        />
        <input
          value={value}
          onChange={(e) => setValue(e.target.value)}
          type="number"
          min="0"
          placeholder="score"
          required
        />
        <button type="submit">Submit score</button>
      </form>

      {status && <p className="status">{status}</p>}
    </section>
  );
}
