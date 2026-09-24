package com.minkaart2.app.data

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

    // Al seleccionar esta categoría, el formulario pide un texto libre obligatorio
    // ("Especifica cuál") y lo guarda en Product.categoriaOtro.
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
}
