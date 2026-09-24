package com.minkaart2.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val Marfil = Color(0xFFF2EDE6)

/**
 * Selector de rol Artesano/Cliente como dos "pills" del mismo ancho, en vez de los
 * RadioButton originales. Mueve exactamente el mismo estado (`rol: String`) que ya
 * controla qué campos adicionales muestra RegisterScreen — no requiere cambios en
 * RegisterViewModel.
 *
 * Los parámetros de color son opcionales y, por defecto, reproducen el estilo
 * original (pensado para fondos oscuros, como Login/Registro). `HomeArtesanoScreen`/
 * `HomeClienteScreen` reutilizan este mismo componente para el cambio de modo
 * artesano/cliente pasando la pareja de colores que ya usan sus chips de categoría
 * (fondo claro), en vez de duplicar el patrón de "pills".
 */
@Composable
fun RolePillToggle(
    opciones: List<Pair<String, String>>, // (valor, etiqueta)
    valorSeleccionado: String,
    onSeleccionar: (String) -> Unit,
    modifier: Modifier = Modifier,
    colorFondoActivo: Color = Color.White.copy(alpha = 0.22f),
    colorFondoInactivo: Color = Color.Transparent,
    colorBorde: Color = Marfil.copy(alpha = 0.35f),
    colorTextoActivo: Color = Marfil,
    colorTextoInactivo: Color = Marfil.copy(alpha = 0.85f)
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
    ) {
        opciones.forEach { (valor, etiqueta) ->
            val activo = valor == valorSeleccionado

            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        color = if (activo) colorFondoActivo else colorFondoInactivo,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .border(
                        width = 1.2.dp,
                        color = colorBorde,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onSeleccionar(valor) }
                    )
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = etiqueta,
                    color = if (activo) colorTextoActivo else colorTextoInactivo,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
