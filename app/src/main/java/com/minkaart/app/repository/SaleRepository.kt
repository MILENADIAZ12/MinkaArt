package com.minkaart2.app.repository

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.minkaart2.app.data.EscalaValoracion
import com.minkaart2.app.data.model.ArtesanoStats
import com.minkaart2.app.data.model.Valoracion
import com.minkaart2.app.data.model.Venta

/**
 * Ventas, valoraciones y confianza acumulada del artesano (Fase 6) — dominio aparte de
 * `ProductRepository`/`AuthRepository`, mismo criterio de "un repositorio por dominio"
 * que ya separa esos dos.
 */
class SaleRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun registrarVenta(
        productoId: String,
        artesanoId: String,
        clienteId: String,
        fechaVenta: Timestamp,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {

        val documento = db.collection("ventas").document()

        val venta = Venta(
            id = documento.id,
            productoId = productoId,
            artesanoId = artesanoId,
            clienteId = clienteId,
            fechaVenta = fechaVenta,
            valorado = false,
            creadoEn = Timestamp.now()
        )

        documento.set(venta)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { error ->
                onError(error.message ?: "Error al registrar la venta")
            }
    }

    /**
     * Ventas del cliente actual, más recientes primero — sin `orderBy` en el query (para
     * no depender de un índice compuesto), ordenadas en memoria, mismo criterio que
     * `obtenerTodosLosProductosActivos`.
     */
    fun obtenerVentasDelCliente(
        clienteId: String,
        onSuccess: (List<Venta>) -> Unit,
        onError: (String) -> Unit
    ) {

        db.collection("ventas")
            .whereEqualTo("clienteId", clienteId)
            .get()
            .addOnSuccessListener { resultado ->

                val ventas = resultado.documents
                    .mapNotNull { it.toObject(Venta::class.java) }
                    .sortedByDescending { it.fechaVenta.seconds }

                onSuccess(ventas)
            }
            .addOnFailureListener { error ->
                onError(error.message ?: "Error al consultar el historial de compras")
            }
    }

    fun obtenerVentaPorId(
        ventaId: String,
        onSuccess: (Venta) -> Unit,
        onError: (String) -> Unit
    ) {

        db.collection("ventas")
            .document(ventaId)
            .get()
            .addOnSuccessListener { documento ->

                val venta = documento.toObject(Venta::class.java)

                if (venta == null) {
                    onError("No se encontró la venta")
                } else {
                    onSuccess(venta)
                }
            }
            .addOnFailureListener { error ->
                onError(error.message ?: "Error al consultar la venta")
            }
    }

    /**
     * Si el artesano todavía no tiene ninguna valoración, el documento no existe — se
     * resuelve con `ArtesanoStats()` (todo en 0/null), no como un error.
     */
    fun obtenerStatsArtesano(
        artesanoId: String,
        onSuccess: (ArtesanoStats) -> Unit,
        onError: (String) -> Unit
    ) {

        db.collection("artesanoStats")
            .document(artesanoId)
            .get()
            .addOnSuccessListener { documento ->

                val stats = documento.toObject(ArtesanoStats::class.java) ?: ArtesanoStats()
                onSuccess(stats)
            }
            .addOnFailureListener { error ->
                onError(error.message ?: "Error al consultar la confianza del artesano")
            }
    }

    /**
     * Una sola transacción: crea `valoraciones/{ventaId}`, marca la venta como valorada,
     * y recalcula calidad del producto + confianza del artesano — todo o nada. Todas las
     * lecturas (producto, stats del artesano) van antes que cualquier escritura, como
     * exige Firestore dentro de una transacción.
     */
    fun enviarValoracion(
        venta: Venta,
        calidad: String,
        confianza: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {

        val usuarioActual = auth.currentUser

        if (usuarioActual == null) {
            onError("No hay un usuario autenticado")
            return
        }

        val ventaRef = db.collection("ventas").document(venta.id)
        val valoracionRef = db.collection("valoraciones").document(venta.id)
        val productoRef = db.collection("productos").document(venta.productoId)
        val statsRef = db.collection("artesanoStats").document(venta.artesanoId)

        val puntosCalidad = EscalaValoracion.puntos(calidad, EscalaValoracion.NIVELES_CALIDAD)
        val puntosConfianza = EscalaValoracion.puntos(confianza, EscalaValoracion.NIVELES_CONFIANZA)

        db.runTransaction { transaction ->

            // Lecturas primero — Firestore exige que toda lectura de una transacción
            // ocurra antes de cualquier escritura.
            val productoSnap = transaction.get(productoRef)
            val statsSnap = transaction.get(statsRef)

            val calidadSumaActual = productoSnap.getLong("calidadSumaPuntos") ?: 0L
            val calidadConteoActual = productoSnap.getLong("calidadConteo") ?: 0L
            val nuevaSumaCalidad = calidadSumaActual + puntosCalidad
            val nuevoConteoCalidad = calidadConteoActual + 1
            val nuevoPromedioCalidad = nuevaSumaCalidad.toDouble() / nuevoConteoCalidad

            val confianzaSumaActual = statsSnap.getLong("confianzaSumaPuntos") ?: 0L
            val confianzaConteoActual = statsSnap.getLong("confianzaConteo") ?: 0L
            val nuevaSumaConfianza = confianzaSumaActual + puntosConfianza
            val nuevoConteoConfianza = confianzaConteoActual + 1
            val nuevoPromedioConfianza = nuevaSumaConfianza.toDouble() / nuevoConteoConfianza

            val valoracion = Valoracion(
                id = venta.id,
                productoId = venta.productoId,
                artesanoId = venta.artesanoId,
                clienteId = venta.clienteId,
                calidad = calidad,
                confianza = confianza,
                creadoEn = Timestamp.now()
            )

            transaction.set(valoracionRef, valoracion)
            transaction.update(ventaRef, "valorado", true)

            transaction.update(
                productoRef,
                mapOf(
                    "calidadSumaPuntos" to nuevaSumaCalidad,
                    "calidadConteo" to nuevoConteoCalidad,
                    "calidadPromedio" to nuevoPromedioCalidad
                )
            )

            transaction.set(
                statsRef,
                mapOf(
                    "confianzaSumaPuntos" to nuevaSumaConfianza,
                    "confianzaConteo" to nuevoConteoConfianza,
                    "confianzaPromedio" to nuevoPromedioConfianza
                ),
                SetOptions.merge()
            )

            Unit
        }
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { error ->
                onError(error.message ?: "Error al enviar la valoración")
            }
    }
}
