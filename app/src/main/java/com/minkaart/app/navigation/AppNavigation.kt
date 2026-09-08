package com.minkaart2.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.minkaart2.app.ui.home.HomeArtesanoScreen
import com.minkaart2.app.ui.home.HomeClienteScreen
import com.minkaart2.app.ui.login.LoginScreen
import com.minkaart2.app.ui.profile.ProfileScreen
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
            HomeClienteScreen()
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
            MyProductsScreen()
        }

        composable(Routes.PUBLICATION) {
            PublicationScreen()
        }

        composable(Routes.PROFILE) {
            ProfileScreen()
        }
    }
}