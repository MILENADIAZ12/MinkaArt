package com.minkaart2.app.ui.ruac

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.minkaart2.app.ui.components.GradientButton
import com.minkaart2.app.ui.components.MinkaFormField
import com.minkaart2.app.ui.components.OutlineButton
import com.minkaart2.app.viewModel.RequestRuacViewModel

private val Guinda = Color(0xFF3B0F1A)
private val Borgona = Color(0xFF66172E)
private val Marfil = Color(0xFFF2EDE6)
private val LineSoft = Color(0xFFEFE6E8)

/**
 * Formulario de RUAC — sirve tanto para "solicitar por primera vez" (todo vacío) como para
 * "Editar RUAC" desde un estado ya verificado/rechazado (ajuste posterior a la Fase 5):
 * como siempre es el RUAC del usuario actual (sin argumento de ruta), simplemente carga
 * sus datos existentes al entrar y los precarga si ya hay algo. El documento solo es
 * obligatorio si no había uno guardado — si el artesano no adjunta uno nuevo, se conserva
 * el que ya tenía (`RequestRuacViewModel.enviarSolicitud`).
 */
@Composable
fun RequestRuacScreen(navController: NavController) {

    var registro by remember { mutableStateOf("") }
    var region by remember { mutableStateOf("") }
    var tecnica by remember { mutableStateOf("") }
    var documentoUri by remember { mutableStateOf<Uri?>(null) }

    val viewModel: RequestRuacViewModel = viewModel()
    val context = LocalContext.current

    val usuarioActual by viewModel.usuarioActual
    val cargando by viewModel.cargando

    val esEdicion = usuarioActual?.ruacEstado != null &&
        usuarioActual?.ruacEstado != "no_solicitado"

    val selectorDocumento = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->

        if (uri != null) {
            documentoUri = uri
        }
    }

    LaunchedEffect(Unit) {

        viewModel.cargarDatosActuales(
            onError = { mensaje ->
                Toast.makeText(context, mensaje, Toast.LENGTH_LONG).show()
            }
        )
    }

    // Precarga los campos una sola vez, cuando llegan los datos del usuario — no vuelve a
    // dispararse mientras el usuario escribe, porque usuarioActual no cambia después de la
    // carga inicial.
    LaunchedEffect(usuarioActual) {

        val datos = usuarioActual

        if (datos != null) {
            registro = datos.ruacRegistro ?: ""
            region = datos.ruacRegion ?: ""
            tecnica = datos.ruacTecnica ?: ""
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
                text = if (esEdicion) "Editar RUAC" else "Solicitar RUAC",
                color = Guinda,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 12.dp)
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

        } else {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {

                Text(
                    text = "Estos datos se envían a revisión del administrador antes de " +
                            "activar tu insignia de RUAC verificado.",
                    color = Guinda.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                MinkaFormField(
                    label = "Número de registro RUAC",
                    value = registro,
                    onValueChange = { registro = it },
                    placeholder = "Ej. RUAC-2024-08842"
                )

                MinkaFormField(
                    label = "Región registrada",
                    value = region,
                    onValueChange = { region = it },
                    placeholder = "Ej: Bogotá, Boyacá, La Guajira"
                )

                MinkaFormField(
                    label = "Técnica registrada",
                    value = tecnica,
                    onValueChange = { tecnica = it },
                    placeholder = "Ej. Tejido en telar"
                )

                Spacer(modifier = Modifier.height(8.dp))

                val yaTieneDocumento = !usuarioActual?.ruacDocumentoUrl.isNullOrBlank()

                OutlineButton(
                    text = when {
                        documentoUri != null -> "Cambiar documento de soporte"
                        yaTieneDocumento -> "Reemplazar documento de soporte"
                        else -> "Adjuntar documento o foto de soporte"
                    },
                    borderColor = Borgona,
                    textColor = Borgona,
                    onClick = {

                        selectorDocumento.launch(
                            arrayOf("application/pdf", "image/*")
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                when {

                    documentoUri != null -> {

                        Text(
                            text = "Documento nuevo seleccionado correctamente",
                            color = Guinda.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    yaTieneDocumento -> {

                        Text(
                            text = "Ya tienes un documento adjunto — no es necesario " +
                                    "volver a subirlo si no cambió",
                            color = Guinda.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                GradientButton(
                    text = "Enviar solicitud",
                    onClick = {

                        viewModel.enviarSolicitud(
                            registro = registro,
                            region = region,
                            tecnica = tecnica,
                            documentoUri = documentoUri,

                            onSuccess = {

                                Toast.makeText(
                                    context,
                                    "Solicitud de RUAC enviada, quedó en revisión",
                                    Toast.LENGTH_LONG
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

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
