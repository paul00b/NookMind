import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Film } from 'lucide-react';
import { useMovies } from '../context/MoviesContext';
import { fetchUpcomingMovies, extractMovieData } from '../lib/tmdb';
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
    const cacheKey = 'nookmind_upcoming_movies';
    const cached = sessionStorage.getItem(cacheKey);
    if (cached) {
      const { data, ts } = JSON.parse(cached);
      if (Date.now() - ts < 24 * 60 * 60 * 1000) {
        setUpcoming(data);
        setLoading(false);
        return;
      }
    }
    fetchUpcomingMovies(20).then(results => {
      const sorted = hasGenreData
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
        : results;
      const top10 = sorted.slice(0, 10);
      sessionStorage.setItem(cacheKey, JSON.stringify({ data: top10, ts: Date.now() }));
      setUpcoming(top10);
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

  if (upcoming.length === 0) return null;

  return (
    <div className="p-4 md:p-8 max-w-6xl mx-auto">
      <div className="mb-4">
        <h1 className="font-serif text-2xl font-bold text-gray-900 dark:text-gray-100">
          {t('nextUp.comingSoonMovies')}
        </h1>
        <p className="text-sm text-gray-500 dark:text-gray-400">
          {hasGenreData ? t('nextUp.basedOnGenres') : t('nextUp.popularReleases')}
        </p>
      </div>

      <div className="flex gap-4 overflow-x-auto pb-2" style={{ scrollbarWidth: 'none' }}>
        {upcoming.map(movie => {
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
            <div key={movie.id} className="flex-shrink-0 w-36">
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
                  className="mt-1.5 text-xs text-gray-500 dark:text-gray-400 hover:text-amber-600 dark:hover:text-amber-400 transition-colors"
                >
                  {t('nextUp.addToWatchlist')}
                </button>
              )}
              {inList && (
                <p className="mt-1.5 text-xs text-emerald-600 dark:text-emerald-400">✓ In list</p>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}
