package com.minkaart2.app.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.minkaart2.app.R
import com.minkaart2.app.data.CatalogosProducto
import com.minkaart2.app.data.VERSION_TERMINOS
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
        val departamentos: List<String>,
        val productosQueVende: String,
        val categoriasArtesanales: List<String>,
        val categoriasArtesanalesOtro: String?,
        val tieneRuac: Boolean,
        val numeroRuac: String,
        val ruacUri: Uri?
    )

    private var datosPendientes: DatosRegistroPendiente? = null

    // =========================
    // PASO 1: VALIDAR Y ENVIAR CÓDIGO DE VERIFICACIÓN
    // Los errores de validación se reportan con un id de recurso (onErrorRes) para que la
    // pantalla los muestre en el idioma de la app, sin guardar un Context aquí; los errores
    // que vienen de Firebase/Cloud Functions siguen llegando como texto por onError.
    // =========================

    fun iniciarRegistro(
        nombre: String,
        correo: String,
        contraseña: String,
        confirmarContraseña: String,
        rol: String,
        aceptoTerminos: Boolean,
        telefono: String = "",
        fechaNacimiento: String = "",
        departamentos: List<String> = emptyList(),
        productosQueVende: String = "",
        categoriasArtesanales: List<String> = emptyList(),
        categoriasArtesanalesOtro: String = "",
        tieneRuac: Boolean = false,
        numeroRuac: String = "",
        ruacUri: Uri? = null,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onErrorRes: (Int) -> Unit
    ) {

        // =========================
        // VALIDACIONES GENERALES
        // =========================

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

        if (!aceptoTerminos) {
            onErrorRes(R.string.err_acepta_terminos)
            return
        }

        // =========================
        // VALIDACIONES ARTESANO
        // =========================

        if (rol == "artesano") {

            val errorArtesano = validarDatosArtesano(
                telefono = telefono,
                fechaNacimiento = fechaNacimiento,
                departamentos = departamentos,
                productosQueVende = productosQueVende,
                categoriasArtesanales = categoriasArtesanales,
                categoriasArtesanalesOtro = categoriasArtesanalesOtro,
                tieneRuac = tieneRuac,
                numeroRuac = numeroRuac,
                ruacUri = ruacUri
            )

            if (errorArtesano != null) {
                onErrorRes(errorArtesano)
                return
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
                    departamentos = departamentos,
                    productosQueVende = productosQueVende,
                    categoriasArtesanales = categoriasArtesanales,
                    categoriasArtesanalesOtro = categoriasArtesanalesOtro,
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
    // VALIDACIÓN DE LOS DATOS OBLIGATORIOS DEL ARTESANO
    // (compartida por el registro con correo y por el registro con Google)
    // Devuelve el id del mensaje de error, o null si todo está bien.
    // =========================

    private fun validarDatosArtesano(
        telefono: String,
        fechaNacimiento: String,
        departamentos: List<String>,
        productosQueVende: String,
        categoriasArtesanales: List<String>,
        categoriasArtesanalesOtro: String,
        tieneRuac: Boolean,
        numeroRuac: String,
        ruacUri: Uri?
    ): Int? {

        if (telefono.isBlank()) return R.string.err_telefono

        if (fechaNacimiento.isBlank()) return R.string.err_fecha_nacimiento

        if (departamentos.isEmpty()) return R.string.err_departamentos

        if (productosQueVende.isBlank()) return R.string.err_productos_que_vende

        if (categoriasArtesanales.isEmpty()) return R.string.err_categorias_artesanales

        if (
            CatalogosProducto.CATEGORIA_OTROS in categoriasArtesanales &&
            categoriasArtesanalesOtro.isBlank()
        ) {
            return R.string.err_categoria_otros
        }

        if (tieneRuac) {

            if (numeroRuac.isBlank()) return R.string.err_ruac_numero

            if (ruacUri == null) return R.string.err_ruac_documento
        }

        return null
    }

    // "Especifica cuál" solo tiene sentido junto a la opción "Otros": se descarta cualquier
    // texto que haya quedado de una selección anterior (mismo criterio que
    // Product.categoriaOtro).
    private fun categoriasOtroFinal(
        categorias: List<String>,
        otro: String
    ): String? =
        if (CatalogosProducto.CATEGORIA_OTROS in categorias) otro.trim() else null

    /** Nombre y correo de la cuenta de Google ya autenticada. */
    fun datosCuentaGoogle(): Pair<String, String> = authRepository.datosCuentaActual()

    // =========================
    // REGISTRO DE ARTESANO CON GOOGLE
    // Google ya verificó el correo y la cuenta de Firebase Auth ya existe: no hay código
    // de 6 dígitos ni contraseña, solo los datos obligatorios del artesano.
    // =========================

    fun completarRegistroGoogle(
        aceptoTerminos: Boolean,
        telefono: String,
        fechaNacimiento: String,
        departamentos: List<String>,
        productosQueVende: String,
        categoriasArtesanales: List<String>,
        categoriasArtesanalesOtro: String,
        tieneRuac: Boolean,
        numeroRuac: String,
        ruacUri: Uri?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onErrorRes: (Int) -> Unit
    ) {

        val usuario = auth.currentUser

        if (usuario == null) {
            onErrorRes(R.string.err_usuario_no_autenticado)
            return
        }

        if (!aceptoTerminos) {
            onErrorRes(R.string.err_acepta_terminos)
            return
        }

        val errorArtesano = validarDatosArtesano(
            telefono = telefono,
            fechaNacimiento = fechaNacimiento,
            departamentos = departamentos,
            productosQueVende = productosQueVende,
            categoriasArtesanales = categoriasArtesanales,
            categoriasArtesanalesOtro = categoriasArtesanalesOtro,
            tieneRuac = tieneRuac,
            numeroRuac = numeroRuac,
            ruacUri = ruacUri
        )

        if (errorArtesano != null) {
            onErrorRes(errorArtesano)
            return
        }

        val datos = DatosRegistroPendiente(
            nombre = usuario.displayName ?: "",
            correo = usuario.email ?: "",
            contraseña = "",
            rol = "artesano",
            telefono = telefono,
            fechaNacimiento = fechaNacimiento,
            departamentos = departamentos,
            productosQueVende = productosQueVende,
            categoriasArtesanales = categoriasArtesanales,
            categoriasArtesanalesOtro = categoriasOtroFinal(
                categoriasArtesanales,
                categoriasArtesanalesOtro
            ),
            tieneRuac = tieneRuac,
            numeroRuac = numeroRuac,
            ruacUri = if (tieneRuac) ruacUri else null
        )

        if (datos.tieneRuac && datos.ruacUri != null) {

            subirRuac(
                uid = usuario.uid,
                ruacUri = datos.ruacUri,

                onSuccess = { ruacUrl ->

                    guardarUsuario(
                        uid = usuario.uid,
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
                uid = usuario.uid,
                datos = datos,
                ruacUrl = "",
                onSuccess = onSuccess,
                onError = onError
            )
        }
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
        departamentos: List<String>,
        productosQueVende: String,
        categoriasArtesanales: List<String>,
        categoriasArtesanalesOtro: String,
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
            departamentos = if (rol == "artesano") departamentos else emptyList(),
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
            categoriasArtesanalesOtro = if (rol == "artesano") {
                categoriasOtroFinal(categoriasArtesanales, categoriasArtesanalesOtro)
            } else {
                null
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
        onError: (String) -> Unit,
        onErrorRes: (Int) -> Unit
    ) {

        val datos = datosPendientes

        if (datos == null) {
            onErrorRes(R.string.err_sin_registro_pendiente)
            return
        }

        if (codigoIngresado.isBlank()) {
            onErrorRes(R.string.err_codigo_vacio)
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

        // El registro sigue pidiendo RUAC igual que antes (Fase 5, Decisión 2), pero ahora
        // escribe directo en los campos nuevos de solicitud en vez de tieneRuac/numeroRuac/
        // ruacUrl (ya no existen en el modelo): un artesano que marque que sí tiene RUAC
        // entra a la misma cola de revisión del administrador que una solicitud hecha
        // después desde su perfil. ruacRegion/ruacTecnica quedan sin llenar porque el
        // registro no los pide.
        val user = User(
            uid = uid,
            nombre = datos.nombre,
            correo = datos.correo,
            rol = datos.rol,
            telefono = datos.telefono,
            fechaNacimiento = datos.fechaNacimiento,
            // `region` (heredado) ya no se escribe: reemplazado por `departamentos`.
            departamentos = datos.departamentos,
            productosQueVende = datos.productosQueVende,
            categoriasArtesanales = datos.categoriasArtesanales,
            categoriasArtesanalesOtro = datos.categoriasArtesanalesOtro,
            ruacEstado = if (datos.tieneRuac) "pendiente" else "no_solicitado",
            ruacRegistro = if (datos.tieneRuac) datos.numeroRuac else null,
            ruacDocumentoUrl = if (datos.tieneRuac) ruacUrl else null,
            ruacSolicitadoEn = if (datos.tieneRuac) Timestamp.now() else null,
            // Solo se llega aquí si el formulario se validó con los términos aceptados.
            aceptoTerminos = true,
            terminosAceptadosEn = Timestamp.now(),
            versionTerminos = VERSION_TERMINOS
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
        onError: (String) -> Unit,
        onErrorRes: (Int) -> Unit
    ) {

        val datos = datosPendientes

        if (datos == null) {
            onErrorRes(R.string.err_sin_registro_pendiente)
            return
        }

        authRepository.enviarCodigoVerificacion(
            correo = datos.correo,
            onSuccess = onSuccess,
            onError = onError
        )
    }
}
