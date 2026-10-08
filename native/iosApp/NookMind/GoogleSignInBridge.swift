import GoogleSignIn
import UIKit
import ComposeApp

/// Presents Google's sign-in flow and hands the identity token back to Kotlin.
///
/// Same contract as Android: the token is requested for the web client (`serverClientId`), the one
/// Supabase knows, and carries the nonce Kotlin generated. GoogleSignIn only accepts a custom nonce
/// from 9.0, which is why the package is pinned at that major version.
final class GoogleSignInBridge: NSObject, IosGoogleSignInBridge {

    /// The iOS OAuth client (`<prefix>.apps.googleusercontent.com`), registered in the Google Cloud
    /// project that owns the web client. Its reversed form is the URL scheme in Info.plist.
    private let clientID: String

    init(clientID: String) {
        self.clientID = clientID
    }

    func signIn(
        hashedNonce: String,
        serverClientId: String,
        onSuccess: @escaping (String) -> Void,
        onFailure: @escaping (String) -> Void
    ) {
        DispatchQueue.main.async {
            guard let presenter = Self.topViewController() else {
                onFailure("No screen to show Google sign-in on.")
                return
            }
            GIDSignIn.sharedInstance.configuration = GIDConfiguration(clientID: self.clientID, serverClientID: serverClientId)
            GIDSignIn.sharedInstance.signIn(
                withPresenting: presenter,
                hint: nil,
                additionalScopes: nil,
                nonce: hashedNonce
            ) { result, error in
                if let error {
                    // An empty message makes the shared LoginScreen fall back to its generic text,
                    // the same treatment the Apple bridge gives a dismissed sheet.
                    if let signInError = error as? GIDSignInError, signInError.code == .canceled {
                        onFailure("")
                    } else {
                        onFailure(error.localizedDescription)
                    }
                    return
                }
                guard let idToken = result?.user.idToken?.tokenString else {
                    onFailure("Google returned no identity token.")
                    return
                }
                onSuccess(idToken)
            }
        }
    }

    func signOut() {
        DispatchQueue.main.async { GIDSignIn.sharedInstance.signOut() }
    }

    /// The controller currently on screen: Compose's own, or whatever it presented on top.
    private static func topViewController() -> UIViewController? {
        let window = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap { $0.windows }
            .first { $0.isKeyWindow }
        var top = window?.rootViewController
        while let presented = top?.presentedViewController {
            top = presented
        }
        return top
    }
}
