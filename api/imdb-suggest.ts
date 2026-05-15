import type { VercelRequest, VercelResponse } from '@vercel/node';
import { applyCors } from './_lib/cors';

export default async function handler(req: VercelRequest, res: VercelResponse) {
  if (applyCors(req, res, ['GET'])) return;

  const { firstChar, query } = req.query;
  if (!firstChar || !query) return res.status(400).json({ error: 'Missing params' });

  const url = `https://v3.sg.media-imdb.com/suggestion/titles/${firstChar}/${query}.json`;
  try {
    const r = await fetch(url);
    if (!r.ok) return res.status(r.status).end();
    const data = await r.json();
    res.setHeader('Cache-Control', 's-maxage=3600');
    return res.json(data);
  } catch {
    return res.status(500).json({ error: 'fetch failed' });
  }
}
