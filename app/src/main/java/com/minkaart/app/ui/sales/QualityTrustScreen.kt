package com.minkaart2.app.ui.sales

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.minkaart2.app.data.EscalaValoracion
import com.minkaart2.app.viewModel.QualityTrustViewModel

private val Guinda = Color(0xFF3B0F1A)
private val Borgona = Color(0xFF66172E)
private val AzulAcero = Color(0xFF55708E)
private val Marfil = Color(0xFFF2EDE6)
private val TextoSecundario = Color(0xFF8A7480)
private val PistaBarra = Color(0xFFEFE0E3)

/**
 * "Calidad y confianza" (Fase 6): calidad es del producto (`producto.calidadPromedio`),
 * confianza es del artesano dueño de ese producto (`artesanoStats/{artesanoId}`) — no del
 * producto (ver la corrección de la Fase 6 en el modelo de datos).
 */
@Composable
fun QualityTrustScreen(
    productId: String,
    navController: NavController
) {

    val viewModel: QualityTrustViewModel = viewModel()
    val context = LocalContext.current

    val producto by viewModel.producto
    val statsArtesano by viewModel.statsArtesano
    val cargando by viewModel.cargando

    LaunchedEffect(productId) {

        viewModel.cargarDatos(
            productoId = productId,
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
                text = "Calidad y confianza",
                color = Guinda,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 12.dp)
            )
        }

        if (cargando || producto == null) {

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

                TarjetaVerificacion(
                    titulo = "Calidad",
                    conteo = productoActual.calidadConteo,
                    promedio = productoActual.calidadPromedio,
                    niveles = EscalaValoracion.NIVELES_CALIDAD
                )

                TarjetaVerificacion(
                    titulo = "Confianza",
                    conteo = statsArtesano.confianzaConteo,
                    promedio = statsArtesano.confianzaPromedio,
                    niveles = EscalaValoracion.NIVELES_CONFIANZA
                )
            }
        }
    }
}

@Composable
private fun TarjetaVerificacion(
    titulo: String,
    conteo: Int,
    promedio: Double?,
    niveles: List<String>
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Marfil)
            .padding(14.dp)
    ) {

        Row(verticalAlignment = Alignment.CenterVertically) {

            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = AzulAcero,
                modifier = Modifier.size(16.dp)
            )

            Text(
                text = titulo,
                color = Guinda,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 7.dp)
            )
        }

        if (conteo == 0 || promedio == null) {

            Text(
                text = "Aún no hay valoraciones",
                color = TextoSecundario,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 10.dp)
            )

        } else {

            Text(
                text = textoVerificadoPor(conteo),
                color = Color(0xFF7A6870),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 10.dp, bottom = 10.dp)
            )

            val porcentaje = (promedio / 4.0).coerceIn(0.0, 1.0)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(PistaBarra)
            ) {

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(porcentaje.toFloat())
                        .clip(RoundedCornerShape(4.dp))
                        .background(Brush.horizontalGradient(listOf(AzulAcero, Borgona)))
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Nivel: ${EscalaValoracion.etiqueta(promedio, niveles)}",
                color = TextoSecundario,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

private fun textoVerificadoPor(conteo: Int): String {

    val plural = if (conteo == 1) "comprador" else "compradores"
    return "Verificada por $conteo $plural"
}
