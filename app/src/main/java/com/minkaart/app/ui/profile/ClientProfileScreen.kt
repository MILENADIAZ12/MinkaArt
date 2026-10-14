package com.minkaart2.app.ui.profile

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.minkaart2.app.navigation.Routes
import com.minkaart2.app.ui.components.IconoCamara
import com.minkaart2.app.viewModel.ClientProfileViewModel

private val Guinda = Color(0xFF3B0F1A)
private val Borgona = Color(0xFF66172E)
private val AzulClaro = Color(0xFFB7C7D9)
private val Marfil = Color(0xFFF2EDE6)
private val TextoSecundario = Color(0xFF8A7480)
private val LineSoft = Color(0xFFEFE6E8)

@Composable
fun ClientProfileScreen(navController: NavController) {

    val viewModel: ClientProfileViewModel = viewModel()
    val context = LocalContext.current

    val usuario by viewModel.usuario
    val cargando by viewModel.cargando

    // Un artesano llega aquí cuando está "viendo como cliente" (rol real en
    // Firestore, nunca la ruta) — no debe ver Historial de compras (las ventas exigen
    // rol == "cliente") y necesita una salida de vuelta a su propio Home.
    val esArtesano = usuario?.rol == "artesano"

    val selectorFoto = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->

        if (uri != null) {

            viewModel.actualizarFotoPerfil(
                fotoUri = uri,
                onSuccess = {
                    Toast.makeText(context, "Foto de perfil actualizada", Toast.LENGTH_SHORT).show()
                },
                onError = { mensaje ->
                    Toast.makeText(context, mensaje, Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    LaunchedEffect(Unit) {

        viewModel.cargarPerfil(
            onError = { mensaje ->
                Toast.makeText(context, mensaje, Toast.LENGTH_LONG).show()
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Marfil)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp, 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { navController.popBackStack() }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = Guinda,
                    modifier = Modifier.size(14.dp)
                )
            }

            Text(
                text = "Mi perfil",
                color = Guinda,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 12.dp)
            )
        }

        if (cargando) {

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(color = Borgona)
            }

        } else {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Box {

                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(AzulClaro),
                        contentAlignment = Alignment.Center
                    ) {

                        if (usuario?.fotoUrl != null) {

                            AsyncImage(
                                model = usuario?.fotoUrl,
                                contentDescription = "Foto de perfil",
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )

                        } else {

                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = null,
                                tint = Guinda,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }

                    // Insignia de cámara para cambiar la foto — siempre editable, esta
                    // pantalla es siempre el propio perfil del usuario, nunca el de otro.
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(Borgona)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { selectorFoto.launch("image/*") }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        IconoCamara(
                            modifier = Modifier.size(12.dp),
                            color = Color.White,
                            colorFondo = Borgona
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = usuario?.nombre ?: "Cliente",
                    color = Guinda,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = usuario?.correo ?: "",
                    color = TextoSecundario,
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(modifier = Modifier.height(20.dp))

                MenuItem(
                    icon = Icons.Filled.Edit,
                    texto = "Información básica",
                    onClick = { navController.navigate(Routes.EDIT_PROFILE) }
                )

                MenuItem(
                    icon = Icons.Filled.Favorite,
                    texto = "Mis likes",
                    onClick = { navController.navigate(Routes.LIKES) }
                )

                if (!esArtesano) {

                    MenuItem(
                        icon = Icons.Filled.DateRange,
                        texto = "Historial de compras",
                        onClick = { navController.navigate(Routes.PURCHASE_HISTORY) }
                    )
                }

                MenuItem(
                    icon = Icons.Filled.Settings,
                    texto = "Configuración de la cuenta",
                    onClick = { navController.navigate(Routes.ACCOUNT_SETTINGS) }
                )

                if (esArtesano) {

                    MenuItem(
                        icon = Icons.Filled.Build,
                        texto = "Cambiar a modo artesano",
                        onClick = {

                            navController.navigate(Routes.HOME_ARTESANO) {
                                popUpTo(Routes.HOME_ARTESANO) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    )
                }

                MenuItem(
                    icon = Icons.Filled.ExitToApp,
                    texto = "Cerrar sesión",
                    esDestructivo = true,
                    onClick = {

                        viewModel.cerrarSesion()

                        navController.navigate(Routes.WELCOME) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun MenuItem(
    icon: ImageVector,
    texto: String,
    onClick: () -> Unit,
    esDestructivo: Boolean = false
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (esDestructivo) Borgona else Guinda,
            modifier = Modifier.size(18.dp)
        )

        Text(
            text = texto,
            color = if (esDestructivo) Borgona else Guinda,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (esDestructivo) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        )

        if (!esDestructivo) {

            Icon(
                imageVector = Icons.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = TextoSecundario,
                modifier = Modifier.size(16.dp)
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(LineSoft)
    )
}
