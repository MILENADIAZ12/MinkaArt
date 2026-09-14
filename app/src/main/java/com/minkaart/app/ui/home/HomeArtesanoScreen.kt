package com.minkaart2.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import android.widget.Toast
import coil.compose.AsyncImage
import com.minkaart2.app.ui.components.GradientButton
import com.minkaart2.app.ui.components.OutlineButton
import com.minkaart2.app.viewModel.HomeArtesanoViewModel
import com.minkaart2.app.viewModel.ProductViewModel

private val Guinda = Color(0xFF3B0F1A)
private val Borgona = Color(0xFF66172E)
private val AzulClaro = Color(0xFFB7C7D9)
private val Marfil = Color(0xFFF2EDE6)

@Composable
fun HomeArtesanoScreen(
    onPublicarClick: () -> Unit,
    onMisPublicacionesClick: () -> Unit
) {

    val homeViewModel: HomeArtesanoViewModel = viewModel()
    val productViewModel: ProductViewModel = viewModel()
    val context = LocalContext.current

    val nombreArtesano by homeViewModel.nombreArtesano
    val productos by productViewModel.misProductos

    LaunchedEffect(Unit) {

        homeViewModel.cargarDatosArtesano(
            onError = { /* si falla, el saludo simplemente queda sin nombre */ }
        )

        productViewModel.cargarMisProductos(
            onError = { mensaje ->
                Toast.makeText(context, mensaje, Toast.LENGTH_LONG).show()
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Marfil)
            .statusBarsPadding()
    ) {

        // Encabezado
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Marfil)
                .padding(18.dp),
            horizontalArrangement = Arrangement.Start
        ) {

            Box(
                modifier = Modifier
                    .height(28.dp)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Guinda)
            )

            Text(
                text = "Minka Art",
                color = Guinda,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 10.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
        ) {

            Text(
                text = "Hola,",
                color = Color(0xFF8A7480),
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                text = nombreArtesano.ifBlank { "artesana/o" },
                color = Guinda,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                GradientButton(
                    text = "Publicar",
                    icon = Icons.Filled.Add,
                    onClick = onPublicarClick,
                    modifier = Modifier.weight(1f)
                )

                OutlineButton(
                    text = "Ver publicaciones",
                    icon = Icons.Filled.List,
                    borderColor = Borgona,
                    textColor = Borgona,
                    onClick = onMisPublicacionesClick,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (productos.isEmpty()) {

                Text(
                    text = "Aún no tienes productos publicados",
                    color = Color(0xFF8A7480),
                    style = MaterialTheme.typography.bodySmall
                )

            } else {

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp),
                    modifier = Modifier.height(220.dp)
                ) {

                    items(productos.take(4)) { producto ->

                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(AzulClaro)
                        ) {

                            if (producto.imagenes.isNotEmpty()) {

                                AsyncImage(
                                    model = producto.imagenes.first(),
                                    contentDescription = producto.nombre,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(8.dp)
                                    .background(
                                        Color.White.copy(alpha = 0.92f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = producto.nombre,
                                    color = Guinda,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
