import { PushNotifications } from '@capacitor/push-notifications';
import { isNative } from './platform';

export async function requestNativePushPermission(): Promise<boolean> {
  if (!isNative()) return false;
  const { receive } = await PushNotifications.requestPermissions();
  return receive === 'granted';
}

export async function getNativeFcmToken(timeoutMs = 10000): Promise<string | null> {
  if (!isNative()) return null;
  return new Promise<string | null>((resolve) => {
    let settled = false;

    const finish = (value: string | null) => {
      if (settled) return;
      settled = true;
      PushNotifications.removeAllListeners();
      resolve(value);
    };

    setTimeout(() => finish(null), timeoutMs);

    PushNotifications.addListener('registration', ({ value }) => finish(value));
    PushNotifications.addListener('registrationError', () => finish(null));
    PushNotifications.register().catch(() => finish(null));
  });
}
