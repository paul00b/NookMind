import { describe, expect, it, vi, beforeEach, afterEach } from 'vitest';

vi.mock('@capacitor/push-notifications', () => ({
  PushNotifications: {
    requestPermissions: vi.fn(),
    register: vi.fn(),
    addListener: vi.fn(),
    removeAllListeners: vi.fn(),
  },
}));

vi.mock('./platform', () => ({
  isNative: vi.fn(),
}));

import { PushNotifications } from '@capacitor/push-notifications';
import { isNative } from './platform';
import { requestNativePushPermission, getNativeFcmToken } from './nativePush';

describe('requestNativePushPermission', () => {
  beforeEach(() => vi.clearAllMocks());

  it('returns false on web without calling plugin', async () => {
    vi.mocked(isNative).mockReturnValue(false);
    const result = await requestNativePushPermission();
    expect(result).toBe(false);
    expect(PushNotifications.requestPermissions).not.toHaveBeenCalled();
  });

  it('returns true when permission is granted on native', async () => {
    vi.mocked(isNative).mockReturnValue(true);
    vi.mocked(PushNotifications.requestPermissions).mockResolvedValue({ receive: 'granted' });
    const result = await requestNativePushPermission();
    expect(result).toBe(true);
  });

  it('returns false when permission is denied on native', async () => {
    vi.mocked(isNative).mockReturnValue(true);
    vi.mocked(PushNotifications.requestPermissions).mockResolvedValue({ receive: 'denied' });
    const result = await requestNativePushPermission();
    expect(result).toBe(false);
  });
});

describe('getNativeFcmToken', () => {
  beforeEach(() => vi.clearAllMocks());
  afterEach(() => vi.clearAllMocks());

  it('returns null on web', async () => {
    vi.mocked(isNative).mockReturnValue(false);
    const token = await getNativeFcmToken();
    expect(token).toBeNull();
  });

  it('returns FCM token on native when registration succeeds', async () => {
    vi.mocked(isNative).mockReturnValue(true);
    vi.mocked(PushNotifications.register).mockResolvedValue(undefined);
    vi.mocked(PushNotifications.addListener).mockImplementation((event: string, cb: unknown) => {
      if (event === 'registration') {
        setTimeout(() => (cb as (data: { value: string }) => void)({ value: 'fake-fcm-token' }), 0);
      }
      return Promise.resolve({ remove: vi.fn() });
    });

    const token = await getNativeFcmToken();
    expect(token).toBe('fake-fcm-token');
    expect(PushNotifications.removeAllListeners).toHaveBeenCalled();
  });

  it('returns null when registration throws', async () => {
    vi.mocked(isNative).mockReturnValue(true);
    vi.mocked(PushNotifications.register).mockRejectedValue(new Error('Registration failed'));
    vi.mocked(PushNotifications.addListener).mockResolvedValue({ remove: vi.fn() });

    const token = await getNativeFcmToken();
    expect(token).toBeNull();
  });
});
