package com.minkaart2.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.minkaart2.app.R

private val Guinda = Color(0xFF3B0F1A)
private val Borgona = Color(0xFF66172E)
private val Marfil = Color(0xFFF2EDE6)
private val LineSoft = Color(0xFFECE0E3)

/**
 * Selector de una lista fija, con el mismo lenguaje visual que [MinkaFormField] (etiqueta
 * pequeña en mayúsculas + caja de fondo Marfil), pero de solo selección (no editable a
 * mano). Se reutiliza para Departamento, Categoría y Clasificación en Publicar/Editar
 * producto — evita construir un selector distinto por campo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MinkaDropdownField(
    label: String,
    opciones: List<String>,
    valorSeleccionado: String,
    onSeleccionar: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = stringResource(R.string.selecciona_opcion),
    // Texto mostrado para cada valor: lo guardado en Firestore sigue siendo el valor en
    // español de `opciones`, esto solo lo traduce en pantalla (ver util/Etiquetas.kt).
    etiqueta: @Composable (String) -> String = { it }
) {
    var expandido by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {

        Text(
            text = label.uppercase(),
            color = Borgona,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        ExposedDropdownMenuBox(
            expanded = expandido,
            onExpandedChange = { expandido = it }
        ) {

            TextField(
                value = if (valorSeleccionado.isEmpty()) "" else etiqueta(valorSeleccionado),
                onValueChange = {},
                readOnly = true,
                placeholder = { Text(placeholder, color = Guinda.copy(alpha = 0.35f)) },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandido)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                textStyle = MaterialTheme.typography.bodyMedium,
                shape = RoundedCornerShape(10.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Marfil,
                    unfocusedContainerColor = Marfil,
                    disabledContainerColor = Marfil,
                    focusedTextColor = Guinda,
                    unfocusedTextColor = Guinda,
                    focusedIndicatorColor = Borgona.copy(alpha = 0.4f),
                    unfocusedIndicatorColor = LineSoft,
                    cursorColor = Borgona
                )
            )

            ExposedDropdownMenu(
                expanded = expandido,
                onDismissRequest = { expandido = false }
            ) {

                opciones.forEach { opcion ->

                    DropdownMenuItem(
                        text = { Text(etiqueta(opcion)) },
                        onClick = {
                            onSeleccionar(opcion)
                            expandido = false
                        }
                    )
                }
            }
        }
    }
}
