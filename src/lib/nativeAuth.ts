import { SocialLogin } from '@capgo/capacitor-social-login';
import { isNative, isIOS } from './platform';
import { generateNonce } from './nonce';

const APP_BUNDLE_ID = 'fr.paulbr.nookmind';

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
      // Apple on Android requires a redirectUrl (OAuth flow) which we don't
      // support — Apple sign-in is iOS-only. Skip on Android to avoid a
      // fatal initialize() rejection that also prevents Google from working.
      ...(isIOS() && {
        apple: {
          clientId: APP_BUNDLE_ID,
        },
      }),
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

export interface AppleSignInResult {
  identityToken: string;
  nonce: string;
}

export async function nativeAppleSignIn(): Promise<AppleSignInResult> {
  await initNativeAuth();
  if (!isIOS()) {
    throw new Error('Apple sign-in is only available on iOS');
  }
  const rawNonce = generateNonce();
  const res = await SocialLogin.login({
    provider: 'apple',
    options: { scopes: ['name', 'email'], nonce: rawNonce },
  });
  if (res.provider !== 'apple') {
    throw new Error('Unexpected provider response');
  }
  const identityToken = res.result.idToken;
  if (!identityToken) {
    throw new Error('Apple sign-in returned no identityToken');
  }
  return { identityToken, nonce: rawNonce };
}
