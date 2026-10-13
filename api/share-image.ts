import type { VercelRequest, VercelResponse } from '@vercel/node';
import { readFileSync } from 'node:fs';
import { join } from 'node:path';
import { ImageResponse } from '@vercel/og';
import {
  ACCENTS, COPY, FRIEND_ACCENT, FRIEND_COPY, loadShareItem, parseFriendCode, parseKind, parseLang, parseSharer,
  type ShareItem, type ShareLang,
} from './_lib/shareItem.js';

// The picture under a shared link in a chat (Open Graph image, 1200x630): the poster, the title in
// the serif of the app, who recommends it, on the night background of NookMind. A friend's
// invitation gets the NookMind badge and the code instead of a poster.

const WIDTH = 1200;
const HEIGHT = 630;

// Shipped with the function by vercel.json (includeFiles), next to it under the project root.
const asset = (name: string) => readFileSync(join(process.cwd(), 'api', '_lib', name));
const fonts = [
  { name: 'Playfair', data: asset('fonts/playfair_bold.ttf'), weight: 700 as const, style: 'normal' as const },
  { name: 'Inter', data: asset('fonts/inter_regular.ttf'), weight: 400 as const, style: 'normal' as const },
  { name: 'Inter', data: asset('fonts/inter_semibold.ttf'), weight: 600 as const, style: 'normal' as const },
];
const logo = `data:image/png;base64,${asset('logo.png').toString('base64')}`;

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

/** The invitation behind the QR code of the Social tab: who invites, and the code. */
export function friendCard(code: string, sharer: string | undefined, lang: ShareLang): Node {
  const friend = FRIEND_COPY[lang];
  const accent = FRIEND_ACCENT;
  const full = { position: 'absolute', top: 0, left: 0, width: WIDTH, height: HEIGHT, display: 'flex' };
  const badge = el('div', {
    width: 300, height: 300, borderRadius: 72, display: 'flex', alignItems: 'center', justifyContent: 'center',
    backgroundImage: `linear-gradient(135deg, ${accent.from}, ${accent.to})`, boxShadow: '0 30px 70px rgba(0,0,0,0.55)',
  }, el('img', { width: 190, height: 190 }, undefined, { src: logo, width: 190, height: 190 }));
  const text = el('div', { display: 'flex', flexDirection: 'column', justifyContent: 'center', flex: 1, paddingLeft: 64 }, [
    el('div', { display: 'flex' }, [
      el('div', {
        display: 'flex', padding: '10px 20px', borderRadius: 40, fontSize: 24, fontWeight: 600, color: accent.text,
        backgroundColor: 'rgba(255,255,255,0.08)', border: `1px solid ${accent.text}44`,
      }, friend.kicker(sharer)),
    ]),
    el('div', {
      display: 'flex', marginTop: 26, fontFamily: 'Playfair', fontWeight: 700, fontSize: titleSize(friend.title(sharer)),
      lineHeight: 1.08, color: '#F4F2EE',
    }, friend.title(sharer)),
    el('div', {
      display: 'flex', flexDirection: 'column', marginTop: 30, padding: '16px 26px', borderRadius: 22, alignSelf: 'flex-start',
      backgroundColor: 'rgba(255,255,255,0.06)', border: '1px solid rgba(255,255,255,0.12)',
    }, [
      el('div', { display: 'flex', fontSize: 18, fontWeight: 600, color: '#B9BBC8', letterSpacing: 2 }, friend.code.toUpperCase()),
      el('div', { display: 'flex', marginTop: 6, fontSize: 48, fontWeight: 600, color: '#F4F2EE', letterSpacing: 14 }, code),
    ]),
  ]);
  return el('div', { width: WIDTH, height: HEIGHT, display: 'flex', position: 'relative', backgroundColor: '#0D0E14', fontFamily: 'Inter' }, [
    el('div', { ...full, backgroundImage: `radial-gradient(circle at 15% 0%, ${accent.from}77 0%, ${accent.from}00 55%)` }),
    el('div', { ...full, backgroundImage: `radial-gradient(circle at 100% 100%, ${accent.to}44 0%, ${accent.to}00 50%)` }),
    el('div', { ...full, alignItems: 'center', padding: '0 90px' }, [badge, text]),
  ]);
}

async function toPng(node: Node): Promise<Buffer> {
  const image = new ImageResponse(node as any, { width: WIDTH, height: HEIGHT, fonts });
  return Buffer.from(await image.arrayBuffer());
}

export async function renderShareCard(item: ShareItem | null, sharer: string | undefined, lang: ShareLang): Promise<Buffer> {
  return toPng(await shareCard(item, sharer, lang));
}

export default async function handler(req: VercelRequest, res: VercelResponse) {
  if (req.query.kind === 'friend') {
    const code = parseFriendCode(req.query.id);
    if (!code) return res.status(404).json({ error: 'No such invitation' });
    try {
      const png = await toPng(friendCard(code, parseSharer(req.query.by), parseLang(req.query.l)));
      res.setHeader('Content-Type', 'image/png');
      res.setHeader('Cache-Control', 'public, max-age=86400, s-maxage=604800, stale-while-revalidate=86400');
      return res.status(200).send(png);
    } catch (err) {
      console.error('[share-image] friend card failed', err);
      return res.status(500).json({ error: 'Image failed' });
    }
  }

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
