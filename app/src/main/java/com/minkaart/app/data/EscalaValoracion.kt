package com.minkaart2.app.data

import kotlin.math.roundToInt

/**
 * Conversión entre las etiquetas que ve el cliente en `RateSaleScreen` (pills, sin
 * ningún campo de texto) y los puntos usados para calcular promedios — un solo lugar
 * compartido por `SaleRepository` (al guardar) y las pantallas que muestran el nivel
 * (`QualityTrustScreen`), mismo criterio que `CatalogosProducto`.
 *
 * Las dos escalas usan la misma conversión por posición: el primer nivel de la lista
 * vale 4 puntos, el último vale 1 — así `puntos()`/`etiqueta()` no necesitan un `when`
 * distinto por escala.
 */
object EscalaValoracion {

    val NIVELES_CALIDAD = listOf("Excelente", "Buena", "Regular", "Baja")
    val NIVELES_CONFIANZA = listOf("Muy alta", "Alta", "Media", "Baja")

    fun puntos(nivel: String, niveles: List<String>): Int {

        val indice = niveles.indexOf(nivel)
        return if (indice == -1) 0 else niveles.size - indice
    }

    /**
     * Redondea el promedio (escala 1.0–4.0) al entero más cercano (mitad hacia arriba,
     * p. ej. 2.5 → 3) y lo mapea de vuelta a la etiqueta de [niveles]. Si [promedio] es
     * `null` (todavía no hay ninguna valoración), no inventa un nivel — devuelve un
     * estado neutro en vez de "Nivel: Baja", que sería engañoso.
     */
    fun etiqueta(promedio: Double?, niveles: List<String>): String {

        if (promedio == null) {
            return "Aún no hay valoraciones"
        }

        val puntosRedondeados = promedio.roundToInt().coerceIn(1, niveles.size)
        return niveles[niveles.size - puntosRedondeados]
    }
}
