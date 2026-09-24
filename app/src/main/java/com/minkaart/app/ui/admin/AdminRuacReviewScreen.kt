package com.minkaart2.app.ui.admin

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.minkaart2.app.ui.components.GradientButton
import com.minkaart2.app.ui.components.OutlineButton
import com.minkaart2.app.viewModel.AdminRuacReviewViewModel

private val Guinda = Color(0xFF3B0F1A)
private val Borgona = Color(0xFF66172E)
private val Marfil = Color(0xFFF2EDE6)
private val TextoSecundario = Color(0xFF8A7480)

@Composable
fun AdminRuacReviewScreen(
    artesanoId: String,
    navController: NavController
) {

    val viewModel: AdminRuacReviewViewModel = viewModel()
    val context = LocalContext.current

    val artesano by viewModel.artesano
    val cargando by viewModel.cargando

    LaunchedEffect(artesanoId) {

        viewModel.cargarArtesano(
            artesanoId = artesanoId,
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
                text = "Revisar solicitud de RUAC",
                color = Guinda,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 12.dp)
            )
        }

        if (cargando || artesano == null) {

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(color = Borgona)
            }

        } else {

            val datos = artesano!!

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {

                Text(
                    text = datos.nombre.ifBlank { "Artesano" },
                    color = Guinda,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(16.dp))

                CampoRuac(etiqueta = "Número de registro", valor = datos.ruacRegistro)
                CampoRuac(etiqueta = "Región", valor = datos.ruacRegion)
                CampoRuac(etiqueta = "Técnica registrada", valor = datos.ruacTecnica)

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "DOCUMENTO DE SOPORTE",
                    color = Borgona,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                val documentoUrl = datos.ruacDocumentoUrl

                if (documentoUrl.isNullOrBlank()) {

                    Text(
                        text = "No se adjuntó ningún documento",
                        color = TextoSecundario,
                        style = MaterialTheme.typography.bodySmall
                    )

                } else {

                    // Si el documento es un PDF, la vista previa no lo renderiza — misma
                    // limitación que ya tenía el RUAC del registro, no es nueva de esta
                    // pantalla. Igual queda como enlace de texto para poder abrirlo.
                    AsyncImage(
                        model = documentoUrl,
                        contentDescription = "Documento RUAC",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White),
                        contentScale = ContentScale.Fit
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {

                    OutlineButton(
                        text = "Rechazar",
                        icon = Icons.Filled.Close,
                        borderColor = Borgona,
                        textColor = Borgona,
                        modifier = Modifier.weight(1f),
                        onClick = {

                            viewModel.rechazar(
                                artesanoId = artesanoId,
                                onSuccess = {

                                    Toast.makeText(
                                        context,
                                        "Solicitud rechazada",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    navController.popBackStack()
                                },
                                onError = { mensaje ->
                                    Toast.makeText(context, mensaje, Toast.LENGTH_LONG).show()
                                }
                            )
                        }
                    )

                    GradientButton(
                        text = "Aprobar",
                        modifier = Modifier.weight(1f),
                        onClick = {

                            viewModel.aprobar(
                                artesanoId = artesanoId,
                                onSuccess = {

                                    Toast.makeText(
                                        context,
                                        "RUAC verificado correctamente",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    navController.popBackStack()
                                },
                                onError = { mensaje ->
                                    Toast.makeText(context, mensaje, Toast.LENGTH_LONG).show()
                                }
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CampoRuac(etiqueta: String, valor: String?) {

    Column(modifier = Modifier.padding(bottom = 10.dp)) {

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
