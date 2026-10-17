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

    // Heredado, solo lectura: reemplazado por `departamentos`. Ningún código nuevo lo
    // vuelve a escribir; se conserva para que un artesano registrado antes de este ajuste
    // no pierda su región al leerlo (mismo criterio que Product.regionOrigen).
    val region: String = "",

    // Departamentos donde trabaja el artesano (selección múltiple de
    // CatalogosProducto.DEPARTAMENTOS).
    val departamentos: List<String> = emptyList(),

    val productosQueVende: String = "",

    // Un artesano puede trabajar varias categorías. Los artesanos registrados antes de la
    // selección múltiple tienen aquí el texto libre que escribieron, y se leen igual.
    val categoriasArtesanales: List<String> = emptyList(),

    // Solo tiene valor cuando categoriasArtesanales incluye CatalogosProducto.CATEGORIA_OTROS
    // ("Otros"): lo que el artesano escribió en "Especifica cuál".
    val categoriasArtesanalesOtro: String? = null,

    // Aceptación de los Términos y Condiciones / Política de Tratamiento de Datos
    // (Ley 1581 de 2012 y Ley 1480 de 2011). Los usuarios registrados antes de esto quedan
    // con aceptoTerminos == false y versionTerminos == "".
    val aceptoTerminos: Boolean = false,
    val terminosAceptadosEn: Timestamp? = null,
    val versionTerminos: String = "",

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

    // Redes sociales del artesano: usuario (con o sin @) o enlace completo. null = no tiene.
    val instagram: String? = null,
    val tiktok: String? = null,
    val youtube: String? = null,

    // Foto de perfil (cliente o artesano) — Storage: usuarios/{uid}/foto_perfil.
    // null hasta que el usuario sube una; se muestra un ícono por defecto mientras tanto.
    val fotoUrl: String? = null,

    // Token de Firebase Cloud Messaging del dispositivo actual — se sobrescribe cada vez
    // que la app arranca con sesión iniciada o cuando el token rota (MinkaMessagingService
    // .onNewToken). Null hasta que el usuario abre la app al menos una vez tras esta fase.
    val fcmToken: String? = null

)
