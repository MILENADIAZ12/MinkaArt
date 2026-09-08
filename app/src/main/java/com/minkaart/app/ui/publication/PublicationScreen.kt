package com.minkaart2.app.ui.publication

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.minkaart2.app.viewModel.ProductViewModel

@Composable
fun PublicationScreen() {

    var nombre by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var precio by remember { mutableStateOf("") }
    var historiaProceso by remember { mutableStateOf("") }
    var regionOrigen by remember { mutableStateOf("") }

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
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Text(
            text = "Registrar producto"
        )

        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = {
                Text("Nombre de la artesanía")
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = descripcion,
            onValueChange = { descripcion = it },
            label = {
                Text("Descripción")
            },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        OutlinedTextField(
            value = precio,
            onValueChange = { nuevoValor ->

                if (nuevoValor.all { it.isDigit() }) {
                    precio = nuevoValor
                }
            },
            label = {
                Text("Precio")
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number
            )
        )

        OutlinedTextField(
            value = historiaProceso,
            onValueChange = { historiaProceso = it },
            label = {
                Text("Historia y proceso de elaboración")
            },
            modifier = Modifier.fillMaxWidth(),
            minLines = 4
        )

        OutlinedTextField(
            value = regionOrigen,
            onValueChange = { regionOrigen = it },
            label = {
                Text("Región de origen")
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = "Imagen de la artesanía"
        )

        OutlinedButton(
            onClick = {
                selectorImagen.launch("image/*")
            },
            modifier = Modifier.fillMaxWidth()
        ) {

            Text(
                if (imagenUri == null) {
                    "Seleccionar imagen"
                } else {
                    "Cambiar imagen"
                }
            )
        }

        if (imagenUri != null) {

            Text(
                text = "Imagen seleccionada correctamente"
            )
        }

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Button(
            onClick = {

                val precioLong = precio.toLongOrNull() ?: 0L

                productViewModel.publicarProducto(
                    nombre = nombre,
                    descripcion = descripcion,
                    precio = precioLong,
                    historiaProceso = historiaProceso,
                    regionOrigen = regionOrigen,
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
        ) {

            Text("Publicar")
        }
    }
}