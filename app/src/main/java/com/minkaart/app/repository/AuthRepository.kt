package com.minkaart2.app.repository

import android.content.Context
import android.net.Uri
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.credentials.CredentialManagerCallback
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider
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

    /**
     * Guarda/actualiza el token de FCM del usuario actual — llamado al abrir la app con
     * sesión iniciada y desde `MinkaMessagingService.onNewToken` cuando el token rota.
     */
    fun guardarTokenNotificaciones(
        uid: String,
        token: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {

        actualizarCamposUsuario(
            uid = uid,
            campos = mapOf("fcmToken" to token),
            onSuccess = onSuccess,
            onError = onError
        )
    }

    /**
     * Todos los usuarios con rol "cliente" — usado por el buscador de
     * `RegisterSaleScreen`, que filtra por nombre/correo en memoria (mismo patrón que el
     * buscador de productos).
     */
    fun obtenerClientes(
        onSuccess: (List<User>) -> Unit,
        onError: (String) -> Unit
    ) {

        firestore.collection("usuarios")
            .whereEqualTo("rol", "cliente")
            .get()
            .addOnSuccessListener { resultado ->

                val clientes = resultado.documents.mapNotNull { documento ->
                    documento.toObject(User::class.java)
                }

                onSuccess(clientes)
            }
            .addOnFailureListener { error ->
                onError(error.message ?: "Error al consultar los clientes")
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

    /**
     * Sube la foto de perfil a Storage (usuarios/{uid}/foto_perfil, un solo archivo —
     * cada subida nueva reemplaza a la anterior) y guarda la URL en
     * usuarios/{uid}.fotoUrl. Mismo patrón subir-y-luego-guardar-URL que
     * enviarSolicitudRuac. Sirve tanto para cliente como para artesano.
     */
    fun subirFotoPerfil(
        uid: String,
        fotoUri: Uri,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {

        val referenciaFoto = storage.reference
            .child("usuarios")
            .child(uid)
            .child("foto_perfil")

        referenciaFoto
            .putFile(fotoUri)
            .addOnSuccessListener {

                referenciaFoto.downloadUrl
                    .addOnSuccessListener { url ->

                        actualizarCamposUsuario(
                            uid = uid,
                            campos = mapOf("fotoUrl" to url.toString()),
                            onSuccess = { onSuccess(url.toString()) },
                            onError = onError
                        )
                    }
                    .addOnFailureListener { error ->
                        onError(error.message ?: "Error al obtener la URL de la foto de perfil")
                    }
            }
            .addOnFailureListener { error ->
                onError(error.message ?: "Error al subir la foto de perfil")
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
    // GOOGLE (Credential Manager + GoogleAuthProvider de Firebase Auth)
    // =========================

    /**
     * Pide a Credential Manager la cuenta de Google y devuelve su ID token. Usa la versión
     * con callback (`getCredentialAsync`) en vez de la `suspend`, para mantener la
     * convención del proyecto de no usar corrutinas. [context] debe ser el de la Activity.
     */
    fun obtenerTokenGoogle(
        context: Context,
        webClientId: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {

        val opcionGoogle = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .build()

        val solicitud = GetCredentialRequest.Builder()
            .addCredentialOption(opcionGoogle)
            .build()

        CredentialManager.create(context).getCredentialAsync(
            context,
            solicitud,
            CancellationSignal(),
            ContextCompat.getMainExecutor(context),
            object : CredentialManagerCallback<GetCredentialResponse, GetCredentialException> {

                override fun onResult(result: GetCredentialResponse) {

                    val credencial = result.credential

                    if (
                        credencial is CustomCredential &&
                        credencial.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                    ) {
                        try {
                            onSuccess(GoogleIdTokenCredential.createFrom(credencial.data).idToken)
                        } catch (error: GoogleIdTokenParsingException) {
                            onError(error.message ?: "No se pudo leer la cuenta de Google")
                        }
                    } else {
                        onError("No se pudo obtener la cuenta de Google")
                    }
                }

                override fun onError(e: GetCredentialException) {

                    onError(
                        when (e) {
                            is GetCredentialCancellationException ->
                                "Inicio de sesión con Google cancelado"

                            is NoCredentialException ->
                                "No hay una cuenta de Google disponible en este dispositivo"

                            else -> e.message ?: "Error al continuar con Google"
                        }
                    )
                }
            }
        )
    }

    /**
     * Autentica en Firebase con el ID token de Google. Si el correo ya existe con otro
     * método (correo y contraseña) devuelve un mensaje claro en vez del error genérico.
     */
    fun iniciarSesionConGoogle(
        idToken: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {

        auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null))
            .addOnSuccessListener { resultado ->

                val uid = resultado.user?.uid

                if (uid == null) {
                    onError("No se pudo obtener el usuario autenticado")
                } else {
                    onSuccess(uid)
                }
            }
            .addOnFailureListener { error ->

                onError(
                    if (error is FirebaseAuthUserCollisionException) {
                        "Este correo ya está registrado con contraseña, " +
                                "inicia sesión con tu contraseña"
                    } else {
                        error.message ?: "Error al continuar con Google"
                    }
                )
            }
    }

    /** Rol del usuario si ya tiene documento en `usuarios`; si no, [onNoExiste]. */
    fun consultarRolSiExiste(
        uid: String,
        onExiste: (String) -> Unit,
        onNoExiste: () -> Unit,
        onError: (String) -> Unit
    ) {

        firestore.collection("usuarios")
            .document(uid)
            .get()
            .addOnSuccessListener { documento ->

                val rol = documento.getString("rol")

                if (documento.exists() && rol != null) {
                    onExiste(rol)
                } else {
                    onNoExiste()
                }
            }
            .addOnFailureListener {
                onError(it.message ?: "Error al consultar el usuario")
            }
    }

    /** Nombre y correo de la cuenta autenticada (los que trae Google). */
    fun datosCuentaActual(): Pair<String, String> {

        val usuario = auth.currentUser

        return Pair(usuario?.displayName ?: "", usuario?.email ?: "")
    }

    /**
     * Crea el documento de un usuario cliente que acaba de autenticarse con Google
     * (el artesano pasa por `RegisterViewModel.completarRegistroGoogle`, que pide sus
     * datos obligatorios). Nunca crea un administrador.
     */
    fun crearClienteConGoogle(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {

        val usuario = auth.currentUser

        if (usuario == null) {
            onError("No se pudo obtener el usuario autenticado")
            return
        }

        val cliente = User(
            uid = usuario.uid,
            nombre = usuario.displayName ?: "",
            correo = usuario.email ?: "",
            rol = "cliente"
        )

        firestore.collection("usuarios")
            .document(usuario.uid)
            .set(cliente)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener {
                onError(it.message ?: "Error al guardar los datos del usuario")
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
     * El artesano solicita (o vuelve a solicitar, tras un rechazo, o edita uno ya
     * verificado — ver [RequestRuacViewModel]) su RUAC: sube el documento de soporte a
     * Storage (mismo path que ya usaba el RUAC del registro) y deja `ruacEstado =
     * "pendiente"` en su propio documento de usuario. Si no se adjunta un documento nuevo
     * ([documentoUri] es `null`) pero ya existe uno guardado ([documentoUrlExistente]), se
     * conserva ese en vez de exigir volver a subirlo — así "Editar RUAC" no obliga a
     * resubir el archivo si el artesano solo corrigió un dato de texto.
     */
    fun enviarSolicitudRuac(
        uid: String,
        registro: String,
        region: String,
        tecnica: String,
        documentoUri: Uri?,
        documentoUrlExistente: String?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {

        if (documentoUri == null) {

            if (documentoUrlExistente.isNullOrBlank()) {
                onError("Adjunta el documento o foto de soporte del RUAC")
                return
            }

            guardarSolicitudRuac(
                uid = uid,
                registro = registro,
                region = region,
                tecnica = tecnica,
                documentoUrl = documentoUrlExistente,
                onSuccess = onSuccess,
                onError = onError
            )

            return
        }

        val referenciaDocumento = storage.reference
            .child("ruac")
            .child(uid)
            .child("documento_ruac")

        referenciaDocumento
            .putFile(documentoUri)
            .addOnSuccessListener {

                referenciaDocumento.downloadUrl
                    .addOnSuccessListener { url ->

                        guardarSolicitudRuac(
                            uid = uid,
                            registro = registro,
                            region = region,
                            tecnica = tecnica,
                            documentoUrl = url.toString(),
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

    private fun guardarSolicitudRuac(
        uid: String,
        registro: String,
        region: String,
        tecnica: String,
        documentoUrl: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {

        val campos = mapOf(
            "ruacRegistro" to registro,
            "ruacRegion" to region,
            "ruacTecnica" to tecnica,
            "ruacDocumentoUrl" to documentoUrl,
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

    /**
     * El artesano retira su propio RUAC verificado — a diferencia de rechazar (que hace un
     * administrador), esto no pasa por ninguna revisión. Deja el usuario en el mismo
     * estado que si nunca hubiera solicitado RUAC; `ruacSolicitadoEn`/`ruacRevisadoPor` no
     * se tocan (no se muestran en ninguna pantalla, quedan como rastro de auditoría).
     */
    fun eliminarRuac(
        uid: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {

        val campos: Map<String, Any?> = mapOf(
            "ruacEstado" to "no_solicitado",
            "ruacRegistro" to null,
            "ruacRegion" to null,
            "ruacTecnica" to null,
            "ruacDocumentoUrl" to null,
            "ruacVerificadoEn" to null,
            "ruacUltimaValidacion" to null
        )

        firestore.collection("usuarios")
            .document(uid)
            .update(campos)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { error ->

                onError(
                    error.message ?: "Error al eliminar el RUAC"
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