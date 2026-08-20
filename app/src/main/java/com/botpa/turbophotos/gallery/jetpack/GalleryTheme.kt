package com.botpa.turbophotos.gallery.jetpack

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.botpa.turbophotos.R

//Fonts
val FONT_OUTFIT = FontFamily(
    Font(R.font.outfit, FontWeight.Normal),
)

val FONT_FRAUNCES = FontFamily(
    Font(R.font.fraunces, FontWeight.Normal),
)

val FONT_OPIFICIO = FontFamily(
    Font(R.font.opificio_bold_rounded, FontWeight.Normal),
)

//Theme
@Composable
fun CoonTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    //Dynamic colors
    val context = LocalContext.current
    val colorScheme = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

    //Create typography
    val appTypography = Typography(
        headlineLarge = Typography().headlineLarge.copy(fontFamily = FONT_FRAUNCES),
        headlineMedium = Typography().headlineMedium.copy(fontFamily = FONT_FRAUNCES),
        headlineSmall = Typography().headlineSmall.copy(fontFamily = FONT_FRAUNCES),
        bodyLarge = Typography().bodyLarge.copy(fontFamily = FONT_OUTFIT),
        bodyMedium = Typography().bodyMedium.copy(fontFamily = FONT_OUTFIT),
        bodySmall = Typography().bodySmall.copy(fontFamily = FONT_OUTFIT)
    )

    //Material theme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = appTypography,
        content = content
    )
}
