import { isNative } from './platform';

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
    throw new Error('VITE_API_BASE_URL is required for native builds.');
  }

  return normalizedPath;
}
