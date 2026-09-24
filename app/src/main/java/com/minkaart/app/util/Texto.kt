package com.minkaart2.app.util

import java.text.Normalizer

/**
 * Quita tildes/diacríticos y pasa a minúsculas, para que la búsqueda no dependa de que el
 * usuario escriba los acentos exactos (p. ej. "ceramica" debe encontrar "Cerámica").
 * `java.text.Normalizer` es parte del JDK — no hace falta ninguna librería nueva.
 *
 * Extraída de `HomeClienteScreen` (donde vivía como función privada) para que
 * `MyProductsScreen` reutilice exactamente la misma lógica de normalización en vez de
 * reimplementarla por su cuenta.
 */
fun normalizar(texto: String): String {

    val descompuesto = Normalizer.normalize(texto, Normalizer.Form.NFD)
    return descompuesto.replace("\\p{Mn}+".toRegex(), "").lowercase()
}
