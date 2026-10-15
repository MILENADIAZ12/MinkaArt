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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.minkaart2.app.R
import com.minkaart2.app.data.CatalogosProducto
import com.minkaart2.app.data.model.Product
import com.minkaart2.app.navigation.Routes
import com.minkaart2.app.ui.components.BottomNavBar
import com.minkaart2.app.ui.components.BottomNavItemData
import com.minkaart2.app.ui.components.EfectoNotificaciones
import com.minkaart2.app.ui.components.IconoAccion
import com.minkaart2.app.ui.components.IconoCamara
import com.minkaart2.app.ui.components.IconoNotificaciones
import com.minkaart2.app.ui.components.LogoMinkaEncabezado
import com.minkaart2.app.ui.components.ProductoFeedCard
import com.minkaart2.app.ui.components.RolePillToggle
import com.minkaart2.app.ui.theme.AzulClaro
import com.minkaart2.app.ui.theme.Borgona
import com.minkaart2.app.ui.theme.Guinda
import com.minkaart2.app.ui.theme.Marfil
import com.minkaart2.app.util.etiquetaCategoria
import com.minkaart2.app.util.etiquetaColor
import com.minkaart2.app.util.etiquetaMateriaPrima
import com.minkaart2.app.util.etiquetaTamano
import com.minkaart2.app.util.normalizar
import com.minkaart2.app.viewModel.HomeClienteViewModel
import com.minkaart2.app.viewModel.NotificationsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val TextoSecundario = Color(0xFF8A7480)

// Valor interno de "sin filtro" en los chips de categoría — nunca se guarda en Firestore,
// solo se compara; lo que se muestra se traduce con R.string.todos.
private const val TODOS = "Todos"

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

    // Filtros del catálogo (panel de filtros). Se mantienen al cambiar de categoría.
    var filtros by remember { mutableStateOf(FiltrosCatalogo()) }
    var filtrosAbiertos by remember { mutableStateOf(false) }

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
    var subirFeed by remember { mutableIntStateOf(0) }
    val focusRequesterBusqueda = remember { FocusRequester() }
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
        listOf(TODOS) + productos
            .map { it.categoria.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

    // Opciones del panel de filtros: derivadas de los valores reales presentes en los
    // productos cargados (igual que las categorías), no de las listas fijas completas.
    val materiasPrimas = remember(productos) {
        productos
            .map { it.materiaPrima.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

    val ubicaciones = remember(productos) {
        productos
            .map { it.departamento.ifBlank { it.regionOrigen }.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

    val colores = remember(productos) {
        productos
            .map { it.color.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sortedBy { CatalogosProducto.COLORES.indexOf(it).let { i -> if (i < 0) Int.MAX_VALUE else i } }
    }

    val tamanos = remember(productos) {
        productos
            .map { it.tamano.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sortedBy { CatalogosProducto.TAMANOS.indexOf(it).let { i -> if (i < 0) Int.MAX_VALUE else i } }
    }

    val textoBuscadoNormalizado = remember(textoBusquedaDebounced) {
        normalizar(textoBusquedaDebounced)
    }

    // El pager y los chips de categoría van en sincronía: deslizar la pantalla cambia la
    // categoría activa y la fila de chips se desplaza sola para que siga visible.
    val pagerState = rememberPagerState(pageCount = { categorias.size })
    val chipsState = rememberLazyListState()
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    LaunchedEffect(pagerState.currentPage) {
        chipsState.animateScrollToItem((pagerState.currentPage - 1).coerceAtLeast(0))
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

            LogoMinkaEncabezado(colorEslogan = Guinda)

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
                opciones = listOf(
                    "cliente" to stringResource(R.string.modo_cliente),
                    "artesano" to stringResource(R.string.modo_artesano)
                ),
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

        // Barra de búsqueda + botón de filtros
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White)
                    .padding(horizontal = 12.dp, vertical = 5.dp),
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
                            text = stringResource(R.string.buscar_placeholder),
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
                        .size(24.dp)
                        .clip(RoundedCornerShape(8.dp))
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
                        modifier = Modifier.size(14.dp),
                        color = Color.White,
                        colorFondo = Borgona
                    )
                }
            }

            // Botón de filtros: ícono vinotinto; el punto/píldora de novedad indica
            // cuántos filtros hay aplicados.
            Box(modifier = Modifier.padding(start = 10.dp)) {

                IconoAccion(
                    painter = painterResource(id = R.drawable.ic_filtros),
                    descripcion = stringResource(R.string.filtros_titulo),
                    onClick = { filtrosAbiertos = true },
                    novedades = filtros.cantidad,
                    tamano = 36.dp
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Chips de categoría — se desplazan solos al deslizar entre categorías; tocar uno
        // también cambia de categoría.
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            state = chipsState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            items(count = categorias.size) { indice ->

                val categoria = categorias[indice]
                val activo = indice == pagerState.currentPage

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (activo) Borgona else Color.White)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                coroutineScope.launch { pagerState.animateScrollToPage(indice) }
                            }
                        )
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = if (categoria == TODOS) {
                            stringResource(R.string.todos)
                        } else {
                            etiquetaCategoria(categoria)
                        },
                        color = if (activo) Marfil else TextoSecundario,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Filtros aplicados, como chips pequeños que se pueden quitar uno a uno.
        if (!filtros.vacio) {

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {

                lazyRowItems(filtros.materiales.toList()) { valor ->
                    ChipFiltroAplicado(
                        texto = etiquetaMateriaPrima(valor),
                        onQuitar = { filtros = filtros.copy(materiales = filtros.materiales - valor) }
                    )
                }

                lazyRowItems(filtros.ubicaciones.toList()) { valor ->
                    ChipFiltroAplicado(
                        texto = valor,
                        onQuitar = { filtros = filtros.copy(ubicaciones = filtros.ubicaciones - valor) }
                    )
                }

                lazyRowItems(filtros.colores) { valor ->
                    ChipFiltroAplicado(
                        texto = etiquetaColor(valor),
                        onQuitar = { filtros = filtros.copy(colores = filtros.colores - valor) }
                    )
                }

                lazyRowItems(filtros.tamanos.toList()) { valor ->
                    ChipFiltroAplicado(
                        texto = etiquetaTamano(valor),
                        onQuitar = { filtros = filtros.copy(tamanos = filtros.tamanos - valor) }
                    )
                }

                item {
                    ChipFiltroAplicado(
                        texto = stringResource(R.string.filtros_limpiar_corto),
                        onQuitar = { filtros = FiltrosCatalogo() },
                        conCruz = false
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
                    text = stringResource(R.string.sin_publicaciones),
                    color = TextoSecundario,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

        } else {

            // Carrusel de páginas: una por categoría. Texto, filtros y categoría se
            // combinan con AND; todo en memoria, sin consultas nuevas.
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                key = { categorias.getOrElse(it) { TODOS } }
            ) { pagina ->

                val categoria = categorias.getOrElse(pagina) { TODOS }

                val productosFiltrados = remember(
                    productos,
                    categoria,
                    filtros,
                    textoBuscadoNormalizado
                ) {
                    filtrarProductos(productos, categoria, filtros, textoBuscadoNormalizado)
                }

                val gridState = rememberLazyGridState()

                LaunchedEffect(subirFeed) {
                    if (subirFeed > 0 && pagina == pagerState.currentPage) {
                        gridState.animateScrollToItem(0)
                    }
                }

                if (productosFiltrados.isEmpty()) {

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.sin_resultados_criterio),
                            color = TextoSecundario,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }

                } else {

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        state = gridState,
                        modifier = Modifier.fillMaxSize(),
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
            }
        }

        BottomNavBar(
            items = listOf(
                BottomNavItemData(
                    icon = Icons.Filled.Home,
                    label = stringResource(R.string.nav_inicio),
                    activo = true,
                    onClick = {}
                ),
                BottomNavItemData(
                    icon = Icons.Filled.Search,
                    label = stringResource(R.string.nav_buscar),
                    activo = false,
                    onClick = {
                        subirFeed++
                        focusRequesterBusqueda.requestFocus()
                        tecladoController?.show()
                    }
                ),
                BottomNavItemData(
                    icon = Icons.Filled.Favorite,
                    label = stringResource(R.string.nav_likes),
                    activo = false,
                    onClick = { navController.navigate(Routes.LIKES) }
                ),
                BottomNavItemData(
                    icon = Icons.Filled.Person,
                    label = stringResource(R.string.nav_perfil),
                    activo = false,
                    onClick = { navController.navigate(Routes.CLIENT_PROFILE) }
                )
            )
        )
    }

    if (filtrosAbiertos) {

        PanelFiltros(
            materiales = materiasPrimas,
            ubicaciones = ubicaciones,
            colores = colores,
            tamanos = tamanos,
            aplicados = filtros,
            onAplicar = {
                filtros = it
                filtrosAbiertos = false
            },
            onCerrar = { filtrosAbiertos = false }
        )
    }
}

private fun filtrarProductos(
    productos: List<Product>,
    categoria: String,
    filtros: FiltrosCatalogo,
    textoNormalizado: String
): List<Product> = productos.filter { producto ->

    val cumpleCategoria = categoria == TODOS || producto.categoria == categoria

    val cumpleTexto = textoNormalizado.isBlank() ||
        normalizar(producto.nombre).contains(textoNormalizado) ||
        normalizar(producto.descripcion).contains(textoNormalizado)

    cumpleCategoria && cumpleTexto && filtros.acepta(producto)
}

/** Chip pequeño de un filtro aplicado; tocarlo lo quita. */
@Composable
private fun ChipFiltroAplicado(
    texto: String,
    onQuitar: () -> Unit,
    conCruz: Boolean = true
) {

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (conCruz) AzulClaro else Color.Transparent)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onQuitar
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            text = texto,
            color = if (conCruz) Guinda else Borgona,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (conCruz) FontWeight.Medium else FontWeight.Bold
        )

        if (conCruz) {

            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = null,
                tint = Guinda,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(12.dp)
            )
        }
    }
}
