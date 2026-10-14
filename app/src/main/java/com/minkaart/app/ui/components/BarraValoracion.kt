package com.minkaart2.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.minkaart2.app.R

private val Guinda = Color(0xFF3B0F1A)
private val Borgona = Color(0xFF66172E)
private val AzulAcero = Color(0xFF55708E)
private val TextoSecundario = Color(0xFF8A7480)
private val PistaBarra = Color(0xFFEFE0E3)

private val TOTAL_PUNTOS = 4.0

/**
 * Barra de Calidad/Confianza con degradado de marca — reemplaza a `ConfianzaDots` (4
 * puntos) siguiendo el diseño de referencia: etiqueta y conteo de verificaciones arriba,
 * barra de progreso debajo. [promedio] va en la misma escala 1.0–4.0 que ya usan
 * `Product.calidadPromedio`/`ArtesanoStats.confianzaPromedio` — la normalización a 0–1
 * la hace este componente, los call sites ya no dividen por 4.0 ellos mismos.
 *
 * Si [conteo] es 0 o [promedio] es `null`, no se inventa un nivel: solo se pinta la
 * pista gris con "Sin verificaciones" — mismo criterio que
 * `EscalaValoracion.etiqueta`.
 */
@Composable
fun BarraValoracion(
    etiqueta: String,
    promedio: Double?,
    conteo: Int,
    modifier: Modifier = Modifier
) {

    Column(modifier = modifier.fillMaxWidth()) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            Text(
                text = etiqueta,
                color = Guinda,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = textoConteo(conteo, promedio),
                color = TextoSecundario,
                style = MaterialTheme.typography.labelSmall
            )
        }

        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(PistaBarra)
        ) {

            if (conteo > 0 && promedio != null) {

                val porcentaje = (promedio / TOTAL_PUNTOS).coerceIn(0.0, 1.0).toFloat()

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(porcentaje)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Brush.horizontalGradient(listOf(Guinda, Borgona, AzulAcero)))
                )
            }
        }
    }
}

/**
 * Variante compacta para la tarjeta del feed (`ProductoFeedCard`): solo la barra, sin
 * etiqueta ni conteo, del mismo alto que ocupaban los puntos de `ConfianzaDots` para no
 * hacer crecer la tarjeta.
 */
@Composable
fun BarraValoracionCompacta(
    promedio: Double?,
    modifier: Modifier = Modifier
) {

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(5.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(PistaBarra)
    ) {

        if (promedio != null) {

            val porcentaje = (promedio / TOTAL_PUNTOS).coerceIn(0.0, 1.0).toFloat()

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(porcentaje)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Brush.horizontalGradient(listOf(Guinda, Borgona, AzulAcero)))
            )
        }
    }
}

@Composable
private fun textoConteo(conteo: Int, promedio: Double?): String {

    if (conteo == 0 || promedio == null) {
        return stringResource(R.string.sin_verificaciones)
    }

    return stringResource(
        if (conteo == 1) R.string.una_verificacion else R.string.n_verificaciones,
        conteo
    )
}
