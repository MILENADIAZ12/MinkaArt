package com.minkaart2.app.ui.login

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.minkaart2.app.navigation.Routes
import com.minkaart2.app.viewModel.LoginViewModel

@Composable
fun LoginScreen(
    navController: NavController
) {

    var correo by remember { mutableStateOf("") }
    var contraseña by remember { mutableStateOf("") }

    val loginViewModel: LoginViewModel = viewModel()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "Iniciar Sesión"
        )

        OutlinedTextField(
            value = correo,
            onValueChange = { correo = it },
            label = {
                Text("Correo")
            },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = contraseña,
            onValueChange = { contraseña = it },
            label = {
                Text("Contraseña")
            },
            modifier = Modifier.fillMaxWidth()
        )

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

            Text("¿Olvidaste tu contraseña?")
        }

        Button(
            onClick = {

                if (correo.isBlank() || contraseña.isBlank()) {

                    Toast.makeText(
                        context,
                        "Ingresa correo y contraseña",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@Button
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
        ) {

            Text("Iniciar sesión")
        }
    }
}