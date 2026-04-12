import type { WatchProvidersResult } from '../types';
import { useTranslation } from 'react-i18next';

interface Props {
  providers: WatchProvidersResult | null;
  loading?: boolean;
}

export default function WatchProviders({ providers, loading }: Props) {
  const { t } = useTranslation();

  if (loading) {
    return (
      <div className="flex gap-2">
        {[1, 2, 3].map(i => (
          <div key={i} className="w-9 h-9 rounded-lg bg-gray-200 dark:bg-gray-700 animate-pulse" />
        ))}
      </div>
    );
  }

  if (!providers || providers.flatrate.length === 0) return null;

  return (
    <div>
      <p className="text-sm text-gray-500 dark:text-gray-400 mb-2">{t('watchProviders.availableOn')}</p>
      <div className="flex flex-wrap gap-2">
        {providers.flatrate.map(provider => (
          <a
            key={provider.provider_id}
            href={providers.link ?? '#'}
            target="_blank"
            rel="noopener noreferrer"
            title={provider.provider_name}
            className="block w-9 h-9 rounded-lg overflow-hidden bg-gray-100 dark:bg-gray-800 hover:scale-110 transition-transform"
          >
            <img
              src={`https://image.tmdb.org/t/p/w92${provider.logo_path}`}
              alt={provider.provider_name}
              className="w-full h-full object-cover"
              loading="lazy"
            />
          </a>
        ))}
      </div>
    </div>
  );
}
