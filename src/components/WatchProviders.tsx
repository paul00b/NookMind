import { useEffect, useState } from 'react';
import type { WatchProvider, WatchProvidersResult } from '../types';
import { useTranslation } from 'react-i18next';
import { fetchWatchProviderDeepLinks } from '../lib/tmdb';

// Fallback: provider search URLs that open the native app via universal links
const PROVIDER_SEARCH_URLS: Record<number, (title: string) => string> = {
  8:    t => `https://www.netflix.com/search?q=${encodeURIComponent(t)}`,
  1796: t => `https://www.netflix.com/search?q=${encodeURIComponent(t)}`,
  9:    t => `https://www.primevideo.com/search?phrase=${encodeURIComponent(t)}`,
  119:  t => `https://www.primevideo.com/search?phrase=${encodeURIComponent(t)}`,
  337:  t => `https://www.disneyplus.com/search?q=${encodeURIComponent(t)}`,
  350:  t => `https://tv.apple.com/search?term=${encodeURIComponent(t)}`,
  381:  t => `https://www.canalplus.com/recherche/${encodeURIComponent(t)}`,
  56:   t => `https://www.canalplus.com/recherche/${encodeURIComponent(t)}`,
  236:  t => `https://www.paramountplus.com/search?q=${encodeURIComponent(t)}`,
  283:  t => `https://www.crunchyroll.com/search?q=${encodeURIComponent(t)}`,
  1899: t => `https://www.max.com/search?q=${encodeURIComponent(t)}`,
  1825: t => `https://www.max.com/search?q=${encodeURIComponent(t)}`,
};

function getFallbackUrl(provider: WatchProvider, title: string, tmdbLink: string | null): string {
  const builder = PROVIDER_SEARCH_URLS[provider.provider_id];
  if (builder) return builder(title);
  return tmdbLink ?? '#';
}

interface Props {
  providers: WatchProvidersResult | null;
  title: string;
  loading?: boolean;
}

export default function WatchProviders({ providers, title, loading }: Props) {
  const { t } = useTranslation();
  const [deepLinks, setDeepLinks] = useState<Record<string, string>>({});

  useEffect(() => {
    if (!providers?.link || providers.flatrate.length === 0) return;
    let active = true;
    fetchWatchProviderDeepLinks(providers.link).then(links => {
      if (active) setDeepLinks(links);
    });
    return () => { active = false; };
  }, [providers?.link, providers?.flatrate.length]);

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
        {providers.flatrate.map(provider => {
          const href = deepLinks[String(provider.provider_id)]
            || getFallbackUrl(provider, title, providers.link);
          return (
            <a
              key={provider.provider_id}
              href={href}
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
          );
        })}
      </div>
    </div>
  );
}
