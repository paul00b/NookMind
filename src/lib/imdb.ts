const IMDB_GRAPHQL = '/api/imdb-graphql';
const IMDB_SUGGEST = '/api/imdb-suggest';
const IMDB_CACHE_TTL_MS = 24 * 60 * 60 * 1000;

export interface EpisodeRating {
  episode: number;
  title: string;
  imdbRating: number | null;
  imdbId?: string;
}

type RawEdge = {
  node: {
    id?: string;
    titleText?: { text: string };
    ratingsSummary?: { aggregateRating: number | null };
    series?: {
      displayableEpisodeNumber?: {
        displayableSeason?: { text?: string };
        episodeNumber?: { episodeNumber: number };
      };
    };
  };
};

// Cache par imdbId — les appels parallèles par saison partagent une seule requête
const episodeCache = new Map<string, Promise<Record<number, EpisodeRating[]> | null>>();
const imdbIdCache = new Map<string, Promise<string | null>>();

function getSessionCache<T>(key: string): T | null {
  if (typeof window === 'undefined') return null;
  try {
    const raw = sessionStorage.getItem(key);
    if (!raw) return null;
    const parsed = JSON.parse(raw) as { ts: number; data: T };
    if (Date.now() - parsed.ts > IMDB_CACHE_TTL_MS) {
      sessionStorage.removeItem(key);
      return null;
    }
    return parsed.data;
  } catch {
    return null;
  }
}

function setSessionCache<T>(key: string, data: T) {
  if (typeof window === 'undefined') return;
  try {
    sessionStorage.setItem(key, JSON.stringify({ ts: Date.now(), data }));
  } catch {
    // Ignore cache write failures.
  }
}

async function doFetch(url: string, options?: RequestInit): Promise<Response | null> {
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), 10000);
  try {
    const res = await fetch(url, { ...options, signal: controller.signal });
    return res.ok ? res : null;
  } catch {
    return null;
  } finally {
    clearTimeout(timeoutId);
  }
}

/** Cherche l'imdbID d'une série via l'API suggestion IMDb. */
export async function fetchSeriesImdbId(title: string): Promise<string | null> {
  const normalizedTitle = title.trim().toLowerCase();
  if (!normalizedTitle) return null;
  const cacheKey = `nookmind_imdb_id_${encodeURIComponent(normalizedTitle)}`;
  const cached = getSessionCache<string | null>(cacheKey);
  if (cached !== null) return cached;

  if (!imdbIdCache.has(normalizedTitle)) {
    imdbIdCache.set(normalizedTitle, (async () => {
      const firstChar = encodeURIComponent(normalizedTitle[0]);
      const query = encodeURIComponent(normalizedTitle);
      const res = await doFetch(`${IMDB_SUGGEST}?firstChar=${firstChar}&query=${query}`);
      if (!res) return null;
      try {
        const data = await res.json();
        const match = (data.d as Array<{ id: string; q?: string }> | undefined)
          ?.find(item => item.q === 'TV series' || item.q === 'TV mini-series' || item.q === 'TV short');
        const imdbId = match?.id ?? null;
        setSessionCache(cacheKey, imdbId);
        return imdbId;
      } catch {
        return null;
      } finally {
        imdbIdCache.delete(normalizedTitle);
      }
    })());
  }

  return imdbIdCache.get(normalizedTitle)!;
}

async function fetchAllEpisodesRaw(imdbId: string): Promise<Record<number, EpisodeRating[]> | null> {
  const cacheKey = `nookmind_imdb_episodes_${imdbId}`;
  const cached = getSessionCache<Record<number, EpisodeRating[]>>(cacheKey);
  if (cached) return cached;

  const allEdges: RawEdge[] = [];
  let cursor: string | null = null;

  // Jusqu'à 10 pages × 100 = 1000 épisodes max
  for (let page = 0; page < 10; page++) {
    const variables: Record<string, unknown> = { id: imdbId, first: 100 };
    if (cursor) variables.after = cursor;

    const body = {
      query: `query AllEpisodes($id: ID!, $first: Int!${cursor ? ', $after: ID' : ''}) {
        title(id: $id) {
          episodes {
            episodes(first: $first${cursor ? ', after: $after' : ''}) {
              pageInfo { hasNextPage endCursor }
              edges {
                node {
                  id
                  titleText { text }
                  ratingsSummary { aggregateRating }
                  series {
                    displayableEpisodeNumber {
                      displayableSeason { text }
                      episodeNumber { episodeNumber }
                    }
                  }
                }
              }
            }
          }
        }
      }`,
      variables,
    };

    const res = await doFetch(IMDB_GRAPHQL, {
      method: 'POST',
      headers: { 'content-type': 'application/json' },
      body: JSON.stringify(body),
    });

    if (!res) return null;
    const data = await res.json();
    if (data.errors?.length) return null;

    const episodesData = data?.data?.title?.episodes?.episodes;
    if (!episodesData?.edges) return null;

    allEdges.push(...(episodesData.edges as RawEdge[]));

    if (!episodesData.pageInfo?.hasNextPage) break;
    cursor = episodesData.pageInfo.endCursor as string;
  }

  if (allEdges.length === 0) return null;

  // Grouper par saison
  const result: Record<number, EpisodeRating[]> = {};
  for (const edge of allEdges) {
    const seasonStr = edge.node.series?.displayableEpisodeNumber?.displayableSeason?.text;
    const seasonNum = seasonStr ? parseInt(seasonStr, 10) : undefined;
    const episodeNum = edge.node.series?.displayableEpisodeNumber?.episodeNumber?.episodeNumber;
    if (!seasonNum || !episodeNum || seasonNum <= 0 || episodeNum <= 0) continue;

    if (!result[seasonNum]) result[seasonNum] = [];
    result[seasonNum].push({
      episode: episodeNum,
      title: edge.node.titleText?.text ?? '',
      imdbRating: edge.node.ratingsSummary?.aggregateRating ?? null,
      imdbId: edge.node.id,
    });
  }

  for (const episodes of Object.values(result)) {
    episodes.sort((a, b) => a.episode - b.episode);
  }

  if (Object.keys(result).length === 0) return null;

  setSessionCache(cacheKey, result);
  return result;
}

function getAllEpisodes(imdbId: string): Promise<Record<number, EpisodeRating[]> | null> {
  if (!episodeCache.has(imdbId)) {
    episodeCache.set(imdbId, fetchAllEpisodesRaw(imdbId));
  }
  return episodeCache.get(imdbId)!;
}

/** Interface identique à omdb.ts — les composants ne changent pas. */
export async function fetchSeasonRatings(
  imdbId: string,
  season: number
): Promise<EpisodeRating[] | null> {
  const all = await getAllEpisodes(imdbId);
  if (!all) return null;
  return all[season] ?? null;
}
