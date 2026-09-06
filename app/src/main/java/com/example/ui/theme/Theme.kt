package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = RoyalBlueDark,
    onPrimary = Color(0xFF0F172A),
    primaryContainer = RoyalBlueContainerDark,
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = AmberGoldDark,
    onSecondary = Color(0xFF78350F),
    secondaryContainer = AmberGoldContainerDark,
    onSecondaryContainer = Color(0xFFFEF3C7),
    tertiary = EmeraldSuccessDark,
    background = SlateBackgroundDark,
    surface = SlateSurfaceDark,
    onBackground = SlateOnSurfaceDark,
    onSurface = SlateOnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    outline = SlateOutlineDark
)

private val LightColorScheme = lightColorScheme(
    primary = RoyalBlueLight,
    onPrimary = Color.White,
    primaryContainer = RoyalBlueContainerLight,
    onPrimaryContainer = Color(0xFF1E3A8A),
    secondary = AmberGoldLight,
    onSecondary = Color.White,
    secondaryContainer = AmberGoldContainerLight,
    onSecondaryContainer = Color(0xFF78350F),
    tertiary = EmeraldSuccessLight,
    background = SlateBackgroundLight,
    surface = SlateSurfaceLight,
    onBackground = SlateOnSurfaceLight,
    onSurface = SlateOnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    outline = SlateOutlineLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun ApexCommerceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MyApplicationTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}

