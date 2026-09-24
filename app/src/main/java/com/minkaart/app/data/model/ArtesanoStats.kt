package com.minkaart2.app.data.model

/**
 * Documento de la colección `artesanoStats/{artesanoId}` (Fase 6). Guarda la confianza
 * acumulada de UN artesano — a diferencia de la calidad, que es por producto, la
 * confianza es una sola cifra por persona, alimentada por todas sus ventas valoradas.
 * No existe hasta que su artesano recibe su primera valoración; mientras tanto se trata
 * como `ArtesanoStats()` (todo en 0/null), no como un error.
 */
data class ArtesanoStats(

    val confianzaSumaPuntos: Int = 0,
    val confianzaConteo: Int = 0,
    val confianzaPromedio: Double? = null

)
