package com.minkaart2.app.ui.profile

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.minkaart2.app.R
import androidx.compose.ui.res.stringResource
import com.minkaart2.app.navigation.Routes
import com.minkaart2.app.ui.components.BarraValoracion
import com.minkaart2.app.ui.components.FilaRedesSociales
import com.minkaart2.app.ui.components.GradientButton
import com.minkaart2.app.ui.components.OutlineButton
import com.minkaart2.app.ui.components.IconoCamara
import com.minkaart2.app.ui.components.PatternBackground
import com.minkaart2.app.ui.components.ProductoFeedCard
import com.minkaart2.app.viewModel.ArtisanProfileViewModel

private val Guinda = Color(0xFF3B0F1A)
private val Borgona = Color(0xFF66172E)
private val AzulClaro = Color(0xFFB7C7D9)
private val AzulAcero = Color(0xFF55708E)
private val Marfil = Color(0xFFF2EDE6)
private val TextoSecundario = Color(0xFF8A7480)

@Composable
fun ArtisanProfileScreen(
    artesanoId: String,
    navController: NavController
) {

    val viewModel: ArtisanProfileViewModel = viewModel()
    val context = LocalContext.current

    val artesano by viewModel.artesano
    val productos by viewModel.productos
    val productosLikeados by viewModel.productosLikeados
    val statsArtesano by viewModel.statsArtesano
    val cargando by viewModel.cargando

    val esPerfilPropio = FirebaseAuth.getInstance().currentUser?.uid == artesanoId
    val lifecycleOwner = LocalLifecycleOwner.current

    var mostrarDialogoEditar by remember { mutableStateOf(false) }
    var mostrarDialogoEliminarRuac by remember { mutableStateOf(false) }
    var mostrarDialogoRedes by remember { mutableStateOf(false) }
    val numeroSeguidores by viewModel.numeroSeguidores
    val siguiendo by viewModel.siguiendo

    val selectorFoto = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->

        if (uri != null) {

            viewModel.actualizarFotoPerfil(
                artesanoId = artesanoId,
                fotoUri = uri,
                onSuccess = {
                    Toast.makeText(context, "Foto de perfil actualizada", Toast.LENGTH_SHORT).show()
                },
                onError = { mensaje ->
                    Toast.makeText(context, mensaje, Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    LaunchedEffect(artesanoId) {

        viewModel.cargarPerfil(
            artesanoId = artesanoId,
            onError = { mensaje ->
                Toast.makeText(context, mensaje, Toast.LENGTH_LONG).show()
            }
        )
    }

    // Refresca al volver de RequestRuacScreen (Fase 5), para ver el nuevo ruacEstado sin
    // reabrir la app — mismo patrón que ya usa MyProductsScreen.
    DisposableEffect(lifecycleOwner) {

        val observer = LifecycleEventObserver { _, event ->

            if (event == Lifecycle.Event.ON_RESUME) {

                viewModel.cargarPerfil(
                    artesanoId = artesanoId,
                    onError = { /* si falla el refresco silencioso, se deja tal cual */ }
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
    ) {

        if (cargando) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
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
                    .windowInsetsPadding(WindowInsets.safeDrawing)
            ) {

                // Cabecera: patrón + degradado, botón de volver, avatar superpuesto
                Box(modifier = Modifier.fillMaxWidth()) {

                    PatternBackground(
                        modifier = Modifier.height(150.dp),
                        gradientColors = listOf(Guinda, Borgona, AzulAcero),
                        patternAlpha = 0.5f
                    ) {}

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
                            contentDescription = "Volver",
                            tint = Guinda,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    // Configuración de la cuenta (idioma): solo el dueño del perfil.
                    if (esPerfilPropio) {

                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.9f))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { navController.navigate(Routes.ACCOUNT_SETTINGS) }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = "Configuración de la cuenta",
                                tint = Guinda,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 0.dp)
                            .offset(y = 30.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(AzulClaro),
                            contentAlignment = Alignment.Center
                        ) {

                            if (artesano?.fotoUrl != null) {

                                AsyncImage(
                                    model = artesano?.fotoUrl,
                                    contentDescription = "Foto de perfil",
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )

                            } else {

                                Icon(
                                    imageVector = Icons.Filled.Person,
                                    contentDescription = null,
                                    tint = Guinda,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }

                        if (esPerfilPropio) {

                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(Borgona)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = { selectorFoto.launch("image/*") }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                IconoCamara(
                                    modifier = Modifier.size(12.dp),
                                    color = Color.White,
                                    colorFondo = Borgona
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(38.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text(
                        text = artesano?.nombre ?: "Artesano",
                        color = Guinda,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    // Estado de RUAC (Fase 5): el chip verificado es visible para
                    // cualquiera; el botón de solicitar/re-solicitar y el chip "en
                    // revisión" solo los ve el propio artesano en su propio perfil.
                    when {

                        artesano?.ruacEstado == "verificado" -> {

                            Box(
                                modifier = Modifier
                                    .padding(top = 6.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xFFFDF1D8))
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = {
                                            navController.navigate(Routes.ruacBadgeRoute(artesanoId))
                                        }
                                    )
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFFA8770F),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "Con registro RUAC",
                                        color = Color(0xFFA8770F),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(start = 4.dp)
                                    )
                                }
                            }

                            if (esPerfilPropio) {

                                Row(
                                    modifier = Modifier.padding(top = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {

                                    Text(
                                        text = "Editar RUAC",
                                        color = AzulAcero,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                            onClick = {
                                                navController.navigate(Routes.REQUEST_RUAC)
                                            }
                                        )
                                    )

                                    Text(
                                        text = "Eliminar RUAC",
                                        color = Borgona,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                            onClick = { mostrarDialogoEliminarRuac = true }
                                        )
                                    )
                                }
                            }
                        }

                        esPerfilPropio && artesano?.ruacEstado == "pendiente" -> {

                            Box(
                                modifier = Modifier
                                    .padding(top = 6.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xFFDFE7F0))
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = "RUAC en revisión",
                                    color = AzulAcero,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        esPerfilPropio -> {

                            // "no_solicitado" o "rechazado"
                            Text(
                                text = if (artesano?.ruacEstado == "rechazado") {
                                    "Volver a solicitar RUAC"
                                } else {
                                    "Agregar RUAC"
                                },
                                color = Borgona,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .padding(top = 6.dp)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = {
                                            navController.navigate(Routes.REQUEST_RUAC)
                                        }
                                    )
                            )
                        }
                    }

                    if (!artesano?.bio.isNullOrBlank()) {

                        Text(
                            text = artesano?.bio ?: "",
                            color = TextoSecundario,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 10.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.padding(top = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Icon(
                            imageVector = Icons.Filled.Phone,
                            contentDescription = null,
                            tint = Borgona,
                            modifier = Modifier.size(14.dp)
                        )

                        val contacto = artesano?.contactoPublico?.ifBlank { null }

                        Text(
                            text = contacto ?: "Sin contacto público",
                            color = if (contacto != null) Guinda else TextoSecundario,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = 6.dp)
                        )

                        if (esPerfilPropio) {

                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Editar contacto",
                                tint = Borgona,
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .size(14.dp)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = { mostrarDialogoEditar = true }
                                    )
                            )
                        }
                    }

                    // Redes sociales: logos que abren la red; el dueño además puede editarlas.
                    val tieneRedes = !artesano?.instagram.isNullOrBlank() ||
                        !artesano?.tiktok.isNullOrBlank() ||
                        !artesano?.youtube.isNullOrBlank()

                    if (tieneRedes || esPerfilPropio) {

                        Row(
                            modifier = Modifier.padding(top = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            FilaRedesSociales(
                                instagram = artesano?.instagram,
                                tiktok = artesano?.tiktok,
                                youtube = artesano?.youtube
                            )

                            if (esPerfilPropio) {

                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = stringResource(R.string.editar_redes),
                                    tint = Borgona,
                                    modifier = Modifier
                                        .padding(start = if (tieneRedes) 14.dp else 0.dp)
                                        .size(14.dp)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                            onClick = { mostrarDialogoRedes = true }
                                        )
                                )

                                if (!tieneRedes) {

                                    Text(
                                        text = stringResource(R.string.editar_redes),
                                        color = Borgona,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(start = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Seguir al artesano: recibe aviso cuando publique un producto nuevo.
                    if (!esPerfilPropio) {

                        Spacer(modifier = Modifier.height(14.dp))

                        if (siguiendo) {

                            OutlineButton(
                                text = stringResource(R.string.siguiendo),
                                onClick = {
                                    viewModel.alternarSeguimiento(
                                        artesanoId = artesanoId,
                                        onSuccess = {
                                            Toast.makeText(
                                                context,
                                                context.getString(R.string.dejaste_de_seguir),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        },
                                        onError = { mensaje ->
                                            Toast.makeText(context, mensaje, Toast.LENGTH_LONG).show()
                                        }
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(0.6f),
                                borderColor = Borgona,
                                textColor = Borgona
                            )

                        } else {

                            GradientButton(
                                text = stringResource(R.string.seguir),
                                onClick = {
                                    viewModel.alternarSeguimiento(
                                        artesanoId = artesanoId,
                                        onSuccess = {
                                            Toast.makeText(
                                                context,
                                                context.getString(R.string.ahora_sigues),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        },
                                        onError = { mensaje ->
                                            Toast.makeText(context, mensaje, Toast.LENGTH_LONG).show()
                                        }
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(0.6f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {

                        EstadisticaItem(
                            valor = productos.size.toString(),
                            etiqueta = "Productos"
                        )

                        EstadisticaItem(
                            valor = numeroSeguidores.toString(),
                            etiqueta = stringResource(R.string.seguidores)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Producto más reciente del artesano, solo para tener un productoId
                    // válido con el que abrir QualityTrustScreen desde el indicador de
                    // Confianza (que es del artesano completo, no de un producto en
                    // particular) — ver Fase 6, Decisión 3.
                    val productoMasReciente = productos.maxByOrNull { it.fechaPublicacion }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (productoMasReciente != null) {
                                    Modifier.clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = {
                                            navController.navigate(
                                                Routes.qualityTrustRoute(productoMasReciente.id)
                                            )
                                        }
                                    )
                                } else {
                                    Modifier
                                }
                            ),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {

                        BarraValoracion(
                            etiqueta = "Calidad",
                            promedio = productos.mapNotNull { it.calidadPromedio }
                                .takeIf { it.isNotEmpty() }?.average(),
                            conteo = productos.sumOf { it.calidadConteo }
                        )

                        BarraValoracion(
                            etiqueta = "Confianza",
                            promedio = statsArtesano.confianzaPromedio,
                            conteo = statsArtesano.confianzaConteo
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "Productos",
                        color = Guinda,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )

                    if (productos.isEmpty()) {

                        Text(
                            text = "Este artesano aún no tiene productos publicados",
                            color = TextoSecundario,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                    } else {

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.height(((productos.size + 1) / 2 * 190).dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {

                            items(
                                items = productos,
                                key = { it.id }
                            ) { producto ->

                                ProductoFeedCard(
                                    producto = producto,
                                    nombreArtesano = artesano?.nombre,
                                    leGustaAlUsuario = productosLikeados.contains(producto.id),
                                    onClick = {
                                        navController.navigate(
                                            Routes.productDetailRoute(producto.id)
                                        )
                                    },
                                    onLikeClick = {
                                        viewModel.alternarLike(
                                            productoId = producto.id,
                                            onError = { mensaje ->
                                                Toast.makeText(context, mensaje, Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    },
                                    confianzaArtesano = statsArtesano.confianzaPromedio
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    if (mostrarDialogoEditar) {

        var bioEditada by remember { mutableStateOf(artesano?.bio ?: "") }
        var contactoEditado by remember { mutableStateOf(artesano?.contactoPublico ?: "") }

        AlertDialog(
            onDismissRequest = { mostrarDialogoEditar = false },
            title = { Text("Editar perfil público") },
            text = {
                Column {

                    OutlinedTextField(
                        value = bioEditada,
                        onValueChange = { bioEditada = it },
                        label = { Text("Sobre mí") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = contactoEditado,
                        onValueChange = { contactoEditado = it },
                        label = { Text("Contacto público") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {

                TextButton(
                    onClick = {

                        viewModel.actualizarContacto(
                            artesanoId = artesanoId,
                            bio = bioEditada,
                            contactoPublico = contactoEditado,

                            onSuccess = {

                                mostrarDialogoEditar = false

                                Toast.makeText(
                                    context,
                                    "Perfil actualizado",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },

                            onError = { mensaje ->
                                Toast.makeText(context, mensaje, Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {

                TextButton(onClick = { mostrarDialogoEditar = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (mostrarDialogoRedes) {

        var instagramEditado by remember { mutableStateOf(artesano?.instagram ?: "") }
        var tiktokEditado by remember { mutableStateOf(artesano?.tiktok ?: "") }
        var youtubeEditado by remember { mutableStateOf(artesano?.youtube ?: "") }

        AlertDialog(
            onDismissRequest = { mostrarDialogoRedes = false },
            title = { Text(stringResource(R.string.redes_sociales)) },
            text = {
                Column {

                    listOf(
                        Triple<String, String, (String) -> Unit>("Instagram", instagramEditado, { instagramEditado = it }),
                        Triple<String, String, (String) -> Unit>("TikTok", tiktokEditado, { tiktokEditado = it }),
                        Triple<String, String, (String) -> Unit>("YouTube", youtubeEditado, { youtubeEditado = it })
                    ).forEachIndexed { indice, (nombre, valor, alCambiar) ->

                        if (indice > 0) Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = valor,
                            onValueChange = alCambiar,
                            label = { Text("$nombre (${stringResource(R.string.red_usuario_o_enlace)})") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {

                TextButton(
                    onClick = {

                        viewModel.actualizarRedesSociales(
                            artesanoId = artesanoId,
                            instagram = instagramEditado,
                            tiktok = tiktokEditado,
                            youtube = youtubeEditado,

                            onSuccess = {

                                mostrarDialogoRedes = false

                                Toast.makeText(
                                    context,
                                    "Perfil actualizado",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },

                            onError = { mensaje ->
                                Toast.makeText(context, mensaje, Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {

                TextButton(onClick = { mostrarDialogoRedes = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (mostrarDialogoEliminarRuac) {

        AlertDialog(
            onDismissRequest = { mostrarDialogoEliminarRuac = false },
            title = { Text("Eliminar RUAC") },
            text = { Text("¿Seguro que quieres eliminar tu RUAC? Perderás la insignia de verificado") },
            confirmButton = {

                TextButton(
                    onClick = {

                        mostrarDialogoEliminarRuac = false

                        viewModel.eliminarRuac(
                            artesanoId = artesanoId,

                            onSuccess = {

                                Toast.makeText(
                                    context,
                                    "RUAC eliminado",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },

                            onError = { mensaje ->
                                Toast.makeText(context, mensaje, Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                ) {
                    Text("Eliminar", color = Borgona)
                }
            },
            dismissButton = {

                TextButton(onClick = { mostrarDialogoEliminarRuac = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun EstadisticaItem(valor: String, etiqueta: String) {

    Column(horizontalAlignment = Alignment.CenterHorizontally) {

        Text(
            text = valor,
            color = Guinda,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = etiqueta,
            color = TextoSecundario,
            style = MaterialTheme.typography.bodySmall
        )
    }
}
