package com.minkaart2.app.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AuthRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

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
}