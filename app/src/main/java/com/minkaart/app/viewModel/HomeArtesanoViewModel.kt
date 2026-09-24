package com.minkaart2.app.viewModel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.minkaart2.app.repository.AuthRepository

class HomeArtesanoViewModel : ViewModel() {

    private val authRepository = AuthRepository()

    private val _nombreArtesano = mutableStateOf("")
    val nombreArtesano: State<String> = _nombreArtesano

    fun cargarDatosArtesano(
        onError: (String) -> Unit
    ) {

        authRepository.obtenerUsuarioActual(
            onSuccess = { usuario ->
                _nombreArtesano.value = usuario.nombre
            },
            onError = onError
        )
    }
}
