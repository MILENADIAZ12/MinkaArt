package com.minkaart2.app.ui.admin

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.minkaart2.app.navigation.Routes
import com.minkaart2.app.ui.components.GlassCard
import com.minkaart2.app.ui.components.GlassTextField
import com.minkaart2.app.ui.components.GradientButton
import com.minkaart2.app.ui.components.PatternBackground
import com.minkaart2.app.viewModel.AdminRegisterViewModel

private val Guinda = Color(0xFF3B0F1A)
private val Borgona = Color(0xFF66172E)
private val AzulAcero = Color(0xFF55708E)
private val Marfil = Color(0xFFF2EDE6)

/**
 * Formulario mínimo, aparte del registro normal (Fase 5): correo, contraseña y el PIN de
 * acceso de administrador. No pasa por el código de verificación de correo — ver
 * `AuthRepository.registrarAdministrador` para la razón. Se llega aquí desde el enlace
 * discreto "¿Eres administrador?" al final de `RegisterScreen`.
 */
@Composable
fun AdminRegisterScreen(navController: NavController) {

    var nombre by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var contraseña by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }

    val viewModel: AdminRegisterViewModel = viewModel()
    val context = LocalContext.current

    PatternBackground(
        gradientColors = listOf(Guinda, Borgona, AzulAcero),
        patternAlpha = 0.35f,
        blurBackground = true,
        darkOverlayAlpha = 0.18f
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(vertical = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            GlassCard(modifier = Modifier.padding(horizontal = 20.dp)) {

                Text(
                    text = "Acceso de administrador",
                    color = Marfil,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "Solo para revisar solicitudes de RUAC",
                    color = Marfil.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(modifier = Modifier.height(16.dp))

                GlassTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = "Nombre completo",
                    icon = Icons.Filled.Person,
                    modifier = Modifier.fillMaxWidth()
                )

                GlassTextField(
                    value = correo,
                    onValueChange = { correo = it },
                    label = "Correo electrónico",
                    icon = Icons.Filled.Email,
                    modifier = Modifier.fillMaxWidth()
                )

                GlassTextField(
                    value = contraseña,
                    onValueChange = { contraseña = it },
                    label = "Contraseña",
                    icon = Icons.Filled.Lock,
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation()
                )

                GlassTextField(
                    value = pin,
                    onValueChange = { pin = it },
                    label = "PIN de acceso de administrador",
                    icon = Icons.Filled.CheckCircle,
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation()
                )

                Spacer(modifier = Modifier.height(16.dp))

                GradientButton(
                    text = "Crear cuenta de administrador",
                    onClick = {

                        viewModel.registrar(
                            nombre = nombre,
                            correo = correo,
                            contraseña = contraseña,
                            pin = pin,

                            onSuccess = {

                                Toast.makeText(
                                    context,
                                    "Cuenta de administrador creada correctamente",
                                    Toast.LENGTH_LONG
                                ).show()

                                navController.navigate(Routes.LOGIN) {
                                    popUpTo(Routes.ADMIN_REGISTER) { inclusive = true }
                                }
                            },

                            onError = { mensaje ->
                                Toast.makeText(context, mensaje, Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
