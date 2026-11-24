package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = MuniPrimaryDark,
    onPrimary = MuniOnPrimaryDark,
    primaryContainer = MuniPrimaryContainerDark,
    onPrimaryContainer = MuniOnPrimaryContainerDark,
    secondary = MuniSecondaryDark,
    onSecondary = MuniOnSecondaryDark,
    secondaryContainer = MuniSecondaryContainerDark,
    onSecondaryContainer = MuniOnSecondaryContainerDark,
    tertiary = MuniTertiaryDark,
    onTertiary = MuniOnTertiaryDark,
    tertiaryContainer = MuniTertiaryContainerDark,
    onTertiaryContainer = MuniOnTertiaryContainerDark,
    background = MuniBackgroundDark,
    onBackground = MuniOnBackgroundDark,
    surface = MuniSurfaceDark,
    onSurface = MuniOnSurfaceDark,
    surfaceVariant = MuniSurfaceVariantDark,
    onSurfaceVariant = MuniOnSurfaceVariantDark,
    outlineVariant = MuniOutlineVariantDark
)

private val LightColorScheme = lightColorScheme(
    primary = MuniPrimaryLight,
    onPrimary = MuniOnPrimaryLight,
    primaryContainer = MuniPrimaryContainerLight,
    onPrimaryContainer = MuniOnPrimaryContainerLight,
    secondary = MuniSecondaryLight,
    onSecondary = MuniOnSecondaryLight,
    secondaryContainer = MuniSecondaryContainerLight,
    onSecondaryContainer = MuniOnSecondaryContainerLight,
    tertiary = MuniTertiaryLight,
    onTertiary = MuniOnTertiaryLight,
    tertiaryContainer = MuniTertiaryContainerLight,
    onTertiaryContainer = MuniOnTertiaryContainerLight,
    background = MuniBackgroundLight,
    onBackground = MuniOnBackgroundLight,
    surface = MuniSurfaceLight,
    onSurface = MuniOnSurfaceLight,
    surfaceVariant = MuniSurfaceVariantLight,
    onSurfaceVariant = MuniOnSurfaceVariantLight,
    outlineVariant = MuniOutlineVariantLight
)

@Composable
fun RevenuePilotTheme(
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
