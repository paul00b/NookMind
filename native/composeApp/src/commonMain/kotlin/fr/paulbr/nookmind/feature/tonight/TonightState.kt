package fr.paulbr.nookmind.feature.tonight

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import fr.paulbr.nookmind.app.AppContainer
import fr.paulbr.nookmind.core.data.Friend

enum class TonightStep { SETUP, DEALING, SWIPE, HANDOFF, WAITING, DUEL, MATCH }

/** How the movie of the evening was found, for the line above the ticket. */
enum class MatchVia { MATCH, SHARED_LOVE, FINAL, EXTRA_TIME, CHANCE }

/** Why the cards could not be dealt. */
enum class DealError { NOT_ENOUGH, ONLINE, GONE }

/** What the online game hears of each move, to tell the other phone. */
interface TonightSync {
    fun vote(card: Int, verdict: Verdict)
    fun undo(card: Int)
}

/** The other phone of an online evening: where its player is, as the last poll saw it. */
@Stable
class TonightRemote(val sessionId: String, val isHost: Boolean, val partnerId: String) {
    var round by mutableIntStateOf(1)
    var partnerJoined by mutableStateOf(false)
    var partnerProgress by mutableIntStateOf(0)
    var partnerLeft by mutableStateOf(false)
    var myPick by mutableStateOf<Int?>(null)
    var partnerPicked by mutableStateOf(false)
    var revealedRound by mutableIntStateOf(0)
    var nudged by mutableStateOf(false)
}

/**
 * One evening of Ce soir ?: the setup, the deck, both players' verdicts and where they lead. On a
 * single phone both players take turns; online ([remote] set) each one plays on their own phone and
 * the partner's verdicts arrive once both went through the deck. Plain Compose state, so the
 * screenshot catalog can build any step directly.
 */
@Stable
class TonightState(partnerName: String = "", services: Set<StreamingService> = emptySet()) {
    var step by mutableStateOf(TonightStep.SETUP)
    var partnerName by mutableStateOf(partnerName)
    var services by mutableStateOf(services)
    var shortOnly by mutableStateOf(false)

    /** The friend picked in the setup, to play online; null for someone on the same phone. */
    var friend by mutableStateOf<Friend?>(null)
    var remote by mutableStateOf<TonightRemote?>(null)
    var sync: TonightSync? = null

    var deck by mutableStateOf<List<TonightCard>>(emptyList())
        private set
    var player by mutableStateOf(Player.ME)
        private set
    var index by mutableIntStateOf(0)
        private set
    val mine = mutableStateMapOf<Int, Verdict>()
    val theirs = mutableStateMapOf<Int, Verdict>()

    var outcome by mutableStateOf<Outcome?>(null)
        private set
    var winner by mutableStateOf<Int?>(null)
        private set
    var via by mutableStateOf(MatchVia.MATCH)
        private set

    /** A winner drawn by chance, waiting for the roulette of the duel before the ticket. */
    var pendingChance by mutableStateOf<Int?>(null)

    var dealError by mutableStateOf<DealError?>(null)
    var showRules by mutableStateOf(false)

    /** Stills of each movie for the long-press preview, fetched when its card comes up. */
    val stills = mutableStateMapOf<Int, List<String>>()

    val winningCard: TonightCard? get() = winner?.let { deck.getOrNull(it) }

    fun verdictsOf(who: Player): Map<Int, Verdict> = if (who == Player.ME) mine else theirs

    fun startRound(cards: List<TonightCard>) {
        deck = cards
        mine.clear()
        theirs.clear()
        index = 0
        player = Player.ME
        outcome = null
        winner = null
        pendingChance = null
        step = TonightStep.SWIPE
    }

    fun decide(verdict: Verdict) {
        if (index >= deck.size) return
        val card = index
        (if (player == Player.ME) mine else theirs)[card] = verdict
        index++
        sync?.vote(card, verdict)
        if (index >= deck.size) finishTurn()
    }

    fun undo() {
        if (index == 0) return
        index--
        (if (player == Player.ME) mine else theirs).remove(index)
        sync?.undo(index)
        // Online, waiting for the other one is left by taking a card back.
        if (step == TonightStep.WAITING) step = TonightStep.SWIPE
    }

    fun startPartnerTurn() {
        player = Player.PARTNER
        index = 0
        step = TonightStep.SWIPE
    }

    fun win(cardIndex: Int, how: MatchVia) {
        winner = cardIndex
        via = how
        pendingChance = null
        step = TonightStep.MATCH
    }

    private fun finishTurn() {
        when {
            remote != null -> step = TonightStep.WAITING
            player == Player.ME -> step = TonightStep.HANDOFF
            else -> settle()
        }
    }

    /** Online: the partner's verdicts, once both went through the deck without a shared yes. */
    fun reveal(partner: Map<Int, Verdict>) {
        theirs.clear()
        theirs.putAll(partner)
        settle()
    }

    private fun settle() {
        val result = outcomeOf(deck.size, mine, theirs)
        outcome = result
        if (result is Outcome.Match) {
            val shared = mine[result.index] == Verdict.LOVE && theirs[result.index] == Verdict.LOVE
            win(result.index, if (shared) MatchVia.SHARED_LOVE else MatchVia.MATCH)
        } else {
            step = TonightStep.DUEL
        }
    }

    /** Back to the setup, for another evening (or another try at this one). */
    fun reset() {
        deck = emptyList()
        mine.clear()
        theirs.clear()
        index = 0
        player = Player.ME
        outcome = null
        winner = null
        pendingChance = null
        dealError = null
        remote = null
        sync = null
        step = TonightStep.SETUP
    }

    /** Screenshot catalog only: puts the state at a given point of a round. */
    fun preview(cards: List<TonightCard>, step: TonightStep, index: Int = 0, player: Player = Player.ME, outcome: Outcome? = null, winner: Int? = null, via: MatchVia = MatchVia.MATCH) {
        deck = cards
        this.index = index
        this.player = player
        this.outcome = outcome
        this.winner = winner
        this.via = via
        this.step = step
    }
}

@Composable
fun rememberTonightState(container: AppContainer): TonightState = remember {
    TonightState(container.prefs.tonightPartner, StreamingService.fromKeys(container.prefs.tonightServices)).apply {
        val last = container.prefs.tonightFriend
        if (last.isNotEmpty()) friend = container.social.friends.value.firstOrNull { it.userId == last && it.accepted }
    }
}
