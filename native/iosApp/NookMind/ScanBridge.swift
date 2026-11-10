import AVFoundation
import UIKit
import VisionKit
import ComposeApp

/// Reads the barcode on the back of a book with VisionKit's live scanner and hands its digits to
/// Kotlin, which looks the ISBN up.
///
/// `DataScannerViewController` exists from iOS 16 and needs a recent camera: before that, or on a
/// device without one, `isAvailable` is false and the shared code hides the scan button.
final class ScanBridge: NSObject, IosScanBridge {

    var isAvailable: Bool {
        if #available(iOS 16.0, *) {
            // isAvailable stays false until the camera is allowed, so it is checked after asking.
            // Kotlin reads this from the UI, on the main thread.
            return MainActor.assumeIsolated { DataScannerViewController.isSupported }
        }
        return false
    }

    private var onResult: ((String?) -> Void)?

    func scan(onResult: @escaping (String?) -> Void) {
        // The camera permission first: the scanner shows nothing without it.
        switch AVCaptureDevice.authorizationStatus(for: .video) {
        case .authorized:
            // Kotlin calls scan from the UI, on the main thread.
            MainActor.assumeIsolated { present(onResult: onResult) }
        case .notDetermined:
            AVCaptureDevice.requestAccess(for: .video) { granted in
                DispatchQueue.main.async {
                    MainActor.assumeIsolated {
                        if granted { self.present(onResult: onResult) } else { onResult(nil) }
                    }
                }
            }
        default:
            onResult(nil)
        }
    }

    @MainActor
    private func present(onResult: @escaping (String?) -> Void) {
        guard #available(iOS 16.0, *), DataScannerViewController.isAvailable, let top = Self.topController() else {
            onResult(nil)
            return
        }
        self.onResult = onResult
        let scanner = DataScannerViewController(
            recognizedDataTypes: [.barcode(symbologies: [.ean13, .upce])],
            qualityLevel: .balanced,
            recognizesMultipleItems: false,
            isHighFrameRateTrackingEnabled: false,
            isHighlightingEnabled: true
        )
        scanner.delegate = self
        let close = UIBarButtonItem(systemItem: .close, primaryAction: UIAction { [weak self] _ in
            self?.finish(with: nil)
        })
        scanner.navigationItem.rightBarButtonItem = close
        let navigation = UINavigationController(rootViewController: scanner)
        navigation.presentationController?.delegate = self
        top.present(navigation, animated: true) {
            try? scanner.startScanning()
        }
    }

    /// Calls back once, then closes the scanner.
    @MainActor
    fileprivate func finish(with code: String?) {
        guard let callback = onResult else { return }
        onResult = nil
        let presenter = Self.topController()?.presentingViewController
        presenter?.dismiss(animated: true)
        callback(code)
    }

    @MainActor
    private static func topController() -> UIViewController? {
        let scene = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }.first
        var top = scene?.windows.first(where: { $0.isKeyWindow })?.rootViewController
        while let presented = top?.presentedViewController { top = presented }
        return top
    }
}

@available(iOS 16.0, *)
extension ScanBridge: DataScannerViewControllerDelegate {
    @MainActor
    func dataScanner(_ dataScanner: DataScannerViewController, didAdd addedItems: [RecognizedItem], allItems: [RecognizedItem]) {
        for item in addedItems {
            if case let .barcode(barcode) = item, let code = barcode.payloadStringValue, !code.isEmpty {
                UINotificationFeedbackGenerator().notificationOccurred(.success)
                dataScanner.stopScanning()
                finish(with: code)
                return
            }
        }
    }
}

extension ScanBridge: UIAdaptivePresentationControllerDelegate {
    /// Swiped down: the scan ends without a code.
    @MainActor
    func presentationControllerDidDismiss(_ presentationController: UIPresentationController) {
        guard let callback = onResult else { return }
        onResult = nil
        callback(nil)
    }
}
