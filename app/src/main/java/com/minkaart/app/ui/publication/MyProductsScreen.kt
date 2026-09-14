package com.minkaart2.app.ui.publication

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.minkaart2.app.data.model.Product
import com.minkaart2.app.viewModel.ProductViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Guinda = Color(0xFF3B0F1A)
private val Borgona = Color(0xFF66172E)
private val Marfil = Color(0xFFF2EDE6)
private val TextoSecundario = Color(0xFF8A7480)
private val LineSoft = Color(0xFFEFE6E8)

@Composable
fun MyProductsScreen(
    onEditarProducto: (String) -> Unit,
    onNuevoProducto: () -> Unit
) {

    val productViewModel: ProductViewModel = viewModel()

    val productos by productViewModel.misProductos
    val cargando by productViewModel.cargando

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var productoAEliminar by remember { mutableStateOf<Product?>(null) }

    LaunchedEffect(Unit) {

        productViewModel.cargarMisProductos(
            onError = { mensaje ->

                Toast.makeText(
                    context,
                    mensaje,
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    // Refresca la lista cada vez que la pantalla vuelve a primer plano (por ejemplo, al
    // regresar de Editar Producto), porque Navigation Compose mantiene esta pantalla y su
    // ProductViewModel vivos en el back stack: sin esto, un LaunchedEffect(Unit) no
    // volvería a ejecutarse y se verían datos desactualizados.
    DisposableEffect(lifecycleOwner) {

        val observer = LifecycleEventObserver { _, event ->

            if (event == Lifecycle.Event.ON_RESUME) {

                productViewModel.cargarMisProductos(
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

            Text(
                text = "Mis publicaciones",
                color = Guinda,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Borgona)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onNuevoProducto
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )

                Text(
                    text = "Nueva",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
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

                Text(
                    text = "Cargando publicaciones...",
                    color = TextoSecundario,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

        } else if (productos.isEmpty()) {

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    text = "Aún no tienes productos publicados",
                    color = TextoSecundario,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

        } else {

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                // contentPadding en vez de Modifier.padding: así el primer y último
                // producto conservan el margen también al hacer scroll, no solo en reposo.
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                items(
                    items = productos,
                    key = { producto -> producto.id }
                ) { producto ->

                    ProductRow(
                        producto = producto,
                        onEditar = { onEditarProducto(producto.id) },
                        onEliminar = { productoAEliminar = producto },
                        onCambiarEstado = { nuevoActivo ->

                            productViewModel.cambiarEstadoProducto(
                                productoId = producto.id,
                                activo = nuevoActivo,
                                onSuccess = {},
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

    val productoParaEliminar = productoAEliminar

    if (productoParaEliminar != null) {

        AlertDialog(
            onDismissRequest = { productoAEliminar = null },
            title = { Text("Eliminar producto") },
            text = { Text("¿Estás seguro de que deseas eliminar este producto?") },
            confirmButton = {

                TextButton(
                    onClick = {

                        val idAEliminar = productoParaEliminar.id
                        productoAEliminar = null

                        productViewModel.eliminarProducto(
                            productoId = idAEliminar,

                            onSuccess = {

                                Toast.makeText(
                                    context,
                                    "Producto eliminado",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },

                            onError = { mensaje ->

                                Toast.makeText(
                                    context,
                                    mensaje,
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        )
                    }
                ) {
                    Text("Eliminar", color = Borgona)
                }
            },
            dismissButton = {

                TextButton(
                    onClick = { productoAEliminar = null }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun ProductRow(
    producto: Product,
    onEditar: () -> Unit,
    onEliminar: () -> Unit,
    onCambiarEstado: (Boolean) -> Unit
) {

    var menuAbierto by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(13.dp))
            .border(1.dp, LineSoft, RoundedCornerShape(13.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.Top
    ) {

        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(9.dp))
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
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {

            Text(
                text = producto.nombre,
                color = Guinda,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = producto.descripcion,
                color = TextoSecundario,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = formatearPrecio(producto.precio),
                color = Borgona,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "${producto.vistas} vistas · ${producto.likes} likes",
                color = TextoSecundario,
                style = MaterialTheme.typography.bodySmall
            )

            if (producto.fechaPublicacion != 0L) {

                Text(
                    text = "Publicado el ${formatearFecha(producto.fechaPublicacion)}",
                    color = TextoSecundario.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {

            EstadoBadge(activo = producto.activo)

            Spacer(modifier = Modifier.height(2.dp))

            Box {

                IconButton(onClick = { menuAbierto = true }) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "Más opciones",
                        tint = TextoSecundario
                    )
                }

                DropdownMenu(
                    expanded = menuAbierto,
                    onDismissRequest = { menuAbierto = false }
                ) {

                    DropdownMenuItem(
                        text = { Text("Editar") },
                        leadingIcon = {
                            Icon(Icons.Filled.Edit, contentDescription = null)
                        },
                        onClick = {
                            menuAbierto = false
                            onEditar()
                        }
                    )

                    DropdownMenuItem(
                        text = {
                            Text(if (producto.activo) "Pausar publicación" else "Activar publicación")
                        },
                        leadingIcon = {
                            Icon(
                                if (producto.activo) Icons.Filled.Close else Icons.Filled.CheckCircle,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            menuAbierto = false
                            onCambiarEstado(!producto.activo)
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("Eliminar") },
                        leadingIcon = {
                            Icon(Icons.Filled.Delete, contentDescription = null)
                        },
                        onClick = {
                            menuAbierto = false
                            onEliminar()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun EstadoBadge(activo: Boolean) {

    val fondo = if (activo) Color(0xFFDFE7F0) else Color(0xFFF2DFE4)
    val texto = if (activo) Color(0xFF55708E) else Borgona

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(fondo)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = if (activo) "Activo" else "Pausado",
            color = texto,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun formatearPrecio(precio: Long): String {

    val formato = NumberFormat.getNumberInstance(Locale("es", "CO"))
    return "$${formato.format(precio)}"
}

private fun formatearFecha(millis: Long): String {

    val formato = SimpleDateFormat("dd/MM/yyyy", Locale("es", "CO"))
    return formato.format(Date(millis))
}
