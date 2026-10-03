package com.minkaart2.app.ui.home

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as lazyRowItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.minkaart2.app.navigation.Routes
import com.minkaart2.app.ui.components.BottomNavBar
import com.minkaart2.app.ui.components.BottomNavItemData
import com.minkaart2.app.ui.components.EfectoNotificaciones
import com.minkaart2.app.ui.components.IconoCamara
import com.minkaart2.app.ui.components.IconoNotificaciones
import com.minkaart2.app.ui.components.ProductoFeedCard
import com.minkaart2.app.ui.components.RolePillToggle
import com.minkaart2.app.viewModel.HomeClienteViewModel
import com.minkaart2.app.viewModel.NotificationsViewModel
import com.minkaart2.app.util.normalizar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val Guinda = Color(0xFF3B0F1A)
private val Borgona = Color(0xFF66172E)
private val AzulAcero = Color(0xFF55708E)
private val Marfil = Color(0xFFF2EDE6)
private val TextoSecundario = Color(0xFF8A7480)

@Composable
fun HomeClienteScreen(navController: NavController) {

    val viewModel: HomeClienteViewModel = viewModel()
    val notificationsViewModel: NotificationsViewModel = viewModel()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    EfectoNotificaciones()

    val productos by viewModel.productos
    val productosLikeados by viewModel.productosLikeados
    val nombresArtesanos by viewModel.nombresArtesanos
    val statsArtesanos by viewModel.statsArtesanos
    val cargando by viewModel.cargando
    val esArtesano by viewModel.esArtesano
    val noLeidas by notificationsViewModel.noLeidas

    var categoriaSeleccionada by remember { mutableStateOf("Todos") }
    var materiaPrimaSeleccionada by remember { mutableStateOf("Todos") }

    // Texto tal como se escribe, y su versión "asentada" 300ms después de la última
    // tecla — evita recalcular el filtro en cada pulsación si el catálogo crece.
    var textoBusqueda by remember { mutableStateOf("") }
    var textoBusquedaDebounced by remember { mutableStateOf("") }

    LaunchedEffect(textoBusqueda) {
        delay(300)
        textoBusquedaDebounced = textoBusqueda
    }

    // Para que "Buscar" de la barra inferior haga algo: sube el feed hasta arriba
    // (la barra de búsqueda vive en esta misma pantalla, no en una ruta aparte) y le
    // da foco al campo de texto para que el teclado se abra listo para escribir.
    val gridState = rememberLazyGridState()
    val focusRequesterBusqueda = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()
    val tecladoController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {

        viewModel.cargarFeed(
            onError = { mensaje ->
                Toast.makeText(context, mensaje, Toast.LENGTH_LONG).show()
            }
        )

        viewModel.cargarRolUsuarioActual(
            onError = { /* si falla, simplemente no se muestra el toggle de modo */ }
        )

        notificationsViewModel.cargar(
            onError = { /* el badge simplemente queda sin número si falla */ }
        )
    }

    // Refresca el badge de no leídas al volver de NotificationsScreen, sin reiniciar la
    // app — mismo patrón que ya usa ArtisanProfileScreen/MyProductsScreen.
    DisposableEffect(lifecycleOwner) {

        val observer = LifecycleEventObserver { _, event ->

            if (event == Lifecycle.Event.ON_RESUME) {

                notificationsViewModel.cargar(
                    onError = { /* si falla el refresco silencioso, se deja tal cual */ }
                )
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val categorias = remember(productos) {
        listOf("Todos") + productos
            .map { it.categoria.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

    val materiasPrimas = remember(productos) {
        listOf("Todos") + productos
            .map { it.materiaPrima.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

    val textoBuscadoNormalizado = remember(textoBusquedaDebounced) {
        normalizar(textoBusquedaDebounced)
    }

    // Categoría, materia prima y texto se combinan con AND: el producto tiene que
    // cumplir todas las condiciones activas a la vez (las que estén en "Todos" no
    // filtran nada).
    val productosFiltrados = remember(
        productos,
        categoriaSeleccionada,
        materiaPrimaSeleccionada,
        textoBuscadoNormalizado
    ) {
        productos.filter { producto ->

            val cumpleCategoria = categoriaSeleccionada == "Todos" ||
                producto.categoria == categoriaSeleccionada

            val cumpleMateriaPrima = materiaPrimaSeleccionada == "Todos" ||
                producto.materiaPrima == materiaPrimaSeleccionada

            val cumpleTexto = textoBuscadoNormalizado.isBlank() ||
                normalizar(producto.nombre).contains(textoBuscadoNormalizado) ||
                normalizar(producto.descripcion).contains(textoBuscadoNormalizado)

            cumpleCategoria && cumpleMateriaPrima && cumpleTexto
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Marfil)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
    ) {

        // Encabezado — mismo patrón que HomeArtesanoScreen, para que los dos Home se
        // vean consistentes (antes este Home no tenía ninguno).
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Marfil)
                .padding(18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Row(verticalAlignment = Alignment.CenterVertically) {

                Box(
                    modifier = Modifier
                        .height(28.dp)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Guinda)
                )

                Text(
                    text = "Minka Art",
                    color = Guinda,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 10.dp)
                )
            }

            IconoNotificaciones(
                noLeidas = noLeidas,
                onClick = { navController.navigate(Routes.NOTIFICATIONS) }
            )
        }

        // Toggle de cambio de modo — solo si el usuario autenticado es un artesano
        // "viendo como cliente" (esArtesano viene del rol real en Firestore, nunca de
        // la ruta), así un cliente real nunca lo ve. Queda fijo, antes del buscador,
        // para funcionar también como el indicador persistente de en qué modo está.
        if (esArtesano) {

            RolePillToggle(
                opciones = listOf("cliente" to "Cliente", "artesano" to "Artesano"),
                valorSeleccionado = "cliente",
                onSeleccionar = { valor ->

                    if (valor == "artesano") {

                        navController.navigate(Routes.HOME_ARTESANO) {
                            popUpTo(Routes.HOME_ARTESANO) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                colorFondoActivo = Borgona,
                colorFondoInactivo = Color.White,
                colorBorde = Color.White,
                colorTextoActivo = Marfil,
                colorTextoInactivo = TextoSecundario
            )
        }

        // Barra de búsqueda
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = TextoSecundario,
                modifier = Modifier.size(16.dp)
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
            ) {

                if (textoBusqueda.isEmpty()) {

                    Text(
                        text = "Buscar artesanía, técnica, región…",
                        color = TextoSecundario,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                BasicTextField(
                    value = textoBusqueda,
                    onValueChange = { textoBusqueda = it },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall.copy(color = Guinda),
                    cursorBrush = SolidColor(Borgona),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequesterBusqueda)
                )
            }

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(Borgona)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { navController.navigate(Routes.RECOGNITION) }
                    ),
                contentAlignment = Alignment.Center
            ) {
                // No hay un ícono de cámara en el set básico de Material (y el
                // proyecto no tiene material-icons-extended) — se dibuja a mano con
                // Canvas en vez de agregar una dependencia nueva solo por un ícono.
                IconoCamara(
                    modifier = Modifier.size(16.dp),
                    color = Color.White,
                    colorFondo = Borgona
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Chips de categoría
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            lazyRowItems(categorias) { categoria ->

                val activo = categoria == categoriaSeleccionada

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (activo) Borgona else Color.White)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { categoriaSeleccionada = categoria }
                        )
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = categoria,
                        color = if (activo) Marfil else TextoSecundario,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Chips de materia prima — mismo patrón que los de categoría, combinables con
        // ella y con el buscador.
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            lazyRowItems(materiasPrimas) { materiaPrima ->

                val activo = materiaPrima == materiaPrimaSeleccionada

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (activo) AzulAcero else Color.White)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { materiaPrimaSeleccionada = materiaPrima }
                        )
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = materiaPrima,
                        color = if (activo) Marfil else TextoSecundario,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (cargando) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(color = Borgona)
            }

        } else if (productos.isEmpty()) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Todavía no hay artesanías publicadas",
                    color = TextoSecundario,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

        } else if (productosFiltrados.isEmpty()) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "No encontramos artesanías con ese criterio",
                    color = TextoSecundario,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }

        } else {

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                state = gridState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                items(
                    items = productosFiltrados,
                    key = { it.id }
                ) { producto ->

                    ProductoFeedCard(
                        producto = producto,
                        nombreArtesano = nombresArtesanos[producto.artesanoId],
                        leGustaAlUsuario = productosLikeados.contains(producto.id),
                        onClick = {
                            navController.navigate(Routes.productDetailRoute(producto.id))
                        },
                        onLikeClick = {
                            viewModel.alternarLike(
                                productoId = producto.id,
                                onError = { mensaje ->
                                    Toast.makeText(context, mensaje, Toast.LENGTH_SHORT).show()
                                }
                            )
                        },
                        onArtesanoClick = {
                            navController.navigate(
                                Routes.artisanProfileRoute(producto.artesanoId)
                            )
                        },
                        confianzaArtesano = statsArtesanos[producto.artesanoId]
                    )
                }
            }
        }

        BottomNavBar(
            items = listOf(
                BottomNavItemData(
                    icon = Icons.Filled.Home,
                    label = "Inicio",
                    activo = true,
                    onClick = {}
                ),
                BottomNavItemData(
                    icon = Icons.Filled.Search,
                    label = "Buscar",
                    activo = false,
                    onClick = {
                        coroutineScope.launch { gridState.animateScrollToItem(0) }
                        focusRequesterBusqueda.requestFocus()
                        tecladoController?.show()
                    }
                ),
                BottomNavItemData(
                    icon = Icons.Filled.Favorite,
                    label = "Likes",
                    activo = false,
                    onClick = { navController.navigate(Routes.LIKES) }
                ),
                BottomNavItemData(
                    icon = Icons.Filled.Person,
                    label = "Perfil",
                    activo = false,
                    onClick = { navController.navigate(Routes.CLIENT_PROFILE) }
                )
            )
        )
    }
}
