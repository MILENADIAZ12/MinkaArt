package com.minkaart2.app.ui.ruac

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.minkaart2.app.viewModel.RuacBadgeViewModel
import java.text.SimpleDateFormat
import java.util.Locale

private val Guinda = Color(0xFF3B0F1A)
private val Borgona = Color(0xFF66172E)
private val Marfil = Color(0xFFF2EDE6)
private val TextoSecundario = Color(0xFF8A7480)
private val Dorado = Color(0xFFA8770F)

/**
 * Reemplaza el `PlaceholderScreen` que tenía `Routes.RUAC_BADGE` — solo muestra datos
 * reales si `ruacEstado == "verificado"`; si alguien entra sin estarlo, muestra un estado
 * vacío en vez de datos incompletos (pedido explícito de la Fase 5).
 */
@Composable
fun RuacBadgeScreen(
    artesanoId: String,
    navController: NavController
) {

    val viewModel: RuacBadgeViewModel = viewModel()
    val context = LocalContext.current

    val artesano by viewModel.artesano
    val cargando by viewModel.cargando

    LaunchedEffect(artesanoId) {

        viewModel.cargarInsignia(
            artesanoId = artesanoId,
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
                text = "Insignia RUAC",
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

        } else if (artesano?.ruacEstado != "verificado") {

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Este artesano no tiene una insignia RUAC verificada",
                    color = TextoSecundario,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }

        } else {

            val datos = artesano!!

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFDF1D8)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = Dorado,
                        modifier = Modifier.size(34.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "RUAC verificado",
                    color = Guinda,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = datos.nombre.ifBlank { "Artesano" },
                    color = TextoSecundario,
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(modifier = Modifier.height(20.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White)
                        .padding(16.dp)
                ) {

                    CampoInsignia(etiqueta = "Número de registro", valor = datos.ruacRegistro)
                    CampoInsignia(etiqueta = "Región", valor = datos.ruacRegion)
                    CampoInsignia(etiqueta = "Técnica registrada", valor = datos.ruacTecnica)
                    CampoInsignia(
                        etiqueta = "Verificado desde",
                        valor = formatearFecha(datos.ruacVerificadoEn?.toDate())
                    )
                    CampoInsignia(
                        etiqueta = "Última validación",
                        valor = formatearFecha(datos.ruacUltimaValidacion?.toDate()),
                        ultimo = true
                    )
                }
            }
        }
    }
}

@Composable
private fun CampoInsignia(
    etiqueta: String,
    valor: String?,
    ultimo: Boolean = false
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = if (ultimo) 0.dp else 10.dp)
    ) {

        Text(
            text = etiqueta.uppercase(),
            color = Borgona,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = valor?.ifBlank { null } ?: "No especificado",
            color = Guinda,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

private fun formatearFecha(fecha: java.util.Date?): String {

    if (fecha == null) return "No especificado"
    val formato = SimpleDateFormat("dd/MM/yyyy", Locale("es", "CO"))
    return formato.format(fecha)
}
