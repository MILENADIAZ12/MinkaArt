package com.minkaart2.app.ui.publication

import android.net.Uri
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.minkaart2.app.ui.components.GradientButton
import com.minkaart2.app.ui.components.ImageUploadBox
import com.minkaart2.app.ui.components.MinkaFormField
import com.minkaart2.app.viewModel.ProductViewModel

private val Guinda = Color(0xFF3B0F1A)
private val Marfil = Color(0xFFF2EDE6)
private val LineSoft = Color(0xFFEFE6E8)

@Composable
fun PublicationScreen(navController: NavController) {

    var nombre by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var precio by remember { mutableStateOf("") }
    var historiaProceso by remember { mutableStateOf("") }
    var regionOrigen by remember { mutableStateOf("") }
    var categoria by remember { mutableStateOf("") }

    var imagenUri by remember {
        mutableStateOf<Uri?>(null)
    }

    val productViewModel: ProductViewModel = viewModel()
    val context = LocalContext.current

    val selectorImagen = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->

        if (uri != null) {
            imagenUri = uri
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Marfil)
            .statusBarsPadding()
    ) {

        // Topbar
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
                text = "Publicar artesanía",
                color = Guinda,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 12.dp)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(LineSoft)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {

            ImageUploadBox(
                onClick = { selectorImagen.launch("image/*") },
                pickedUri = imagenUri,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            MinkaFormField(
                label = "Nombre de la artesanía",
                value = nombre,
                onValueChange = { nombre = it },
                placeholder = "Ej. Ruana en lana virgen"
            )

            MinkaFormField(
                label = "Descripción",
                value = descripcion,
                onValueChange = { descripcion = it },
                singleLine = false,
                minLines = 3,
                placeholder = "Breve descripción del producto…"
            )

            MinkaFormField(
                label = "Precio",
                value = precio,
                onValueChange = { nuevoValor ->

                    if (nuevoValor.all { it.isDigit() }) {
                        precio = nuevoValor
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                placeholder = "$ 0"
            )

            MinkaFormField(
                label = "Historia y proceso de elaboración",
                value = historiaProceso,
                onValueChange = { historiaProceso = it },
                singleLine = false,
                minLines = 4,
                placeholder = "Cómo se elabora, técnica y tradición…"
            )

            MinkaFormField(
                label = "Región de origen",
                value = regionOrigen,
                onValueChange = { regionOrigen = it },
                placeholder = "Ej: Bogotá, Boyacá, La Guajira"
            )

            MinkaFormField(
                label = "Categoría",
                value = categoria,
                onValueChange = { categoria = it },
                placeholder = "Ej: Tejidos, Cerámica, Joyería"
            )

            if (imagenUri != null) {

                Text(
                    text = "Imagen seleccionada correctamente",
                    color = Guinda.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            GradientButton(
                text = "Publicar",
                onClick = {

                    val precioLong = precio.toLongOrNull() ?: 0L

                    productViewModel.publicarProducto(
                        nombre = nombre,
                        descripcion = descripcion,
                        precio = precioLong,
                        historiaProceso = historiaProceso,
                        regionOrigen = regionOrigen,
                        categoria = categoria,
                        imagenUri = imagenUri,

                        onSuccess = {

                            Toast.makeText(
                                context,
                                "Producto publicado correctamente",
                                Toast.LENGTH_SHORT
                            ).show()

                            nombre = ""
                            descripcion = ""
                            precio = ""
                            historiaProceso = ""
                            regionOrigen = ""
                            categoria = ""
                            imagenUri = null
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

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
