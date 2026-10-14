package com.minkaart2.app.viewModel

import androidx.lifecycle.ViewModel
import com.minkaart2.app.R
import com.minkaart2.app.repository.AuthRepository

class AdminRegisterViewModel : ViewModel() {

    private val authRepository = AuthRepository()

    // Los errores de validación llegan como id de recurso (onErrorRes) para que la pantalla
    // los muestre en el idioma de la app; los de Firebase llegan como texto por onError.
    fun registrar(
        nombre: String,
        correo: String,
        contraseña: String,
        confirmarContraseña: String,
        pin: String,
        aceptoTerminos: Boolean,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onErrorRes: (Int) -> Unit
    ) {

        if (nombre.isBlank()) {
            onErrorRes(R.string.err_ingresa_nombre)
            return
        }

        if (correo.isBlank()) {
            onErrorRes(R.string.err_ingresa_correo)
            return
        }

        if (contraseña.length < 6) {
            onErrorRes(R.string.err_contrasena_corta)
            return
        }

        if (contraseña != confirmarContraseña) {
            onErrorRes(R.string.err_contrasenas_no_coinciden)
            return
        }

        if (pin.isBlank()) {
            onErrorRes(R.string.err_pin_admin)
            return
        }

        if (!aceptoTerminos) {
            onErrorRes(R.string.err_acepta_terminos)
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
