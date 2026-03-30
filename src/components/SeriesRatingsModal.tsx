import { useEffect, useRef, useState } from 'react';
import { X, Tv, Plus } from 'lucide-react';
import { fetchSeriesImdbId, fetchSeasonRatings, type EpisodeRating } from '../lib/imdb';
import { useTranslation } from 'react-i18next';

interface SeriesRatingsModalProps {
  isOpen: boolean;
  onClose: () => void;
  title: string;
  creator?: string;
  description?: string | null;
  posterUrl?: string | null;
  firstAirDate?: string | null;
  totalSeasons?: number | null;
  genre?: string | null;
  showAddButton?: boolean;
  onAdd?: () => void;
}

export type SeasonState = EpisodeRating[] | 'loading' | 'error';

export function getRatingStyle(rating: number | null): { background: string; color: string } {
  if (rating === null) return { background: '#374151', color: '#6b7280' };
  if (rating >= 9) return { background: '#16a34a', color: '#ffffff' };
  if (rating >= 8) return { background: '#4ade80', color: '#14532d' };
  if (rating >= 7) return { background: '#facc15', color: '#713f12' };
  if (rating >= 6) return { background: '#f97316', color: '#ffffff' };
  if (rating >= 5) return { background: '#ef4444', color: '#ffffff' };
  return { background: '#7f1d1d', color: '#fca5a5' };
}

function computeStats(seasonRatings: Record<number, SeasonState>) {
  const allRatings: number[] = [];
  for (const val of Object.values(seasonRatings)) {
    if (Array.isArray(val)) {
      val.forEach(ep => { if (ep.imdbRating !== null) allRatings.push(ep.imdbRating); });
    }
  }
  if (allRatings.length === 0) return null;
  return {
    average: (allRatings.reduce((a, b) => a + b, 0) / allRatings.length).toFixed(1),
    best: Math.max(...allRatings).toFixed(1),
    worst: Math.min(...allRatings).toFixed(1),
  };
}

const MAX_SEASONS_FALLBACK = 20;

export default function SeriesRatingsModal({
  isOpen,
  onClose,
  title,
  creator,
  description,
  posterUrl,
  firstAirDate,
  totalSeasons,
  genre,
  showAddButton = false,
  onAdd,
}: SeriesRatingsModalProps) {
  const { t } = useTranslation();
  const [descExpanded, setDescExpanded] = useState(false);
  const [descTruncated, setDescTruncated] = useState(false);
  const descRef = useRef<HTMLParagraphElement>(null);
  const [imdbId, setImdbId] = useState<string | null>(null);
  const [loadingImdb, setLoadingImdb] = useState(false);
  const [imdbError, setImdbError] = useState<'not_found' | 'no_key' | null>(null);
  const [seasonRatings, setSeasonRatings] = useState<Record<number, SeasonState>>({});
  const [fetchKey, setFetchKey] = useState(0); // pour le bouton Réessayer

  // Verrouillage du scroll body à l'ouverture
  useEffect(() => {
    if (!isOpen) return;
    document.body.style.overflow = 'hidden';
    return () => { document.body.style.overflow = ''; };
  }, [isOpen]);

  // Détecte si la description est tronquée
  useEffect(() => {
    if (!descRef.current) return;
    setDescTruncated(descRef.current.scrollHeight > descRef.current.clientHeight);
  }, [description, isOpen]);

  // Reset et recherche imdbID à l'ouverture
  useEffect(() => {
    if (!isOpen) return;
    setImdbId(null);
    setImdbError(null);
    setSeasonRatings({});
    setLoadingImdb(true);

    fetchSeriesImdbId(title).then(id => {
      setLoadingImdb(false);
      if (!id) { setImdbError('not_found'); return; }
      setImdbId(id);
    });
  }, [isOpen, title, fetchKey]);

  // Charge les saisons en parallèle une fois l'imdbID obtenu
  useEffect(() => {
    if (!imdbId) return;
    const max = totalSeasons ?? MAX_SEASONS_FALLBACK;

    for (let s = 1; s <= max; s++) {
      const season = s;
      setSeasonRatings(prev => ({ ...prev, [season]: 'loading' }));
      fetchSeasonRatings(imdbId, season).then(ratings => {
        if (ratings === null && !totalSeasons) {
          // Saison inexistante en mode auto-detect : on l'ignore
          setSeasonRatings(prev => {
            const next = { ...prev };
            delete next[season];
            return next;
          });
        } else {
          setSeasonRatings(prev => ({ ...prev, [season]: ratings ?? 'error' }));
        }
      });
    }
  }, [imdbId, totalSeasons]);

  if (!isOpen) return null;

  const stats = computeStats(seasonRatings);
  const seasons = Object.keys(seasonRatings)
    .map(Number)
    .sort((a, b) => a - b);
  const hasData = seasons.length > 0;

  return (
    <div className="fixed inset-0 z-[60] flex items-end md:items-center justify-center p-0 md:p-4">
      <div className="absolute inset-0 bg-black/50 backdrop-blur-sm animate-fade-in" onClick={onClose} />

      <div className="relative z-10 w-full md:max-w-2xl card animate-slide-up md:rounded-2xl rounded-t-3xl rounded-b-none max-h-[90vh] flex flex-col overflow-hidden">
        {/* Close */}
        <button onClick={onClose} className="absolute top-4 right-4 btn-ghost p-2 z-10">
          <X size={20} />
        </button>

        {/* Header */}
        <div className="flex items-start gap-4 p-6 pb-4 flex-shrink-0">
          <div className="w-14 md:w-16 aspect-[2/3] rounded-lg overflow-hidden bg-gray-100 dark:bg-gray-800 flex-shrink-0">
            {posterUrl ? (
              <img src={posterUrl} alt={title} className="w-full h-full object-cover" />
            ) : (
              <div className="w-full h-full flex items-center justify-center">
                <Tv size={22} className="text-gray-400" />
              </div>
            )}
          </div>
          <div className="flex-1 min-w-0 pr-8">
            <h2 className="font-bold text-base text-gray-900 dark:text-gray-100 leading-tight">{title}</h2>
            <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">
              {[creator, genre, firstAirDate?.slice(0, 4), totalSeasons ? t('seriesDetail.seasons', { count: totalSeasons }) : null]
                .filter(Boolean)
                .join(' · ')}
            </p>
          </div>
        </div>
        {description && (
          <div className="px-6 pb-4 flex-shrink-0">
            <p
              ref={descRef}
              className={`text-xs text-gray-500 dark:text-gray-400 leading-relaxed ${descExpanded ? '' : 'line-clamp-3'}`}
            >
              {description}
            </p>
            {(descTruncated || descExpanded) && (
              <button onClick={() => setDescExpanded(e => !e)} className="inline-flex items-center gap-1 text-xs text-amber-600 mt-1">
                {descExpanded ? '▲ Voir moins' : '▼ Voir plus'}
              </button>
            )}
          </div>
        )}

        {/* Corps scrollable */}
        <div className="overflow-y-auto flex-1">
          {/* Erreurs */}
          {imdbError === 'no_key' && (
            <div className="px-6 py-8 text-center text-sm text-gray-500 dark:text-gray-400">
              {t('seriesDetail.imdbNoApiKey')}
            </div>
          )}
          {imdbError === 'not_found' && (
            <div className="px-6 py-8 text-center space-y-3">
              <p className="text-sm text-gray-500 dark:text-gray-400">{t('seriesDetail.imdbNotAvailable')}</p>
              <button
                onClick={() => setFetchKey(k => k + 1)}
                className="btn-ghost text-sm"
              >
                {t('seriesDetail.imdbRetry')}
              </button>
            </div>
          )}

          {/* Loading imdb ID */}
          {loadingImdb && (
            <div className="px-6 py-8 text-center text-sm text-gray-400 animate-pulse">
              {t('seriesDetail.imdbLoading')}
            </div>
          )}

          {/* Stats */}
          {stats && (
            <div className="flex border-t border-b border-black/[0.06] dark:border-white/[0.06]">
              {[
                { value: stats.average, label: t('seriesDetail.imdbAverage'), border: true },
                { value: stats.best,    label: t('seriesDetail.imdbBest'),    border: true },
                { value: stats.worst,   label: t('seriesDetail.imdbWorst'),   border: false },
              ].map(({ value, label, border }) => {
                const style = getRatingStyle(parseFloat(value));
                return (
                  <div key={label} className={`flex-1 py-3 flex flex-col items-center gap-1.5${border ? ' border-r border-black/[0.06] dark:border-white/[0.06]' : ''}`}>
                    <div
                      className="px-3 py-1 rounded-md text-sm font-extrabold"
                      style={style}
                    >
                      {value}
                    </div>
                    <div className="text-[10px] text-gray-500 dark:text-gray-400 uppercase tracking-wide">
                      {label}
                    </div>
                  </div>
                );
              })}
            </div>
          )}

          {/* Grille épisodes */}
          {hasData && (
            <div className="px-6 py-4">
              <p className="text-[10px] font-semibold text-gray-400 dark:text-gray-500 uppercase tracking-widest mb-3">
                {t('seriesDetail.imdbRatings')}
              </p>
              <div className="overflow-x-auto pb-2">
                <div className="flex gap-2.5" style={{ minWidth: 'max-content' }}>
                  {/* Colonne numéros d'épisodes */}
                  {seasons.some(s => Array.isArray(seasonRatings[s]) && (seasonRatings[s] as EpisodeRating[]).length > 0) && (() => {
                    const maxEps = Math.max(...seasons.map(s => Array.isArray(seasonRatings[s]) ? (seasonRatings[s] as EpisodeRating[]).length : 0));
                    return (
                      <div key="ep-labels">
                        <div className="h-[18px] mb-1.5" />
                        <div className="flex flex-col gap-1">
                          {Array.from({ length: maxEps }, (_, i) => (
                            <div key={i} className="w-7 h-7 flex items-center justify-center text-[10px] font-medium text-gray-400 dark:text-gray-500">
                              E{i + 1}
                            </div>
                          ))}
                        </div>
                      </div>
                    );
                  })()}
                  {seasons.map(s => {
                    const state = seasonRatings[s];
                    return (
                      <div key={s}>
                        <div className="text-[10px] text-gray-500 dark:text-gray-400 font-semibold text-center mb-1.5">
                          S{s}
                        </div>
                        <div className="flex flex-col gap-1">
                          {state === 'loading' ? (
                            Array.from({ length: 6 }).map((_, i) => (
                              <div
                                key={i}
                                className="w-11 h-7 rounded animate-pulse bg-gray-200 dark:bg-gray-700"
                              />
                            ))
                          ) : state === 'error' ? (
                            <div className="w-11 h-7 rounded bg-gray-200 dark:bg-gray-700 flex items-center justify-center text-[9px] text-gray-400">
                              —
                            </div>
                          ) : (
                            state.map(ep => {
                              const style = getRatingStyle(ep.imdbRating);
                              const cell = (
                                <div
                                  className="w-11 h-7 rounded flex items-center justify-center text-xs font-bold select-none"
                                  style={style}
                                >
                                  {ep.imdbRating?.toFixed(1) ?? 'N/A'}
                                </div>
                              );
                              return ep.imdbId ? (
                                <a
                                  key={ep.episode}
                                  href={`https://www.imdb.com/title/${ep.imdbId}/`}
                                  target="_blank"
                                  rel="noopener noreferrer"
                                  title={`E${ep.episode} · ${ep.title}${ep.imdbRating ? ` · ${ep.imdbRating}` : ''}`}
                                  className="hover:opacity-80 transition-opacity"
                                >
                                  {cell}
                                </a>
                              ) : (
                                <div key={ep.episode} title={`E${ep.episode} · ${ep.title}`} className="cursor-default">
                                  {cell}
                                </div>
                              );
                            })
                          )}
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>

              {/* Légende */}
              <div className="flex flex-wrap gap-x-3 gap-y-1.5 mt-4">
                {[
                  { label: '9–10', style: getRatingStyle(9.5) },
                  { label: '8–9',  style: getRatingStyle(8.5) },
                  { label: '7–8',  style: getRatingStyle(7.5) },
                  { label: '6–7',  style: getRatingStyle(6.5) },
                  { label: '5–6',  style: getRatingStyle(5.5) },
                  { label: '<5',   style: getRatingStyle(4)   },
                ].map(({ label, style }) => (
                  <div key={label} className="flex items-center gap-1.5">
                    <div className="w-3 h-3 rounded-sm flex-shrink-0" style={{ background: style.background }} />
                    <span className="text-[10px] text-gray-500 dark:text-gray-400">{label}</span>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Bouton ajout (uniquement depuis recherche) */}
          {showAddButton && onAdd && (
            <div className="px-6 pb-6 pt-2 flex-shrink-0 flex justify-center">
              <button
                onClick={() => { onClose(); onAdd(); }}
                className="btn-primary flex items-center gap-2 px-5 py-2.5"
              >
                <Plus size={16} />
                {t('seriesDetail.addToCollection')}
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
