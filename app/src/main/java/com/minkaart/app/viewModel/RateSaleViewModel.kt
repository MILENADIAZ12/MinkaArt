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

class RateSaleViewModel : ViewModel() {

    private val saleRepository = SaleRepository()
    private val productRepository = ProductRepository()
    private val authRepository = AuthRepository()

    private val _venta = mutableStateOf<Venta?>(null)
    val venta: State<Venta?> = _venta

    private val _producto = mutableStateOf<Product?>(null)
    val producto: State<Product?> = _producto

    private val _nombreArtesano = mutableStateOf("")
    val nombreArtesano: State<String> = _nombreArtesano

    private val _cargando = mutableStateOf(true)
    val cargando: State<Boolean> = _cargando

    /**
     * [onYaValorada] se llama con el `productoId` de la venta cuando ya fue valorada (o no
     * pertenece al usuario actual) — la pantalla debe redirigir a `ProductDetailScreen` en
     * vez de mostrar el formulario de nuevo.
     */
    fun cargarVenta(
        ventaId: String,
        onYaValorada: (productoId: String) -> Unit,
        onError: (String) -> Unit
    ) {

        _cargando.value = true

        saleRepository.obtenerVentaPorId(
            ventaId = ventaId,
            onSuccess = { venta ->

                val uidActual = FirebaseAuth.getInstance().currentUser?.uid

                if (venta.clienteId != uidActual || venta.valorado) {

                    _cargando.value = false
                    onYaValorada(venta.productoId)

                } else {

                    _venta.value = venta

                    productRepository.obtenerProductoPorId(
                        productoId = venta.productoId,
                        onSuccess = { producto ->

                            _producto.value = producto
                            _cargando.value = false

                            authRepository.obtenerUsuarioPorId(
                                uid = venta.artesanoId,
                                onSuccess = { usuario -> _nombreArtesano.value = usuario.nombre },
                                onError = { /* la tarjeta simplemente no muestra el nombre del taller */ }
                            )
                        },
                        onError = { mensaje ->

                            _cargando.value = false
                            onError(mensaje)
                        }
                    )
                }
            },
            onError = { mensaje ->

                _cargando.value = false
                onError(mensaje)
            }
        )
    }

    fun enviarValoracion(
        calidad: String,
        confianza: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {

        val ventaActual = _venta.value

        if (ventaActual == null) {
            onError("No se pudo cargar la venta")
            return
        }

        saleRepository.enviarValoracion(
            venta = ventaActual,
            calidad = calidad,
            confianza = confianza,
            onSuccess = onSuccess,
            onError = onError
        )
    }
}
