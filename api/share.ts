import type { VercelRequest, VercelResponse } from '@vercel/node';
import { ACCENTS, ANDROID_PACKAGE, COPY, IOS_STORE_URL, STORE_URL, loadShareItem, parseKind, parseLang, parseSharer } from './_lib/shareItem.js';

// The page behind a shared link (/s/:kind/:id, rewritten here by vercel.json): Open Graph tags for
// the preview in chats, and for whoever opens it, the poster, a few words and a button that opens
// NookMind on the add sheet, or the store when the app is not installed: Google Play, or on an iPhone
// the TestFlight or App Store link set in IOS_STORE_URL.

function escapeHtml(value: string): string {
  return value.replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[c]!);
}

function clip(text: string, max: number): string {
  return text.length <= max ? text : `${text.slice(0, max - 1).trimEnd()}…`;
}

/** A value for an inline script, unable to close the script element. */
function scriptValue(value: unknown): string {
  return JSON.stringify(value).replace(/</g, '\\u003c');
}

export default async function handler(req: VercelRequest, res: VercelResponse) {
  const kind = parseKind(req.query.kind);
  const id = typeof req.query.id === 'string' ? req.query.id : '';
  const lang = parseLang(req.query.l ?? req.headers['accept-language']);
  const sharer = parseSharer(req.query.by);
  const copy = COPY[lang];
  const item = kind ? await loadShareItem(kind, id, lang) : null;

  const host = (req.headers['x-forwarded-host'] as string | undefined) ?? req.headers.host ?? '';
  const origin = `https://${host}`;
  const query = new URLSearchParams({ kind: kind ?? 'movie', id, l: lang, ...(sharer ? { by: sharer } : {}) });
  const imageUrl = `${origin}/api/share-image?${query}`;
  const pageUrl = `${origin}${req.url ?? ''}`;
  const accent = ACCENTS[item?.kind ?? 'movie'];

  const appPath = item ? `add/${item.kind}/${encodeURIComponent(item.id)}` : '';
  const appUrl = `nookmind://${appPath}`;
  const intentUrl = `intent://${appPath}#Intent;scheme=nookmind;package=${ANDROID_PACKAGE};S.browser_fallback_url=${encodeURIComponent(STORE_URL)};end`;

  const title = item ? `${item.title}${item.year ? ` (${item.year})` : ''}` : 'NookMind';
  const description = item ? copy.description(sharer, item.title) : copy.tagline;
  const meta = item ? [item.year, item.by, item.extra].filter(Boolean).join(' · ') : '';
  const kicker = sharer ? copy.recommends(sharer) : copy.discover;
  const background = item?.backdrop ?? item?.poster;

  const html = `<!doctype html>
<html lang="${lang}">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">
<title>${escapeHtml(title)} · NookMind</title>
<meta name="description" content="${escapeHtml(description)}">
<meta name="theme-color" content="#0D0E14">
<meta property="og:type" content="website">
<meta property="og:site_name" content="NookMind">
<meta property="og:title" content="${escapeHtml(title)}">
<meta property="og:description" content="${escapeHtml(item?.overview ? clip(item.overview, 180) : description)}">
<meta property="og:image" content="${escapeHtml(imageUrl)}">
<meta property="og:image:width" content="1200">
<meta property="og:image:height" content="630">
<meta property="og:url" content="${escapeHtml(pageUrl)}">
<meta name="twitter:card" content="summary_large_image">
<meta name="twitter:image" content="${escapeHtml(imageUrl)}">
<link rel="icon" href="/favicon.svg">
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;600&family=Playfair+Display:wght@700&display=swap" rel="stylesheet">
<style>
  :root { --bg: #0D0E14; --text: #F4F2EE; --muted: #A3A5B4; --accent: ${accent.text}; --from: ${accent.from}; --to: ${accent.to}; }
  * { box-sizing: border-box; }
  html, body { margin: 0; background: var(--bg); color: var(--text); font-family: Inter, system-ui, sans-serif; -webkit-font-smoothing: antialiased; }
  body { position: relative; min-height: 100vh; min-height: 100dvh; overflow-x: hidden; }
  .stage { position: absolute; top: 0; left: 0; right: 0; height: 100vh; overflow: hidden; }
  .backdrop { position: absolute; top: -60px; left: -15%; right: -15%; bottom: 0; background: center / cover no-repeat; filter: blur(42px) saturate(1.25); opacity: .5; }
  .shade { position: absolute; inset: 0; background:
      radial-gradient(120% 60% at 50% 0%, color-mix(in srgb, var(--from) 35%, transparent) 0%, transparent 70%),
      linear-gradient(180deg, rgba(13,14,20,.15) 0%, rgba(13,14,20,.75) 45%, var(--bg) 92%); }
  main { position: relative; max-width: 460px; margin: 0 auto; padding: max(28px, env(safe-area-inset-top)) 24px max(32px, env(safe-area-inset-bottom)); text-align: center; }
  .brand { display: inline-flex; align-items: center; gap: 10px; color: var(--text); text-decoration: none; font-family: 'Playfair Display', Georgia, serif; font-weight: 700; font-size: 20px; opacity: .92; }
  .brand img { width: 30px; height: 30px; border-radius: 8px; }
  .poster { display: block; width: 62%; max-width: 250px; aspect-ratio: 2 / 3; object-fit: cover; margin: 30px auto 0; border-radius: 18px;
    box-shadow: 0 34px 80px rgba(0,0,0,.6), 0 0 0 1px rgba(255,255,255,.1); animation: rise .9s cubic-bezier(.2,.8,.2,1) both; background: #171A24; }
  .poster.empty { display: grid; place-items: center; background: linear-gradient(135deg, var(--from), var(--to)); }
  .kicker { display: inline-block; margin-top: 30px; padding: 8px 16px; border-radius: 999px; font-size: 14px; font-weight: 600; color: var(--accent);
    background: rgba(255,255,255,.07); border: 1px solid color-mix(in srgb, var(--accent) 35%, transparent); animation: fade .8s .25s both; }
  h1 { margin: 16px 0 0; font-family: 'Playfair Display', Georgia, serif; font-weight: 700; font-size: clamp(30px, 8.5vw, 40px); line-height: 1.1; animation: fade .8s .35s both; }
  .meta { margin-top: 10px; color: var(--muted); font-size: 15px; animation: fade .8s .45s both; }
  .overview { margin: 18px auto 0; max-width: 400px; color: #C9CAD4; font-size: 15px; line-height: 1.55; display: -webkit-box; -webkit-line-clamp: 4; -webkit-box-orient: vertical; overflow: hidden; animation: fade .8s .55s both; }
  .cta { display: flex; align-items: center; justify-content: center; gap: 10px; margin-top: 30px; height: 58px; border-radius: 18px; color: #fff; text-decoration: none;
    font-weight: 600; font-size: 17px; background: linear-gradient(120deg, var(--from), var(--to)); box-shadow: 0 14px 40px color-mix(in srgb, var(--from) 45%, transparent);
    transition: transform .15s ease; animation: fade .8s .65s both; }
  .cta:active { transform: scale(.97); }
  .store { display: inline-block; margin-top: 16px; color: var(--muted); font-size: 14px; text-decoration: none; border-bottom: 1px solid rgba(163,165,180,.35); padding-bottom: 2px; }
  footer { margin-top: 40px; color: #6F7283; font-size: 13px; }
  @keyframes rise { from { opacity: 0; transform: translateY(18px) scale(.97); } to { opacity: 1; transform: none; } }
  @keyframes fade { from { opacity: 0; transform: translateY(8px); } to { opacity: 1; transform: none; } }
  @media (prefers-reduced-motion: reduce) { * { animation: none !important; } }
</style>
</head>
<body>
<div class="stage">
${background ? `<div class="backdrop" style="background-image:url('${escapeHtml(background)}')"></div>` : ''}
<div class="shade"></div>
</div>
<main>
  <a class="brand" data-store href="${escapeHtml(STORE_URL)}"><img src="/logo.png" alt="">NookMind</a>
  ${item
    ? `${item.poster ? `<img class="poster" src="${escapeHtml(item.poster)}" alt="">` : `<div class="poster empty"><img src="/logo.png" alt="" width="80" height="80"></div>`}
  <div class="kicker">${escapeHtml(kicker)}</div>
  <h1>${escapeHtml(item.title)}</h1>
  ${meta ? `<div class="meta">${escapeHtml(meta)}</div>` : ''}
  ${item.overview ? `<p class="overview">${escapeHtml(item.overview)}</p>` : ''}
  <a class="cta" id="open" href="${escapeHtml(appUrl)}">${escapeHtml(copy.cta)}</a>
  <a class="store" data-store href="${escapeHtml(STORE_URL)}">${escapeHtml(copy.store)}</a>`
    : `<h1 style="margin-top:40px">${escapeHtml(copy.notFound)}</h1>
  <a class="cta" data-store href="${escapeHtml(STORE_URL)}">${escapeHtml(copy.storeOnly)}</a>`}
  <footer>${escapeHtml(copy.tagline)}</footer>
</main>
<script>
  (function () {
    var ua = navigator.userAgent;
    var open = document.getElementById('open');
    // Android opens the app through an intent, which falls back to the store when it is missing.
    if (/android/i.test(ua)) { if (open) open.href = ${scriptValue(intentUrl)}; return; }
    // An iPhone or an iPad (which says it is a Mac) gets the TestFlight or App Store link instead of
    // Google Play, or no download at all while there is none.
    var ios = /iPhone|iPad|iPod/.test(ua) || (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1);
    if (!ios) return;
    var store = ${scriptValue(IOS_STORE_URL ?? null)};
    document.querySelectorAll('[data-store]').forEach(function (link) {
      if (store) link.href = store;
      else if (link.classList.contains('brand')) link.removeAttribute('href');
      else link.remove();
    });
  })();
</script>
</body>
</html>`;

  res.setHeader('Content-Type', 'text/html; charset=utf-8');
  res.setHeader('Cache-Control', item ? 'public, max-age=3600, s-maxage=86400, stale-while-revalidate=604800' : 'public, max-age=60');
  return res.status(item ? 200 : 404).send(html);
}
