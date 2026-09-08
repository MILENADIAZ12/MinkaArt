package com.minkaart2.app.ui.register

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.minkaart2.app.viewmodel.RegisterViewModel
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen() {

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "Registro"
        )

        // =========================
        // NOMBRE
        // =========================

        OutlinedTextField(
            value = nombre,
            onValueChange = {
                nombre = it
            },
            label = {
                Text("Nombre")
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // =========================
        // CORREO
        // =========================

        OutlinedTextField(
            value = correo,
            onValueChange = {
                correo = it
            },
            label = {
                Text("Correo electrónico")
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // =========================
        // CONTRASEÑA
        // =========================

        OutlinedTextField(
            value = contraseña,
            onValueChange = {
                contraseña = it
            },
            label = {
                Text("Contraseña")
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // =========================
        // SELECCIÓN DEL ROL
        // =========================

        Text(
            text = "¿Cómo deseas registrarte?",
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            RadioButton(
                selected = rol == "artesano",
                onClick = {
                    rol = "artesano"
                }
            )

            Text(
                text = "Artesano"
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            RadioButton(
                selected = rol == "cliente",
                onClick = {

                    rol = "cliente"

                    // Limpiar datos relacionados con RUAC
                    tieneRuac = false
                    numeroRuac = ""
                    ruacUri = null
                }
            )

            Text(
                text = "Cliente"
            )
        }

        // =====================================================
        // CAMPOS QUE SOLO APARECEN SI EL USUARIO ES ARTESANO
        // =====================================================

        if (rol == "artesano") {

            Text(
                text = "Información del artesano",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )

            // =========================
            // TELÉFONO
            // =========================

            OutlinedTextField(
                value = telefono,
                onValueChange = {
                    telefono = it
                },
                label = {
                    Text("Número de teléfono")
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // =========================
            // FECHA DE NACIMIENTO
            // =========================

            Text(
                text = "Fecha de nacimiento",
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedButton(
                onClick = {
                    mostrarCalendario = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {

                Text(
                    text = if (fechaNacimiento.isBlank()) {
                        "Seleccionar fecha de nacimiento"
                    } else {
                        fechaNacimiento
                    }
                )
            }

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

            OutlinedTextField(
                value = region,
                onValueChange = {
                    region = it
                },
                label = {
                    Text("Región")
                },
                placeholder = {
                    Text("Ej: Bogotá, Boyacá, La Guajira")
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // =========================
            // PRODUCTOS QUE VENDE
            // =========================

            OutlinedTextField(
                value = productosQueVende,
                onValueChange = {
                    productosQueVende = it
                },
                label = {
                    Text("¿Qué productos artesanales vendes?")
                },
                placeholder = {
                    Text("Ej: Mochilas, tejidos, accesorios")
                },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            // =========================
            // CATEGORÍAS
            // =========================

            OutlinedTextField(
                value = categoriasTexto,
                onValueChange = {
                    categoriasTexto = it
                },
                label = {
                    Text("Categorías artesanales")
                },
                placeholder = {
                    Text("Ej: Tejidos, cerámica, joyería")
                },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            Text(
                text = "Separa las categorías con comas.",
                modifier = Modifier.fillMaxWidth()
            )

            // =========================
            // RUAC
            // =========================

            Text(
                text = "¿Tienes registro RUAC?",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                RadioButton(
                    selected = tieneRuac,
                    onClick = {
                        tieneRuac = true
                    }
                )

                Text("Sí")
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
                    }
                )

                Text("No")
            }

            // =========================
            // SI TIENE RUAC
            // =========================

            if (tieneRuac) {

                OutlinedTextField(
                    value = numeroRuac,
                    onValueChange = {
                        numeroRuac = it
                    },
                    label = {
                        Text("Número de registro RUAC")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedButton(
                    onClick = {

                        selectorRuac.launch(
                            arrayOf(
                                "application/pdf",
                                "image/*"
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {

                    Text(
                        text = if (ruacUri == null) {
                            "Adjuntar documento RUAC"
                        } else {
                            "Cambiar documento RUAC"
                        }
                    )
                }

                if (ruacUri != null) {

                    Text(
                        text = "Documento RUAC seleccionado correctamente",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // =========================
        // BOTÓN REGISTRARSE
        // =========================

        Button(
            onClick = {

                val categorias = categoriasTexto
                    .split(",")
                    .map {
                        it.trim()
                    }
                    .filter {
                        it.isNotBlank()
                    }

                registerViewModel.registrarUsuario(

                    nombre = nombre,
                    correo = correo,
                    contraseña = contraseña,
                    rol = rol,

                    telefono = if (rol == "artesano") {
                        telefono
                    } else {
                        ""
                    },

                    fechaNacimiento = if (rol == "artesano") {
                        fechaNacimiento
                    } else {
                        ""
                    },

                    region = if (rol == "artesano") {
                        region
                    } else {
                        ""
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

                    tieneRuac = (
                            rol == "artesano" &&
                                    tieneRuac
                            ),

                    numeroRuac = if (
                        rol == "artesano" &&
                        tieneRuac
                    ) {
                        numeroRuac
                    } else {
                        ""
                    },

                    ruacUri = if (
                        rol == "artesano" &&
                        tieneRuac
                    ) {
                        ruacUri
                    } else {
                        null
                    },

                    onSuccess = {

                        Toast.makeText(
                            context,
                            "Usuario registrado correctamente",
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {

            Text(
                text = "Registrarse"
            )
        }
    }
}