package com.botpa.turbophotos.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.botpa.turbophotos.R

//Fonts
val FONT_COMFORTAA = FontFamily(
    Font(R.font.comfortaa_medium, FontWeight.Medium),
    Font(R.font.comfortaa_bold, FontWeight.Bold),
)

val FONT_POPPINS = FontFamily(
    Font(R.font.poppins, FontWeight.Normal),
)

val FONT_OPIFICIO = FontFamily(
    Font(R.font.opificio_bold_rounded, FontWeight.Bold),
)

//Theme
@Composable
fun CoonTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    //Dynamic colors
    val context = LocalContext.current
    val colorScheme = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

    //Create typography
    val appTypography = androidx.compose.material3.Typography(
        headlineLarge = androidx.compose.material3.Typography().headlineLarge.copy(fontFamily = FONT_COMFORTAA),
        headlineMedium = androidx.compose.material3.Typography().headlineMedium.copy(fontFamily = FONT_COMFORTAA),
        headlineSmall = androidx.compose.material3.Typography().headlineSmall.copy(fontFamily = FONT_COMFORTAA),
        bodyLarge = androidx.compose.material3.Typography().bodyLarge.copy(fontFamily = FONT_POPPINS),
        bodyMedium = androidx.compose.material3.Typography().bodyMedium.copy(fontFamily = FONT_POPPINS),
        bodySmall = androidx.compose.material3.Typography().bodySmall.copy(fontFamily = FONT_POPPINS)
    )

    //Material theme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = appTypography,
        content = content
    )
}