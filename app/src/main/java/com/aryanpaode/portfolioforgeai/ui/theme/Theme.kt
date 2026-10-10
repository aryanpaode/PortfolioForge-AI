package com.aryanpaode.portfolioforgeai.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val PortfolioForgeColors = lightColorScheme(
    primary = Color(0xFFB71C1C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD6),
    onPrimaryContainer = Color(0xFF410002),
    secondary = Color(0xFF8C1D18),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDAD6),
    onSecondaryContainer = Color(0xFF410002),
    tertiary = Color(0xFF7A5900),
    background = Color(0xFFFFFBFB),
    onBackground = Color(0xFF211A1A),
    surface = Color.White,
    onSurface = Color(0xFF211A1A),
    surfaceVariant = Color(0xFFFFF0EF),
    onSurfaceVariant = Color(0xFF534342),
    outline = Color(0xFF8A7472),
    error = Color(0xFFBA1A1A),
    onError = Color.White
)

private val PortfolioForgeTypography = Typography(
    headlineMedium = TextStyle(fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold),
    headlineSmall = TextStyle(fontSize = 25.sp, lineHeight = 31.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 17.sp, lineHeight = 25.sp),
    bodyMedium = TextStyle(fontSize = 16.sp, lineHeight = 23.sp),
    bodySmall = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 14.sp, lineHeight = 19.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 12.sp, lineHeight = 17.sp)
)

@Composable
fun PortfolioForgeTheme(content: @Composable () -> Unit) {
    // Fixed red/white palette for a consistent college-demo appearance on all devices.
    MaterialTheme(
        colorScheme = PortfolioForgeColors,
        typography = PortfolioForgeTypography,
        content = content
    )
}
