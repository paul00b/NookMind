import type { Series } from '../types';
import StarRating from './StarRating';
import { Tv } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { formatWaitingLabel, getEffectiveSeriesStatus, isSeriesWaiting } from '../lib/seriesUtils';

interface SeriesCardProps {
  series: Series;
  onClick: () => void;
}

export default function SeriesCard({ series, onClick }: SeriesCardProps) {
  const { t, i18n } = useTranslation();
  const effectiveStatus = getEffectiveSeriesStatus(series);
  return (
    <button
      onClick={onClick}
      className="card text-left group cursor-pointer hover:scale-[1.02] transition-transform duration-200 overflow-hidden flex flex-col"
    >
      {/* Poster */}
      <div className="relative w-full aspect-[2/3] bg-gray-100 dark:bg-gray-800 rounded-xl overflow-hidden mb-3">
        {series.poster_url ? (
          <img
            src={series.poster_url}
            alt={series.title}
            className="w-full h-full object-cover"
            loading="lazy"
          />
        ) : (
          <div className="w-full h-full flex items-center justify-center">
            <Tv size={40} className="text-gray-300 dark:text-gray-600" />
          </div>
        )}
        {/* Status badge */}
        {(() => {
          const isWaiting = isSeriesWaiting(series);
          const futureFirstEpLabel = effectiveStatus === 'want_to_watch'
            ? formatWaitingLabel(
                series.first_air_date,
                '',
                t('seriesCard.waitingTomorrow'),
                (d) => t('seriesCard.waitingDays', { count: d }),
                i18n.language
              )
            : '';
          const bgClass = effectiveStatus === 'watched'
            ? 'bg-emerald-500/90'
            : isWaiting
            ? 'bg-purple-500/90'
            : effectiveStatus === 'watching'
            ? 'bg-blue-500/90'
            : futureFirstEpLabel
            ? 'bg-sky-500/90'
            : 'bg-amber-500/90';
          const label = effectiveStatus === 'watched'
            ? t('seriesCard.watched')
            : isWaiting
            ? formatWaitingLabel(
                series.next_air_date,
                t('seriesCard.waitingNextSeason'),
                t('seriesCard.waitingTomorrow'),
                (d) => t('seriesCard.waitingDays', { count: d }),
                i18n.language
              )
            : effectiveStatus === 'watching'
            ? `S${series.watched_seasons.length}/${series.seasons ?? '?'}`
            : futureFirstEpLabel || t('seriesCard.wantToWatch');
          return (
            <div className={`absolute top-2 right-2 text-xs font-medium px-2 py-0.5 rounded-full text-white ${bgClass}`}>
              {label}
            </div>
          );
        })()}
      </div>

      {/* Info */}
      <div className="px-2 pb-2 flex-1 flex flex-col gap-1">
        <h3 className="font-serif font-semibold text-sm leading-tight text-gray-900 dark:text-gray-100 line-clamp-2 group-hover:text-amber-600 dark:group-hover:text-amber-400 transition-colors">
          {series.title}
        </h3>
        <p className="text-xs text-gray-500 dark:text-gray-400 truncate">{series.creator}</p>
        {effectiveStatus === 'watched' && series.rating && (
          <StarRating value={series.rating} readonly size={13} />
        )}
      </div>
    </button>
  );
}
