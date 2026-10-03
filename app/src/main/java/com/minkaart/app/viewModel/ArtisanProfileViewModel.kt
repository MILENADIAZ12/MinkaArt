package com.minkaart2.app.viewModel

import android.net.Uri
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.minkaart2.app.data.model.ArtesanoStats
import com.minkaart2.app.data.model.Product
import com.minkaart2.app.data.model.User
import com.minkaart2.app.repository.AuthRepository
import com.minkaart2.app.repository.ProductRepository
import com.minkaart2.app.repository.SaleRepository

class ArtisanProfileViewModel : ViewModel() {

    private val authRepository = AuthRepository()
    private val productRepository = ProductRepository()
    private val saleRepository = SaleRepository()

    private val _artesano = mutableStateOf<User?>(null)
    val artesano: State<User?> = _artesano

    private val _productos = mutableStateOf<List<Product>>(emptyList())
    val productos: State<List<Product>> = _productos

    private val _productosLikeados = mutableStateOf<Set<String>>(emptySet())
    val productosLikeados: State<Set<String>> = _productosLikeados

    // Confianza real del artesano (Fase 6) — un solo documento, no un promedio de
    // productos: corrige el error de fases anteriores donde esto se calculaba mal
    // promediando Product.confianzaPromedio (campo que ni siquiera existe ya).
    private val _statsArtesano = mutableStateOf(ArtesanoStats())
    val statsArtesano: State<ArtesanoStats> = _statsArtesano

    private val _cargando = mutableStateOf(true)
    val cargando: State<Boolean> = _cargando

    fun cargarPerfil(
        artesanoId: String,
        onError: (String) -> Unit
    ) {

        _cargando.value = true

        authRepository.obtenerUsuarioPorId(
            uid = artesanoId,
            onSuccess = { usuario ->

                _artesano.value = usuario
                _cargando.value = false
            },
            onError = { mensaje ->

                _cargando.value = false
                onError(mensaje)
            }
        )

        productRepository.obtenerProductosPorArtesano(
            artesanoId = artesanoId,
            onSuccess = { productos -> _productos.value = productos },
            onError = { /* la pantalla simplemente muestra la grilla vacía */ }
        )

        productRepository.obtenerProductosLikeadosPorUsuario(
            onSuccess = { ids -> _productosLikeados.value = ids },
            onError = { /* sin corazones rellenos si esto falla, no bloquea la pantalla */ }
        )

        saleRepository.obtenerStatsArtesano(
            artesanoId = artesanoId,
            onSuccess = { stats -> _statsArtesano.value = stats },
            onError = { /* el indicador de confianza simplemente queda en gris */ }
        )
    }

    fun alternarLike(
        productoId: String,
        onError: (String) -> Unit
    ) {

        productRepository.alternarLike(
            productoId = productoId,

            onSuccess = { nuevoLikeado ->

                _productosLikeados.value = if (nuevoLikeado) {
                    _productosLikeados.value + productoId
                } else {
                    _productosLikeados.value - productoId
                }
            },

            onError = onError
        )
    }

    fun actualizarContacto(
        artesanoId: String,
        bio: String,
        contactoPublico: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {

        authRepository.actualizarCamposUsuario(
            uid = artesanoId,
            campos = mapOf(
                "bio" to bio,
                "contactoPublico" to contactoPublico
            ),
            onSuccess = {

                _artesano.value = _artesano.value?.copy(
                    bio = bio,
                    contactoPublico = contactoPublico
                )

                onSuccess()
            },
            onError = onError
        )
    }

    fun actualizarFotoPerfil(
        artesanoId: String,
        fotoUri: Uri,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {

        authRepository.subirFotoPerfil(
            uid = artesanoId,
            fotoUri = fotoUri,
            onSuccess = { url ->

                _artesano.value = _artesano.value?.copy(fotoUrl = url)
                onSuccess()
            },
            onError = onError
        )
    }

    /**
     * El propio artesano retira su RUAC verificado (ajuste posterior a la Fase 5) — a
     * diferencia de un rechazo del administrador, esto no pasa por ninguna revisión.
     * Actualiza `_artesano` localmente, mismo patrón que [actualizarContacto].
     */
    fun eliminarRuac(
        artesanoId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {

        authRepository.eliminarRuac(
            uid = artesanoId,
            onSuccess = {

                _artesano.value = _artesano.value?.copy(
                    ruacEstado = "no_solicitado",
                    ruacRegistro = null,
                    ruacRegion = null,
                    ruacTecnica = null,
                    ruacDocumentoUrl = null,
                    ruacVerificadoEn = null,
                    ruacUltimaValidacion = null
                )

                onSuccess()
            },
            onError = onError
        )
    }
}
