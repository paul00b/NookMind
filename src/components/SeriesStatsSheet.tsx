import { BarChart2, X, Clock, Tv, Film, Star, Tag, User } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import SheetModal, { SheetCloseButton } from './SheetModal';
import { useSeriesStats } from '../hooks/useSeriesStats';
import type { Series } from '../types';

interface Props {
  isOpen: boolean;
  onClose: () => void;
  series: Series[];
}

function formatHours(minutes: number): string {
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  if (h === 0) return `${m}min`;
  if (m === 0) return `${h}h`;
  return `${h}h ${m}min`;
}

function StatCard({
  icon,
  label,
  value,
}: {
  icon: React.ReactNode;
  label: string;
  value: string | number | null;
}) {
  return (
    <div className="bg-amber-500/5 dark:bg-amber-500/10 rounded-2xl p-4 flex flex-col gap-2">
      <div className="flex items-center gap-2 text-amber-600 dark:text-amber-400">
        {icon}
        <span className="text-xs font-medium text-gray-500 dark:text-gray-400">{label}</span>
      </div>
      <span className="font-serif text-lg font-bold text-gray-900 dark:text-gray-100 truncate">
        {value ?? '—'}
      </span>
    </div>
  );
}

function SeriesStatsSheetContent({ onClose, series }: Omit<Props, 'isOpen'>) {
  const { i18n } = useTranslation();
  const isFr = i18n.language.startsWith('fr');
  const stats = useSeriesStats(series);

  return (
    <SheetModal
      onClose={onClose}
      panelClassName="bg-[#f8f6f1] dark:bg-[#1a1f2e] rounded-t-3xl md:rounded-3xl shadow-2xl animate-slide-up px-6 pt-5 pb-8 md:max-w-md"
      scrollable
    >
      <SheetCloseButton className="absolute top-4 right-4 p-2 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 transition-colors">
        <X size={18} />
      </SheetCloseButton>

      {/* Header */}
      <div className="flex items-center gap-3 mb-6">
        <div className="w-12 h-12 rounded-2xl bg-teal-500/10 flex items-center justify-center shrink-0">
          <BarChart2 size={22} className="text-teal-500" />
        </div>
        <div>
          <h2 className="font-serif text-lg font-bold text-gray-900 dark:text-gray-100">
            {isFr ? 'Mes stats' : 'My stats'}
          </h2>
          <p className="text-xs text-teal-600 dark:text-teal-400 font-medium">
            {isFr ? 'Séries vues & en cours' : 'Watched & watching'}
          </p>
        </div>
      </div>

      {/* Hero stat: heures totales */}
      <div className="bg-teal-500/10 dark:bg-teal-500/15 rounded-2xl p-5 mb-4 flex items-center gap-4">
        <Clock size={28} className="text-teal-500 shrink-0" />
        <div>
          <p className="text-xs text-gray-500 dark:text-gray-400 mb-1">
            {isFr ? 'Temps total regardé' : 'Total watch time'}
          </p>
          {stats.loadingMinutes ? (
            <div className="h-8 w-24 bg-teal-500/20 rounded-lg animate-pulse" />
          ) : (
            <p className="font-serif text-3xl font-bold text-teal-600 dark:text-teal-400">
              {stats.totalMinutes !== null ? formatHours(stats.totalMinutes) : '—'}
            </p>
          )}
        </div>
      </div>

      {/* Grille de fun facts */}
      <div className="grid grid-cols-2 gap-3">
        <StatCard
          icon={<Tv size={14} />}
          label={isFr ? 'Séries' : 'Series'}
          value={stats.totalSeries}
        />
        <StatCard
          icon={<Film size={14} />}
          label={isFr ? 'Épisodes' : 'Episodes'}
          value={stats.watchedEpisodesCount > 0 ? stats.watchedEpisodesCount : null}
        />
        <StatCard
          icon={<BarChart2 size={14} />}
          label={isFr ? 'Saisons' : 'Seasons'}
          value={stats.watchedSeasonsCount > 0 ? stats.watchedSeasonsCount : null}
        />
        <StatCard
          icon={<Star size={14} />}
          label={isFr ? 'Note moyenne' : 'Avg rating'}
          value={stats.averageRating !== null ? `${stats.averageRating}/5` : null}
        />
        <StatCard
          icon={<Tag size={14} />}
          label={isFr ? 'Genre favori' : 'Top genre'}
          value={stats.favoriteGenre}
        />
        <StatCard
          icon={<User size={14} />}
          label={isFr ? 'Créateur favori' : 'Top creator'}
          value={stats.favoriteCreator}
        />
      </div>
    </SheetModal>
  );
}

export default function SeriesStatsSheet({ isOpen, onClose, series }: Props) {
  if (!isOpen) return null;
  return <SeriesStatsSheetContent onClose={onClose} series={series} />;
}
