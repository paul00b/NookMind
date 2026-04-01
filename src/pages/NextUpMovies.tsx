import { useEffect, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { CalendarDays, Clock3, Film, Plus, X } from 'lucide-react';
import SheetModal from '../components/SheetModal';
import { useMovies } from '../context/MoviesContext';
import { extractDirector, extractMovieData, fetchMovieDetails, fetchRecentMovies, fetchUpcomingMovies, getPosterUrl } from '../lib/tmdb';
import type { Movie, TmdbMovie } from '../types';

function daysUntil(dateStr: string): number {
  const today = new Date(); today.setHours(0, 0, 0, 0);
  const target = new Date(dateStr); target.setHours(0, 0, 0, 0);
  return Math.round((target.getTime() - today.getTime()) / 86400000);
}

function formatDate(dateStr: string, lang: string): string {
  return new Date(dateStr).toLocaleDateString(lang === 'fr' ? 'fr-FR' : 'en-GB', {
    day: 'numeric', month: 'short', year: 'numeric',
  });
}

export default function NextUpMovies() {
  const { movies, addMovie } = useMovies();
  const { t, i18n } = useTranslation();

  const [upcoming, setUpcoming] = useState<TmdbMovie[]>([]);
  const [recent, setRecent] = useState<TmdbMovie[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedMovie, setSelectedMovie] = useState<TmdbMovie | null>(null);
  const [selectedMovieDetails, setSelectedMovieDetails] = useState<TmdbMovie | null>(null);
  const [selectedMovieLoading, setSelectedMovieLoading] = useState(false);

  // Compute user's top genres from watched movies
  const topGenres = (() => {
    const counts: Record<string, number> = {};
    movies.filter(m => m.status === 'watched' && m.genre).forEach(m => {
      counts[m.genre!] = (counts[m.genre!] ?? 0) + 1;
    });
    return Object.entries(counts).sort((a, b) => b[1] - a[1]).map(([g]) => g);
  })();

  const hasGenreData = movies.filter(m => m.status === 'watched').length >= 3;

  useEffect(() => {
    const upcomingCacheKey = 'nookmind_upcoming_movies_v3';
    const recentCacheKey = 'nookmind_recent_movies_v3';

    const sortByTopGenres = (results: TmdbMovie[]) => (
      hasGenreData
        ? [...results].sort((a, b) => {
            const aGenre = a.genres?.[0]?.name ?? '';
            const bGenre = b.genres?.[0]?.name ?? '';
            const aScore = topGenres.indexOf(aGenre);
            const bScore = topGenres.indexOf(bGenre);
            if (aScore === -1 && bScore === -1) return 0;
            if (aScore === -1) return 1;
            if (bScore === -1) return -1;
            return aScore - bScore;
          })
        : results
    );

    const loadFromCache = (cacheKey: string): TmdbMovie[] | null => {
      const cached = sessionStorage.getItem(cacheKey);
      if (!cached) return null;
      const { data, ts } = JSON.parse(cached);
      if (Date.now() - ts >= 24 * 60 * 60 * 1000) return null;
      if (!Array.isArray(data) || data.length === 0) return null;
      return data;
    };

    const cachedUpcoming = loadFromCache(upcomingCacheKey);
    const cachedRecent = loadFromCache(recentCacheKey);
    if (cachedUpcoming) setUpcoming(cachedUpcoming);
    if (cachedRecent) setRecent(cachedRecent);
    if (cachedUpcoming || cachedRecent) setLoading(false);

    Promise.all([
      fetchUpcomingMovies(20),
      fetchRecentMovies(20),
    ]).then(([upcomingResults, recentResults]) => {
      const nextUpcoming = sortByTopGenres(upcomingResults).slice(0, 10);
      const nextRecent = sortByTopGenres(recentResults).slice(0, 10);

      sessionStorage.setItem(upcomingCacheKey, JSON.stringify({ data: nextUpcoming, ts: Date.now() }));
      sessionStorage.setItem(recentCacheKey, JSON.stringify({ data: nextRecent, ts: Date.now() }));

      setUpcoming(nextUpcoming);
      setRecent(nextRecent);
      setLoading(false);
    });
  }, []);

  const watchlistIds = new Set(movies.filter(m => m.tmdb_id != null).map(m => m.tmdb_id!));
  const moviesByTmdbId = useMemo(
    () => new Map(movies.filter(movie => movie.tmdb_id != null).map(movie => [movie.tmdb_id!, movie])),
    [movies]
  );

  useEffect(() => {
    if (!selectedMovie) {
      setSelectedMovieDetails(null);
      setSelectedMovieLoading(false);
      return;
    }

    let active = true;
    setSelectedMovieLoading(true);
    fetchMovieDetails(selectedMovie.id)
      .then(details => {
        if (!active) return;
        setSelectedMovieDetails(details);
      })
      .finally(() => {
        if (active) setSelectedMovieLoading(false);
      });

    return () => { active = false; };
  }, [selectedMovie]);

  const handleAdd = async (movie: TmdbMovie) => {
    await addMovie({ ...extractMovieData(movie), status: 'want_to_watch', rating: null, personal_note: null });
  };

  if (loading) {
    return (
      <div className="max-w-6xl mx-auto px-0 md:px-8 py-4 md:py-8">
        <div className="px-4 md:px-0">
          <div className="h-7 w-40 bg-gray-100 dark:bg-gray-800 rounded-lg animate-pulse mb-6" />
        </div>
        <div className="flex gap-4 overflow-hidden px-4 md:px-0">
          {Array.from({ length: 5 }).map((_, i) => (
            <div key={i} className="flex-shrink-0 w-36">
              <div className="aspect-[2/3] rounded-xl bg-gray-100 dark:bg-gray-800 animate-pulse" />
              <div className="h-4 mt-2 bg-gray-100 dark:bg-gray-800 rounded animate-pulse" />
            </div>
          ))}
        </div>
      </div>
    );
  }

  if (upcoming.length === 0 && recent.length === 0) return null;

  const renderMovieSection = (sectionTitle: string, sectionSubtitle: string, sectionMovies: TmdbMovie[]) => {
    if (sectionMovies.length === 0) return null;

    return (
      <section className="mb-8 last:mb-0">
        <div className="mb-4 px-4 md:px-0">
          <h2 className="font-serif text-2xl font-bold text-gray-900 dark:text-gray-100">
            {sectionTitle}
          </h2>
          <p className="text-sm text-gray-500 dark:text-gray-400">
            {sectionSubtitle}
          </p>
        </div>

        <div
          className="flex gap-5 overflow-x-auto pb-2 px-4 md:px-0"
          style={{ scrollbarWidth: 'none' }}
        >
          {sectionMovies.map(movie => {
            const inList = watchlistIds.has(movie.id);
            const days = movie.release_date ? daysUntil(movie.release_date) : null;
            const dateStr = movie.release_date ? formatDate(movie.release_date, i18n.language) : null;

            const dateLabel = (() => {
              if (days == null || !dateStr) return null;
              if (days < 0) return t('nextUp.releasedOn', { date: dateStr });
              if (days === 0) return dateStr;
              if (days === 1) return t('nextUp.inDays', { days });
              return t('nextUp.inDaysPlural', { days });
            })();

            return (
              <div key={`${sectionTitle}-${movie.id}`} className="flex h-full min-h-[21rem] flex-shrink-0 flex-col w-36">
                <button
                  type="button"
                  onClick={() => setSelectedMovie(movie)}
                  className="aspect-[2/3] rounded-xl overflow-hidden bg-gray-100 dark:bg-gray-800 text-left transition-transform hover:scale-[1.02] focus:outline-none focus:ring-2 focus:ring-amber-400/40"
                  aria-label={t('nextUp.openMovieDetails', { title: movie.title })}
                >
                  {movie.poster_path
                    ? <img
                        src={`https://image.tmdb.org/t/p/w300${movie.poster_path}`}
                        alt={movie.title}
                        className="w-full h-full object-cover"
                      />
                    : <div className="w-full h-full flex items-center justify-center">
                        <Film size={24} className="text-gray-400" />
                      </div>
                  }
                </button>
                <p className="text-sm font-medium text-gray-900 dark:text-gray-100 mt-2 line-clamp-2 leading-tight">
                  {movie.title}
                </p>
                {dateLabel && (
                  <p className="text-xs text-amber-600 dark:text-amber-400 mt-0.5">{dateLabel}</p>
                )}
                {!inList && (
                  <button
                    onClick={() => void handleAdd(movie)}
                    className="mt-auto inline-flex min-h-8 items-center justify-center rounded-full border border-gray-200 bg-white px-2 text-xs font-medium text-gray-600 transition-colors hover:border-amber-200 hover:bg-amber-50 hover:text-amber-700 focus:outline-none focus:ring-2 focus:ring-amber-400/40 dark:border-gray-700 dark:bg-gray-800 dark:text-gray-300 dark:hover:border-amber-800/60 dark:hover:bg-amber-500/10 dark:hover:text-amber-300"
                  >
                    {t('nextUp.addToWatchlist')}
                  </button>
                )}
                {inList && (
                  <p className="mt-auto inline-flex min-h-8 items-center justify-center rounded-full border border-emerald-200 bg-emerald-50 px-3 text-xs font-medium text-emerald-700 dark:border-emerald-900/60 dark:bg-emerald-500/10 dark:text-emerald-300">✓ In list</p>
                )}
              </div>
            );
          })}
        </div>
      </section>
    );
  };

  return (
    <div className="max-w-6xl mx-auto px-0 md:px-8 py-4 md:py-8">
      {renderMovieSection(
        t('nextUp.recentReleasesMovies'),
        t('nextUp.newlyReleasedMovies'),
        recent
      )}
      {renderMovieSection(
        t('nextUp.comingSoonMovies'),
        hasGenreData ? t('nextUp.basedOnGenres') : t('nextUp.popularReleases'),
        upcoming
      )}

      {selectedMovie && (
        <MoviePreviewSheet
          movie={selectedMovie}
          details={selectedMovieDetails}
          loading={selectedMovieLoading}
          existingMovie={moviesByTmdbId.get(selectedMovie.id) ?? null}
          lang={i18n.language}
          t={t}
          onAdd={handleAdd}
          onClose={() => setSelectedMovie(null)}
        />
      )}
    </div>
  );
}

function MoviePreviewSheet({
  movie,
  details,
  loading,
  existingMovie,
  lang,
  t,
  onAdd,
  onClose,
}: {
  movie: TmdbMovie;
  details: TmdbMovie | null;
  loading: boolean;
  existingMovie: Movie | null;
  lang: string;
  t: (key: string, opts?: Record<string, unknown>) => string;
  onAdd: (movie: TmdbMovie) => Promise<void>;
  onClose: () => void;
}) {
  const mergedMovie = details ?? movie;
  const posterUrl = getPosterUrl(mergedMovie.poster_path) ?? existingMovie?.poster_url ?? null;
  const director = existingMovie?.director || extractDirector(mergedMovie);
  const rawRuntime = mergedMovie.runtime ?? existingMovie?.runtime ?? null;
  const runtime = typeof rawRuntime === 'number' && rawRuntime > 0 ? rawRuntime : null;
  const releaseDate = mergedMovie.release_date || existingMovie?.release_date || null;
  const genre = mergedMovie.genres?.[0]?.name ?? existingMovie?.genre ?? null;
  const overview = mergedMovie.overview?.trim() || existingMovie?.description || '';
  const inList = existingMovie != null;

  return (
    <SheetModal
      onClose={onClose}
      panelClassName="md:max-w-xl card rounded-t-3xl rounded-b-none md:rounded-3xl max-h-[88vh] overflow-y-auto animate-slide-up"
    >
        <button
          type="button"
          onClick={onClose}
          className="absolute right-4 top-4 h-10 w-10 rounded-full bg-white/85 text-gray-700 shadow-sm backdrop-blur flex items-center justify-center dark:bg-gray-900/85 dark:text-gray-200"
          aria-label={t('nextUp.closeMovieDetails')}
        >
          <X size={18} />
        </button>

        <div className="p-5 md:p-6">
          <div className="flex gap-4">
            <div className="w-24 md:w-28 aspect-[2/3] rounded-2xl overflow-hidden bg-gray-100 dark:bg-gray-800 flex-shrink-0">
              {posterUrl
                ? <img src={posterUrl} alt={movie.title} className="w-full h-full object-cover" />
                : <div className="w-full h-full flex items-center justify-center"><Film size={28} className="text-gray-400" /></div>
              }
            </div>

            <div className="min-w-0 flex-1 pr-8">
              <p className="text-xs font-semibold uppercase tracking-[0.18em] text-amber-600 dark:text-amber-400">
                {t('nextUp.detailComingSoon')}
              </p>
              <h2 className="mt-1 font-serif text-2xl font-bold leading-tight text-gray-900 dark:text-gray-100">
                {movie.title}
              </h2>
              {director && director !== 'Unknown Director' && (
                <p className="mt-1 text-sm text-gray-600 dark:text-gray-400">{director}</p>
              )}

              <div className="mt-3 flex flex-wrap gap-2 text-xs">
                {genre && (
                  <span className="rounded-full bg-amber-500/10 px-3 py-1 font-medium text-amber-700 dark:text-amber-400">
                    {genre}
                  </span>
                )}
                {releaseDate && (
                  <span className="inline-flex items-center gap-1 rounded-full bg-gray-100 px-3 py-1 text-gray-600 dark:bg-gray-800 dark:text-gray-300">
                    <CalendarDays size={12} />
                    {formatDate(releaseDate, lang)}
                  </span>
                )}
                {runtime && (
                  <span className="inline-flex items-center gap-1 rounded-full bg-gray-100 px-3 py-1 text-gray-600 dark:bg-gray-800 dark:text-gray-300">
                    <Clock3 size={12} />
                    {t('movieDetail.runtime', { count: runtime })}
                  </span>
                )}
              </div>
            </div>
          </div>

          <div className="mt-5">
            <p className="text-sm text-gray-500 dark:text-gray-400">{t('movieDetail.description')}</p>
            <p className="mt-2 text-sm leading-relaxed text-gray-700 dark:text-gray-300">
              {loading && !overview ? t('nextUp.loadingMovieDetails') : overview || t('nextUp.noMovieOverview')}
            </p>
          </div>

          <div className="mt-6 flex justify-center">
            {inList ? (
              <div className="inline-flex min-h-10 items-center justify-center rounded-full border border-emerald-200 bg-emerald-50 px-4 text-sm font-medium text-emerald-700 dark:border-emerald-900/60 dark:bg-emerald-500/10 dark:text-emerald-300">
                {t('movieHome.inWatchlist')}
              </div>
            ) : (
              <button
                type="button"
                onClick={() => void onAdd(movie)}
                className="inline-flex min-h-11 min-w-[9.5rem] items-center justify-center gap-2.5 rounded-full bg-amber-500 px-5 text-sm font-semibold text-white transition-colors hover:bg-amber-600"
              >
                <Plus size={18} strokeWidth={2.4} />
                {t('nextUp.addToWatchlist')}
              </button>
            )}
          </div>
        </div>
    </SheetModal>
  );
}
