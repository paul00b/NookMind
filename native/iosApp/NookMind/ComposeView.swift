import SwiftUI
import UIKit
import ComposeApp

/// Hosts the shared Compose UI. `MainViewController()` is the Kotlin entry point in `iosMain`.
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
