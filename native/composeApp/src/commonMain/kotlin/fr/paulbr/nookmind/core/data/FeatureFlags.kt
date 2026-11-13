package fr.paulbr.nookmind.core.data

import fr.paulbr.nookmind.core.platform.logDebug
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

/**
 * Features turned on for one account from the Supabase dashboard (table `user_features`, see
 * supabase-user-features-migration.sql): each user reads only their own row and cannot write it.
 * Read on platforms that allow it ([enabled]: Android); elsewhere the set stays empty.
 */
class FeatureFlags(
    private val client: SupabaseClient,
    private val auth: AuthRepository,
    scope: CoroutineScope,
    enabled: Boolean,
) {
    private val _features = MutableStateFlow<Set<String>>(emptySet())
    val features: StateFlow<Set<String>> = _features

    @Serializable
    private data class Row(val features: List<String> = emptyList())

    init {
        if (enabled) scope.launch {
            auth.state.collectLatest { state ->
                when (state) {
                    is AuthState.SignedIn -> load(state.user.id)
                    AuthState.SignedOut -> _features.value = emptySet()
                    AuthState.Loading -> Unit
                }
            }
        }
    }

    private suspend fun load(userId: String) {
        if (userId.isBlank()) return
        runCatching {
            val row = client.from("user_features").select(Columns.raw("features")) {
                filter { eq("user_id", userId) }
                limit(1)
            }.decodeSingleOrNull<Row>()
            _features.value = row?.features.orEmpty().toSet()
        }.onFailure { logDebug("flags", "user_features failed", it) }
    }

    companion object {
        const val JELLYSEERR = "jellyseerr"
    }
}
