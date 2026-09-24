package com.minkaart2.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val Borgona = Color(0xFF66172E)
private val ColorInactivo = Color(0xFFC9B8BD)

/**
 * Un ítem de la barra de navegación inferior — ícono + etiqueta, resaltado si está activo.
 */
data class BottomNavItemData(
    val icon: ImageVector,
    val label: String,
    val activo: Boolean,
    val onClick: () -> Unit
)

/**
 * Barra de navegación inferior compartida entre `HomeClienteScreen` y `HomeArtesanoScreen`
 * — antes vivía como `Row` + `private fun BottomNavItem` duplicados dentro de
 * `HomeClienteScreen.kt`. Aplica su propio inset inferior (`WindowInsets.navigationBars`)
 * *después* del fondo blanco, así el fondo llega hasta el borde real de la pantalla (barra
 * de gestos/3 botones) y solo el contenido (íconos/etiquetas) queda por encima de esa zona
 * — antes la pantalla que la usaba reservaba ese espacio con `windowInsetsPadding` en su
 * contenedor raíz, lo que empujaba toda la barra (fondo incluido) lejos del borde y dejaba
 * una franja del color de fondo de la pantalla debajo de la barra blanca. Por eso la
 * pantalla que use esta barra debe reservar solo el inset **superior**
 * (`WindowInsets.safeDrawing.only(WindowInsetsSides.Top)`), no el inferior — esta barra ya
 * lo cubre.
 */
@Composable
fun BottomNavBar(items: List<BottomNavItemData>) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {

        items.forEach { item ->
            BottomNavItem(
                icon = item.icon,
                label = item.label,
                activo = item.activo,
                onClick = item.onClick
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    icon: ImageVector,
    label: String,
    activo: Boolean,
    onClick: () -> Unit
) {

    Column(
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (activo) Borgona else ColorInactivo,
            modifier = Modifier.size(20.dp)
        )

        Text(
            text = label,
            color = if (activo) Borgona else ColorInactivo,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (activo) FontWeight.Bold else FontWeight.Normal
        )
    }
}
