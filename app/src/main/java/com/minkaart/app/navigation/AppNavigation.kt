package com.minkaart2.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.auth.FirebaseAuth
import com.minkaart2.app.ui.admin.AdminRegisterScreen
import com.minkaart2.app.ui.admin.AdminRuacListScreen
import com.minkaart2.app.ui.admin.AdminRuacReviewScreen
import com.minkaart2.app.ui.components.PlaceholderScreen
import com.minkaart2.app.ui.home.HomeArtesanoScreen
import com.minkaart2.app.ui.home.HomeClienteScreen
import com.minkaart2.app.ui.login.LoginScreen
import com.minkaart2.app.ui.product.ProductDetailScreen
import com.minkaart2.app.ui.profile.ArtisanProfileScreen
import com.minkaart2.app.ui.profile.ClientProfileScreen
import com.minkaart2.app.ui.profile.LikesScreen
import com.minkaart2.app.ui.profile.ProfileScreen
import com.minkaart2.app.ui.publication.EditProductScreen
import com.minkaart2.app.ui.publication.PublicationScreen
import com.minkaart2.app.ui.register.RegisterScreen
import com.minkaart2.app.ui.ruac.RequestRuacScreen
import com.minkaart2.app.ui.ruac.RuacBadgeScreen
import com.minkaart2.app.ui.welcome.WelcomeScreen
import com.minkaart2.app.ui.publication.MyProductsScreen

@Composable
fun AppNavigation() {

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.WELCOME
    ) {

        composable(Routes.WELCOME) {
            WelcomeScreen(navController)
        }

        composable(Routes.LOGIN) {
            LoginScreen(navController)
        }

        composable(Routes.REGISTER) {
            RegisterScreen(navController)
        }

        composable(Routes.HOME_CLIENTE) {
            HomeClienteScreen(navController)
        }

        composable(Routes.HOME_ARTESANO) {

            HomeArtesanoScreen(
                navController = navController,
                onPublicarClick = {
                    navController.navigate(Routes.PUBLICATION)
                },
                onMisPublicacionesClick = {
                    navController.navigate(Routes.MY_PRODUCTS)
                },
                onPerfilClick = {

                    val uid = FirebaseAuth.getInstance().currentUser?.uid

                    if (uid != null) {
                        navController.navigate(Routes.artisanProfileRoute(uid))
                    }
                }
            )
        }
        composable(Routes.MY_PRODUCTS) {

            MyProductsScreen(
                onEditarProducto = { productId ->
                    navController.navigate(Routes.editarProductoRoute(productId))
                },
                onNuevoProducto = {
                    navController.navigate(Routes.PUBLICATION)
                }
            )
        }

        composable(Routes.PUBLICATION) {
            PublicationScreen(navController)
        }

        composable(
            route = Routes.EDIT_PRODUCT,
            arguments = listOf(
                navArgument("productId") { type = NavType.StringType }
            )
        ) { backStackEntry ->

            val productId = backStackEntry.arguments?.getString("productId")

            if (productId != null) {

                EditProductScreen(
                    productId = productId,
                    navController = navController
                )
            }
        }

        composable(
            route = Routes.PRODUCT_DETAIL,
            arguments = listOf(
                navArgument("productId") { type = NavType.StringType }
            )
        ) { backStackEntry ->

            val productId = backStackEntry.arguments?.getString("productId")

            if (productId != null) {

                ProductDetailScreen(
                    productId = productId,
                    navController = navController
                )
            }
        }

        composable(
            route = Routes.ARTISAN_PROFILE,
            arguments = listOf(
                navArgument("artesanoId") { type = NavType.StringType }
            )
        ) { backStackEntry ->

            val artesanoId = backStackEntry.arguments?.getString("artesanoId")

            if (artesanoId != null) {

                ArtisanProfileScreen(
                    artesanoId = artesanoId,
                    navController = navController
                )
            }
        }

        composable(Routes.CLIENT_PROFILE) {
            ClientProfileScreen(navController)
        }

        composable(Routes.LIKES) {
            LikesScreen(navController)
        }

        composable(Routes.PURCHASE_HISTORY) {
            PlaceholderScreen(titulo = "Historial de compras")
        }

        composable(Routes.ACCOUNT_SETTINGS) {
            PlaceholderScreen(titulo = "Configuración de la cuenta")
        }

        composable(Routes.RECOGNITION) {
            PlaceholderScreen(titulo = "Reconocimiento visual por cámara")
        }

        composable(
            route = Routes.RUAC_BADGE,
            arguments = listOf(
                navArgument("artesanoId") { type = NavType.StringType }
            )
        ) { backStackEntry ->

            val artesanoId = backStackEntry.arguments?.getString("artesanoId")

            if (artesanoId != null) {

                RuacBadgeScreen(
                    artesanoId = artesanoId,
                    navController = navController
                )
            }
        }

        composable(Routes.PROFILE) {
            ProfileScreen()
        }

        // =========================
        // FASE 5 — Insignia RUAC (administrador)
        // =========================

        composable(Routes.ADMIN_REGISTER) {
            AdminRegisterScreen(navController)
        }

        composable(Routes.ADMIN_RUAC_LIST) {
            AdminRuacListScreen(navController)
        }

        composable(
            route = Routes.ADMIN_RUAC_REVIEW,
            arguments = listOf(
                navArgument("artesanoId") { type = NavType.StringType }
            )
        ) { backStackEntry ->

            val artesanoId = backStackEntry.arguments?.getString("artesanoId")

            if (artesanoId != null) {

                AdminRuacReviewScreen(
                    artesanoId = artesanoId,
                    navController = navController
                )
            }
        }

        composable(Routes.REQUEST_RUAC) {
            RequestRuacScreen(navController)
        }
    }
}
