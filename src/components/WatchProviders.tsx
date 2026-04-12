import type { WatchProvider, WatchProvidersResult } from '../types';
import { useTranslation } from 'react-i18next';

const PROVIDER_SEARCH_URLS: Record<number, (title: string) => string> = {
  8:    t => `https://www.netflix.com/search?q=${encodeURIComponent(t)}`,       // Netflix
  1796: t => `https://www.netflix.com/search?q=${encodeURIComponent(t)}`,       // Netflix Ads
  9:    t => `https://www.primevideo.com/search?phrase=${encodeURIComponent(t)}`,// Prime Video
  119:  t => `https://www.primevideo.com/search?phrase=${encodeURIComponent(t)}`,// Prime Video
  337:  t => `https://www.disneyplus.com/search?q=${encodeURIComponent(t)}`,    // Disney+
  350:  t => `https://tv.apple.com/search?term=${encodeURIComponent(t)}`,       // Apple TV+
  381:  t => `https://www.canalplus.com/recherche/${encodeURIComponent(t)}`,    // Canal+
  56:   t => `https://www.canalplus.com/recherche/${encodeURIComponent(t)}`,    // OCS (now Canal+)
  236:  t => `https://www.paramountplus.com/search?q=${encodeURIComponent(t)}`, // Paramount+
  283:  t => `https://www.crunchyroll.com/search?q=${encodeURIComponent(t)}`,   // Crunchyroll
  531:  t => `https://www.paramountplus.com/search?q=${encodeURIComponent(t)}`, // Paramount+ Amazon
  1899: t => `https://www.max.com/search?q=${encodeURIComponent(t)}`,           // Max
  1825: t => `https://www.max.com/search?q=${encodeURIComponent(t)}`,           // Max Amazon
};

function getProviderUrl(provider: WatchProvider, title: string, fallbackLink: string | null): string {
  const builder = PROVIDER_SEARCH_URLS[provider.provider_id];
  if (builder) return builder(title);
  return fallbackLink ?? '#';
}

interface Props {
  providers: WatchProvidersResult | null;
  title: string;
  loading?: boolean;
}

export default function WatchProviders({ providers, title, loading }: Props) {
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
            href={getProviderUrl(provider, title, providers.link)}
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
