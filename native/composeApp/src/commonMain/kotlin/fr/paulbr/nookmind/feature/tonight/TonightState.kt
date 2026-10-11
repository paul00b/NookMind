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

enum class TonightStep { SETUP, DEALING, SWIPE, HANDOFF, DUEL, MATCH }

/** How the movie of the evening was found, for the line above the ticket. */
enum class MatchVia { MATCH, SHARED_LOVE, FINAL, EXTRA_TIME, CHANCE }

/**
 * One evening of Ce soir ? on a single phone: the setup, the deck, both players' verdicts and where
 * they lead. Plain Compose state, so the screenshot catalog can build any step directly.
 */
@Stable
class TonightState(partnerName: String = "", services: Set<StreamingService> = emptySet()) {
    var step by mutableStateOf(TonightStep.SETUP)
    var partnerName by mutableStateOf(partnerName)
    var services by mutableStateOf(services)
    var shortOnly by mutableStateOf(false)

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

    var dealError by mutableStateOf(false)
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
        step = TonightStep.SWIPE
    }

    fun decide(verdict: Verdict) {
        if (index >= deck.size) return
        (if (player == Player.ME) mine else theirs)[index] = verdict
        index++
        if (index >= deck.size) finishTurn()
    }

    fun undo() {
        if (index == 0) return
        index--
        (if (player == Player.ME) mine else theirs).remove(index)
    }

    fun startPartnerTurn() {
        player = Player.PARTNER
        index = 0
        step = TonightStep.SWIPE
    }

    fun win(cardIndex: Int, how: MatchVia) {
        winner = cardIndex
        via = how
        step = TonightStep.MATCH
    }

    private fun finishTurn() {
        if (player == Player.ME) {
            step = TonightStep.HANDOFF
            return
        }
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
        dealError = false
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
    TonightState(container.prefs.tonightPartner, StreamingService.fromKeys(container.prefs.tonightServices))
}
