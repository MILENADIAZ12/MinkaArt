package com.minkaart2.app.viewModel

import androidx.lifecycle.ViewModel
import com.minkaart2.app.repository.AuthRepository

class AdminRegisterViewModel : ViewModel() {

    private val authRepository = AuthRepository()

    fun registrar(
        nombre: String,
        correo: String,
        contraseña: String,
        pin: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {

        if (nombre.isBlank()) {
            onError("Ingresa tu nombre")
            return
        }

        if (correo.isBlank()) {
            onError("Ingresa tu correo electrónico")
            return
        }

        if (contraseña.length < 6) {
            onError("La contraseña debe tener mínimo 6 caracteres")
            return
        }

        if (pin.isBlank()) {
            onError("Ingresa el PIN de acceso de administrador")
            return
        }

        authRepository.registrarAdministrador(
            nombre = nombre,
            correo = correo,
            contraseña = contraseña,
            pinIngresado = pin,
            onSuccess = onSuccess,
            onError = onError
        )
    }
}
