package com.minkaart2.app.viewModel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.minkaart2.app.data.model.User
import com.minkaart2.app.repository.AuthRepository

class AdminRuacListViewModel : ViewModel() {

    private val authRepository = AuthRepository()

    private val _solicitudes = mutableStateOf<List<User>>(emptyList())
    val solicitudes: State<List<User>> = _solicitudes

    private val _cargando = mutableStateOf(true)
    val cargando: State<Boolean> = _cargando

    fun cerrarSesion() {
        authRepository.cerrarSesion()
    }

    fun cargarSolicitudes(onError: (String) -> Unit) {

        _cargando.value = true

        authRepository.obtenerSolicitudesRuacPendientes(
            onSuccess = { usuarios ->

                // Más antiguos primero — se ordena en memoria para no depender de un
                // índice compuesto (ver AuthRepository.obtenerSolicitudesRuacPendientes).
                _solicitudes.value = usuarios.sortedBy {
                    it.ruacSolicitadoEn?.seconds ?: 0L
                }

                _cargando.value = false
            },
            onError = { mensaje ->

                _cargando.value = false
                onError(mensaje)
            }
        )
    }
}
