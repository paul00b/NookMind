import { isNative } from './platform';

const NATIVE_FALLBACK_API_BASE_URL = 'https://paulbr.fr';

function normalizeBaseUrl(value: string): string {
  return value.replace(/\/+$/, '');
}

export function getApiUrl(path: string): string {
  const envBaseUrl = import.meta.env.VITE_API_BASE_URL as string | undefined;
  const normalizedPath = path.startsWith('/') ? path : `/${path}`;

  if (envBaseUrl) {
    return `${normalizeBaseUrl(envBaseUrl)}${normalizedPath}`;
  }

  if (isNative()) {
    return `${NATIVE_FALLBACK_API_BASE_URL}${normalizedPath}`;
  }

  return normalizedPath;
}
