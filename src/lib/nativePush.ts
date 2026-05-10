import { FirebaseMessaging } from '@capacitor-firebase/messaging';
import { isNative } from './platform';

export async function requestNativePushPermission(): Promise<boolean> {
  if (!isNative()) return false;
  const { receive } = await FirebaseMessaging.requestPermissions();
  return receive === 'granted';
}

export async function getNativeFcmToken(): Promise<string | null> {
  if (!isNative()) return null;
  try {
    const { token } = await FirebaseMessaging.getToken();
    return token;
  } catch {
    return null;
  }
}
