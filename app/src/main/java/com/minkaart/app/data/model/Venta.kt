package com.minkaart2.app.data.model

import com.google.firebase.Timestamp

/**
 * Documento de la colección `ventas/{autoId}` (Fase 6). El artesano la crea desde
 * `RegisterSaleScreen`; el cliente la valora una sola vez desde `RateSaleScreen`
 * (`valorado` pasa a `true` junto con la creación de `valoraciones/{id-de-esta-venta}`).
 */
data class Venta(

    val id: String = "",
    val productoId: String = "",
    val artesanoId: String = "",
    val clienteId: String = "",
    val fechaVenta: Timestamp = Timestamp.now(),
    val valorado: Boolean = false,
    val creadoEn: Timestamp? = null

)
