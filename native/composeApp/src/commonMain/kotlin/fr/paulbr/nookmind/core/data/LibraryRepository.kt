package fr.paulbr.nookmind.core.data

import fr.paulbr.nookmind.core.domain.prependItem
import fr.paulbr.nookmind.core.domain.removeItemById
import fr.paulbr.nookmind.core.domain.replaceItemById
import fr.paulbr.nookmind.core.model.LibraryItem
import fr.paulbr.nookmind.core.network.AppJson
import fr.paulbr.nookmind.core.network.DbJson
import fr.paulbr.nookmind.core.platform.logDebug
import fr.paulbr.nookmind.core.ui.ToastController
import fr.paulbr.nookmind.core.ui.ToastKind
import fr.paulbr.nookmind.core.ui.ToastMessage
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.jetbrains.compose.resources.StringResource

/**
 * Body of an insert: every field of [item] except the three the database owns, plus the user id.
 *
 * Encoded with [DbJson] so a property still equal to its default (an empty `author`, the initial
 * `status`) is present in the payload instead of being dropped. `books.author` is NOT NULL with no
 * database default, so dropping it fails the insert.
 */
fun <T : LibraryItem> insertPayload(serializer: KSerializer<T>, item: T, userId: String): JsonObject =
    buildJsonObject {
        (DbJson.encodeToJsonElement(serializer, item) as JsonObject).forEach { (key, value) ->
            if (key != "id" && key != "created_at" && key != "user_id") put(key, value)
        }
        put("user_id", JsonPrimitive(userId))
    }

class LibraryMessages(
    val fetchError: StringResource,
    val addError: StringResource,
    val updateError: StringResource,
    val deleteError: StringResource,
    val addSuccess: StringResource,
    val deleteSuccess: StringResource,
)

/**
 * Port of src/context/createLibraryContext.tsx: one Supabase table scoped to the signed-in user,
 * an in-memory list ordered by `created_at DESC`, optimistic list updates after each write.
 */
class LibraryRepository<T : LibraryItem>(
    private val table: String,
    private val serializer: KSerializer<T>,
    private val client: SupabaseClient,
    private val auth: AuthRepository,
    private val toasts: ToastController,
    private val messages: LibraryMessages,
    scope: CoroutineScope,
) {
    private val _items = MutableStateFlow<List<T>>(emptyList())
    val items: StateFlow<List<T>> = _items

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _loadedForUser = MutableStateFlow<String?>(null)
    /** Id of the user whose library is currently loaded (null while loading / signed out). */
    val loadedForUser: StateFlow<String?> = _loadedForUser

    init {
        scope.launch {
            auth.state.collectLatest { state ->
                when (state) {
                    is AuthState.SignedIn -> if (_loadedForUser.value != state.user.id) fetch(state.user.id)
                    AuthState.SignedOut -> { _items.value = emptyList(); _loadedForUser.value = null }
                    AuthState.Loading -> Unit
                }
            }
        }
    }

    suspend fun refetch() {
        auth.currentUser?.let { fetch(it.id) }
    }

    private suspend fun fetch(userId: String) {
        _loading.value = true
        try {
            val result = client.from(table).select {
                filter { eq("user_id", userId) }
                order("created_at", Order.DESCENDING)
            }
            val rows = AppJson.decodeFromString(ListSerializer(serializer), result.data)
            _items.value = rows
            _loadedForUser.value = userId
        } catch (t: Throwable) {
            logDebug("library", "fetch $table failed", t)
            toasts.error(messages.fetchError)
        } finally {
            _loading.value = false
        }
    }

    /** Inserts [item] (its id / user_id / created_at are ignored) and prepends the stored row. */
    suspend fun add(item: T): T? {
        val user = auth.currentUser ?: return null
        return try {
            val payload = insertPayload(serializer, item, user.id)
            val stored = AppJson.decodeFromString(ListSerializer(serializer), client.from(table).insert(payload) { select() }.data).first()
            _items.update { prependItem(it, stored) }
            toasts.success(messages.addSuccess)
            stored
        } catch (t: Throwable) {
            logDebug("library", "insert $table failed", t)
            toasts.show(t.message?.takeIf { it.isNotBlank() }?.let { ToastMessage.Text(it) } ?: ToastMessage.Resource(messages.addError), ToastKind.ERROR)
            null
        }
    }

    /** Partial update (`Partial<T>` of the web app) as a JSON object of column → value. */
    suspend fun update(id: String, patch: JsonObject): T? = try {
        val stored = AppJson.decodeFromString(ListSerializer(serializer), client.from(table).update(patch) {
            select()
            filter { eq("id", id) }
        }.data).first()
        _items.update { replaceItemById(it, stored) { row -> row.id } }
        stored
    } catch (t: Throwable) {
        logDebug("library", "update $table failed", t)
        toasts.show(t.message?.takeIf { it.isNotBlank() }?.let { ToastMessage.Text(it) } ?: ToastMessage.Resource(messages.updateError), ToastKind.ERROR)
        null
    }

    suspend fun delete(id: String): Boolean = try {
        client.from(table).delete { filter { eq("id", id) } }
        _items.update { removeItemById(it, id) { row -> row.id } }
        toasts.success(messages.deleteSuccess)
        true
    } catch (t: Throwable) {
        logDebug("library", "delete $table failed", t)
        toasts.show(t.message?.takeIf { it.isNotBlank() }?.let { ToastMessage.Text(it) } ?: ToastMessage.Resource(messages.deleteError), ToastKind.ERROR)
        false
    }

    /** Local-only replacement (used after silent background refreshes). */
    fun replaceLocal(item: T) = _items.update { replaceItemById(it, item) { row -> row.id } }

    /** Replaces the whole in-memory list without touching the network (previews, screenshots). */
    fun seedLocal(items: List<T>) {
        _items.value = items
        _loading.value = false
    }
}
