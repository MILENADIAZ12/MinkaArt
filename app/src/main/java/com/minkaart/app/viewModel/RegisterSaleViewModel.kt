package com.minkaart2.app.viewModel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.minkaart2.app.data.model.Product
import com.minkaart2.app.data.model.User
import com.minkaart2.app.repository.AuthRepository
import com.minkaart2.app.repository.ProductRepository
import com.minkaart2.app.repository.SaleRepository

class RegisterSaleViewModel : ViewModel() {

    private val productRepository = ProductRepository()
    private val authRepository = AuthRepository()
    private val saleRepository = SaleRepository()

    private val _producto = mutableStateOf<Product?>(null)
    val producto: State<Product?> = _producto

    private val _clientes = mutableStateOf<List<User>>(emptyList())
    val clientes: State<List<User>> = _clientes

    private val _clientesCargados = mutableStateOf(false)
    val clientesCargados: State<Boolean> = _clientesCargados

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
            },
            onError = { mensaje ->

                _cargando.value = false
                onError(mensaje)
            }
        )

        authRepository.obtenerClientes(
            onSuccess = { clientes ->
                _clientes.value = clientes
                _clientesCargados.value = true
            },
            onError = { /* sin lista cargada no se puede validar el correo: no se marca como "No existe" */ }
        )
    }

    fun registrarVenta(
        clienteId: String,
        fechaVenta: Timestamp,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {

        val productoActual = _producto.value
        val artesanoId = FirebaseAuth.getInstance().currentUser?.uid

        if (productoActual == null) {
            onError("No se pudo cargar el producto")
            return
        }

        if (artesanoId == null) {
            onError("No hay un usuario autenticado")
            return
        }

        saleRepository.registrarVenta(
            productoId = productoActual.id,
            artesanoId = artesanoId,
            clienteId = clienteId,
            fechaVenta = fechaVenta,
            onSuccess = onSuccess,
            onError = onError
        )
    }
}
