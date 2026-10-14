package com.minkaart2.app.ui.admin

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.minkaart2.app.data.model.User
import com.minkaart2.app.navigation.Routes
import com.minkaart2.app.ui.components.LogoMinkaEncabezado
import com.minkaart2.app.viewModel.AdminRuacListViewModel
import java.text.SimpleDateFormat
import java.util.Locale

private val Guinda = Color(0xFF3B0F1A)
private val Borgona = Color(0xFF66172E)
private val Marfil = Color(0xFFF2EDE6)
private val TextoSecundario = Color(0xFF8A7480)
private val LineSoft = Color(0xFFEFE6E8)

/**
 * Único destino del rol "administrador" (Fase 5) — no tiene Home propio más allá de esto,
 * según lo pedido: "el administrador solo aprueba RUAC".
 */
@Composable
fun AdminRuacListScreen(navController: NavController) {

    val viewModel: AdminRuacListViewModel = viewModel()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val solicitudes by viewModel.solicitudes
    val cargando by viewModel.cargando

    LaunchedEffect(Unit) {

        viewModel.cargarSolicitudes(
            onError = { mensaje ->
                Toast.makeText(context, mensaje, Toast.LENGTH_LONG).show()
            }
        )
    }

    // Refresca al volver de Revisar (aprobar/rechazar quita la solicitud de esta lista),
    // mismo patrón que MyProductsScreen.
    DisposableEffect(lifecycleOwner) {

        val observer = LifecycleEventObserver { _, event ->

            if (event == Lifecycle.Event.ON_RESUME) {

                viewModel.cargarSolicitudes(
                    onError = { /* si falla el refresco silencioso, se deja la lista tal cual */ }
                )
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
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
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            LogoMinkaEncabezado(
                titulo = "Solicitudes de RUAC",
                colorTitulo = Guinda,
                colorEslogan = Borgona
            )

            // Herramienta aparte para el documento de grado (ver README, "Evaluación") —
            // no genera ni toca huellas visuales, el administrador no participa en el
            // reconocimiento en sí: eso ya es automático al publicar/editar y con el
            // autocompletado silencioso del artesano (ver GeneradorHuellas).
            Text(
                text = "Evaluar búsqueda visual",
                color = Borgona,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { navController.navigate(Routes.ADMIN_VISUAL_EVAL) }
                )
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(LineSoft)
        )

        if (cargando) {

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(color = Borgona)
            }

        } else if (solicitudes.isEmpty()) {

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "No hay solicitudes de RUAC pendientes",
                    color = TextoSecundario,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

        } else {

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                items(
                    items = solicitudes,
                    key = { it.uid }
                ) { artesano ->

                    SolicitudRow(
                        artesano = artesano,
                        onClick = {
                            navController.navigate(Routes.adminRuacReviewRoute(artesano.uid))
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SolicitudRow(
    artesano: User,
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
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFFB7C7D9))
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {

            Text(
                text = artesano.nombre.ifBlank { "Artesano" },
                color = Guinda,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = artesano.ruacTecnica?.ifBlank { null } ?: "Técnica no especificada",
                color = TextoSecundario,
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                text = "Solicitado el ${formatearFecha(artesano)}",
                color = TextoSecundario.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodySmall
            )
        }

        Icon(
            imageVector = Icons.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = TextoSecundario
        )
    }
}

private fun formatearFecha(artesano: User): String {

    val fecha = artesano.ruacSolicitadoEn?.toDate() ?: return "fecha desconocida"
    val formato = SimpleDateFormat("dd/MM/yyyy", Locale("es", "CO"))
    return formato.format(fecha)
}
