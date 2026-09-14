package com.minkaart2.app.repository

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.minkaart2.app.data.model.Product

class ProductRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    fun guardarProducto(
        producto: Product,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {

        val usuarioActual = auth.currentUser

        if (usuarioActual == null) {
            onError("No hay un usuario autenticado")
            return
        }

        val artesanoId = usuarioActual.uid

        val documento = db.collection("productos").document()

        val productoFinal = producto.copy(
            id = documento.id,
            artesanoId = artesanoId,
            fechaPublicacion = System.currentTimeMillis()
        )

        documento
            .set(productoFinal)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { error ->
                onError(
                    error.message ?: "Error al guardar el producto"
                )
            }
    }

    fun guardarProductoConImagen(
        producto: Product,
        imagenUri: Uri,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {

        val usuarioActual = auth.currentUser

        if (usuarioActual == null) {
            onError("No hay un usuario autenticado")
            return
        }

        val artesanoId = usuarioActual.uid

        // Generamos primero el ID del producto
        val documento = db.collection("productos").document()

        // Ubicación de la fotografía en Firebase Storage
        val imagenReferencia = storage.reference
            .child("productos")
            .child(artesanoId)
            .child(documento.id)
            .child("imagen_principal")

        // Primero subimos la fotografía
        imagenReferencia
            .putFile(imagenUri)
            .addOnSuccessListener {

                // Obtenemos la URL pública de descarga
                imagenReferencia.downloadUrl
                    .addOnSuccessListener { url ->

                        val productoFinal = producto.copy(
                            id = documento.id,
                            artesanoId = artesanoId,
                            imagenes = listOf(url.toString()),
                            fechaPublicacion = System.currentTimeMillis()
                        )

                        // Solo después de subir la imagen
                        // guardamos el producto en Firestore
                        documento
                            .set(productoFinal)
                            .addOnSuccessListener {
                                onSuccess()
                            }
                            .addOnFailureListener { error ->
                                onError(
                                    error.message
                                        ?: "Error al guardar el producto"
                                )
                            }
                    }
                    .addOnFailureListener { error ->
                        onError(
                            error.message
                                ?: "Error al obtener la URL de la imagen"
                        )
                    }
            }
            .addOnFailureListener { error ->
                onError(
                    error.message
                        ?: "Error al subir la imagen"
                )
            }
    }

    fun obtenerMisProductos(
        onSuccess: (List<Product>) -> Unit,
        onError: (String) -> Unit
    ) {

        val usuarioActual = auth.currentUser

        if (usuarioActual == null) {
            onError("No hay un usuario autenticado")
            return
        }

        val artesanoId = usuarioActual.uid

        db.collection("productos")
            .whereEqualTo("artesanoId", artesanoId)
            .get()
            .addOnSuccessListener { resultado ->

                val productos = resultado.documents.mapNotNull { documento ->
                    documento.toObject(Product::class.java)
                }

                onSuccess(productos)
            }
            .addOnFailureListener { error ->

                onError(
                    error.message ?: "Error al consultar tus publicaciones"
                )
            }
    }

    fun obtenerProductoPorId(
        productoId: String,
        onSuccess: (Product) -> Unit,
        onError: (String) -> Unit
    ) {

        db.collection("productos")
            .document(productoId)
            .get()
            .addOnSuccessListener { documento ->

                val producto = documento.toObject(Product::class.java)

                if (producto == null) {
                    onError("No se encontró el producto")
                } else {
                    onSuccess(producto)
                }
            }
            .addOnFailureListener { error ->

                onError(
                    error.message ?: "Error al consultar el producto"
                )
            }
    }

    fun actualizarProducto(
        productoId: String,
        producto: Product,
        nuevaImagenUri: Uri?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {

        val usuarioActual = auth.currentUser

        if (usuarioActual == null) {
            onError("No hay un usuario autenticado")
            return
        }

        val artesanoId = usuarioActual.uid
        val documento = db.collection("productos").document(productoId)

        if (nuevaImagenUri == null) {

            // Sin imagen nueva: actualizamos los campos del producto
            // y conservamos las imágenes que ya tenía.
            documento
                .set(producto.copy(id = productoId, artesanoId = artesanoId))
                .addOnSuccessListener {
                    onSuccess()
                }
                .addOnFailureListener { error ->
                    onError(
                        error.message ?: "Error al actualizar el producto"
                    )
                }

        } else {

            // Con imagen nueva: se sube al mismo path que usa
            // guardarProductoConImagen, sobrescribiendo la anterior.
            val imagenReferencia = storage.reference
                .child("productos")
                .child(artesanoId)
                .child(productoId)
                .child("imagen_principal")

            imagenReferencia
                .putFile(nuevaImagenUri)
                .addOnSuccessListener {

                    imagenReferencia.downloadUrl
                        .addOnSuccessListener { url ->

                            val productoFinal = producto.copy(
                                id = productoId,
                                artesanoId = artesanoId,
                                imagenes = listOf(url.toString())
                            )

                            documento
                                .set(productoFinal)
                                .addOnSuccessListener {
                                    onSuccess()
                                }
                                .addOnFailureListener { error ->
                                    onError(
                                        error.message
                                            ?: "Error al actualizar el producto"
                                    )
                                }
                        }
                        .addOnFailureListener { error ->
                            onError(
                                error.message
                                    ?: "Error al obtener la URL de la imagen"
                            )
                        }
                }
                .addOnFailureListener { error ->
                    onError(
                        error.message ?: "Error al subir la imagen"
                    )
                }
        }
    }

    fun eliminarProducto(
        productoId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {

        db.collection("productos")
            .document(productoId)
            .delete()
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { error ->

                onError(
                    error.message ?: "Error al eliminar el producto"
                )
            }
    }

    fun cambiarEstadoProducto(
        productoId: String,
        activo: Boolean,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {

        db.collection("productos")
            .document(productoId)
            .update("activo", activo)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { error ->

                onError(
                    error.message ?: "Error al cambiar el estado del producto"
                )
            }
    }

    fun obtenerTodosLosProductosActivos(
        onSuccess: (List<Product>) -> Unit,
        onError: (String) -> Unit
    ) {

        // No se filtra "activo == true" en el propio query: los productos publicados
        // antes de agregar ese campo no lo tienen guardado en Firestore en absoluto, así
        // que un whereEqualTo los excluiría a todos (el valor por defecto de Kotlin solo
        // aplica al deserializar un documento que YA llegó, no antes). Se trae todo y se
        // filtra en memoria, donde el default `activo = true` sí aplica correctamente.
        db.collection("productos")
            .get()
            .addOnSuccessListener { resultado ->

                val productos = resultado.documents
                    .mapNotNull { documento -> documento.toObject(Product::class.java) }
                    .filter { producto -> producto.activo }

                onSuccess(productos)
            }
            .addOnFailureListener { error ->

                onError(
                    error.message ?: "Error al consultar el catálogo"
                )
            }
    }

    /**
     * Da o quita el like del usuario actual sobre un producto, en una transacción:
     * crea o borra productos/{productoId}/likes/{uid} y ajusta el contador `likes` del
     * producto en el mismo movimiento, para que nunca queden desincronizados.
     */
    fun alternarLike(
        productoId: String,
        onSuccess: (Boolean) -> Unit,
        onError: (String) -> Unit
    ) {

        val usuarioActual = auth.currentUser

        if (usuarioActual == null) {
            onError("No hay un usuario autenticado")
            return
        }

        val uid = usuarioActual.uid
        val productoRef = db.collection("productos").document(productoId)
        val likeRef = productoRef.collection("likes").document(uid)

        db.runTransaction { transaction ->

            val likeExistente = transaction.get(likeRef)
            val yaLeDioLike = likeExistente.exists()

            if (yaLeDioLike) {

                transaction.delete(likeRef)
                transaction.update(productoRef, "likes", FieldValue.increment(-1))

            } else {

                val datosLike = hashMapOf(
                    "userId" to uid,
                    "creadoEn" to FieldValue.serverTimestamp()
                )

                transaction.set(likeRef, datosLike)
                transaction.update(productoRef, "likes", FieldValue.increment(1))
            }

            // El nuevo estado: true si acabamos de dar like, false si lo acabamos de quitar.
            !yaLeDioLike
        }
            .addOnSuccessListener { nuevoLikeado ->
                onSuccess(nuevoLikeado)
            }
            .addOnFailureListener { error ->

                onError(
                    error.message ?: "Error al actualizar el like"
                )
            }
    }

    fun existeLikeDelUsuarioActual(
        productoId: String,
        onSuccess: (Boolean) -> Unit,
        onError: (String) -> Unit
    ) {

        val usuarioActual = auth.currentUser

        if (usuarioActual == null) {
            onError("No hay un usuario autenticado")
            return
        }

        db.collection("productos")
            .document(productoId)
            .collection("likes")
            .document(usuarioActual.uid)
            .get()
            .addOnSuccessListener { documento ->
                onSuccess(documento.exists())
            }
            .addOnFailureListener { error ->

                onError(
                    error.message ?: "Error al consultar el like"
                )
            }
    }

    /**
     * Un solo `collectionGroup` en vez de una consulta por cada producto visible: cada
     * documento de like guarda `userId` como campo (no solo como id), así se puede saber
     * de una vez qué productos ya le gustan al usuario actual.
     */
    fun obtenerProductosLikeadosPorUsuario(
        onSuccess: (Set<String>) -> Unit,
        onError: (String) -> Unit
    ) {

        val usuarioActual = auth.currentUser

        if (usuarioActual == null) {
            onError("No hay un usuario autenticado")
            return
        }

        db.collectionGroup("likes")
            .whereEqualTo("userId", usuarioActual.uid)
            .get()
            .addOnSuccessListener { resultado ->

                val idsProductos = resultado.documents.mapNotNull { documento ->
                    // El documento vive en productos/{productoId}/likes/{uid}:
                    // el "abuelo" (parent.parent) es el documento del producto.
                    documento.reference.parent.parent?.id
                }.toSet()

                onSuccess(idsProductos)
            }
            .addOnFailureListener { error ->

                onError(
                    error.message ?: "Error al consultar tus likes"
                )
            }
    }
}