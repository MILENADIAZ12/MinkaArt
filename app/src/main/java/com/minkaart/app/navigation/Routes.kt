package com.minkaart2.app.navigation

object Routes {

    const val WELCOME = "welcome"

    const val LOGIN = "login"

    // Argumento `rol` ("cliente" | "artesano"); `google` es true cuando la persona ya se
    // autenticó con Google y solo falta completar su registro (ver RegisterScreen).
    const val REGISTER = "register/{rol}?google={google}"

    fun registerRoute(rol: String, google: Boolean = false) = "register/$rol?google=$google"

    // Elegir el rol ANTES de registrarse. `google` es true cuando llega alguien nuevo
    // autenticado con Google desde Login: aún no tiene documento en `usuarios`.
    const val ROLE_SELECTION = "role_selection?google={google}"

    fun roleSelectionRoute(google: Boolean = false) = "role_selection?google=$google"

    // Términos y Condiciones / Política de Tratamiento de Datos Personales. Se abre desde
    // el enlace de la casilla de aceptación de los registros.
    const val TERMS = "terms"

    const val HOME_CLIENTE = "home_cliente"

    const val HOME_ARTESANO = "home_artesano"

    /**
     * Home que le corresponde a un [rol] guardado en `usuarios/{uid}.rol` — lo usan
     * `LoginScreen` (tras iniciar sesión) y `SplashViewModel` (sesión persistente).
     * `null` si el rol no se reconoce.
     */
    fun homeDeRol(rol: String): String? = when (rol.trim().lowercase()) {
        "cliente" -> HOME_CLIENTE
        "artesano" -> HOME_ARTESANO
        "administrador" -> ADMIN_RUAC_LIST
        else -> null
    }

    const val PROFILE = "profile"

    const val PUBLICATION = "publication"

    const val MY_PRODUCTS = "my_products"

    const val EDIT_PRODUCT = "edit_product/{productId}"

    fun editarProductoRoute(productId: String) = "edit_product/$productId"

    const val PRODUCT_DETAIL = "product_detail/{productId}"

    fun productDetailRoute(productId: String) = "product_detail/$productId"

    const val LIKES = "likes"

    const val RUAC_BADGE = "ruac_badge/{artesanoId}"

    fun ruacBadgeRoute(artesanoId: String) = "ruac_badge/$artesanoId"

    const val ARTISAN_PROFILE = "artisan_profile/{artesanoId}"

    fun artisanProfileRoute(artesanoId: String) = "artisan_profile/$artesanoId"

    const val CLIENT_PROFILE = "client_profile"

    const val PURCHASE_HISTORY = "purchase_history"

    const val ACCOUNT_SETTINGS = "account_settings"

    // Editar información básica (nombre, teléfono, ciudad/región, foto) — cliente y artesano.
    const val EDIT_PROFILE = "edit_profile"

    const val RECOGNITION = "recognition"

    // =========================
    // FASE 5 — Insignia RUAC (administrador)
    // =========================

    const val ADMIN_REGISTER = "admin_register"

    const val ADMIN_RUAC_LIST = "admin_ruac_list"

    const val ADMIN_RUAC_REVIEW = "admin_ruac_review/{artesanoId}"

    fun adminRuacReviewRoute(artesanoId: String) = "admin_ruac_review/$artesanoId"

    const val REQUEST_RUAC = "request_ruac"

    // Evaluación de la búsqueda visual (solo administrador) — ver README, "Evaluación".
    const val ADMIN_VISUAL_EVAL = "admin_visual_eval"

    // =========================
    // FASE 6 — Registrar venta, Valorar compra, Calidad y confianza
    // =========================

    const val REGISTER_SALE = "register_sale/{productId}"

    fun registerSaleRoute(productId: String) = "register_sale/$productId"

    const val RATE_SALE = "rate_sale/{ventaId}"

    fun rateSaleRoute(ventaId: String) = "rate_sale/$ventaId"

    const val QUALITY_TRUST = "quality_trust/{productId}"

    fun qualityTrustRoute(productId: String) = "quality_trust/$productId"

    // =========================
    // NOTIFICACIONES (campana)
    // =========================

    const val NOTIFICATIONS = "notifications"

    /**
     * Traduce un [tipo] + [referenciaId] de `Notificacion` a una ruta navegable — usado
     * tanto por `NotificationsScreen` (al tocar una fila) como por `AppNavigation` (deep
     * link desde el push de FCM), para no repetir esta tabla en dos sitios. `null` si el
     * tipo no se reconoce (defensivo: nunca debería pasar, pero evita un crash si un
     * cliente viejo recibe un tipo agregado después).
     */
    fun rutaDeNotificacion(tipo: String, referenciaId: String): String? = when (tipo) {
        "venta" -> rateSaleRoute(referenciaId)
        "valoracion" -> qualityTrustRoute(referenciaId)
        "like", "publicacion" -> productDetailRoute(referenciaId)
        "seguidor" -> artisanProfileRoute(referenciaId)
        "ruac_aprobado" -> ruacBadgeRoute(referenciaId)
        "ruac_rechazado" -> REQUEST_RUAC
        else -> null
    }
}