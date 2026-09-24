package com.minkaart2.app.ui.sales

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.minkaart2.app.data.EscalaValoracion
import com.minkaart2.app.navigation.Routes
import com.minkaart2.app.ui.components.GradientButton
import com.minkaart2.app.viewModel.RateSaleViewModel

private val Guinda = Color(0xFF3B0F1A)
private val Borgona = Color(0xFF66172E)
private val AzulClaro = Color(0xFFB7C7D9)
private val AzulAcero = Color(0xFF55708E)
private val Marfil = Color(0xFFF2EDE6)
private val TextoSecundario = Color(0xFF8A7480)
private val LineSoft = Color(0xFFEFE6E8)

/**
 * "Valora tu compra" (Fase 6) — sin ningún campo de texto, solo dos grillas de pills de
 * selección única. Valida que la venta sea del usuario actual y que no esté valorada ya;
 * si no, redirige a `ProductDetailScreen` en vez de mostrar el formulario de nuevo.
 */
@Composable
fun RateSaleScreen(
    ventaId: String,
    navController: NavController
) {

    val viewModel: RateSaleViewModel = viewModel()
    val context = LocalContext.current

    val venta by viewModel.venta
    val producto by viewModel.producto
    val nombreArtesano by viewModel.nombreArtesano
    val cargando by viewModel.cargando

    var calidadSeleccionada by remember { mutableStateOf<String?>(null) }
    var confianzaSeleccionada by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(ventaId) {

        viewModel.cargarVenta(
            ventaId = ventaId,

            onYaValorada = { productoId ->

                navController.navigate(Routes.productDetailRoute(productoId)) {
                    popUpTo(Routes.RATE_SALE) { inclusive = true }
                }
            },

            onError = { mensaje ->
                Toast.makeText(context, mensaje, Toast.LENGTH_LONG).show()
                navController.popBackStack()
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
                .background(Color.White)
                .padding(16.dp, 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Marfil)
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
                text = "Valora tu compra",
                color = Guinda,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 12.dp)
            )
        }

        if (cargando || venta == null || producto == null) {

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(color = Borgona)
            }

        } else {

            val productoActual = producto!!

            Column(modifier = Modifier.padding(16.dp)) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Marfil, RoundedCornerShape(14.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(AzulClaro)
                    ) {

                        if (productoActual.imagenes.isNotEmpty()) {

                            AsyncImage(
                                model = productoActual.imagenes.first(),
                                contentDescription = productoActual.nombre,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    Column(modifier = Modifier.padding(start = 10.dp)) {

                        Text(
                            text = productoActual.nombre,
                            color = Guinda,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = nombreArtesano.ifBlank { "Artesano" },
                            color = TextoSecundario,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Text(
                    text = "Calidad del producto",
                    color = Guinda,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(top = 18.dp, bottom = 10.dp)
                )

                RatePillGrid(
                    opciones = EscalaValoracion.NIVELES_CALIDAD,
                    seleccionado = calidadSeleccionada,
                    onSeleccionar = { calidadSeleccionada = it }
                )

                Text(
                    text = "Confianza en el artesano",
                    color = Guinda,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(top = 14.dp, bottom = 10.dp)
                )

                RatePillGrid(
                    opciones = EscalaValoracion.NIVELES_CONFIANZA,
                    seleccionado = confianzaSeleccionada,
                    onSeleccionar = { confianzaSeleccionada = it }
                )

                Spacer(modifier = Modifier.height(20.dp))

                GradientButton(
                    text = "Enviar valoración",
                    enabled = calidadSeleccionada != null && confianzaSeleccionada != null,
                    onClick = {

                        viewModel.enviarValoracion(
                            calidad = calidadSeleccionada ?: return@GradientButton,
                            confianza = confianzaSeleccionada ?: return@GradientButton,

                            onSuccess = {

                                Toast.makeText(
                                    context,
                                    "¡Gracias por tu valoración!",
                                    Toast.LENGTH_SHORT
                                ).show()

                                navController.popBackStack()
                            },

                            onError = { mensaje ->
                                Toast.makeText(context, mensaje, Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun RatePillGrid(
    opciones: List<String>,
    seleccionado: String?,
    onSeleccionar: (String) -> Unit
) {

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.height(100.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        items(opciones) { opcion ->

            val estaSeleccionado = opcion == seleccionado

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .then(
                        if (estaSeleccionado) {
                            Modifier.background(
                                Brush.horizontalGradient(listOf(AzulAcero, Borgona))
                            )
                        } else {
                            Modifier
                                .background(Color.White)
                                .border(1.4.dp, LineSoft, RoundedCornerShape(12.dp))
                        }
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onSeleccionar(opcion) }
                    )
                    .padding(vertical = 12.dp, horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = opcion,
                    color = if (estaSeleccionado) Color.White else TextoSecundario,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
