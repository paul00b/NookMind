package fr.paulbr.nookmind.feature.social

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.paulbr.nookmind.core.data.SocialProfile
import fr.paulbr.nookmind.core.designsystem.NookShapes
import fr.paulbr.nookmind.core.designsystem.NookTheme
import fr.paulbr.nookmind.core.designsystem.Palette
import fr.paulbr.nookmind.core.designsystem.components.HairlineDivider
import fr.paulbr.nookmind.core.designsystem.components.IconGhostButton
import fr.paulbr.nookmind.core.designsystem.components.NookSheet
import fr.paulbr.nookmind.core.designsystem.components.NookTextField
import fr.paulbr.nookmind.core.designsystem.components.PrimaryButton
import fr.paulbr.nookmind.core.designsystem.components.SheetCloseButton
import fr.paulbr.nookmind.core.designsystem.components.Spinner
import fr.paulbr.nookmind.core.designsystem.icons.LucideIcons
import fr.paulbr.nookmind.resources.Res
import fr.paulbr.nookmind.resources.logo
import fr.paulbr.nookmind.resources.social_add
import fr.paulbr.nookmind.resources.social_addFriends
import fr.paulbr.nookmind.resources.social_addWithCode
import fr.paulbr.nookmind.resources.social_codePlaceholder
import fr.paulbr.nookmind.resources.social_copy
import fr.paulbr.nookmind.resources.social_inviteHint
import fr.paulbr.nookmind.resources.social_or
import fr.paulbr.nookmind.resources.social_qrDescription
import fr.paulbr.nookmind.resources.social_shareLink
import io.github.alexzhirkevich.qrose.options.*
import io.github.alexzhirkevich.qrose.rememberQrCodePainter
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * "Add friends", from the button next to the Social title: my invitation as a QR code to scan
 * (the link opens NookMind on this same sheet, code filled in) and as a link to send, then a code
 * to type for the other way round. [initialCode] comes from such a link.
 */
@Composable
internal fun FriendInviteSheet(
    profile: SocialProfile?,
    link: String?,
    initialCode: String,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onAdd: suspend (String) -> Boolean,
    onClose: () -> Unit,
) {
    val colors = NookTheme.colors
    val accent = socialAccent()
    var draft by remember(initialCode) { mutableStateOf(initialCode) }
    var sending by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    NookSheet(onClose = onClose, maxWidth = 480.dp) { controller ->
        Box(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(Res.string.social_addFriends), style = NookTheme.type.h3Serif, color = colors.textStrong, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(4.dp))
                Text(stringResource(Res.string.social_inviteHint), style = NookTheme.type.sm, color = colors.textSubtle, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(20.dp))

                // Dark modules on white in both themes: a scanner needs the contrast more than the
                // card needs to match the night background.
                Box(
                    Modifier.size(224.dp).clip(NookShapes.xl3).background(Color.White).padding(18.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    val content = link ?: profile?.friendCode
                    if (content == null) {
                        Spinner(size = 24.dp, color = accent)
                    } else {
                        InvitationQr(content, ink = Palette.Purple800)
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        profile?.friendCode ?: "······",
                        style = NookTheme.type.sans(26, FontWeight.Bold, 32).copy(letterSpacing = 5.sp),
                        color = colors.textStrong,
                    )
                    Spacer(Modifier.width(6.dp))
                    IconGhostButton(LucideIcons.Copy, stringResource(Res.string.social_copy), onCopy, enabled = profile != null, size = 18.dp)
                }
                Spacer(Modifier.height(14.dp))
                PrimaryButton(
                    stringResource(Res.string.social_shareLink),
                    onClick = onShare,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = profile != null,
                    icon = LucideIcons.Share,
                    color = Palette.Purple500,
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                )

                Spacer(Modifier.height(24.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    HairlineDivider(Modifier.weight(1f))
                    Text(stringResource(Res.string.social_or), style = NookTheme.type.xs, color = colors.textFaint, modifier = Modifier.padding(horizontal = 12.dp))
                    HairlineDivider(Modifier.weight(1f))
                }
                Spacer(Modifier.height(18.dp))

                Text(stringResource(Res.string.social_addWithCode), style = NookTheme.type.sans(14, FontWeight.SemiBold, 20), color = colors.textStrong, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NookTextField(
                        value = draft,
                        onValueChange = { value -> draft = value.uppercase().filter { it.isLetterOrDigit() }.take(6) },
                        placeholder = stringResource(Res.string.social_codePlaceholder),
                        modifier = Modifier.weight(1f),
                        textStyle = NookTheme.type.sans(16, FontWeight.SemiBold, 24).copy(letterSpacing = 2.sp),
                    )
                    PrimaryButton(
                        stringResource(Res.string.social_add),
                        onClick = {
                            sending = true
                            scope.launch {
                                if (onAdd(draft)) {
                                    draft = ""
                                    controller.close()
                                }
                                sending = false
                            }
                        },
                        enabled = draft.length == 6,
                        loading = sending,
                        icon = LucideIcons.UserPlus,
                        color = Palette.Purple500,
                    )
                }
            }
            SheetCloseButton(controller, Modifier.align(Alignment.TopEnd).padding(top = 0.dp, end = 8.dp), size = 18.dp)
        }
    }
}

/** The invitation as a QR code: rounded modules in the Social purple, the NookMind books in the middle. */
@Composable
private fun InvitationQr(content: String, ink: Color) {
    val logo = painterResource(Res.drawable.logo)
    val painter = rememberQrCodePainter(content) {
        logo {
            painter = logo
            padding = QrLogoPadding.Natural(.12f)
            shape = QrLogoShape.circle()
            size = 0.22f
        }
        shapes {
            ball = QrBallShape.roundCorners(.3f)
            darkPixel = QrPixelShape.roundCorners()
            frame = QrFrameShape.roundCorners(.3f)
        }
        colors {
            dark = QrBrush.solid(ink)
            frame = QrBrush.solid(ink)
            ball = QrBrush.solid(ink)
        }
    }
    Image(painter, stringResource(Res.string.social_qrDescription), Modifier.fillMaxWidth())
}
