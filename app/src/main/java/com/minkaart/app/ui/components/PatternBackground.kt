package com.minkaart2.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.minkaart2.app.R

/**
 * Fondo compartido de las pantallas de entrada/autenticación: degradado diagonal de marca
 * más el patrón textil como overlay, con blur opcional.
 *
 * Importante: el blur (cuando [blurBackground] es true) se aplica a ESTA capa de fondo,
 * nunca al [content] que se dibuja encima. `Modifier.blur()` difumina el propio Composable
 * al que se aplica (y sus hijos) — no difumina lo que hay detrás, que es como funciona
 * `backdrop-filter` en CSS. Si el blur se aplicara sobre el contenido (p. ej. sobre una
 * GlassCard con texto dentro), el texto saldría borroso. Por eso el fondo y el contenido
 * son capas separadas dentro del mismo [Box]: el fondo se difumina, el contenido no.
 */
@Composable
fun PatternBackground(
    modifier: Modifier = Modifier,
    gradientColors: List<Color>,
    patternAlpha: Float,
    blurBackground: Boolean = false,
    darkOverlayAlpha: Float = 0f,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {

        // Capa de fondo: degradado + patrón. Se difumina completa cuando corresponde,
        // porque no hay nada más detrás de la tarjeta de vidrio que este fondo.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (blurBackground) Modifier.blur(14.dp) else Modifier
                )
                .background(
                    Brush.linearGradient(
                        colors = gradientColors,
                        start = Offset(0f, 0f),
                        end = Offset(1000f, 1600f)
                    )
                )
        ) {
            Image(
                painter = painterResource(id = R.drawable.bg_pattern_tejido),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Transparent),
                contentScale = ContentScale.Crop,
                alpha = patternAlpha
            )
        }

        // Velo oscuro opcional, encima del fondo (ya difuminado) y debajo del contenido.
        // Sirve para bajarle brillo al fondo cuando el texto encima cuesta leerse —
        // por ejemplo sobre la franja de AzulAcero del degradado, que es más clara.
        if (darkOverlayAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = darkOverlayAlpha))
            )
        }

        // Capa de contenido: siempre nítida, sin blur.
        content()
    }
}
