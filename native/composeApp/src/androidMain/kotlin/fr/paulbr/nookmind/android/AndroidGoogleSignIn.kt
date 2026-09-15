package fr.paulbr.nookmind.android

import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import fr.paulbr.nookmind.core.config.AppConfig
import fr.paulbr.nookmind.core.platform.GoogleSignInProvider
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID

/**
 * Google Sign-In through Credential Manager (replaces `@capgo/capacitor-social-login`).
 * Returns the Google ID token that Supabase exchanges for a session.
 */
class AndroidGoogleSignIn : GoogleSignInProvider {

    override val isAvailable: Boolean get() = AppConfig.googleWebClientId != null

    override suspend fun signIn(): String {
        val activity = ActivityTracker.current ?: error("No visible activity for Google sign-in")
        val clientId = AppConfig.googleWebClientId ?: error("GOOGLE_AUTH_WEB_CLIENT_ID is not configured")
        val option = GetGoogleIdOption.Builder()
            .setServerClientId(clientId)
            // First pass: accounts already used with the app; the retry below widens to every account.
            .setFilterByAuthorizedAccounts(true)
            .setAutoSelectEnabled(false)
            .setNonce(newNonce())
            .build()
        val manager = CredentialManager.create(activity)
        val response = try {
            manager.getCredential(activity, GetCredentialRequest.Builder().addCredentialOption(option).build())
        } catch (e: Throwable) {
            if (e is GetCredentialCancellationException) throw e
            val wide = GetGoogleIdOption.Builder()
                .setServerClientId(clientId)
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(false)
                .setNonce(newNonce())
                .build()
            manager.getCredential(activity, GetCredentialRequest.Builder().addCredentialOption(wide).build())
        }
        val credential = response.credential
        if (credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            error("Unexpected credential type: ${credential.type}")
        }
        return GoogleIdTokenCredential.createFrom(credential.data).idToken
    }

    override suspend fun signOut() {
        val activity = ActivityTracker.current ?: return
        runCatching { CredentialManager.create(activity).clearCredentialState(ClearCredentialStateRequest()) }
    }

    /** SHA-256 of a random UUID, as Google requires a hashed nonce. */
    private fun newNonce(): String {
        val raw = UUID.randomUUID().toString() + SecureRandom().nextLong()
        return MessageDigest.getInstance("SHA-256").digest(raw.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
