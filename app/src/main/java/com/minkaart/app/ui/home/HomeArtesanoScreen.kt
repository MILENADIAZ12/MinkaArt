package com.minkaart2.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeArtesanoScreen(
    onPublicarClick: () -> Unit,
    onMisPublicacionesClick: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "Home Artesano"
        )

        Button(
            onClick = onPublicarClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Publicar artesanía")
        }

        Button(
            onClick = onMisPublicacionesClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Mis publicaciones")
        }
    }
}