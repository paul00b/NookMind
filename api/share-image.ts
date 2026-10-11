import type { VercelRequest, VercelResponse } from '@vercel/node';
import { readFileSync } from 'node:fs';
import { ImageResponse } from '@vercel/og';
import { ACCENTS, COPY, loadShareItem, parseKind, parseLang, parseSharer, type ShareItem, type ShareLang } from './_lib/shareItem.js';

// The picture under a shared link in a chat (Open Graph image, 1200x630): the poster, the title in
// the serif of the app, who recommends it, on the night background of NookMind.

const WIDTH = 1200;
const HEIGHT = 630;

const fonts = [
  { name: 'Playfair', data: readFileSync(new URL('./_lib/fonts/playfair_bold.ttf', import.meta.url)), weight: 700 as const, style: 'normal' as const },
  { name: 'Inter', data: readFileSync(new URL('./_lib/fonts/inter_regular.ttf', import.meta.url)), weight: 400 as const, style: 'normal' as const },
  { name: 'Inter', data: readFileSync(new URL('./_lib/fonts/inter_semibold.ttf', import.meta.url)), weight: 600 as const, style: 'normal' as const },
];
const logo = `data:image/png;base64,${readFileSync(new URL('./_lib/logo.png', import.meta.url)).toString('base64')}`;

type Node = { type: string; props: Record<string, unknown> };

function el(type: string, style: Record<string, unknown>, children?: unknown, extra: Record<string, unknown> = {}): Node {
  return { type, props: { style, children, ...extra } };
}

/** An image as a data URL, or null when it cannot be read (the card then does without it). */
async function inline(url: string | undefined): Promise<string | null> {
  if (!url) return null;
  try {
    const res = await fetch(url, { signal: AbortSignal.timeout(5000) });
    if (!res.ok) return null;
    const type = res.headers.get('content-type') ?? 'image/jpeg';
    if (!/image\/(jpeg|png)/.test(type)) return null;
    return `data:${type};base64,${Buffer.from(await res.arrayBuffer()).toString('base64')}`;
  } catch {
    return null;
  }
}

function titleSize(title: string): number {
  if (title.length <= 16) return 78;
  if (title.length <= 28) return 66;
  if (title.length <= 44) return 54;
  return 46;
}

function clip(text: string, max: number): string {
  return text.length <= max ? text : `${text.slice(0, max - 1).trimEnd()}…`;
}

export async function shareCard(item: ShareItem | null, sharer: string | undefined, lang: ShareLang): Promise<Node> {
  const copy = COPY[lang];
  const accent = ACCENTS[item?.kind ?? 'movie'];
  const [poster, backdrop] = await Promise.all([inline(item?.poster), inline(item?.backdrop ?? item?.poster)]);
  const title = clip(item?.title ?? 'NookMind', 70);
  const meta = [item?.year, item?.by, item?.extra].filter(Boolean).join('  ·  ');
  const kicker = sharer ? copy.recommends(sharer) : copy.discover;

  const layers: unknown[] = [];
  if (backdrop) {
    layers.push(el('img', { position: 'absolute', top: 0, left: 0, width: WIDTH, height: HEIGHT, objectFit: 'cover', opacity: 0.38 }, undefined, { src: backdrop, width: WIDTH, height: HEIGHT }));
  }
  layers.push(el('div', {
    position: 'absolute', top: 0, left: 0, width: WIDTH, height: HEIGHT, display: 'flex',
    backgroundImage: 'linear-gradient(90deg, rgba(13,14,20,0.97) 0%, rgba(13,14,20,0.9) 45%, rgba(13,14,20,0.62) 100%)',
  }));
  layers.push(el('div', {
    position: 'absolute', top: 0, left: 0, width: WIDTH, height: HEIGHT, display: 'flex',
    backgroundImage: `radial-gradient(circle at 18% 0%, ${accent.from}66 0%, ${accent.from}00 55%)`,
  }));

  const posterNode = poster
    ? el('img', {
      width: 300, height: 450, borderRadius: 22, objectFit: 'cover',
      boxShadow: '0 30px 70px rgba(0,0,0,0.65)', border: '1px solid rgba(255,255,255,0.12)',
    }, undefined, { src: poster, width: 300, height: 450 })
    : el('div', {
      width: 300, height: 450, borderRadius: 22, display: 'flex', alignItems: 'center', justifyContent: 'center',
      backgroundImage: `linear-gradient(135deg, ${accent.from}, ${accent.to})`,
    }, el('img', { width: 120, height: 120 }, undefined, { src: logo, width: 120, height: 120 }));

  const text = el('div', { display: 'flex', flexDirection: 'column', justifyContent: 'center', flex: 1, paddingLeft: 60 }, [
    el('div', { display: 'flex', alignItems: 'center' }, [
      el('div', {
        display: 'flex', padding: '10px 20px', borderRadius: 40, fontSize: 24, fontWeight: 600, color: accent.text,
        backgroundColor: 'rgba(255,255,255,0.08)', border: `1px solid ${accent.text}44`,
      }, kicker),
    ]),
    el('div', {
      display: 'flex', marginTop: 26, fontFamily: 'Playfair', fontWeight: 700, fontSize: titleSize(title),
      lineHeight: 1.08, color: '#F4F2EE',
    }, title),
    meta ? el('div', { display: 'flex', marginTop: 22, fontSize: 28, color: '#B9BBC8' }, meta) : el('div', { display: 'flex' }),
    el('div', { display: 'flex', alignItems: 'center', marginTop: 46 }, [
      el('img', { width: 44, height: 44, borderRadius: 10 }, undefined, { src: logo, width: 44, height: 44 }),
      el('div', { display: 'flex', marginLeft: 14, fontFamily: 'Playfair', fontWeight: 700, fontSize: 30, color: '#F4F2EE' }, 'NookMind'),
      el('div', {
        display: 'flex', marginLeft: 18, padding: '6px 14px', borderRadius: 10, fontSize: 20, fontWeight: 600,
        color: '#0D0E14', backgroundColor: accent.text,
      }, copy.kind[item?.kind ?? 'movie']),
    ]),
  ]);

  return el('div', {
    width: WIDTH, height: HEIGHT, display: 'flex', position: 'relative', backgroundColor: '#0D0E14', fontFamily: 'Inter',
  }, [
    ...layers,
    el('div', { position: 'absolute', top: 0, left: 0, width: WIDTH, height: HEIGHT, display: 'flex', alignItems: 'center', padding: '0 80px' }, [posterNode, text]),
  ]);
}

export async function renderShareCard(item: ShareItem | null, sharer: string | undefined, lang: ShareLang): Promise<Buffer> {
  const image = new ImageResponse((await shareCard(item, sharer, lang)) as any, { width: WIDTH, height: HEIGHT, fonts });
  return Buffer.from(await image.arrayBuffer());
}

export default async function handler(req: VercelRequest, res: VercelResponse) {
  const kind = parseKind(req.query.kind);
  const id = typeof req.query.id === 'string' ? req.query.id : '';
  const lang = parseLang(req.query.l);
  const sharer = parseSharer(req.query.by);
  try {
    const item = kind ? await loadShareItem(kind, id, lang) : null;
    const png = await renderShareCard(item, sharer, lang);
    res.setHeader('Content-Type', 'image/png');
    // Cached by Vercel's CDN: the same link gives the same picture.
    res.setHeader('Cache-Control', item ? 'public, max-age=86400, s-maxage=604800, stale-while-revalidate=86400' : 'public, max-age=300');
    return res.status(200).send(png);
  } catch (err) {
    console.error('[share-image] failed', err);
    return res.status(500).json({ error: 'Image failed' });
  }
}
