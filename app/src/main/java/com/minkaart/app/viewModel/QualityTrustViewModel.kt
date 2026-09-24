package com.minkaart2.app.viewModel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.minkaart2.app.data.model.ArtesanoStats
import com.minkaart2.app.data.model.Product
import com.minkaart2.app.repository.ProductRepository
import com.minkaart2.app.repository.SaleRepository

class QualityTrustViewModel : ViewModel() {

    private val productRepository = ProductRepository()
    private val saleRepository = SaleRepository()

    private val _producto = mutableStateOf<Product?>(null)
    val producto: State<Product?> = _producto

    private val _statsArtesano = mutableStateOf(ArtesanoStats())
    val statsArtesano: State<ArtesanoStats> = _statsArtesano

    private val _cargando = mutableStateOf(true)
    val cargando: State<Boolean> = _cargando

    fun cargarDatos(
        productoId: String,
        onError: (String) -> Unit
    ) {

        _cargando.value = true

        productRepository.obtenerProductoPorId(
            productoId = productoId,
            onSuccess = { producto ->

                _producto.value = producto
                _cargando.value = false

                saleRepository.obtenerStatsArtesano(
                    artesanoId = producto.artesanoId,
                    onSuccess = { stats -> _statsArtesano.value = stats },
                    onError = { /* la tarjeta de confianza simplemente queda en el estado neutro */ }
                )
            },
            onError = { mensaje ->

                _cargando.value = false
                onError(mensaje)
            }
        )
    }
}
