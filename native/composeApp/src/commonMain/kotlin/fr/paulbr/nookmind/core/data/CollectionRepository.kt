package fr.paulbr.nookmind.core.data

import fr.paulbr.nookmind.core.domain.addMappedIds
import fr.paulbr.nookmind.core.domain.removeItemById
import fr.paulbr.nookmind.core.domain.removeMappedId
import fr.paulbr.nookmind.core.model.CollectionItem
import fr.paulbr.nookmind.core.platform.logDebug
import fr.paulbr.nookmind.core.ui.ToastController
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.jetbrains.compose.resources.StringResource

class CollectionMessages(
    val fetchError: StringResource,
    val createError: StringResource,
    val deleteError: StringResource,
    val addError: StringResource,
    val removeError: StringResource,
)

/**
 * Port of src/context/createCollectionContext.tsx: a `*_categories` table plus its
 * `*_category_items` join table, exposed as collections with their mapped item ids.
 */
class CollectionRepository<T : CollectionItem>(
    private val collectionTable: String,
    private val joinTable: String,
    private val mappedIdKey: String,
    private val client: SupabaseClient,
    private val auth: AuthRepository,
    private val toasts: ToastController,
    private val messages: CollectionMessages,
    private val build: (id: String, userId: String, title: String, createdAt: String, itemIds: List<String>) -> T,
    scope: CoroutineScope,
) {
    private val _items = MutableStateFlow<List<T>>(emptyList())
    val items: StateFlow<List<T>> = _items

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private var loadedForUser: String? = null

    init {
        scope.launch {
            auth.state.collectLatest { state ->
                when (state) {
                    is AuthState.SignedIn -> if (loadedForUser != state.user.id) fetch(state.user.id)
                    AuthState.SignedOut -> { _items.value = emptyList(); loadedForUser = null }
                    AuthState.Loading -> Unit
                }
            }
        }
    }

    private fun rowToCollection(row: JsonObject): T {
        val ids = row[joinTable]?.let { runCatching { it.jsonArray }.getOrNull() }
            ?.mapNotNull { it.jsonObject[mappedIdKey]?.jsonPrimitive?.contentOrNull }
            ?: emptyList()
        return build(
            row["id"]!!.jsonPrimitive.content,
            row["user_id"]?.jsonPrimitive?.contentOrNull ?: "",
            row["title"]?.jsonPrimitive?.contentOrNull ?: "",
            row["created_at"]?.jsonPrimitive?.contentOrNull ?: "",
            ids,
        )
    }

    private suspend fun fetch(userId: String) {
        _loading.value = true
        try {
            val rows = client.from(collectionTable).select(Columns.raw("*, $joinTable($mappedIdKey)")) {
                filter { eq("user_id", userId) }
                order("created_at", Order.ASCENDING)
            }.decodeList<JsonObject>()
            _items.value = rows.map(::rowToCollection)
            loadedForUser = userId
        } catch (t: Throwable) {
            logDebug("collections", "fetch $collectionTable failed", t)
            toasts.error(messages.fetchError)
        } finally {
            _loading.value = false
        }
    }

    suspend fun create(title: String): T? {
        val user = auth.currentUser ?: return null
        return try {
            val row = client.from(collectionTable).insert(buildJsonObject {
                put("title", title.trim())
                put("user_id", user.id)
            }) { select() }.decodeSingle<JsonObject>()
            val created = build(
                row["id"]!!.jsonPrimitive.content,
                user.id,
                row["title"]?.jsonPrimitive?.contentOrNull ?: title.trim(),
                row["created_at"]?.jsonPrimitive?.contentOrNull ?: "",
                emptyList(),
            )
            _items.update { it + created }
            created
        } catch (t: Throwable) {
            logDebug("collections", "create failed", t)
            toasts.error(messages.createError)
            null
        }
    }

    suspend fun delete(id: String): Boolean = try {
        client.from(collectionTable).delete { filter { eq("id", id) } }
        _items.update { removeItemById(it, id) { c -> c.id } }
        true
    } catch (t: Throwable) {
        logDebug("collections", "delete failed", t)
        toasts.error(messages.deleteError)
        false
    }

    suspend fun addMappedItems(collectionId: String, itemIds: List<String>) {
        if (itemIds.isEmpty()) return
        try {
            val rows = JsonArray(itemIds.map { itemId ->
                buildJsonObject {
                    put("category_id", collectionId)
                    put(mappedIdKey, JsonPrimitive(itemId))
                }
            })
            client.from(joinTable).upsert(rows) { onConflict = "category_id,$mappedIdKey" }
            _items.update { current ->
                addMappedIds(current, collectionId, { it.id }, { item, ids -> rebuild(item, ids) }, itemIds) { it.itemIds }
            }
        } catch (t: Throwable) {
            logDebug("collections", "addMappedItems failed", t)
            toasts.error(messages.addError)
        }
    }

    suspend fun removeMappedItem(collectionId: String, itemId: String) {
        try {
            client.from(joinTable).delete {
                filter {
                    eq("category_id", collectionId)
                    eq(mappedIdKey, itemId)
                }
            }
            _items.update { current ->
                removeMappedId(current, collectionId, { it.id }, { item, ids -> rebuild(item, ids) }, itemId) { it.itemIds }
            }
        } catch (t: Throwable) {
            logDebug("collections", "removeMappedItem failed", t)
            toasts.error(messages.removeError)
        }
    }

    private fun rebuild(item: T, ids: List<String>): T = build(item.id, item.userId, item.title, item.createdAt, ids)
}
