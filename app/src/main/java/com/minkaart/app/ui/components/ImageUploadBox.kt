package com.minkaart2.app.ui.components

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

private val Marfil = Color(0xFFF2EDE6)
private val Borgona = Color(0xFF66172E)
private val LineSoft = Color(0xFFD9C3CA)
private val IconMuted = Color(0xFFB79AA4)

/**
 * Caja de selección de imágenes compartida entre Publicar y Editar producto: muestra en
 * fila las miniaturas ya guardadas ([imagenesExistentes], solo en Editar) seguidas de las
 * recién elegidas ([imagenesNuevas]), cada una con su botón de eliminar, y una casilla de
 * "agregar" al final mientras no se llegue a [maxImagenes]. La selección misma
 * (ActivityResultContracts.GetMultipleContents()) la dispara cada pantalla vía
 * [onClickAgregar] — este componente es solo la presentación visual.
 */
@Composable
fun ImageUploadBox(
    onClickAgregar: () -> Unit,
    onEliminarExistente: (String) -> Unit = {},
    onEliminarNueva: (Uri) -> Unit = {},
    modifier: Modifier = Modifier,
    imagenesExistentes: List<String> = emptyList(),
    imagenesNuevas: List<Uri> = emptyList(),
    maxImagenes: Int = 15
) {
    val total = imagenesExistentes.size + imagenesNuevas.size

    Column(modifier = modifier.fillMaxWidth()) {

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            items(imagenesExistentes) { url ->
                MiniaturaImagen(
                    modelo = url,
                    onEliminar = { onEliminarExistente(url) }
                )
            }

            items(imagenesNuevas) { uri ->
                MiniaturaImagen(
                    modelo = uri,
                    onEliminar = { onEliminarNueva(uri) }
                )
            }

            if (total < maxImagenes) {

                item {
                    Column(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Marfil, RoundedCornerShape(14.dp))
                            .border(
                                BorderStroke(1.6.dp, LineSoft),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onClickAgregar
                            ),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            tint = IconMuted
                        )

                        Text(
                            text = "Agregar",
                            color = Borgona,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }

        Text(
            text = "$total/$maxImagenes imágenes *",
            color = IconMuted,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun MiniaturaImagen(
    modelo: Any,
    onEliminar: () -> Unit
) {
    Box(modifier = Modifier.size(90.dp)) {

        AsyncImage(
            model = modelo,
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .size(90.dp)
                .clip(RoundedCornerShape(14.dp))
                .border(BorderStroke(1.dp, LineSoft), RoundedCornerShape(14.dp)),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(20.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onEliminar
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Quitar imagen",
                tint = Color.White,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}
