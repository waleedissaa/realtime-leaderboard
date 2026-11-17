const BASE = import.meta.env.VITE_API_URL ?? "http://localhost:8080";

async function request(path, options) {
  const res = await fetch(`${BASE}${path}`, options);
  if (!res.ok) {
    const problem = await res.json().catch(() => ({}));
    throw new Error(problem.detail ?? `Request failed (${res.status})`);
  }
  return res.json();
}

// Remembers the last ETag and body so a 304 can reuse them.
const cache = new Map();

export async function fetchRankings(slug, limit = 10) {
  const path = `/api/leaderboards/${slug}/rankings?limit=${limit}`;
  const cached = cache.get(path);

  const res = await fetch(`${BASE}${path}`, {
    headers: cached ? { "If-None-Match": cached.etag } : {},
  });

  if (res.status === 304 && cached) {
    return { ...cached.body, unchanged: true };
  }
  if (!res.ok) {
    const problem = await res.json().catch(() => ({}));
    throw new Error(problem.detail ?? `Request failed (${res.status})`);
  }

  const body = await res.json();
  const etag = res.headers.get("ETag");
  if (etag) {
    cache.set(path, { etag, body });
  }
  return body;
}

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
