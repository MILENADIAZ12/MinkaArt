package com.minkaart2.app.data.model

data class Product(

    val id: String = "",

    // UID del artesano que publicó el producto
    val artesanoId: String = "",

    val nombre: String = "",

    val descripcion: String = "",

    val precio: Long = 0L,

    val historiaProceso: String = "",

    val regionOrigen: String = "",

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

    // Se calculan en una fase futura ("Valorar compra"). Mientras sean null, la UI los
    // muestra como "sin valoraciones aún" en vez de inventar un valor.
    val calidadPromedio: Double? = null,
    val confianzaPromedio: Double? = null

)