const OMDB_BASE = 'https://www.omdbapi.com';

function getApiKey(): string {
  return import.meta.env.VITE_OMDB_API_KEY ?? '';
}

export interface EpisodeRating {
  episode: number;
  title: string;
  imdbRating: number | null; // null si "N/A"
}

/** Cherche l'imdbID d'une série par titre. Retourne null si non trouvé ou pas de clé API. */
export async function fetchSeriesImdbId(title: string): Promise<string | null> {
  const key = getApiKey();
  if (!key) return null;
  try {
    const res = await fetch(
      `${OMDB_BASE}/?t=${encodeURIComponent(title)}&type=series&apikey=${key}`
    );
    if (!res.ok) return null;
    const data = await res.json();
    return data.Response === 'True' ? (data.imdbID as string) : null;
  } catch {
    return null;
  }
}

/** Récupère les épisodes d'une saison avec leurs notes IMDB. Retourne null si saison inexistante. */
export async function fetchSeasonRatings(
  imdbId: string,
  season: number
): Promise<EpisodeRating[] | null> {
  const key = getApiKey();
  if (!key) return null;
  try {
    const res = await fetch(
      `${OMDB_BASE}/?i=${imdbId}&Season=${season}&apikey=${key}`
    );
    if (!res.ok) return null;
    const data = await res.json();
    if (data.Response !== 'True' || !Array.isArray(data.Episodes)) return null;
    return (data.Episodes as Array<{ Episode: string; Title: string; imdbRating: string }>).map(ep => ({
      episode: parseInt(ep.Episode, 10),
      title: ep.Title,
      imdbRating: ep.imdbRating === 'N/A' ? null : parseFloat(ep.imdbRating),
    }));
  } catch {
    return null;
  }
}
