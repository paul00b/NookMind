import type { VercelRequest, VercelResponse } from '@vercel/node';

const DEFAULT_ALLOWED_ORIGINS = [
  'capacitor://localhost',
  'http://localhost',
  'https://localhost',
  'https://paulbr.fr',
];

function getAllowedOrigins(): string[] {
  const envBaseUrl = process.env.VITE_API_BASE_URL?.trim();
  const envSiteUrl = process.env.VITE_SITE_URL?.trim();

  return [...DEFAULT_ALLOWED_ORIGINS, envBaseUrl, envSiteUrl].filter(
    (value): value is string => Boolean(value)
  );
}

export function applyCors(req: VercelRequest, res: VercelResponse, methods: string[]): boolean {
  const origin = req.headers.origin;
  const allowedOrigins = getAllowedOrigins();

  if (origin && allowedOrigins.includes(origin)) {
    res.setHeader('Access-Control-Allow-Origin', origin);
    res.setHeader('Vary', 'Origin');
  }

  res.setHeader('Access-Control-Allow-Methods', [...methods, 'OPTIONS'].join(', '));
  res.setHeader('Access-Control-Allow-Headers', 'Authorization, Content-Type');

  if (req.method === 'OPTIONS') {
    res.status(204).end();
    return true;
  }

  return false;
}
