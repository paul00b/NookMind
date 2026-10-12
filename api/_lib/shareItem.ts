// What a shared link shows: a movie or a series from TMDB, a book from Google Books, read on the
// server so the link previews and the share page work for people without the app.

export type ShareKind = 'movie' | 'series' | 'book';
export type ShareLang = 'fr' | 'en';

export interface ShareItem {
  kind: ShareKind;
  id: string;
  title: string;
  year?: string;
  /** Director, creator or author. */
  by?: string;
  /** Runtime, seasons or pages, already worded. */
  extra?: string;
  overview?: string;
  poster?: string;
  backdrop?: string;
}

const TMDB_KEY = process.env.TMDB_API_KEY ?? process.env.VITE_TMDB_API_KEY;
const TMDB_IMAGES = 'https://image.tmdb.org/t/p';

export function parseKind(value: unknown): ShareKind | null {
  return value === 'movie' || value === 'series' || value === 'book' ? value : null;
}

/** A TMDB id is a number; a Google Books id is a short word of letters, digits, - and _. */
export function validId(kind: ShareKind, id: string): boolean {
  return kind === 'book' ? /^[A-Za-z0-9_-]{4,24}$/.test(id) : /^\d{1,9}$/.test(id);
}

export function parseLang(value: unknown): ShareLang {
  return typeof value === 'string' && value.toLowerCase().startsWith('en') ? 'en' : 'fr';
}

/** The first name of whoever shared, kept short and plain. */
export function parseSharer(value: unknown): string | undefined {
  if (typeof value !== 'string') return undefined;
  const name = value.trim().split(/\s+/)[0]?.replace(/[<>"'&]/g, '').slice(0, 24);
  return name || undefined;
}

function runtime(minutes: number | undefined): string | undefined {
  if (!minutes || minutes <= 0) return undefined;
  if (minutes < 60) return `${minutes} min`;
  return `${Math.floor(minutes / 60)} h ${String(minutes % 60).padStart(2, '0')}`;
}

function stripHtml(text: string | undefined): string | undefined {
  const plain = text?.replace(/<[^>]*>/g, ' ').replace(/&nbsp;/g, ' ').replace(/\s+/g, ' ').trim();
  return plain || undefined;
}

async function getJson(url: string): Promise<Record<string, any> | null> {
  try {
    const res = await fetch(url, { signal: AbortSignal.timeout(6000) });
    return res.ok ? ((await res.json()) as Record<string, any>) : null;
  } catch {
    return null;
  }
}

export async function loadShareItem(kind: ShareKind, id: string, lang: ShareLang): Promise<ShareItem | null> {
  if (!validId(kind, id)) return null;
  const language = lang === 'en' ? 'en-US' : 'fr-FR';

  if (kind === 'book') {
    const data = await getJson(`https://www.googleapis.com/books/v1/volumes/${encodeURIComponent(id)}`);
    const info = data?.volumeInfo;
    if (!info?.title) return null;
    // zoom=1 is the cover Google always has; larger zooms are sometimes a placeholder.
    const thumb: string | undefined = info.imageLinks?.thumbnail ?? info.imageLinks?.smallThumbnail;
    const poster = thumb?.replace('http://', 'https://').replace(/([?&]zoom=)\d+/, '$11').replace('&edge=curl', '');
    return {
      kind,
      id,
      title: info.title,
      year: typeof info.publishedDate === 'string' ? info.publishedDate.slice(0, 4) : undefined,
      by: Array.isArray(info.authors) ? info.authors.slice(0, 2).join(', ') : undefined,
      extra: info.pageCount > 0 ? `${info.pageCount} pages` : undefined,
      overview: stripHtml(info.description),
      poster,
    };
  }

  if (!TMDB_KEY) return null;
  const path = kind === 'movie' ? 'movie' : 'tv';
  const data = await getJson(`https://api.themoviedb.org/3/${path}/${id}?api_key=${TMDB_KEY}&language=${language}&append_to_response=credits`);
  if (!data) return null;
  const poster = data.poster_path ? `${TMDB_IMAGES}/w780${data.poster_path}` : undefined;
  const backdrop = data.backdrop_path ? `${TMDB_IMAGES}/w1280${data.backdrop_path}` : undefined;

  if (kind === 'movie') {
    const director = (data.credits?.crew as Array<{ job?: string; name?: string }> | undefined)?.find((c) => c.job === 'Director')?.name;
    return {
      kind,
      id,
      title: data.title || data.original_title,
      year: typeof data.release_date === 'string' ? data.release_date.slice(0, 4) : undefined,
      by: director,
      extra: runtime(data.runtime),
      overview: data.overview || undefined,
      poster,
      backdrop,
    };
  }

  const seasons: number | undefined = data.number_of_seasons;
  return {
    kind,
    id,
    title: data.name || data.original_name,
    year: typeof data.first_air_date === 'string' ? data.first_air_date.slice(0, 4) : undefined,
    by: (data.created_by as Array<{ name?: string }> | undefined)?.[0]?.name,
    extra: seasons ? (lang === 'en' ? `${seasons} season${seasons > 1 ? 's' : ''}` : `${seasons} saison${seasons > 1 ? 's' : ''}`) : undefined,
    overview: data.overview || undefined,
    poster,
    backdrop,
  };
}

/** The colours of each kind, as in the app: indigo for movies, teal for series, amber for books. */
export const ACCENTS: Record<ShareKind, { text: string; from: string; to: string }> = {
  movie: { text: '#A5B4FC', from: '#4F46E5', to: '#9333EA' },
  series: { text: '#5EEAD4', from: '#0F766E', to: '#0891B2' },
  book: { text: '#FCD34D', from: '#B45309', to: '#D97706' },
};

export const COPY = {
  fr: {
    kind: { movie: 'Film', series: 'Série', book: 'Livre' } as Record<ShareKind, string>,
    recommends: (name: string) => `${name} te recommande`,
    discover: 'À découvrir sur NookMind',
    cta: 'Ajouter dans NookMind',
    store: 'Pas encore l’app ? Télécharger NookMind',
    storeOnly: 'Télécharger NookMind',
    notFound: 'Ce lien ne mène plus nulle part.',
    tagline: 'Tes films, séries et livres, au même endroit.',
    description: (name: string | undefined, title: string) => name ? `${name} te recommande « ${title} » sur NookMind.` : `« ${title} » sur NookMind.`,
  },
  en: {
    kind: { movie: 'Movie', series: 'Series', book: 'Book' } as Record<ShareKind, string>,
    recommends: (name: string) => `${name} recommends`,
    discover: 'Discover on NookMind',
    cta: 'Add to NookMind',
    store: 'Don’t have the app? Get NookMind',
    storeOnly: 'Get NookMind',
    notFound: 'This link leads nowhere anymore.',
    tagline: 'Your movies, series and books, in one place.',
    description: (name: string | undefined, title: string) => name ? `${name} recommends “${title}” on NookMind.` : `“${title}” on NookMind.`,
  },
};

export const ANDROID_PACKAGE = 'fr.paulbr.nookmind';
export const STORE_URL = `https://play.google.com/store/apps/details?id=${ANDROID_PACKAGE}`;
export const IOS_BUNDLE_ID = 'fr.paulbr.nookmind';
/** The Apple developer team of the iOS app. Public (it is in every signed build); APPLE_TEAM_ID overrides it. */
export const APPLE_TEAM_ID = '73QPJZQA92';

/**
 * Where an iPhone gets the app, from IOS_STORE_URL on Vercel: the TestFlight public link while the
 * app is in beta (https://testflight.apple.com/join/...), its App Store page afterwards. Undefined
 * until it is set, and the page then offers no download on an iPhone rather than the Play Store.
 */
export const IOS_STORE_URL = (() => {
  const value = process.env.IOS_STORE_URL?.trim();
  return value && /^https:\/\/(testflight|apps)\.apple\.com\//.test(value) ? value : undefined;
})();
