package com.minkaart2.app.ui.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Campo de texto con el lenguaje visual "de vidrio" usado en Login y Registro: ícono a la
 * izquierda, sin caja, con una línea inferior en vez de un borde completo. Envuelve el
 * [TextField] de Material 3 estándar para no perder ningún comportamiento (teclado,
 * transformación visual para contraseñas, una o varias líneas) que ya usan las pantallas.
 */
@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    val marfil = Color(0xFFF2EDE6)

    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = { Text(label) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = marfil.copy(alpha = 0.75f)
            )
        },
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation,
        textStyle = MaterialTheme.typography.bodyMedium,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            focusedTextColor = marfil,
            unfocusedTextColor = marfil.copy(alpha = 0.9f),
            focusedLabelColor = marfil.copy(alpha = 0.85f),
            unfocusedLabelColor = marfil.copy(alpha = 0.7f),
            focusedIndicatorColor = marfil.copy(alpha = 0.6f),
            unfocusedIndicatorColor = marfil.copy(alpha = 0.3f),
            cursorColor = marfil
        )
    )
}
