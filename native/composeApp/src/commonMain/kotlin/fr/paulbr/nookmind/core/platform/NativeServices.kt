package fr.paulbr.nookmind.core.platform

/** Native Google Sign-In (Credential Manager on Android, GoogleSignIn SDK on iOS). */
interface GoogleSignInProvider {
    val isAvailable: Boolean

    /** Returns a Google ID token to hand to Supabase (`signInWithIdToken`). */
    suspend fun signIn(): String

    suspend fun signOut()
}

class AppleSignInResult(val idToken: String, val nonce: String, val givenName: String?, val familyName: String?)

/** Sign in with Apple — iOS only, the button is hidden elsewhere (as in the web app). */
interface AppleSignInProvider {
    val isAvailable: Boolean
    suspend fun signIn(): AppleSignInResult
    suspend fun signOut()
}

/** Push transport (FCM on Android, APNs→FCM on iOS). */
interface PushPlatform {
    val isSupported: Boolean
    suspend fun requestPermission(): Boolean
    suspend fun getToken(): String?
}

object UnavailableGoogleSignIn : GoogleSignInProvider {
    override val isAvailable: Boolean = false
    override suspend fun signIn(): String = throw IllegalStateException("Google sign-in is not available on this platform.")
    override suspend fun signOut() = Unit
}

object UnavailableAppleSignIn : AppleSignInProvider {
    override val isAvailable: Boolean = false
    override suspend fun signIn(): AppleSignInResult = throw IllegalStateException("Apple sign-in is only available on iOS.")
    override suspend fun signOut() = Unit
}

object UnsupportedPushPlatform : PushPlatform {
    override val isSupported: Boolean = false
    override suspend fun requestPermission(): Boolean = false
    override suspend fun getToken(): String? = null
}
