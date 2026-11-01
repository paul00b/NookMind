package fr.paulbr.nookmind.feature.tonight

import fr.paulbr.nookmind.app.AppContainer
import fr.paulbr.nookmind.core.domain.isSameTitle
import fr.paulbr.nookmind.core.model.MovieStatus
import fr.paulbr.nookmind.core.network.TmdbApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * A movie I swipe "seen" goes in my library as watched, so I neither search for it afterwards nor
 * get it dealt again: the decks leave out what I have watched. Taking the card back undoes it.
 *
 * Series stay out: their "seen" also means "started", which says nothing of where I am in them.
 */
internal class SeenToLibrary(private val container: AppContainer) {
    private val jobs = mutableMapOf<Int, Job>()
    private val undos = mutableMapOf<Int, suspend () -> Unit>()

    fun seen(card: TonightCard) {
        if (card.kind != TonightKind.MOVIE) return
        jobs[card.tmdbId] = container.scope.launch { mark(card)?.let { undos[card.tmdbId] = it } }
    }

    fun unseen(card: TonightCard) {
        if (card.kind != TonightKind.MOVIE) return
        container.scope.launch {
            jobs.remove(card.tmdbId)?.join()
            undos.remove(card.tmdbId)?.invoke()
        }
    }

    /** Marks the movie watched and returns how to take it back, or null when nothing changed. */
    private suspend fun mark(card: TonightCard): (suspend () -> Unit)? {
        val movies = container.movies
        val existing = movies.items.value.firstOrNull { isSameTitle(it.tmdbId, it.title, card.tmdbId, card.title) }
        return when {
            existing == null -> {
                // When I saw it is unknown: no date rather than today's.
                val added = movies.add(
                    TmdbApi.extractMovieData(card.movie).copy(status = MovieStatus.WATCHED, rating = null, personalNote = null, watchedDate = null),
                ) ?: return null
                val undo: suspend () -> Unit = { movies.delete(added.id) }
                undo
            }
            existing.status == MovieStatus.WANT_TO_WATCH -> {
                movies.update(existing.id, buildJsonObject { put("status", MovieStatus.WATCHED.key) }) ?: return null
                val undo: suspend () -> Unit = { movies.update(existing.id, buildJsonObject { put("status", MovieStatus.WANT_TO_WATCH.key) }) }
                undo
            }
            else -> null
        }
    }
}
