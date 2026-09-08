
package com.minkaart2.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Paleta Minka Art — tokens tomados del brand kit.
 * Estos son los colores crudos. El mapeo a los roles de Material 3
 * (primary, background, surface, etc.) se hace en Theme.kt.
 */

// Fondos claros
val Beige = Color(0xFFF1E3D0)          // fondo principal de la app
val Cream = Color(0xFFFBF5EA)          // superficies, tarjetas
val WarmWhite = Color(0xFFFFFDF9)      // campos de texto, superficies elevadas

// Cafés
val Espresso = Color(0xFF2B1B10)       // fondos oscuros, hero
val EspressoDeep = Color(0xFF3B2718)   // degradado sobre Espresso
val BrownDeep = Color(0xFF4A3122)      // texto principal
val BrownMid = Color(0xFF8A5A3B)       // texto secundario
val BrownSoft = Color(0xFFB08662)      // texto terciario, iconos apagados

// Terracota — color de acento y acción
val Terracotta = Color(0xFFC6704E)     // botones primarios, chip activo
val TerracottaLight = Color(0xFFE4936C) // acento sobre fondos oscuros
val TerracottaDark = Color(0xFFA2543A)  // estado presionado, subtítulos de sección

// Rosa arcilla — acento suave
val PinkClay = Color(0xFFE7BFB6)
val PinkClaySoft = Color(0xFFF3DCD5)

// Líneas y bordes
val LineLight = Color(0xFFE2CFB6)      // bordes sobre fondo claro
val LineDark = Color(0xFF523620)       // bordes sobre fondo oscuro

// -------------------------------------------------------------------
// Temporal: colores del template de Android Studio.
// Theme.kt todavía los referencia. Se eliminan en el siguiente paso.
// -------------------------------------------------------------------
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

