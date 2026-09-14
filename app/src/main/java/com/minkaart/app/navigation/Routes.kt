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

    const val RECOGNITION = "recognition"
}