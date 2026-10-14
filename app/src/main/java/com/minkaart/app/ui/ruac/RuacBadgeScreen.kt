package com.minkaart2.app.ui.ruac

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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.ThumbUp
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.minkaart2.app.R
import com.minkaart2.app.ui.components.PatternBackground
import com.minkaart2.app.ui.theme.AzulAcero
import com.minkaart2.app.ui.theme.AzulClaro
import com.minkaart2.app.ui.theme.Borgona
import com.minkaart2.app.ui.theme.Dorado
import com.minkaart2.app.ui.theme.FondoCampo
import com.minkaart2.app.ui.theme.Guinda
import com.minkaart2.app.ui.theme.Marfil
import com.minkaart2.app.ui.theme.TextoPlaceholder
import com.minkaart2.app.viewModel.RuacBadgeViewModel
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Insignia RUAC en tres bloques verticales: encabezado con el patrón de rombos, datos del
 * registro en una tarjeta superpuesta y "¿Por qué es importante?". Solo muestra datos
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

    if (cargando) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Marfil),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(color = Borgona)
        }

        return
    }

    val datos = artesano

    if (datos == null || datos.ruacEstado != "verificado") {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Marfil)
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {

            BotonAtras(
                onClick = { navController.popBackStack() },
                modifier = Modifier.padding(16.dp)
            )

            Text(
                text = stringResource(R.string.ruac_no_verificada),
                color = TextoPlaceholder,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 32.dp)
            )
        }

        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Marfil)
            .verticalScroll(rememberScrollState())
    ) {

        // Bloque 1 — encabezado con el patrón de rombos y una capa vinotinto translúcida
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
        ) {

            PatternBackground(
                modifier = Modifier.fillMaxSize(),
                gradientColors = listOf(Guinda, Borgona),
                patternAlpha = 1f
            ) {}

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Guinda.copy(alpha = 0.78f))
            )

            BotonAtras(
                onClick = { navController.popBackStack() },
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(16.dp),
                vidrio = true
            )

            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(top = 24.dp, start = 24.dp, end = 24.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(Marfil),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = Dorado,
                        modifier = Modifier.size(52.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = datos.nombre.ifBlank { "Artesano" },
                    color = Marfil,
                    fontFamily = FontFamily.Serif,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Marfil.copy(alpha = 0.14f))
                        .border(1.dp, Marfil.copy(alpha = 0.5f), CircleShape)
                        .padding(start = 10.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = Marfil,
                        modifier = Modifier.size(16.dp)
                    )

                    Text(
                        text = stringResource(R.string.ruac_verificado),
                        color = Marfil,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
            }
        }

        // Bloques 2 y 3 suben 28.dp para que la tarjeta se superponga al encabezado.
        Column(modifier = Modifier.offset(y = (-28).dp)) {

            // Bloque 2 — información del registro
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(FondoCampo)
                    .padding(horizontal = 24.dp)
                    .padding(top = 22.dp, bottom = 8.dp)
            ) {

                Text(
                    text = stringResource(R.string.ruac_info_registro),
                    color = Guinda,
                    fontFamily = FontFamily.Serif,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                val noEspecificado = stringResource(R.string.no_especificado)

                FilaRegistro(
                    stringResource(R.string.ruac_registro),
                    datos.ruacRegistro?.ifBlank { null } ?: noEspecificado
                )
                FilaRegistro(
                    stringResource(R.string.ruac_region),
                    datos.ruacRegion?.ifBlank { null } ?: noEspecificado
                )
                FilaRegistro(
                    stringResource(R.string.ruac_tecnica),
                    datos.ruacTecnica?.ifBlank { null } ?: noEspecificado
                )
                FilaRegistro(
                    stringResource(R.string.ruac_verificado_desde),
                    formatearFecha(datos.ruacVerificadoEn?.toDate(), noEspecificado)
                )
                FilaRegistro(
                    stringResource(R.string.ruac_ultima_validacion),
                    formatearFecha(datos.ruacUltimaValidacion?.toDate(), noEspecificado),
                    ultima = true
                )
            }

            // Bloque 3 — ¿Por qué es importante?
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 24.dp, bottom = 32.dp)
            ) {

                Text(
                    text = stringResource(R.string.ruac_por_que),
                    color = Guinda,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(AzulClaro.copy(alpha = 0.3f))
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {

                    PuntoImportante(
                        icono = Icons.Filled.CheckCircle,
                        titulo = stringResource(R.string.ruac_autenticidad),
                        texto = stringResource(R.string.ruac_autenticidad_desc)
                    )

                    PuntoImportante(
                        icono = Icons.Filled.ThumbUp,
                        titulo = stringResource(R.string.ruac_confianza),
                        texto = stringResource(R.string.ruac_confianza_desc)
                    )

                    PuntoImportante(
                        icono = Icons.Filled.Favorite,
                        titulo = stringResource(R.string.ruac_comercio_justo),
                        texto = stringResource(R.string.ruac_comercio_justo_desc)
                    )
                }

                Text(
                    text = stringResource(R.string.ruac_nota_admin),
                    color = TextoPlaceholder,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 4.dp, top = 14.dp)
                )
            }
        }
    }
}

/** Botón "atrás"; con [vidrio] se dibuja como tarjeta de vidrio sobre el encabezado. */
@Composable
private fun BotonAtras(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    vidrio: Boolean = false
) {

    val forma = RoundedCornerShape(14.dp)

    Box(
        modifier = modifier
            .size(44.dp)
            .clip(forma)
            .background(if (vidrio) Marfil.copy(alpha = 0.18f) else Color.White)
            .then(
                if (vidrio) Modifier.border(1.dp, Marfil.copy(alpha = 0.35f), forma)
                else Modifier
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.volver),
            tint = if (vidrio) Marfil else Guinda,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun FilaRegistro(
    etiqueta: String,
    valor: String,
    ultima: Boolean = false
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            text = etiqueta,
            color = AzulAcero,
            style = MaterialTheme.typography.bodySmall
        )

        Text(
            text = valor,
            color = Guinda,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
            modifier = Modifier.padding(start = 16.dp)
        )
    }

    if (!ultima) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Guinda.copy(alpha = 0.08f))
        )
    }
}

@Composable
private fun PuntoImportante(
    icono: ImageVector,
    titulo: String,
    texto: String
) {

    Row(verticalAlignment = Alignment.Top) {

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Guinda),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icono,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        Column(modifier = Modifier.padding(start = 14.dp)) {

            Text(
                text = titulo,
                color = Guinda,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = texto,
                color = Guinda,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

private fun formatearFecha(fecha: java.util.Date?, sinDato: String): String {

    if (fecha == null) return sinDato
    val formato = SimpleDateFormat("dd/MM/yyyy", Locale("es", "CO"))
    return formato.format(fecha)
}
