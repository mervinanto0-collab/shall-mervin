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
    primary = MedPrimaryDark,
    onPrimary = MedOnPrimaryDark,
    primaryContainer = MedPrimaryContainerDark,
    onPrimaryContainer = MedOnPrimaryContainerDark,
    secondary = MedSecondaryDark,
    onSecondary = MedOnSecondaryDark,
    secondaryContainer = MedSecondaryContainerDark,
    background = MedBackgroundDark,
    surface = MedSurfaceDark,
    onBackground = MedOnBackgroundDark,
    onSurface = MedOnSurfaceDark,
    surfaceVariant = MedSurfaceVariantDark
)

private val LightColorScheme = lightColorScheme(
    primary = MedPrimary,
    onPrimary = MedOnPrimary,
    primaryContainer = MedPrimaryContainer,
    onPrimaryContainer = MedOnPrimaryContainer,
    secondary = MedSecondary,
    onSecondary = MedOnSecondary,
    secondaryContainer = MedSecondaryContainer,
    onSecondaryContainer = MedOnSecondaryContainer,
    tertiary = MedTertiary,
    onTertiary = MedOnTertiary,
    tertiaryContainer = MedTertiaryContainer,
    onTertiaryContainer = MedOnTertiaryContainer,
    background = MedBackground,
    onBackground = MedOnBackground,
    surface = MedSurface,
    onSurface = MedOnSurface,
    surfaceVariant = MedSurfaceVariant,
    onSurfaceVariant = MedOnSurfaceVariant
)

@Composable
fun CarePulseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep medical brand colors consistent
    content: @Composable () -> Unit,
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

// Keep backward alias if referenced
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) = CarePulseTheme(darkTheme, dynamicColor, content)
