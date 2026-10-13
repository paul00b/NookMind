package fr.paulbr.nookmind.core.platform

import com.russhwolf.settings.Settings
import kotlinx.browser.localStorage

/**
 * One store of the app in the browser's localStorage, its keys prefixed with [prefix]: the stores
 * ("prefs", "cache", "session") share one localStorage, and clearing the cache must not sign out.
 */
internal class LocalStorageSettings(private val prefix: String) : Settings {
    private fun full(key: String) = prefix + key
    private fun read(key: String): String? = localStorage.getItem(full(key))
    private fun write(key: String, value: String) = localStorage.setItem(full(key), value)

    override val keys: Set<String>
        get() = buildSet {
            for (i in 0 until localStorage.length) {
                val key = localStorage.key(i) ?: continue
                if (key.startsWith(prefix)) add(key.removePrefix(prefix))
            }
        }

    override val size: Int get() = keys.size

    override fun clear() = keys.forEach(::remove)

    override fun remove(key: String) = localStorage.removeItem(full(key))

    override fun hasKey(key: String): Boolean = read(key) != null

    override fun putInt(key: String, value: Int) = write(key, value.toString())
    override fun getInt(key: String, defaultValue: Int): Int = getIntOrNull(key) ?: defaultValue
    override fun getIntOrNull(key: String): Int? = read(key)?.toIntOrNull()

    override fun putLong(key: String, value: Long) = write(key, value.toString())
    override fun getLong(key: String, defaultValue: Long): Long = getLongOrNull(key) ?: defaultValue
    override fun getLongOrNull(key: String): Long? = read(key)?.toLongOrNull()

    override fun putString(key: String, value: String) = write(key, value)
    override fun getString(key: String, defaultValue: String): String = read(key) ?: defaultValue
    override fun getStringOrNull(key: String): String? = read(key)

    override fun putFloat(key: String, value: Float) = write(key, value.toString())
    override fun getFloat(key: String, defaultValue: Float): Float = getFloatOrNull(key) ?: defaultValue
    override fun getFloatOrNull(key: String): Float? = read(key)?.toFloatOrNull()

    override fun putDouble(key: String, value: Double) = write(key, value.toString())
    override fun getDouble(key: String, defaultValue: Double): Double = getDoubleOrNull(key) ?: defaultValue
    override fun getDoubleOrNull(key: String): Double? = read(key)?.toDoubleOrNull()

    override fun putBoolean(key: String, value: Boolean) = write(key, value.toString())
    override fun getBoolean(key: String, defaultValue: Boolean): Boolean = getBooleanOrNull(key) ?: defaultValue
    override fun getBooleanOrNull(key: String): Boolean? = read(key)?.toBooleanStrictOrNull()
}
