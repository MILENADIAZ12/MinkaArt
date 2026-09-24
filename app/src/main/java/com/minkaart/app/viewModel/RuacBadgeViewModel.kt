package com.minkaart2.app.viewModel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.minkaart2.app.data.model.User
import com.minkaart2.app.repository.AuthRepository

class RuacBadgeViewModel : ViewModel() {

    private val authRepository = AuthRepository()

    private val _artesano = mutableStateOf<User?>(null)
    val artesano: State<User?> = _artesano

    private val _cargando = mutableStateOf(true)
    val cargando: State<Boolean> = _cargando

    fun cargarInsignia(
        artesanoId: String,
        onError: (String) -> Unit
    ) {

        _cargando.value = true

        authRepository.obtenerUsuarioPorId(
            uid = artesanoId,
            onSuccess = { usuario ->

                _artesano.value = usuario
                _cargando.value = false
            },
            onError = { mensaje ->

                _cargando.value = false
                onError(mensaje)
            }
        )
    }
}
