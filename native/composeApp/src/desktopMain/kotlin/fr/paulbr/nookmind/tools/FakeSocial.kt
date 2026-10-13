package fr.paulbr.nookmind.tools

import fr.paulbr.nookmind.core.data.Friend
import fr.paulbr.nookmind.core.data.FriendItem
import fr.paulbr.nookmind.core.data.ReceivedReaction
import fr.paulbr.nookmind.core.data.SentReaction
import fr.paulbr.nookmind.core.data.SocialProfile
import fr.paulbr.nookmind.core.data.TonightSessionRow
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/** Two friends, a request each way and what they share, for the Social screenshots. */
object FakeSocial {
    val profile = SocialProfile(id = "user-1", displayName = "Paul Broussolle", friendCode = "K7Q2MX")

    val friends = listOf(
        Friend("f1", "u-lea", "Léa Martin", null, "accepted", incoming = false),
        Friend("f2", "u-tom", "Tom Bernard", null, "accepted", incoming = true),
        Friend("f3", "u-ines", "Inès Roux", null, "pending", incoming = true),
        Friend("f4", "u-sam", "Sam Petit", null, "pending", incoming = false),
    )

    private fun ago(duration: Duration): String = (Clock.System.now() - duration).toString()

    /** Built at render time, once [FakeCovers] gave the library its artwork. */
    fun library(): List<FriendItem> = listOf(
        FriendItem("u-lea", "movie", "693134", "Dune: Part Two", "Denis Villeneuve", FakeData.movies[1].posterUrl, "watched", 5.0, ago(3.hours)),
        FriendItem("u-tom", "series", "94997", "House of the Dragon", "Ryan Condal", FakeData.series[1].posterUrl, "watching", null, ago(20.hours)),
        FriendItem("u-lea", "book", "g6", "Project Hail Mary", "Andy Weir", FakeData.books[5].coverUrl, "reading", null, ago(30.hours)),
        FriendItem("u-lea", "series", "66732", "Stranger Things", "The Duffer Brothers", FakeData.series[2].posterUrl, "watching", null, ago(40.hours)),
        FriendItem("u-tom", "movie", "157336", "Interstellar", "Christopher Nolan", FakeData.newMovie.posterUrl, "want_to_watch", null, ago(26.hours)),
        FriendItem("u-lea", "series", "95396", "Severance", "Dan Erickson", FakeData.newSeries.posterUrl, "watched", 4.5, ago(50.hours)),
        FriendItem("u-tom", "book", "g9", "La Croisée des chemins", "Franck Thilliez", FakeData.newBook.coverUrl, "read", 4.0, ago(80.hours)),
        FriendItem("u-tom", "movie", "438631", "Dune", "Denis Villeneuve", FakeData.movies[0].posterUrl, "watched", 4.0, ago(200.hours)),
        FriendItem("u-lea", "series", "1396", "Breaking Bad", "Vince Gilligan", FakeData.series[0].posterUrl, "watched", 5.0, ago(300.hours)),
        FriendItem("u-lea", "book", "g1", "Dune", "Frank Herbert", FakeData.books[0].coverUrl, "read", 4.5, ago(400.hours)),
        FriendItem("u-tom", "book", "g1", "Dune", "Frank Herbert", FakeData.books[0].coverUrl, "want_to_read", null, ago(500.hours)),
        FriendItem("u-lea", "movie", "496243", "Parasite", "Bong Joon-ho", FakeData.movies[4].posterUrl, "watched", 5.0, ago(140.hours)),
        FriendItem("u-lea", "book", "g4", "Piranesi", "Susanna Clarke", FakeData.books[3].coverUrl, "read", 4.5, ago(220.hours)),
        FriendItem("u-lea", "series", "1399", "Game of Thrones", "David Benioff", FakeData.series[3].posterUrl, "watched", 3.5, ago(600.hours)),
    )

    fun received(): List<ReceivedReaction> = listOf(
        ReceivedReaction("u-tom", "Tom Bernard", "movie", "438631", "Dune", "culte", ago(2.hours)),
        ReceivedReaction("u-lea", "Léa Martin", "book", "g4", "Piranesi", "merci", ago(28.hours)),
    )

    val sent = listOf(SentReaction("u-lea", "movie", "693134", "culte"))

    fun invitations(): List<TonightSessionRow> = listOf(
        TonightSessionRow(id = "s-1", host = "u-lea", guest = "user-1", createdAt = ago(4.minutes)),
    )
}
