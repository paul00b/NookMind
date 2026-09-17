package fr.paulbr.nookmind.android

import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import fr.paulbr.nookmind.core.config.AppConfig
import fr.paulbr.nookmind.core.platform.GoogleSignInProvider
import fr.paulbr.nookmind.core.platform.GoogleSignInResult
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID

/**
 * Google Sign-In through Credential Manager (replaces `@capgo/capacitor-social-login`).
 * Returns the Google ID token that Supabase exchanges for a session.
 */
class AndroidGoogleSignIn : GoogleSignInProvider {

    override val isAvailable: Boolean get() = AppConfig.googleWebClientId != null

    override suspend fun signIn(): GoogleSignInResult {
        val activity = ActivityTracker.current ?: error("No visible activity for Google sign-in")
        val clientId = AppConfig.googleWebClientId ?: error("GOOGLE_AUTH_WEB_CLIENT_ID is not configured")

        // Supabase compares the `nonce` claim of the ID token against the SHA-256 (hexadecimal) of
        // the raw value it is handed, so Google gets the digest and Supabase gets the raw string.
        // One without the other is rejected, which is why signIn() returns both.
        val rawNonce = UUID.randomUUID().toString() + SecureRandom().nextLong()
        val hashedNonce = MessageDigest.getInstance("SHA-256")
            .digest(rawNonce.toByteArray())
            .joinToString("") { "%02x".format(it) }

        // The button flow, not the One Tap banner. GetGoogleIdOption only offers accounts that have
        // already authorised this app and raises NoCredentialException for anyone else, which is
        // precisely what a first sign-in is. GetSignInWithGoogleOption always opens the account
        // picker, and is what Google documents for a "Sign in with Google" button.
        val option = GetSignInWithGoogleOption.Builder(serverClientId = clientId)
            .setNonce(hashedNonce)
            .build()

        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val response = CredentialManager.create(activity).getCredential(activity, request)

        val credential = response.credential
        if (credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            error("Unexpected credential type: ${credential.type}")
        }
        return GoogleSignInResult(
            idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken,
            nonce = rawNonce,
        )
    }

    override suspend fun signOut() {
        val activity = ActivityTracker.current ?: return
        runCatching { CredentialManager.create(activity).clearCredentialState(ClearCredentialStateRequest()) }
    }
}
