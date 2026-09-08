package com.minkaart2.app.viewModel

import androidx.lifecycle.ViewModel
import com.minkaart2.app.repository.AuthRepository

class LoginViewModel : ViewModel() {

    private val authRepository = AuthRepository()

    fun iniciarSesionYObtenerRol(
        correo: String,
        contraseña: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {

        authRepository.iniciarSesionYObtenerRol(
            correo = correo,
            contraseña = contraseña,
            onSuccess = onSuccess,
            onError = onError
        )
    }

    fun recuperarContraseña(
        correo: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {

        authRepository.recuperarContraseña(
            correo = correo,
            onSuccess = onSuccess,
            onError = onError
        )
    }
}