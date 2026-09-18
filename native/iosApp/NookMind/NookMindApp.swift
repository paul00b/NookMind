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
        }
    }
}
