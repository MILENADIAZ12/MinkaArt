package com.minkaart2.app.data

import androidx.annotation.StringRes
import com.minkaart2.app.R

/**
 * Listas fijas para el formulario de Publicar/Editar producto — un solo lugar para que
 * `PublicationScreen` y `EditProductScreen` usen exactamente las mismas opciones, en vez
 * de repetirlas (mismo criterio que `util/Texto.kt`).
 */
object CatalogosProducto {

    val DEPARTAMENTOS = listOf(
        "Amazonas", "Antioquia", "Arauca", "Atlántico", "Bolívar", "Boyacá", "Caldas",
        "Caquetá", "Casanare", "Cauca", "Cesar", "Chocó", "Córdoba", "Cundinamarca",
        "Guainía", "Guaviare", "Huila", "La Guajira", "Magdalena", "Meta", "Nariño",
        "Norte de Santander", "Putumayo", "Quindío", "Risaralda",
        "San Andrés y Providencia", "Santander", "Sucre", "Tolima", "Valle del Cauca",
        "Vaupés", "Vichada", "Bogotá D.C."
    )

    // 14 oficios + "Moda" y "Otros" como categorías propias (ver plan: se pidió
    // explícitamente que fueran dos categorías separadas, no una fusionada).
    val CATEGORIAS = listOf(
        "Textilería y tejeduría",
        "Cestería y fibras vegetales (paja, mimbre, palma)",
        "Alfarería y cerámica",
        "Marroquinería y talabartería (trabajo en cuero)",
        "Joyería y orfebrería (metales y piedras preciosas)",
        "Metalistería y forja (hierro, bronce, cobre)",
        "Talla y ebanistería en madera",
        "Cantería y escultura en piedra",
        "Vidriería y vidrio soplado",
        "Papel y cartonería (papel maché)",
        "Cerería (velas artesanales)",
        "Juguetería artesanal",
        "Trabajo en hueso, cuerno, concha y coral",
        "Pintura popular y lacas",
        "Moda",
        "Otros"
    )


    const val CATEGORIA_OTROS = "Otros"

    // Antes era texto libre: cada producto guardaba su propia variante (p. ej. "Fibra
    // Chocolatillo" vs. "Chocolatillo") y los chips de filtro de materia prima de
    // HomeClienteScreen se volvían un catálogo interminable. Mismo patrón que
    // CATEGORIAS/CATEGORIA_OTROS: lista curada + "Otra" con texto libre aparte
    // (Product.materiaPrimaOtro).
    val MATERIAS_PRIMAS = listOf(
        "Algodón",
        "Lana",
        "Fibra de alpaca",
        "Seda",
        "Acrílico",
        "Lino",
        "Fique",
        "Cabuya",
        "Iraca",
        "Cumare",
        "Chocolatillo",
        "Palma",
        "Caña flecha",
        "Junco",
        "Mimbre",
        "Chambira",
        "Werregue",
        "Paja tetera",
        "Enea",
        "Cuero",
        "Cuero ecológico",
        "Hueso",
        "Cacho o asta",
        "Concha",
        "Coral",
        "Plumas",
        "Plata",
        "Plata 950",
        "Oro",
        "Cobre",
        "Bronce",
        "Hierro",
        "Latón",
        "Chaquiras",
        "Mostacilla",
        "Semillas",
        "Madera",
        "Guadua",
        "Balso",
        "Piedra",
        "Totumo o calabazo",
        "Tagua",
        "Barro o arcilla",
        "Vidrio",
        "Cerámica",
        "Papel maché",
        "Cera",
        "Caucho",
        "Textil reciclado",
        "Otra"
    )

    // Al seleccionar esta opción, el formulario pide un texto libre obligatorio
    // ("Especifica cuál") y lo guarda en Product.materiaPrimaOtro.
    const val MATERIA_PRIMA_OTRA = "Otra"

    val CLASIFICACIONES = listOf(
        "Artesanía indígena",
        "Artesanía tradicional popular",
        "Artesanía contemporánea o neoartesanía"
    )

    // Opción líder del dropdown de Clasificación — representa Product.clasificacion == null.
    const val CLASIFICACION_SIN_ESPECIFICAR = "Sin especificar"

    // Filtros del catálogo del cliente (Product.color / Product.tamano), ambos opcionales.
    val COLORES = listOf(
        "Natural/crudo", "Blanco", "Negro", "Café", "Rojo", "Naranja", "Amarillo",
        "Verde", "Azul", "Morado", "Rosado", "Gris", "Multicolor"
    )

    val TAMANOS = listOf(
        "Pequeño (menos de 15 cm)",
        "Mediano (15–40 cm)",
        "Grande (más de 40 cm)"
    )

    // Opción líder de los dropdowns opcionales de Color y Tamaño — representa "".
    const val SIN_ESPECIFICAR = "Sin especificar"

    // =========================
    // ETIQUETAS TRADUCIBLES
    // Lo que se guarda en Firestore SIEMPRE es el valor en español de las listas de arriba;
    // estos mapas solo dicen qué texto mostrar según el idioma de la app. Un valor que no
    // está en el mapa (p. ej. texto libre de un artesano antiguo) se muestra tal cual.
    // Los departamentos son nombres propios y no se traducen.
    // =========================

    private val ETIQUETAS_CATEGORIAS: Map<String, Int> = mapOf(
        CATEGORIAS[0] to R.string.cat_textileria,
        CATEGORIAS[1] to R.string.cat_cesteria,
        CATEGORIAS[2] to R.string.cat_alfareria,
        CATEGORIAS[3] to R.string.cat_marroquineria,
        CATEGORIAS[4] to R.string.cat_joyeria,
        CATEGORIAS[5] to R.string.cat_metaleria,
        CATEGORIAS[6] to R.string.cat_talla,
        CATEGORIAS[7] to R.string.cat_canteria,
        CATEGORIAS[8] to R.string.cat_vidrieria,
        CATEGORIAS[9] to R.string.cat_papel,
        CATEGORIAS[10] to R.string.cat_cereria,
        CATEGORIAS[11] to R.string.cat_jugueteria,
        CATEGORIAS[12] to R.string.cat_hueso,
        CATEGORIAS[13] to R.string.cat_pintura,
        CATEGORIAS[14] to R.string.cat_moda,
        CATEGORIAS[15] to R.string.cat_otros
    )

    private val ETIQUETAS_CLASIFICACIONES: Map<String, Int> = mapOf(
        CLASIFICACIONES[0] to R.string.clas_indigena,
        CLASIFICACIONES[1] to R.string.clas_tradicional,
        CLASIFICACIONES[2] to R.string.clas_contemporanea,
        CLASIFICACION_SIN_ESPECIFICAR to R.string.sin_especificar
    )

    private val ETIQUETAS_COLORES: Map<String, Int> = mapOf(
        COLORES[0] to R.string.color_natural,
        COLORES[1] to R.string.color_blanco,
        COLORES[2] to R.string.color_negro,
        COLORES[3] to R.string.color_cafe,
        COLORES[4] to R.string.color_rojo,
        COLORES[5] to R.string.color_naranja,
        COLORES[6] to R.string.color_amarillo,
        COLORES[7] to R.string.color_verde,
        COLORES[8] to R.string.color_azul,
        COLORES[9] to R.string.color_morado,
        COLORES[10] to R.string.color_rosado,
        COLORES[11] to R.string.color_gris,
        COLORES[12] to R.string.color_multicolor,
        SIN_ESPECIFICAR to R.string.sin_especificar
    )

    private val ETIQUETAS_TAMANOS: Map<String, Int> = mapOf(
        TAMANOS[0] to R.string.tam_pequeno,
        TAMANOS[1] to R.string.tam_mediano,
        TAMANOS[2] to R.string.tam_grande,
        SIN_ESPECIFICAR to R.string.sin_especificar
    )

    private val ETIQUETAS_MATERIAS_PRIMAS: Map<String, Int> = mapOf(
        MATERIAS_PRIMAS[0] to R.string.mat_algodon,
        MATERIAS_PRIMAS[1] to R.string.mat_lana,
        MATERIAS_PRIMAS[2] to R.string.mat_alpaca,
        MATERIAS_PRIMAS[3] to R.string.mat_seda,
        MATERIAS_PRIMAS[4] to R.string.mat_acrilico,
        MATERIAS_PRIMAS[5] to R.string.mat_lino,
        MATERIAS_PRIMAS[6] to R.string.mat_fique,
        MATERIAS_PRIMAS[7] to R.string.mat_cabuya,
        MATERIAS_PRIMAS[8] to R.string.mat_iraca,
        MATERIAS_PRIMAS[9] to R.string.mat_cumare,
        MATERIAS_PRIMAS[10] to R.string.mat_chocolatillo,
        MATERIAS_PRIMAS[11] to R.string.mat_palma,
        MATERIAS_PRIMAS[12] to R.string.mat_cana_flecha,
        MATERIAS_PRIMAS[13] to R.string.mat_junco,
        MATERIAS_PRIMAS[14] to R.string.mat_mimbre,
        MATERIAS_PRIMAS[15] to R.string.mat_chambira,
        MATERIAS_PRIMAS[16] to R.string.mat_werregue,
        MATERIAS_PRIMAS[17] to R.string.mat_paja_tetera,
        MATERIAS_PRIMAS[18] to R.string.mat_enea,
        MATERIAS_PRIMAS[19] to R.string.mat_cuero,
        MATERIAS_PRIMAS[20] to R.string.mat_cuero_ecologico,
        MATERIAS_PRIMAS[21] to R.string.mat_hueso,
        MATERIAS_PRIMAS[22] to R.string.mat_cacho,
        MATERIAS_PRIMAS[23] to R.string.mat_concha,
        MATERIAS_PRIMAS[24] to R.string.mat_coral,
        MATERIAS_PRIMAS[25] to R.string.mat_plumas,
        MATERIAS_PRIMAS[26] to R.string.mat_plata,
        MATERIAS_PRIMAS[27] to R.string.mat_plata_950,
        MATERIAS_PRIMAS[28] to R.string.mat_oro,
        MATERIAS_PRIMAS[29] to R.string.mat_cobre,
        MATERIAS_PRIMAS[30] to R.string.mat_bronce,
        MATERIAS_PRIMAS[31] to R.string.mat_hierro,
        MATERIAS_PRIMAS[32] to R.string.mat_laton,
        MATERIAS_PRIMAS[33] to R.string.mat_chaquiras,
        MATERIAS_PRIMAS[34] to R.string.mat_mostacilla,
        MATERIAS_PRIMAS[35] to R.string.mat_semillas,
        MATERIAS_PRIMAS[36] to R.string.mat_madera,
        MATERIAS_PRIMAS[37] to R.string.mat_guadua,
        MATERIAS_PRIMAS[38] to R.string.mat_balso,
        MATERIAS_PRIMAS[39] to R.string.mat_piedra,
        MATERIAS_PRIMAS[40] to R.string.mat_totumo,
        MATERIAS_PRIMAS[41] to R.string.mat_tagua,
        MATERIAS_PRIMAS[42] to R.string.mat_barro,
        MATERIAS_PRIMAS[43] to R.string.mat_vidrio,
        MATERIAS_PRIMAS[44] to R.string.mat_ceramica,
        MATERIAS_PRIMAS[45] to R.string.mat_papel_mache,
        MATERIAS_PRIMAS[46] to R.string.mat_cera,
        MATERIAS_PRIMAS[47] to R.string.mat_caucho,
        MATERIAS_PRIMAS[48] to R.string.mat_textil_reciclado,
        MATERIAS_PRIMAS[49] to R.string.mat_otra
    )

    @StringRes
    fun etiquetaCategoria(valor: String): Int? = ETIQUETAS_CATEGORIAS[valor]

    @StringRes
    fun etiquetaClasificacion(valor: String): Int? = ETIQUETAS_CLASIFICACIONES[valor]

    @StringRes
    fun etiquetaColor(valor: String): Int? = ETIQUETAS_COLORES[valor]

    @StringRes
    fun etiquetaTamano(valor: String): Int? = ETIQUETAS_TAMANOS[valor]

    @StringRes
    fun etiquetaMateriaPrima(valor: String): Int? = ETIQUETAS_MATERIAS_PRIMAS[valor]
}
