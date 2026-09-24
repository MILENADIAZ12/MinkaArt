package com.minkaart2.app.viewModel

import android.net.Uri
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.minkaart2.app.data.model.User
import com.minkaart2.app.repository.AuthRepository

class RequestRuacViewModel : ViewModel() {

    private val authRepository = AuthRepository()

    // Datos actuales del usuario (si ya tenía un RUAC solicitado/verificado/rechazado):
    // sirve tanto para "solicitar por primera vez" (queda en null, formulario vacío) como
    // para "Editar RUAC" (se precargan los campos).
    private val _usuarioActual = mutableStateOf<User?>(null)
    val usuarioActual: State<User?> = _usuarioActual

    private val _cargando = mutableStateOf(true)
    val cargando: State<Boolean> = _cargando

    fun cargarDatosActuales(onError: (String) -> Unit) {

        _cargando.value = true

        authRepository.obtenerUsuarioActual(
            onSuccess = { usuario ->

                _usuarioActual.value = usuario
                _cargando.value = false
            },
            onError = { mensaje ->

                _cargando.value = false
                onError(mensaje)
            }
        )
    }

    fun enviarSolicitud(
        registro: String,
        region: String,
        tecnica: String,
        documentoUri: Uri?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {

        val uid = FirebaseAuth.getInstance().currentUser?.uid

        if (uid == null) {
            onError("No hay un usuario autenticado")
            return
        }

        if (registro.isBlank()) {
            onError("Ingresa el número de registro RUAC")
            return
        }

        if (region.isBlank()) {
            onError("Ingresa la región registrada")
            return
        }

        if (tecnica.isBlank()) {
            onError("Ingresa la técnica registrada")
            return
        }

        authRepository.enviarSolicitudRuac(
            uid = uid,
            registro = registro,
            region = region,
            tecnica = tecnica,
            documentoUri = documentoUri,
            documentoUrlExistente = _usuarioActual.value?.ruacDocumentoUrl,
            onSuccess = onSuccess,
            onError = onError
        )
    }
}
