import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Film } from 'lucide-react';
import { useMovies } from '../context/MoviesContext';
import { fetchRecentMovies, fetchUpcomingMovies, extractMovieData } from '../lib/tmdb';
import type { TmdbMovie } from '../types';

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

  const handleAdd = async (movie: TmdbMovie) => {
    await addMovie({ ...extractMovieData(movie), status: 'want_to_watch', rating: null, personal_note: null });
  };

  if (loading) {
    return (
      <div className="p-4 md:p-8 max-w-6xl mx-auto">
        <div className="h-7 w-40 bg-gray-100 dark:bg-gray-800 rounded-lg animate-pulse mb-6" />
        <div className="flex gap-4 overflow-hidden">
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
        <div className="mb-4">
          <h2 className="font-serif text-2xl font-bold text-gray-900 dark:text-gray-100">
            {sectionTitle}
          </h2>
          <p className="text-sm text-gray-500 dark:text-gray-400">
            {sectionSubtitle}
          </p>
        </div>

        <div className="flex gap-5 overflow-x-auto pb-2" style={{ scrollbarWidth: 'none' }}>
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
                <div className="aspect-[2/3] rounded-xl overflow-hidden bg-gray-100 dark:bg-gray-800">
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
                </div>
                <p className="text-sm font-medium text-gray-900 dark:text-gray-100 mt-2 line-clamp-2 leading-tight">
                  {movie.title}
                </p>
                {dateLabel && (
                  <p className="text-xs text-amber-600 dark:text-amber-400 mt-0.5">{dateLabel}</p>
                )}
                {!inList && (
                  <button
                    onClick={() => handleAdd(movie)}
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
    <div className="p-4 md:p-8 max-w-6xl mx-auto">
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
    </div>
  );
}
