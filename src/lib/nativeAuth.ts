import { SocialLogin } from '@capgo/capacitor-social-login';
import { isIOS, isNative } from './platform';

export interface NativeAppleSignInResult {
    idToken: string;
    nonce: string;
    profile?: {
        email?: string | null;
        givenName?: string | null;
        familyName?: string | null;
    };
}

let initPromise: Promise<void> | null = null;

export function initNativeAuth(): Promise<void> {
    if (initPromise) return initPromise;
    initPromise = (async () => {
        if (!isNative()) return;
        const webClientId = import.meta.env.VITE_GOOGLE_AUTH_WEB_CLIENT_ID;
        const iOSClientId = import.meta.env.VITE_GOOGLE_AUTH_IOS_CLIENT_ID;
        const config: Parameters<typeof SocialLogin.initialize>[0] = {
            google: {
                webClientId,
                iOSClientId,
                iOSServerClientId: webClientId,
                mode: 'online',
            },
        };
        if (isIOS()) {
            config.apple = {};
        }
        await SocialLogin.initialize(config);
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

function generateNonce(length: number): string {
    const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
    const bytes = crypto.getRandomValues(new Uint8Array(length));
    return Array.from(bytes, (b) => chars[b % chars.length]).join('');
}

async function sha256Hex(plain: string): Promise<string> {
    const encoder = new TextEncoder();
    const data = encoder.encode(plain);
    const hashBuffer = await crypto.subtle.digest('SHA-256', data);
    return Array.from(new Uint8Array(hashBuffer))
        .map((b) => b.toString(16).padStart(2, '0'))
        .join('');
}

export async function nativeAppleSignIn(): Promise<NativeAppleSignInResult> {
    if (!isIOS()) {
        throw new Error('Apple sign-in is only available on iOS');
    }
    await initNativeAuth();

    const rawNonce = generateNonce(32);
    const hashedNonce = await sha256Hex(rawNonce);

    const res = await SocialLogin.login({
        provider: 'apple',
        options: {
            scopes: ['email', 'name'],
            nonce: hashedNonce,
        },
    });

    if (res.provider !== 'apple') {
        throw new Error('Unexpected provider response');
    }

    const idToken = res.result.idToken;
    if (!idToken) {
        throw new Error('Apple sign-in returned no idToken');
    }

    return {
        idToken,
        nonce: rawNonce,
        profile: res.result.profile,
    };
}

export async function nativeAppleSignOut(): Promise<void> {
    if (!isIOS()) return;
    try {
        await SocialLogin.logout({ provider: 'apple' });
    } catch {
        // already signed out — ignore
    }
}
