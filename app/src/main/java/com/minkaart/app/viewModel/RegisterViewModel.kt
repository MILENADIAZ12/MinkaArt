package com.minkaart2.app.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.minkaart2.app.data.model.User
import com.minkaart2.app.repository.AuthRepository

class RegisterViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val authRepository = AuthRepository()

    // =========================
    // DATOS EN ESPERA DE VERIFICACIÓN
    // (se guardan antes de crear la cuenta y se usan recién
    // cuando el usuario confirma el código de verificación)
    // =========================

    private data class DatosRegistroPendiente(
        val nombre: String,
        val correo: String,
        val contraseña: String,
        val rol: String,
        val telefono: String,
        val fechaNacimiento: String,
        val region: String,
        val productosQueVende: String,
        val categoriasArtesanales: List<String>,
        val tieneRuac: Boolean,
        val numeroRuac: String,
        val ruacUri: Uri?
    )

    private var datosPendientes: DatosRegistroPendiente? = null

    // =========================
    // PASO 1: VALIDAR Y ENVIAR CÓDIGO DE VERIFICACIÓN
    // =========================

    fun iniciarRegistro(
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
        // ENVIAR CÓDIGO DE VERIFICACIÓN
        // (la cuenta de Firebase Auth todavía no existe)
        // =========================

        authRepository.enviarCodigoVerificacion(
            correo = correo,

            onSuccess = {

                guardarDatosPendientes(
                    nombre = nombre,
                    correo = correo,
                    contraseña = contraseña,
                    rol = rol,
                    telefono = telefono,
                    fechaNacimiento = fechaNacimiento,
                    region = region,
                    productosQueVende = productosQueVende,
                    categoriasArtesanales = categoriasArtesanales,
                    tieneRuac = tieneRuac,
                    numeroRuac = numeroRuac,
                    ruacUri = ruacUri
                )

                onSuccess()
            },

            onError = onError
        )
    }

    // =========================
    // GUARDA LOS DATOS DEL FORMULARIO EN MEMORIA
    // LISTOS PARA CUANDO SE VERIFIQUE EL CÓDIGO
    // =========================

    private fun guardarDatosPendientes(
        nombre: String,
        correo: String,
        contraseña: String,
        rol: String,
        telefono: String,
        fechaNacimiento: String,
        region: String,
        productosQueVende: String,
        categoriasArtesanales: List<String>,
        tieneRuac: Boolean,
        numeroRuac: String,
        ruacUri: Uri?
    ) {

        datosPendientes = DatosRegistroPendiente(
            nombre = nombre,
            correo = correo,
            contraseña = contraseña,
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
            tieneRuac = rol == "artesano" && tieneRuac,
            numeroRuac = if (rol == "artesano" && tieneRuac) numeroRuac else "",
            ruacUri = if (rol == "artesano" && tieneRuac) ruacUri else null
        )
    }

    // =========================
    // PASO 2: VERIFICAR EL CÓDIGO Y, SOLO SI ES VÁLIDO,
    // CREAR LA CUENTA Y GUARDAR EL USUARIO
    // =========================

    fun confirmarCodigoDeVerificacion(
        codigoIngresado: String,
        onVerificado: () -> Unit,
        onError: (String) -> Unit
    ) {

        val datos = datosPendientes

        if (datos == null) {
            onError("No hay un registro pendiente de confirmar")
            return
        }

        if (codigoIngresado.isBlank()) {
            onError("Ingresa el código de 6 dígitos que recibiste por correo")
            return
        }

        authRepository.verificarCodigo(
            correo = datos.correo,
            codigo = codigoIngresado.trim(),

            onSuccess = {

                crearCuentaYGuardarUsuario(
                    datos = datos,

                    onSuccess = {
                        datosPendientes = null
                        onVerificado()
                    },

                    onError = onError
                )
            },

            // Si el código es inválido, no se pierden los datos del
            // formulario: la usuaria puede reintentar.
            onError = onError
        )
    }

    // =========================
    // CREAR CUENTA EN FIREBASE AUTH, SUBIR EL RUAC
    // SI APLICA Y GUARDAR EL USUARIO EN FIRESTORE
    // =========================

    private fun crearCuentaYGuardarUsuario(
        datos: DatosRegistroPendiente,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {

        auth.createUserWithEmailAndPassword(
            datos.correo,
            datos.contraseña
        ).addOnCompleteListener { task ->

            if (!task.isSuccessful) {

                onError(
                    task.exception?.message
                        ?: "Error al registrar usuario"
                )

                return@addOnCompleteListener
            }

            val usuarioCreado = auth.currentUser

            if (usuarioCreado == null) {

                onError(
                    "No se pudo obtener el usuario registrado"
                )

                return@addOnCompleteListener
            }

            val uid = usuarioCreado.uid

            if (datos.tieneRuac && datos.ruacUri != null) {

                subirRuac(
                    uid = uid,
                    ruacUri = datos.ruacUri,

                    onSuccess = { ruacUrl ->

                        guardarUsuario(
                            uid = uid,
                            datos = datos,
                            ruacUrl = ruacUrl,
                            onSuccess = onSuccess,
                            onError = onError
                        )
                    },

                    onError = onError
                )

            } else {

                guardarUsuario(
                    uid = uid,
                    datos = datos,
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
        datos: DatosRegistroPendiente,
        ruacUrl: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {

        val user = User(
            uid = uid,
            nombre = datos.nombre,
            correo = datos.correo,
            rol = datos.rol,
            telefono = datos.telefono,
            fechaNacimiento = datos.fechaNacimiento,
            region = datos.region,
            productosQueVende = datos.productosQueVende,
            categoriasArtesanales = datos.categoriasArtesanales,
            tieneRuac = datos.tieneRuac,
            numeroRuac = datos.numeroRuac,
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

    // =========================
    // REENVIAR CÓDIGO DE VERIFICACIÓN
    // =========================

    fun reenviarCodigoDeVerificacion(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {

        val datos = datosPendientes

        if (datos == null) {
            onError("No hay un registro pendiente de confirmar")
            return
        }

        authRepository.enviarCodigoVerificacion(
            correo = datos.correo,
            onSuccess = onSuccess,
            onError = onError
        )
    }
}
