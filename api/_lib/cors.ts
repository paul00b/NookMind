import type { VercelRequest, VercelResponse } from '@vercel/node';

export function applyCors(req: VercelRequest, res: VercelResponse, methods: string[]): boolean {
  const origin = req.headers.origin;

  // Native WebViews can emit varying origins (`capacitor://localhost`,
  // `http(s)://localhost`, or even `null`), so we allow any origin here.
  // These routes do not rely on cookies; authenticated routes use Bearer tokens.
  res.setHeader('Access-Control-Allow-Origin', origin || '*');
  res.setHeader('Vary', 'Origin');

  res.setHeader('Access-Control-Allow-Methods', [...methods, 'OPTIONS'].join(', '));
  res.setHeader('Access-Control-Allow-Headers', 'Authorization, Content-Type, X-Requested-With');
  res.setHeader('Access-Control-Max-Age', '86400');

  if (req.method === 'OPTIONS') {
    res.status(204).end();
    return true;
  }

  return false;
}
