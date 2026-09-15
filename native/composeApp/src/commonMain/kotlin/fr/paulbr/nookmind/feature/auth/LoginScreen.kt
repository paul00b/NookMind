package fr.paulbr.nookmind.feature.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fr.paulbr.nookmind.app.AppContainer
import fr.paulbr.nookmind.core.designsystem.NookShapes
import fr.paulbr.nookmind.core.designsystem.NookTheme
import fr.paulbr.nookmind.core.designsystem.Palette
import fr.paulbr.nookmind.core.designsystem.components.BannerTone
import fr.paulbr.nookmind.core.designsystem.components.HairlineDivider
import fr.paulbr.nookmind.core.designsystem.components.InlineBanner
import fr.paulbr.nookmind.core.designsystem.components.LabeledField
import fr.paulbr.nookmind.core.designsystem.components.NookCard
import fr.paulbr.nookmind.core.designsystem.components.NookTextField
import fr.paulbr.nookmind.core.designsystem.components.OutlinedActionButton
import fr.paulbr.nookmind.core.designsystem.components.PasswordField
import fr.paulbr.nookmind.core.designsystem.components.PrimaryButton
import fr.paulbr.nookmind.core.designsystem.components.SegmentedPill
import fr.paulbr.nookmind.core.platform.isIos
import fr.paulbr.nookmind.resources.Res
import fr.paulbr.nookmind.resources.login_appleSignInFailed
import fr.paulbr.nookmind.resources.login_confirmEmail
import fr.paulbr.nookmind.resources.login_continueWithApple
import fr.paulbr.nookmind.resources.login_continueWithGoogle
import fr.paulbr.nookmind.resources.login_createAccount
import fr.paulbr.nookmind.resources.login_email
import fr.paulbr.nookmind.resources.login_emailPlaceholder
import fr.paulbr.nookmind.resources.login_logIn
import fr.paulbr.nookmind.resources.login_or
import fr.paulbr.nookmind.resources.login_password
import fr.paulbr.nookmind.resources.login_signUp
import fr.paulbr.nookmind.resources.login_tagline
import fr.paulbr.nookmind.resources.logo
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** The multicolour Google "G" of the login button (paths from Login.tsx). */
val GoogleLogo: ImageVector by lazy {
    ImageVector.Builder(name = "GoogleG", defaultWidth = 18.dp, defaultHeight = 18.dp, viewportWidth = 18f, viewportHeight = 18f).apply {
        addPath(addPathNodes("M17.64 9.2c0-.637-.057-1.251-.164-1.84H9v3.481h4.844c-.209 1.125-.843 2.078-1.796 2.717v2.258h2.908c1.702-1.567 2.684-3.875 2.684-6.615z"), fill = SolidColor(Color(0xFF4285F4)), pathFillType = PathFillType.NonZero)
        addPath(addPathNodes("M9 18c2.43 0 4.467-.806 5.956-2.184l-2.908-2.258c-.806.54-1.837.86-3.048.86-2.344 0-4.328-1.584-5.036-3.711H.957v2.332A8.997 8.997 0 0 0 9 18z"), fill = SolidColor(Color(0xFF34A853)), pathFillType = PathFillType.NonZero)
        addPath(addPathNodes("M3.964 10.707A5.41 5.41 0 0 1 3.682 9c0-.593.102-1.17.282-1.707V4.961H.957A8.996 8.996 0 0 0 0 9c0 1.452.348 2.827.957 4.039l3.007-2.332z"), fill = SolidColor(Color(0xFFFBBC05)), pathFillType = PathFillType.NonZero)
        addPath(addPathNodes("M9 3.58c1.321 0 2.508.454 3.44 1.345l2.582-2.58C13.463.891 11.426 0 9 0A8.997 8.997 0 0 0 .957 4.961L3.964 6.293C4.672 4.166 6.656 3.58 9 3.58z"), fill = SolidColor(Color(0xFFEA4335)), pathFillType = PathFillType.NonZero)
    }.build()
}

/** Port of Login.tsx. */
@Composable
fun LoginScreen(container: AppContainer) {
    val colors = NookTheme.colors
    val scope = rememberCoroutineScope()
    var mode by rememberSaveable { mutableStateOf("login") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var loading by mutableStateOfSaveable(false)
    var googleLoading by mutableStateOfSaveable(false)
    var appleLoading by mutableStateOfSaveable(false)
    var error by mutableStateOfSaveable("")
    var success by mutableStateOfSaveable("")
    val confirmEmailText = stringResource(Res.string.login_confirmEmail)
    val appleFailedText = stringResource(Res.string.login_appleSignInFailed)
    val showApple = isIos && container.auth.appleAvailable

    fun submit() {
        if (email.isBlank() || password.length < 6 || loading) return
        error = ""; success = ""; loading = true
        scope.launch {
            val result = if (mode == "login") container.auth.signIn(email.trim(), password) else container.auth.signUp(email.trim(), password)
            result.onFailure { error = it.message ?: "" }
            if (mode == "signup" && result.isSuccess) success = confirmEmailText
            loading = false
        }
    }

    Box(Modifier.fillMaxSize().background(colors.background).imePadding(), contentAlignment = Alignment.Center) {
        Column(
            Modifier.fillMaxWidth().widthIn(max = 384.dp).verticalScroll(rememberScrollState()).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(painterResource(Res.drawable.logo), contentDescription = "NookMind", modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(12.dp))
            Text("NookMind", style = NookTheme.type.h1Serif, color = colors.textStrong)
            Spacer(Modifier.height(12.dp))
            Text(stringResource(Res.string.login_tagline), style = NookTheme.type.sm, color = colors.textSubtle, textAlign = TextAlign.Center)
            Spacer(Modifier.height(32.dp))

            NookCard(Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(24.dp)) {
                SegmentedPill(
                    options = listOf("login" to stringResource(Res.string.login_logIn), "signup" to stringResource(Res.string.login_signUp)),
                    selected = mode,
                    onSelect = { mode = it; error = ""; success = "" },
                )
                Spacer(Modifier.height(20.dp))

                LabeledField(stringResource(Res.string.login_email)) {
                    NookTextField(
                        value = email, onValueChange = { email = it },
                        placeholder = stringResource(Res.string.login_emailPlaceholder),
                        keyboardType = KeyboardType.Email, imeAction = ImeAction.Next,
                    )
                }
                Spacer(Modifier.height(12.dp))
                LabeledField(stringResource(Res.string.login_password)) {
                    PasswordField(
                        value = password, onValueChange = { password = it },
                        keyboardActions = KeyboardActions(onDone = { submit() }),
                    )
                }

                if (error.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    InlineBanner(error, tone = BannerTone.ERROR)
                }
                if (success.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    InlineBanner(success, tone = BannerTone.SUCCESS)
                }

                Spacer(Modifier.height(20.dp))
                PrimaryButton(
                    text = if (mode == "login") stringResource(Res.string.login_logIn) else stringResource(Res.string.login_createAccount),
                    onClick = ::submit,
                    modifier = Modifier.fillMaxWidth(),
                    loading = loading,
                    enabled = email.isNotBlank() && password.length >= 6,
                )

                Spacer(Modifier.height(20.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HairlineDivider(Modifier.weight(1f), color = colors.borderNeutral)
                    Text(stringResource(Res.string.login_or), style = NookTheme.type.xs, color = colors.textFaint)
                    HairlineDivider(Modifier.weight(1f), color = colors.borderNeutral)
                }
                Spacer(Modifier.height(20.dp))

                OutlinedActionButton(
                    text = stringResource(Res.string.login_continueWithGoogle),
                    onClick = {
                        error = ""; googleLoading = true
                        scope.launch {
                            container.auth.signInWithGoogle().onFailure { error = it.message ?: "" }
                            googleLoading = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    loading = googleLoading,
                    background = colors.surface,
                    leading = { Image(GoogleLogo, contentDescription = null, modifier = Modifier.size(18.dp)) },
                )

                if (showApple) {
                    Spacer(Modifier.height(12.dp))
                    OutlinedActionButton(
                        text = stringResource(Res.string.login_continueWithApple),
                        onClick = {
                            error = ""; appleLoading = true
                            scope.launch {
                                container.auth.signInWithApple().onFailure { error = it.message?.takeIf { m -> m.isNotBlank() } ?: appleFailedText }
                                appleLoading = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        loading = appleLoading,
                        background = Palette.Gray950,
                        contentColor = Palette.White,
                        borderColor = Palette.Gray900,
                        leading = { Text("", style = NookTheme.type.sans(16, FontWeight.SemiBold), color = Palette.White) },
                    )
                }
            }
        }
    }
}

@Composable
private fun <T : Any> mutableStateOfSaveable(initial: T) = rememberSaveable { mutableStateOf(initial) }
