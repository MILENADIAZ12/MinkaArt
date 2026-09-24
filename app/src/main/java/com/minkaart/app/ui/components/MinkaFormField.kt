package com.minkaart2.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val Guinda = Color(0xFF3B0F1A)
private val Borgona = Color(0xFF66172E)
private val Marfil = Color(0xFFF2EDE6)
private val LineSoft = Color(0xFFECE0E3)

/**
 * Campo de formulario para pantallas de fondo claro (Publicar/Editar producto), replicando
 * el patrón "field-label" + "field-input" del mockup: una etiqueta pequeña en mayúsculas
 * encima del campo, en vez de la etiqueta flotante de Material. Envuelve un [TextField]
 * estándar (mismo patrón que GlassTextField de la Fase 1, pero para fondo claro) para no
 * perder capacidades que ya usan los formularios actuales.
 */
@Composable
fun MinkaFormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    placeholder: String? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {

        Text(
            text = label.uppercase(),
            color = Borgona,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )

        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = if (placeholder != null) {
                { Text(placeholder, color = Guinda.copy(alpha = 0.35f)) }
            } else {
                null
            },
            singleLine = singleLine,
            minLines = minLines,
            keyboardOptions = keyboardOptions,
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
    }
}
