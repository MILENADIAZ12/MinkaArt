package com.minkaart2.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.auth.FirebaseAuth
import com.minkaart2.app.ui.admin.AdminRegisterScreen
import com.minkaart2.app.ui.admin.AdminRuacListScreen
import com.minkaart2.app.ui.admin.AdminRuacReviewScreen
import com.minkaart2.app.ui.admin.AdminVisualEvalScreen
import com.minkaart2.app.ui.home.HomeArtesanoScreen
import com.minkaart2.app.ui.home.HomeClienteScreen
import com.minkaart2.app.ui.login.LoginScreen
import com.minkaart2.app.ui.notifications.NotificationsScreen
import com.minkaart2.app.ui.product.ProductDetailScreen
import com.minkaart2.app.ui.profile.ArtisanProfileScreen
import com.minkaart2.app.ui.profile.ClientProfileScreen
import com.minkaart2.app.ui.profile.EditProfileScreen
import com.minkaart2.app.ui.profile.LikesScreen
import com.minkaart2.app.ui.profile.ProfileScreen
import com.minkaart2.app.ui.publication.EditProductScreen
import com.minkaart2.app.ui.publication.PublicationScreen
import com.minkaart2.app.ui.recognition.RecognitionScreen
import com.minkaart2.app.ui.register.RegisterScreen
import com.minkaart2.app.ui.ruac.RequestRuacScreen
import com.minkaart2.app.ui.ruac.RuacBadgeScreen
import com.minkaart2.app.ui.roleselection.RoleSelectionScreen
import com.minkaart2.app.ui.sales.PurchaseHistoryScreen
import com.minkaart2.app.ui.sales.QualityTrustScreen
import com.minkaart2.app.ui.sales.RateSaleScreen
import com.minkaart2.app.ui.sales.RegisterSaleScreen
import com.minkaart2.app.ui.settings.AccountSettingsScreen
import com.minkaart2.app.ui.terms.TermsScreen
import com.minkaart2.app.ui.welcome.WelcomeScreen
import com.minkaart2.app.ui.publication.MyProductsScreen
import com.minkaart2.app.viewModel.SesionViewModel

@Composable
fun AppNavigation(tipoInicial: String? = null, referenciaInicial: String? = null) {

    val navController = rememberNavController()

    // Deep link desde el push de una notificación (MinkaMessagingService): navega una
    // sola vez por cada par (tipo, referencia) nuevo que llegue, resuelto con la misma
    // tabla que usa NotificationsScreen al tocar una fila (Routes.rutaDeNotificacion), en
    // vez de repetir aquí el `when` de tipos. Si la sesión de Firebase Auth ya había
    // expirado al tocar la notificación, la pantalla destino simplemente fallará su
    // propia validación (fuera de alcance encadenar un login-y-continuar para este caso).
    LaunchedEffect(tipoInicial, referenciaInicial) {

        if (tipoInicial != null && referenciaInicial != null) {

            val ruta = Routes.rutaDeNotificacion(tipoInicial, referenciaInicial)

            if (ruta != null) {
                navController.navigate(ruta)
            }
        }
    }

    // Sesión persistente: con sesión guardada la app arranca directo en el Home del rol.
    val sesionViewModel: SesionViewModel = viewModel()
    val destinoInicial = remember { sesionViewModel.destinoInicial() }

    // Sesión de antes del almacén local (hay usuario en Auth pero no rol guardado): se
    // arranca en Bienvenida y, tras consultar el rol una vez, se pasa a su Home — solo si
    // la persona sigue en Bienvenida (no si una notificación ya la llevó a otra pantalla).
    LaunchedEffect(Unit) {

        if (sesionViewModel.requiereRecuperarRol()) {

            sesionViewModel.recuperarRol { home ->

                if (navController.currentDestination?.route == Routes.WELCOME) {

                    navController.navigate(home) {
                        popUpTo(Routes.WELCOME) { inclusive = true }
                    }
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = destinoInicial
    ) {

        composable(Routes.WELCOME) {
            WelcomeScreen(navController)
        }

        composable(Routes.LOGIN) {
            LoginScreen(navController)
        }

        composable(
            route = Routes.ROLE_SELECTION,
            arguments = listOf(
                navArgument("google") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { backStackEntry ->

            RoleSelectionScreen(
                navController = navController,
                modoGoogle = backStackEntry.arguments?.getBoolean("google") ?: false
            )
        }

        composable(
            route = Routes.REGISTER,
            arguments = listOf(
                navArgument("rol") { type = NavType.StringType },
                navArgument("google") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { backStackEntry ->

            val rol = backStackEntry.arguments?.getString("rol")

            if (rol == "cliente" || rol == "artesano") {

                RegisterScreen(
                    navController = navController,
                    rolElegido = rol,
                    modoGoogleInicial = backStackEntry.arguments?.getBoolean("google") ?: false
                )
            }
        }

        composable(Routes.TERMS) {
            TermsScreen(navController)
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
                },
                onRegistrarVenta = { productId ->
                    navController.navigate(Routes.registerSaleRoute(productId))
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
            PurchaseHistoryScreen(navController)
        }

        composable(Routes.ACCOUNT_SETTINGS) {
            AccountSettingsScreen(navController)
        }

        composable(Routes.EDIT_PROFILE) {
            EditProfileScreen(navController)
        }

        composable(Routes.RECOGNITION) {
            RecognitionScreen(navController = navController)
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

        composable(Routes.NOTIFICATIONS) {
            NotificationsScreen(navController)
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

        composable(Routes.ADMIN_VISUAL_EVAL) {
            AdminVisualEvalScreen(navController)
        }

        // =========================
        // FASE 6 — Registrar venta, Valorar compra, Calidad y confianza
        // =========================

        composable(
            route = Routes.REGISTER_SALE,
            arguments = listOf(
                navArgument("productId") { type = NavType.StringType }
            )
        ) { backStackEntry ->

            val productId = backStackEntry.arguments?.getString("productId")

            if (productId != null) {

                RegisterSaleScreen(
                    productId = productId,
                    navController = navController
                )
            }
        }

        composable(
            route = Routes.RATE_SALE,
            arguments = listOf(
                navArgument("ventaId") { type = NavType.StringType }
            )
        ) { backStackEntry ->

            val ventaId = backStackEntry.arguments?.getString("ventaId")

            if (ventaId != null) {

                RateSaleScreen(
                    ventaId = ventaId,
                    navController = navController
                )
            }
        }

        composable(
            route = Routes.QUALITY_TRUST,
            arguments = listOf(
                navArgument("productId") { type = NavType.StringType }
            )
        ) { backStackEntry ->

            val productId = backStackEntry.arguments?.getString("productId")

            if (productId != null) {

                QualityTrustScreen(
                    productId = productId,
                    navController = navController
                )
            }
        }
    }
}
