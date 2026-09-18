import AuthenticationServices
import UIKit
import ComposeApp

/// Presents the system Sign in with Apple sheet and hands the identity token back to Kotlin.
///
/// The nonce arrives already hashed: Kotlin generates it and keeps the raw value, because Supabase
/// checks the token's `nonce` claim against the SHA-256 of what it is given. This class only puts
/// the digest in the request, as Apple expects.
final class AppleSignInBridge: NSObject, IosAppleSignInBridge,
    ASAuthorizationControllerDelegate, ASAuthorizationControllerPresentationContextProviding {

    private var onSuccess: ((String, String?, String?) -> Void)?
    private var onFailure: ((String) -> Void)?
    // Kept alive for the duration of the flow: the controller is not retained by the system.
    private var controller: ASAuthorizationController?

    func signIn(
        hashedNonce: String,
        onSuccess: @escaping (String, String?, String?) -> Void,
        onFailure: @escaping (String) -> Void
    ) {
        self.onSuccess = onSuccess
        self.onFailure = onFailure

        let request = ASAuthorizationAppleIDProvider().createRequest()
        request.requestedScopes = [.fullName, .email]
        request.nonce = hashedNonce

        let controller = ASAuthorizationController(authorizationRequests: [request])
        controller.delegate = self
        controller.presentationContextProvider = self
        self.controller = controller
        controller.performRequests()
    }

    // MARK: ASAuthorizationControllerDelegate

    func authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithAuthorization authorization: ASAuthorization
    ) {
        defer { finish() }
        guard let credential = authorization.credential as? ASAuthorizationAppleIDCredential,
              let tokenData = credential.identityToken,
              let idToken = String(data: tokenData, encoding: .utf8)
        else {
            onFailure?("Apple returned no identity token.")
            return
        }
        // Only present on the first authorisation of this Apple ID; nil afterwards, by design.
        onSuccess?(idToken, credential.fullName?.givenName, credential.fullName?.familyName)
    }

    func authorizationController(controller: ASAuthorizationController, didCompleteWithError error: Error) {
        defer { finish() }
        // An empty message makes the shared LoginScreen fall back to its generic Apple failure text,
        // which is the closest it has to "nothing happened" for a dismissed sheet.
        if let authError = error as? ASAuthorizationError, authError.code == .canceled {
            onFailure?("")
            return
        }
        onFailure?(error.localizedDescription)
    }

    // MARK: ASAuthorizationControllerPresentationContextProviding

    func presentationAnchor(for controller: ASAuthorizationController) -> ASPresentationAnchor {
        UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap { $0.windows }
            .first { $0.isKeyWindow } ?? ASPresentationAnchor()
    }

    private func finish() {
        onSuccess = nil
        onFailure = nil
        controller = nil
    }
}
