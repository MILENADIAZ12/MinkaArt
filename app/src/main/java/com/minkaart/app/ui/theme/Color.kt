
package com.minkaart2.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Paleta Minka Art — brand kit v2.
 * Estos son los colores crudos. El mapeo a los roles de Material 3
 * (primary, background, surface, etc.) se hace en Theme.kt.
 */

val Guinda = Color(0xFF3B0F1A)      // Vinotinto profundo — fondos oscuros, texto principal
val Borgona = Color(0xFF66172E)     // Marca, botones, degradados
val AzulClaro = Color(0xFFB7C7D9)   // Detalles, fondos suaves
val AzulAcero = Color(0xFF55708E)   // Acentos fríos, degradado de botones
val Marfil = Color(0xFFF2EDE6)      // Blanco cálido — fondo principal, texto sobre oscuro
val VerdeConfirmacion = Color(0xFF2E7D32) // Mensajes de validación correcta (p. ej. "Correo correcto")

// Campos de formulario/filtros (panel de filtros, configuración)
val FondoCampo = Color(0xFFFBF8F4)  // Blanco cálido de los campos y tarjetas
val BordeCampo = Color(0xFFD9D2C9)  // Borde gris claro en estado normal
val TextoPlaceholder = Color(0xFF6F6A66)
val Dorado = Color(0xFFC9A24B)      // Ícono de insignia RUAC

/**
 * Muestra de color para el filtro de color: el nombre es el valor guardado en
 * `Product.color` (`CatalogosProducto.COLORES`). `null` = sin muestra sólida (Multicolor
 * se dibuja con degradado aparte).
 */
fun muestraDeColor(nombre: String): Color? = when (nombre) {
    "Natural/crudo" -> Color(0xFFD8C9AC)
    "Blanco" -> Color(0xFFFFFFFF)
    "Negro" -> Color(0xFF1C1A1A)
    "Café" -> Color(0xFF6B4226)
    "Rojo" -> Color(0xFFB3202C)
    "Naranja" -> Color(0xFFE07B22)
    "Amarillo" -> Color(0xFFE8C531)
    "Verde" -> Color(0xFF3F8A4B)
    "Azul" -> Color(0xFF2F5FA8)
    "Morado" -> Color(0xFF6E3F8F)
    "Rosado" -> Color(0xFFE88AA8)
    "Gris" -> Color(0xFF8D8D8D)
    else -> null
}
