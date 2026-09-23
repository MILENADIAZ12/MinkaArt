package com.minkaart2.app.data.model

data class Product(

    val id: String = "",

    // UID del artesano que publicó el producto
    val artesanoId: String = "",

    val nombre: String = "",

    val descripcion: String = "",

    val precio: Long = 0L,

    val historiaProceso: String = "",

    // Uno de CatalogosProducto.MATERIAS_PRIMAS; alimenta los chips de filtro de materia
    // prima del catálogo de cliente, igual que categoria. "" en productos publicados
    // antes de agregar este campo. Productos publicados cuando este campo todavía era
    // texto libre pueden tener un valor que no está en la lista curada — EditProductScreen
    // lo detecta y lo mueve a "Otra" + materiaPrimaOtro para no perder el dato (mismo
    // criterio que la migración de regionOrigen).
    val materiaPrima: String = "",

    // Solo tiene valor cuando materiaPrima == CatalogosProducto.MATERIA_PRIMA_OTRA
    // ("Otra"). Mismo patrón que categoriaOtro.
    val materiaPrimaOtro: String? = null,

    // Heredado: reemplazado por departamento/municipio (ajuste posterior a la Fase 5).
    // Ningún código nuevo vuelve a escribir este campo — se conserva solo para que un
    // producto publicado antes de este ajuste no pierda su región al leerlo. La UI lo usa
    // como respaldo de solo lectura mientras departamento esté vacío.
    val regionOrigen: String = "",

    val departamento: String = "",
    val municipio: String = "",

    // null = el artesano no especificó ninguna (el selector del formulario es opcional).
    val clasificacion: String? = null,

    // Solo tiene valor cuando categoria == CatalogosProducto.CATEGORIA_OTROS ("Otros").
    val categoriaOtro: String? = null,

    // Aquí se guardarán posteriormente las URL de Firebase Storage
    val imagenes: List<String> = emptyList(),

    // Vistas y likes: campos reales, pero todavía no hay ningún flujo (detalle de
    // producto, botón de like) que los incremente — quedan en 0 hasta esa fase futura.
    val vistas: Long = 0L,
    val likes: Long = 0L,

    // El artesano puede pausar/activar una publicación desde el Panel.
    val activo: Boolean = true,

    // Epoch millis (reloj del cliente, no serverTimestamp). 0L en productos publicados
    // antes de agregar este campo — la UI omite la fecha en ese caso.
    val fechaPublicacion: Long = 0L,

    // Texto libre (ej. "Tejidos", "Cerámica"), usado por los chips de filtro del
    // catálogo de cliente. Puede quedar vacío.
    val categoria: String = "",

    // Calidad del producto (Fase 6): se alimenta de cada valoración de compra
    // (RateSaleScreen), en puntos 1-4. calidadSumaPuntos/calidadConteo permiten
    // recalcular el promedio de forma incremental sin tener que releer todas las
    // valoraciones. Mientras calidadConteo sea 0, calidadPromedio es null y la UI lo
    // muestra como "sin valoraciones aún" en vez de inventar un valor.
    val calidadSumaPuntos: Int = 0,
    val calidadConteo: Int = 0,
    val calidadPromedio: Double? = null

    // "confianza" NO vive aquí: es del artesano, no del producto (ver
    // artesanoStats/{artesanoId} y ArtesanoStats.kt) — corregido en la Fase 6, antes
    // vivía por error como Product.confianzaPromedio (nunca llegó a calcularse).

)