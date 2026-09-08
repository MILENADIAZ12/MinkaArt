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

    fun publicarProducto(
        nombre: String,
        descripcion: String,
        precio: Long,
        historiaProceso: String,
        regionOrigen: String,
        imagenUri: Uri?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {

        if (nombre.isBlank()) {
            onError("Ingresa el nombre del producto")
            return
        }

        if (descripcion.isBlank()) {
            onError("Ingresa la descripción del producto")
            return
        }

        if (precio <= 0) {
            onError("Ingresa un precio válido")
            return
        }

        if (historiaProceso.isBlank()) {
            onError("Ingresa la historia o proceso de elaboración")
            return
        }

        if (regionOrigen.isBlank()) {
            onError("Ingresa la región de origen")
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
            regionOrigen = regionOrigen
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
}