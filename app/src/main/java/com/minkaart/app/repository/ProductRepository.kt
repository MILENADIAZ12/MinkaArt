package com.minkaart2.app.repository

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
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
            artesanoId = artesanoId
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
                            imagenes = listOf(url.toString())
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
}