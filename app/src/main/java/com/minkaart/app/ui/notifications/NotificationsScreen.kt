package com.minkaart2.app.ui.notifications

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.minkaart2.app.data.model.Notificacion
import com.minkaart2.app.navigation.Routes
import com.minkaart2.app.viewModel.NotificationsViewModel
import java.text.SimpleDateFormat
import java.util.Locale

private val Guinda = Color(0xFF3B0F1A)
private val Borgona = Color(0xFF66172E)
private val AzulAcero = Color(0xFF55708E)
private val AzulClaro = Color(0xFFB7C7D9)
private val Marfil = Color(0xFFF2EDE6)
private val TextoSecundario = Color(0xFF8A7480)
private val LineSoft = Color(0xFFEFE6E8)

/**
 * Centro de notificaciones abierto desde la campana de `HomeClienteScreen`/
 * `HomeArtesanoScreen`. Estructura calcada de `PurchaseHistoryScreen` (encabezado con
 * flecha de volver, estados de carga/vacío, filas de tarjeta) — mismo criterio de reuso
 * de patrones visuales del proyecto.
 */
@Composable
fun NotificationsScreen(navController: NavController) {

    val viewModel: NotificationsViewModel = viewModel()
    val context = LocalContext.current

    val notificaciones by viewModel.notificaciones
    val noLeidas by viewModel.noLeidas
    val cargando by viewModel.cargando

    LaunchedEffect(Unit) {

        viewModel.cargar(
            onError = { mensaje ->
                Toast.makeText(context, mensaje, Toast.LENGTH_LONG).show()
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Marfil)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp, 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { navController.popBackStack() }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = Guinda,
                    modifier = Modifier.size(14.dp)
                )
            }

            Text(
                text = "Notificaciones",
                color = Guinda,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .padding(start = 12.dp)
                    .weight(1f)
            )

            if (noLeidas > 0) {

                Text(
                    text = "Marcar todas",
                    color = AzulAcero,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {

                            viewModel.marcarTodasLeidas(
                                onError = { mensaje ->
                                    Toast.makeText(context, mensaje, Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    )
                )
            }
        }

        if (cargando) {

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(color = Borgona)
            }

        } else if (notificaciones.isEmpty()) {

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.Notifications,
                    contentDescription = null,
                    tint = TextoSecundario,
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    text = "Todavía no tienes notificaciones",
                    color = TextoSecundario,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 10.dp, start = 32.dp, end = 32.dp)
                )
            }

        } else {

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                items(
                    items = notificaciones,
                    key = { it.id }
                ) { notificacion ->

                    NotificacionRow(
                        notificacion = notificacion,
                        onClick = {

                            viewModel.marcarLeida(
                                notificacionId = notificacion.id,
                                onError = { /* la fila sigue mostrándose como no leída, se reintenta al volver a abrir */ }
                            )

                            val ruta = Routes.rutaDeNotificacion(
                                tipo = notificacion.tipo,
                                referenciaId = notificacion.referenciaId
                            )

                            if (ruta != null) {
                                navController.navigate(ruta)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificacionRow(
    notificacion: Notificacion,
    onClick: () -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(13.dp))
            .border(1.dp, LineSoft, RoundedCornerShape(13.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(AzulClaro),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = iconoDeTipo(notificacion.tipo),
                contentDescription = null,
                tint = Guinda,
                modifier = Modifier.size(16.dp)
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp)
        ) {

            Text(
                text = notificacion.titulo,
                color = Guinda,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = notificacion.mensaje,
                color = TextoSecundario,
                style = MaterialTheme.typography.bodySmall
            )

            notificacion.creadoEn?.let {

                Text(
                    text = formatearFecha(it.toDate()),
                    color = TextoSecundario.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        if (!notificacion.leida) {

            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(Borgona)
            )
        }
    }
}

private fun iconoDeTipo(tipo: String): ImageVector = when (tipo) {
    "venta" -> Icons.Filled.ShoppingCart
    "valoracion" -> Icons.Filled.Star
    "like" -> Icons.Filled.Favorite
    "ruac_aprobado", "ruac_rechazado" -> Icons.Filled.CheckCircle
    else -> Icons.Filled.Notifications
}

private fun formatearFecha(fecha: java.util.Date): String {

    val formato = SimpleDateFormat("dd/MM/yyyy", Locale("es", "CO"))
    return formato.format(fecha)
}
