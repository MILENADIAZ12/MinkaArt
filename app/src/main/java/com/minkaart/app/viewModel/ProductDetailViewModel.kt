package com.minkaart2.app.viewModel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.minkaart2.app.data.model.ArtesanoStats
import com.minkaart2.app.data.model.Product
import com.minkaart2.app.data.model.User
import com.minkaart2.app.repository.AuthRepository
import com.minkaart2.app.repository.ProductRepository
import com.minkaart2.app.repository.SaleRepository

class ProductDetailViewModel : ViewModel() {

    private val productRepository = ProductRepository()
    private val authRepository = AuthRepository()
    private val saleRepository = SaleRepository()

    private val _producto = mutableStateOf<Product?>(null)
    val producto: State<Product?> = _producto

    private val _artesano = mutableStateOf<User?>(null)
    val artesano: State<User?> = _artesano

    // Confianza del artesano dueño del producto (Fase 6) — corrige el error de fases
    // anteriores donde esto vivía por error en Product.confianzaPromedio.
    private val _statsArtesano = mutableStateOf(ArtesanoStats())
    val statsArtesano: State<ArtesanoStats> = _statsArtesano

    private val _yaLeDioLike = mutableStateOf(false)
    val yaLeDioLike: State<Boolean> = _yaLeDioLike

    private val _cargando = mutableStateOf(true)
    val cargando: State<Boolean> = _cargando

    // Guarda para no contar la misma apertura dos veces (recomposición o rotación) —
    // no es un State a propósito, no necesita disparar recomposición. La instancia de
    // este ViewModel sobrevive a la rotación, así que basta con compararlo aquí.
    private var vistaContadaPara: String? = null

    fun cargarDetalle(
        productoId: String,
        onError: (String) -> Unit
    ) {

        _cargando.value = true

        productRepository.obtenerProductoPorId(
            productoId = productoId,

            onSuccess = { producto ->

                _producto.value = producto
                _cargando.value = false

                // Cuenta la vista una sola vez por apertura real de la pantalla, y
                // nunca si quien la abre es el propio artesano dueño del producto
                // (en cualquier modo, artesano o "viendo como cliente").
                if (vistaContadaPara != productoId) {

                    vistaContadaPara = productoId

                    val uidActual = FirebaseAuth.getInstance().currentUser?.uid

                    if (uidActual != null && uidActual != producto.artesanoId) {

                        productRepository.incrementarVista(
                            productoId = productoId,
                            onError = { /* contador en segundo plano, no se avisa si falla */ }
                        )
                    }
                }

                // Firestore no tiene joins: una segunda lectura encadenada para
                // traer los datos del artesano dueño del producto.
                if (producto.artesanoId.isNotBlank()) {

                    authRepository.obtenerUsuarioPorId(
                        uid = producto.artesanoId,
                        onSuccess = { usuario -> _artesano.value = usuario },
                        onError = { /* la pantalla sigue mostrando el producto sin datos del artesano */ }
                    )

                    saleRepository.obtenerStatsArtesano(
                        artesanoId = producto.artesanoId,
                        onSuccess = { stats -> _statsArtesano.value = stats },
                        onError = { /* los puntos de confianza simplemente quedan en gris */ }
                    )
                }

                productRepository.existeLikeDelUsuarioActual(
                    productoId = productoId,
                    onSuccess = { existe -> _yaLeDioLike.value = existe },
                    onError = { /* se asume que no le ha dado like todavía */ }
                )
            },

            onError = { mensaje ->

                _cargando.value = false
                onError(mensaje)
            }
        )
    }

    /**
     * Nombre y correo de quien está viendo el producto: van en el mensaje de WhatsApp para
     * que el artesano pueda registrar la venta con ese correo. Si falla la lectura del
     * documento, se usa el correo de Firebase Auth y el nombre queda vacío.
     */
    fun obtenerDatosDelContacto(
        onDatos: (nombre: String, correo: String) -> Unit
    ) {

        val correoAuth = FirebaseAuth.getInstance().currentUser?.email ?: ""

        authRepository.obtenerUsuarioActual(
            onSuccess = { usuario -> onDatos(usuario.nombre, usuario.correo.ifBlank { correoAuth }) },
            onError = { onDatos("", correoAuth) }
        )
    }

    fun alternarLike(
        productoId: String,
        onError: (String) -> Unit
    ) {

        productRepository.alternarLike(
            productoId = productoId,
            onSuccess = { nuevoLikeado -> _yaLeDioLike.value = nuevoLikeado },
            onError = onError
        )
    }
}
