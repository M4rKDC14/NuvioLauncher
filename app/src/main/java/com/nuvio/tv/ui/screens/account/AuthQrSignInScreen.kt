@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.nuvio.tv.ui.screens.account

import androidx.compose.ui.focus.onFocusChanged
import com.nuvio.tv.ui.theme.NuvioTheme

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.nuvio.tv.BuildConfig
import com.nuvio.tv.R
import com.nuvio.tv.domain.model.AuthState
import com.nuvio.tv.ui.components.BrandWordmark
import com.nuvio.tv.ui.components.SkeletonBar
import com.nuvio.tv.ui.components.rememberShimmerBrush
import com.nuvio.tv.ui.screens.detail.requestFocusAfterFrames
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay

private val AuthTextPrimary = Color(0xFFF5F7F8)
private val AuthTextSecondary = Color(0xFF969CA3)
private val AuthPaneBackground = Color.White.copy(alpha = 0.022f)
private val AuthPaneBorder = Color.White.copy(alpha = 0.07f)
private val AuthSecondaryButtonBackground = Color.White.copy(alpha = 0.05f)
private val AuthSecondaryButtonBorder = Color.White.copy(alpha = 0.09f)

@Composable
fun AuthQrSignInScreen(
    onBackPress: () -> Unit = {},
    onContinue: (() -> Unit)? = null,
    viewModel: AccountViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val fullAccount = uiState.authState as? AuthState.FullAccount
    val isSignedIn = fullAccount != null
    val isOnboardingMode = onContinue != null
    val useEmailLogin = viewModel.usesEmailPasswordLogin
    val useQrLogin = !useEmailLogin
    val isApproved = remember(uiState.qrLoginStatus) {
        uiState.qrLoginStatus?.contains("approved", ignoreCase = true) == true
    }
    var onboardingTransitionHandled by remember(isOnboardingMode) { mutableStateOf(false) }
    var exitRequested by remember { mutableStateOf(false) }
    var showSignOutConfirmation by remember { mutableStateOf(false) }
    val loginFocusRequester = remember { FocusRequester() }

    fun leaveAuthScreen() {
        exitRequested = true
        viewModel.clearQrLoginSession()
        onBackPress()
    }

    fun continueFromAuthScreen() {
        exitRequested = true
        if (onContinue != null && !isSignedIn) {
            viewModel.signOut()
        }
        viewModel.clearQrLoginSession()
        if (onContinue != null) {
            onContinue()
        } else {
            onBackPress()
        }
    }

    BackHandler {
        leaveAuthScreen()
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearQrLoginSession()
        }
    }

    LaunchedEffect(uiState.authState, isSignedIn, uiState.qrLoginCode, uiState.isLoading, uiState.error, exitRequested) {
        if (
            useQrLogin &&
            !exitRequested &&
            uiState.authState !is AuthState.Loading &&
            !isSignedIn &&
            uiState.qrLoginCode.isNullOrBlank() &&
            uiState.error.isNullOrBlank() &&
            !uiState.isLoading
        ) {
            viewModel.startQrLogin()
        }
    }

    LaunchedEffect(isSignedIn) {
        if (useQrLogin && isSignedIn && !uiState.qrLoginCode.isNullOrBlank()) {
            viewModel.clearQrLoginSession()
        }
    }

    LaunchedEffect(isApproved, uiState.isLoading) {
        if (useQrLogin && isApproved && !uiState.isLoading) {
            viewModel.exchangeQrLogin()
        }
    }

    LaunchedEffect(isOnboardingMode, isSignedIn) {
        if (!isOnboardingMode || onboardingTransitionHandled) return@LaunchedEffect
        if (isSignedIn) {
            onboardingTransitionHandled = true
            exitRequested = true
            viewModel.clearQrLoginSession()
            onContinue.invoke()
        }
    }

    // Nuvio Launcher (approved mockup): the launcher's own background; no Nuvio logo panel, no glass panel and no
    // server menu; everything in one centred column.
    Box(modifier = Modifier.fillMaxSize()) {
        com.nuvio.tv.ui.launcher.LauncherBlurredBackdrop(com.nuvio.tv.LauncherChrome.lastBackdrop.value)
        AuthQrLoginPane(
            modifier = Modifier
                .align(Alignment.Center)
                .widthIn(max = 560.dp),
            fullAccount = fullAccount,
            uiState = uiState,
            isSignedIn = isSignedIn,
            isOnboardingMode = isOnboardingMode,
            useEmailLogin = useEmailLogin,
            onSignIn = viewModel::signIn,
            onRefreshOrSignOut = {
                if (isSignedIn) {
                    showSignOutConfirmation = true
                } else {
                    viewModel.startQrLogin()
                }
            },
            onBackOrContinue = {
                if (isOnboardingMode) {
                    continueFromAuthScreen()
                } else {
                    leaveAuthScreen()
                }
            },
            initialFocusRequester = loginFocusRequester
        )
    }

    LaunchedEffect(Unit) {
        loginFocusRequester.requestFocusAfterFrames(frames = 3)
    }

    if (showSignOutConfirmation) {
        AccountSignOutConfirmationDialog(
            onConfirm = {
                viewModel.signOut()
                showSignOutConfirmation = false
            },
            onDismiss = { showSignOutConfirmation = false }
        )
    }
}

@Composable
private fun AuthQrBrandPanel(
    modifier: Modifier,
    isSignedIn: Boolean,
    fullAccount: AuthState.FullAccount?,
    useEmailLogin: Boolean
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center
    ) {
        BrandWordmark(
            contentDescription = stringResource(R.string.cd_nuvio),
            modifier = Modifier.height(60.dp),
            contentScale = ContentScale.Fit
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = stringResource(R.string.auth_qr_tagline),
            modifier = Modifier.widthIn(max = 440.dp),
            style = MaterialTheme.typography.displayLarge.copy(
                color = AuthTextPrimary,
                fontSize = 40.sp,
                lineHeight = 45.sp,
                fontWeight = FontWeight.SemiBold
            )
        )
        if (isSignedIn || useEmailLogin) {
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = if (isSignedIn) {
                    stringResource(R.string.auth_qr_connected)
                } else {
                    stringResource(R.string.auth_email_hint)
                },
                modifier = Modifier.widthIn(max = 400.dp),
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = AuthTextSecondary,
                    fontSize = 17.sp,
                    lineHeight = 26.sp,
                    fontWeight = FontWeight.Normal
                )
            )
        }
        if (isSignedIn && fullAccount != null) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = fullAccount.email,
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF7CFF9B)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = fullAccount.userId,
                style = MaterialTheme.typography.bodySmall,
                color = AuthTextSecondary
            )
        }
    }
}

@Composable
private fun AuthQrLoginPane(
    modifier: Modifier,
    fullAccount: AuthState.FullAccount?,
    uiState: AccountUiState,
    isSignedIn: Boolean,
    isOnboardingMode: Boolean,
    useEmailLogin: Boolean,
    onSignIn: (String, String) -> Unit,
    onRefreshOrSignOut: () -> Unit,
    onBackOrContinue: () -> Unit,
    initialFocusRequester: FocusRequester
) {
    val focusEmail = useEmailLogin && !isSignedIn
    val focusMainAction = !focusEmail && !uiState.isLoading
    // Sizes (approved mockup, then everything 20% smaller): heading 20.8sp white, text 11.2sp / 10.4sp / 9.6sp grey,
    // QR 144dp, code 19.2sp white, 28.8dp pills.
    Column(
        modifier = modifier.padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.auth_qr_tagline),
            style = MaterialTheme.typography.displayLarge.copy(
                color = Color.White,
                fontSize = 20.8.sp,
                lineHeight = 25.6.sp,
                fontWeight = FontWeight.SemiBold
            ),
            textAlign = TextAlign.Center
        )
        if (isSignedIn && fullAccount != null) {
            Text(   // the signed-in email (was on the removed left panel)
                text = fullAccount.email,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.2.sp),
                color = com.nuvio.tv.ui.launcher.LauncherStyle.TileTitleIdle,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (isSignedIn) {
                stringResource(R.string.auth_qr_synced_data)
            } else if (useEmailLogin) {
                stringResource(R.string.auth_email_instruction)
            } else {
                stringResource(R.string.auth_qr_scan_instruction)
            },
            style = MaterialTheme.typography.bodyLarge.copy(
                color = com.nuvio.tv.ui.launcher.LauncherStyle.TileTitleIdle,
                fontSize = 11.2.sp,
                lineHeight = 16.8.sp
            ),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (isSignedIn && !isOnboardingMode) {
            AccountConnectedStatsStrip(
                stats = uiState.connectedStats,
                isLoading = uiState.isStatsLoading
            )
        } else if (isSignedIn && isOnboardingMode) {
            StatusPill(
                text = stringResource(R.string.auth_qr_finishing),
                containerColor = AuthSecondaryButtonBackground,
                contentColor = AuthTextSecondary
            )
        } else if (useEmailLogin) {
            AuthEmailLoginForm(
                uiState = uiState,
                onSignIn = onSignIn,
                initialFocusRequester = initialFocusRequester
            )
        } else {
            AuthQrCodeBlock(uiState = uiState)
        }

        Spacer(modifier = Modifier.height(16.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(9.6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSignedIn || !useEmailLogin) {
                AuthLauncherPill(
                    text = when {
                        isSignedIn -> stringResource(R.string.account_sign_out)
                        uiState.isLoading -> stringResource(R.string.auth_qr_please_wait)
                        else -> stringResource(R.string.auth_qr_refresh)
                    },
                    onClick = onRefreshOrSignOut,
                    enabled = !uiState.isLoading,
                    modifier = if (focusMainAction) Modifier.focusRequester(initialFocusRequester) else Modifier
                )
            }
            AuthLauncherPill(
                text = if (isOnboardingMode) {
                    if (isSignedIn) stringResource(R.string.auth_qr_continue) else stringResource(R.string.auth_qr_continue_without_account)
                } else {
                    stringResource(R.string.auth_qr_back)
                },
                onClick = onBackOrContinue,
                modifier = if (!focusEmail && !focusMainAction) {
                    Modifier.focusRequester(initialFocusRequester)
                } else {
                    Modifier
                }
            )
        }
    }
}

/** Nuvio Launcher (approved mockup): a 28.8dp pill, 11.2sp; grey on white 12%, focused the white pill, dark bold. */
@Composable
@OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)
private fun AuthLauncherPill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var focused by remember { mutableStateOf(false) }
    androidx.tv.material3.Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .height(28.8.dp)
            .onFocusChanged { focused = it.isFocused },
        colors = androidx.tv.material3.ClickableSurfaceDefaults.colors(
            containerColor = Color.White.copy(alpha = 0.12f),
            focusedContainerColor = com.nuvio.tv.ui.launcher.LauncherStyle.FocusWhite,
            disabledContainerColor = Color.White.copy(alpha = 0.06f)
        ),
        scale = androidx.tv.material3.ClickableSurfaceDefaults.scale(focusedScale = 1f),
        shape = androidx.tv.material3.ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(999.dp))
    ) {
        Box(modifier = Modifier.fillMaxHeight().padding(horizontal = 14.4.dp), contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 11.2.sp,
                    fontWeight = if (focused) FontWeight.Bold else FontWeight.Normal
                ),
                color = if (focused) com.nuvio.tv.ui.launcher.LauncherStyle.FocusText else com.nuvio.tv.ui.launcher.LauncherStyle.TileTitleIdle
            )
        }
    }
}

@Composable
private fun AuthEmailLoginForm(
    uiState: AccountUiState,
    onSignIn: (String, String) -> Unit,
    initialFocusRequester: FocusRequester
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    val canSignIn = email.isNotBlank() && password.isNotBlank() && !uiState.isLoading
    val submit = {
        if (canSignIn) {
            onSignIn(email.trim(), password)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.md)
    ) {
        InputField(
            value = email,
            onValueChange = { email = it },
            placeholder = stringResource(R.string.auth_email_placeholder),
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
            modifier = Modifier.focusRequester(initialFocusRequester)
        )
        InputField(
            value = password,
            onValueChange = { password = it },
            placeholder = stringResource(R.string.auth_password_placeholder),
            keyboardType = KeyboardType.Password,
            isPassword = true,
            imeAction = ImeAction.Done,
            onImeAction = submit
        )
        Button(
            onClick = submit,
            enabled = canSignIn,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.colors(
                containerColor = Color.White,
                focusedContainerColor = Color.White,
                contentColor = Color.Black,
                focusedContentColor = Color.Black,
                disabledContainerColor = Color.White.copy(alpha = 0.12f),
                disabledContentColor = AuthTextPrimary.copy(alpha = 0.58f)
            ),
            shape = ButtonDefaults.shape(RoundedCornerShape(16.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (uiState.isLoading) {
                        stringResource(R.string.auth_email_signing_in)
                    } else {
                        stringResource(R.string.auth_email_sign_in)
                    },
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }
        }
        AuthTermsAcknowledgement()
        uiState.error?.takeIf { it.isNotBlank() }?.let { error ->
            StatusPill(
                text = error,
                containerColor = Color(0x33C62828),
                contentColor = Color(0xFFFF6E6E)
            )
        }
    }
}

@Composable
private fun AuthQrCodeBlock(
    uiState: AccountUiState
) {
    val qrBitmap = remember(uiState.qrLoginBitmap) { uiState.qrLoginBitmap?.asImageBitmap() }
    if (qrBitmap != null) {
        Image(
            bitmap = qrBitmap,
            contentDescription = stringResource(R.string.cd_qr_login),
            modifier = Modifier
                .size(144.dp)
                .background(Color.White, RoundedCornerShape(6.4.dp))
                .padding(6.4.dp),
            contentScale = ContentScale.Fit
        )
    } else {
        Box(
            modifier = Modifier
                .size(144.dp)
                .background(AuthSecondaryButtonBackground, RoundedCornerShape(6.4.dp))
                .border(1.dp, AuthSecondaryButtonBorder, RoundedCornerShape(6.4.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (uiState.isLoading) stringResource(R.string.auth_qr_generating) else stringResource(R.string.auth_qr_unavailable),
                color = AuthTextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }

    AuthQrManualCodeDetails(
        verificationUri = uiState.qrLoginVerificationUri,
        qrLoginCode = uiState.qrLoginUserCode ?: uiState.qrLoginCode,
        expiresAtMillis = uiState.qrLoginExpiresAtMillis,
        isLoading = uiState.isLoading
    )

    Spacer(modifier = Modifier.height(9.6.dp))
    AuthTermsAcknowledgement()

    val statusText = uiState.error ?: uiState.qrLoginStatus
    if (!statusText.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(9.6.dp))
        if (uiState.error != null) {
            StatusPill(
                text = statusText,
                containerColor = Color(0x33C62828),
                contentColor = Color(0xFFFF6E6E)
            )
        } else {
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.6.sp),
                color = com.nuvio.tv.ui.launcher.LauncherStyle.TileTitleIdle,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun AuthQrManualCodeDetails(
    verificationUri: String?,
    qrLoginCode: String?,
    expiresAtMillis: Long?,
    isLoading: Boolean
) {
    val displayUri = verificationUri?.takeIf { it.isNotBlank() }
    val displayCode = qrLoginCode?.takeIf { it.isNotBlank() }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (displayUri != null && displayCode != null) {
            Spacer(modifier = Modifier.height(12.8.dp))
            Text(
                text = stringResource(R.string.auth_qr_manual_instruction, displayVerificationUri(displayUri)),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 10.4.sp),
                color = com.nuvio.tv.ui.launcher.LauncherStyle.TileTitleIdle,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.4.dp))
            Text(
                text = formatDeviceLoginCode(displayCode),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = if (displayCode.length == 6) 19.2.sp else 11.2.sp,
                    letterSpacing = if (displayCode.length == 6) 2.4.sp else 0.sp
                ),
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            if (expiresAtMillis != null) {
                Spacer(modifier = Modifier.height(4.8.dp))
                AuthQrExpiresText(expiresAtMillis = expiresAtMillis)
            }
        } else if (isLoading) {
            val shimmerBrush = rememberShimmerBrush(backdropAware = true)
            Spacer(modifier = Modifier.height(18.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp),
                contentAlignment = Alignment.Center
            ) {
                SkeletonBar(width = 238.dp, height = 12.dp, brush = shimmerBrush, cornerRadius = 6.dp)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp),
                contentAlignment = Alignment.Center
            ) {
                SkeletonBar(width = 116.dp, height = 22.dp, brush = shimmerBrush, cornerRadius = 8.dp)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp),
                contentAlignment = Alignment.Center
            ) {
                SkeletonBar(width = 84.dp, height = 8.dp, brush = shimmerBrush, cornerRadius = 4.dp)
            }
        }
    }
}

// Owns the 1 s ticker so only this line recomposes while the code counts down.
@Composable
private fun AuthQrExpiresText(expiresAtMillis: Long) {
    val nowMillis by produceState(initialValue = System.currentTimeMillis(), key1 = expiresAtMillis) {
        while (true) {
            value = System.currentTimeMillis()
            delay(1_000)
        }
    }
    val remainingMillis = (expiresAtMillis - nowMillis).coerceAtLeast(0L)
    Text(
        text = stringResource(R.string.auth_qr_expires, formatDuration(remainingMillis)),
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.6.sp),
        color = com.nuvio.tv.ui.launcher.LauncherStyle.TileTitleIdle
    )
}

private fun formatDeviceLoginCode(value: String): String =
    if (value.length == 6) "${value.take(3)}-${value.drop(3)}" else value

private fun displayVerificationUri(value: String): String = value
    .removePrefix("https://")
    .removePrefix("http://")
    .trimEnd('/')

@Composable
private fun AuthTermsAcknowledgement() {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.auth_qr_terms_prefix),
            style = MaterialTheme.typography.bodyMedium.copy(
                color = com.nuvio.tv.ui.launcher.LauncherStyle.TileTitleIdle,
                fontSize = 9.6.sp,
                lineHeight = 14.sp
            )
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = stringResource(R.string.auth_qr_terms_link),
            modifier = Modifier
                .clickable {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://nuvio.tv/terms")))
                }
                .semantics { role = Role.Button },
            style = MaterialTheme.typography.bodyMedium.copy(
                color = com.nuvio.tv.ui.launcher.LauncherStyle.TileTitleIdle,
                fontSize = 9.6.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@Composable
private fun StatusPill(
    text: String,
    containerColor: Color,
    contentColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(NuvioTheme.spacing.hairline, NuvioTheme.colors.Border.copy(alpha = 0.35f), RoundedCornerShape(NuvioTheme.radii.md))
            .background(containerColor, RoundedCornerShape(NuvioTheme.radii.md))
            .padding(horizontal = NuvioTheme.spacing.md, vertical = 10.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = contentColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.wrapContentHeight()
        )
    }
}

@Composable
private fun AccountConnectedStatsStrip(
    stats: AccountConnectedStats?,
    isLoading: Boolean
) {
    val values = if (isLoading) {
        listOf("...", "...", "...", "...")
    } else {
        listOf(
            (stats?.addons ?: 0).toString(),
            (stats?.plugins ?: 0).toString(),
            (stats?.library ?: 0).toString(),
            (stats?.watchProgress ?: 0).toString()
        )
    }
    val labels = listOf(
        stringResource(R.string.account_stat_addons),
        stringResource(R.string.account_stat_plugins),
        stringResource(R.string.account_stat_library),
        stringResource(R.string.account_stat_progress)
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.sm)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(NuvioTheme.spacing.hairline)
                .background(NuvioTheme.colors.Border.copy(alpha = 0.8f))
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(values.size) { index ->
                AccountStatItem(
                    value = values[index],
                    label = labels[index],
                    modifier = Modifier.weight(1f)
                )
                if (index != values.lastIndex) {
                    Box(
                        modifier = Modifier
                            .height(44.dp)
                            .width(NuvioTheme.spacing.hairline)
                            .background(NuvioTheme.colors.Border.copy(alpha = 0.75f))
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(NuvioTheme.spacing.hairline)
                .background(NuvioTheme.colors.Border.copy(alpha = 0.8f))
        )
    }
}

@Composable
private fun AccountStatItem(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = NuvioTheme.colors.TextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(NuvioTheme.spacing.xxs))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = NuvioTheme.colors.TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

private fun Modifier.authGradientBackground(): Modifier = drawWithCache {
    val angleRadians = 122.0 * PI / 180.0
    val directionX = sin(angleRadians).toFloat()
    val directionY = (-cos(angleRadians)).toFloat()
    val halfLength = (abs(size.width * directionX) + abs(size.height * directionY)) / 2f
    val center = Offset(size.width / 2f, size.height / 2f)
    val start = Offset(
        x = center.x - directionX * halfLength,
        y = center.y - directionY * halfLength
    )
    val end = Offset(
        x = center.x + directionX * halfLength,
        y = center.y + directionY * halfLength
    )
    val brush = Brush.linearGradient(
        colorStops = arrayOf(
            0f to Color(0xFF21113B),
            0.14f to Color(0xFF21113B),
            0.26f to Color(0xFF1A0E2F),
            0.36f to Color(0xFF130A23),
            0.48f to Color(0xFF0A060F),
            0.60f to Color(0xFF050408),
            0.70f to Color.Black,
            1f to Color.Black
        ),
        start = start,
        end = end
    )
    onDrawBehind {
        drawRect(brush = brush)
    }
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0L)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
