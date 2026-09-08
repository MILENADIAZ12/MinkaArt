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
    val imagenes: List<String> = emptyList()

)