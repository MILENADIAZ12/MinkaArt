package com.minkaart2.app.data.model

import com.google.firebase.Timestamp

/**
 * Documento de la colección `notificaciones/{autoId}` (campana de notificaciones). A
 * diferencia del resto de colecciones del proyecto, **solo las Cloud Functions la
 * escriben** (`allow create: if false` en las reglas) — así ningún usuario puede
 * fabricar un aviso falso para otro. El propio destinatario solo puede marcarla como
 * leída (`allow update` restringido al campo `leida`).
 *
 * [tipo] es una de: "venta" (venta registrada, destinatario cliente), "valoracion"
 * (valoración recibida, destinatario artesano), "like" (me gusta recibido, destinatario
 * artesano), "publicacion" (producto nuevo de un artesano al que se sigue, destinatario
 * seguidor), "ruac_aprobado" / "ruac_rechazado" (revisión de RUAC, destinatario
 * artesano). [referenciaId] cambia de significado según [tipo]: ventaId, productoId o
 * uid del artesano — ver `Routes.rutaDeNotificacion`, que es el único lugar que interpreta
 * esa combinación para navegar.
 */
data class Notificacion(

    val id: String = "",
    val destinatarioId: String = "",
    val tipo: String = "",
    val titulo: String = "",
    val mensaje: String = "",
    val referenciaId: String = "",
    val leida: Boolean = false,
    val creadoEn: Timestamp? = null

)
