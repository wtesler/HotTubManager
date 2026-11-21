package tesler.will.hottubmanager.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = SpaPrimaryLight,
    onPrimary = SpaOnPrimaryLight,
    primaryContainer = SpaPrimaryContainerLight,
    onPrimaryContainer = SpaOnPrimaryContainerLight,
    secondary = SpaSecondaryLight,
    onSecondary = SpaOnSecondaryLight,
    secondaryContainer = SpaSecondaryContainerLight,
    onSecondaryContainer = SpaOnSecondaryContainerLight,
    tertiary = SpaTertiaryLight,
    onTertiary = SpaOnTertiaryLight,
    tertiaryContainer = SpaTertiaryContainerLight,
    onTertiaryContainer = SpaOnTertiaryContainerLight,
    error = SpaErrorLight,
    onError = SpaOnErrorLight,
    errorContainer = SpaErrorContainerLight,
    onErrorContainer = SpaOnErrorContainerLight,
    background = SpaBackgroundLight,
    onBackground = SpaOnBackgroundLight,
    surface = SpaSurfaceLight,
    onSurface = SpaOnSurfaceLight,
    surfaceVariant = SpaSurfaceVariantLight,
    onSurfaceVariant = SpaOnSurfaceVariantLight,
    outline = SpaOutlineLight
)

private val DarkColorScheme = darkColorScheme(
    primary = SpaPrimaryDark,
    onPrimary = SpaOnPrimaryDark,
    primaryContainer = SpaPrimaryContainerDark,
    onPrimaryContainer = SpaOnPrimaryContainerDark,
    secondary = SpaSecondaryDark,
    onSecondary = SpaOnSecondaryDark,
    secondaryContainer = SpaSecondaryContainerDark,
    onSecondaryContainer = SpaOnSecondaryContainerDark,
    tertiary = SpaTertiaryDark,
    onTertiary = SpaOnTertiaryDark,
    tertiaryContainer = SpaTertiaryContainerDark,
    onTertiaryContainer = SpaOnTertiaryContainerDark,
    error = SpaErrorDark,
    onError = SpaOnErrorDark,
    errorContainer = SpaErrorContainerDark,
    onErrorContainer = SpaOnErrorContainerDark,
    background = SpaBackgroundDark,
    onBackground = SpaOnBackgroundDark,
    surface = SpaSurfaceDark,
    onSurface = SpaOnSurfaceDark,
    surfaceVariant = SpaSurfaceVariantDark,
    onSurfaceVariant = SpaOnSurfaceVariantDark,
    outline = SpaOutlineDark
)

@Immutable
data class SpaSemanticColors(
    val good: Color,
    val goodContainer: Color,
    val warning: Color,
    val warningContainer: Color
)

private val LightSemanticColors = SpaSemanticColors(
    good = SpaStatusGreenLight,
    goodContainer = SpaStatusGreenContainerLight,
    warning = SpaStatusAmberLight,
    warningContainer = SpaStatusAmberContainerLight
)

private val DarkSemanticColors = SpaSemanticColors(
    good = SpaStatusGreenDark,
    goodContainer = SpaStatusGreenContainerDark,
    warning = SpaStatusAmberDark,
    warningContainer = SpaStatusAmberContainerDark
)

val LocalSpaColors = staticCompositionLocalOf { LightSemanticColors }

@Composable
fun HotTubTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val semanticColors = if (darkTheme) DarkSemanticColors else LightSemanticColors

    CompositionLocalProvider(LocalSpaColors provides semanticColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SpaTypography,
            content = content
        )
    }
}
