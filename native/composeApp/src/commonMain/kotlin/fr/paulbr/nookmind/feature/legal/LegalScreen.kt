package fr.paulbr.nookmind.feature.legal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fr.paulbr.nookmind.core.designsystem.NookTheme
import fr.paulbr.nookmind.core.platform.isFrenchDevice
import fr.paulbr.nookmind.resources.Res
import fr.paulbr.nookmind.resources.legal_back
import fr.paulbr.nookmind.resources.legal_lastUpdated
import fr.paulbr.nookmind.resources.legal_privacyLastUpdated
import fr.paulbr.nookmind.resources.legal_privacyTitle
import fr.paulbr.nookmind.resources.legal_termsTitle
import org.jetbrains.compose.resources.stringResource

enum class LegalKind { PRIVACY, TERMS }

private data class LegalSection(val title: String, val paragraphs: List<String> = emptyList(), val bullets: List<String> = emptyList(), val after: String? = null)

// The same text as the privacy page of the website (src/pages/Privacy.tsx), which store listings
// link to: keep the two in step.
private const val CONTACT_EMAIL = "broussolle.paul@gmail.com"

private val PRIVACY_SECTIONS_FR = listOf(
    LegalSection(
        "1. Données collectées",
        paragraphs = listOf("NookMind collecte uniquement les données nécessaires au fonctionnement de l'application :"),
        bullets = listOf(
            "Adresse e-mail et identifiant de compte (via Google, Apple ou inscription par e-mail)",
            "Nom d'affichage : celui fourni par Google ou Apple à la connexion, ou celui que vous choisissez dans les paramètres",
            "Données de bibliothèque : livres, films et séries ajoutés, collections, notes personnelles, évaluations, avancement de lecture ou de visionnage",
            "Données sociales, si vous les utilisez : votre code ami, vos amis, les réactions que vous envoyez et vos parties « Ce soir ? » à distance (films proposés et votes)",
            "Jeton de notification (Firebase Cloud Messaging), uniquement si vous activez les notifications",
        ),
        after = "Sur le site web, nous mesurons la fréquentation avec Vercel Analytics, sans cookie et sans identifier les visiteurs. L'application mobile ne contient ni publicité ni outil de suivi.",
    ),
    LegalSection(
        "2. Utilisation des données",
        paragraphs = listOf("Vos données sont utilisées exclusivement pour :"),
        bullets = listOf(
            "Vous authentifier et sécuriser votre compte",
            "Stocker et synchroniser votre bibliothèque personnelle entre vos appareils",
            "Faire fonctionner les fonctions sociales que vous utilisez : amis, réactions, « Ce soir ? » à distance",
            "Vous envoyer les notifications que vous avez activées (nouvelles saisons, sorties de films, demandes d'amis et invitations)",
        ),
        after = "Nous ne vendons pas et ne monétisons pas vos données personnelles. Nous ne les partageons avec personne, en dehors des amis que vous ajoutez (section 3) et des services de la section 4.",
    ),
    LegalSection(
        "3. Ce que voient vos amis",
        paragraphs = listOf("Les fonctions sociales sont facultatives. Un ami s'ajoute avec son code ami, et il doit accepter la demande. Une fois que vous êtes amis :"),
        bullets = listOf(
            "Il voit votre nom d'affichage et les films, séries et livres de votre bibliothèque, avec leur statut. Dans l'onglet Social, vous choisissez ce que vous partagez : films, séries, livres, évaluations",
            "Vos notes personnelles ne sont jamais partagées",
            "Il voit les réactions que vous laissez sur ses titres",
            "Pendant une partie « Ce soir ? » à distance, il voit les films proposés, puis vos votes à la fin de la partie",
        ),
        after = "Personne d'autre que vos amis n'a accès à ces informations, et vous pouvez retirer un ami à tout moment. Quand vous partagez un titre par lien, ce lien contient votre prénom, pour que la personne qui le reçoit sache qui le lui recommande.",
    ),
    LegalSection(
        "4. Hébergement et services tiers",
        paragraphs = listOf("NookMind s'appuie sur les services suivants, qui traitent les données pour notre compte :"),
        bullets = listOf(
            "Supabase : hébergement de la base de données et authentification",
            "Vercel : hébergement du site et des fonctions serveur",
            "Google Firebase Cloud Messaging : envoi des notifications",
            "Google et Apple : connexion à votre compte (OAuth 2.0), si vous choisissez ce mode",
        ),
        after = "Pour afficher les fiches des livres, films et séries, l'application interroge TMDB, Google Books et IMDb. Ces services reçoivent vos termes de recherche et les titres consultés, mais jamais votre adresse e-mail ni votre identifiant de compte.",
    ),
    LegalSection(
        "5. Conservation",
        paragraphs = listOf("Vos données sont conservées tant que votre compte existe. Elles sont supprimées définitivement lorsque vous supprimez votre compte."),
    ),
    LegalSection(
        "6. Supprimer votre compte",
        paragraphs = listOf(
            "Depuis l'application : ouvrez les Paramètres, touchez « Supprimer le compte », puis confirmez. La suppression est immédiate.",
            "Sans l'application : écrivez à $CONTACT_EMAIL depuis l'adresse e-mail de votre compte, avec pour objet « Suppression de compte NookMind ». Nous supprimons le compte sous 30 jours au plus et vous le confirmons par e-mail.",
        ),
        after = "Dans les deux cas, sont supprimés : votre compte, vos livres, films, séries, collections, notes, évaluations, votre profil, vos amis, vos réactions, vos parties « Ce soir ? » et vos abonnements aux notifications. Aucune donnée n'est conservée après la suppression.",
    ),
    LegalSection(
        "7. Vos droits",
        paragraphs = listOf("Conformément au RGPD, vous pouvez accéder à vos données, les rectifier, en demander la portabilité ou l'effacement, en écrivant à $CONTACT_EMAIL. Vous pouvez aussi adresser une réclamation à la CNIL (cnil.fr)."),
    ),
    LegalSection(
        "8. Contact",
        paragraphs = listOf("Pour toute question relative à vos données personnelles : $CONTACT_EMAIL."),
    ),
)

private val PRIVACY_SECTIONS_EN = listOf(
    LegalSection(
        "1. Data we collect",
        paragraphs = listOf("NookMind only collects the data it needs to work:"),
        bullets = listOf(
            "Email address and account ID (via Google, Apple or email sign-up)",
            "Display name: the one provided by Google or Apple when you sign in, or the one you choose in the settings",
            "Library data: books, movies and series you add, collections, personal notes, ratings, reading or watching progress",
            "Social data, if you use it: your friend code, your friends, the reactions you send and your remote “Tonight?” games (suggested movies and votes)",
            "Notification token (Firebase Cloud Messaging), only if you turn notifications on",
        ),
        after = "On the website, we measure traffic with Vercel Analytics, without cookies and without identifying visitors. The mobile app contains no ads and no tracking tools.",
    ),
    LegalSection(
        "2. How we use it",
        paragraphs = listOf("Your data is used only to:"),
        bullets = listOf(
            "Authenticate you and keep your account secure",
            "Store and sync your personal library across your devices",
            "Run the social features you use: friends, reactions, remote “Tonight?” games",
            "Send you the notifications you turned on (new seasons, movie releases, friend requests and invitations)",
        ),
        after = "We do not sell or monetize your personal data. We share it with no one, apart from the friends you add (section 3) and the services in section 4.",
    ),
    LegalSection(
        "3. What your friends see",
        paragraphs = listOf("The social features are optional. You add a friend with their friend code, and they have to accept the request. Once you are friends:"),
        bullets = listOf(
            "They see your display name and the movies, series and books in your library, with their status. In the Social tab, you choose what you share: movies, series, books, ratings",
            "Your personal notes are never shared",
            "They see the reactions you leave on their titles",
            "During a remote “Tonight?” game, they see the suggested movies, then your votes at the end of the game",
        ),
        after = "No one but your friends can see this, and you can remove a friend at any time. When you share a title by link, the link includes your first name, so the person who gets it knows who recommends it.",
    ),
    LegalSection(
        "4. Hosting and third-party services",
        paragraphs = listOf("NookMind relies on the following services, which process data on our behalf:"),
        bullets = listOf(
            "Supabase: database hosting and authentication",
            "Vercel: website and server functions hosting",
            "Google Firebase Cloud Messaging: notification delivery",
            "Google and Apple: signing in to your account (OAuth 2.0), if you choose to",
        ),
        after = "To show book, movie and series details, the app queries TMDB, Google Books and IMDb. These services receive your search terms and the titles you view, but never your email address or account ID.",
    ),
    LegalSection(
        "5. Retention",
        paragraphs = listOf("Your data is kept for as long as your account exists. It is permanently deleted when you delete your account."),
    ),
    LegalSection(
        "6. Delete your account",
        paragraphs = listOf(
            "From the app: open Settings, tap “Delete account”, then confirm. Deletion is immediate.",
            "Without the app: email $CONTACT_EMAIL from your account's email address, with the subject “NookMind account deletion”. We delete the account within 30 days at most and confirm by email.",
        ),
        after = "Either way, we delete your account, books, movies, series, collections, notes, ratings, profile, friends, reactions, “Tonight?” games and notification subscriptions. No data is kept after deletion.",
    ),
    LegalSection(
        "7. Your rights",
        paragraphs = listOf("Under the GDPR, you can access, correct, export or erase your data by emailing $CONTACT_EMAIL. You can also lodge a complaint with the CNIL (cnil.fr), the French data protection authority."),
    ),
    LegalSection(
        "8. Contact",
        paragraphs = listOf("For any question about your personal data: $CONTACT_EMAIL."),
    ),
)

private val TERMS_SECTIONS = listOf(
    LegalSection("1. Acceptation des conditions", paragraphs = listOf("En utilisant NookMind, vous acceptez les présentes conditions d'utilisation. Si vous n'acceptez pas ces conditions, veuillez ne pas utiliser l'application.")),
    LegalSection("2. Description du service", paragraphs = listOf("NookMind est une application personnelle de gestion de bibliothèque permettant de suivre des livres, films et séries. Le service est fourni tel quel, sans garantie de disponibilité continue.")),
    LegalSection(
        "3. Utilisation acceptable",
        paragraphs = listOf("Vous vous engagez à utiliser NookMind uniquement à des fins personnelles et légales. Il est interdit :"),
        bullets = listOf("D'utiliser le service à des fins commerciales", "De tenter d'accéder aux données d'autres utilisateurs", "D'automatiser des requêtes de manière abusive"),
    ),
    LegalSection("4. Compte utilisateur", paragraphs = listOf("Vous êtes responsable de la sécurité de votre compte. En cas d'utilisation non autorisée de votre compte, contactez-nous immédiatement.")),
    LegalSection("5. Limitation de responsabilité", paragraphs = listOf("NookMind ne peut être tenu responsable de la perte de données ou de toute interruption de service. Nous recommandons de ne pas stocker d'informations critiques uniquement dans l'application.")),
    LegalSection("6. Modifications", paragraphs = listOf("Nous nous réservons le droit de modifier ces conditions à tout moment. Les modifications seront notifiées via l'application.")),
)

/** Port of Privacy.tsx / Terms.tsx. */
@Composable
fun LegalScreen(kind: LegalKind, onBack: () -> Unit) {
    val colors = NookTheme.colors
    BackHandler(enabled = true, onBack = onBack)
    val title = if (kind == LegalKind.PRIVACY) stringResource(Res.string.legal_privacyTitle) else stringResource(Res.string.legal_termsTitle)
    val sections = when {
        kind == LegalKind.TERMS -> TERMS_SECTIONS
        isFrenchDevice() -> PRIVACY_SECTIONS_FR
        else -> PRIVACY_SECTIONS_EN
    }
    Box(Modifier.fillMaxSize().background(colors.background).windowInsetsPadding(WindowInsets.safeDrawing), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.fillMaxWidth().widthIn(max = 672.dp).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 48.dp)) {
            Text(title, style = NookTheme.type.h1Serif, color = colors.textStrong)
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(if (kind == LegalKind.PRIVACY) Res.string.legal_privacyLastUpdated else Res.string.legal_lastUpdated),
                style = NookTheme.type.sm, color = colors.textSubtle,
            )
            Spacer(Modifier.height(32.dp))
            sections.forEach { section ->
                Text(section.title, style = NookTheme.type.sans(14, FontWeight.SemiBold, 20), color = colors.textStrong)
                Spacer(Modifier.height(8.dp))
                section.paragraphs.forEach { Text(it, style = NookTheme.type.sans(14, lineHeight = 22), color = colors.textBody2) }
                if (section.bullets.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    section.bullets.forEach { bullet ->
                        Row(Modifier.padding(start = 8.dp, bottom = 4.dp)) {
                            Text("•  ", style = NookTheme.type.sans(14, lineHeight = 22), color = colors.textBody2)
                            Text(bullet, style = NookTheme.type.sans(14, lineHeight = 22), color = colors.textBody2)
                        }
                    }
                }
                section.after?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, style = NookTheme.type.sans(14, lineHeight = 22), color = colors.textBody2)
                }
                Spacer(Modifier.height(24.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(Res.string.legal_back),
                style = NookTheme.type.sm,
                color = colors.amberText,
                modifier = Modifier.clickable(onClick = onBack).padding(vertical = 4.dp),
            )
        }
    }
}
