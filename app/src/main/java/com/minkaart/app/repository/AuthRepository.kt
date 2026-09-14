package com.minkaart2.app.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.minkaart2.app.data.model.User

class AuthRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val functions = Firebase.functions

    fun iniciarSesion(
        correo: String,
        contraseña: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        auth.signInWithEmailAndPassword(correo, contraseña)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener {
                onError(it.message ?: "Error al iniciar sesión")
            }
    }

    fun obtenerRolUsuario(
        uid: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        firestore.collection("usuarios")
            .document(uid)
            .get()
            .addOnSuccessListener { documento ->

                val rol = documento.getString("rol")

                if (rol != null) {
                    onSuccess(rol)
                } else {
                    onError("No se encontró el rol del usuario")
                }
            }
            .addOnFailureListener {
                onError(it.message ?: "Error al consultar el rol")
            }
    }

    fun iniciarSesionYObtenerRol(
        correo: String,
        contraseña: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {

        iniciarSesion(
            correo = correo,
            contraseña = contraseña,

            onSuccess = {

                val uid = auth.currentUser?.uid

                if (uid == null) {
                    onError("No se pudo obtener el usuario autenticado")
                    return@iniciarSesion
                }

                obtenerRolUsuario(
                    uid = uid,
                    onSuccess = onSuccess,
                    onError = onError
                )
            },

            onError = onError
        )
    }

    fun obtenerUsuarioActual(
        onSuccess: (User) -> Unit,
        onError: (String) -> Unit
    ) {

        val usuarioActual = auth.currentUser

        if (usuarioActual == null) {
            onError("No hay un usuario autenticado")
            return
        }

        firestore.collection("usuarios")
            .document(usuarioActual.uid)
            .get()
            .addOnSuccessListener { documento ->

                val usuario = documento.toObject(User::class.java)

                if (usuario == null) {
                    onError("No se encontró la información del usuario")
                } else {
                    onSuccess(usuario)
                }
            }
            .addOnFailureListener {
                onError(
                    it.message ?: "Error al consultar los datos del usuario"
                )
            }
    }

    fun obtenerUsuarioPorId(
        uid: String,
        onSuccess: (User) -> Unit,
        onError: (String) -> Unit
    ) {

        firestore.collection("usuarios")
            .document(uid)
            .get()
            .addOnSuccessListener { documento ->

                val usuario = documento.toObject(User::class.java)

                if (usuario == null) {
                    onError("No se encontró la información del artesano")
                } else {
                    onSuccess(usuario)
                }
            }
            .addOnFailureListener {
                onError(
                    it.message ?: "Error al consultar los datos del artesano"
                )
            }
    }

    fun recuperarContraseña(
        correo: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        auth.sendPasswordResetEmail(correo)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener {
                onError(
                    it.message
                        ?: "Error al enviar el correo de recuperación"
                )
            }
    }

    // =========================
    // VERIFICACIÓN DE CORREO POR CÓDIGO DE 6 DÍGITOS
    // (Cloud Functions: enviarCodigoVerificacion / verificarCodigo)
    // =========================

    fun enviarCodigoVerificacion(
        correo: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {

        functions
            .getHttpsCallable("enviarCodigoVerificacion")
            .call(hashMapOf("correo" to correo))
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { error ->
                onError(mapearErrorEnvioCodigo(error))
            }
    }

    fun verificarCodigo(
        correo: String,
        codigo: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {

        functions
            .getHttpsCallable("verificarCodigo")
            .call(
                hashMapOf(
                    "correo" to correo,
                    "codigo" to codigo
                )
            )
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { error ->
                onError(mapearErrorVerificacionCodigo(error))
            }
    }

    private fun mapearErrorEnvioCodigo(error: Exception): String {

        val excepcion = error as? FirebaseFunctionsException
            ?: return error.message ?: "No pudimos enviar el código. Intenta de nuevo."

        return when (excepcion.code) {
            FirebaseFunctionsException.Code.INVALID_ARGUMENT ->
                "El correo electrónico no es válido"

            FirebaseFunctionsException.Code.INTERNAL ->
                "No pudimos enviar el correo. Intenta de nuevo."

            FirebaseFunctionsException.Code.UNAVAILABLE,
            FirebaseFunctionsException.Code.DEADLINE_EXCEEDED ->
                "Revisa tu conexión a internet e intenta de nuevo"

            else -> excepcion.message ?: "No pudimos enviar el código. Intenta de nuevo."
        }
    }

    private fun mapearErrorVerificacionCodigo(error: Exception): String {

        val excepcion = error as? FirebaseFunctionsException
            ?: return error.message ?: "No pudimos verificar el código. Intenta de nuevo."

        return when (excepcion.code) {
            FirebaseFunctionsException.Code.INVALID_ARGUMENT ->
                "El código es incorrecto. Revísalo e intenta de nuevo."

            FirebaseFunctionsException.Code.NOT_FOUND ->
                "No hay un código pendiente para este correo. Solicita uno nuevo."

            FirebaseFunctionsException.Code.RESOURCE_EXHAUSTED ->
                "Demasiados intentos fallidos. Solicita un código nuevo."

            FirebaseFunctionsException.Code.DEADLINE_EXCEEDED ->
                "El código expiró. Solicita uno nuevo."

            FirebaseFunctionsException.Code.UNAVAILABLE ->
                "Revisa tu conexión a internet e intenta de nuevo"

            else -> excepcion.message ?: "No pudimos verificar el código. Intenta de nuevo."
        }
    }
}