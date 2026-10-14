package com.minkaart2.app.ui.publication

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.minkaart2.app.data.CatalogosProducto
import com.minkaart2.app.data.model.Product
import com.minkaart2.app.ui.components.GradientButton
import com.minkaart2.app.ui.components.ImageUploadBox
import com.minkaart2.app.ui.components.MinkaDropdownField
import com.minkaart2.app.ui.components.MinkaFormField
import com.minkaart2.app.viewModel.ProductViewModel

private val Guinda = Color(0xFF3B0F1A)
private val Borgona = Color(0xFF66172E)
private val Marfil = Color(0xFFF2EDE6)
private val LineSoft = Color(0xFFEFE6E8)

@Composable
fun EditProductScreen(
    productId: String,
    navController: NavController
) {

    val productViewModel: ProductViewModel = viewModel()
    val context = LocalContext.current

    var cargando by remember { mutableStateOf(true) }
    var productoOriginal by remember { mutableStateOf<Product?>(null) }

    var nombre by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var precio by remember { mutableStateOf("") }
    var historiaProceso by remember { mutableStateOf("") }
    var materiaPrima by remember { mutableStateOf("") }
    var materiaPrimaOtro by remember { mutableStateOf("") }
    var departamento by remember { mutableStateOf("") }
    var municipio by remember { mutableStateOf("") }
    var categoria by remember { mutableStateOf("") }
    var categoriaOtro by remember { mutableStateOf("") }
    var clasificacion by remember { mutableStateOf(CatalogosProducto.CLASIFICACION_SIN_ESPECIFICAR) }
    var color by remember { mutableStateOf(CatalogosProducto.SIN_ESPECIFICAR) }
    var tamano by remember { mutableStateOf(CatalogosProducto.SIN_ESPECIFICAR) }

    val imagenesExistentes = remember { mutableStateListOf<String>() }
    val imagenesNuevas = remember { mutableStateListOf<Uri>() }

    val selectorImagenes = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->

        val espacioDisponible = ProductViewModel.MAX_IMAGENES -
            (imagenesExistentes.size + imagenesNuevas.size)
        imagenesNuevas.addAll(uris.take(espacioDisponible))
    }

    LaunchedEffect(productId) {

        productViewModel.obtenerProductoPorId(
            productoId = productId,

            onSuccess = { producto ->

                productoOriginal = producto
                nombre = producto.nombre
                descripcion = producto.descripcion
                precio = producto.precio.toString()
                historiaProceso = producto.historiaProceso

                // Materia prima era texto libre antes de este ajuste: si el valor
                // guardado no está en la lista curada actual, se mueve a "Otra" +
                // materiaPrimaOtro para no perderlo (mismo criterio que la migración
                // de regionOrigen más abajo).
                if (producto.materiaPrima.isBlank() ||
                    CatalogosProducto.MATERIAS_PRIMAS.contains(producto.materiaPrima)
                ) {
                    materiaPrima = producto.materiaPrima
                    materiaPrimaOtro = producto.materiaPrimaOtro ?: ""
                } else {
                    materiaPrima = CatalogosProducto.MATERIA_PRIMA_OTRA
                    materiaPrimaOtro = producto.materiaPrima
                }

                if (producto.departamento.isNotBlank()) {

                    // Ya está en el esquema nuevo.
                    departamento = producto.departamento
                    municipio = producto.municipio

                } else if (producto.regionOrigen.isNotBlank()) {

                    // Producto anterior a este ajuste: solo tiene regionOrigen (texto
                    // libre). Si coincide exactamente (sin distinguir mayúsculas) con un
                    // departamento real, se precarga ese departamento; si no, se deja el
                    // dropdown sin seleccionar y ese texto viejo pasa a Municipio para no
                    // perderlo silenciosamente — el artesano lo reorganiza al guardar.
                    val match = CatalogosProducto.DEPARTAMENTOS.firstOrNull {
                        it.equals(producto.regionOrigen.trim(), ignoreCase = true)
                    }

                    if (match != null) {
                        departamento = match
                    } else {
                        municipio = producto.regionOrigen.trim()
                    }
                }

                categoria = producto.categoria
                categoriaOtro = producto.categoriaOtro ?: ""
                clasificacion = producto.clasificacion
                    ?: CatalogosProducto.CLASIFICACION_SIN_ESPECIFICAR
                color = producto.color.ifEmpty { CatalogosProducto.SIN_ESPECIFICAR }
                tamano = producto.tamano.ifEmpty { CatalogosProducto.SIN_ESPECIFICAR }

                imagenesExistentes.clear()
                imagenesExistentes.addAll(producto.imagenes)

                cargando = false
            },

            onError = { mensaje ->

                Toast.makeText(context, mensaje, Toast.LENGTH_LONG).show()
                cargando = false
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

        // Topbar
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
                text = "Editar artesanía",
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
                    text = "* Campos obligatorios",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                ImageUploadBox(
                    onClickAgregar = { selectorImagenes.launch("image/*") },
                    imagenesExistentes = imagenesExistentes,
                    imagenesNuevas = imagenesNuevas,
                    onEliminarExistente = { url -> imagenesExistentes.remove(url) },
                    onEliminarNueva = { uri -> imagenesNuevas.remove(uri) },
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                MinkaFormField(
                    label = "Nombre de la artesanía *",
                    value = nombre,
                    onValueChange = { nombre = it }
                )

                MinkaFormField(
                    label = "Descripción *",
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    singleLine = false,
                    minLines = 3
                )

                MinkaFormField(
                    label = "Precio *",
                    value = precio,
                    onValueChange = { nuevoValor ->

                        if (nuevoValor.all { it.isDigit() }) {
                            precio = nuevoValor
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                MinkaFormField(
                    label = "Historia y proceso de elaboración *",
                    value = historiaProceso,
                    onValueChange = { historiaProceso = it },
                    singleLine = false,
                    minLines = 4
                )

                MinkaDropdownField(
                    label = "Materia prima *",
                    opciones = CatalogosProducto.MATERIAS_PRIMAS,
                    valorSeleccionado = materiaPrima,
                    onSeleccionar = { materiaPrima = it }
                )

                if (materiaPrima == CatalogosProducto.MATERIA_PRIMA_OTRA) {

                    MinkaFormField(
                        label = "Especifica cuál *",
                        value = materiaPrimaOtro,
                        onValueChange = { materiaPrimaOtro = it }
                    )
                }

                MinkaDropdownField(
                    label = "Departamento *",
                    opciones = CatalogosProducto.DEPARTAMENTOS,
                    valorSeleccionado = departamento,
                    onSeleccionar = { departamento = it }
                )

                MinkaFormField(
                    label = "Municipio *",
                    value = municipio,
                    onValueChange = { municipio = it }
                )

                MinkaDropdownField(
                    label = "Categoría *",
                    opciones = CatalogosProducto.CATEGORIAS,
                    valorSeleccionado = categoria,
                    onSeleccionar = { categoria = it }
                )

                if (categoria == CatalogosProducto.CATEGORIA_OTROS) {

                    MinkaFormField(
                        label = "Especifica cuál *",
                        value = categoriaOtro,
                        onValueChange = { categoriaOtro = it }
                    )
                }

                MinkaDropdownField(
                    label = "Clasificación (opcional)",
                    opciones = listOf(CatalogosProducto.CLASIFICACION_SIN_ESPECIFICAR) +
                        CatalogosProducto.CLASIFICACIONES,
                    valorSeleccionado = clasificacion,
                    onSeleccionar = { clasificacion = it }
                )

                MinkaDropdownField(
                    label = "Color (opcional)",
                    opciones = listOf(CatalogosProducto.SIN_ESPECIFICAR) + CatalogosProducto.COLORES,
                    valorSeleccionado = color,
                    onSeleccionar = { color = it }
                )

                MinkaDropdownField(
                    label = "Tamaño (opcional)",
                    opciones = listOf(CatalogosProducto.SIN_ESPECIFICAR) + CatalogosProducto.TAMANOS,
                    valorSeleccionado = tamano,
                    onSeleccionar = { tamano = it }
                )

                Spacer(modifier = Modifier.height(12.dp))

                GradientButton(
                    text = "Guardar cambios",
                    onClick = {

                        val original = productoOriginal

                        if (original == null) {
                            Toast.makeText(
                                context,
                                "No se pudo cargar el producto original",
                                Toast.LENGTH_LONG
                            ).show()
                            return@GradientButton
                        }

                        val precioLong = precio.toLongOrNull() ?: 0L

                        productViewModel.actualizarProducto(
                            productoOriginal = original,
                            nombre = nombre,
                            descripcion = descripcion,
                            precio = precioLong,
                            historiaProceso = historiaProceso,
                            materiaPrima = materiaPrima,
                            materiaPrimaOtro = materiaPrimaOtro,
                            departamento = departamento,
                            municipio = municipio,
                            categoria = categoria,
                            categoriaOtro = categoriaOtro,
                            clasificacion = clasificacion
                                .takeUnless { it == CatalogosProducto.CLASIFICACION_SIN_ESPECIFICAR },
                            color = if (color == CatalogosProducto.SIN_ESPECIFICAR) "" else color,
                            tamano = if (tamano == CatalogosProducto.SIN_ESPECIFICAR) "" else tamano,
                            imagenesExistentes = imagenesExistentes.toList(),
                            imagenesNuevasUris = imagenesNuevas.toList(),

                            onSuccess = {

                                Toast.makeText(
                                    context,
                                    "Producto actualizado correctamente",
                                    Toast.LENGTH_SHORT
                                ).show()

                                navController.popBackStack()
                            },

                            onError = { mensaje ->

                                Toast.makeText(
                                    context,
                                    mensaje,
                                    Toast.LENGTH_LONG
                                ).show()
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
