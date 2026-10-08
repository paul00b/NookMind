import FirebaseMessaging
import UIKit
import UserNotifications
import ComposeApp

/// The iOS side of push: the notification permission, and the FCM registration token that the
/// shared PushRepository stores in Supabase, exactly as on Android.
///
/// The Vercel routes send through Firebase Cloud Messaging, which forwards to APNs. That needs the
/// APNs key uploaded in the Firebase console and the `aps-environment` entitlement, which only a
/// paid developer team can sign with: see `AppDelegate` for when this bridge is wired at all.
final class PushBridge: NSObject, IosPushBridge {

    func requestPermission(onResult: @escaping (KotlinBoolean) -> Void) {
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge]) { granted, _ in
            DispatchQueue.main.async {
                // The APNs token, and with it the FCM token, only exists once the app is registered.
                // Registering again after a grant is harmless and covers a first refusal later undone
                // in the Settings app.
                if granted { UIApplication.shared.registerForRemoteNotifications() }
                onResult(KotlinBoolean(bool: granted))
            }
        }
    }

    func fetchToken(onResult: @escaping (String?) -> Void) {
        Messaging.messaging().token { token, error in
            // Fails when APNs has not handed a device token yet, typically on a build without the
            // push entitlement. The shared code reports "token unavailable", which is the truth.
            onResult(error == nil ? token : nil)
        }
    }
}
