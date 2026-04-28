import { describe, expect, it } from 'vitest';
import { generateNonce, sha256Hex } from './nonce';

describe('generateNonce', () => {
  it('returns a non-empty string of at least 32 chars', () => {
    const n = generateNonce();
    expect(typeof n).toBe('string');
    expect(n.length).toBeGreaterThanOrEqual(32);
  });

  it('returns a different value each time', () => {
    const a = generateNonce();
    const b = generateNonce();
    expect(a).not.toBe(b);
  });
});

describe('sha256Hex', () => {
  it('hashes a known input to a known sha-256 hex digest', async () => {
    const hex = await sha256Hex('abc');
    expect(hex).toBe('ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad');
  });

  it('returns 64 hex chars', async () => {
    const hex = await sha256Hex('hello world');
    expect(hex).toMatch(/^[0-9a-f]{64}$/);
  });
});
