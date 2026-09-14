package com.minkaart2.app.ui.login

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.minkaart2.app.viewModel.LoginViewModel

private val Guinda = Color(0xFF3B0F1A)
private val Borgona = Color(0xFF66172E)
private val AzulAcero = Color(0xFF55708E)
private val Marfil = Color(0xFFF2EDE6)

@Composable
fun LoginScreen(
    navController: NavController
) {

    var correo by remember { mutableStateOf("") }
    var contraseña by remember { mutableStateOf("") }
    var recuerdame by remember { mutableStateOf(true) }

    val loginViewModel: LoginViewModel = viewModel()
    val context = LocalContext.current

    PatternBackground(
        gradientColors = listOf(Guinda, Borgona, AzulAcero),
        patternAlpha = 0.35f,
        blurBackground = true,
        darkOverlayAlpha = 0.18f
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            GlassCard(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
            ) {

                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        tint = Marfil.copy(alpha = 0.85f),
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Bienvenido de nuevo",
                    color = Marfil,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Ingresa a tu cuenta Minka Art",
                    color = Marfil.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(modifier = Modifier.height(18.dp))

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

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = recuerdame,
                            onCheckedChange = { recuerdame = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = AzulAcero,
                                uncheckedColor = Marfil.copy(alpha = 0.6f),
                                checkmarkColor = Marfil
                            ),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(0.dp))
                        Text(
                            text = "Recuérdame",
                            color = Marfil.copy(alpha = 0.85f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    TextButton(
                        onClick = {

                            if (correo.isBlank()) {

                                Toast.makeText(
                                    context,
                                    "Ingresa tu correo para recuperar la contraseña",
                                    Toast.LENGTH_SHORT
                                ).show()

                                return@TextButton
                            }

                            loginViewModel.recuperarContraseña(
                                correo = correo,

                                onSuccess = {

                                    Toast.makeText(
                                        context,
                                        "Correo de recuperación enviado",
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
                        }
                    ) {
                        Text(
                            text = "¿Olvidaste tu contraseña?",
                            color = Marfil.copy(alpha = 0.85f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                GradientButton(
                    text = "INICIAR SESIÓN",
                    onClick = {

                        if (correo.isBlank() || contraseña.isBlank()) {

                            Toast.makeText(
                                context,
                                "Ingresa correo y contraseña",
                                Toast.LENGTH_SHORT
                            ).show()

                            return@GradientButton
                        }

                        loginViewModel.iniciarSesionYObtenerRol(
                            correo = correo,
                            contraseña = contraseña,

                            onSuccess = { rol ->

                                when (rol.trim().lowercase()) {

                                    "cliente" -> {

                                        navController.navigate(
                                            Routes.HOME_CLIENTE
                                        ) {

                                            popUpTo(Routes.LOGIN) {
                                                inclusive = true
                                            }
                                        }
                                    }

                                    "artesano" -> {

                                        navController.navigate(
                                            Routes.HOME_ARTESANO
                                        ) {

                                            popUpTo(Routes.LOGIN) {
                                                inclusive = true
                                            }
                                        }
                                    }

                                    else -> {

                                        Toast.makeText(
                                            context,
                                            "El rol del usuario no es válido: $rol",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
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

                Spacer(modifier = Modifier.height(14.dp))

                Row {
                    Text(
                        text = "¿No tienes cuenta? ",
                        color = Marfil.copy(alpha = 0.75f),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Regístrate",
                        color = Marfil,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            navController.navigate(Routes.REGISTER)
                        }
                    )
                }
            }
        }
    }
}
