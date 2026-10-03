package com.minkaart2.app.ui.sales

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.Timestamp
import com.minkaart2.app.ui.components.GradientButton
import com.minkaart2.app.ui.theme.VerdeConfirmacion
import com.minkaart2.app.viewModel.RegisterSaleViewModel
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Date

private val Guinda = Color(0xFF3B0F1A)
private val Borgona = Color(0xFF66172E)
private val AzulClaro = Color(0xFFB7C7D9)
private val Marfil = Color(0xFFF2EDE6)
private val TextoSecundario = Color(0xFF8A7480)
private val LineSoft = Color(0xFFEFE6E8)

/**
 * Se llega desde "Registrar venta" en el menú de tres puntos de una fila de
 * `MyProductsScreen` — el producto ya viene elegido, no se puede cambiar desde acá
 * (Fase 6).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterSaleScreen(
    productId: String,
    navController: NavController
) {

    val viewModel: RegisterSaleViewModel = viewModel()
    val context = LocalContext.current

    val producto by viewModel.producto
    val clientes by viewModel.clientes
    val clientesCargados by viewModel.clientesCargados
    val cargando by viewModel.cargando

    var correoCliente by remember { mutableStateOf("") }
    var mostrarCalendario by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )

    var fechaVentaMillis by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(productId) {

        viewModel.cargarDatos(
            productoId = productId,
            onError = { mensaje ->
                Toast.makeText(context, mensaje, Toast.LENGTH_LONG).show()
                navController.popBackStack()
            }
        )
    }

    // El artesano escribe el correo completo del cliente (que el cliente le manda por
    // WhatsApp desde "Contactar artesano"); se valida contra los clientes registrados.
    val clienteEncontrado = remember(clientes, correoCliente) {

        val correoBuscado = correoCliente.trim()

        if (correoBuscado.isBlank()) {
            null
        } else {
            clientes.firstOrNull { it.correo.trim().equals(correoBuscado, ignoreCase = true) }
        }
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
                .background(Color.White)
                .padding(16.dp, 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Marfil)
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
                text = "Registrar venta",
                color = Guinda,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 12.dp)
            )
        }

        if (cargando || producto == null) {

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(color = Borgona)
            }

        } else {

            val productoActual = producto!!

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {

                Text(
                    text = "PRODUCTO VENDIDO",
                    color = Borgona,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Marfil, RoundedCornerShape(14.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(AzulClaro)
                    ) {

                        if (productoActual.imagenes.isNotEmpty()) {

                            AsyncImage(
                                model = productoActual.imagenes.first(),
                                contentDescription = productoActual.nombre,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    Column(modifier = Modifier.padding(start = 10.dp)) {

                        Text(
                            text = productoActual.nombre,
                            color = Guinda,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = formatearPrecio(productoActual.precio),
                            color = Borgona,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "CORREO DEL CLIENTE *",
                    color = Borgona,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Marfil)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Icon(
                        imageVector = Icons.Filled.Email,
                        contentDescription = null,
                        tint = TextoSecundario,
                        modifier = Modifier.size(16.dp)
                    )

                    Box(modifier = Modifier.padding(start = 8.dp)) {

                        if (correoCliente.isEmpty()) {

                            Text(
                                text = "correo@ejemplo.com",
                                color = TextoSecundario,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        BasicTextField(
                            value = correoCliente,
                            onValueChange = { correoCliente = it },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            textStyle = MaterialTheme.typography.bodySmall.copy(color = Guinda),
                            cursorBrush = SolidColor(Borgona),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                if (correoCliente.isNotBlank() && clienteEncontrado != null) {

                    Text(
                        text = "Correo correcto · ${clienteEncontrado.nombre}",
                        color = VerdeConfirmacion,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 6.dp)
                    )

                } else if (correoCliente.isNotBlank() && clientesCargados) {

                    Text(
                        text = "No existe",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "FECHA DE VENTA",
                    color = Borgona,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Marfil)
                        .border(1.dp, LineSoft, RoundedCornerShape(10.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { mostrarCalendario = true }
                        )
                        .padding(horizontal = 12.dp, vertical = 12.dp)
                ) {

                    Text(
                        text = formatearFecha(fechaVentaMillis),
                        color = Guinda,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                GradientButton(
                    text = "Confirmar venta",
                    enabled = clienteEncontrado != null,
                    onClick = {

                        val cliente = clienteEncontrado ?: return@GradientButton

                        viewModel.registrarVenta(
                            clienteId = cliente.uid,
                            fechaVenta = Timestamp(Date(fechaVentaMillis)),

                            onSuccess = {

                                Toast.makeText(
                                    context,
                                    "Venta registrada correctamente",
                                    Toast.LENGTH_SHORT
                                ).show()

                                navController.popBackStack()
                            },

                            onError = { mensaje ->
                                Toast.makeText(context, mensaje, Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (mostrarCalendario) {

        DatePickerDialog(
            onDismissRequest = { mostrarCalendario = false },
            confirmButton = {

                TextButton(
                    onClick = {

                        val seleccionMillis = datePickerState.selectedDateMillis

                        if (seleccionMillis != null) {
                            fechaVentaMillis = seleccionMillis
                        }

                        mostrarCalendario = false
                    }
                ) {
                    Text("Aceptar")
                }
            },
            dismissButton = {

                TextButton(onClick = { mostrarCalendario = false }) {
                    Text("Cancelar")
                }
            }
        ) {

            DatePicker(state = datePickerState)
        }
    }
}

private fun formatearPrecio(precio: Long): String {

    val formato = java.text.NumberFormat.getNumberInstance(java.util.Locale("es", "CO"))
    return "$${formato.format(precio)}"
}

private fun formatearFecha(millis: Long): String {

    val fecha = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
    return fecha.format(DateTimeFormatter.ofPattern("dd / MM / yyyy"))
}
