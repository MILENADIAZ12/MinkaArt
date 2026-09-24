package com.minkaart2.app.viewModel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.minkaart2.app.data.model.Product
import com.minkaart2.app.data.model.Venta
import com.minkaart2.app.repository.AuthRepository
import com.minkaart2.app.repository.ProductRepository
import com.minkaart2.app.repository.SaleRepository

class PurchaseHistoryViewModel : ViewModel() {

    private val saleRepository = SaleRepository()
    private val productRepository = ProductRepository()
    private val authRepository = AuthRepository()

    private val _ventas = mutableStateOf<List<Venta>>(emptyList())
    val ventas: State<List<Venta>> = _ventas

    // productoId -> Product, para no repetir una lectura por cada fila
    private val _productos = mutableStateOf<Map<String, Product>>(emptyMap())
    val productos: State<Map<String, Product>> = _productos

    // artesanoId -> nombre, mismo patrón de caché que HomeClienteViewModel
    private val _nombresArtesanos = mutableStateOf<Map<String, String>>(emptyMap())
    val nombresArtesanos: State<Map<String, String>> = _nombresArtesanos

    private val _cargando = mutableStateOf(true)
    val cargando: State<Boolean> = _cargando

    fun cargarHistorial(onError: (String) -> Unit) {

        val uid = FirebaseAuth.getInstance().currentUser?.uid

        if (uid == null) {
            onError("No hay un usuario autenticado")
            return
        }

        _cargando.value = true

        saleRepository.obtenerVentasDelCliente(
            clienteId = uid,
            onSuccess = { ventas ->

                _ventas.value = ventas
                _cargando.value = false

                productRepository.obtenerProductosPorIds(
                    ids = ventas.map { it.productoId }.distinct(),
                    onSuccess = { productos ->
                        _productos.value = productos.associateBy { it.id }
                    },
                    onError = { /* las filas simplemente no muestran el nombre/imagen del producto */ }
                )

                cargarNombresDeArtesanos(ventas.map { it.artesanoId }.distinct())
            },
            onError = { mensaje ->

                _cargando.value = false
                onError(mensaje)
            }
        )
    }

    private fun cargarNombresDeArtesanos(artesanoIds: List<String>) {

        val idsFaltantes = artesanoIds
            .filter { it.isNotBlank() }
            .filterNot { _nombresArtesanos.value.containsKey(it) }

        idsFaltantes.forEach { artesanoId ->

            authRepository.obtenerUsuarioPorId(
                uid = artesanoId,
                onSuccess = { usuario ->

                    _nombresArtesanos.value = _nombresArtesanos.value + (artesanoId to usuario.nombre)
                },
                onError = { /* esa fila simplemente no muestra el nombre del artesano */ }
            )
        }
    }
}
