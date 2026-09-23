package com.minkaart2.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import android.widget.Toast
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.minkaart2.app.data.model.Product
import com.minkaart2.app.navigation.Routes
import com.minkaart2.app.ui.components.BottomNavBar
import com.minkaart2.app.ui.components.BottomNavItemData
import com.minkaart2.app.ui.components.EfectoNotificaciones
import com.minkaart2.app.ui.components.GradientButton
import com.minkaart2.app.ui.components.IconoNotificaciones
import com.minkaart2.app.ui.components.OutlineButton
import com.minkaart2.app.ui.components.RolePillToggle
import com.minkaart2.app.viewModel.HomeArtesanoViewModel
import com.minkaart2.app.viewModel.NotificationsViewModel
import com.minkaart2.app.viewModel.ProductViewModel

private val Guinda = Color(0xFF3B0F1A)
private val Borgona = Color(0xFF66172E)
private val AzulClaro = Color(0xFFB7C7D9)
private val Marfil = Color(0xFFF2EDE6)
private val TextoSecundario = Color(0xFF8A7480)

@Composable
fun HomeArtesanoScreen(
    navController: NavController,
    onPublicarClick: () -> Unit,
    onMisPublicacionesClick: () -> Unit,
    onPerfilClick: () -> Unit
) {

    val homeViewModel: HomeArtesanoViewModel = viewModel()
    val productViewModel: ProductViewModel = viewModel()
    val notificationsViewModel: NotificationsViewModel = viewModel()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    EfectoNotificaciones()

    val nombreArtesano by homeViewModel.nombreArtesano
    val productos by productViewModel.misProductos
    val noLeidas by notificationsViewModel.noLeidas

    LaunchedEffect(Unit) {

        homeViewModel.cargarDatosArtesano(
            onError = { /* si falla, el saludo simplemente queda sin nombre */ }
        )

        productViewModel.cargarMisProductos(
            onError = { mensaje ->
                Toast.makeText(context, mensaje, Toast.LENGTH_LONG).show()
            }
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

                // Refresca el panel de Estadísticas (likes/vistas pueden haber
                // cambiado mientras el artesano no estaba en esta pantalla).
                productViewModel.cargarMisProductos(
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
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
    ) {

        // Encabezado
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

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                IconoNotificaciones(
                    noLeidas = noLeidas,
                    onClick = { navController.navigate(Routes.NOTIFICATIONS) }
                )

                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(AzulClaro)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onPerfilClick
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = "Mi perfil",
                        tint = Guinda,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Cambio de modo — reemplaza al antiguo ítem "Explorar" del bottom nav, que
        // mandaba a HomeClienteScreen sin ninguna señal de que se cambió de "identidad".
        // "Artesano" ya está aquí; tocar "Cliente" es la única acción real.
        RolePillToggle(
            opciones = listOf("artesano" to "Artesano", "cliente" to "Cliente"),
            valorSeleccionado = "artesano",
            onSeleccionar = { valor ->

                if (valor == "cliente") {
                    navController.navigate(Routes.HOME_CLIENTE)
                }
            },
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp),
            colorFondoActivo = Borgona,
            colorFondoInactivo = Color.White,
            colorBorde = Color.White,
            colorTextoActivo = Marfil,
            colorTextoInactivo = TextoSecundario
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 18.dp)
        ) {

            Text(
                text = "Hola,",
                color = Color(0xFF8A7480),
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                text = nombreArtesano.ifBlank { "artesana/o" },
                color = Guinda,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                GradientButton(
                    text = "Publicar",
                    icon = Icons.Filled.Add,
                    onClick = onPublicarClick,
                    modifier = Modifier.weight(1f)
                )

                OutlineButton(
                    text = "Ver publicaciones",
                    icon = Icons.Filled.List,
                    borderColor = Borgona,
                    textColor = Borgona,
                    onClick = onMisPublicacionesClick,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            PanelEstadisticas(
                productos = productos,
                onProductoClick = { productoId ->
                    navController.navigate(Routes.productDetailRoute(productoId))
                }
            )

            Spacer(modifier = Modifier.height(20.dp))
        }

        val uidActual = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        BottomNavBar(
            items = listOf(
                BottomNavItemData(
                    icon = Icons.Filled.Home,
                    label = "Inicio",
                    activo = true,
                    onClick = {}
                ),
                BottomNavItemData(
                    icon = Icons.Filled.CheckCircle,
                    label = "RUAC",
                    activo = false,
                    onClick = {
                        navController.navigate(Routes.ruacBadgeRoute(uidActual))
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
                    onClick = onPerfilClick
                )
            )
        )
    }
}

/**
 * Panel de Estadísticas: se calcula sobre TODOS los productos del artesano
 * (`productViewModel.misProductos`, ya cargado por HomeArtesanoScreen), no solo la
 * vista previa de 4. Si ningún producto tiene vistas ni likes, muestra un estado
 * neutro en vez de destacar un producto al azar en el empate en 0.
 */
@Composable
private fun PanelEstadisticas(
    productos: List<Product>,
    onProductoClick: (String) -> Unit
) {

    val sumaLikes = remember(productos) { productos.sumOf { it.likes } }
    val sumaVistas = remember(productos) { productos.sumOf { it.vistas } }
    val activos = remember(productos) { productos.count { it.activo } }

    val productoMasVisto = remember(productos) {
        productos.filter { it.vistas > 0 }.maxByOrNull { it.vistas }
    }
    val productoConMasLikes = remember(productos) {
        productos.filter { it.likes > 0 }.maxByOrNull { it.likes }
    }

    Column {

        Text(
            text = "Estadísticas",
            color = Guinda,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {

            TarjetaMetrica(
                icon = Icons.Filled.Favorite,
                numero = "$sumaLikes",
                etiqueta = "Likes",
                modifier = Modifier.weight(1f)
            )

            TarjetaMetrica(
                icon = Icons.Filled.Search,
                numero = "$sumaVistas",
                etiqueta = "Vistas",
                modifier = Modifier.weight(1f)
            )

            TarjetaMetrica(
                icon = Icons.Filled.CheckCircle,
                numero = "$activos/${productos.size}",
                etiqueta = "Activos",
                modifier = Modifier.weight(1f)
            )
        }

        if (productoMasVisto == null && productoConMasLikes == null) {

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Aún no hay actividad",
                color = TextoSecundario,
                style = MaterialTheme.typography.bodySmall
            )

        } else {

            if (productoMasVisto != null) {

                Spacer(modifier = Modifier.height(9.dp))

                FilaProductoDestacado(
                    producto = productoMasVisto,
                    etiqueta = "Producto más visto",
                    valor = "${productoMasVisto.vistas} vistas",
                    onClick = { onProductoClick(productoMasVisto.id) }
                )
            }

            if (productoConMasLikes != null) {

                Spacer(modifier = Modifier.height(9.dp))

                FilaProductoDestacado(
                    producto = productoConMasLikes,
                    etiqueta = "Producto con más likes",
                    valor = "${productoConMasLikes.likes} likes",
                    onClick = { onProductoClick(productoConMasLikes.id) }
                )
            }
        }
    }
}

@Composable
private fun TarjetaMetrica(
    icon: ImageVector,
    numero: String,
    etiqueta: String,
    modifier: Modifier = Modifier
) {

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Borgona,
            modifier = Modifier.size(16.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = numero,
            color = Guinda,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )

        Text(
            text = etiqueta,
            color = TextoSecundario,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun FilaProductoDestacado(
    producto: Product,
    etiqueta: String,
    valor: String,
    onClick: () -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(AzulClaro)
        ) {

            if (producto.imagenes.isNotEmpty()) {

                AsyncImage(
                    model = producto.imagenes.first(),
                    contentDescription = producto.nombre,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp)
        ) {

            Text(
                text = etiqueta,
                color = TextoSecundario,
                style = MaterialTheme.typography.labelSmall
            )

            Text(
                text = producto.nombre,
                color = Guinda,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        Text(
            text = valor,
            color = Borgona,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}
