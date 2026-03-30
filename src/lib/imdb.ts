const IMDB_GRAPHQL = 'https://caching.graphql.imdb.com/';
const IMDB_SUGGEST = 'https://v3.sg.media-imdb.com/suggestion/titles';

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
  const firstChar = encodeURIComponent(title[0].toLowerCase());
  const query = encodeURIComponent(title.toLowerCase());
  const res = await doFetch(`${IMDB_SUGGEST}/${firstChar}/${query}.json`);
  if (!res) return null;
  try {
    const data = await res.json();
    const match = (data.d as Array<{ id: string; q?: string }> | undefined)
      ?.find(item => item.q === 'TV series' || item.q === 'TV mini-series' || item.q === 'TV short');
    return match?.id ?? null;
  } catch {
    return null;
  }
}

async function fetchAllEpisodesRaw(imdbId: string): Promise<Record<number, EpisodeRating[]> | null> {
  const allEdges: RawEdge[] = [];
  let cursor: string | null = null;

  // Jusqu'à 10 pages × 100 = 1000 épisodes max
  for (let page = 0; page < 10; page++) {
    const variables: Record<string, unknown> = { id: imdbId, first: 100 };
    if (cursor) variables.after = cursor;

    const body = {
      query: `query AllEpisodes($id: ID!, $first: Int!${cursor ? ', $after: String' : ''}) {
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
      headers: {
        'content-type': 'application/json',
        'x-imdb-client-name': 'imdb-web-nextjs-client',
        'x-imdb-user-country': 'US',
        'x-imdb-user-language': 'en-US',
      },
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

  return Object.keys(result).length > 0 ? result : null;
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
