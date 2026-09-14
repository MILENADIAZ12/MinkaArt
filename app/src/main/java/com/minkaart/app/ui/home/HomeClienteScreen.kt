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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as lazyRowItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.minkaart2.app.data.model.Product
import com.minkaart2.app.navigation.Routes
import com.minkaart2.app.ui.components.ConfianzaDots
import com.minkaart2.app.viewModel.HomeClienteViewModel

private val Guinda = Color(0xFF3B0F1A)
private val Borgona = Color(0xFF66172E)
private val AzulAcero = Color(0xFF55708E)
private val Marfil = Color(0xFFF2EDE6)
private val TextoSecundario = Color(0xFF8A7480)

@Composable
fun HomeClienteScreen(navController: NavController) {

    val viewModel: HomeClienteViewModel = viewModel()
    val context = LocalContext.current

    val productos by viewModel.productos
    val productosLikeados by viewModel.productosLikeados
    val nombresArtesanos by viewModel.nombresArtesanos
    val cargando by viewModel.cargando

    var categoriaSeleccionada by remember { mutableStateOf("Todos") }

    LaunchedEffect(Unit) {

        viewModel.cargarFeed(
            onError = { mensaje ->
                Toast.makeText(context, mensaje, Toast.LENGTH_LONG).show()
            }
        )
    }

    val categorias = remember(productos) {
        listOf("Todos") + productos
            .map { it.categoria.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

    val productosFiltrados = if (categoriaSeleccionada == "Todos") {
        productos
    } else {
        productos.filter { it.categoria == categoriaSeleccionada }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Marfil)
            .statusBarsPadding()
    ) {

        // Barra de búsqueda
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp, 12.dp)
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

            Text(
                text = "Buscar artesanía, técnica, región…",
                color = TextoSecundario,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
            )

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
                // No hay un ícono de cámara en el set básico de Material; se usa
                // Search como acceso visual, coherente con "reconocimiento visual".
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "Reconocimiento visual",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

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

        Spacer(modifier = Modifier.height(10.dp))

        if (cargando) {

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(color = Borgona)
            }

        } else if (productosFiltrados.isEmpty()) {

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Todavía no hay artesanías publicadas",
                    color = TextoSecundario,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

        } else {

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
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
                        }
                    )
                }
            }
        }

        // Bottom nav
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {

            BottomNavItem(
                icon = Icons.Filled.Home,
                label = "Inicio",
                activo = true,
                onClick = {}
            )

            BottomNavItem(
                icon = Icons.Filled.Search,
                label = "Buscar",
                activo = false,
                onClick = { /* la barra de búsqueda ya está visible en esta misma pantalla */ }
            )

            BottomNavItem(
                icon = Icons.Filled.Favorite,
                label = "Likes",
                activo = false,
                onClick = { navController.navigate(Routes.LIKES) }
            )

            BottomNavItem(
                icon = Icons.Filled.Person,
                label = "Perfil",
                activo = false,
                onClick = { navController.navigate(Routes.PROFILE) }
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    icon: ImageVector,
    label: String,
    activo: Boolean,
    onClick: () -> Unit
) {

    Column(
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (activo) Borgona else Color(0xFFC9B8BD),
            modifier = Modifier.size(20.dp)
        )

        Text(
            text = label,
            color = if (activo) Borgona else Color(0xFFC9B8BD),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (activo) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun ProductoFeedCard(
    producto: Product,
    nombreArtesano: String?,
    leGustaAlUsuario: Boolean,
    onClick: () -> Unit,
    onLikeClick: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.1f)
                .background(Marfil)
        ) {

            if (producto.imagenes.isNotEmpty()) {

                AsyncImage(
                    model = producto.imagenes.first(),
                    contentDescription = producto.nombre,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.92f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onLikeClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (leGustaAlUsuario) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = "Me gusta",
                    tint = Borgona,
                    modifier = Modifier.size(12.dp)
                )
            }
        }

        Column(modifier = Modifier.padding(8.dp)) {

            Text(
                text = producto.nombre,
                color = Guinda,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = nombreArtesano ?: "",
                color = TextoSecundario,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            ConfianzaDots(
                valor = producto.confianzaPromedio,
                color = AzulAcero
            )
        }
    }
}
