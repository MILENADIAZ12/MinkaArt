package com.minkaart2.app.data.model

data class User(

    val uid: String = "",
    val nombre: String = "",
    val correo: String = "",
    val rol: String = "",
    val telefono: String = "",

    // Datos adicionales del artesano
    val fechaNacimiento: String = "",
    val region: String = "",
    val productosQueVende: String = "",

    // Un artesano puede trabajar varias categorías
    val categoriasArtesanales: List<String> = emptyList(),

    // Registro RUAC
    val tieneRuac: Boolean = false,
    val numeroRuac: String = "",
    val ruacUrl: String = ""

)