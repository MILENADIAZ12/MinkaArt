package com.minkaart2.app.viewModel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.minkaart2.app.data.model.User
import com.minkaart2.app.repository.AuthRepository

class AdminRuacReviewViewModel : ViewModel() {

    private val authRepository = AuthRepository()

    private val _artesano = mutableStateOf<User?>(null)
    val artesano: State<User?> = _artesano

    private val _cargando = mutableStateOf(true)
    val cargando: State<Boolean> = _cargando

    fun cargarArtesano(
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

    fun aprobar(
        artesanoId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {

        val adminUid = FirebaseAuth.getInstance().currentUser?.uid

        if (adminUid == null) {
            onError("No hay una sesión de administrador activa")
            return
        }

        authRepository.aprobarRuac(
            artesanoId = artesanoId,
            adminUid = adminUid,
            onSuccess = onSuccess,
            onError = onError
        )
    }

    fun rechazar(
        artesanoId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {

        val adminUid = FirebaseAuth.getInstance().currentUser?.uid

        if (adminUid == null) {
            onError("No hay una sesión de administrador activa")
            return
        }

        authRepository.rechazarRuac(
            artesanoId = artesanoId,
            adminUid = adminUid,
            onSuccess = onSuccess,
            onError = onError
        )
    }
}
