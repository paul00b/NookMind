import { describe, expect, it, vi, beforeEach } from 'vitest';

vi.mock('@capacitor-firebase/messaging', () => ({
  FirebaseMessaging: {
    requestPermissions: vi.fn(),
    getToken: vi.fn(),
    addListener: vi.fn(),
  },
}));

vi.mock('./platform', () => ({
  isNative: vi.fn(),
  isIOS: vi.fn(),
}));

import { FirebaseMessaging } from '@capacitor-firebase/messaging';
import { isIOS, isNative } from './platform';
import { requestNativePushPermission, getNativeFcmToken } from './nativePush';

describe('requestNativePushPermission', () => {
  beforeEach(() => vi.clearAllMocks());

  it('returns false on web without calling plugin', async () => {
    vi.mocked(isNative).mockReturnValue(false);
    const result = await requestNativePushPermission();
    expect(result).toBe(false);
    expect(FirebaseMessaging.requestPermissions).not.toHaveBeenCalled();
  });

  it('returns true when permission is granted on native', async () => {
    vi.mocked(isNative).mockReturnValue(true);
    vi.mocked(FirebaseMessaging.requestPermissions).mockResolvedValue({ receive: 'granted' });
    const result = await requestNativePushPermission();
    expect(result).toBe(true);
  });

  it('returns false when permission is denied on native', async () => {
    vi.mocked(isNative).mockReturnValue(true);
    vi.mocked(FirebaseMessaging.requestPermissions).mockResolvedValue({ receive: 'denied' });
    const result = await requestNativePushPermission();
    expect(result).toBe(false);
  });
});

describe('getNativeFcmToken', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(isIOS).mockReturnValue(false);
  });

  it('returns null on web', async () => {
    vi.mocked(isNative).mockReturnValue(false);
    const token = await getNativeFcmToken();
    expect(token).toBeNull();
  });

  it('returns FCM token on native when getToken succeeds', async () => {
    vi.mocked(isNative).mockReturnValue(true);
    vi.mocked(FirebaseMessaging.getToken).mockResolvedValue({ token: 'fake-fcm-token' });
    const token = await getNativeFcmToken();
    expect(token).toBe('fake-fcm-token');
  });

  it('returns null when getToken throws', async () => {
    vi.mocked(isNative).mockReturnValue(true);
    vi.mocked(FirebaseMessaging.getToken).mockRejectedValue(new Error('Registration failed'));
    const token = await getNativeFcmToken();
    expect(token).toBeNull();
  });

  it('waits for tokenReceived on iOS when initial getToken fails', async () => {
    vi.useFakeTimers();
    vi.mocked(isNative).mockReturnValue(true);
    vi.mocked(isIOS).mockReturnValue(true);
    vi.mocked(FirebaseMessaging.getToken).mockRejectedValueOnce(new Error('APNS token has not been set yet'));

    const remove = vi.fn().mockResolvedValue(undefined);
    vi.mocked(FirebaseMessaging.addListener).mockImplementation(async (eventName, listener) => {
      if ((eventName as string) === 'tokenReceived') {
        setTimeout(() => listener({ token: 'delayed-fcm-token' }), 10);
      }
      return { remove };
    });

    const tokenPromise = getNativeFcmToken();
    await vi.advanceTimersByTimeAsync(20);

    await expect(tokenPromise).resolves.toBe('delayed-fcm-token');
    expect(remove).toHaveBeenCalled();
    vi.useRealTimers();
  });
});
