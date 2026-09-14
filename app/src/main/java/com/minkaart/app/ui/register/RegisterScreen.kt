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
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.minkaart2.app.navigation.Routes
import com.minkaart2.app.ui.components.GlassCard
import com.minkaart2.app.ui.components.GlassTextField
import com.minkaart2.app.ui.components.GradientButton
import com.minkaart2.app.ui.components.OutlineButton
import com.minkaart2.app.ui.components.PatternBackground
import com.minkaart2.app.ui.components.RolePillToggle
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
fun RegisterScreen(navController: NavController) {

    // =========================
    // DATOS GENERALES
    // =========================

    var nombre by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var contraseña by remember { mutableStateOf("") }
    var rol by remember { mutableStateOf("cliente") }

    // =========================
    // DATOS DEL ARTESANO
    // =========================

    var telefono by remember { mutableStateOf("") }
    var fechaNacimiento by remember { mutableStateOf("") }
    var region by remember { mutableStateOf("") }
    var productosQueVende by remember { mutableStateOf("") }
    var categoriasTexto by remember { mutableStateOf("") }

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
    val context = LocalContext.current

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
                    text = "Crea tu cuenta",
                    color = Marfil,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Únete como artesano o comprador",
                    color = Marfil.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(modifier = Modifier.height(16.dp))

                // =========================
                // SELECCIÓN DEL ROL
                // =========================

                RolePillToggle(
                    opciones = listOf("artesano" to "Artesano", "cliente" to "Cliente"),
                    valorSeleccionado = rol,
                    onSeleccionar = { nuevoRol ->

                        rol = nuevoRol

                        if (nuevoRol == "cliente") {
                            tieneRuac = false
                            numeroRuac = ""
                            ruacUri = null
                        }
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // =========================
                // NOMBRE
                // =========================

                GlassTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = "Nombre completo",
                    icon = Icons.Filled.Person,
                    modifier = Modifier.fillMaxWidth()
                )

                // =========================
                // CORREO
                // =========================

                GlassTextField(
                    value = correo,
                    onValueChange = { correo = it },
                    label = "Correo electrónico",
                    icon = Icons.Filled.Email,
                    modifier = Modifier.fillMaxWidth()
                )

                // =========================
                // CONTRASEÑA
                // =========================

                GlassTextField(
                    value = contraseña,
                    onValueChange = { contraseña = it },
                    label = "Contraseña",
                    icon = Icons.Filled.Lock,
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation()
                )

                // =====================================================
                // CAMPOS QUE SOLO APARECEN SI EL USUARIO ES ARTESANO
                // =====================================================

                if (rol == "artesano") {

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Información del artesano",
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
                        label = "Número de teléfono",
                        icon = Icons.Filled.Phone,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )

                    // =========================
                    // FECHA DE NACIMIENTO
                    // =========================

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Fecha de nacimiento",
                        color = Marfil.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlineButton(
                        text = if (fechaNacimiento.isBlank()) {
                            "Seleccionar fecha de nacimiento"
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
                                                "Selecciona una fecha",
                                                Toast.LENGTH_SHORT
                                            ).show()

                                        } else if (
                                            fechaMillis > System.currentTimeMillis()
                                        ) {

                                            Toast.makeText(
                                                context,
                                                "La fecha de nacimiento no puede ser futura",
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

                                    Text("Aceptar")
                                }
                            },
                            dismissButton = {

                                TextButton(
                                    onClick = {
                                        mostrarCalendario = false
                                    }
                                ) {

                                    Text("Cancelar")
                                }
                            }
                        ) {

                            DatePicker(
                                state = datePickerState
                            )
                        }
                    }

                    // =========================
                    // REGIÓN
                    // =========================

                    GlassTextField(
                        value = region,
                        onValueChange = { region = it },
                        label = "Región (ej: Bogotá, Boyacá, La Guajira)",
                        icon = Icons.Filled.LocationOn,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // =========================
                    // PRODUCTOS QUE VENDE
                    // =========================

                    GlassTextField(
                        value = productosQueVende,
                        onValueChange = { productosQueVende = it },
                        label = "¿Qué productos artesanales vendes?",
                        icon = Icons.Filled.ShoppingCart,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        minLines = 2
                    )

                    // =========================
                    // CATEGORÍAS
                    // =========================

                    GlassTextField(
                        value = categoriasTexto,
                        onValueChange = { categoriasTexto = it },
                        label = "Categorías artesanales (ej: Tejidos, cerámica, joyería)",
                        icon = Icons.Filled.Star,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        minLines = 2
                    )

                    Text(
                        text = "Separa las categorías con comas.",
                        color = Marfil.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // =========================
                    // RUAC
                    // =========================

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "¿Tienes registro RUAC?",
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

                        Text("Sí", color = Marfil)
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

                        Text("No", color = Marfil)
                    }

                    // =========================
                    // SI TIENE RUAC
                    // =========================

                    if (tieneRuac) {

                        GlassTextField(
                            value = numeroRuac,
                            onValueChange = { numeroRuac = it },
                            label = "Número de registro RUAC",
                            icon = Icons.Filled.Person,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlineButton(
                            text = if (ruacUri == null) {
                                "Adjuntar documento RUAC"
                            } else {
                                "Cambiar documento RUAC"
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
                                text = "Documento RUAC seleccionado correctamente",
                                color = Marfil.copy(alpha = 0.85f),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // =========================
                // BOTÓN: ENVIAR CORREO / REENVIAR / REGISTRADO
                // =========================

                Spacer(modifier = Modifier.height(16.dp))

                if (registroCompletado) {

                    // Ya se confirmó el correo y se guardó el usuario:
                    // se muestra como confirmación, no es una acción más.
                    GradientButton(
                        text = "✓ Has sido registrado correctamente",
                        onClick = {},
                        enabled = false,
                        modifier = Modifier.fillMaxWidth()
                    )

                } else if (correoEnviado) {

                    Text(
                        text = "Te enviamos un código de 6 dígitos a tu correo. " +
                                "Ingrésalo aquí abajo; expira en 10 minutos.",
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
                        label = "Código de 6 dígitos",
                        icon = Icons.Filled.Lock,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.NumberPassword
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    GradientButton(
                        text = "Verificar",
                        onClick = {

                            registerViewModel.confirmarCodigoDeVerificacion(
                                codigoIngresado = codigoVerificacion,

                                onVerificado = {

                                    registroCompletado = true

                                    Toast.makeText(
                                        context,
                                        "Has sido registrado correctamente",
                                        Toast.LENGTH_LONG
                                    ).show()
                                },

                                onError = { mensaje ->

                                    Toast.makeText(
                                        context,
                                        mensaje,
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlineButton(
                        text = "Reenviar código",
                        onClick = {

                            codigoVerificacion = ""

                            registerViewModel.reenviarCodigoDeVerificacion(
                                onSuccess = {

                                    Toast.makeText(
                                        context,
                                        "Código de verificación reenviado",
                                        Toast.LENGTH_LONG
                                    ).show()
                                },

                                onError = { mensaje ->

                                    Toast.makeText(
                                        context,
                                        mensaje,
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                } else {

                    GradientButton(
                        text = "Enviar código de verificación",
                        onClick = {

                            val categorias = categoriasTexto
                                .split(",")
                                .map { it.trim() }
                                .filter { it.isNotBlank() }

                            registerViewModel.iniciarRegistro(

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
                                    categorias
                                } else {
                                    emptyList()
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
                                        "Te enviamos un código de verificación",
                                        Toast.LENGTH_LONG
                                    ).show()
                                },

                                onError = { mensaje ->

                                    Toast.makeText(
                                        context,
                                        mensaje,
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
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
                    text = "¿Eres administrador? Regístrate aquí",
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
