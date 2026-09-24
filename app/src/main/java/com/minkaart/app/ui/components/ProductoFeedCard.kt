package com.minkaart2.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.minkaart2.app.data.model.Product

private val Guinda = Color(0xFF3B0F1A)
private val Borgona = Color(0xFF66172E)
private val Marfil = Color(0xFFF2EDE6)
private val TextoSecundario = Color(0xFF8A7480)

/**
 * Tarjeta de producto compartida entre el feed del cliente (`HomeClienteScreen`) y la
 * grilla de productos del Perfil del artesano (`ArtisanProfileScreen`) — antes vivía como
 * `private fun` dentro de `HomeClienteScreen.kt`.
 *
 * El nombre del artesano tiene su propio `clickable` (independiente del de toda la
 * tarjeta) para poder navegar a su perfil sin abrir el detalle del producto.
 */
@Composable
fun ProductoFeedCard(
    producto: Product,
    nombreArtesano: String?,
    leGustaAlUsuario: Boolean,
    onClick: () -> Unit,
    onLikeClick: () -> Unit,
    onArtesanoClick: () -> Unit = {},
    // Confianza del ARTESANO dueño del producto (Fase 6, corrige un error de fases
    // anteriores: la confianza vivía por error en Product.confianzaPromedio). Viene en
    // escala 1.0-4.0 (o null si el artesano no tiene ninguna valoración todavía) — se
    // pasa tal cual a BarraValoracionCompacta, que hace su propia normalización a 0-1.
    confianzaArtesano: Double? = null
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.1f)
                .background(Marfil)
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
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.92f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onLikeClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (leGustaAlUsuario) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = "Me gusta",
                    tint = Borgona,
                    modifier = Modifier.size(12.dp)
                )
            }
        }

        Column(modifier = Modifier.padding(8.dp)) {

            Text(
                text = producto.nombre,
                color = Guinda,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = nombreArtesano ?: "",
                color = TextoSecundario,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(bottom = 6.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onArtesanoClick
                    )
            )

            BarraValoracionCompacta(promedio = confianzaArtesano)
        }
    }
}
