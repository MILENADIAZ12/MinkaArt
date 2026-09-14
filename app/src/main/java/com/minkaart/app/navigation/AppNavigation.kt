package com.minkaart2.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.minkaart2.app.ui.components.PlaceholderScreen
import com.minkaart2.app.ui.home.HomeArtesanoScreen
import com.minkaart2.app.ui.home.HomeClienteScreen
import com.minkaart2.app.ui.login.LoginScreen
import com.minkaart2.app.ui.product.ProductDetailScreen
import com.minkaart2.app.ui.profile.ProfileScreen
import com.minkaart2.app.ui.publication.EditProductScreen
import com.minkaart2.app.ui.publication.PublicationScreen
import com.minkaart2.app.ui.register.RegisterScreen
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
            RegisterScreen()
        }

        composable(Routes.HOME_CLIENTE) {
            HomeClienteScreen(navController)
        }

        composable(Routes.HOME_ARTESANO) {

            HomeArtesanoScreen(
                onPublicarClick = {
                    navController.navigate(Routes.PUBLICATION)
                },
                onMisPublicacionesClick = {
                    navController.navigate(Routes.MY_PRODUCTS)
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

        composable(Routes.RECOGNITION) {
            PlaceholderScreen(titulo = "Reconocimiento visual por cámara")
        }

        composable(Routes.LIKES) {
            PlaceholderScreen(titulo = "Tus artesanías con like")
        }

        composable(
            route = Routes.RUAC_BADGE,
            arguments = listOf(
                navArgument("artesanoId") { type = NavType.StringType }
            )
        ) {
            PlaceholderScreen(titulo = "Insignia RUAC del artesano")
        }

        composable(Routes.PROFILE) {
            ProfileScreen()
        }
    }
}
