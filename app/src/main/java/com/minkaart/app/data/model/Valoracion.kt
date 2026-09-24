package com.minkaart2.app.data.model

import com.google.firebase.Timestamp

/**
 * Documento de la colección `valoraciones/{ventaId}` (Fase 6) — usa el mismo id que la
 * venta que valora, así una venta solo se puede valorar una vez sin lógica extra (un
 * `create` en un id ya ocupado lo rechaza Firestore). Nunca se edita ni se borra (reglas:
 * `allow update, delete: if false`), a propósito, para que el puntaje no se pueda
 * manipular después de aprobado.
 */
data class Valoracion(

    val id: String = "",
    val productoId: String = "",
    val artesanoId: String = "",
    val clienteId: String = "",
    val calidad: String = "",
    val confianza: String = "",
    val creadoEn: Timestamp? = null

)
