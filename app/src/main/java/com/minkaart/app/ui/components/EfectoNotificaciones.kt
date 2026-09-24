package com.minkaart2.app.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import com.minkaart2.app.repository.AuthRepository

/**
 * Pide el permiso de notificaciones (obligatorio desde Android 13/API 33, no existe en
 * versiones anteriores) y guarda el token de FCM del usuario actual — compartido entre
 * `HomeClienteScreen` y `HomeArtesanoScreen` para no duplicar la lógica del lanzador de
 * permiso y la obtención del token en cada pantalla.
 */
@Composable
fun EfectoNotificaciones() {

    val context = LocalContext.current
    val authRepository = remember { AuthRepository() }

    val lanzadorPermiso = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* concedido o no: si lo niega, la app sigue funcionando sin notificaciones */ }

    LaunchedEffect(Unit) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            lanzadorPermiso.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@LaunchedEffect

        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->

                authRepository.guardarTokenNotificaciones(
                    uid = uid,
                    token = token,
                    onSuccess = {},
                    onError = {} // si falla, se reintenta la próxima vez que se abra esta pantalla
                )
            }
    }
}
