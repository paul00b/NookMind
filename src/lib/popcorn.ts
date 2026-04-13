export interface PopcornContext {
  kind: 'movie' | 'series' | 'episode';
  title: string;
  year?: string | null;
  season?: number | null;
  episode?: number | null;
}

export interface PopcornLaunchResult {
  mode: 'launched' | 'shared' | 'copied' | 'unsupported';
  query: string;
  url?: string;
}

const STORAGE_KEY = 'nookmind_popcorn_launcher_template';
const ENV_TEMPLATE = import.meta.env.VITE_POPCORNTIME_LAUNCHER_TEMPLATE as string | undefined;

function normalizeWhitespace(value: string): string {
  return value.replace(/\s+/g, ' ').trim();
}

function extractYear(value: string | null | undefined): string | null {
  if (!value) return null;
  const match = value.match(/\b(\d{4})\b/);
  return match?.[1] ?? null;
}

function padEpisodePart(value: number | null | undefined): string {
  if (typeof value !== 'number' || Number.isNaN(value)) return '00';
  return String(value).padStart(2, '0');
}

export function buildMoviePopcornQuery(title: string, releaseDate?: string | null): string {
  const year = extractYear(releaseDate);
  return normalizeWhitespace([title, year].filter(Boolean).join(' '));
}

export function buildSeriesPopcornQuery(title: string): string {
  return normalizeWhitespace(title);
}

export function buildEpisodePopcornQuery(
  title: string,
  season?: number | null,
  episode?: number | null,
  releaseDate?: string | null
): string {
  const seasonPart = typeof season === 'number' ? `S${padEpisodePart(season)}` : '';
  const episodePart = typeof episode === 'number' ? `E${padEpisodePart(episode)}` : '';
  const year = extractYear(releaseDate);
  return normalizeWhitespace([title, `${seasonPart}${episodePart}`, year].filter(Boolean).join(' '));
}

export function getPopcornLauncherTemplate(): string {
  if (typeof window === 'undefined') return ENV_TEMPLATE?.trim() ?? '';
  return (localStorage.getItem(STORAGE_KEY) ?? ENV_TEMPLATE ?? '').trim();
}

export function setPopcornLauncherTemplate(template: string) {
  if (typeof window === 'undefined') return;
  const trimmed = template.trim();
  if (trimmed) localStorage.setItem(STORAGE_KEY, trimmed);
  else localStorage.removeItem(STORAGE_KEY);
}

export function buildPopcornLauncherUrl(template: string, query: string, context: PopcornContext): string {
  const replacements: Record<string, string> = {
    '{{query}}': encodeURIComponent(query),
    '{{title}}': encodeURIComponent(context.title),
    '{{year}}': encodeURIComponent(context.year ?? ''),
    '{{season}}': encodeURIComponent(context.season != null ? String(context.season) : ''),
    '{{episode}}': encodeURIComponent(context.episode != null ? String(context.episode) : ''),
    '{{kind}}': encodeURIComponent(context.kind),
  };

  return Object.entries(replacements).reduce(
    (url, [token, value]) => url.replaceAll(token, value),
    template
  );
}

async function copyText(value: string): Promise<boolean> {
  if (typeof navigator !== 'undefined' && navigator.clipboard?.writeText) {
    try {
      await navigator.clipboard.writeText(value);
      return true;
    } catch {
      // Fall back to a textarea copy flow below.
    }
  }

  if (typeof document === 'undefined') return false;
  try {
    const textarea = document.createElement('textarea');
    textarea.value = value;
    textarea.setAttribute('readonly', 'true');
    textarea.style.position = 'fixed';
    textarea.style.opacity = '0';
    document.body.appendChild(textarea);
    textarea.select();
    const copied = document.execCommand('copy');
    document.body.removeChild(textarea);
    return copied;
  } catch {
    return false;
  }
}

async function shareText(value: string, context: PopcornContext): Promise<boolean> {
  if (typeof navigator === 'undefined' || typeof navigator.share !== 'function') return false;

  try {
    await navigator.share({
      title: context.title,
      text: value,
    });
    return true;
  } catch {
    return false;
  }
}

export async function launchPopcornTime(query: string, context: PopcornContext): Promise<PopcornLaunchResult> {
  const template = getPopcornLauncherTemplate();
  if (template) {
    const url = buildPopcornLauncherUrl(template, query, context);
    window.location.assign(url);
    return { mode: 'launched', query, url };
  }

  const shared = await shareText(query, context);
  if (shared) {
    return {
      mode: 'shared',
      query,
    };
  }

  const copied = await copyText(query);
  return {
    mode: copied ? 'copied' : 'unsupported',
    query,
  };
}
