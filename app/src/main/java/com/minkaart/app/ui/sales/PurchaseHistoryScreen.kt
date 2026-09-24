package com.minkaart2.app.ui.sales

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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.minkaart2.app.data.model.Venta
import com.minkaart2.app.navigation.Routes
import com.minkaart2.app.viewModel.PurchaseHistoryViewModel
import java.text.SimpleDateFormat
import java.util.Locale

private val Guinda = Color(0xFF3B0F1A)
private val Borgona = Color(0xFF66172E)
private val AzulAcero = Color(0xFF55708E)
private val Marfil = Color(0xFFF2EDE6)
private val TextoSecundario = Color(0xFF8A7480)
private val LineSoft = Color(0xFFEFE6E8)

/**
 * Reemplaza el `PlaceholderScreen` que tenía `Routes.PURCHASE_HISTORY` (Fase 6). Cada
 * fila decide su propio destino: pendiente de valorar → `RateSaleScreen`; ya valorada →
 * `ProductDetailScreen` (no tiene sentido volver a abrir el formulario).
 */
@Composable
fun PurchaseHistoryScreen(navController: NavController) {

    val viewModel: PurchaseHistoryViewModel = viewModel()
    val context = LocalContext.current

    val ventas by viewModel.ventas
    val productos by viewModel.productos
    val nombresArtesanos by viewModel.nombresArtesanos
    val cargando by viewModel.cargando

    LaunchedEffect(Unit) {

        viewModel.cargarHistorial(
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
                text = "Historial de compras",
                color = Guinda,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 12.dp)
            )
        }

        if (cargando) {

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(color = Borgona)
            }

        } else if (ventas.isEmpty()) {

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Todavía no tienes compras registradas",
                    color = TextoSecundario,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }

        } else {

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                items(
                    items = ventas,
                    key = { it.id }
                ) { venta ->

                    val producto = productos[venta.productoId]

                    VentaRow(
                        venta = venta,
                        nombreProducto = producto?.nombre ?: "Producto",
                        imagenProducto = producto?.imagenes?.firstOrNull(),
                        nombreArtesano = nombresArtesanos[venta.artesanoId] ?: "",
                        onClick = {

                            if (venta.valorado) {
                                navController.navigate(Routes.productDetailRoute(venta.productoId))
                            } else {
                                navController.navigate(Routes.rateSaleRoute(venta.id))
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun VentaRow(
    venta: Venta,
    nombreProducto: String,
    imagenProducto: String?,
    nombreArtesano: String,
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
                .size(46.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(Marfil)
        ) {

            if (!imagenProducto.isNullOrBlank()) {

                AsyncImage(
                    model = imagenProducto,
                    contentDescription = nombreProducto,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp)
        ) {

            Text(
                text = nombreProducto,
                color = Guinda,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (nombreArtesano.isNotBlank()) {

                Text(
                    text = nombreArtesano,
                    color = TextoSecundario,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = formatearFecha(venta),
                color = TextoSecundario.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodySmall
            )
        }

        EstadoBadge(valorado = venta.valorado)
    }
}

@Composable
private fun EstadoBadge(valorado: Boolean) {

    val fondo = if (valorado) Color(0xFFDFE7F0) else Color(0xFFF2DFE4)
    val texto = if (valorado) AzulAcero else Borgona

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(fondo)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = if (valorado) "Valorado" else "Pendiente de valorar",
            color = texto,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun formatearFecha(venta: Venta): String {

    val formato = SimpleDateFormat("dd/MM/yyyy", Locale("es", "CO"))
    return formato.format(venta.fechaVenta.toDate())
}
