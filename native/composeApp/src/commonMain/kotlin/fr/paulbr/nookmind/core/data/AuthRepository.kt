package fr.paulbr.nookmind.core.data

import fr.paulbr.nookmind.core.network.NookMindApi
import fr.paulbr.nookmind.core.platform.AppleSignInProvider
import fr.paulbr.nookmind.core.platform.GoogleSignInProvider
import fr.paulbr.nookmind.core.platform.logDebug
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Apple
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/** Snapshot of the signed-in user as the UI needs it. */
data class AuthUser(
    val id: String,
    val email: String?,
    val fullName: String?,
    val avatarUrl: String?,
) {
    /** `user_metadata.full_name || email.split('@')[0] || fallback` */
    fun displayName(fallback: String): String = fullName?.takeIf { it.isNotBlank() }
        ?: email?.substringBefore('@')?.takeIf { it.isNotBlank() }
        ?: fallback
}

sealed interface AuthState {
    data object Loading : AuthState
    data object SignedOut : AuthState
    data class SignedIn(val user: AuthUser) : AuthState
}

/** Port of src/context/AuthContext.tsx on top of supabase-kt. */
class AuthRepository(
    private val client: SupabaseClient,
    private val api: NookMindApi,
    private val prefs: AppPreferences,
    private val googleSignIn: GoogleSignInProvider,
    private val appleSignIn: AppleSignInProvider,
    scope: CoroutineScope,
) {
    private val _state = MutableStateFlow<AuthState>(AuthState.Loading)
    val state: StateFlow<AuthState> = _state

    val currentUser: AuthUser? get() = (_state.value as? AuthState.SignedIn)?.user

    val googleAvailable: Boolean get() = googleSignIn.isAvailable
    val appleAvailable: Boolean get() = appleSignIn.isAvailable

    init {
        scope.launch {
            client.auth.sessionStatus.collect { status ->
                _state.value = when (status) {
                    is SessionStatus.Authenticated -> AuthState.SignedIn(status.session.user?.toAuthUser() ?: client.auth.currentUserOrNull()?.toAuthUser() ?: AuthUser("", null, null, null))
                    is SessionStatus.NotAuthenticated -> AuthState.SignedOut
                    is SessionStatus.Initializing -> AuthState.Loading
                    is SessionStatus.RefreshFailure -> {
                        // Keep the app usable offline with the last known user, like the web session cache.
                        client.auth.currentUserOrNull()?.toAuthUser()?.let { AuthState.SignedIn(it) } ?: AuthState.SignedOut
                    }
                }
            }
        }
    }

    suspend fun accessToken(): String? = client.auth.currentSessionOrNull()?.accessToken

    suspend fun signIn(email: String, password: String): Result<Unit> = runCatching {
        client.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun signUp(email: String, password: String): Result<Unit> = runCatching {
        client.auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
        Unit
    }

    suspend fun signOut() {
        runCatching { googleSignIn.signOut() }
        runCatching { appleSignIn.signOut() }
        runCatching { client.auth.signOut() }.onFailure { logDebug("auth", "signOut failed", it) }
        prefs.onboardingCompleted = false
    }

    suspend fun signInWithGoogle(): Result<Unit> = runCatching {
        val idToken = googleSignIn.signIn()
        client.auth.signInWith(IDToken) {
            this.idToken = idToken
            provider = Google
        }
    }

    suspend fun signInWithApple(): Result<Unit> = runCatching {
        val result = appleSignIn.signIn()
        client.auth.signInWith(IDToken) {
            idToken = result.idToken
            provider = Apple
            nonce = result.nonce
        }
        val fullName = listOfNotNull(result.givenName, result.familyName).filter { it.isNotBlank() }.joinToString(" ")
        if (fullName.isNotBlank() || result.givenName != null || result.familyName != null) {
            runCatching {
                client.auth.updateUser {
                    data = buildJsonObject {
                        if (fullName.isNotBlank()) put("full_name", fullName)
                        result.givenName?.let { put("given_name", it) }
                        result.familyName?.let { put("family_name", it) }
                    }
                }
            }
        }
    }

    /** Persists the display name in `user_metadata.full_name`. */
    suspend fun updateDisplayName(name: String): Result<Unit> = runCatching {
        client.auth.updateUser { data = buildJsonObject { put("full_name", name) } }
        (_state.value as? AuthState.SignedIn)?.let { _state.value = AuthState.SignedIn(it.user.copy(fullName = name)) }
    }

    /** Deletes every row of the user and the auth user through the Vercel route, then signs out. */
    suspend fun deleteAccount(notSignedInMessage: String): Result<Unit> = runCatching {
        val token = accessToken() ?: throw IllegalStateException(notSignedInMessage)
        val error = api.deleteAccount(token)
        if (error != null) throw IllegalStateException(error)
        signOut()
    }

    private fun UserInfo.toAuthUser(): AuthUser = AuthUser(
        id = id,
        email = email,
        fullName = userMetadata?.get("full_name")?.jsonPrimitive?.contentOrNull,
        avatarUrl = userMetadata?.get("avatar_url")?.jsonPrimitive?.contentOrNull,
    )
}
