package com.minkaart2.app.viewModel

import android.net.Uri
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.minkaart2.app.data.model.Product
import com.minkaart2.app.repository.ProductRepository

class ProductViewModel : ViewModel() {

    private val productRepository = ProductRepository()

    // Lista de publicaciones del artesano actual
    private val _misProductos = mutableStateOf<List<Product>>(emptyList())
    val misProductos: State<List<Product>> = _misProductos

    // Indica si estamos consultando Firestore
    private val _cargando = mutableStateOf(false)
    val cargando: State<Boolean> = _cargando

    private fun validarCamposProducto(
        nombre: String,
        descripcion: String,
        precio: Long,
        historiaProceso: String,
        regionOrigen: String,
        onError: (String) -> Unit
    ): Boolean {

        if (nombre.isBlank()) {
            onError("Ingresa el nombre del producto")
            return false
        }

        if (descripcion.isBlank()) {
            onError("Ingresa la descripción del producto")
            return false
        }

        if (precio <= 0) {
            onError("Ingresa un precio válido")
            return false
        }

        if (historiaProceso.isBlank()) {
            onError("Ingresa la historia o proceso de elaboración")
            return false
        }

        if (regionOrigen.isBlank()) {
            onError("Ingresa la región de origen")
            return false
        }

        return true
    }

    fun publicarProducto(
        nombre: String,
        descripcion: String,
        precio: Long,
        historiaProceso: String,
        regionOrigen: String,
        categoria: String,
        imagenUri: Uri?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {

        val camposValidos = validarCamposProducto(
            nombre = nombre,
            descripcion = descripcion,
            precio = precio,
            historiaProceso = historiaProceso,
            regionOrigen = regionOrigen,
            onError = onError
        )

        if (!camposValidos) {
            return
        }

        if (imagenUri == null) {
            onError("Debes seleccionar una imagen del producto")
            return
        }

        val producto = Product(
            nombre = nombre,
            descripcion = descripcion,
            precio = precio,
            historiaProceso = historiaProceso,
            regionOrigen = regionOrigen,
            categoria = categoria.trim()
        )

        productRepository.guardarProductoConImagen(
            producto = producto,
            imagenUri = imagenUri,
            onSuccess = onSuccess,
            onError = onError
        )
    }

    fun cargarMisProductos(
        onError: (String) -> Unit
    ) {

        _cargando.value = true

        productRepository.obtenerMisProductos(
            onSuccess = { productos ->

                _misProductos.value = productos
                _cargando.value = false
            },

            onError = { mensaje ->

                _cargando.value = false
                onError(mensaje)
            }
        )
    }

    fun obtenerProductoPorId(
        productoId: String,
        onSuccess: (Product) -> Unit,
        onError: (String) -> Unit
    ) {

        productRepository.obtenerProductoPorId(
            productoId = productoId,
            onSuccess = onSuccess,
            onError = onError
        )
    }

    fun actualizarProducto(
        productoOriginal: Product,
        nombre: String,
        descripcion: String,
        precio: Long,
        historiaProceso: String,
        regionOrigen: String,
        categoria: String,
        nuevaImagenUri: Uri?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {

        val camposValidos = validarCamposProducto(
            nombre = nombre,
            descripcion = descripcion,
            precio = precio,
            historiaProceso = historiaProceso,
            regionOrigen = regionOrigen,
            onError = onError
        )

        if (!camposValidos) {
            return
        }

        // .copy() sobre el producto original: conserva vistas, likes, activo,
        // fechaPublicacion, calidadPromedio/confianzaPromedio e imagenes tal cual, y solo
        // reemplaza los campos que el formulario de edición sí controla.
        val producto = productoOriginal.copy(
            nombre = nombre,
            descripcion = descripcion,
            precio = precio,
            historiaProceso = historiaProceso,
            regionOrigen = regionOrigen,
            categoria = categoria.trim()
        )

        productRepository.actualizarProducto(
            productoId = productoOriginal.id,
            producto = producto,
            nuevaImagenUri = nuevaImagenUri,
            onSuccess = onSuccess,
            onError = onError
        )
    }

    fun eliminarProducto(
        productoId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {

        productRepository.eliminarProducto(
            productoId = productoId,
            onSuccess = {

                _misProductos.value = _misProductos.value.filterNot { producto ->
                    producto.id == productoId
                }

                onSuccess()
            },
            onError = onError
        )
    }

    fun cambiarEstadoProducto(
        productoId: String,
        activo: Boolean,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {

        productRepository.cambiarEstadoProducto(
            productoId = productoId,
            activo = activo,
            onSuccess = {

                _misProductos.value = _misProductos.value.map { producto ->

                    if (producto.id == productoId) {
                        producto.copy(activo = activo)
                    } else {
                        producto
                    }
                }

                onSuccess()
            },
            onError = onError
        )
    }
}
