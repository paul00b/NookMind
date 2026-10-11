package fr.paulbr.nookmind.core.data

import fr.paulbr.nookmind.core.network.AppJson
import fr.paulbr.nookmind.core.network.NookMindApi
import fr.paulbr.nookmind.core.platform.logDebug
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

/** The signed-in user as friends see them, with what they share. */
@Serializable
data class SocialProfile(
    val id: String,
    @SerialName("display_name") val displayName: String = "",
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("friend_code") val friendCode: String = "",
    @SerialName("share_movies") val shareMovies: Boolean = true,
    @SerialName("share_series") val shareSeries: Boolean = true,
    @SerialName("share_books") val shareBooks: Boolean = true,
    @SerialName("share_ratings") val shareRatings: Boolean = true,
)

/** A friend, or a request one way or the other. */
@Serializable
data class Friend(
    @SerialName("friendship_id") val friendshipId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("display_name") val displayName: String = "",
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val status: String = "pending",
    val incoming: Boolean = false,
) {
    val accepted: Boolean get() = status == "accepted"
    val firstName: String get() = firstNameOf(displayName)
}

/** One movie, series or book a friend shares, without their notes. */
@Serializable
data class FriendItem(
    val owner: String,
    val kind: String,
    @SerialName("item_key") val itemKey: String,
    val title: String = "",
    val subtitle: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    val status: String = "",
    val rating: Double? = null,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("finished_at") val finishedAt: String? = null,
)

/** A reaction a friend left on one of my movies, series or books. */
@Serializable
data class ReceivedReaction(
    @SerialName("from_user") val fromUser: String,
    @SerialName("display_name") val displayName: String = "",
    val kind: String,
    @SerialName("item_key") val itemKey: String,
    @SerialName("item_title") val itemTitle: String = "",
    val reaction: String,
    @SerialName("created_at") val createdAt: String = "",
)

/** A reaction I left on a friend's item, to show it as chosen. */
@Serializable
data class SentReaction(
    @SerialName("to_user") val toUser: String,
    val kind: String,
    @SerialName("item_key") val itemKey: String,
    val reaction: String,
)

/** One evening of Ce soir ? played online, as the database keeps it. */
@Serializable
data class TonightSessionRow(
    val id: String,
    val host: String,
    val guest: String,
    val deck: JsonElement = JsonArray(emptyList()),
    val round: Int = 1,
    val status: String = "invited",
    @SerialName("host_progress") val hostProgress: Int = 0,
    @SerialName("guest_progress") val guestProgress: Int = 0,
    @SerialName("host_pick") val hostPick: Int? = null,
    @SerialName("guest_pick") val guestPick: Int? = null,
    val winner: Int? = null,
    val via: String? = null,
    @SerialName("cancelled_by") val cancelledBy: String? = null,
    @SerialName("created_at") val createdAt: String = "",
) {
    val deckSize: Int get() = (deck as? JsonArray)?.size ?: 0
}

@Serializable
data class TonightVote(@SerialName("user_id") val userId: String, val card: Int, val verdict: String)

enum class SocialStatus { IDLE, LOADING, READY, NOT_SET_UP, ERROR }

enum class AddFriendResult { REQUESTED, ACCEPTED, ALREADY, SELF, NOT_FOUND, FAILED }

/** The first word of a display name, as the social screens address people. */
fun firstNameOf(name: String): String = name.trim().substringBefore(' ')

/**
 * Friends, what they share, reactions and the online evenings of Ce soir ?, on top of the tables
 * and functions of `supabase-social-migration.sql`. Everything a friend sees of someone goes
 * through those functions, which never return personal notes.
 */
class SocialRepository(
    private val client: SupabaseClient,
    private val auth: AuthRepository,
    private val api: NookMindApi,
    private val scope: CoroutineScope,
) {
    private val _status = MutableStateFlow(SocialStatus.IDLE)
    val status: StateFlow<SocialStatus> = _status

    private val _profile = MutableStateFlow<SocialProfile?>(null)
    val profile: StateFlow<SocialProfile?> = _profile

    private val _friends = MutableStateFlow<List<Friend>>(emptyList())
    val friends: StateFlow<List<Friend>> = _friends

    private val _library = MutableStateFlow<List<FriendItem>>(emptyList())
    /** Everything friends share, newest first. */
    val library: StateFlow<List<FriendItem>> = _library

    private val _received = MutableStateFlow<List<ReceivedReaction>>(emptyList())
    val received: StateFlow<List<ReceivedReaction>> = _received

    private val _sent = MutableStateFlow<List<SentReaction>>(emptyList())
    val sent: StateFlow<List<SentReaction>> = _sent

    private val _invitations = MutableStateFlow<List<TonightSessionRow>>(emptyList())
    /** Evenings a friend invited me to in the last hours, still waiting for me. */
    val invitations: StateFlow<List<TonightSessionRow>> = _invitations

    private var seeded = false

    init {
        scope.launch {
            auth.state.collectLatest { state ->
                when (state) {
                    is AuthState.SignedIn -> if (!seeded) refresh()
                    AuthState.SignedOut -> clear()
                    AuthState.Loading -> Unit
                }
            }
        }
    }

    val myId: String? get() = auth.currentUser?.id

    fun friendById(id: String): Friend? = _friends.value.firstOrNull { it.userId == id }

    /** Profile, friends, their shared libraries and the reactions, in one go. */
    suspend fun refresh() {
        val user = auth.currentUser ?: return
        if (seeded) return
        if (_status.value != SocialStatus.READY) _status.value = SocialStatus.LOADING
        try {
            val name = user.displayName("").trim()
            var profile = decode(SocialProfile.serializer(), rpc("ensure_profile", buildJsonObject { if (name.isNotEmpty()) put("p_name", name) }))
            // Keep the name and the picture friends see in step with the account.
            val avatar = user.avatarUrl
            if ((name.isNotEmpty() && name != profile.displayName) || (avatar != null && avatar != profile.avatarUrl)) {
                runCatching {
                    client.from("profiles").update(buildJsonObject {
                        if (name.isNotEmpty()) put("display_name", name)
                        if (avatar != null) put("avatar_url", avatar)
                    }) { filter { eq("id", user.id) } }
                    profile = profile.copy(displayName = name.ifEmpty { profile.displayName }, avatarUrl = avatar ?: profile.avatarUrl)
                }
            }
            _profile.value = profile
            _friends.value = decodeList(Friend.serializer(), rpc("my_friends"))
            _library.value = decodeList(FriendItem.serializer(), rpc("friends_library")).sortedByDescending { it.createdAt }
            _received.value = decodeList(ReceivedReaction.serializer(), rpc("my_reactions"))
            _sent.value = decodeList(
                SentReaction.serializer(),
                client.from("reactions").select { filter { eq("from_user", user.id) } }.data,
            )
            refreshInvitations()
            _status.value = SocialStatus.READY
        } catch (t: Throwable) {
            logDebug("social", "refresh failed", t)
            _status.value = if (t.isMissingSetup()) SocialStatus.NOT_SET_UP else SocialStatus.ERROR
        }
    }

    /** Just the pending invitations, cheap enough to poll while the Social tab is open. */
    suspend fun refreshInvitations() {
        val user = auth.currentUser ?: return
        if (seeded) return
        runCatching {
            val since = (Clock.System.now() - 6.hours).toString()
            val rows = decodeList(TonightSessionRow.serializer(), client.from("tonight_sessions").select {
                filter {
                    eq("guest", user.id)
                    eq("status", "invited")
                    gt("created_at", since)
                }
                order("created_at", Order.DESCENDING)
            }.data)
            _invitations.value = rows
        }.onFailure { logDebug("social", "invitations failed", it) }
    }

    // ── Friends ────────────────────────────────────────────────────────────

    suspend fun addFriend(code: String): AddFriendResult {
        val clean = code.trim().uppercase().filter { it.isLetterOrDigit() }
        if (clean.isEmpty()) return AddFriendResult.NOT_FOUND
        val result = try {
            when (AppJson.decodeFromString(String.serializer(), rpc("request_friend", buildJsonObject { put("p_code", clean) }))) {
                "requested" -> AddFriendResult.REQUESTED
                "accepted" -> AddFriendResult.ACCEPTED
                "already" -> AddFriendResult.ALREADY
                "self" -> AddFriendResult.SELF
                else -> AddFriendResult.NOT_FOUND
            }
        } catch (t: Throwable) {
            logDebug("social", "request_friend failed", t)
            return AddFriendResult.FAILED
        }
        reload()
        when (result) {
            AddFriendResult.REQUESTED -> notify(buildJsonObject { put("type", "friend_request"); put("code", clean) })
            // They had already asked: entering their code accepted it.
            AddFriendResult.ACCEPTED -> notify(buildJsonObject { put("type", "friend_accepted"); put("code", clean) })
            else -> Unit
        }
        return result
    }

    suspend fun accept(friend: Friend): Boolean = try {
        rpc("accept_friend", buildJsonObject { put("p_friendship", friend.friendshipId) })
        reload()
        notify(buildJsonObject { put("type", "friend_accepted"); put("user_id", friend.userId) })
        true
    } catch (t: Throwable) {
        logDebug("social", "accept failed", t)
        false
    }

    /** Declines a request, cancels mine, or removes a friend: the friendship row goes either way. */
    suspend fun remove(friend: Friend): Boolean = try {
        client.from("friendships").delete { filter { eq("id", friend.friendshipId) } }
        reload()
        true
    } catch (t: Throwable) {
        logDebug("social", "remove friend failed", t)
        false
    }

    suspend fun updateSharing(movies: Boolean, series: Boolean, books: Boolean, ratings: Boolean): Boolean {
        val user = auth.currentUser ?: return false
        val before = _profile.value ?: return false
        _profile.value = before.copy(shareMovies = movies, shareSeries = series, shareBooks = books, shareRatings = ratings)
        return try {
            client.from("profiles").update(buildJsonObject {
                put("share_movies", movies)
                put("share_series", series)
                put("share_books", books)
                put("share_ratings", ratings)
            }) { filter { eq("id", user.id) } }
            true
        } catch (t: Throwable) {
            logDebug("social", "sharing update failed", t)
            _profile.value = before
            false
        }
    }

    // ── Reactions ──────────────────────────────────────────────────────────

    /** Sets my reaction to a friend's item, or takes it back when it is the same one again. */
    suspend fun react(item: FriendItem, reaction: String): Boolean {
        val user = auth.currentUser ?: return false
        val current = _sent.value.firstOrNull { it.toUser == item.owner && it.kind == item.kind && it.itemKey == item.itemKey }
        val before = _sent.value
        return try {
            if (current?.reaction == reaction) {
                _sent.update { list -> list - current }
                client.from("reactions").delete {
                    filter {
                        eq("from_user", user.id)
                        eq("to_user", item.owner)
                        eq("kind", item.kind)
                        eq("item_key", item.itemKey)
                    }
                }
            } else {
                val row = SentReaction(item.owner, item.kind, item.itemKey, reaction)
                _sent.update { list -> list.filterNot { it.toUser == item.owner && it.kind == item.kind && it.itemKey == item.itemKey } + row }
                client.from("reactions").upsert(buildJsonObject {
                    put("from_user", user.id)
                    put("to_user", item.owner)
                    put("kind", item.kind)
                    put("item_key", item.itemKey)
                    put("item_title", item.title)
                    put("reaction", reaction)
                    put("created_at", Clock.System.now().toString())
                }) { onConflict = "from_user,to_user,kind,item_key" }
                notify(buildJsonObject {
                    put("type", "reaction")
                    put("to_user", item.owner)
                    put("kind", item.kind)
                    put("item_key", item.itemKey)
                })
            }
            true
        } catch (t: Throwable) {
            logDebug("social", "reaction failed", t)
            _sent.value = before
            false
        }
    }

    // ── Ce soir ? online ───────────────────────────────────────────────────

    suspend fun createTonight(guest: String, deck: JsonArray): TonightSessionRow? {
        val user = auth.currentUser ?: return null
        return try {
            val row = decodeList(TonightSessionRow.serializer(), client.from("tonight_sessions").insert(buildJsonObject {
                put("host", user.id)
                put("guest", guest)
                put("deck", deck)
            }) { select() }.data).first()
            notify(buildJsonObject { put("type", "tonight_invite"); put("session_id", row.id) })
            row
        } catch (t: Throwable) {
            logDebug("social", "create session failed", t)
            null
        }
    }

    /** Sends the invitation again, for a friend who has not opened it yet. */
    suspend fun nudge(sessionId: String) {
        notify(buildJsonObject { put("type", "tonight_invite"); put("session_id", sessionId) })
    }

    suspend fun joinTonight(sessionId: String): TonightSessionRow? = sessionCall("tonight_join", buildJsonObject { put("p_session", sessionId) })
        .also { _invitations.update { list -> list.filterNot { it.id == sessionId } } }

    suspend fun tonightSession(sessionId: String): TonightSessionRow? = try {
        decodeList(TonightSessionRow.serializer(), client.from("tonight_sessions").select { filter { eq("id", sessionId) } }.data).firstOrNull()
    } catch (t: Throwable) {
        logDebug("social", "session poll failed", t)
        null
    }

    suspend fun vote(sessionId: String, card: Int, verdict: String): TonightSessionRow? = sessionCall("tonight_vote", buildJsonObject {
        put("p_session", sessionId)
        put("p_card", card)
        put("p_verdict", verdict)
    })

    suspend fun undoVote(sessionId: String, card: Int): TonightSessionRow? = sessionCall("tonight_undo", buildJsonObject {
        put("p_session", sessionId)
        put("p_card", card)
    })

    suspend fun reveal(sessionId: String): List<TonightVote>? = try {
        decodeList(TonightVote.serializer(), rpc("tonight_reveal", buildJsonObject { put("p_session", sessionId) }))
    } catch (t: Throwable) {
        logDebug("social", "reveal failed", t)
        null
    }

    suspend fun pick(sessionId: String, card: Int, a: Int, b: Int): TonightSessionRow? = sessionCall("tonight_pick", buildJsonObject {
        put("p_session", sessionId)
        put("p_card", card)
        put("p_a", a)
        put("p_b", b)
    })

    suspend fun chance(sessionId: String, cards: List<Int>): TonightSessionRow? = sessionCall("tonight_chance", buildJsonObject {
        put("p_session", sessionId)
        putJsonArray("p_cards") { cards.forEach { add(JsonPrimitive(it)) } }
    })

    suspend fun redeal(sessionId: String, deck: JsonArray): TonightSessionRow? = sessionCall("tonight_redeal", buildJsonObject {
        put("p_session", sessionId)
        put("p_deck", deck)
    })

    suspend fun cancelTonight(sessionId: String) {
        runCatching { rpc("tonight_cancel", buildJsonObject { put("p_session", sessionId) }) }
            .onFailure { logDebug("social", "cancel failed", it) }
        _invitations.update { list -> list.filterNot { it.id == sessionId } }
    }

    /** Declines an invitation: the evening is called off for both. */
    fun decline(session: TonightSessionRow) {
        _invitations.update { list -> list.filterNot { it.id == session.id } }
        scope.launch { cancelTonight(session.id) }
    }

    // ── Screenshots ────────────────────────────────────────────────────────

    /** Fills the social state without the network (screenshot catalog). */
    fun seedLocal(
        profile: SocialProfile,
        friends: List<Friend>,
        library: List<FriendItem>,
        received: List<ReceivedReaction> = emptyList(),
        sent: List<SentReaction> = emptyList(),
        invitations: List<TonightSessionRow> = emptyList(),
    ) {
        seeded = true
        _profile.value = profile
        _friends.value = friends
        _library.value = library
        _received.value = received
        _sent.value = sent
        _invitations.value = invitations
        _status.value = SocialStatus.READY
    }

    // ── Plumbing ───────────────────────────────────────────────────────────

    private suspend fun reload() {
        runCatching {
            _friends.value = decodeList(Friend.serializer(), rpc("my_friends"))
            _library.value = decodeList(FriendItem.serializer(), rpc("friends_library")).sortedByDescending { it.createdAt }
        }.onFailure { logDebug("social", "reload failed", it) }
    }

    private fun clear() {
        if (seeded) return
        _status.value = SocialStatus.IDLE
        _profile.value = null
        _friends.value = emptyList()
        _library.value = emptyList()
        _received.value = emptyList()
        _sent.value = emptyList()
        _invitations.value = emptyList()
    }

    private suspend fun sessionCall(function: String, parameters: JsonObject): TonightSessionRow? = try {
        val data = rpc(function, parameters)
        if (data.isBlank() || data == "null") null else decode(TonightSessionRow.serializer(), data)
    } catch (t: Throwable) {
        logDebug("social", "$function failed", t)
        null
    }

    private suspend fun rpc(function: String, parameters: JsonObject = buildJsonObject {}): String =
        client.postgrest.rpc(function, parameters).data

    /** Fire and forget: a notification that does not go out never blocks the action itself. */
    private fun notify(body: JsonObject) {
        scope.launch {
            val token = auth.accessToken() ?: return@launch
            val result = api.callJson("/api/social/notify", "POST", body, token)
            if (!result.ok) logDebug("social", "notify ${result.status}: ${result.rawText?.take(160)}")
        }
    }

    private fun <T> decode(serializer: kotlinx.serialization.KSerializer<T>, data: String): T = AppJson.decodeFromString(serializer, data)

    private fun <T> decodeList(serializer: kotlinx.serialization.KSerializer<T>, data: String): List<T> =
        if (data.isBlank() || data == "null") emptyList() else AppJson.decodeFromString(ListSerializer(serializer), data)

    private fun Throwable.isMissingSetup(): Boolean =
        this is PostgrestRestException && code in setOf("PGRST202", "PGRST205", "42P01", "42883")

    companion object {
        /** Whether an invitation is still fresh enough to show (the poll filters the same way). */
        fun isRecent(createdAt: String, now: Instant = Clock.System.now()): Boolean =
            runCatching { now - Instant.parse(createdAt) < 6.hours }.getOrDefault(false)
    }
}
