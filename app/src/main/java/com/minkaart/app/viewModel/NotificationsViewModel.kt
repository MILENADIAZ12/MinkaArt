package com.minkaart2.app.viewModel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.minkaart2.app.data.model.Notificacion
import com.minkaart2.app.repository.NotificationRepository

/**
 * Estado de la campana de notificaciones — compartido por `HomeClienteScreen`,
 * `HomeArtesanoScreen` (badge de no leídas) y `NotificationsScreen` (lista completa).
 */
class NotificationsViewModel : ViewModel() {

    private val repository = NotificationRepository()

    private val _notificaciones = mutableStateOf<List<Notificacion>>(emptyList())
    val notificaciones: State<List<Notificacion>> = _notificaciones

    // Derivado en memoria de la lista ya cargada, sin ningún query propio.
    private val _noLeidas = mutableStateOf(0)
    val noLeidas: State<Int> = _noLeidas

    private val _cargando = mutableStateOf(true)
    val cargando: State<Boolean> = _cargando

    fun cargar(onError: (String) -> Unit) {

        val uid = FirebaseAuth.getInstance().currentUser?.uid

        if (uid == null) {
            onError("No hay un usuario autenticado")
            return
        }

        _cargando.value = true

        repository.obtenerNotificaciones(
            uid = uid,
            onSuccess = { notificaciones ->

                _notificaciones.value = notificaciones
                _noLeidas.value = notificaciones.count { !it.leida }
                _cargando.value = false
            },
            onError = { mensaje ->

                _cargando.value = false
                onError(mensaje)
            }
        )
    }

    fun marcarLeida(notificacionId: String, onError: (String) -> Unit) {

        repository.marcarComoLeida(
            notificacionId = notificacionId,
            onSuccess = {

                _notificaciones.value = _notificaciones.value.map {
                    if (it.id == notificacionId) it.copy(leida = true) else it
                }
                _noLeidas.value = _notificaciones.value.count { !it.leida }
            },
            onError = onError
        )
    }

    fun marcarTodasLeidas(onError: (String) -> Unit) {

        val idsNoLeidas = _notificaciones.value.filter { !it.leida }.map { it.id }

        repository.marcarTodasComoLeidas(
            idsNoLeidas = idsNoLeidas,
            onSuccess = {

                _notificaciones.value = _notificaciones.value.map { it.copy(leida = true) }
                _noLeidas.value = 0
            },
            onError = onError
        )
    }
}
