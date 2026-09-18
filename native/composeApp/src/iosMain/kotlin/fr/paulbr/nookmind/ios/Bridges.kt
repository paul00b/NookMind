package fr.paulbr.nookmind.ios

import fr.paulbr.nookmind.core.config.AppConfig
import fr.paulbr.nookmind.core.platform.AppleSignInProvider
import fr.paulbr.nookmind.core.platform.AppleSignInResult
import fr.paulbr.nookmind.core.platform.GoogleSignInProvider
import fr.paulbr.nookmind.core.platform.GoogleSignInResult
import fr.paulbr.nookmind.core.platform.PushPlatform
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.CoreCrypto.CC_SHA256
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH
import platform.Foundation.NSUUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/*
 * The seam between the shared Kotlin and the Swift that talks to Apple's SDKs.
 *
 * The shared providers (`GoogleSignInProvider`, `AppleSignInProvider`, `PushPlatform`) are suspend
 * interfaces, and a Swift class cannot usefully implement a Kotlin suspend function: Kotlin/Native
 * exports it as a completion-handler method, but calling it back from Kotlin as a suspension does
 * not go through the coroutine machinery. So Swift implements these callback interfaces instead,
 * and the adapters below turn one callback into one suspension.
 *
 * Nonces are generated here, not in Swift, so the rule learnt on Android lives in one place:
 * the identity provider gets the SHA-256 of the nonce, Supabase gets the raw value and checks
 * that the token's `nonce` claim is its digest. One without the other is rejected.
 */

/** Sign in with Apple, implemented in Swift with `ASAuthorizationController`. */
interface IosAppleSignInBridge {
    /**
     * Must call exactly one of the two callbacks, once, on any thread. [hashedNonce] goes in
     * `ASAuthorizationAppleIDRequest.nonce`. The names are only non-null on the very first sign-in
     * of an Apple ID with this app, which is Apple's behaviour and not a bug.
     */
    fun signIn(
        hashedNonce: String,
        onSuccess: (idToken: String, givenName: String?, familyName: String?) -> Unit,
        onFailure: (message: String) -> Unit,
    )
}

/** Google Sign-In, implemented in Swift with the GoogleSignIn-iOS SDK. */
interface IosGoogleSignInBridge {
    /** [hashedNonce] is the `nonce` parameter of `GIDSignIn.signIn`; the SDK's server client id must be the web client. */
    fun signIn(hashedNonce: String, onSuccess: (idToken: String) -> Unit, onFailure: (message: String) -> Unit)
    fun signOut()
}

/** Push transport, implemented in Swift with UserNotifications and Firebase Messaging (APNs -> FCM token). */
interface IosPushBridge {
    fun requestPermission(onResult: (granted: Boolean) -> Unit)
    /** The FCM registration token, or null when Firebase is not configured in this build. */
    fun fetchToken(onResult: (token: String?) -> Unit)
}

internal class Nonce private constructor(val raw: String, val hashed: String) {
    companion object {
        /** Two UUIDs from the system CSPRNG, then the hex SHA-256 the provider embeds in the token. */
        fun generate(): Nonce {
            val raw = NSUUID().UUIDString + NSUUID().UUIDString
            return Nonce(raw, sha256Hex(raw))
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
internal fun sha256Hex(input: String): String {
    val bytes = input.encodeToByteArray()
    val digest = ByteArray(CC_SHA256_DIGEST_LENGTH)
    bytes.usePinned { source ->
        digest.usePinned { target ->
            CC_SHA256(source.addressOf(0), bytes.size.convert(), target.addressOf(0).reinterpret<UByteVar>())
        }
    }
    return digest.joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }
}

internal class IosAppleSignIn(private val bridge: IosAppleSignInBridge) : AppleSignInProvider {
    override val isAvailable: Boolean = true

    override suspend fun signIn(): AppleSignInResult {
        val nonce = Nonce.generate()
        return suspendCancellableCoroutine { continuation ->
            bridge.signIn(
                hashedNonce = nonce.hashed,
                onSuccess = { idToken, givenName, familyName ->
                    if (continuation.isActive) continuation.resume(AppleSignInResult(idToken, nonce.raw, givenName, familyName))
                },
                onFailure = { message ->
                    if (continuation.isActive) continuation.resumeWithException(IllegalStateException(message))
                },
            )
        }
    }

    /** Apple offers no client-side sign-out; the grant is revoked from the Apple ID settings. */
    override suspend fun signOut() = Unit
}

internal class IosGoogleSignIn(private val bridge: IosGoogleSignInBridge) : GoogleSignInProvider {
    /** Same condition as Android: without the web client id there is nothing to request a token for. */
    override val isAvailable: Boolean get() = AppConfig.googleWebClientId != null

    override suspend fun signIn(): GoogleSignInResult {
        val nonce = Nonce.generate()
        return suspendCancellableCoroutine { continuation ->
            bridge.signIn(
                hashedNonce = nonce.hashed,
                onSuccess = { idToken -> if (continuation.isActive) continuation.resume(GoogleSignInResult(idToken, nonce.raw)) },
                onFailure = { message -> if (continuation.isActive) continuation.resumeWithException(IllegalStateException(message)) },
            )
        }
    }

    override suspend fun signOut() = bridge.signOut()
}

internal class IosPushPlatform(private val bridge: IosPushBridge) : PushPlatform {
    override val isSupported: Boolean = true

    override suspend fun requestPermission(): Boolean = suspendCancellableCoroutine { continuation ->
        bridge.requestPermission { granted -> if (continuation.isActive) continuation.resume(granted) }
    }

    override suspend fun getToken(): String? = suspendCancellableCoroutine { continuation ->
        bridge.fetchToken { token -> if (continuation.isActive) continuation.resume(token) }
    }
}
