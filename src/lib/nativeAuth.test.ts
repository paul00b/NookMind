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
import { isNative, isIOS } from './platform';

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

  it('initializes SocialLogin once on native', async () => {
    vi.mocked(isNative).mockReturnValue(true);
    const { initNativeAuth } = await loadModule();
    await initNativeAuth();
    await initNativeAuth();
    expect(SocialLogin.initialize).toHaveBeenCalledTimes(1);
    const arg = vi.mocked(SocialLogin.initialize).mock.calls[0][0];
    expect(arg).toHaveProperty('google');
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

  it('throws on non-iOS', async () => {
    vi.mocked(isIOS).mockReturnValue(false);
    const { nativeAppleSignIn } = await loadModule();
    await expect(nativeAppleSignIn()).rejects.toThrow(/iOS/i);
  });

  it('returns identityToken + raw nonce on iOS', async () => {
    vi.mocked(isIOS).mockReturnValue(true);
    vi.mocked(SocialLogin.login).mockResolvedValue({
      provider: 'apple',
      result: { idToken: 'apple-id', accessToken: null, profile: { user: 'u', email: null, givenName: null, familyName: null } },
    } as never);
    const { nativeAppleSignIn } = await loadModule();
    const result = await nativeAppleSignIn();
    expect(result.identityToken).toBe('apple-id');
    expect(result.nonce).toMatch(/^[0-9a-f]{64}$/);
    const callArgs = vi.mocked(SocialLogin.login).mock.calls[0][0] as { provider: string; options: { nonce?: string; scopes?: string[] } };
    expect(callArgs.provider).toBe('apple');
    expect(callArgs.options.nonce).toBe(result.nonce);
    expect(callArgs.options.scopes).toEqual(['name', 'email']);
  });

  it('throws when Apple returns no idToken', async () => {
    vi.mocked(isIOS).mockReturnValue(true);
    vi.mocked(SocialLogin.login).mockResolvedValue({
      provider: 'apple',
      result: { idToken: null, accessToken: null, profile: { user: 'u', email: null, givenName: null, familyName: null } },
    } as never);
    const { nativeAppleSignIn } = await loadModule();
    await expect(nativeAppleSignIn()).rejects.toThrow();
  });
});
