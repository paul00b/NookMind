import GoogleSignIn
import SwiftUI

@main
struct NookMindApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate

    var body: some Scene {
        WindowGroup {
            ComposeView()
                // Compose draws edge to edge and applies the safe area and keyboard insets itself,
                // the way enableEdgeToEdge() does on Android. Letting SwiftUI inset the view too
                // would leave a blank band under the status bar.
                .ignoresSafeArea()
                // Google's sign-in page returns through the reversed client id URL scheme.
                .onOpenURL { url in _ = GIDSignIn.sharedInstance.handle(url) }
        }
    }
}
