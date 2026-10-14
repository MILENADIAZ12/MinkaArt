package com.minkaart2.app.ui.product

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.minkaart2.app.R
import com.minkaart2.app.navigation.Routes
import com.minkaart2.app.ui.components.BarraValoracion
import com.minkaart2.app.ui.components.GradientButton
import com.minkaart2.app.util.etiquetaCategoria
import com.minkaart2.app.viewModel.ProductDetailViewModel
import java.net.URLEncoder

private val Guinda = Color(0xFF3B0F1A)
private val Borgona = Color(0xFF66172E)
private val AzulAcero = Color(0xFF55708E)
private val Marfil = Color(0xFFF2EDE6)
private val TextoSecundario = Color(0xFF7A6870)

@Composable
fun ProductDetailScreen(
    productId: String,
    navController: NavController
) {

    val viewModel: ProductDetailViewModel = viewModel()
    val context = LocalContext.current

    val producto by viewModel.producto
    val artesano by viewModel.artesano
    val statsArtesano by viewModel.statsArtesano
    val yaLeDioLike by viewModel.yaLeDioLike
    val cargando by viewModel.cargando

    LaunchedEffect(productId) {

        viewModel.cargarDetalle(
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
            .background(Color.White)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {

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

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {

                var visorAbierto by remember { mutableStateOf(false) }
                val pagerState = rememberPagerState(
                    pageCount = { productoActual.imagenes.size.coerceAtLeast(1) }
                )

                // Imagen grande (carrusel si hay varias) + corazón + botón de volver
                Box(modifier = Modifier.fillMaxWidth()) {

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(230.dp)
                            .background(Marfil)
                    ) {

                        if (productoActual.imagenes.isNotEmpty()) {

                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxSize()
                            ) { pagina ->

                                AsyncImage(
                                    model = productoActual.imagenes[pagina],
                                    contentDescription = productoActual.nombre,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                            onClick = { visorAbierto = true }
                                        ),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            if (productoActual.imagenes.size > 1) {

                                Row(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {

                                    repeat(productoActual.imagenes.size) { indice ->

                                        val activo = indice == pagerState.currentPage

                                        Box(
                                            modifier = Modifier
                                                .size(if (activo) 7.dp else 5.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (activo) Color.White else Color.White.copy(alpha = 0.5f)
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .padding(12.dp)
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.9f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { navController.popBackStack() }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.volver),
                            tint = Guinda,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.92f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {

                                    viewModel.alternarLike(
                                        productoId = productId,
                                        onError = { mensaje ->
                                            Toast.makeText(context, mensaje, Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (yaLeDioLike) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = stringResource(R.string.me_gusta),
                            tint = Borgona,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                if (visorAbierto && productoActual.imagenes.isNotEmpty()) {

                    VisorImagenesDialog(
                        imagenes = productoActual.imagenes,
                        paginaInicial = pagerState.currentPage,
                        onDismiss = { visorAbierto = false }
                    )
                }

                Column(modifier = Modifier.padding(16.dp)) {

                    Text(
                        text = productoActual.nombre,
                        color = Guinda,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = formatearPrecio(productoActual.precio),
                        color = Borgona,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                    )

                    if (productoActual.categoria.isNotBlank()) {

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Marfil)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = etiquetaCategoria(productoActual.categoria),
                                color = AzulAcero,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    SeccionTexto(titulo = stringResource(R.string.detalle_descripcion), texto = productoActual.descripcion)
                    SeccionTexto(titulo = stringResource(R.string.detalle_historia), texto = productoActual.historiaProceso)
                    SeccionTexto(titulo = stringResource(R.string.detalle_region), texto = productoActual.regionOrigen)

                    // Fila del artesano — el contacto público (editable por el propio
                    // artesano desde su perfil) tiene prioridad; si no lo configuró, se
                    // cae al teléfono de registro para no romper el flujo de contacto.
                    val datosArtesano = artesano
                    val contactoMostrado = datosArtesano
                        ?.contactoPublico
                        ?.ifBlank { null }
                        ?: datosArtesano?.telefono
                        ?: ""

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Marfil)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    navController.navigate(
                                        Routes.artisanProfileRoute(productoActual.artesanoId)
                                    )
                                }
                            )
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFB7C7D9)),
                            contentAlignment = Alignment.Center
                        ) {

                            // Foto de perfil del artesano (recorte circular); si no subió
                            // ninguna, se queda el ícono por defecto.
                            val fotoArtesano = datosArtesano?.fotoUrl

                            if (!fotoArtesano.isNullOrBlank()) {

                                AsyncImage(
                                    model = fotoArtesano,
                                    contentDescription = datosArtesano.nombre,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                )

                            } else {

                                Icon(
                                    imageVector = Icons.Filled.Person,
                                    contentDescription = null,
                                    tint = Guinda,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 10.dp)
                        ) {

                            Text(
                                text = datosArtesano?.nombre ?: stringResource(R.string.modo_artesano),
                                color = Guinda,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )

                            if (contactoMostrado.isNotBlank()) {

                                Text(
                                    text = contactoMostrado,
                                    color = TextoSecundario,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        if (datosArtesano?.ruacEstado == "verificado") {

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xFFFDF1D8))
                                    .padding(horizontal = 7.dp, vertical = 5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = stringResource(R.string.con_registro_ruac),
                                    tint = Color(0xFFA8770F),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    navController.navigate(Routes.qualityTrustRoute(productId))
                                }
                            ),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {

                        BarraValoracion(
                            etiqueta = stringResource(R.string.calidad),
                            promedio = productoActual.calidadPromedio,
                            conteo = productoActual.calidadConteo
                        )

                        // Confianza es del ARTESANO, no del producto (Fase 6, corrige un
                        // error de fases anteriores) — se lee de statsArtesano en vez de
                        // productoActual.confianzaPromedio (campo que ya no existe).
                        BarraValoracion(
                            etiqueta = stringResource(R.string.confianza),
                            promedio = statsArtesano.confianzaPromedio,
                            conteo = statsArtesano.confianzaConteo
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    GradientButton(
                        text = stringResource(R.string.contactar_artesano),
                        onClick = {

                            viewModel.obtenerDatosDelContacto { nombreCliente, correoCliente ->

                                contactarPorWhatsApp(
                                    context = context,
                                    telefono = contactoMostrado,
                                    nombreProducto = productoActual.nombre,
                                    nombreCliente = nombreCliente,
                                    correoCliente = correoCliente
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun SeccionTexto(titulo: String, texto: String) {

    if (texto.isBlank()) return

    Text(
        text = titulo.uppercase(),
        color = Guinda,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
    )

    Text(
        text = texto,
        color = TextoSecundario,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

/**
 * Visor de imágenes en pantalla completa: se abre al tocar la imagen principal del
 * Detalle. Mantiene el carrusel (se puede seguir deslizando entre imágenes) y agrega
 * pinch-to-zoom + paneo sobre la imagen visible ([ZoomableImage]).
 */
@Composable
private fun VisorImagenesDialog(
    imagenes: List<String>,
    paginaInicial: Int,
    onDismiss: () -> Unit
) {

    val pagerState = rememberPagerState(
        initialPage = paginaInicial,
        pageCount = { imagenes.size }
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { pagina ->

                ZoomableImage(
                    url = imagenes[pagina],
                    modifier = Modifier.fillMaxSize()
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.cerrar),
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }

            if (imagenes.size > 1) {

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 20.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${pagerState.currentPage + 1}/${imagenes.size}",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

/**
 * Imagen con pinch-to-zoom (1x a 4x) y paneo mientras está ampliada; vuelve a 1x/centrada
 * cuando se suelta por debajo de esa escala. `detectTransformGestures` da escala y
 * desplazamiento del gesto, no hay ninguna librería nueva de por medio.
 */
@Composable
private fun ZoomableImage(
    url: String,
    modifier: Modifier = Modifier
) {

    var escala by rememberSaveable { mutableStateOf(1f) }
    var desplazamiento by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .pointerInput(url) {
                detectTransformGestures { _, pan, zoom, _ ->

                    val nuevaEscala = (escala * zoom).coerceIn(1f, 4f)

                    desplazamiento = if (nuevaEscala <= 1f) {
                        Offset.Zero
                    } else {
                        val limite = 400f * (nuevaEscala - 1f)
                        Offset(
                            x = (desplazamiento.x + pan.x * nuevaEscala).coerceIn(-limite, limite),
                            y = (desplazamiento.y + pan.y * nuevaEscala).coerceIn(-limite, limite)
                        )
                    }

                    escala = nuevaEscala
                }
            },
        contentAlignment = Alignment.Center
    ) {

        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = escala,
                    scaleY = escala,
                    translationX = desplazamiento.x,
                    translationY = desplazamiento.y
                )
        )
    }
}

private fun formatearPrecio(precio: Long): String {

    val formato = java.text.NumberFormat.getNumberInstance(java.util.Locale("es", "CO"))
    return "$${formato.format(precio)}"
}

/**
 * Abre WhatsApp con un mensaje prellenado hacia el número del artesano
 * (https://wa.me/, un enlace https normal: casi siempre resuelve, abriendo WhatsApp si
 * está instalado o el navegador si no). Si ni eso funciona (sin ninguna app que maneje el
 * enlace), cae a marcar el número directamente.
 */
private fun contactarPorWhatsApp(
    context: android.content.Context,
    telefono: String,
    nombreProducto: String,
    nombreCliente: String,
    correoCliente: String
) {

    if (telefono.isBlank()) {
        Toast.makeText(
            context,
            context.getString(R.string.sin_telefono_artesano),
            Toast.LENGTH_LONG
        ).show()
        return
    }

    val numeroLimpio = telefono.filter { it.isDigit() }
    // El nombre y el correo permiten al artesano registrar la venta en Minka Art (ver
    // RegisterSaleScreen: se identifica al cliente por su correo exacto).
    val datosCliente = buildString {
        if (nombreCliente.isNotBlank()) append("\nNombre: $nombreCliente")
        if (correoCliente.isNotBlank()) append("\nCorreo: $correoCliente")
    }

    val mensaje = "Hola, vi tu $nombreProducto en Minka Art y me gustaría saber más." +
        if (datosCliente.isEmpty()) {
            ""
        } else {
            "\n\nMis datos en Minka Art, por si quieres registrar la venta:$datosCliente"
        }
    val mensajeCodificado = URLEncoder.encode(mensaje, "UTF-8")

    try {

        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://wa.me/$numeroLimpio?text=$mensajeCodificado")
        )
        context.startActivity(intent)

    } catch (error: ActivityNotFoundException) {

        try {

            val intentLlamada = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$telefono"))
            context.startActivity(intentLlamada)

        } catch (errorLlamada: ActivityNotFoundException) {

            Toast.makeText(
                context,
                context.getString(R.string.no_se_pudo_abrir_whatsapp),
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
