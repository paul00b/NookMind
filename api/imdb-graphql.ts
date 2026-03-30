import type { VercelRequest, VercelResponse } from '@vercel/node';

export default async function handler(req: VercelRequest, res: VercelResponse) {
  if (req.method !== 'POST') return res.status(405).end();

  try {
    const r = await fetch('https://caching.graphql.imdb.com/', {
      method: 'POST',
      headers: {
        'content-type': 'application/json',
        'x-imdb-client-name': 'imdb-web-nextjs-client',
        'x-imdb-user-country': 'US',
        'x-imdb-user-language': 'en-US',
      },
      body: JSON.stringify(req.body),
    });
    if (!r.ok) return res.status(r.status).end();
    const data = await r.json();
    res.setHeader('Cache-Control', 's-maxage=3600');
    return res.json(data);
  } catch {
    return res.status(500).json({ error: 'fetch failed' });
  }
}
