import { FirebaseMessaging } from '@capacitor-firebase/messaging';
import { isIOS, isNative } from './platform';

const IOS_TOKEN_TIMEOUT_MS = 6000;

export async function requestNativePushPermission(): Promise<boolean> {
  if (!isNative()) return false;
  const { receive } = await FirebaseMessaging.requestPermissions();
  return receive === 'granted';
}

async function waitForIosFcmToken(): Promise<string | null> {
  let resolved = false;

  return new Promise(async (resolve) => {
    const finish = async (token: string | null) => {
      if (resolved) return;
      resolved = true;
      await Promise.allSettled([
        tokenListener?.remove(),
        apnsListener?.remove(),
      ]);
      resolve(token);
    };

    const tokenListener = await FirebaseMessaging.addListener('tokenReceived', (event) => {
      void finish(event.token ?? null);
    });

    const apnsListener = await FirebaseMessaging.addListener('apnsTokenReceived', async () => {
      try {
        const { token } = await FirebaseMessaging.getToken();
        await finish(token ?? null);
      } catch (error) {
        console.warn('[push] APNs token received but FCM token is still unavailable', error);
      }
    });

    window.setTimeout(() => {
      void finish(null);
    }, IOS_TOKEN_TIMEOUT_MS);
  });
}

export async function getNativeFcmToken(): Promise<string | null> {
  if (!isNative()) return null;
  try {
    const { token } = await FirebaseMessaging.getToken();
    return token;
  } catch (error) {
    if (!isIOS()) {
      console.error('[push] Failed to get native FCM token', error);
      return null;
    }

    const waitedToken = await waitForIosFcmToken();
    if (waitedToken) return waitedToken;

    console.error('[push] Failed to get iOS FCM token after waiting for APNs registration', error);
    return null;
  }
}
