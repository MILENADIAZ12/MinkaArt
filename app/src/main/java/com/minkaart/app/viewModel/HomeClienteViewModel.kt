package com.minkaart2.app.viewModel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.minkaart2.app.data.model.Product
import com.minkaart2.app.repository.AuthRepository
import com.minkaart2.app.repository.ProductRepository
import com.minkaart2.app.repository.SaleRepository

class HomeClienteViewModel : ViewModel() {

    private val productRepository = ProductRepository()
    private val authRepository = AuthRepository()
    private val saleRepository = SaleRepository()

    private val _productos = mutableStateOf<List<Product>>(emptyList())
    val productos: State<List<Product>> = _productos

    // ids de producto a los que el usuario actual ya les dio like
    private val _productosLikeados = mutableStateOf<Set<String>>(emptySet())
    val productosLikeados: State<Set<String>> = _productosLikeados

    // artesanoId -> nombre, para no repetir la misma consulta por cada producto
    // del mismo artesano
    private val _nombresArtesanos = mutableStateOf<Map<String, String>>(emptyMap())
    val nombresArtesanos: State<Map<String, String>> = _nombresArtesanos

    // artesanoId -> confianzaPromedio (Fase 6: la confianza es del artesano, no del
    // producto), cacheada igual que nombresArtesanos para no repetir la consulta.
    private val _statsArtesanos = mutableStateOf<Map<String, Double?>>(emptyMap())
    val statsArtesanos: State<Map<String, Double?>> = _statsArtesanos

    private val _cargando = mutableStateOf(false)
    val cargando: State<Boolean> = _cargando

    // Si el usuario autenticado es un artesano "viendo como cliente" — se decide por
    // el rol real en Firestore (usuarios/{uid}), nunca por la ruta, para que un
    // cliente real nunca vea el toggle de cambio de modo.
    private val _esArtesano = mutableStateOf(false)
    val esArtesano: State<Boolean> = _esArtesano

    fun cargarRolUsuarioActual(
        onError: (String) -> Unit
    ) {

        authRepository.obtenerUsuarioActual(
            onSuccess = { usuario -> _esArtesano.value = usuario.rol == "artesano" },
            onError = onError
        )
    }

    fun cargarFeed(
        onError: (String) -> Unit
    ) {

        _cargando.value = true

        productRepository.obtenerTodosLosProductosActivos(
            onSuccess = { productos ->

                _productos.value = productos
                _cargando.value = false

                cargarNombresDeArtesanos(productos)
                cargarStatsDeArtesanos(productos)
            },

            onError = { mensaje ->

                _cargando.value = false
                onError(mensaje)
            }
        )

        productRepository.obtenerProductosLikeadosPorUsuario(
            onSuccess = { ids ->
                _productosLikeados.value = ids
            },
            onError = { /* si falla, el feed se ve igual solo que sin corazones rellenos */ }
        )
    }

    private fun cargarNombresDeArtesanos(productos: List<Product>) {

        val idsFaltantes = productos
            .map { it.artesanoId }
            .filter { it.isNotBlank() }
            .distinct()
            .filterNot { _nombresArtesanos.value.containsKey(it) }

        idsFaltantes.forEach { artesanoId ->

            authRepository.obtenerUsuarioPorId(
                uid = artesanoId,
                onSuccess = { usuario ->

                    _nombresArtesanos.value = _nombresArtesanos.value + (artesanoId to usuario.nombre)
                },
                onError = { /* si falla, esa tarjeta simplemente no muestra el nombre del taller */ }
            )
        }
    }

    private fun cargarStatsDeArtesanos(productos: List<Product>) {

        val idsFaltantes = productos
            .map { it.artesanoId }
            .filter { it.isNotBlank() }
            .distinct()
            .filterNot { _statsArtesanos.value.containsKey(it) }

        idsFaltantes.forEach { artesanoId ->

            saleRepository.obtenerStatsArtesano(
                artesanoId = artesanoId,
                onSuccess = { stats ->

                    _statsArtesanos.value = _statsArtesanos.value +
                        (artesanoId to stats.confianzaPromedio)
                },
                onError = { /* esa tarjeta simplemente muestra los puntos de confianza en gris */ }
            )
        }
    }

    fun alternarLike(
        productoId: String,
        onError: (String) -> Unit
    ) {

        productRepository.alternarLike(
            productoId = productoId,

            onSuccess = { nuevoLikeado ->

                _productosLikeados.value = if (nuevoLikeado) {
                    _productosLikeados.value + productoId
                } else {
                    _productosLikeados.value - productoId
                }
            },

            onError = onError
        )
    }
}
