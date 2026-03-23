import { useTranslation } from 'react-i18next';

interface SeasonGridProps {
  totalSeasons: number;
  watchedSeasons: number[];
  onChange?: (watchedSeasons: number[]) => void;
  readonly?: boolean;
  compact?: boolean;
}

export default function SeasonGrid({ totalSeasons, watchedSeasons, onChange, readonly, compact }: SeasonGridProps) {
  const { t } = useTranslation();

  const toggle = (season: number) => {
    if (readonly || !onChange) return;
    const next = watchedSeasons.includes(season)
      ? watchedSeasons.filter(s => s !== season)
      : [...watchedSeasons, season].sort((a, b) => a - b);
    onChange(next);
  };

  const size = compact ? 'w-6 h-6 text-[10px]' : 'w-8 h-8 text-xs';

  return (
    <div>
      {!compact && (
        <p className="text-sm text-gray-500 dark:text-gray-400 mb-2">
          {t('seriesDetail.seasonsProgress', { watched: watchedSeasons.length, total: totalSeasons })}
        </p>
      )}
      <div className="flex flex-wrap gap-1.5">
        {Array.from({ length: totalSeasons }, (_, i) => i + 1).map(season => {
          const isWatched = watchedSeasons.includes(season);
          return (
            <button
              key={season}
              type="button"
              onClick={() => toggle(season)}
              disabled={readonly}
              className={`${size} rounded-lg font-semibold transition-all flex items-center justify-center ${
                isWatched
                  ? 'bg-emerald-500 text-white shadow-sm'
                  : 'bg-gray-100 dark:bg-gray-800 text-gray-500 dark:text-gray-400 hover:bg-amber-500/20 hover:text-amber-600 dark:hover:text-amber-400'
              } ${readonly ? 'cursor-default' : 'cursor-pointer'}`}
              title={`${t('seriesDetail.season')} ${season}`}
            >
              {season}
            </button>
          );
        })}
      </div>
    </div>
  );
}

export function deriveSeriesStatus(watchedSeasons: number[], totalSeasons: number | null): 'watched' | 'watching' | 'want_to_watch' {
  if (watchedSeasons.length === 0) return 'want_to_watch';
  if (totalSeasons && watchedSeasons.length >= totalSeasons) return 'watched';
  return 'watching';
}
