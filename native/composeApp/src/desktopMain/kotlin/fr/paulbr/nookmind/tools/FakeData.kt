package fr.paulbr.nookmind.tools

import fr.paulbr.nookmind.core.model.Book
import fr.paulbr.nookmind.core.model.BookCategory
import fr.paulbr.nookmind.core.model.BookStatus
import fr.paulbr.nookmind.core.model.GoogleBookVolume
import fr.paulbr.nookmind.core.model.GoogleImageLinks
import fr.paulbr.nookmind.core.model.GoogleVolumeInfo
import fr.paulbr.nookmind.core.model.Movie
import fr.paulbr.nookmind.core.model.MovieCategory
import fr.paulbr.nookmind.core.model.MovieStatus
import fr.paulbr.nookmind.core.model.Series
import fr.paulbr.nookmind.core.model.SeriesCategory
import fr.paulbr.nookmind.core.model.SeriesStatus

/** Sample library used by the screenshot tool (no network: covers stay as placeholders). */
object FakeData {
    private const val USER = "user-1"

    val books: List<Book> = listOf(
        Book("b1", USER, "g1", "Dune", "Frank Herbert", "Set on the desert planet Arrakis, Dune is the story of the boy Paul Atreides, heir to a noble family tasked with ruling an inhospitable world where the only thing of value is the spice melange.", null, "1965-08-01", 412, "Science Fiction", BookStatus.READING, null, "Reread for the film.", 180, "2026-09-10T10:00:00+00:00"),
        Book("b2", USER, "g2", "Le Comte de Monte-Cristo", "Alexandre Dumas", "Edmond Dantès est injustement emprisonné au château d'If.", null, "1844-01-01", 1276, "Classique", BookStatus.WANT_TO_READ, null, null, null, "2026-09-09T10:00:00+00:00"),
        Book("b3", USER, "g3", "The Left Hand of Darkness", "Ursula K. Le Guin", null, null, "1969-03-01", 304, "Science Fiction", BookStatus.WANT_TO_READ, null, null, null, "2026-09-08T10:00:00+00:00"),
        Book("b4", USER, "g4", "Piranesi", "Susanna Clarke", null, null, "2020-09-15", 272, "Fantasy", BookStatus.READ, 4.5, "Strange and beautiful.", null, "2026-09-07T10:00:00+00:00"),
        Book("b5", USER, "g5", "L'Anomalie", "Hervé Le Tellier", null, null, "2020-08-20", 336, "Roman", BookStatus.READ, 3.5, null, null, "2026-09-06T10:00:00+00:00"),
        Book("b6", USER, "g6", "Project Hail Mary", "Andy Weir", null, null, "2021-05-04", 476, "Science Fiction", BookStatus.READ, 5.0, null, null, "2026-09-05T10:00:00+00:00"),
        Book("b7", USER, "g7", "Kafka sur le rivage", "Haruki Murakami", null, null, "2002-09-12", 640, "Roman", BookStatus.WANT_TO_READ, null, null, null, "2026-09-04T10:00:00+00:00"),
    )

    val bookCategories: List<BookCategory> = listOf(
        BookCategory("c1", USER, "Été 2026", "2026-06-01T10:00:00+00:00", listOf("b1", "b4")),
        BookCategory("c2", USER, "Classiques", "2026-06-02T10:00:00+00:00", emptyList()),
    )

    val bookSearchResults: List<GoogleBookVolume> = listOf(
        GoogleBookVolume("v1", GoogleVolumeInfo(title = "Dune", authors = listOf("Frank Herbert"), publishedDate = "1965", imageLinks = GoogleImageLinks(smallThumbnail = null))),
        GoogleBookVolume("v2", GoogleVolumeInfo(title = "Dune Messiah", authors = listOf("Frank Herbert"), publishedDate = "1969")),
        GoogleBookVolume("v3", GoogleVolumeInfo(title = "Children of Dune", authors = listOf("Frank Herbert"), publishedDate = "1976")),
        GoogleBookVolume("v4", GoogleVolumeInfo(title = "The Dune Encyclopedia", authors = listOf("Willis E. McNelly"), publishedDate = "1984")),
    )

    val movies: List<Movie> = listOf(
        Movie("m1", USER, 438631, "Dune", "Denis Villeneuve", "Paul Atreides, a brilliant and gifted young man born into a great destiny beyond his understanding, must travel to the most dangerous planet in the universe.", null, "2021-09-15", 155, "Science Fiction", MovieStatus.WATCHED, "2026-08-30", 4.5, null, "2026-09-10T10:00:00+00:00"),
        Movie("m2", USER, 693134, "Dune: Part Two", "Denis Villeneuve", null, null, "2024-02-27", 167, "Science Fiction", MovieStatus.WATCHED, "2025-03-02", 5.0, "Even better on IMAX.", "2026-09-09T10:00:00+00:00"),
        Movie("m3", USER, 27205, "Inception", "Christopher Nolan", null, null, "2010-07-15", 148, "Action", MovieStatus.WANT_TO_WATCH, null, null, null, "2026-09-08T10:00:00+00:00"),
        Movie("m4", USER, 1124, "The Prestige", "Christopher Nolan", null, null, "2006-10-19", 130, "Drama", MovieStatus.WANT_TO_WATCH, null, null, null, "2026-09-07T10:00:00+00:00"),
        Movie("m5", USER, 496243, "Parasite", "Bong Joon-ho", null, null, "2019-05-30", 133, "Thriller", MovieStatus.WATCHED, "2024-11-11", 4.0, null, "2026-09-06T10:00:00+00:00"),
    )

    val movieCategories: List<MovieCategory> = listOf(
        MovieCategory("mc1", USER, "Nolan", "2026-06-01T10:00:00+00:00", listOf("m3", "m4")),
    )

    val series: List<Series> = listOf(
        Series("s1", USER, 1396, "Breaking Bad", "Vince Gilligan", "A chemistry teacher diagnosed with cancer turns to manufacturing methamphetamine.", null, "2008-01-20", 5, listOf(1, 2), mapOf("3" to listOf(1, 2, 3)), "Drama", SeriesStatus.WATCHING, null, null, null, null, null, "2026-09-10T10:00:00+00:00"),
        Series("s2", USER, 94997, "House of the Dragon", "Ryan Condal", null, null, "2022-08-21", 2, listOf(1, 2), emptyMap(), "Fantasy", SeriesStatus.WATCHING, null, null, "2026-12-15", 3, 1, "2026-09-09T10:00:00+00:00"),
        Series("s3", USER, 66732, "Stranger Things", "The Duffer Brothers", null, null, "2016-07-15", 5, listOf(1, 2, 3, 4), emptyMap(), "Science Fiction", SeriesStatus.WATCHING, null, null, "2026-10-01", 5, 1, "2026-09-08T10:00:00+00:00"),
        Series("s4", USER, 1399, "Game of Thrones", "David Benioff", null, null, "2011-04-17", 8, (1..8).toList(), emptyMap(), "Fantasy", SeriesStatus.WATCHED, 4.0, null, null, null, null, "2026-09-07T10:00:00+00:00"),
        Series("s5", USER, 60625, "Rick and Morty", "Dan Harmon", null, null, "2013-12-02", 7, emptyList(), emptyMap(), "Animation", SeriesStatus.WANT_TO_WATCH, null, null, null, null, null, "2026-09-06T10:00:00+00:00"),
    )

    val seriesCategories: List<SeriesCategory> = listOf(
        SeriesCategory("sc1", USER, "HBO", "2026-06-01T10:00:00+00:00", listOf("s2", "s4")),
    )
}
