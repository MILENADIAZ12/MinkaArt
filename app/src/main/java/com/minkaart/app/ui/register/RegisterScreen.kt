package com.minkaart2.app.ui.register

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.minkaart2.app.R
import com.minkaart2.app.data.CatalogosProducto
import com.minkaart2.app.navigation.Routes
import com.minkaart2.app.ui.components.BotonOjoContraseña
import com.minkaart2.app.ui.components.CheckboxTerminos
import com.minkaart2.app.ui.components.GlassCard
import com.minkaart2.app.ui.components.GlassTextField
import com.minkaart2.app.ui.components.GradientButton
import com.minkaart2.app.ui.components.MinkaMultiSelectField
import com.minkaart2.app.ui.components.OutlineButton
import com.minkaart2.app.ui.components.PatternBackground
import com.minkaart2.app.util.SesionLocal
import com.minkaart2.app.util.etiquetaCategoria
import com.minkaart2.app.viewModel.GoogleSignInViewModel
import com.minkaart2.app.viewmodel.RegisterViewModel
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val Guinda = Color(0xFF3B0F1A)
private val Borgona = Color(0xFF66172E)
private val AzulClaro = Color(0xFFB7C7D9)
private val AzulAcero = Color(0xFF55708E)
private val Marfil = Color(0xFFF2EDE6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    navController: NavController,
    rolElegido: String,
    modoGoogleInicial: Boolean = false
) {

    // =========================
    // DATOS GENERALES
    // =========================

    var nombre by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var contraseña by remember { mutableStateOf("") }
    val rol = rolElegido
    var confirmarContraseña by remember { mutableStateOf("") }
    var verContraseña by remember { mutableStateOf(false) }
    var verConfirmacion by remember { mutableStateOf(false) }

    // Términos y Condiciones / Política de Tratamiento de Datos (obligatorio).
    var aceptoTerminos by remember { mutableStateOf(false) }

    // true cuando la persona ya se autenticó con Google y solo falta completar el registro
    // (artesano): sin contraseña ni código de verificación.
    var modoGoogle by remember { mutableStateOf(modoGoogleInicial) }

    // =========================
    // DATOS DEL ARTESANO
    // =========================

    var telefono by remember { mutableStateOf("") }
    var fechaNacimiento by remember { mutableStateOf("") }
    var departamentos by remember { mutableStateOf<List<String>>(emptyList()) }
    var productosQueVende by remember { mutableStateOf("") }
    var categorias by remember { mutableStateOf<List<String>>(emptyList()) }
    var categoriasOtro by remember { mutableStateOf("") }

    // =========================
    // CALENDARIO
    // =========================

    var mostrarCalendario by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState()

    // =========================
    // RUAC
    // =========================

    var tieneRuac by remember { mutableStateOf(false) }
    var numeroRuac by remember { mutableStateOf("") }
    var ruacUri by remember { mutableStateOf<Uri?>(null) }

    // =========================
    // VERIFICACIÓN DE CORREO
    // =========================

    var correoEnviado by remember { mutableStateOf(false) }
    var registroCompletado by remember { mutableStateOf(false) }
    var codigoVerificacion by remember { mutableStateOf("") }

    // =========================
    // VIEWMODEL Y CONTEXTO
    // =========================

    val registerViewModel: RegisterViewModel = viewModel()
    val googleViewModel: GoogleSignInViewModel = viewModel()
    val context = LocalContext.current

    // Errores del ViewModel: los de Firebase llegan como texto; los de validación como id de
    // recurso, para mostrarlos en el idioma de la app.
    val mostrarError: (String) -> Unit = { mensaje ->
        Toast.makeText(context, mensaje, Toast.LENGTH_LONG).show()
    }

    val mostrarErrorRes: (Int) -> Unit = { recurso ->
        Toast.makeText(context, context.getString(recurso), Toast.LENGTH_LONG).show()
    }

    val textoRegistrado = stringResource(R.string.registro_exitoso)
    val textoSeleccionaFecha = stringResource(R.string.selecciona_fecha)
    val textoFechaFutura = stringResource(R.string.fecha_nacimiento_futura)

    // =========================
    // SELECTOR DEL RUAC
    // =========================

    val selectorRuac = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->

        if (uri != null) {
            ruacUri = uri
        }
    }

    // =========================
    // PANTALLA
    // =========================

    PatternBackground(
        gradientColors = listOf(Guinda, Borgona, AzulAcero),
        patternAlpha = 0.35f,
        blurBackground = true,
        darkOverlayAlpha = 0.18f
    ) {
        // Column + Arrangement.Center en vez de Box: cuando el formulario es corto
        // (cliente) centra la tarjeta verticalmente; cuando es más largo que la
        // pantalla (artesano con RUAC) el propio scroll hace que se vea desde arriba,
        // sin necesidad de una rama de código distinta para cada caso.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(vertical = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            GlassCard(
                modifier = Modifier.padding(horizontal = 20.dp)
            ) {

                Text(
                    text = stringResource(R.string.registro_titulo),
                    color = Marfil,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(4.dp))

                // El rol se eligió en la pantalla anterior: aquí solo se muestra cuál es,
                // con la opción de volver para cambiarlo.
                Text(
                    text = stringResource(
                        if (rol == "artesano") {
                            R.string.registro_como_artesano
                        } else {
                            R.string.registro_como_cliente
                        }
                    ),
                    color = Marfil.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )

                TextButton(
                    onClick = {

                        if (modoGoogle) {
                            googleViewModel.cancelarRegistroGoogle()
                        }

                        navController.popBackStack()
                    }
                ) {
                    Text(
                        text = stringResource(R.string.cambiar_rol),
                        color = AzulClaro,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Text(
                    text = stringResource(R.string.campos_obligatorios),
                    color = Marfil.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (modoGoogle) {

                    // Nombre y correo vienen de la cuenta de Google ya autenticada.
                    val (nombreGoogle, correoGoogle) = remember { registerViewModel.datosCuentaGoogle() }

                    Text(
                        text = stringResource(
                            R.string.cuenta_de_google,
                            nombreGoogle,
                            correoGoogle
                        ),
                        color = Marfil.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth()
                    )

                } else {

                    // =========================
                    // NOMBRE
                    // =========================

                    GlassTextField(
                        value = nombre,
                        onValueChange = { nombre = it },
                        label = stringResource(R.string.campo_nombre_completo),
                        icon = Icons.Filled.Person,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // =========================
                    // CORREO
                    // =========================

                    GlassTextField(
                        value = correo,
                        onValueChange = { correo = it },
                        label = stringResource(R.string.campo_correo_obligatorio),
                        icon = Icons.Filled.Email,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (!modoGoogle) {

                    // =========================
                    // CONTRASEÑA Y CONFIRMACIÓN
                    // =========================

                    GlassTextField(
                        value = contraseña,
                        onValueChange = { contraseña = it },
                        label = stringResource(R.string.campo_contrasena_obligatorio),
                        icon = Icons.Filled.Lock,
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (verContraseña) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        trailingIcon = {
                            BotonOjoContraseña(
                                visible = verContraseña,
                                onAlternar = { verContraseña = !verContraseña }
                            )
                        }
                    )

                    // Aviso de seguridad: es cierto — la contraseña la guarda Firebase
                    // Authentication con hash y nunca se escribe en Firestore.
                    Text(
                        text = stringResource(R.string.contrasena_cifrada),
                        color = Marfil.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth()
                    )

                    GlassTextField(
                        value = confirmarContraseña,
                        onValueChange = { confirmarContraseña = it },
                        label = stringResource(R.string.campo_confirmar_contrasena),
                        icon = Icons.Filled.Lock,
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (verConfirmacion) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        trailingIcon = {
                            BotonOjoContraseña(
                                visible = verConfirmacion,
                                onAlternar = { verConfirmacion = !verConfirmacion }
                            )
                        }
                    )
                }

                // =====================================================
                // CAMPOS QUE SOLO APARECEN SI EL USUARIO ES ARTESANO
                // =====================================================

                if (rol == "artesano") {

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(R.string.info_artesano),
                        color = Marfil,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // =========================
                    // TELÉFONO
                    // =========================

                    GlassTextField(
                        value = telefono,
                        onValueChange = { telefono = it },
                        label = stringResource(R.string.campo_telefono),
                        icon = Icons.Filled.Phone,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )

                    // =========================
                    // FECHA DE NACIMIENTO
                    // =========================

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(R.string.campo_fecha_nacimiento),
                        color = Marfil.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlineButton(
                        text = if (fechaNacimiento.isBlank()) {
                            stringResource(R.string.seleccionar_fecha_nacimiento)
                        } else {
                            fechaNacimiento
                        },
                        onClick = { mostrarCalendario = true },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // =========================
                    // CALENDARIO
                    // =========================

                    if (mostrarCalendario) {

                        DatePickerDialog(
                            onDismissRequest = {
                                mostrarCalendario = false
                            },
                            confirmButton = {

                                TextButton(
                                    onClick = {

                                        val fechaMillis =
                                            datePickerState.selectedDateMillis

                                        if (fechaMillis == null) {

                                            Toast.makeText(
                                                context,
                                                textoSeleccionaFecha,
                                                Toast.LENGTH_SHORT
                                            ).show()

                                        } else if (
                                            fechaMillis > System.currentTimeMillis()
                                        ) {

                                            Toast.makeText(
                                                context,
                                                textoFechaFutura,
                                                Toast.LENGTH_LONG
                                            ).show()

                                        } else {

                                            val fecha = Instant
                                                .ofEpochMilli(fechaMillis)
                                                .atZone(ZoneOffset.UTC)
                                                .toLocalDate()

                                            fechaNacimiento = fecha.format(
                                                DateTimeFormatter.ofPattern(
                                                    "dd/MM/yyyy"
                                                )
                                            )

                                            mostrarCalendario = false
                                        }
                                    }
                                ) {

                                    Text(stringResource(R.string.aceptar))
                                }
                            },
                            dismissButton = {

                                TextButton(
                                    onClick = {
                                        mostrarCalendario = false
                                    }
                                ) {

                                    Text(stringResource(R.string.cancelar))
                                }
                            }
                        ) {

                            DatePicker(
                                state = datePickerState
                            )
                        }
                    }

                    // =========================
                    // DEPARTAMENTOS (selección múltiple)
                    // =========================

                    Spacer(modifier = Modifier.height(16.dp))

                    MinkaMultiSelectField(
                        label = stringResource(R.string.campo_departamentos),
                        opciones = CatalogosProducto.DEPARTAMENTOS,
                        seleccionados = departamentos,
                        onCambio = { departamentos = it },
                        colorEtiqueta = Marfil.copy(alpha = 0.85f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // =========================
                    // PRODUCTOS QUE VENDE
                    // =========================

                    GlassTextField(
                        value = productosQueVende,
                        onValueChange = { productosQueVende = it },
                        label = stringResource(R.string.campo_productos_que_vendes),
                        icon = Icons.Filled.ShoppingCart,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        minLines = 2
                    )

                    // =========================
                    // CATEGORÍAS (selección múltiple)
                    // =========================

                    Spacer(modifier = Modifier.height(16.dp))

                    MinkaMultiSelectField(
                        label = stringResource(R.string.campo_categorias_artesanales),
                        opciones = CatalogosProducto.CATEGORIAS,
                        seleccionados = categorias,
                        onCambio = { categorias = it },
                        colorEtiqueta = Marfil.copy(alpha = 0.85f),
                        etiqueta = { etiquetaCategoria(it) }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (CatalogosProducto.CATEGORIA_OTROS in categorias) {

                        GlassTextField(
                            value = categoriasOtro,
                            onValueChange = { categoriasOtro = it },
                            label = stringResource(R.string.campo_especifica_cual),
                            icon = Icons.Filled.Person,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // =========================
                    // RUAC
                    // =========================

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(R.string.tienes_ruac),
                        color = Marfil,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        RadioButton(
                            selected = tieneRuac,
                            onClick = { tieneRuac = true },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = AzulClaro,
                                unselectedColor = Marfil.copy(alpha = 0.5f)
                            )
                        )

                        Text(stringResource(R.string.si), color = Marfil)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        RadioButton(
                            selected = !tieneRuac,
                            onClick = {

                                tieneRuac = false
                                numeroRuac = ""
                                ruacUri = null
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = AzulClaro,
                                unselectedColor = Marfil.copy(alpha = 0.5f)
                            )
                        )

                        Text(stringResource(R.string.no), color = Marfil)
                    }

                    // =========================
                    // SI TIENE RUAC
                    // =========================

                    if (tieneRuac) {

                        GlassTextField(
                            value = numeroRuac,
                            onValueChange = { numeroRuac = it },
                            label = stringResource(R.string.campo_numero_ruac),
                            icon = Icons.Filled.Person,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlineButton(
                            text = if (ruacUri == null) {
                                stringResource(R.string.adjuntar_ruac)
                            } else {
                                stringResource(R.string.cambiar_ruac)
                            },
                            onClick = {

                                selectorRuac.launch(
                                    arrayOf(
                                        "application/pdf",
                                        "image/*"
                                    )
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (ruacUri != null) {

                            Text(
                                text = stringResource(R.string.ruac_seleccionado),
                                color = Marfil.copy(alpha = 0.85f),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // =========================
                // TÉRMINOS Y CONDICIONES
                // (también en modo Google: ahí todavía no se ha creado el documento)
                // =========================

                if (!registroCompletado) {

                    Spacer(modifier = Modifier.height(8.dp))

                    CheckboxTerminos(
                        aceptado = aceptoTerminos,
                        onCambio = { aceptoTerminos = it },
                        onAbrirTerminos = { navController.navigate(Routes.TERMS) }
                    )
                }

                // =========================
                // BOTÓN: ENVIAR CORREO / REENVIAR / REGISTRADO
                // =========================

                Spacer(modifier = Modifier.height(16.dp))

                if (modoGoogle) {

                    GradientButton(
                        text = stringResource(R.string.completar_registro),
                        onClick = {

                            registerViewModel.completarRegistroGoogle(
                                aceptoTerminos = aceptoTerminos,
                                telefono = telefono,
                                fechaNacimiento = fechaNacimiento,
                                departamentos = departamentos,
                                productosQueVende = productosQueVende,
                                categoriasArtesanales = categorias,
                                categoriasArtesanalesOtro = categoriasOtro,
                                tieneRuac = tieneRuac,
                                numeroRuac = if (tieneRuac) numeroRuac else "",
                                ruacUri = if (tieneRuac) ruacUri else null,

                                onSuccess = {

                                    Toast.makeText(
                                        context,
                                        textoRegistrado,
                                        Toast.LENGTH_LONG
                                    ).show()

                                    SesionLocal.guardar("artesano", recordar = true)

                                    navController.navigate(Routes.HOME_ARTESANO) {
                                        popUpTo(Routes.WELCOME) { inclusive = true }
                                    }
                                },

                                onError = mostrarError,
                                onErrorRes = mostrarErrorRes
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                } else if (registroCompletado) {

                    // Ya se confirmó el correo y se guardó el usuario:
                    // se muestra como confirmación, no es una acción más.
                    GradientButton(
                        text = stringResource(R.string.registro_exitoso_boton),
                        onClick = {},
                        enabled = false,
                        modifier = Modifier.fillMaxWidth()
                    )

                } else if (correoEnviado) {

                    Text(
                        text = stringResource(R.string.codigo_enviado_info),
                        color = Marfil.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    GlassTextField(
                        value = codigoVerificacion,
                        onValueChange = { valor ->

                            codigoVerificacion = valor
                                .filter { it.isDigit() }
                                .take(6)
                        },
                        label = stringResource(R.string.campo_codigo),
                        icon = Icons.Filled.Lock,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.NumberPassword
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    GradientButton(
                        text = stringResource(R.string.verificar),
                        onClick = {

                            registerViewModel.confirmarCodigoDeVerificacion(
                                codigoIngresado = codigoVerificacion,

                                onVerificado = {

                                    registroCompletado = true

                                    Toast.makeText(
                                        context,
                                        textoRegistrado,
                                        Toast.LENGTH_LONG
                                    ).show()
                                },

                                onError = mostrarError,
                                onErrorRes = mostrarErrorRes
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val textoCodigoReenviado = stringResource(R.string.codigo_reenviado)

                    OutlineButton(
                        text = stringResource(R.string.reenviar_codigo),
                        onClick = {

                            codigoVerificacion = ""

                            registerViewModel.reenviarCodigoDeVerificacion(
                                onSuccess = {

                                    Toast.makeText(
                                        context,
                                        textoCodigoReenviado,
                                        Toast.LENGTH_LONG
                                    ).show()
                                },

                                onError = mostrarError,
                                onErrorRes = mostrarErrorRes
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                } else {

                    val textoCodigoEnviado = stringResource(R.string.codigo_enviado_toast)

                    GradientButton(
                        text = stringResource(R.string.enviar_codigo),
                        onClick = {

                            registerViewModel.iniciarRegistro(

                                nombre = nombre,
                                correo = correo,
                                contraseña = contraseña,
                                confirmarContraseña = confirmarContraseña,
                                rol = rol,
                                aceptoTerminos = aceptoTerminos,

                                telefono = if (rol == "artesano") telefono else "",

                                fechaNacimiento = if (rol == "artesano") {
                                    fechaNacimiento
                                } else {
                                    ""
                                },

                                departamentos = if (rol == "artesano") {
                                    departamentos
                                } else {
                                    emptyList()
                                },

                                productosQueVende = if (rol == "artesano") {
                                    productosQueVende
                                } else {
                                    ""
                                },

                                categoriasArtesanales = if (rol == "artesano") {
                                    categorias
                                } else {
                                    emptyList()
                                },

                                categoriasArtesanalesOtro = if (rol == "artesano") {
                                    categoriasOtro
                                } else {
                                    ""
                                },

                                tieneRuac = (rol == "artesano" && tieneRuac),

                                numeroRuac = if (rol == "artesano" && tieneRuac) {
                                    numeroRuac
                                } else {
                                    ""
                                },

                                ruacUri = if (rol == "artesano" && tieneRuac) {
                                    ruacUri
                                } else {
                                    null
                                },

                                onSuccess = {

                                    correoEnviado = true

                                    Toast.makeText(
                                        context,
                                        textoCodigoEnviado,
                                        Toast.LENGTH_LONG
                                    ).show()
                                },

                                onError = mostrarError,
                                onErrorRes = mostrarErrorRes
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (!modoGoogle && !correoEnviado && !registroCompletado) {

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlineButton(
                        text = stringResource(R.string.continuar_google),
                        onClick = {

                            googleViewModel.continuarConGoogle(
                                context = context,

                                // Ya tenía cuenta: se enruta según su rol, como el login.
                                onExistente = { rolExistente ->

                                    val destino = Routes.homeDeRol(rolExistente)

                                    if (destino == null) {

                                        Toast.makeText(
                                            context,
                                            context.getString(
                                                R.string.rol_no_valido,
                                                rolExistente
                                            ),
                                            Toast.LENGTH_LONG
                                        ).show()

                                    } else {

                                        SesionLocal.guardar(rolExistente, recordar = true)

                                        navController.navigate(destino) {
                                            popUpTo(Routes.WELCOME) { inclusive = true }
                                        }
                                    }
                                },

                                // Usuario nuevo con el rol ya elegido en la pantalla anterior.
                                onNuevo = {

                                    if (rol == "cliente") {

                                        googleViewModel.crearClienteConGoogle(
                                            aceptoTerminos = aceptoTerminos,
                                            onSuccess = {
                                                SesionLocal.guardar("cliente", recordar = true)
                                                navController.navigate(Routes.HOME_CLIENTE) {
                                                    popUpTo(Routes.WELCOME) { inclusive = true }
                                                }
                                            },
                                            onError = mostrarError,
                                            onErrorRes = { recurso ->

                                                // Quedó una sesión de Google sin documento: se
                                                // cierra para no dejar a la persona "logueada"
                                                // sin haber aceptado los términos.
                                                googleViewModel.cancelarRegistroGoogle()
                                                mostrarErrorRes(recurso)
                                            }
                                        )

                                    } else {

                                        // Artesano: debe completar sus datos obligatorios.
                                        modoGoogle = true
                                    }
                                },

                                onError = mostrarError
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Entrada discreta al registro de administrador (Fase 5) — no es un
                // tercer botón de rol junto a Artesano/Cliente, solo un texto pequeño al
                // final, como pide el encargo.
                Text(
                    text = stringResource(R.string.eres_administrador),
                    color = Marfil.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.clickable {
                        navController.navigate(Routes.ADMIN_REGISTER)
                    }
                )
            }
        }
    }
}
