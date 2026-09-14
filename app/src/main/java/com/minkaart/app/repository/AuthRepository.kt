package com.minkaart2.app.repository

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.minkaart2.app.data.model.User

class AuthRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val functions = Firebase.functions
    private val storage = FirebaseStorage.getInstance()

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

    /**
     * Actualización parcial genérica de campos del propio usuario — la usan tanto la
     * edición de "nombre" (cliente) como la de "bio"/"contactoPublico" (artesano), en vez
     * de tener una función de actualización distinta por cada campo.
     */
    fun actualizarCamposUsuario(
        uid: String,
        campos: Map<String, Any>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {

        firestore.collection("usuarios")
            .document(uid)
            .update(campos)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener {
                onError(
                    it.message ?: "Error al actualizar los datos del usuario"
                )
            }
    }

    fun cerrarSesion() {
        auth.signOut()
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

    // =========================
    // FASE 5 — REGISTRO DE ADMINISTRADOR (validación de PIN contra Firestore)
    // =========================

    /**
     * Crea la cuenta de administrador validando el PIN contra `configuracion/adminAccess`.
     * No pasa por el código de verificación de correo (ver README/plan: es un registro
     * interno, raro, ya protegido por el PIN). La regla de `configuracion/adminAccess`
     * exige `request.auth != null`, así que el PIN solo se puede leer **después** de crear
     * la cuenta de Auth — si el PIN no coincide, se borra esa cuenta recién creada
     * (`currentUser.delete()`) para no dejar un huérfano en Firebase Auth.
     */
    fun registrarAdministrador(
        nombre: String,
        correo: String,
        contraseña: String,
        pinIngresado: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {

        auth.createUserWithEmailAndPassword(correo, contraseña)
            .addOnSuccessListener {

                val uidCreado = auth.currentUser?.uid

                if (uidCreado == null) {
                    onError("No se pudo crear la cuenta de administrador")
                    return@addOnSuccessListener
                }

                firestore.collection("configuracion")
                    .document("adminAccess")
                    .get()
                    .addOnSuccessListener { documento ->

                        val pinReal = documento.getString("pin")

                        if (pinReal == null || pinReal != pinIngresado) {

                            deshacerCuentaCreada()
                            onError("El PIN de administrador es incorrecto")
                            return@addOnSuccessListener
                        }

                        val administrador = User(
                            uid = uidCreado,
                            nombre = nombre,
                            correo = correo,
                            rol = "administrador"
                        )

                        firestore.collection("usuarios")
                            .document(uidCreado)
                            .set(administrador)
                            .addOnSuccessListener { onSuccess() }
                            .addOnFailureListener { error ->

                                deshacerCuentaCreada()

                                onError(
                                    error.message
                                        ?: "Error al guardar la cuenta de administrador"
                                )
                            }
                    }
                    .addOnFailureListener { error ->

                        deshacerCuentaCreada()

                        onError(
                            error.message
                                ?: "No se pudo validar el PIN de administrador"
                        )
                    }
            }
            .addOnFailureListener { error ->

                onError(
                    error.message ?: "Error al crear la cuenta de administrador"
                )
            }
    }

    /**
     * Borra la cuenta de Firebase Auth recién creada cuando el PIN resulta incorrecto o la
     * validación falla — evita dejarla huérfana (sin documento en `usuarios`), el mismo
     * principio que ya sigue el registro normal de cliente/artesano.
     */
    private fun deshacerCuentaCreada() {

        auth.currentUser?.delete()
        auth.signOut()
    }

    // =========================
    // FASE 5 — SOLICITUD Y REVISIÓN DE RUAC
    // =========================

    /**
     * El artesano solicita (o vuelve a solicitar, tras un rechazo) su RUAC: sube el
     * documento de soporte a Storage (mismo path que ya usaba el RUAC del registro) y deja
     * `ruacEstado = "pendiente"` en su propio documento de usuario.
     */
    fun enviarSolicitudRuac(
        uid: String,
        registro: String,
        region: String,
        tecnica: String,
        documentoUri: Uri,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {

        val referenciaDocumento = storage.reference
            .child("ruac")
            .child(uid)
            .child("documento_ruac")

        referenciaDocumento
            .putFile(documentoUri)
            .addOnSuccessListener {

                referenciaDocumento.downloadUrl
                    .addOnSuccessListener { url ->

                        val campos = mapOf(
                            "ruacRegistro" to registro,
                            "ruacRegion" to region,
                            "ruacTecnica" to tecnica,
                            "ruacDocumentoUrl" to url.toString(),
                            "ruacEstado" to "pendiente",
                            "ruacSolicitadoEn" to FieldValue.serverTimestamp()
                        )

                        actualizarCamposUsuario(
                            uid = uid,
                            campos = campos,
                            onSuccess = onSuccess,
                            onError = onError
                        )
                    }
                    .addOnFailureListener { error ->

                        onError(
                            error.message
                                ?: "Error al obtener la URL del documento RUAC"
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

    /**
     * Trae los artesanos con una solicitud de RUAC pendiente. Es una consulta de un solo
     * campo (`ruacEstado == "pendiente"`) para no depender de un índice compuesto — el
     * orden por fecha de solicitud (más antiguos primero) se hace en memoria, en el
     * ViewModel, igual que `obtenerTodosLosProductosActivos` filtra `activo` en memoria.
     */
    fun obtenerSolicitudesRuacPendientes(
        onSuccess: (List<User>) -> Unit,
        onError: (String) -> Unit
    ) {

        firestore.collection("usuarios")
            .whereEqualTo("ruacEstado", "pendiente")
            .get()
            .addOnSuccessListener { resultado ->

                val solicitudes = resultado.documents.mapNotNull { documento ->
                    documento.toObject(User::class.java)
                }

                onSuccess(solicitudes)
            }
            .addOnFailureListener { error ->

                onError(
                    error.message ?: "Error al consultar las solicitudes de RUAC"
                )
            }
    }

    fun aprobarRuac(
        artesanoId: String,
        adminUid: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {

        val ahora = FieldValue.serverTimestamp()

        actualizarCamposUsuario(
            uid = artesanoId,
            campos = mapOf(
                "ruacEstado" to "verificado",
                "ruacVerificadoEn" to ahora,
                "ruacUltimaValidacion" to ahora,
                "ruacRevisadoPor" to adminUid
            ),
            onSuccess = onSuccess,
            onError = onError
        )
    }

    fun rechazarRuac(
        artesanoId: String,
        adminUid: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {

        actualizarCamposUsuario(
            uid = artesanoId,
            campos = mapOf(
                "ruacEstado" to "rechazado",
                "ruacRevisadoPor" to adminUid
            ),
            onSuccess = onSuccess,
            onError = onError
        )
    }
}