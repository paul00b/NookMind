import { Tv } from 'lucide-react';
import type { Series, SeriesCategory } from '../types';
import { useTranslation } from 'react-i18next';
import CategoryItemPickerModal from './CategoryItemPickerModal';

interface Props {
  category: SeriesCategory;
  series: Series[];
  onConfirm: (seriesIds: string[]) => void;
  onClose: () => void;
}

export default function CategorySeriesPickerModal({ category, series, onConfirm, onClose }: Props) {
  const { t } = useTranslation();
  return (
    <CategoryItemPickerModal
      existingIds={category.series_ids}
      items={series.map(s => ({ id: s.id, title: s.title, subtitle: s.creator, imageUrl: s.poster_url }))}
      onConfirm={onConfirm}
      onClose={onClose}
      config={{
        header: t('seriesLibrary.addSeriesTo', { name: category.title }),
        searchPlaceholder: t('seriesLibrary.searchSeries'),
        noItemsFound: t('seriesLibrary.noSeriesFound'),
        alreadyInCategory: t('seriesLibrary.alreadyInCategory'),
        cancel: t('seriesLibrary.cancel'),
        confirmLabel: n => n > 0 ? t('seriesLibrary.addNSeries', { count: n }) : t('seriesLibrary.confirmAdd'),
        fallbackIcon: <Tv size={14} className="text-gray-400" />,
      }}
    />
  );
}
