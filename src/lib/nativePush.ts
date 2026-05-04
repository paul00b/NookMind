import { PushNotifications } from '@capacitor/push-notifications';
import { isNative } from './platform';

export async function requestNativePushPermission(): Promise<boolean> {
  if (!isNative()) return false;
  const { receive } = await PushNotifications.requestPermissions();
  return receive === 'granted';
}

export async function getNativeFcmToken(): Promise<string | null> {
  if (!isNative()) return null;
  return new Promise<string | null>((resolve) => {
    let settled = false;

    PushNotifications.addListener('registration', ({ value }) => {
      if (!settled) {
        settled = true;
        PushNotifications.removeAllListeners();
        resolve(value);
      }
    });

    PushNotifications.addListener('registrationError', () => {
      if (!settled) {
        settled = true;
        PushNotifications.removeAllListeners();
        resolve(null);
      }
    });

    PushNotifications.register().catch(() => {
      if (!settled) {
        settled = true;
        PushNotifications.removeAllListeners();
        resolve(null);
      }
    });
  });
}
