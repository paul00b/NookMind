package fr.paulbr.nookmind.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ─────────────────────────────────────────────────────────────────────────────
// Mirrors src/types/index.ts of the web app. Field names keep the snake_case
// database column names through @SerialName so Supabase rows decode directly.
// ─────────────────────────────────────────────────────────────────────────────

enum class MediaMode(val key: String) {
    BOOKS("books"),
    MOVIES("movies"),
    SERIES("series");

    companion object {
        fun fromKey(key: String?): MediaMode = entries.firstOrNull { it.key == key } ?: BOOKS
    }
}

enum class ThemeMode(val key: String) {
    LIGHT("light"),
    DARK("dark"),
    SYSTEM("system");

    companion object {
        fun fromKey(key: String?): ThemeMode = entries.firstOrNull { it.key == key } ?: SYSTEM
    }
}

interface LibraryItem {
    val id: String
    val userId: String
    val createdAt: String
    val title: String
}

interface CollectionItem {
    val id: String
    val userId: String
    val title: String
    val createdAt: String
    val itemIds: List<String>
}

// ── Books ────────────────────────────────────────────────────────────────────

@Serializable
enum class BookStatus(val key: String) {
    @SerialName("read") READ("read"),
    @SerialName("want_to_read") WANT_TO_READ("want_to_read"),
    @SerialName("reading") READING("reading");

    companion object {
        fun fromKey(key: String): BookStatus = entries.first { it.key == key }
    }
}

@Serializable
data class Book(
    override val id: String = "",
    @SerialName("user_id") override val userId: String = "",
    @SerialName("google_books_id") val googleBooksId: String? = null,
    override val title: String,
    val author: String = "",
    val description: String? = null,
    @SerialName("cover_url") val coverUrl: String? = null,
    @SerialName("published_date") val publishedDate: String? = null,
    @SerialName("page_count") val pageCount: Int? = null,
    val genre: String? = null,
    val status: BookStatus = BookStatus.WANT_TO_READ,
    val rating: Double? = null,
    @SerialName("personal_note") val personalNote: String? = null,
    @SerialName("current_page") val currentPage: Int? = null,
    @SerialName("created_at") override val createdAt: String = "",
) : LibraryItem

@Serializable
data class BookCategory(
    override val id: String,
    @SerialName("user_id") override val userId: String,
    override val title: String,
    @SerialName("created_at") override val createdAt: String,
    @SerialName("book_ids") override val itemIds: List<String> = emptyList(),
) : CollectionItem

// ── Movies ───────────────────────────────────────────────────────────────────

@Serializable
enum class MovieStatus(val key: String) {
    @SerialName("watched") WATCHED("watched"),
    @SerialName("want_to_watch") WANT_TO_WATCH("want_to_watch");

    companion object {
        fun fromKey(key: String): MovieStatus = entries.first { it.key == key }
    }
}

@Serializable
data class Movie(
    override val id: String = "",
    @SerialName("user_id") override val userId: String = "",
    @SerialName("tmdb_id") val tmdbId: Int? = null,
    override val title: String,
    val director: String = "",
    val description: String? = null,
    @SerialName("poster_url") val posterUrl: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    val runtime: Int? = null,
    val genre: String? = null,
    val status: MovieStatus = MovieStatus.WANT_TO_WATCH,
    @SerialName("watched_date") val watchedDate: String? = null,
    val rating: Double? = null,
    @SerialName("personal_note") val personalNote: String? = null,
    @SerialName("created_at") override val createdAt: String = "",
) : LibraryItem

@Serializable
data class MovieCategory(
    override val id: String,
    @SerialName("user_id") override val userId: String,
    override val title: String,
    @SerialName("created_at") override val createdAt: String,
    @SerialName("movie_ids") override val itemIds: List<String> = emptyList(),
) : CollectionItem

// ── Series ───────────────────────────────────────────────────────────────────

@Serializable
enum class SeriesStatus(val key: String) {
    @SerialName("watched") WATCHED("watched"),
    @SerialName("want_to_watch") WANT_TO_WATCH("want_to_watch"),
    @SerialName("watching") WATCHING("watching");

    companion object {
        fun fromKey(key: String): SeriesStatus = entries.first { it.key == key }
    }
}

@Serializable
data class Series(
    override val id: String = "",
    @SerialName("user_id") override val userId: String = "",
    @SerialName("tmdb_id") val tmdbId: Int? = null,
    override val title: String,
    val creator: String = "",
    val description: String? = null,
    @SerialName("poster_url") val posterUrl: String? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    val seasons: Int? = null,
    @SerialName("watched_seasons") val watchedSeasons: List<Int> = emptyList(),
    @SerialName("watched_episodes") val watchedEpisodes: Map<String, List<Int>> = emptyMap(),
    val genre: String? = null,
    val status: SeriesStatus = SeriesStatus.WANT_TO_WATCH,
    val rating: Double? = null,
    @SerialName("personal_note") val personalNote: String? = null,
    @SerialName("next_air_date") val nextAirDate: String? = null,
    @SerialName("next_season_number") val nextSeasonNumber: Int? = null,
    @SerialName("next_episode_number") val nextEpisodeNumber: Int? = null,
    @SerialName("created_at") override val createdAt: String = "",
) : LibraryItem

@Serializable
data class SeriesCategory(
    override val id: String,
    @SerialName("user_id") override val userId: String,
    override val title: String,
    @SerialName("created_at") override val createdAt: String,
    @SerialName("series_ids") override val itemIds: List<String> = emptyList(),
) : CollectionItem

// ── Google Books ─────────────────────────────────────────────────────────────

@Serializable
data class GoogleBookVolume(
    val id: String,
    val volumeInfo: GoogleVolumeInfo = GoogleVolumeInfo(),
)

@Serializable
data class GoogleVolumeInfo(
    val title: String = "",
    val authors: List<String>? = null,
    val description: String? = null,
    val imageLinks: GoogleImageLinks? = null,
    val publishedDate: String? = null,
    val pageCount: Int? = null,
    val categories: List<String>? = null,
)

@Serializable
data class GoogleImageLinks(
    val thumbnail: String? = null,
    val smallThumbnail: String? = null,
)

// ── TMDB ─────────────────────────────────────────────────────────────────────

@Serializable
data class TmdbGenre(val id: Int = 0, val name: String = "")

@Serializable
data class TmdbCastMember(
    val id: Int,
    val name: String = "",
    val character: String? = null,
    @SerialName("profile_path") val profilePath: String? = null,
)

@Serializable
data class TmdbCrewMember(val job: String = "", val name: String = "")

@Serializable
data class TmdbCredits(
    val cast: List<TmdbCastMember>? = null,
    val crew: List<TmdbCrewMember> = emptyList(),
)

@Serializable
data class TmdbMovie(
    val id: Int,
    val title: String = "",
    @SerialName("original_title") val originalTitle: String? = null,
    val overview: String = "",
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("release_date") val releaseDate: String = "",
    val runtime: Int? = null,
    val genres: List<TmdbGenre>? = null,
    val credits: TmdbCredits? = null,
)

@Serializable
data class TmdbEpisode(
    @SerialName("air_date") val airDate: String? = null,
    @SerialName("episode_number") val episodeNumber: Int,
    @SerialName("season_number") val seasonNumber: Int,
    val name: String = "",
    val overview: String? = null,
    val runtime: Int? = null,
    @SerialName("still_path") val stillPath: String? = null,
    @SerialName("vote_average") val voteAverage: Double? = null,
)

@Serializable
data class TmdbSeasonDetails(val episodes: List<TmdbEpisode> = emptyList())

@Serializable
data class TmdbSeasonSummary(
    @SerialName("season_number") val seasonNumber: Int,
    @SerialName("episode_count") val episodeCount: Int = 0,
    @SerialName("air_date") val airDate: String? = null,
)

@Serializable
data class TmdbCreator(val name: String = "")

@Serializable
data class TmdbSeries(
    val id: Int,
    val name: String = "",
    val overview: String = "",
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("first_air_date") val firstAirDate: String = "",
    @SerialName("number_of_seasons") val numberOfSeasons: Int? = null,
    val genres: List<TmdbGenre>? = null,
    @SerialName("created_by") val createdBy: List<TmdbCreator>? = null,
    @SerialName("next_episode_to_air") val nextEpisodeToAir: TmdbEpisode? = null,
    @SerialName("last_episode_to_air") val lastEpisodeToAir: TmdbEpisode? = null,
    val seasons: List<TmdbSeasonSummary>? = null,
    val credits: TmdbCredits? = null,
)

@Serializable
data class TmdbCredit(
    val id: Int,
    val title: String? = null,
    val name: String? = null,
    val character: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("media_type") val mediaType: String = "movie",
)

@Serializable
data class TmdbCombinedCredits(val cast: List<TmdbCredit>? = null)

@Serializable
data class TmdbPerson(
    val id: Int,
    val name: String = "",
    val biography: String = "",
    val birthday: String? = null,
    val deathday: String? = null,
    @SerialName("place_of_birth") val placeOfBirth: String? = null,
    @SerialName("profile_path") val profilePath: String? = null,
    @SerialName("combined_credits") val combinedCredits: TmdbCombinedCredits? = null,
)

@Serializable
data class WatchProvider(
    @SerialName("provider_id") val providerId: Int,
    @SerialName("provider_name") val providerName: String = "",
    @SerialName("logo_path") val logoPath: String = "",
)

@Serializable
data class WatchProvidersResult(
    val flatrate: List<WatchProvider> = emptyList(),
    val link: String? = null,
)

// ── IMDb (via the Vercel proxy) ───────────────────────────────────────────────

@Serializable
data class EpisodeRating(
    val episode: Int,
    val title: String = "",
    val imdbRating: Double? = null,
    val imdbId: String? = null,
)

@Serializable
data class MovieImdbRating(val imdbId: String, val rating: Double? = null)

/** Loading state of one IMDb season column, as in `imdbRatingStyle.ts`. */
sealed interface SeasonState {
    data object Loading : SeasonState
    data object Error : SeasonState
    data class Loaded(val episodes: List<EpisodeRating>) : SeasonState
}

// ── Push ──────────────────────────────────────────────────────────────────────

@Serializable
data class NotificationPreferences(
    @SerialName("notify_episodes") val notifyEpisodes: Boolean = true,
    @SerialName("notify_seasons") val notifySeasons: Boolean = true,
    @SerialName("notify_movies") val notifyMovies: Boolean = true,
)
