import UIKit
import ComposeApp

/// Process-wide setup, the counterpart of `NookMindApplication` on Android: builds the shared
/// `AppContainer` with whichever native services this build has.
final class AppDelegate: NSObject, UIApplicationDelegate {

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        // Sign in with Apple needs the capability on the App ID, which only a paid developer team
        // can grant. The flag in Info.plist says whether this build has it; when it does not, the
        // bridge is left out and the shared code hides the button, exactly like on desktop. A
        // visible button that fails on tap would be worse than no button.
        let appleSignInEnabled = Bundle.main.object(forInfoDictionaryKey: "NookMindAppleSignInEnabled") as? Bool ?? false

        IosApp.shared.initialize(
            appleSignIn: appleSignInEnabled ? AppleSignInBridge() : nil,
            // Google Sign-In (GoogleSignIn-iOS) and push (Firebase Messaging) are the next step:
            // both need a Swift package and console registrations that do not exist yet.
            googleSignIn: nil,
            push: nil
        )
        return true
    }
}
