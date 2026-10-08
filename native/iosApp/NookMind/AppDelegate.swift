import FirebaseCore
import FirebaseMessaging
import UIKit
import UserNotifications
import ComposeApp

/// Process-wide setup, the counterpart of `NookMindApplication` on Android: builds the shared
/// `AppContainer` with whichever native services this build has.
///
/// Each service is wired only when the build can actually deliver it. When one is left out, the
/// shared code gets its "unavailable" implementation and hides the matching button or section,
/// exactly like on desktop. A visible button that fails on tap would be worse than no button.
final class AppDelegate: NSObject, UIApplicationDelegate, MessagingDelegate, UNUserNotificationCenterDelegate {

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        IosApp.shared.initialize(
            appleSignIn: Self.flag("NookMindAppleSignInEnabled") ? AppleSignInBridge() : nil,
            googleSignIn: Self.googleSignIn(),
            push: setUpPush(application)
        )
        return true
    }

    // MARK: Which services this build has

    /// Sign in with Apple and push both need entitlements that only a paid developer team can sign
    /// with. `NOOKMIND_PAID_TEAM` in Signing.xcconfig decides, and lands in Info.plist as YES or NO.
    private static func flag(_ key: String) -> Bool {
        (Bundle.main.object(forInfoDictionaryKey: key) as? String) == "YES"
    }

    /// Google needs the iOS OAuth client id, and its reversed form registered as a URL scheme so the
    /// browser can hand the result back. GoogleSignIn raises an exception, not an error, when the
    /// scheme is missing: so a half-configured build gets no button rather than a crash on tap.
    private static func googleSignIn() -> GoogleSignInBridge? {
        let suffix = ".apps.googleusercontent.com"
        guard let clientID = Bundle.main.object(forInfoDictionaryKey: "NookMindGoogleClientID") as? String,
              clientID.hasSuffix(suffix), clientID.count > suffix.count
        else { return nil }
        let scheme = "com.googleusercontent.apps." + String(clientID.dropLast(suffix.count))
        let urlTypes = Bundle.main.object(forInfoDictionaryKey: "CFBundleURLTypes") as? [[String: Any]] ?? []
        let schemes = urlTypes.flatMap { $0["CFBundleURLSchemes"] as? [String] ?? [] }
        guard schemes.contains(scheme) else { return nil }
        return GoogleSignInBridge(clientID: clientID)
    }

    /// Firebase is configured only when `GoogleService-Info.plist` is in the bundle (it is git-ignored),
    /// and push only on a paid-team build, the only kind APNs delivers to.
    private func setUpPush(_ application: UIApplication) -> PushBridge? {
        guard Self.flag("NookMindPushEnabled"),
              Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist") != nil
        else { return nil }
        FirebaseApp.configure()
        Messaging.messaging().delegate = self
        UNUserNotificationCenter.current().delegate = self
        application.registerForRemoteNotifications()
        return PushBridge()
    }

    // MARK: APNs and FCM tokens

    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        // Firebase swizzles this too; setting it explicitly costs nothing and survives swizzling being off.
        Messaging.messaging().apnsToken = deviceToken
    }

    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        // Same as onNewToken on Android: keep the stored subscription in sync when Firebase rotates it.
        guard let token = fcmToken else { return }
        IosApp.shared.onPushTokenRefreshed(token: token)
    }

    // MARK: Notifications

    /// Without this, iOS drops a notification that arrives while the app is open. Android shows it
    /// from the messaging service in the same situation.
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.banner, .list, .sound])
    }

    /// A tap opens the route the notification carries, like the intent extra on Android.
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        if let route = response.notification.request.content.userInfo["route"] as? String {
            IosApp.shared.openRoute(route: route)
        }
        completionHandler()
    }
}
