const BASE = import.meta.env.VITE_API_URL ?? "http://localhost:8080";

async function request(path, options) {
  const res = await fetch(`${BASE}${path}`, options);
  if (!res.ok) {
    const problem = await res.json().catch(() => ({}));
    throw new Error(problem.detail ?? `Request failed (${res.status})`);
  }
  return res.json();
}

export const fetchRankings = (slug, limit = 10) =>
  request(`/api/leaderboards/${slug}/rankings?limit=${limit}`);

export const createPlayer = (username, displayName) =>
  request("/api/players", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ username, displayName }),
  });

export const submitScore = (slug, playerId, value) =>
  request(`/api/leaderboards/${slug}/scores`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ playerId: Number(playerId), value: Number(value) }),
  });
