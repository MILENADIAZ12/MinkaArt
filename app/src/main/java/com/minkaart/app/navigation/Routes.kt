package com.minkaart2.app.navigation

object Routes {

    const val WELCOME = "welcome"

    const val LOGIN = "login"

    const val REGISTER = "register"

    const val HOME_CLIENTE = "home_cliente"

    const val HOME_ARTESANO = "home_artesano"

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

    const val RECOGNITION = "recognition"

    // =========================
    // FASE 5 — Insignia RUAC (administrador)
    // =========================

    const val ADMIN_REGISTER = "admin_register"

    const val ADMIN_RUAC_LIST = "admin_ruac_list"

    const val ADMIN_RUAC_REVIEW = "admin_ruac_review/{artesanoId}"

    fun adminRuacReviewRoute(artesanoId: String) = "admin_ruac_review/$artesanoId"

    const val REQUEST_RUAC = "request_ruac"
}