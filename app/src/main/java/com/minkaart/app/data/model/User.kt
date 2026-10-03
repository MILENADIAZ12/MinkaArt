package com.minkaart2.app.data.model

import com.google.firebase.Timestamp

data class User(

    val uid: String = "",
    val nombre: String = "",
    val correo: String = "",
    val rol: String = "", // "cliente", "artesano" o "administrador"
    val telefono: String = "",

    // Datos adicionales del artesano
    val fechaNacimiento: String = "",
    val region: String = "",
    val productosQueVende: String = "",

    // Un artesano puede trabajar varias categorías
    val categoriasArtesanales: List<String> = emptyList(),

    // Registro RUAC (Fase 5): reemplaza los campos viejos tieneRuac/numeroRuac/ruacUrl —
    // ver README ("Firestore — colección usuarios") para la nota de migración: un
    // documento anterior a esta fase con tieneRuac == true no se migra automáticamente,
    // queda en ruacEstado == "no_solicitado" hasta que el artesano vuelva a solicitarlo.
    val ruacEstado: String = "no_solicitado", // no_solicitado | pendiente | verificado | rechazado
    val ruacRegistro: String? = null,
    val ruacRegion: String? = null,
    val ruacTecnica: String? = null,
    val ruacDocumentoUrl: String? = null,
    val ruacSolicitadoEn: Timestamp? = null,
    val ruacVerificadoEn: Timestamp? = null,
    val ruacUltimaValidacion: Timestamp? = null,
    val ruacRevisadoPor: String? = null,

    // Perfil público del artesano (Fase 4)
    val bio: String? = null,
    val contactoPublico: String? = null,

    // Foto de perfil (cliente o artesano) — Storage: usuarios/{uid}/foto_perfil.
    // null hasta que el usuario sube una; se muestra un ícono por defecto mientras tanto.
    val fotoUrl: String? = null,

    // Token de Firebase Cloud Messaging del dispositivo actual — se sobrescribe cada vez
    // que la app arranca con sesión iniciada o cuando el token rota (MinkaMessagingService
    // .onNewToken). Null hasta que el usuario abre la app al menos una vez tras esta fase.
    val fcmToken: String? = null

)
