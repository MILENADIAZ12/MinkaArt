package com.minkaart2.app.viewModel

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.minkaart2.app.data.CatalogosProducto
import com.minkaart2.app.data.model.Product
import com.minkaart2.app.repository.ProductRepository
import com.minkaart2.app.repository.VisualSearchRepository
import com.minkaart2.app.vision.GeneradorHuellas

class ProductViewModel : ViewModel() {

    companion object {
        const val MAX_IMAGENES = 15
    }

    private val productRepository = ProductRepository()
    private val visualSearchRepository = VisualSearchRepository()
    private val generadorHuellas = GeneradorHuellas(visualSearchRepository)

    /**
     * Calcula y guarda la huella visual de un producto recién publicado o editado, en
     * segundo plano — si falla, el producto se publica/actualiza igual (ver
     * MotorHuellaVisual/GeneradorHuellas), solo se registra el error.
     */
    private fun generarHuellaEnSegundoPlano(producto: Product) {

        generadorHuellas.procesarPendientes(
            productos = listOf(producto),
            versionesExistentes = emptyMap(),
            onProgreso = { _, _ -> },
            onCompletado = { _, fallidas, _ ->

                if (fallidas > 0) {
                    Log.w("ProductViewModel", "No se pudo generar la huella visual de ${producto.id}")
                }
            }
        )
    }

    // Lista de publicaciones del artesano actual
    private val _misProductos = mutableStateOf<List<Product>>(emptyList())
    val misProductos: State<List<Product>> = _misProductos

    // Indica si estamos consultando Firestore
    private val _cargando = mutableStateOf(false)
    val cargando: State<Boolean> = _cargando

    private fun validarCamposProducto(
        nombre: String,
        descripcion: String,
        precio: Long,
        historiaProceso: String,
        materiaPrima: String,
        materiaPrimaOtro: String,
        departamento: String,
        municipio: String,
        categoria: String,
        categoriaOtro: String,
        onError: (String) -> Unit
    ): Boolean {

        if (nombre.isBlank()) {
            onError("Ingresa el nombre del producto")
            return false
        }

        if (descripcion.isBlank()) {
            onError("Ingresa la descripción del producto")
            return false
        }

        if (precio <= 0) {
            onError("Ingresa un precio válido")
            return false
        }

        if (historiaProceso.isBlank()) {
            onError("Ingresa la historia o proceso de elaboración")
            return false
        }

        if (materiaPrima.isBlank()) {
            onError("Selecciona la materia prima")
            return false
        }

        if (materiaPrima == CatalogosProducto.MATERIA_PRIMA_OTRA && materiaPrimaOtro.isBlank()) {
            onError("Especifica la materia prima en \"Especifica cuál\"")
            return false
        }

        if (departamento.isBlank()) {
            onError("Selecciona el departamento de origen")
            return false
        }

        if (municipio.isBlank()) {
            onError("Ingresa el municipio de origen")
            return false
        }

        if (categoria.isBlank()) {
            onError("Selecciona una categoría")
            return false
        }

        if (categoria == CatalogosProducto.CATEGORIA_OTROS && categoriaOtro.isBlank()) {
            onError("Especifica la categoría en \"Especifica cuál\"")
            return false
        }

        return true
    }

    fun publicarProducto(
        nombre: String,
        descripcion: String,
        precio: Long,
        historiaProceso: String,
        materiaPrima: String,
        materiaPrimaOtro: String,
        departamento: String,
        municipio: String,
        categoria: String,
        categoriaOtro: String,
        clasificacion: String?,
        color: String,
        tamano: String,
        imagenesUris: List<Uri>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {

        val camposValidos = validarCamposProducto(
            nombre = nombre,
            descripcion = descripcion,
            precio = precio,
            historiaProceso = historiaProceso,
            materiaPrima = materiaPrima,
            materiaPrimaOtro = materiaPrimaOtro,
            departamento = departamento,
            municipio = municipio,
            categoria = categoria,
            categoriaOtro = categoriaOtro,
            onError = onError
        )

        if (!camposValidos) {
            return
        }

        if (imagenesUris.isEmpty()) {
            onError("Debes seleccionar al menos una imagen del producto")
            return
        }

        if (imagenesUris.size > MAX_IMAGENES) {
            onError("Puedes subir un máximo de $MAX_IMAGENES imágenes")
            return
        }

        // categoriaOtro/materiaPrimaOtro solo tienen sentido junto a su opción "Otro(a)"
        // — se descarta cualquier texto que haya quedado de un cambio de selección
        // anterior.
        val categoriaOtroFinal = if (categoria == CatalogosProducto.CATEGORIA_OTROS) {
            categoriaOtro.trim()
        } else {
            null
        }

        val materiaPrimaOtroFinal = if (materiaPrima == CatalogosProducto.MATERIA_PRIMA_OTRA) {
            materiaPrimaOtro.trim()
        } else {
            null
        }

        val producto = Product(
            nombre = nombre,
            descripcion = descripcion,
            precio = precio,
            historiaProceso = historiaProceso,
            materiaPrima = materiaPrima,
            materiaPrimaOtro = materiaPrimaOtroFinal,
            departamento = departamento,
            municipio = municipio,
            categoria = categoria.trim(),
            categoriaOtro = categoriaOtroFinal,
            clasificacion = clasificacion,
            color = color,
            tamano = tamano
        )

        productRepository.guardarProductoConImagenes(
            producto = producto,
            imagenesUris = imagenesUris,
            onSuccess = { productoGuardado ->
                generarHuellaEnSegundoPlano(productoGuardado)
                onSuccess()
            },
            onError = onError
        )
    }

    fun cargarMisProductos(
        onError: (String) -> Unit,
        onSuccess: (List<Product>) -> Unit = {}
    ) {

        _cargando.value = true

        productRepository.obtenerMisProductos(
            onSuccess = { productos ->

                _misProductos.value = productos
                _cargando.value = false
                onSuccess(productos)
            },

            onError = { mensaje ->

                _cargando.value = false
                onError(mensaje)
            }
        )
    }

    fun obtenerProductoPorId(
        productoId: String,
        onSuccess: (Product) -> Unit,
        onError: (String) -> Unit
    ) {

        productRepository.obtenerProductoPorId(
            productoId = productoId,
            onSuccess = onSuccess,
            onError = onError
        )
    }

    fun actualizarProducto(
        productoOriginal: Product,
        nombre: String,
        descripcion: String,
        precio: Long,
        historiaProceso: String,
        materiaPrima: String,
        materiaPrimaOtro: String,
        departamento: String,
        municipio: String,
        categoria: String,
        categoriaOtro: String,
        clasificacion: String?,
        color: String,
        tamano: String,
        imagenesExistentes: List<String>,
        imagenesNuevasUris: List<Uri>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {

        val camposValidos = validarCamposProducto(
            nombre = nombre,
            descripcion = descripcion,
            precio = precio,
            historiaProceso = historiaProceso,
            materiaPrima = materiaPrima,
            materiaPrimaOtro = materiaPrimaOtro,
            departamento = departamento,
            municipio = municipio,
            categoria = categoria,
            categoriaOtro = categoriaOtro,
            onError = onError
        )

        if (!camposValidos) {
            return
        }

        val totalImagenes = imagenesExistentes.size + imagenesNuevasUris.size

        if (totalImagenes == 0) {
            onError("Debes conservar o agregar al menos una imagen del producto")
            return
        }

        if (totalImagenes > MAX_IMAGENES) {
            onError("Puedes tener un máximo de $MAX_IMAGENES imágenes")
            return
        }

        val categoriaOtroFinal = if (categoria == CatalogosProducto.CATEGORIA_OTROS) {
            categoriaOtro.trim()
        } else {
            null
        }

        val materiaPrimaOtroFinal = if (materiaPrima == CatalogosProducto.MATERIA_PRIMA_OTRA) {
            materiaPrimaOtro.trim()
        } else {
            null
        }

        // .copy() sobre el producto original: conserva vistas, likes, activo,
        // fechaPublicacion, calidadPromedio/confianzaPromedio, imagenes y regionOrigen
        // (heredado, ver Product.kt) tal cual, y solo reemplaza los campos que el
        // formulario de edición sí controla.
        val producto = productoOriginal.copy(
            nombre = nombre,
            descripcion = descripcion,
            precio = precio,
            historiaProceso = historiaProceso,
            materiaPrima = materiaPrima,
            materiaPrimaOtro = materiaPrimaOtroFinal,
            departamento = departamento,
            municipio = municipio,
            categoria = categoria.trim(),
            categoriaOtro = categoriaOtroFinal,
            clasificacion = clasificacion,
            color = color,
            tamano = tamano
        )

        productRepository.actualizarProducto(
            productoId = productoOriginal.id,
            producto = producto,
            imagenesExistentes = imagenesExistentes,
            imagenesNuevasUris = imagenesNuevasUris,
            onSuccess = { productoActualizado ->

                // Solo recalcula la huella si las imágenes de verdad cambiaron — editar
                // solo el precio o la descripción no debe volver a procesar imágenes.
                if (productoActualizado.imagenes != productoOriginal.imagenes) {
                    generarHuellaEnSegundoPlano(productoActualizado)
                }

                onSuccess()
            },
            onError = onError
        )
    }

    fun eliminarProducto(
        productoId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {

        productRepository.eliminarProducto(
            productoId = productoId,
            onSuccess = {

                _misProductos.value = _misProductos.value.filterNot { producto ->
                    producto.id == productoId
                }

                // Best-effort: si el producto nunca tuvo huella, Firestore niega el
                // delete sobre un documento que no existe (mismo tipo de error de
                // evaluación ya documentado para productoBaseIgual) — se ignora, es un
                // caso normal, no debe afectar el resultado del borrado del producto.
                visualSearchRepository.eliminarHuella(
                    productId = productoId,
                    onSuccess = {},
                    onError = {}
                )

                onSuccess()
            },
            onError = onError
        )
    }

    fun cambiarEstadoProducto(
        productoId: String,
        activo: Boolean,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {

        productRepository.cambiarEstadoProducto(
            productoId = productoId,
            activo = activo,
            onSuccess = {

                _misProductos.value = _misProductos.value.map { producto ->

                    if (producto.id == productoId) {
                        producto.copy(activo = activo)
                    } else {
                        producto
                    }
                }

                onSuccess()
            },
            onError = onError
        )
    }
}
