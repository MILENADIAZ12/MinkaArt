package com.minkaart2.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.minkaart2.app.MainActivity
import com.minkaart2.app.R
import com.minkaart2.app.repository.AuthRepository

const val PREFERENCIAS_NOTIFICACIONES = "preferencias_notificaciones"
const val PREF_NOTIF_ACTIVIDAD = "notif_actividad"
const val PREF_NOTIF_LIKES = "notif_likes"

private const val CANAL_AVISOS = "minka_avisos"
private const val EXTRA_TIPO = "tipo"
private const val EXTRA_REFERENCIA_ID = "referenciaId"

/**
 * Recibe los mensajes de FCM — configurados como `data`-only desde las Cloud Functions de
 * notificaciones (no `notification`-only) para que `onMessageReceived` se dispare
 * siempre (app abierta, en segundo plano o cerrada) y la notificación se construya acá
 * de forma consistente, en vez de depender del comportamiento por defecto de FCM.
 *
 * Generalizado a cualquier tipo de notificación (venta, valoración, like, RUAC), no solo
 * ventas: el canal antes se llamaba "ventas_pendientes"; efecto secundario aceptado, a un
 * usuario que ya tenía la app instalada le queda ese canal viejo huérfano en los ajustes
 * del sistema hasta que reinstale.
 */
class MinkaMessagingService : FirebaseMessagingService() {

    private val authRepository = AuthRepository()

    override fun onNewToken(token: String) {

        super.onNewToken(token)

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        authRepository.guardarTokenNotificaciones(
            uid = uid,
            token = token,
            onSuccess = {},
            onError = {} // si falla, se reintenta en el próximo arranque de la app
        )
    }

    override fun onMessageReceived(message: RemoteMessage) {

        super.onMessageReceived(message)

        val tipo = message.data["tipo"] ?: return
        val referenciaId = message.data["referenciaId"] ?: return
        val titulo = message.data["titulo"] ?: "MinkaArt"
        val cuerpo = message.data["cuerpo"] ?: ""

        // Preferencias de Configuración → Notificaciones (activadas por defecto). Solo
        // controlan el aviso push del dispositivo; la lista de la campana no se toca.
        val preferencias = getSharedPreferences(PREFERENCIAS_NOTIFICACIONES, MODE_PRIVATE)
        val clavePreferencia = if (tipo == "like") PREF_NOTIF_LIKES else PREF_NOTIF_ACTIVIDAD

        if (!preferencias.getBoolean(clavePreferencia, true)) return

        crearCanalDeNotificaciones()

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TIPO, tipo)
            putExtra(EXTRA_REFERENCIA_ID, referenciaId)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            referenciaId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificacion = NotificationCompat.Builder(this, CANAL_AVISOS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(titulo)
            .setContentText(cuerpo)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager?.notify(referenciaId.hashCode(), notificacion)
    }

    private fun crearCanalDeNotificaciones() {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val canal = NotificationChannel(
            CANAL_AVISOS,
            "Avisos de MinkaArt",
            NotificationManager.IMPORTANCE_HIGH
        )

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager?.createNotificationChannel(canal)
    }
}
