
package com.minkaart2.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Tema Minka Art — brand kit v2.
 *
 * Solo hay 5 tokens de marca (Guinda, Borgona, AzulClaro, AzulAcero, Marfil) para los
 * ~15 roles de Material 3, así que varios roles reutilizan el mismo token — no se
 * inventan colores fuera del brand kit.
 *
 * El color dinámico (Material You) está desactivado a propósito: con minSdk 31
 * siempre estaría activo y reemplazaría la paleta de marca por los colores del
 * fondo de pantalla del usuario.
 */

private val LightColorScheme = lightColorScheme(
    // Acción principal
    primary = Borgona,
    onPrimary = Marfil,
    primaryContainer = AzulClaro,
    onPrimaryContainer = Guinda,

    // Acción secundaria
    secondary = AzulAcero,
    onSecondary = Marfil,
    secondaryContainer = AzulClaro,
    onSecondaryContainer = Guinda,

    // Acento suave (chips, etiquetas)
    tertiary = AzulClaro,
    onTertiary = Guinda,
    tertiaryContainer = AzulClaro,
    onTertiaryContainer = Guinda,

    // Fondos y superficies
    background = Marfil,
    onBackground = Guinda,
    surface = Marfil,
    onSurface = Guinda,
    surfaceVariant = AzulClaro.copy(alpha = 0.3f),
    onSurfaceVariant = Guinda,

    // Bordes
    outline = AzulClaro,
    outlineVariant = AzulClaro.copy(alpha = 0.5f),

    // Inversos (snackbars, tooltips)
    inverseSurface = Guinda,
    inverseOnSurface = Marfil,
    inversePrimary = AzulClaro,

    scrim = Guinda
)

private val DarkColorScheme = darkColorScheme(
    primary = AzulClaro,
    onPrimary = Guinda,
    primaryContainer = Borgona,
    onPrimaryContainer = Marfil,

    secondary = AzulClaro,
    onSecondary = Guinda,
    secondaryContainer = AzulAcero,
    onSecondaryContainer = Marfil,

    tertiary = AzulClaro,
    onTertiary = Guinda,
    tertiaryContainer = Borgona,
    onTertiaryContainer = Marfil,

    background = Guinda,
    onBackground = Marfil,
    surface = Guinda,
    onSurface = Marfil,
    surfaceVariant = Borgona,
    onSurfaceVariant = AzulClaro,

    outline = AzulAcero,
    outlineVariant = Borgona,

    inverseSurface = Marfil,
    inverseOnSurface = Guinda,
    inversePrimary = Borgona,

    scrim = Guinda
)

@Composable
fun MinkaArt_btaTheme(
    // La app se muestra siempre en claro: el brand kit y las pantallas
    // están diseñados sobre fondo marfil. Cambiar a isSystemInDarkTheme()
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
