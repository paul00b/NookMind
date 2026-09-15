package fr.paulbr.nookmind.core.network

import com.russhwolf.settings.Settings
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.KSerializer
import kotlin.time.Clock

/**
 * Persistent TTL cache — the native counterpart of the `sessionStorage` caches of the web app
 * (TMDB 6 h, IMDb 24 h, releases 24 h). "Update application" in Settings calls [clear].
 */
class JsonCache(private val settings: Settings, private val namespace: String = "cache") {
    private val json = AppJson

    fun <T> get(key: String, serializer: KSerializer<T>, ttlMs: Long): T? {
        val raw = settings.getStringOrNull(fullKey(key)) ?: return null
        return runCatching {
            val entry = json.decodeFromString(Entry.serializer(), raw)
            if (Clock.System.now().toEpochMilliseconds() - entry.ts > ttlMs) {
                settings.remove(fullKey(key))
                null
            } else {
                json.decodeFromString(serializer, entry.data)
            }
        }.getOrNull()
    }

    fun <T> put(key: String, serializer: KSerializer<T>, value: T) {
        runCatching {
            val entry = Entry(Clock.System.now().toEpochMilliseconds(), json.encodeToString(serializer, value))
            settings.putString(fullKey(key), json.encodeToString(Entry.serializer(), entry))
        }
    }

    fun remove(key: String) = settings.remove(fullKey(key))

    fun clear() {
        settings.keys.filter { it.startsWith("$namespace:") }.forEach { settings.remove(it) }
    }

    private fun fullKey(key: String) = "$namespace:$key"

    @kotlinx.serialization.Serializable
    private data class Entry(val ts: Long, val data: String)
}

/**
 * De-duplicates concurrent identical requests: callers awaiting the same key share one
 * in-flight computation (the `Map<key, Promise>` pattern of tmdb.ts / imdb.ts).
 */
class InFlightRequests<K, V> {
    private val mutex = Mutex()
    private val inFlight = HashMap<K, CompletableDeferred<V>>()

    suspend fun run(key: K, block: suspend () -> V): V {
        val (deferred, owner) = mutex.withLock {
            val existing = inFlight[key]
            if (existing != null) existing to false
            else CompletableDeferred<V>().also { inFlight[key] = it } to true
        }
        if (!owner) return deferred.await()
        try {
            val value = block()
            deferred.complete(value)
            return value
        } catch (t: Throwable) {
            deferred.completeExceptionally(t)
            throw t
        } finally {
            mutex.withLock { inFlight.remove(key) }
        }
    }
}
