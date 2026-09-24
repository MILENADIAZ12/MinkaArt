package com.minkaart2.app.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.minkaart2.app.data.model.Notificacion

/**
 * Notificaciones de la campana — dominio aparte de `AuthRepository`/`ProductRepository`/
 * `SaleRepository`, mismo criterio de "un repositorio por dominio" que ya separa esos
 * tres. Solo lee y marca como leída: la app **nunca crea** un documento de
 * `notificaciones` (eso lo hacen las Cloud Functions, ver `functions/src/index.ts`), así
 * que este repositorio no tiene ningún `guardar`/`crear`.
 */
class NotificationRepository {

    private val db = FirebaseFirestore.getInstance()

    /**
     * Sin `orderBy` en el query (para no depender de un índice compuesto) — mismo
     * criterio que `SaleRepository.obtenerVentasDelCliente`: se ordena en memoria por
     * fecha, más reciente primero.
     */
    fun obtenerNotificaciones(
        uid: String,
        onSuccess: (List<Notificacion>) -> Unit,
        onError: (String) -> Unit
    ) {

        db.collection("notificaciones")
            .whereEqualTo("destinatarioId", uid)
            .get()
            .addOnSuccessListener { resultado ->

                val notificaciones = resultado.documents
                    .mapNotNull { it.toObject(Notificacion::class.java) }
                    .sortedByDescending { it.creadoEn?.seconds ?: 0L }

                onSuccess(notificaciones)
            }
            .addOnFailureListener { error ->
                onError(error.message ?: "Error al consultar las notificaciones")
            }
    }

    fun marcarComoLeida(
        notificacionId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {

        db.collection("notificaciones")
            .document(notificacionId)
            .update("leida", true)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { error ->
                onError(error.message ?: "Error al marcar la notificación como leída")
            }
    }

    fun marcarTodasComoLeidas(
        idsNoLeidas: List<String>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {

        if (idsNoLeidas.isEmpty()) {
            onSuccess()
            return
        }

        val lote = db.batch()

        idsNoLeidas.forEach { id ->
            lote.update(db.collection("notificaciones").document(id), "leida", true)
        }

        lote.commit()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { error ->
                onError(error.message ?: "Error al marcar las notificaciones como leídas")
            }
    }
}
