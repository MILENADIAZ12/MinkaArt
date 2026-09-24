package com.minkaart2.app.viewModel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.minkaart2.app.data.model.Product
import com.minkaart2.app.repository.ProductRepository

class LikesViewModel : ViewModel() {

    private val productRepository = ProductRepository()

    private val _productos = mutableStateOf<List<Product>>(emptyList())
    val productos: State<List<Product>> = _productos

    private val _cargando = mutableStateOf(true)
    val cargando: State<Boolean> = _cargando

    fun cargarLikes(
        onError: (String) -> Unit
    ) {

        _cargando.value = true

        productRepository.obtenerProductosLikeadosPorUsuario(
            onSuccess = { ids ->

                productRepository.obtenerProductosPorIds(
                    ids = ids.toList(),
                    onSuccess = { productos ->

                        _productos.value = productos
                        _cargando.value = false
                    },
                    onError = { mensaje ->

                        _cargando.value = false
                        onError(mensaje)
                    }
                )
            },
            onError = { mensaje ->

                _cargando.value = false
                onError(mensaje)
            }
        )
    }
}
