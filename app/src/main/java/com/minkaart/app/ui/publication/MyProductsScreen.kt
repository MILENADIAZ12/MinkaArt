package com.minkaart2.app.ui.publication

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.minkaart2.app.data.model.Product
import com.minkaart2.app.viewModel.ProductViewModel

@Composable
fun MyProductsScreen() {

    val productViewModel: ProductViewModel = viewModel()

    val productos by productViewModel.misProductos
    val cargando by productViewModel.cargando

    val context = LocalContext.current

    LaunchedEffect(Unit) {

        productViewModel.cargarMisProductos(
            onError = { mensaje ->

                Toast.makeText(
                    context,
                    mensaje,
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text(
            text = "Mis publicaciones"
        )

        if (cargando) {

            Column(
                modifier = Modifier
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                CircularProgressIndicator()

                Text(
                    text = "Cargando publicaciones..."
                )
            }

        } else if (productos.isEmpty()) {

            Column(
                modifier = Modifier
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    text = "Aún no tienes productos publicados"
                )
            }

        } else {

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                items(
                    items = productos,
                    key = { producto ->
                        producto.id
                    }
                ) { producto ->

                    ProductCard(
                        producto = producto
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductCard(
    producto: Product
) {

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            if (producto.imagenes.isNotEmpty()) {

                AsyncImage(
                    model = producto.imagenes.first(),
                    contentDescription = producto.nombre,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentScale = ContentScale.Crop
                )
            }

            Text(
                text = producto.nombre
            )

            Text(
                text = "$${producto.precio}"
            )

            Text(
                text = producto.regionOrigen
            )
        }
    }
}