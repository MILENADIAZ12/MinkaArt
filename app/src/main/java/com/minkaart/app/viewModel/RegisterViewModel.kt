package com.minkaart2.app.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.minkaart2.app.data.model.User

class RegisterViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    fun registrarUsuario(
        nombre: String,
        correo: String,
        contraseña: String,
        rol: String,
        telefono: String = "",
        fechaNacimiento: String = "",
        region: String = "",
        productosQueVende: String = "",
        categoriasArtesanales: List<String> = emptyList(),
        tieneRuac: Boolean = false,
        numeroRuac: String = "",
        ruacUri: Uri? = null,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {

        // =========================
        // VALIDACIONES GENERALES
        // =========================

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

        // =========================
        // VALIDACIONES ARTESANO
        // =========================

        if (rol == "artesano") {

            if (telefono.isBlank()) {
                onError("Ingresa tu número de teléfono")
                return
            }

            if (fechaNacimiento.isBlank()) {
                onError("Selecciona tu fecha de nacimiento")
                return
            }

            if (region.isBlank()) {
                onError("Ingresa tu región")
                return
            }

            if (productosQueVende.isBlank()) {
                onError("Indica qué productos artesanales vendes")
                return
            }

            if (categoriasArtesanales.isEmpty()) {
                onError("Ingresa al menos una categoría artesanal")
                return
            }

            if (tieneRuac) {

                if (numeroRuac.isBlank()) {
                    onError("Ingresa el número de registro RUAC")
                    return
                }

                if (ruacUri == null) {
                    onError("Debes adjuntar tu documento RUAC")
                    return
                }
            }
        }

        // =========================
        // FIREBASE AUTHENTICATION
        // =========================

        auth.createUserWithEmailAndPassword(
            correo,
            contraseña
        ).addOnCompleteListener { task ->

            if (!task.isSuccessful) {

                onError(
                    task.exception?.message
                        ?: "Error al registrar usuario"
                )

                return@addOnCompleteListener
            }

            val uid = auth.currentUser?.uid

            if (uid == null) {

                onError(
                    "No se pudo obtener el usuario registrado"
                )

                return@addOnCompleteListener
            }

            // =========================
            // ARTESANO CON RUAC
            // =========================

            if (
                rol == "artesano" &&
                tieneRuac &&
                ruacUri != null
            ) {

                subirRuac(
                    uid = uid,
                    ruacUri = ruacUri,

                    onSuccess = { ruacUrl ->

                        guardarUsuario(
                            uid = uid,
                            nombre = nombre,
                            correo = correo,
                            rol = rol,
                            telefono = telefono,
                            fechaNacimiento = fechaNacimiento,
                            region = region,
                            productosQueVende = productosQueVende,
                            categoriasArtesanales = categoriasArtesanales,
                            tieneRuac = true,
                            numeroRuac = numeroRuac,
                            ruacUrl = ruacUrl,
                            onSuccess = onSuccess,
                            onError = onError
                        )
                    },

                    onError = onError
                )

            } else {

                // =========================
                // CLIENTE O ARTESANO SIN RUAC
                // =========================

                guardarUsuario(
                    uid = uid,
                    nombre = nombre,
                    correo = correo,
                    rol = rol,
                    telefono = if (rol == "artesano") telefono else "",
                    fechaNacimiento = if (rol == "artesano") {
                        fechaNacimiento
                    } else {
                        ""
                    },
                    region = if (rol == "artesano") region else "",
                    productosQueVende = if (rol == "artesano") {
                        productosQueVende
                    } else {
                        ""
                    },
                    categoriasArtesanales = if (rol == "artesano") {
                        categoriasArtesanales
                    } else {
                        emptyList()
                    },
                    tieneRuac = false,
                    numeroRuac = "",
                    ruacUrl = "",
                    onSuccess = onSuccess,
                    onError = onError
                )
            }
        }
    }

    // =========================
    // SUBIR RUAC A STORAGE
    // =========================

    private fun subirRuac(
        uid: String,
        ruacUri: Uri,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {

        val referenciaRuac = storage.reference
            .child("ruac")
            .child(uid)
            .child("documento_ruac")

        referenciaRuac
            .putFile(ruacUri)
            .addOnSuccessListener {

                referenciaRuac.downloadUrl
                    .addOnSuccessListener { url ->

                        onSuccess(
                            url.toString()
                        )
                    }
                    .addOnFailureListener { error ->

                        onError(
                            error.message
                                ?: "Error al obtener la URL del RUAC"
                        )
                    }
            }
            .addOnFailureListener { error ->

                onError(
                    error.message
                        ?: "Error al subir el documento RUAC"
                )
            }
    }

    // =========================
    // GUARDAR EN FIRESTORE
    // =========================

    private fun guardarUsuario(
        uid: String,
        nombre: String,
        correo: String,
        rol: String,
        telefono: String,
        fechaNacimiento: String,
        region: String,
        productosQueVende: String,
        categoriasArtesanales: List<String>,
        tieneRuac: Boolean,
        numeroRuac: String,
        ruacUrl: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {

        val user = User(
            uid = uid,
            nombre = nombre,
            correo = correo,
            rol = rol,
            telefono = telefono,
            fechaNacimiento = fechaNacimiento,
            region = region,
            productosQueVende = productosQueVende,
            categoriasArtesanales = categoriasArtesanales,
            tieneRuac = tieneRuac,
            numeroRuac = numeroRuac,
            ruacUrl = ruacUrl
        )

        db.collection("usuarios")
            .document(uid)
            .set(user)
            .addOnSuccessListener {

                onSuccess()
            }
            .addOnFailureListener { error ->

                onError(
                    error.message
                        ?: "Error al guardar los datos del usuario"
                )
            }
    }
}