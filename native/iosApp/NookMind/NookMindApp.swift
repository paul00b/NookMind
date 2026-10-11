import ComposeApp
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
                // A shared link (nookmind://add/movie/438631) opens the item; anything else is
                // Google's sign-in page returning through the reversed client id URL scheme.
                .onOpenURL { url in
                    if !IosApp.shared.openLink(url: url.absoluteString) {
                        _ = GIDSignIn.sharedInstance.handle(url)
                    }
                }
        }
    }
}
