import type { VercelRequest, VercelResponse } from '@vercel/node';

export default async function handler(req: VercelRequest, res: VercelResponse) {
  if (req.method !== 'GET') return res.status(405).end();

  const { url } = req.query;
  if (!url || typeof url !== 'string') return res.status(400).json({ error: 'missing url' });

  try {
    const r = await fetch(url, {
      headers: {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
        'Accept': 'text/html,application/xhtml+xml',
        'Accept-Language': 'fr-FR,fr;q=0.9,en;q=0.8',
      },
    });
    if (!r.ok) return res.status(r.status).json({ error: 'fetch failed' });

    const html = await r.text();

    // Extract provider links from ott_offer sections
    // Pattern: <div class="ott_offer">...<a href="JUSTWATCH_URL">...<img src="LOGO">...</a>...</div>
    const providers: Record<string, string> = {};
    const regex = /<a\s[^>]*href="(https:\/\/click\.justwatch\.com\/a\?[^"]+)"[^>]*>\s*<img[^>]*src="([^"]+)"[^>]*>/g;
    let match;

    while ((match = regex.exec(html)) !== null) {
      const href = match[1];
      const imgSrc = match[2];

      // Decode the cx parameter to get provider name
      try {
        const cxMatch = href.match(/cx=([^&]+)/);
        if (!cxMatch) continue;
        const cxData = JSON.parse(atob(cxMatch[1]));
        const clickoutCtx = cxData?.data?.find((d: { schema: string }) =>
          d.schema?.includes('clickout_context')
        );
        if (!clickoutCtx) continue;

        const providerId = clickoutCtx.data?.providerId;
        const monetizationType = clickoutCtx.data?.monetizationType;

        // Only keep flatrate (subscription) links, skip rent/buy
        if (monetizationType !== 'flatrate') continue;

        // Use providerId as key to deduplicate (keep first = best quality)
        const key = String(providerId);
        if (!providers[key]) {
          providers[key] = href;
        }
      } catch {
        // Skip malformed entries
      }
    }

    res.setHeader('Cache-Control', 's-maxage=86400');
    return res.json({ providers });
  } catch {
    return res.status(500).json({ error: 'fetch failed' });
  }
}
