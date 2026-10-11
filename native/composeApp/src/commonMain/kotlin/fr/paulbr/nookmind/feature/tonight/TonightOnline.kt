package fr.paulbr.nookmind.feature.tonight

import fr.paulbr.nookmind.core.data.SocialRepository
import fr.paulbr.nookmind.core.data.TonightSessionRow
import fr.paulbr.nookmind.core.model.TmdbCredits
import fr.paulbr.nookmind.core.model.TmdbCrewMember
import fr.paulbr.nookmind.core.model.TmdbGenre
import fr.paulbr.nookmind.core.model.TmdbMovie
import fr.paulbr.nookmind.core.model.WatchProvider
import fr.paulbr.nookmind.core.network.AppJson
import fr.paulbr.nookmind.core.network.DbJson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement

/**
 * A card as both phones share it, in the deck of the session. Reasons name a player by id, so each
 * phone can say "your list" or "Léa's list".
 */
@Serializable
internal data class SharedCard(
    val id: Int,
    val title: String,
    val overview: String = "",
    @SerialName("poster_path") val posterPath: String? = null,
    val poster: String? = null,
    @SerialName("release_date") val releaseDate: String = "",
    val year: String? = null,
    val runtime: Int? = null,
    val director: String? = null,
    val genres: List<String> = emptyList(),
    @SerialName("provider_id") val providerId: Int? = null,
    @SerialName("provider_name") val providerName: String? = null,
    @SerialName("provider_logo") val providerLogo: String? = null,
    val link: String? = null,
    val reason: String = "trending",
    val by: String? = null,
    val about: String? = null,
)

/** The deck as it goes in the session, seen from [myId] dealing for [partnerId]. */
internal fun encodeDeck(cards: List<TonightCard>, myId: String, partnerId: String): JsonArray =
    DbJson.encodeToJsonElement(ListSerializer(SharedCard.serializer()), cards.map { it.toShared(myId, partnerId) }) as JsonArray

/** The deck of a session, with the reasons said from [myId]'s side. */
internal fun decodeDeck(deck: JsonElement, myId: String, partnerName: String): List<TonightCard> =
    runCatching { AppJson.decodeFromJsonElement(ListSerializer(SharedCard.serializer()), deck) }.getOrDefault(emptyList())
        .map { it.toCard(myId, partnerName) }

internal fun TonightCard.toShared(myId: String, partnerId: String): SharedCard {
    val (kind, by, about) = when (val r = reason) {
        CardReason.Watchlist -> Triple("watchlist", myId, null)
        is CardReason.PartnerWatchlist -> Triple("watchlist", partnerId, null)
        CardReason.BothWatchlists -> Triple("both", null, null)
        is CardReason.BecauseYouLiked -> Triple("liked", myId, r.title)
        is CardReason.PartnerLiked -> Triple("liked", partnerId, r.title)
        CardReason.Trending -> Triple("trending", null, null)
    }
    return SharedCard(
        id = tmdbId, title = title, overview = movie.overview, posterPath = movie.posterPath, poster = posterUrl,
        releaseDate = movie.releaseDate, year = year, runtime = runtime, director = director, genres = genres,
        providerId = provider?.providerId, providerName = provider?.providerName, providerLogo = provider?.logoPath,
        link = providersLink, reason = kind, by = by, about = about,
    )
}

internal fun SharedCard.toCard(myId: String, partnerName: String): TonightCard {
    val mineToo = by == myId
    val reason = when (reason) {
        "watchlist" -> if (mineToo) CardReason.Watchlist else CardReason.PartnerWatchlist(partnerName)
        "both" -> CardReason.BothWatchlists
        "liked" -> if (mineToo) CardReason.BecauseYouLiked(about.orEmpty()) else CardReason.PartnerLiked(partnerName, about.orEmpty())
        else -> CardReason.Trending
    }
    return TonightCard(
        movie = TmdbMovie(
            id = id, title = title, overview = overview, posterPath = posterPath, releaseDate = releaseDate, runtime = runtime,
            genres = genres.map { TmdbGenre(name = it) },
            credits = director?.let { TmdbCredits(crew = listOf(TmdbCrewMember("Director", it))) },
        ),
        posterUrl = poster, year = year, runtime = runtime, director = director, genres = genres,
        provider = if (providerId != null && providerName != null) WatchProvider(providerId, providerName, providerLogo.orEmpty()) else null,
        providersLink = link, reason = reason,
    )
}

internal val Verdict.key: String get() = name.lowercase()

internal fun verdictOfKey(key: String): Verdict? = Verdict.entries.firstOrNull { it.key == key }

internal fun matchViaOf(key: String?): MatchVia = when (key) {
    "shared_love" -> MatchVia.SHARED_LOVE
    "extra_time" -> MatchVia.EXTRA_TIME
    "chance" -> MatchVia.CHANCE
    else -> MatchVia.MATCH
}

/**
 * Keeps an online evening in step: sends this phone's moves in order (retrying a few times on a bad
 * connection) and applies what the session says of the other phone, from the answers and the poll.
 */
internal class TonightLink(
    private val social: SocialRepository,
    private val state: TonightState,
    private val remote: TonightRemote,
    private val myId: String,
    private val scope: CoroutineScope,
) : TonightSync {
    private val queue = Channel<suspend () -> TonightSessionRow?>(Channel.UNLIMITED)

    init {
        scope.launch {
            for (call in queue) call()?.let(::applyRow)
        }
    }

    override fun vote(card: Int, verdict: Verdict) {
        queue.trySend { retrying { social.vote(remote.sessionId, card, verdict.key) } }
    }

    override fun undo(card: Int) {
        queue.trySend { retrying { social.undoVote(remote.sessionId, card) } }
    }

    fun pick(card: Int, a: Int, b: Int) {
        remote.myPick = card
        queue.trySend { retrying { social.pick(remote.sessionId, card, a, b) } }
    }

    fun chance(cards: List<Int>) {
        queue.trySend { retrying { social.chance(remote.sessionId, cards) } }
    }

    /** A new deck for both phones; false when the session did not take it. */
    suspend fun redeal(cards: List<TonightCard>): Boolean {
        val row = social.redeal(remote.sessionId, encodeDeck(cards, myId, remote.partnerId)) ?: return false
        if (row.round <= remote.round) return false
        remote.round = row.round
        resetRound()
        state.startRound(cards)
        applyRow(row)
        return true
    }

    suspend fun poll() {
        social.tonightSession(remote.sessionId)?.let(::applyRow)
    }

    fun applyRow(row: TonightSessionRow) {
        if (state.remote !== remote || state.step == TonightStep.MATCH) return
        val partnerProgress = if (remote.isHost) row.guestProgress else row.hostProgress
        if (row.status != "invited" || partnerProgress > 0) remote.partnerJoined = true
        if (row.status == "cancelled") {
            if (row.cancelledBy != myId) remote.partnerLeft = true
            return
        }
        if (row.round > remote.round) {
            // The other phone dealt again.
            remote.round = row.round
            resetRound()
            state.startRound(decodeDeck(row.deck, myId, state.partnerName))
            return
        }
        if (row.round < remote.round) return
        remote.partnerProgress = partnerProgress
        remote.partnerPicked = (if (remote.isHost) row.guestPick else row.hostPick) != null
        row.winner?.let { winner ->
            val via = matchViaOf(row.via)
            if (via == MatchVia.CHANCE && state.step == TonightStep.DUEL) {
                if (state.pendingChance == null) state.pendingChance = winner
            } else {
                state.win(winner, via)
            }
            return
        }
        val size = state.deck.size
        if (state.step == TonightStep.WAITING && size > 0 && row.hostProgress >= size && row.guestProgress >= size && remote.revealedRound != row.round) {
            remote.revealedRound = row.round
            scope.launch {
                val votes = social.reveal(remote.sessionId)
                if (votes.isNullOrEmpty()) {
                    // Not visible yet (or the network failed): the next poll asks again.
                    remote.revealedRound = 0
                    return@launch
                }
                if (state.remote !== remote || state.step != TonightStep.WAITING) return@launch
                state.reveal(votes.filter { it.userId != myId }.mapNotNull { v -> verdictOfKey(v.verdict)?.let { v.card to it } }.toMap())
            }
        }
    }

    private fun resetRound() {
        remote.partnerProgress = 0
        remote.myPick = null
        remote.partnerPicked = false
        remote.revealedRound = 0
    }

    private suspend fun retrying(call: suspend () -> TonightSessionRow?): TonightSessionRow? {
        repeat(3) { attempt ->
            call()?.let { return it }
            delay(700L * (attempt + 1))
        }
        return null
    }
}
