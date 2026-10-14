package com.minkaart2.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minkaart2.app.ui.theme.AzulClaro
import com.minkaart2.app.ui.theme.Guinda
import com.minkaart2.app.ui.theme.Marfil

/**
 * Ícono de acción de Minka Art (notificaciones, carrito, mensajes, perfil, filtro): círculo
 * RELLENO vinotinto con el pictograma en blanco. Si hay [novedades] muestra el punto de
 * novedad: círculo `AzulClaro` de 10.dp con borde de 2.dp en `Marfil`, centrado sobre el
 * borde del círculo en su esquina superior derecha (la mitad queda por fuera). Con más de
 * una novedad el punto crece a una píldora con el número (máx. "9+").
 */
@Composable
fun IconoAccion(
    painter: Painter,
    descripcion: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    novedades: Int = 0,
    tamano: Dp = 30.dp
) {

    Box(
        modifier = modifier.size(tamano),
        contentAlignment = Alignment.Center
    ) {

        Box(
            modifier = Modifier
                .size(tamano)
                .clip(CircleShape)
                .background(Guinda)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {

            Icon(
                painter = painter,
                contentDescription = descripcion,
                tint = Color.White,
                modifier = Modifier.size(tamano * 0.53f)
            )
        }

        if (novedades > 0) {

            // Centro del punto = punto del borde del círculo a 45° (arriba a la derecha):
            // x = r + 0,707r, y = r − 0,707r (r = tamano / 2), medido desde la esquina
            // superior izquierda.
            val radio = tamano / 2
            val centroX = radio + radio * 0.707f
            val centroY = radio - radio * 0.707f

            if (novedades == 1) {

                val diametro = 14.dp // 10.dp de punto + 2.dp de borde por lado

                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = centroX - diametro / 2, y = centroY - diametro / 2)
                        .size(diametro)
                        .clip(CircleShape)
                        .background(Marfil)
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(AzulClaro)
                )

            } else {

                val alto = 18.dp

                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = centroX - alto / 2, y = centroY - alto / 2)
                        .height(alto)
                        .defaultMinSize(minWidth = alto)
                        .clip(CircleShape)
                        .background(AzulClaro)
                        .border(2.dp, Marfil, CircleShape)
                        .padding(horizontal = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (novedades > 9) "9+" else novedades.toString(),
                        color = Guinda,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun IconoAccion(
    icono: ImageVector,
    descripcion: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    novedades: Int = 0,
    tamano: Dp = 30.dp
) {
    IconoAccion(
        painter = rememberVectorPainter(icono),
        descripcion = descripcion,
        onClick = onClick,
        modifier = modifier,
        novedades = novedades,
        tamano = tamano
    )
}

/**
 * Campana de notificaciones compartida entre `HomeClienteScreen` y `HomeArtesanoScreen`
 * — mismo tamaño (30.dp) que el avatar de perfil del encabezado del artesano, para que la
 * pareja de íconos quede alineada.
 */
@Composable
fun IconoNotificaciones(noLeidas: Int, onClick: () -> Unit) {

    IconoAccion(
        icono = Icons.Filled.Notifications,
        descripcion = "Notificaciones",
        onClick = onClick,
        novedades = noLeidas
    )
}
