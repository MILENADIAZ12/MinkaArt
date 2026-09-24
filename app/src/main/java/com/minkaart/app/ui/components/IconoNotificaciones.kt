package com.minkaart2.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val Guinda = Color(0xFF3B0F1A)
private val Borgona = Color(0xFF66172E)
private val AzulClaro = Color(0xFFB7C7D9)

/**
 * Campana de notificaciones compartida entre `HomeClienteScreen` y `HomeArtesanoScreen`
 * — mismo tamaño y forma (30.dp, círculo `AzulClaro`) que el avatar de perfil del
 * encabezado del artesano, para que la pareja de íconos quede alineada. Muestra un punto
 * `Borgona` en la esquina superior derecha mientras haya notificaciones sin leer.
 */
@Composable
fun IconoNotificaciones(noLeidas: Int, onClick: () -> Unit) {

    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(AzulClaro)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {

        Icon(
            imageVector = Icons.Filled.Notifications,
            contentDescription = "Notificaciones",
            tint = Guinda,
            modifier = Modifier.size(16.dp)
        )

        if (noLeidas > 0) {

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(Borgona)
            )
        }
    }
}
