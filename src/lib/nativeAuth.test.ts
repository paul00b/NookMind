import { describe, expect, it, vi, beforeEach } from 'vitest';

vi.mock('@capgo/capacitor-social-login', () => ({
  SocialLogin: {
    initialize: vi.fn(),
    login: vi.fn(),
    logout: vi.fn(),
  },
}));

vi.mock('./platform', () => ({
  isNative: vi.fn(),
  isIOS: vi.fn(),
  isAndroid: vi.fn(),
  isWeb: vi.fn(),
}));

import { SocialLogin } from '@capgo/capacitor-social-login';
import { isIOS, isNative } from './platform';

async function loadModule() {
  vi.resetModules();
  return await import('./nativeAuth');
}

describe('initNativeAuth', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('is a no-op on web', async () => {
    vi.mocked(isNative).mockReturnValue(false);
    const { initNativeAuth } = await loadModule();
    await initNativeAuth();
    expect(SocialLogin.initialize).not.toHaveBeenCalled();
  });

  it('initializes SocialLogin once on native (idempotent)', async () => {
    vi.mocked(isNative).mockReturnValue(true);
    vi.mocked(isIOS).mockReturnValue(false);
    const { initNativeAuth } = await loadModule();
    await initNativeAuth();
    await initNativeAuth();
    expect(SocialLogin.initialize).toHaveBeenCalledTimes(1);
    const arg = vi.mocked(SocialLogin.initialize).mock.calls[0][0];
    expect(arg).toHaveProperty('google');
    expect(arg).not.toHaveProperty('apple');
  });

  it('initializes Apple provider on iOS', async () => {
    vi.mocked(isNative).mockReturnValue(true);
    vi.mocked(isIOS).mockReturnValue(true);
    const { initNativeAuth } = await loadModule();
    await initNativeAuth();
    expect(SocialLogin.initialize).toHaveBeenCalledTimes(1);
    const arg = vi.mocked(SocialLogin.initialize).mock.calls[0][0];
    expect(arg).toHaveProperty('apple');
  });
});

describe('nativeGoogleSignIn', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('returns idToken from online response', async () => {
    vi.mocked(SocialLogin.login).mockResolvedValue({
      provider: 'google',
      result: { responseType: 'online', idToken: 'fake-id', accessToken: { token: 'a' }, profile: {} },
    } as never);
    const { nativeGoogleSignIn } = await loadModule();
    const token = await nativeGoogleSignIn();
    expect(token).toBe('fake-id');
  });

  it('throws when idToken missing', async () => {
    vi.mocked(SocialLogin.login).mockResolvedValue({
      provider: 'google',
      result: { responseType: 'online', idToken: '', accessToken: { token: 'a' }, profile: {} },
    } as never);
    const { nativeGoogleSignIn } = await loadModule();
    await expect(nativeGoogleSignIn()).rejects.toThrow(/idToken/i);
  });

  it('throws on offline response (no idToken)', async () => {
    vi.mocked(SocialLogin.login).mockResolvedValue({
      provider: 'google',
      result: { responseType: 'offline', serverAuthCode: 'x' },
    } as never);
    const { nativeGoogleSignIn } = await loadModule();
    await expect(nativeGoogleSignIn()).rejects.toThrow();
  });
});

describe('nativeGoogleSignOut', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('is a no-op on web', async () => {
    vi.mocked(isNative).mockReturnValue(false);
    const { nativeGoogleSignOut } = await loadModule();
    await nativeGoogleSignOut();
    expect(SocialLogin.logout).not.toHaveBeenCalled();
  });

  it('calls logout on native', async () => {
    vi.mocked(isNative).mockReturnValue(true);
    const { nativeGoogleSignOut } = await loadModule();
    await nativeGoogleSignOut();
    expect(SocialLogin.logout).toHaveBeenCalledWith({ provider: 'google' });
  });

  it('swallows logout errors', async () => {
    vi.mocked(isNative).mockReturnValue(true);
    vi.mocked(SocialLogin.logout).mockRejectedValue(new Error('not logged in'));
    const { nativeGoogleSignOut } = await loadModule();
    await expect(nativeGoogleSignOut()).resolves.toBeUndefined();
  });
});

describe('nativeAppleSignIn', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('returns idToken, nonce, and profile from Apple response', async () => {
    vi.mocked(isNative).mockReturnValue(true);
    vi.mocked(isIOS).mockReturnValue(true);
    vi.mocked(SocialLogin.login).mockResolvedValue({
      provider: 'apple',
      result: {
        idToken: 'apple-id-token',
        accessToken: null,
        profile: { email: 'a@example.com', givenName: 'Ada', familyName: 'Lovelace' },
      },
    } as never);

    const { nativeAppleSignIn } = await loadModule();
    const result = await nativeAppleSignIn();
    expect(result.idToken).toBe('apple-id-token');
    expect(result.nonce).toHaveLength(32);
    expect(result.profile?.givenName).toBe('Ada');
    expect(SocialLogin.login).toHaveBeenCalledWith({
      provider: 'apple',
      options: {
        scopes: ['email', 'name'],
        nonce: expect.any(String),
      },
    });
  });

  it('throws off iOS', async () => {
    vi.mocked(isIOS).mockReturnValue(false);
    const { nativeAppleSignIn } = await loadModule();
    await expect(nativeAppleSignIn()).rejects.toThrow(/iOS/i);
  });

  it('throws when idToken missing', async () => {
    vi.mocked(isNative).mockReturnValue(true);
    vi.mocked(isIOS).mockReturnValue(true);
    vi.mocked(SocialLogin.login).mockResolvedValue({
      provider: 'apple',
      result: { idToken: null, accessToken: null, profile: {} },
    } as never);

    const { nativeAppleSignIn } = await loadModule();
    await expect(nativeAppleSignIn()).rejects.toThrow(/idToken/i);
  });
});

describe('nativeAppleSignOut', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('is a no-op off iOS', async () => {
    vi.mocked(isIOS).mockReturnValue(false);
    const { nativeAppleSignOut } = await loadModule();
    await nativeAppleSignOut();
    expect(SocialLogin.logout).not.toHaveBeenCalled();
  });

  it('calls logout on iOS', async () => {
    vi.mocked(isIOS).mockReturnValue(true);
    const { nativeAppleSignOut } = await loadModule();
    await nativeAppleSignOut();
    expect(SocialLogin.logout).toHaveBeenCalledWith({ provider: 'apple' });
  });
});
