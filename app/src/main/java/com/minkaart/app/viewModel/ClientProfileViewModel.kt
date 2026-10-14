package com.minkaart2.app.viewModel

import android.net.Uri
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.minkaart2.app.data.model.User
import com.minkaart2.app.repository.AuthRepository

class ClientProfileViewModel : ViewModel() {

    private val authRepository = AuthRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _usuario = mutableStateOf<User?>(null)
    val usuario: State<User?> = _usuario

    private val _cargando = mutableStateOf(true)
    val cargando: State<Boolean> = _cargando

    fun cargarPerfil(
        onError: (String) -> Unit
    ) {

        _cargando.value = true

        authRepository.obtenerUsuarioActual(
            onSuccess = { usuario ->

                _usuario.value = usuario
                _cargando.value = false
            },
            onError = { mensaje ->

                _cargando.value = false
                onError(mensaje)
            }
        )
    }

    fun actualizarNombre(
        nombre: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {

        val uid = auth.currentUser?.uid

        if (uid == null) {
            onError("No hay un usuario autenticado")
            return
        }

        authRepository.actualizarCamposUsuario(
            uid = uid,
            campos = mapOf("nombre" to nombre),
            onSuccess = {

                _usuario.value = _usuario.value?.copy(nombre = nombre)
                onSuccess()
            },
            onError = onError
        )
    }

    /**
     * Guarda nombre, teléfono y ciudad/región del propio usuario (pantalla "Editar
     * información básica", la misma para cliente y artesano). La validación vive aquí.
     * La ciudad/región se guarda en el campo `region` del documento.
     */
    fun actualizarInformacionBasica(
        nombre: String,
        telefono: String,
        ciudad: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {

        val uid = auth.currentUser?.uid

        if (uid == null) {
            onError("No hay un usuario autenticado")
            return
        }

        if (nombre.isBlank()) {
            onError("El nombre no puede estar vacío")
            return
        }

        val nombreFinal = nombre.trim()
        val telefonoFinal = telefono.trim()
        val ciudadFinal = ciudad.trim()

        authRepository.actualizarCamposUsuario(
            uid = uid,
            campos = mapOf(
                "nombre" to nombreFinal,
                "telefono" to telefonoFinal,
                "region" to ciudadFinal
            ),
            onSuccess = {

                _usuario.value = _usuario.value?.copy(
                    nombre = nombreFinal,
                    telefono = telefonoFinal,
                    region = ciudadFinal
                )
                onSuccess()
            },
            onError = onError
        )
    }

    /** Envía al correo de la cuenta el enlace de Firebase para cambiar la contraseña. */
    fun enviarCorreoCambioContraseña(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {

        val correo = auth.currentUser?.email ?: _usuario.value?.correo

        if (correo.isNullOrBlank()) {
            onError("No hay un correo asociado a la cuenta")
            return
        }

        authRepository.recuperarContraseña(
            correo = correo,
            onSuccess = onSuccess,
            onError = onError
        )
    }

    fun actualizarFotoPerfil(
        fotoUri: Uri,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {

        val uid = auth.currentUser?.uid

        if (uid == null) {
            onError("No hay un usuario autenticado")
            return
        }

        authRepository.subirFotoPerfil(
            uid = uid,
            fotoUri = fotoUri,
            onSuccess = { url ->

                _usuario.value = _usuario.value?.copy(fotoUrl = url)
                onSuccess()
            },
            onError = onError
        )
    }

    fun cerrarSesion() {
        authRepository.cerrarSesion()
    }
}
