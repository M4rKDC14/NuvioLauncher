package com.nuvio.tv.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Typography
import com.nuvio.tv.R
import com.nuvio.tv.domain.model.AppFont

// The bundled fonts are variable fonts: without explicit variation settings every weight drew as Regular.
@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private fun vFont(res: Int, weight: FontWeight) = Font(
    res, weight,
    variationSettings = androidx.compose.ui.text.font.FontVariation.Settings(weight, androidx.compose.ui.text.font.FontStyle.Normal)
)

val DMSansFamily = FontFamily(
    vFont(R.font.dm_sans_variable, FontWeight.Normal),
    vFont(R.font.dm_sans_variable, FontWeight.Medium),
    vFont(R.font.dm_sans_variable, FontWeight.SemiBold),
    vFont(R.font.dm_sans_variable, FontWeight.Bold)
)

val InterFamily = FontFamily(
    vFont(R.font.inter_variable, FontWeight.Normal),
    vFont(R.font.inter_variable, FontWeight.Medium),
    vFont(R.font.inter_variable, FontWeight.SemiBold),
    vFont(R.font.inter_variable, FontWeight.Bold)
)

val OpenSansFamily = FontFamily(
    vFont(R.font.opensans_variable, FontWeight.Normal),
    vFont(R.font.opensans_variable, FontWeight.Medium),
    vFont(R.font.opensans_variable, FontWeight.SemiBold),
    vFont(R.font.opensans_variable, FontWeight.Bold)
)

// Nuvio Launcher: San Francisco alternatives (Manrope, Plus Jakarta Sans), a Helvetica-style font (Arimo) and
// Microsoft's open Segoe UI stand-in (Selawik). Licences in assets/font-licenses (Selawik: SIL OFL 1.1).
val ManropeFamily = FontFamily(
    vFont(R.font.manrope_variable, FontWeight.Normal),
    vFont(R.font.manrope_variable, FontWeight.Medium),
    vFont(R.font.manrope_variable, FontWeight.SemiBold),
    vFont(R.font.manrope_variable, FontWeight.Bold)
)

val PlusJakartaSansFamily = FontFamily(
    vFont(R.font.plus_jakarta_sans_variable, FontWeight.Normal),
    vFont(R.font.plus_jakarta_sans_variable, FontWeight.Medium),
    vFont(R.font.plus_jakarta_sans_variable, FontWeight.SemiBold),
    vFont(R.font.plus_jakarta_sans_variable, FontWeight.Bold)
)

val ArimoFamily = FontFamily(
    vFont(R.font.arimo_variable, FontWeight.Normal),
    vFont(R.font.arimo_variable, FontWeight.Medium),
    vFont(R.font.arimo_variable, FontWeight.SemiBold),
    vFont(R.font.arimo_variable, FontWeight.Bold)
)

// Selawik comes as separate files (not a variable font): regular also serves Medium, which it has no file for.
val SelawikFamily = FontFamily(
    Font(R.font.selawik_regular, FontWeight.Normal),
    Font(R.font.selawik_regular, FontWeight.Medium),
    Font(R.font.selawik_semibold, FontWeight.SemiBold),
    Font(R.font.selawik_bold, FontWeight.Bold)
)

fun getFontFamily(appFont: AppFont): FontFamily = when (appFont) {
    AppFont.INTER -> InterFamily
    AppFont.MANROPE -> ManropeFamily
    AppFont.PLUS_JAKARTA_SANS -> PlusJakartaSansFamily
    AppFont.ARIMO -> ArimoFamily
    AppFont.SELAWIK -> SelawikFamily
    AppFont.DM_SANS -> DMSansFamily
    AppFont.OPEN_SANS -> OpenSansFamily
}

@Immutable
data class NuvioTextStyleTokens(
    val display: TextStyle,
    val displayCompact: TextStyle,
    val headline: TextStyle,
    val sectionTitle: TextStyle,
    val cardTitle: TextStyle,
    val body: TextStyle,
    val bodyCompact: TextStyle,
    val metadata: TextStyle,
    val badge: TextStyle,
    val button: TextStyle,
    val tab: TextStyle,
    val nav: TextStyle,
    val playerControl: TextStyle
)

@OptIn(ExperimentalTvMaterial3Api::class)
fun buildNuvioTypography(fontFamily: FontFamily): Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 48.sp,
        lineHeight = 56.sp,
        letterSpacing = (-0.5).sp
    ),
    displayMedium = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.5.sp
    )
)

@OptIn(ExperimentalTvMaterial3Api::class)
val NuvioTypography = buildNuvioTypography(InterFamily)

@OptIn(ExperimentalTvMaterial3Api::class)
fun buildNuvioTextStyles(typography: Typography): NuvioTextStyleTokens = NuvioTextStyleTokens(
    display = typography.displayLarge,
    displayCompact = typography.displayMedium,
    headline = typography.headlineLarge,
    sectionTitle = typography.headlineMedium,
    cardTitle = typography.titleMedium,
    body = typography.bodyLarge,
    bodyCompact = typography.bodyMedium,
    metadata = typography.labelMedium,
    badge = typography.labelSmall.copy(
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.8.sp
    ),
    button = typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    tab = typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
    nav = typography.titleMedium,
    playerControl = typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
)

@OptIn(ExperimentalTvMaterial3Api::class)
val NuvioTextStyles = buildNuvioTextStyles(NuvioTypography)
