
package com.minkaart2.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Tema Minka Art.
 *
 * El color dinámico (Material You) está desactivado a propósito: con minSdk 31
 * siempre estaría activo y reemplazaría la paleta de marca por los colores del
 * fondo de pantalla del usuario.
 */

private val LightColorScheme = lightColorScheme(
    // Acción principal
    primary = Terracotta,
    onPrimary = WarmWhite,
    primaryContainer = PinkClaySoft,
    onPrimaryContainer = BrownDeep,

    // Acción secundaria
    secondary = BrownMid,
    onSecondary = Cream,
    secondaryContainer = PinkClay,
    onSecondaryContainer = BrownDeep,

    // Acento suave (chips, etiquetas)
    tertiary = PinkClay,
    onTertiary = BrownDeep,
    tertiaryContainer = PinkClaySoft,
    onTertiaryContainer = BrownDeep,

    // Fondos y superficies
    background = Beige,
    onBackground = BrownDeep,
    surface = Cream,
    onSurface = BrownDeep,
    surfaceVariant = WarmWhite,
    onSurfaceVariant = BrownMid,

    // Bordes
    outline = LineLight,
    outlineVariant = PinkClaySoft,

    // Inversos (snackbars, tooltips)
    inverseSurface = Espresso,
    inverseOnSurface = Cream,
    inversePrimary = TerracottaLight,

    scrim = Espresso
)

private val DarkColorScheme = darkColorScheme(
    primary = TerracottaLight,
    onPrimary = Espresso,
    primaryContainer = TerracottaDark,
    onPrimaryContainer = Cream,

    secondary = BrownSoft,
    onSecondary = Espresso,
    secondaryContainer = BrownDeep,
    onSecondaryContainer = Cream,

    tertiary = PinkClay,
    onTertiary = Espresso,
    tertiaryContainer = BrownDeep,
    onTertiaryContainer = PinkClaySoft,

    background = Espresso,
    onBackground = Cream,
    surface = EspressoDeep,
    onSurface = Cream,
    surfaceVariant = BrownDeep,
    onSurfaceVariant = BrownSoft,

    outline = LineDark,
    outlineVariant = BrownDeep,

    inverseSurface = Beige,
    inverseOnSurface = BrownDeep,
    inversePrimary = Terracotta,

    scrim = Espresso
)

@Composable
fun MinkaArt_btaTheme(
    // La app se muestra siempre en claro: el brand kit y las pantallas
    // están diseñados sobre fondo beige. Cambiar a isSystemInDarkTheme()
    // solo cuando el modo oscuro esté revisado pantalla por pantalla.
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}

