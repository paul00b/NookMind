import { SocialLogin } from '@capgo/capacitor-social-login';
import { isNative } from './platform';

let initPromise: Promise<void> | null = null;

export function initNativeAuth(): Promise<void> {
  if (initPromise) return initPromise;
  initPromise = (async () => {
    if (!isNative()) return;
    const webClientId = import.meta.env.VITE_GOOGLE_AUTH_WEB_CLIENT_ID;
    const iOSClientId = import.meta.env.VITE_GOOGLE_AUTH_IOS_CLIENT_ID;
    await SocialLogin.initialize({
      google: {
        webClientId,
        iOSClientId,
        iOSServerClientId: webClientId,
        mode: 'online',
      },
    });
  })();
  return initPromise;
}

export async function nativeGoogleSignIn(): Promise<string> {
  await initNativeAuth();
  const res = await SocialLogin.login({
    provider: 'google',
    options: { scopes: ['email', 'profile'] },
  });
  if (res.provider !== 'google') {
    throw new Error('Unexpected provider response');
  }
  if (res.result.responseType !== 'online') {
    throw new Error('Google sign-in returned offline response (no idToken)');
  }
  const idToken = res.result.idToken;
  if (!idToken) {
    throw new Error('Google sign-in returned no idToken');
  }
  return idToken;
}

export async function nativeGoogleSignOut(): Promise<void> {
  if (!isNative()) return;
  try {
    await SocialLogin.logout({ provider: 'google' });
  } catch {
    // already signed out — ignore
  }
}

