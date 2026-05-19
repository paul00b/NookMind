import { describe, expect, it, vi, beforeEach, afterEach } from 'vitest';

vi.mock('./platform', () => ({ isNative: vi.fn() }));

import { isNative } from './platform';

describe('getApiUrl', () => {
    beforeEach(async () => {
        vi.resetModules();
        vi.mocked(isNative).mockReturnValue(false);
    });

    afterEach(() => {
        vi.unstubAllEnvs();
    });

    it('returns relative path on web when no env var is set', async () => {
        vi.stubEnv('VITE_API_BASE_URL', '');
        const { getApiUrl } = await import('./api');
        expect(getApiUrl('/api/push/test')).toBe('/api/push/test');
    });

    it('prepends a leading slash if missing', async () => {
        vi.stubEnv('VITE_API_BASE_URL', '');
        const { getApiUrl } = await import('./api');
        expect(getApiUrl('api/push/test')).toBe('/api/push/test');
    });

    it('uses env base URL when set', async () => {
        vi.stubEnv('VITE_API_BASE_URL', 'https://nookmind.paulbr.fr');
        const { getApiUrl } = await import('./api');
        expect(getApiUrl('/api/push/test')).toBe('https://nookmind.paulbr.fr/api/push/test');
    });

    it('strips trailing slash from base URL', async () => {
        vi.stubEnv('VITE_API_BASE_URL', 'https://nookmind.paulbr.fr/');
        const { getApiUrl } = await import('./api');
        expect(getApiUrl('/api/push/test')).toBe('https://nookmind.paulbr.fr/api/push/test');
    });

    it('strips trailing quote from base URL (Vercel env var copy-paste bug)', async () => {
        vi.stubEnv('VITE_API_BASE_URL', 'https://nookmind.paulbr.fr"');
        const { getApiUrl } = await import('./api');
        expect(getApiUrl('/api/push/test')).toBe('https://nookmind.paulbr.fr/api/push/test');
    });

    it('strips surrounding quotes from base URL', async () => {
        vi.stubEnv('VITE_API_BASE_URL', '"https://nookmind.paulbr.fr"');
        const { getApiUrl } = await import('./api');
        expect(getApiUrl('/api/push/test')).toBe('https://nookmind.paulbr.fr/api/push/test');
    });

    it('strips single quotes from base URL', async () => {
        vi.stubEnv('VITE_API_BASE_URL', "'https://nookmind.paulbr.fr'");
        const { getApiUrl } = await import('./api');
        expect(getApiUrl('/api/push/test')).toBe('https://nookmind.paulbr.fr/api/push/test');
    });

    it('throws on native when env var is missing', async () => {
        vi.stubEnv('VITE_API_BASE_URL', '');
        vi.mocked(isNative).mockReturnValue(true);
        const { getApiUrl } = await import('./api');
        expect(() => getApiUrl('/api/push/test')).toThrow('VITE_API_BASE_URL is required for native builds');
    });
});
