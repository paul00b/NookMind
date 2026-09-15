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
import fr.paulbr.nookmind.resources.Res
import fr.paulbr.nookmind.resources.legal_back
import fr.paulbr.nookmind.resources.legal_lastUpdated
import fr.paulbr.nookmind.resources.legal_privacyTitle
import fr.paulbr.nookmind.resources.legal_termsTitle
import org.jetbrains.compose.resources.stringResource

enum class LegalKind { PRIVACY, TERMS }

private data class LegalSection(val title: String, val paragraphs: List<String> = emptyList(), val bullets: List<String> = emptyList(), val after: String? = null)

// The web pages are French-only static content; kept identical here.
private val PRIVACY_SECTIONS = listOf(
    LegalSection(
        "1. Données collectées",
        paragraphs = listOf("NookMind collecte uniquement les données nécessaires au fonctionnement de l'application :"),
        bullets = listOf(
            "Adresse e-mail et identifiant de compte (via Google OAuth, Apple OAuth ou inscription directe)",
            "Données de bibliothèque : livres, films et séries ajoutés, notes personnelles, statuts de lecture/visionnage",
        ),
    ),
    LegalSection(
        "2. Utilisation des données",
        paragraphs = listOf("Vos données sont utilisées exclusivement pour :"),
        bullets = listOf("Vous authentifier et sécuriser votre compte", "Stocker et synchroniser votre bibliothèque personnelle"),
        after = "Nous ne vendons pas, ne partageons pas et ne monétisons pas vos données personnelles.",
    ),
    LegalSection(
        "3. Hébergement et sécurité",
        paragraphs = listOf("Les données sont stockées sur Supabase, un service sécurisé conforme aux standards RGPD. Les authentifications Google et Apple sont gérées via OAuth 2.0."),
    ),
    LegalSection(
        "4. Suppression des données",
        paragraphs = listOf("Vous pouvez demander la suppression de votre compte et de toutes vos données à tout moment en nous contactant."),
    ),
    LegalSection(
        "5. Contact",
        paragraphs = listOf("Pour toute question relative à vos données personnelles, contactez-nous à l'adresse associée au projet."),
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
    val sections = if (kind == LegalKind.PRIVACY) PRIVACY_SECTIONS else TERMS_SECTIONS
    Box(Modifier.fillMaxSize().background(colors.background).windowInsetsPadding(WindowInsets.safeDrawing), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.fillMaxWidth().widthIn(max = 672.dp).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 48.dp)) {
            Text(title, style = NookTheme.type.h1Serif, color = colors.textStrong)
            Spacer(Modifier.height(8.dp))
            Text(stringResource(Res.string.legal_lastUpdated), style = NookTheme.type.sm, color = colors.textSubtle)
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
