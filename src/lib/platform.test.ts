import { describe, expect, it, vi, beforeEach } from 'vitest';

vi.mock('@capacitor/core', () => ({
  Capacitor: {
    isNativePlatform: vi.fn(),
    getPlatform: vi.fn(),
  },
}));

import { Capacitor } from '@capacitor/core';
import { isNative, isIOS, isAndroid, isWeb } from './platform';

describe('platform detection', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('returns true for isNative when Capacitor reports native', () => {
    vi.mocked(Capacitor.isNativePlatform).mockReturnValue(true);
    expect(isNative()).toBe(true);
  });

  it('returns false for isNative when Capacitor reports web', () => {
    vi.mocked(Capacitor.isNativePlatform).mockReturnValue(false);
    expect(isNative()).toBe(false);
  });

  it('returns true for isIOS when platform is ios', () => {
    vi.mocked(Capacitor.getPlatform).mockReturnValue('ios');
    expect(isIOS()).toBe(true);
    expect(isAndroid()).toBe(false);
    expect(isWeb()).toBe(false);
  });

  it('returns true for isAndroid when platform is android', () => {
    vi.mocked(Capacitor.getPlatform).mockReturnValue('android');
    expect(isAndroid()).toBe(true);
    expect(isIOS()).toBe(false);
    expect(isWeb()).toBe(false);
  });

  it('returns true for isWeb when platform is web', () => {
    vi.mocked(Capacitor.getPlatform).mockReturnValue('web');
    expect(isWeb()).toBe(true);
    expect(isIOS()).toBe(false);
    expect(isAndroid()).toBe(false);
  });
});
