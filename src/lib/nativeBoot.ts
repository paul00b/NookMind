import { App } from '@capacitor/app';
import { SplashScreen } from '@capacitor/splash-screen';
import { StatusBar, Style } from '@capacitor/status-bar';
import { Keyboard, KeyboardResize } from '@capacitor/keyboard';
import { isNative, isIOS, isAndroid } from './platform';
import { initNativeAuth } from './nativeAuth';

/**
 * Wires native plugin behaviors. Safe to call on web — no-ops there.
 * Idempotent: only registers listeners once.
 */
let booted = false;

export async function nativeBoot(): Promise<void> {
  if (booted) return;
  booted = true;

  if (!isNative()) return;

  try {
    await initNativeAuth();
  } catch (e) {
    console.error('Failed to init native auth', e);
  }

  // Status bar — match dark background of theme
  try {
    await StatusBar.setStyle({ style: Style.Dark });
    if (isAndroid()) {
      await StatusBar.setBackgroundColor({ color: '#0f1117' });
    }
  } catch {
    // status bar plugin may not be available on some platforms — ignore
  }

  // Keyboard — resize web view natively when keyboard appears (iOS-specific tweak)
  if (isIOS()) {
    try {
      await Keyboard.setResizeMode({ mode: KeyboardResize.Native });
    } catch {
      // ignore
    }
  }

  // Hardware back button (Android) — go back in router history, exit if at root
  if (isAndroid()) {
    App.addListener('backButton', ({ canGoBack }) => {
      if (canGoBack) {
        window.history.back();
      } else {
        App.exitApp();
      }
    });
  }

  // Hide splash once the app is hydrated
  // Caller (main.tsx) calls this after createRoot().render — we hide on the next frame.
  requestAnimationFrame(() => {
    SplashScreen.hide().catch(() => {
      // splash plugin may have already auto-hidden — ignore
    });
  });
}
