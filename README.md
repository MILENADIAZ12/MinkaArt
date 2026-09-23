# MinkaArt

App Android nativa (Kotlin + Jetpack Compose) para un marketplace de artesanías
colombianas: conecta artesanos con compradores, poniendo en primer plano la historia y
el proceso de elaboración de cada pieza, no solo el producto.

## Contexto del producto

MinkaArt tiene dos tipos de usuario, definidos por el campo `rol` (`"cliente"` o
`"artesano"`) en el documento de usuario de Firestore:

- **Artesano**: se registra con datos adicionales (teléfono, fecha de nacimiento, región,
  qué productos vende, categorías artesanales) y puede publicar artesanías con foto,
  precio, descripción, historia/proceso de elaboración y región de origen.
- **Cliente**: se registra con los datos básicos y consume el catálogo de todos los
  artesanos, con búsqueda, filtro por categoría, "me gusta" y detalle de producto con
  contacto por WhatsApp (ver [Flujos principales](#flujos-principales)).

El registro de artesano incluye un campo opcional de **RUAC** (Registro Único de
Artesanos y Artesanas de Colombia, la acreditación oficial de Artesanías de Colombia): si
el artesano indica que lo tiene, debe aportar el número y adjuntar el documento
(PDF o imagen). El modelo ya guarda esa información, aunque el sistema todavía no la
verifica de ninguna forma.

## Stack técnico

| Capa | Tecnología | Versión |
|---|---|---|
| Lenguaje | Kotlin | 2.0.21 |
| UI | Jetpack Compose (Material 3) | BOM 2024.09.00 |
| Build | Android Gradle Plugin | 8.13.2 |
| Build | Gradle (wrapper) | 8.13 |
| Navegación | Navigation Compose | 2.9.0 |
| Estado / MVVM | `lifecycle-viewmodel-compose` | 2.8.7 |
| Backend | Firebase (BOM) — Auth, Firestore, Storage | 33.10.0 |
| Backend | Firebase Cloud Functions (`firebase-functions-ktx`, BoM) | 33.10.0 |
| Notificaciones push | Firebase Cloud Messaging (`firebase-messaging`, BoM) | 33.10.0 |
| Envío de correo | nodemailer (SMTP de Gmail, en Cloud Functions) | 6.9.15 |
| Carga de imágenes | Coil (`coil-compose`) | 2.7.0 |
| `minSdk` / `targetSdk` / `compileSdk` | — | 31 / 36 / 36 |
| JVM target | — | 11 |

Proyecto Firebase: `minka-art` (bucket de Storage `minka-art.firebasestorage.app`),
configurado en `app/google-services.json`, que está versionado en el repositorio.

Además del proyecto Android, el repositorio incluye `functions/`: un backend de Cloud
Functions en Node.js + TypeScript (`onCall`, v2) que genera y valida el código de
verificación de correo y lo envía por el SMTP de Gmail con [nodemailer](https://nodemailer.com). Ver
[Modelo de datos](#modelo-de-datos) y [Flujos principales](#flujos-principales).

## Arquitectura

MVVM por capas, con Firebase llamado directamente desde el repositorio (no hay una capa
de red intermedia). **Todo el código asíncrono usa callbacks `onSuccess` / `onError`**,
no corrutinas ni `Flow` — es la convención del proyecto, ver `CLAUDE.md`.

```
MainActivity → MinkaArt_btaTheme → AppNavigation (NavHost)
    Screen (Compose)  →  ViewModel  →  Repository  →  Firebase (Auth / Firestore / Storage)
                                                    →  Cloud Functions → Firestore / Gmail SMTP
```

| Capa | Paquete | Responsabilidad |
|---|---|---|
| Entrada | `com.minkaart2.app` (`MainActivity.kt`) | Activity única, `enableEdgeToEdge()`, monta el tema y `AppNavigation`. |
| Navegación | `com.minkaart2.app.navigation` | `Routes` (constantes de ruta) y `AppNavigation` (`NavHost`). |
| UI | `com.minkaart2.app.ui.*` | Un paquete por pantalla (Composables), sin lógica de negocio. |
| Estado | `com.minkaart2.app.viewModel` / `viewmodel` | Validación de formularios y estado observable (`State<T>`). |
| Datos | `com.minkaart2.app.repository` | Llamadas directas a Firebase Auth/Firestore/Storage, y a las Cloud Functions de verificación de correo (`AuthRepository.enviarCodigoVerificacion` / `verificarCodigo`). |
| Modelo | `com.minkaart2.app.data.model` | `data class` que se serializan/deserializan con Firestore. |
| Tema | `com.minkaart2.app.ui.theme` | Paleta de marca, tipografía, `MaterialTheme`. |
| Backend | `functions/src/index.ts` | Cloud Functions `onCall` (Node.js + TypeScript): generan, guardan y validan el código de verificación, y lo envían por correo con `nodemailer` sobre el SMTP de Gmail. Además, cuatro triggers de Firestore (`notificarNuevaVenta`, `notificarNuevaValoracion`, `notificarLikeNuevo`, `notificarCambioRuac`) escriben en `notificaciones` y envían el push de FCM. |

## Estructura del proyecto

> ⚠️ El paquete Kotlin es `com.minkaart2.app`, pero las carpetas físicas en disco son
> `com/minkaart/app/` (sin el `2`). Es intencional en el sentido de que compila así, pero
> es fácil de pasar por alto: si buscas código por ruta de carpeta y no aparece, revisa
> también por nombre de paquete.

```
app/src/main/java/com/minkaart/app/
├── MainActivity.kt
├── data/
│   ├── CatalogosProducto.kt # departamentos, categorías, materias primas y clasificaciones
│   │                         # de Publicar/Editar producto (listas fijas, compartidas)
│   ├── EscalaValoracion.kt  # niveles de calidad/confianza <-> puntos <-> etiqueta (Fase 6)
│   └── model/
│       ├── ArtesanoStats.kt # documento de "artesanoStats" — confianza del artesano
│       ├── Notificacion.kt # documento de "notificaciones" — solo lo escriben Cloud Functions
│       ├── Product.kt      # documento de la colección "productos"
│       ├── User.kt          # documento de la colección "usuarios"
│       ├── Valoracion.kt    # documento de "valoraciones"
│       └── Venta.kt         # documento de la colección "ventas"
├── navigation/
│   ├── AppNavigation.kt      # NavHost con todas las rutas registradas
│   └── Routes.kt             # constantes de ruta (String), incluye rutaDeNotificacion()
├── repository/
│   ├── AuthRepository.kt     # login, rol de usuario, recuperar contraseña, datos de usuarios, actualizar campos, cerrar sesión
│   ├── NotificationRepository.kt # leer notificaciones y marcarlas como leídas (nunca las crea)
│   └── ProductRepository.kt  # guardar/editar/eliminar producto, consultas por id/artesano/lote de ids, like con transacción
├── viewModel/                 # ¡ojo con mayúscula/minúscula, ver más abajo!
│   ├── ArtisanProfileViewModel.kt
│   ├── ClientProfileViewModel.kt
│   ├── HomeArtesanoViewModel.kt
│   ├── LikesViewModel.kt
│   ├── LoginViewModel.kt
│   ├── NotificationsViewModel.kt # estado de la campana: lista + conteo de no leídas
│   ├── ProductViewModel.kt
│   └── RegisterViewModel.kt
└── ui/
    ├── admin/                     # Rol "administrador" (Fase 5)
    │   ├── AdminRegisterScreen.kt  # formulario mínimo: correo, contraseña, PIN
    │   ├── AdminRuacListScreen.kt  # solicitudes de RUAC pendientes
    │   └── AdminRuacReviewScreen.kt # aprobar/rechazar una solicitud
    ├── ruac/                      # Solicitud e insignia de RUAC (Fase 5)
    │   ├── RequestRuacScreen.kt   # el artesano solicita/re-solicita su RUAC
    │   └── RuacBadgeScreen.kt     # insignia real (reemplaza el placeholder de RUAC_BADGE)
    ├── sales/                     # Ventas, valoraciones, calidad/confianza (Fase 6)
    │   ├── RegisterSaleScreen.kt  # el artesano registra una venta a un cliente
    │   ├── RateSaleScreen.kt      # el cliente valora una compra (solo pills, sin texto)
    │   ├── QualityTrustScreen.kt  # resumen de calidad (producto) y confianza (artesano)
    │   └── PurchaseHistoryScreen.kt # historial real del cliente (reemplaza el placeholder)
    ├── components/                # Composables reutilizables del brand kit v2
    │   ├── PatternBackground.kt   # degradado + patrón textil, con blur opcional
    │   ├── GlassCard.kt           # tarjeta "de vidrio esmerilado"
    │   ├── GlassTextField.kt      # campo con ícono y línea inferior (fondo oscuro)
    │   ├── GradientButton.kt      # botón con degradado + variante outline
    │   ├── RolePillToggle.kt      # selector Artesano/Cliente en pills (Registro, y
    │   │                         # también el cambio de modo de los dos Home, con
    │   │                         # colores propios vía parámetros opcionales)
    │   ├── ImageUploadBox.kt      # selector de varias imágenes con miniaturas + eliminar
    │   │                         # (Publicar/Editar), hasta ProductViewModel.MAX_IMAGENES
    │   ├── MinkaFormField.kt      # campo con etiqueta + caja (fondo claro, artesano)
    │   ├── PlaceholderScreen.kt   # relleno para rutas de fases futuras (recognition, RUAC, etc.)
    │   ├── BarraValoracion.kt     # barra con degradado de marca (calidad/confianza), reemplaza a ConfianzaDots
    │   ├── ProductoFeedCard.kt    # tarjeta de producto (feed del cliente y perfil del artesano)
    │   ├── BottomNavBar.kt        # barra inferior (Home cliente y Home artesano)
    │   ├── IconoNotificaciones.kt # campana con badge de no leídas, compartida por los dos Home
    │   ├── MinkaDropdownField.kt  # selector de una lista fija (Departamento/Categoría/Clasificación)
    │   └── EfectoNotificaciones.kt # pide permiso de notificaciones + guarda el token FCM
    ├── home/
    │   ├── HomeArtesanoScreen.kt  # encabezado con campana + avatar de perfil
    │   └── HomeClienteScreen.kt   # encabezado con campana, búsqueda, chips, feed, bottom nav
    ├── login/LoginScreen.kt
    ├── notifications/NotificationsScreen.kt # centro de notificaciones, abierto desde la campana
    ├── product/ProductDetailScreen.kt  # detalle, artesano, calidad/confianza, contacto WhatsApp
    ├── profile/
    │   ├── ArtisanProfileScreen.kt  # perfil público del artesano, editable por su dueño
    │   ├── ClientProfileScreen.kt   # menú del cliente: info básica, likes, cerrar sesión
    │   ├── LikesScreen.kt           # lista de productos con like del usuario actual
    │   └── ProfileScreen.kt         # placeholder genérico, sin usar desde esta fase
    ├── publication/
    │   ├── MyProductsScreen.kt    # "Panel de Publicaciones": lista, editar/eliminar (menú ⋮)
    │   ├── PublicationScreen.kt
    │   └── EditProductScreen.kt
    ├── register/RegisterScreen.kt
    ├── welcome/WelcomeScreen.kt
    └── theme/
        ├── Color.kt
        ├── Theme.kt
        └── Type.kt
├── service/
│   └── MinkaMessagingService.kt   # FirebaseMessagingService: recibe el push, construye
│                                   # la notificación, guarda el token si rota
└── util/
    └── Texto.kt                    # normalizar(): quita tildes/mayúsculas para
                                     # búsqueda de texto; compartida por HomeClienteScreen
                                     # y MyProductsScreen (Ajustes 2)
```

Además, en la raíz del repositorio (fuera de `app/`):

```
firebase.json               # configuración del proyecto Firebase (Functions, Firestore)
.firebaserc                  # alias del proyecto Firebase (minka-art)
firestore.indexes.json       # índices compuestos de Firestore
functions/
├── src/index.ts             # Cloud Functions onCall (enviarCodigoVerificacion, verificarCodigo)
│                             # y triggers de Firestore que alimentan "notificaciones"
│                             # (notificarNuevaVenta, notificarNuevaValoracion,
│                             # notificarLikeNuevo, notificarCambioRuac)
├── lib/                     # salida compilada de TypeScript (generada, no editar a mano)
├── package.json
└── SETUP.md                 # cómo configurar el secreto RESEND_API_KEY y desplegar
```

`LoginViewModel.kt` y `ProductViewModel.kt` declaran `package com.minkaart2.app.viewModel`
(con `M` mayúscula), mientras que `RegisterViewModel.kt`, en la misma carpeta, declara
`package com.minkaart2.app.viewmodel` (todo minúscula). Son dos paquetes Kotlin distintos
que conviven en el mismo directorio — al importar, copia el import de un archivo ya
existente en vez de escribirlo de memoria.

## Navegación

Todas las rutas están centralizadas en `Routes.kt` y se registran (o no) en el `NavHost`
de `AppNavigation.kt`:

| Constante | Valor | Pantalla | ¿Registrada en el `NavHost`? | ¿Alguien navega hacia ella? |
|---|---|---|---|---|
| `WELCOME` | `welcome` | `WelcomeScreen` | Sí (destino inicial) | — |
| `LOGIN` | `login` | `LoginScreen` | Sí | `WelcomeScreen`, `LoginScreen` (recuperar contraseña) |
| `REGISTER` | `register` | `RegisterScreen` | Sí | `WelcomeScreen` |
| `HOME_CLIENTE` | `home_cliente` | `HomeClienteScreen` (catálogo real) | Sí | `LoginScreen` (rol `cliente`), toggle "Cliente" del encabezado de `HomeArtesanoScreen` (cambio de modo) |
| `HOME_ARTESANO` | `home_artesano` | `HomeArtesanoScreen` | Sí | `LoginScreen` (rol `artesano`), toggle "Artesano" del encabezado de `HomeClienteScreen` y fila "Cambiar a modo artesano" de `ClientProfileScreen` (ambos solo si el usuario es artesano — vuelven al modo artesano) |
| `MY_PRODUCTS` | `my_products` | `MyProductsScreen` (Panel de Publicaciones) | Sí | `HomeArtesanoScreen` |
| `PUBLICATION` | `publication` | `PublicationScreen` | Sí | `HomeArtesanoScreen`, `MyProductsScreen` (chip "Nueva") |
| `EDIT_PRODUCT` | `edit_product/{productId}` | `EditProductScreen` | Sí, con argumento de ruta | `MyProductsScreen` (menú ⋮ → Editar) |
| `PRODUCT_DETAIL` | `product_detail/{productId}` | `ProductDetailScreen` | Sí, con argumento de ruta | `HomeClienteScreen` (tap en una tarjeta del feed) |
| `ARTISAN_PROFILE` | `artisan_profile/{artesanoId}` | `ArtisanProfileScreen` | Sí, con argumento de ruta | `HomeClienteScreen` (nombre del artesano en una tarjeta), `ProductDetailScreen` (fila del artesano), `HomeArtesanoScreen` (ícono de perfil, propio) |
| `CLIENT_PROFILE` | `client_profile` | `ClientProfileScreen` | Sí | `HomeClienteScreen` (bottom nav "Perfil") — también la ve un artesano "viendo como cliente" (rol real, no la ruta), con contenido algo distinto (ver Flujos principales) |
| `LIKES` | `likes` | `LikesScreen` | Sí | `HomeClienteScreen` (bottom nav "Likes"), `ClientProfileScreen` ("Mis likes") |
| `PURCHASE_HISTORY` | `purchase_history` | `PurchaseHistoryScreen` (real desde Fase 6) | Sí | `ClientProfileScreen` ("Historial de compras") |
| `ACCOUNT_SETTINGS` | `account_settings` | `PlaceholderScreen` | Sí | `ClientProfileScreen` ("Configuración de la cuenta") |
| `PROFILE` | `profile` | `ProfileScreen` (placeholder) | Sí | Nadie — reemplazado por `ARTISAN_PROFILE`/`CLIENT_PROFILE`; queda sin usar |
| `RUAC_BADGE` | `ruac_badge/{artesanoId}` | `RuacBadgeScreen` (real desde Fase 5) | Sí, con argumento de ruta | `ArtisanProfileScreen` (chip de insignia RUAC verificado) |
| `RECOGNITION` | `recognition` | `PlaceholderScreen` | Sí | `HomeClienteScreen` (ícono de la barra de búsqueda) |
| `ADMIN_REGISTER` | `admin_register` | `AdminRegisterScreen` | Sí | `RegisterScreen` (enlace "¿Eres administrador?") |
| `ADMIN_RUAC_LIST` | `admin_ruac_list` | `AdminRuacListScreen` | Sí | `LoginScreen` (rol `administrador`) |
| `ADMIN_RUAC_REVIEW` | `admin_ruac_review/{artesanoId}` | `AdminRuacReviewScreen` | Sí, con argumento de ruta | `AdminRuacListScreen` (tap en una solicitud) |
| `REQUEST_RUAC` | `request_ruac` | `RequestRuacScreen` | Sí | `ArtisanProfileScreen` (botón "Agregar RUAC"/"Volver a solicitar RUAC", solo el propio artesano) |
| `REGISTER_SALE` | `register_sale/{productId}` | `RegisterSaleScreen` | Sí, con argumento de ruta | `MyProductsScreen` (menú ⋮ → "Registrar venta") |
| `RATE_SALE` | `rate_sale/{ventaId}` | `RateSaleScreen` | Sí, con argumento de ruta | `PurchaseHistoryScreen` (venta "Pendiente de valorar") |
| `QUALITY_TRUST` | `quality_trust/{productId}` | `QualityTrustScreen` | Sí, con argumento de ruta | `ProductDetailScreen` (bloque de calidad/confianza), `ArtisanProfileScreen` (indicador de confianza) |
| `NOTIFICATIONS` | `notifications` | `NotificationsScreen` | Sí | `HomeClienteScreen`, `HomeArtesanoScreen` (campana del encabezado) |

## Modelo de datos

### Firestore — colección `usuarios/{uid}`

Definido en `app/src/main/java/com/minkaart/app/data/model/User.kt`. El documento id es
el `uid` de Firebase Auth.

| Campo | Tipo | Notas |
|---|---|---|
| `uid` | `String` | |
| `nombre` | `String` | |
| `correo` | `String` | |
| `rol` | `String` | `"cliente"`, `"artesano"` o `"administrador"` (Fase 5; se compara en minúsculas al hacer login) |
| `telefono` | `String` | Solo artesano |
| `fechaNacimiento` | `String` | Solo artesano, formato `dd/MM/yyyy` |
| `region` | `String` | Solo artesano |
| `productosQueVende` | `String` | Solo artesano, texto libre |
| `categoriasArtesanales` | `List<String>` | Solo artesano; se captura como texto separado por comas y se parte en el registro |
| `ruacEstado` | `String` | Solo artesano. `"no_solicitado"` (default) \| `"pendiente"` \| `"verificado"` \| `"rechazado"` (Fase 5) |
| `ruacRegistro` | `String?` | Número de registro RUAC declarado por el artesano |
| `ruacRegion` | `String?` | Región declarada en la solicitud de RUAC (queda `null` si el RUAC vino del registro, ver más abajo) |
| `ruacTecnica` | `String?` | Técnica declarada en la solicitud de RUAC (mismo caso que `ruacRegion`) |
| `ruacDocumentoUrl` | `String?` | URL de descarga en Storage del documento/foto de soporte |
| `ruacSolicitadoEn` | `Timestamp?` | Cuándo se envió la solicitud |
| `ruacVerificadoEn` | `Timestamp?` | Cuándo un administrador la aprobó |
| `ruacUltimaValidacion` | `Timestamp?` | Se llena junto con `ruacVerificadoEn` al aprobar |
| `ruacRevisadoPor` | `String?` | `uid` del administrador que aprobó/rechazó (auditoría) |
| `bio` | `String?` | Solo artesano; texto corto "Sobre mí" en su Perfil, editable por él mismo |
| `contactoPublico` | `String?` | Solo artesano; el contacto que decide mostrar públicamente (puede ser distinto de `telefono`). `ProductDetailScreen` lo usa para "Contactar artesano", con `telefono` como respaldo si está vacío |
| `fcmToken` | `String?` | Token de Firebase Cloud Messaging del dispositivo actual. Se guarda/actualiza al abrir `HomeClienteScreen`/`HomeArtesanoScreen` (`EfectoNotificaciones`) y cuando el token rota (`MinkaMessagingService.onNewToken`). `null` hasta que el usuario abre la app al menos una vez tras este ajuste |

Desde la Fase 3, cualquier usuario autenticado puede leer el documento de **cualquier**
usuario (antes solo el propio uid) — necesario para que el cliente vea el perfil del
artesano dueño de un producto. Efecto secundario aceptado: campos no pensados para
mostrarse (`correo`, `fechaNacimiento`, datos de RUAC) también quedan visibles para
cualquier usuario con sesión; no existe un documento de "perfil público" separado.

**Nota de migración (Fase 5)**: los campos `ruacEstado`/`ruacRegistro`/`ruacRegion`/
`ruacTecnica`/`ruacDocumentoUrl`/... **reemplazan por completo** a los campos viejos
`tieneRuac`/`numeroRuac`/`ruacUrl` (ya no existen en `User.kt`) — no conviven ambos
esquemas. El proyecto no tiene Cloud Functions ni un script de migración, así que esto es
retrocompatible solo en el sentido de que ningún documento existente deja de leerse
(`toObject()` ignora campos que sobran y usa el default de los que faltan), **pero no
preserva el significado**: cualquier artesano que ya tuviera `tieneRuac == true` antes de
esta fase queda visto como `ruacEstado == "no_solicitado"` hasta que vuelva a solicitar el
RUAC desde su perfil con el flujo nuevo. El formulario de registro de artesano sigue
pidiendo RUAC igual que siempre (número + documento); si el artesano dice que sí tiene,
queda directo en `ruacEstado = "pendiente"` con `ruacRegistro`/`ruacDocumentoUrl` llenos
(entra a la misma cola de revisión del administrador) pero `ruacRegion`/`ruacTecnica`
quedan en `null`, porque el registro no los pide — solo `RequestRuacScreen` (solicitud
posterior desde el perfil) los captura.

**RUAC editable/eliminable (ajuste posterior a la Fase 5)**: `"verificado"` dejó de ser un
estado final. El propio artesano puede "Editar RUAC" (reabre `RequestRuacScreen`
precargado con sus datos; al guardar vuelve a `ruacEstado = "pendiente"` y actualiza
`ruacSolicitadoEn`, así que la insignia deja de mostrarse como verificada hasta que el
administrador la revise de nuevo — la solicitud editada reaparece en
`AdminRuacListScreen` igual que una nueva) o "Eliminar RUAC" (`ruacEstado =
"no_solicitado"` y limpia `ruacRegistro`/`ruacRegion`/`ruacTecnica`/`ruacDocumentoUrl`/
`ruacVerificadoEn`/`ruacUltimaValidacion`; no pasa por revisión, es el artesano retirando
su propio dato — `ruacSolicitadoEn`/`ruacRevisadoPor` no se tocan, quedan como rastro de
auditoría que no se muestra en ninguna pantalla).

### Firestore — documento `configuracion/adminAccess`

Un único documento, con un campo `pin: String`, que el administrador necesita para
registrarse (ver más abajo). **No lo crea ninguna pantalla de la app** — hay que crearlo a
mano en Firebase Console → Firestore Database antes de poder registrar la primera cuenta
de administrador. Ver la advertencia de seguridad sobre este PIN en "Estado actual y
pendientes".

### Firestore — colección `productos/{autoId}`

Definido en `app/src/main/java/com/minkaart/app/data/model/Product.kt`. El id lo genera
Firestore (`document()` sin argumentos).

| Campo | Tipo | Notas |
|---|---|---|
| `id` | `String` | Igual al id del documento |
| `artesanoId` | `String` | `uid` del artesano que publicó, tomado de `FirebaseAuth` |
| `nombre` | `String` | |
| `descripcion` | `String` | |
| `precio` | `Long` | |
| `historiaProceso` | `String` | Historia / proceso de elaboración |
| `materiaPrima` | `String` | Uno de los 50 valores curados de `CatalogosProducto.MATERIAS_PRIMAS` (dropdown en Publicar/Editar; era texto libre hasta un ajuste posterior, revertido porque generaba un catálogo interminable de chips de filtro) — si se elige "Otra", aparece un campo obligatorio "Especifica cuál" que se guarda en `materiaPrimaOtro`. `""` en productos publicados antes de agregar este campo. Alimenta los chips de filtro de materia prima de `HomeClienteScreen`, igual que `categoria` — derivados de los valores reales presentes en los productos cargados, no directamente de la lista fija |
| `materiaPrimaOtro` | `String?` | Solo cuando `materiaPrima == "Otra"`. `null` en cualquier otro caso. `EditProductScreen` detecta un `materiaPrima` de un producto anterior al dropdown que no está en la lista curada y lo mueve aquí automáticamente (mismo criterio que la migración de `regionOrigen`) |
| `regionOrigen` | `String` | **Heredado, solo lectura** (ver nota de migración abajo): ningún código nuevo lo vuelve a escribir, reemplazado por `departamento`/`municipio` |
| `departamento` | `String` | Uno de los 33 departamentos de Colombia (`CatalogosProducto.DEPARTAMENTOS`), elegido con un dropdown en Publicar/Editar |
| `municipio` | `String` | Texto libre — no hay una lista completa de los ~1100 municipios de Colombia dentro de la app |
| `clasificacion` | `String?` | Opcional. Uno de `CatalogosProducto.CLASIFICACIONES` ("Artesanía indígena" / "tradicional popular" / "contemporánea o neoartesanía") o `null` si no se especificó |
| `imagenes` | `List<String>` | URLs de Storage, hasta `ProductViewModel.MAX_IMAGENES` (15). `ProductDetailScreen` las muestra en un carrusel (`HorizontalPager`); tocar la imagen abre un visor de pantalla completa con zoom (pinch) y paneo |
| `vistas` | `Long` | Por defecto `0`. Sube en 1 cada vez que `ProductDetailScreen` abre el producto (`ProductRepository.incrementarVista`, `FieldValue.increment(1)`), salvo que quien lo abra sea el propio artesano dueño (en cualquier modo). Cuenta por apertura, no por visitante único: abrir el mismo producto varias veces suma varias vistas |
| `likes` | `Long` | Por defecto `0`. Se incrementa/decrementa con `ProductRepository.alternarLike`, en una transacción junto con `productos/{id}/likes/{uid}` (ver más abajo) |
| `activo` | `Boolean` | Por defecto `true`. El artesano lo cambia desde el Panel de Publicaciones (menú ⋮ → Pausar/Activar); el catálogo del cliente (`HomeClienteScreen`) solo muestra los productos con `activo == true` |
| `fechaPublicacion` | `Long` | Epoch millis, con el reloj del dispositivo (no `FieldValue.serverTimestamp()`); `0` en productos publicados antes de agregar este campo |
| `categoria` | `String` | Una de las 16 opciones curadas de `CatalogosProducto.CATEGORIAS` (antes era texto libre); alimenta los chips de filtro del catálogo de cliente, que siguen derivándose de los valores reales guardados, no de esta lista fija |
| `categoriaOtro` | `String?` | Solo cuando `categoria == "Otros"`: el texto libre que escribió el artesano en "Especifica cuál". `null` en cualquier otro caso (se limpia solo si el artesano cambia de categoría) |
| `calidadSumaPuntos` | `Int` | Por defecto `0`. Suma de los puntos (1-4) de cada valoración de compra recibida — junto con `calidadConteo`, permite recalcular el promedio sin releer todas las valoraciones |
| `calidadConteo` | `Int` | Por defecto `0`. Cuántas valoraciones recibió este producto |
| `calidadPromedio` | `Double?` | `null` mientras `calidadConteo == 0`; la UI lo trata como "sin valoraciones aún" en vez de inventar un valor. Se recalcula en `SaleRepository.enviarValoracion` (Fase 6) |

**La confianza NO vive en `productos`** — es del artesano, no del producto (ver
`artesanoStats/{artesanoId}` más abajo). Antes de la Fase 6 existía por error un campo
`Product.confianzaPromedio`, que nunca llegó a calcularse (se corrigió, ya no existe).

**Nota de migración (`regionOrigen` → `departamento`/`municipio`)**: un producto publicado
antes de este ajuste solo tiene `regionOrigen` (texto libre); su tarjeta/detalle lo sigue
mostrando tal cual hasta que el artesano lo edite. Al abrir `EditProductScreen`, si
`departamento` viene vacío se intenta un match exacto (sin distinguir mayúsculas) entre
`regionOrigen` y los 33 departamentos: si coincide, se precarga ese departamento; si no
coincide (porque `regionOrigen` traía texto libre, p. ej. una ciudad), el dropdown de
departamento queda sin seleccionar y ese texto viejo se pone en el campo Municipio para no
perderlo — el artesano lo reorganiza y, al guardar, el producto ya queda en el esquema
nuevo. No hay ninguna migración automática por script.

Ambos `data class` declaran valores por defecto en todos los campos: es un requisito de
`DocumentSnapshot.toObject()` de Firestore, no un estilo opcional. Gracias a eso, agregar
campos nuevos a `Product` es retrocompatible sin ningún script de migración: los documentos
ya existentes en Firestore, que no tienen esos campos, se leen con sus valores por defecto
automáticamente. **Importante**: ese valor por defecto solo aplica *después* de traer el
documento — un filtro de Firestore como `whereEqualTo("activo", true)` no encuentra los
documentos que nunca tuvieron el campo escrito, aunque el default en Kotlin sea `true`. Por
eso `obtenerTodosLosProductosActivos` trae todos los productos y filtra `activo` **en
memoria**, no en el propio query — así los productos publicados antes de agregar el campo
también aparecen en el catálogo.

### Firestore — colección `productos/{autoId}/likes/{uid}`

Subcolección: un documento por usuario que le dio "me gusta" a ese producto.

| Campo | Tipo | Notas |
|---|---|---|
| `userId` | `String` | Igual al id del documento; se guarda también como campo para poder hacer `collectionGroup("likes").whereEqualTo("userId", uid)` y saber en una sola consulta qué productos ya le gustan al usuario actual, sin una consulta por cada tarjeta del feed |
| `creadoEn` | `Timestamp` | `FieldValue.serverTimestamp()` |

`ProductRepository.alternarLike` crea o borra este documento y ajusta `likes` en el
producto padre dentro de la misma transacción de Firestore, para que nunca queden
desincronizados. La consulta por `collectionGroup` requiere un índice de campo único con
alcance de grupo de colección, declarado en `firestore.indexes.json` (`fieldOverrides`, no
el arreglo `indexes` — ese es solo para índices compuestos).

**Regla de Firestore para la consulta `collectionGroup`**: la regla anidada
`match /productos/{productId}/likes/{userId}` (en `y`) solo autoriza lecturas/escrituras
dirigidas a esa ruta exacta o consultas acotadas a la subcolección de un producto puntual
— **no** autoriza un `collectionGroup("likes")` que recorra los "likes" de todos los
productos a la vez, que es justo lo que usa `obtenerProductosLikeadosPorUsuario()` para
`LikesScreen`. Esto causaba `PERMISSION_DENIED` al abrir Likes aunque la regla anidada ya
permitiera el mismo campo para lecturas directas. La solución es una regla aparte, a nivel
raíz, con el comodín `{path=**}`:

```
match /{path=**}/likes/{likeId} {
  allow read: if request.auth != null;
}
```

Las dos reglas conviven sin conflicto (en Firestore, si cualquier `match` aplicable
concede el permiso, la operación se permite). El índice de `firestore.indexes.json` ya
era correcto — el `PERMISSION_DENIED` era un problema de reglas, no de índices.

### Firestore — colección `ventas/{autoId}` (Fase 6)

| Campo | Tipo | Notas |
|---|---|---|
| `id` | `String` | Igual al id del documento |
| `productoId` | `String` | |
| `artesanoId` | `String` | Debe ser el dueño real de `productoId` — verificado con un `get()` en la regla de `create` |
| `clienteId` | `String` | Debe tener `rol == "cliente"` — verificado igual que `artesanoId` |
| `fechaVenta` | `Timestamp` | Elegida en `RegisterSaleScreen` (por defecto, hoy) |
| `valorado` | `Boolean` | Por defecto `false`. Pasa a `true` junto con la creación de `valoraciones/{esta-misma-id}` |
| `creadoEn` | `Timestamp?` | Reloj del cliente (`Timestamp.now()`), no `serverTimestamp()` — se escribe como parte de un objeto completo, no de un mapa parcial |

Solo el artesano dueño del producto puede crear una venta, y solo el cliente comprador
puede leerla o marcarla como valorada — a diferencia de `productos`, nadie más (ni un
tercer usuario ni un administrador) puede leer una venta ajena.

### Firestore — colección `valoraciones/{ventaId}` (Fase 6)

Usa **el mismo id que la venta** que valora — así una venta solo se puede valorar una vez
sin lógica extra (un `create` sobre un id ya ocupado lo rechaza Firestore directamente).

| Campo | Tipo | Notas |
|---|---|---|
| `id` | `String` | Igual al id de la venta |
| `productoId` / `artesanoId` / `clienteId` | `String` | Copiados de la venta |
| `calidad` | `String` | Una de `EscalaValoracion.NIVELES_CALIDAD` ("Excelente"/"Buena"/"Regular"/"Baja") |
| `confianza` | `String` | Una de `EscalaValoracion.NIVELES_CONFIANZA` ("Muy alta"/"Alta"/"Media"/"Baja") |
| `creadoEn` | `Timestamp?` | |

Nunca se edita ni se borra (`allow update, delete: if false` en las reglas), a propósito,
para que el puntaje no se pueda manipular después de aprobado.

### Firestore — documento `artesanoStats/{artesanoId}` (Fase 6)

La **confianza** del artesano — a diferencia de la calidad (que es por producto), la
confianza es una sola cifra por persona, acumulada de todas sus ventas valoradas. No
existe hasta la primera valoración recibida; mientras tanto se trata como
`ArtesanoStats()` (todo en 0/`null`), no como un error.

| Campo | Tipo | Notas |
|---|---|---|
| `confianzaSumaPuntos` | `Int` | Suma de puntos (1-4) de cada valoración de confianza recibida |
| `confianzaConteo` | `Int` | Cuántas valoraciones recibió este artesano |
| `confianzaPromedio` | `Double?` | `null` mientras `confianzaConteo == 0` |

`SaleRepository.enviarValoracion` actualiza `productos` (calidad) y `artesanoStats`
(confianza) en la **misma transacción** que crea la valoración y marca la venta como
valorada — todo o nada.

**Conversión a puntos** (`data/EscalaValoracion.kt`): el primer nivel de cada escala vale
4 puntos, el último vale 1 (Excelente/Muy alta=4 … Baja=1). El nivel mostrado en pantalla
es el promedio redondeado al entero más cercano (mitad hacia arriba) mapeado de vuelta a
la etiqueta; si el conteo es 0, se muestra "Aún no hay valoraciones" en vez de inventar
un nivel.

**Bug corregido — `PERMISSION_DENIED` al enviar una valoración**: la regla de
`productos/{productId}` comparaba `resource.data.calidadSumaPuntos`/`calidadConteo` con
acceso directo (`.campo`). Un producto publicado o editado por última vez *antes* de la
Fase 6 no tiene esos campos escritos en el documento real de Firestore (el default de
Kotlin `= 0` solo aplica después de leer el documento). Acceder con `.` a un campo que no
existe **no da `null`, produce un error de evaluación**, y ese error hace que toda la
regla se deniegue — sin ningún detalle en el cliente, solo `PERMISSION_DENIED`. Se
corrigió usando `.get(campo, valorPorDefecto)` en toda la comparación de `productos`
(ver `y`, función `productoBaseIgual`), y de paso se extrajo esa comparación a una
función de reglas reutilizable en vez de repetirla tres veces — la próxima vez que se
agregue un campo a `Product` se agrega en un solo lugar.

**Tercera rama de `allow update` para `productos` (conteo de vistas)**: igual que las
ramas de like y de calidad, un no-dueño puede actualizar `vistas` siempre que
`productoBaseIgual` se cumpla, `likes`/`calidadSumaPuntos`/`calidadConteo` no cambien, y
`vistas` suba en exactamente 1 — la usa `ProductDetailViewModel.cargarDetalle` al abrir
el detalle. `vistas` salió de `productoBaseIgual` (antes bloqueaba cualquier cambio,
como el resto de campos "base") y `materiaPrima` entró — se había quedado afuera desde
que se agregó a `Product.kt`, lo que dejaba a cualquier no-dueño cambiarlo sin que
ninguna rama lo notara. `materiaPrimaOtro` entró al mismo tiempo que `materiaPrima` se
convirtió en dropdown, por la misma razón.

### Firestore — colección `notificaciones/{autoId}`

Definido en `app/src/main/java/com/minkaart/app/data/model/Notificacion.kt`. Respalda la
campana de notificaciones (ícono en `HomeClienteScreen`/`HomeArtesanoScreen`,
`NotificationsScreen`). A diferencia de todas las demás colecciones del proyecto, **la
app nunca crea un documento aquí** — las reglas lo bloquean (`allow create: if false`) y
solo las Cloud Functions (Admin SDK, que ignora las reglas) los escriben, para que ningún
usuario pueda fabricar un aviso falso para otro. El propio destinatario solo puede
marcarlo como leído.

| Campo | Tipo | Notas |
|---|---|---|
| `id` | `String` | Igual al id del documento |
| `destinatarioId` | `String` | `uid` de quien debe ver la notificación |
| `tipo` | `String` | `"venta"` \| `"valoracion"` \| `"like"` \| `"ruac_aprobado"` \| `"ruac_rechazado"` |
| `titulo` / `mensaje` | `String` | Texto mostrado en la fila y en el push de FCM |
| `referenciaId` | `String` | Cambia de significado según `tipo`: `ventaId` (venta), `productoId` (valoracion/like) o el propio `uid` del artesano (ruac_aprobado/rechazado) — `Routes.rutaDeNotificacion(tipo, referenciaId)` es el único lugar que interpreta esta combinación para navegar |
| `leida` | `Boolean` | Por defecto `false` |
| `creadoEn` | `Timestamp?` | `FieldValue.serverTimestamp()`, puesto por la Cloud Function |

La consulta de `NotificationRepository.obtenerNotificaciones` es una sola igualdad
(`whereEqualTo("destinatarioId", uid)`); el orden por fecha se hace en memoria, mismo
criterio que `obtenerTodosLosProductosActivos`/`obtenerVentasDelCliente` — no hace falta
ningún índice compuesto nuevo en `firestore.indexes.json`.

Las cuatro Cloud Functions que escriben aquí comparten el helper `avisar()` en
`functions/src/index.ts`, que además envía el push de FCM (`data`-only, mismo criterio ya
documentado abajo) si `usuarios/{destinatarioId}.fcmToken` existe:

- `notificarNuevaVenta` (trigger en `ventas/{ventaId}`, ya existía) → cliente.
- `notificarNuevaValoracion` (trigger en `valoraciones/{ventaId}`) → artesano.
- `notificarLikeNuevo` (trigger en `productos/{productId}/likes/{userId}`) → artesano
  dueño del producto, salvo que se dé like a sí mismo.
- `notificarCambioRuac` (trigger en `usuarios/{uid}`, solo si `ruacEstado` cambió a
  `"verificado"` o `"rechazado"`) → el propio artesano.

### Firestore — colección `codigosVerificacion/{correo}`

Definida y usada exclusivamente por las Cloud Functions (`functions/src/index.ts`); la app
Android **nunca lee ni escribe esta colección directamente**, solo llama a las funciones
`enviarCodigoVerificacion` y `verificarCodigo`. El documento id es el correo electrónico.

| Campo | Tipo | Notas |
|---|---|---|
| `codigo` | `String` | 6 dígitos, generado por la función |
| `expiracion` | `Timestamp` | 10 minutos después de generado |
| `intentos` | `Number` | Se incrementa en cada código incorrecto; máximo 5 |
| `creado` | `Timestamp` | `FieldValue.serverTimestamp()` |

El documento se borra automáticamente cuando el código se verifica con éxito, para que no
pueda reutilizarse.

### Firebase Storage

| Ruta | Contenido |
|---|---|
| `productos/{artesanoId}/{productoId}/imagen_{índice}` | Fotos de una artesanía (`imagen_0`, `imagen_1`, …, hasta 15), subidas al publicar. Al editar, las imágenes nuevas se suben con el prefijo `imagen_{marcaDeTiempo}_{índice}` para no chocar con los índices ya ocupados por las que se conservan — el orden final en `Product.imagenes` es el que importa, no el nombre del archivo en Storage |
| `productos/{artesanoId}/{productoId}/imagen_principal` | **Heredado**: nombre de archivo usado antes de agregar soporte de varias imágenes (una sola foto por producto). Los productos publicados antes de este ajuste conservan su imagen en esa ruta; su URL sigue guardada tal cual en `Product.imagenes`, nadie reconstruye la ruta a partir del nombre |
| `ruac/{uid}/documento_ruac` | Documento RUAC adjuntado en el registro |

## Flujos principales

- **Registro** (`RegisterScreen` → `RegisterViewModel`): valida nombre, correo y
  contraseña (mínimo 6 caracteres) para cualquier rol; si el rol es `artesano`, exige
  además teléfono, fecha de nacimiento, región, productos que vende y al menos una
  categoría. Si `tieneRuac == true`, exige número y documento. El registro se completa
  en dos pasos, con verificación de correo por código de 6 dígitos generado por Cloud
  Functions (no por el mecanismo nativo de Firebase Auth): **la cuenta de Firebase Auth
  se crea solo después de verificar el código**, así nunca queda una cuenta huérfana en
  Auth si el usuario no termina el proceso.
  1. **`RegisterViewModel.iniciarRegistro`**: tras las validaciones locales, llama a
     `AuthRepository.enviarCodigoVerificacion` (Cloud Function `enviarCodigoVerificacion`),
     que genera un código de 6 dígitos, lo guarda en `codigosVerificacion/{correo}` con
     expiración de 10 minutos y lo envía por correo con nodemailer vía el SMTP de Gmail.
     Los datos del formulario
     (incluida la contraseña y el `Uri` del RUAC) quedan en memoria del ViewModel
     (`datosPendientes`) — **ninguna cuenta de Firebase Auth existe todavía** y el
     documento en `usuarios` tampoco se escribe. El botón "Registrarse" pasa a mostrarse
     como "Enviar código de verificación" y, tras el envío, la pantalla muestra un campo
     numérico para el código de 6 dígitos, un botón "Verificar" y otro "Reenviar código".
  2. **`RegisterViewModel.confirmarCodigoDeVerificacion`**: el usuario ingresa el código
     recibido; la función llama a `AuthRepository.verificarCodigo` (Cloud Function
     `verificarCodigo`), que lo compara contra Firestore (máximo 5 intentos fallidos, 10
     minutos de vigencia). **Solo si el código es válido**, `RegisterViewModel` recién ahí
     crea la cuenta con `createUserWithEmailAndPassword`, sube el RUAC a Storage si aplica
     y escribe el documento en `usuarios`; el botón cambia a "✓ Has sido registrado
     correctamente". Si el código es incorrecto, expiró o se agotaron los intentos, se
     muestra el mensaje de error correspondiente y **los datos del formulario no se
     pierden**: el usuario puede reintentar o reenviar el código sin volver a llenarlo.
- **Login** (`LoginScreen` → `LoginViewModel` → `AuthRepository`): autentica con
  Firebase Auth y luego consulta el campo `rol` en `usuarios/{uid}` para decidir el
  destino (`HOME_CLIENTE` o `HOME_ARTESANO`), limpiando `LOGIN` del back stack.
- **Publicar artesanía** (`PublicationScreen` → `ProductViewModel` → `ProductRepository`):
  valida todos los campos y que haya al menos una imagen seleccionada (máximo
  `ProductViewModel.MAX_IMAGENES` = 15, elegidas con
  `ActivityResultContracts.GetMultipleContents()` vía `ImageUploadBox`), sube cada imagen a
  Storage y solo entonces escribe el producto en Firestore con las URLs en el mismo orden
  en que se eligieron — el orden de subida importa, si alguna falla no se crea el
  documento. `EditProductScreen` puede quitar imágenes existentes y agregar nuevas en la
  misma edición; ver la nota de migración de `regionOrigen` y la tabla de Storage más
  abajo para el prefijo de archivo que usa cada caso. Materia prima es un dropdown de 50
  opciones curadas (`CatalogosProducto.MATERIAS_PRIMAS`) — si se elige "Otra", aparece
  un campo obligatorio "Especifica cuál" que se guarda en `materiaPrimaOtro` (mismo
  patrón que Categoría/"Otros"; era texto libre hasta un ajuste posterior, revertido
  porque generaba un catálogo interminable en los chips de filtro de materia prima). El
  origen se captura como
  Departamento (dropdown, `CatalogosProducto.DEPARTAMENTOS`) + Municipio (texto libre,
  `MinkaDropdownField`/`MinkaFormField`), la Categoría es un dropdown de 16 opciones
  curadas (`CatalogosProducto.CATEGORIAS`) — si se elige "Otros", aparece un campo
  obligatorio "Especifica cuál" que se guarda en `categoriaOtro` — y la Clasificación es
  un dropdown opcional de 3 valores (o "Sin especificar" → `null`). `EditProductScreen`
  usa el mismo formulario y componentes; ver las notas de migración de `regionOrigen` y
  de `materiaPrima` en el modelo de `productos` para cómo se comportan con productos
  publicados antes de cada ajuste.
- **Gestión de productos** (`HomeArtesanoScreen` → `MyProductsScreen` "Panel de
  Publicaciones" → `EditProductScreen`): el Home del artesano saluda por nombre (vía
  `HomeArtesanoViewModel.cargarDatosArtesano` → `AuthRepository.obtenerUsuarioActual`),
  muestra el toggle de cambio de modo (ver "Cambio de modo artesano/cliente" más abajo),
  los botones Publicar/Ver publicaciones y el panel de Estadísticas — ya no muestra una
  grilla de productos (se quitó: era redundante con la grilla de `ArtisanProfileScreen` y
  la lista de `MyProductsScreen`, las dos a un tap de distancia); el botón "Ver
  publicaciones" lleva al Panel, que lista todos los productos del artesano con un menú
  de tres puntos por tarjeta. El Home del artesano
  tiene su propio bottom nav (`BottomNavBar`, compartido con `HomeClienteScreen`), ahora
  con 4 ítems (antes 5: se quitó "Explorar", reemplazado por el toggle — ver más abajo):
  Inicio (la propia pantalla), RUAC → `Routes.ruacBadgeRoute` (placeholder), Likes →
  `Routes.LIKES`, Perfil → `Routes.artisanProfileRoute` con el uid del propio artesano
  (mismo destino que el ícono de perfil del encabezado, que se dejó también por no
  estorbar):
  - **Editar** navega a `EditProductScreen` (ruta `edit_product/{productId}`), que carga
    el producto con `ProductViewModel.obtenerProductoPorId`, precarga el formulario y,
    al guardar, llama a `actualizarProducto` (mismas validaciones que publicar) y vuelve
    al Panel.
  - **Eliminar** muestra un `AlertDialog` de confirmación antes de llamar a
    `ProductViewModel.eliminarProducto`, que borra el documento en Firestore y lo quita
    de la lista en memoria al instante.
  - **Pausar publicación / Activar publicación** llama a
    `ProductViewModel.cambiarEstadoProducto`, que actualiza solo el campo `activo` en
    Firestore (`update()` parcial, no reescribe el resto del documento) y refleja el
    cambio al instante en la lista en memoria. Cada tarjeta muestra además "`{vistas}`
    vistas · `{likes}` likes" (ambos contadores reales — ver la nota de `vistas` en el
    modelo de `productos`) y, si el producto tiene `fechaPublicacion`, "Publicado el
    dd/MM/yyyy".
  - El Panel se refresca automáticamente cada vez que vuelve a primer plano
    (`ON_RESUME`), para que los cambios de Editar/Publicar se vean sin reiniciar la app —
    Navigation Compose mantiene su `ProductViewModel` vivo en el back stack, así que un
    `LaunchedEffect(Unit)` por sí solo no bastaría.
  - El Panel tiene su propia barra de búsqueda + chips de categoría (mismo componente
    visual y misma lógica de filtro — texto y categoría combinados con AND — que usa
    `HomeClienteScreen`), pero acotada a **`misProductos`**: el artesano busca solo dentro
    de su propio catálogo, no en el de los demás. La función `normalizar()` (quita
    tildes/mayúsculas para que la búsqueda no dependa de escribir los acentos exactos) se
    extrajo a `util/Texto.kt` para que ambas pantallas la reutilicen en vez de tener cada
    una su propia copia. `HomeArtesanoScreen` en sí (el saludo + el panel de
    Estadísticas) no tiene buscador propio ni lista de productos — se decidió poner la
    búsqueda en el Panel, por ser la única pantalla del lado artesano con una lista real
    de productos sobre la que buscar.
  - **Panel de Estadísticas** (debajo de Publicar/Ver publicaciones, en el propio
    `HomeArtesanoScreen`): se calcula sobre **todos** los productos del artesano
    (`productViewModel.misProductos`, la misma lista que ya carga esta pantalla, sin
    ninguna consulta nueva) — suma de likes, suma de vistas, activos/total, y el
    producto más visto y el producto con más likes (miniatura + nombre + el número,
    tap → `Routes.productDetailRoute`). Si ningún producto tiene vistas ni likes,
    muestra "Aún no hay actividad" en vez de destacar un producto al azar en el empate
    en 0. Se refresca junto con el resto del Home al volver a primer plano (mismo
    `ON_RESUME` que ya usan el Panel de Publicaciones y el Perfil del artesano).
- **Cambio de modo artesano/cliente**: reemplaza al antiguo ítem "Explorar" del bottom
  nav de `HomeArtesanoScreen`, que mandaba a `HomeClienteScreen` sin ninguna señal de
  que se cambió de "identidad" y sin forma clara de volver. Ahora es un toggle
  ("Artesano"/"Cliente", reutilizando `RolePillToggle` — ver "Diseño y marca") en el
  encabezado de los dos Home:
  - En `HomeArtesanoScreen`, tocar "Cliente" navega a `Routes.HOME_CLIENTE` con un
    `navigate()` normal (`HOME_ARTESANO` se queda debajo en el back stack a propósito).
  - En `HomeClienteScreen`, el toggle solo aparece si `HomeClienteViewModel.esArtesano`
    es `true` — se decide leyendo el `rol` real del usuario autenticado en
    `usuarios/{uid}` (`cargarRolUsuarioActual`, mismo patrón que
    `HomeArtesanoViewModel.cargarDatosArtesano`), **nunca por la ruta**: un cliente real
    jamás ve este toggle. Queda fijo antes del buscador (no hace scroll con el feed),
    así funciona también como indicador persistente de en qué modo está. Tocar
    "Artesano" limpia todo lo explorado y vuelve a un `HOME_ARTESANO` limpio:
    ```kotlin
    navController.navigate(Routes.HOME_ARTESANO) {
        popUpTo(Routes.HOME_ARTESANO) { inclusive = true }
        launchSingleTop = true
    }
    ```
  - `ClientProfileScreen` también es consciente del rol: si quien lo abre es un
    artesano "viendo como cliente", oculta "Historial de compras" (las ventas exigen
    `rol == "cliente"` en `AuthRepository.obtenerClientes`, así que ahí siempre vería
    una lista vacía sin motivo claro) y agrega una fila "Cambiar a modo artesano" con
    la misma navegación de arriba — una salida adicional para cuando llegó a Perfil sin
    volver a pasar por `HomeClienteScreen` (p. ej. desde Likes). Ninguna otra pantalla
    alcanzable desde el catálogo (Likes, notificaciones, Detalle, perfil de artesano)
    necesita ocultarse.
  - Al iniciar sesión, el artesano siempre entra en modo artesano (`LoginScreen` ya lo
    manda a `HOME_ARTESANO`) — el modo no se persiste entre sesiones.
- **Catálogo del cliente** (`HomeClienteScreen` → `HomeClienteViewModel`): barra de
  búsqueda real (el ícono de cámara navega a `Routes.RECOGNITION`, todavía un
  `PlaceholderScreen` — el reconocimiento visual con OpenCV es de otra fase), chips de
  categoría y, debajo, chips de materia prima — ambos armados dinámicamente a partir de
  los valores distintos de `categoria`/`materiaPrima` presentes en los productos
  cargados, no de una lista fija; los tres filtros (categoría, materia prima, texto) se
  combinan con AND. El feed (`obtenerTodosLosProductosActivos`)
  trae los productos de **todos** los artesanos, a diferencia del Panel de Publicaciones,
  que solo trae los propios; el nombre del artesano de cada tarjeta se resuelve con
  `AuthRepository.obtenerUsuarioPorId`, cacheado por `artesanoId` en el ViewModel para no
  repetir la consulta por cada producto del mismo artesano. El corazón de cada tarjeta usa
  `productosLikeados` (`ProductRepository.obtenerProductosLikeadosPorUsuario`, una sola
  consulta con `collectionGroup` en vez de una por tarjeta) para saber si ya se le dio like.
  Tap en una tarjeta navega a `Routes.productDetailRoute(productId)`. El bottom nav
  (`ui/components/BottomNavBar.kt`, compartido con `HomeArtesanoScreen`) navega a
  `Routes.CLIENT_PROFILE` desde "Perfil" y a `Routes.LIKES` desde "Likes"; "Buscar" no
  navega a ningún lado, solo hay una barra de búsqueda en esta misma pantalla.

  **El buscador filtra de verdad** (`titulo`/`descripcion` de cada producto), combinado
  con **AND** junto al chip de categoría y al chip de materia prima activos — los tres
  filtros se aplican en memoria
  sobre los productos ya cargados, sin ninguna consulta nueva a Firestore por cada tecla:
  Firestore no tiene búsqueda de texto completo nativa y el tamaño del catálogo no
  justifica un servicio externo (Algolia, extensiones, etc.). El texto escrito se
  "asienta" 300ms después de la última tecla (`LaunchedEffect(textoBusqueda) { delay(300);
  ... }`) antes de recalcular el filtro, y tanto el texto escrito como
  `nombre`/`descripcion` de cada producto se normalizan (sin tildes, minúsculas, vía
  `java.text.Normalizer` del propio JDK) para que "ceramica" encuentre "Cerámica".
- **Detalle y contacto** (`ProductDetailScreen` → `ProductDetailViewModel`): encadena dos
  lecturas (`obtenerProductoPorId` y, con el `artesanoId` del resultado,
  `obtenerUsuarioPorId`) porque Firestore no tiene joins. Al cargar el producto,
  `cargarDetalle` suma 1 a `vistas` (`ProductRepository.incrementarVista`) salvo que
  quien lo abra sea el propio artesano dueño (en cualquier modo) — un guard en el
  ViewModel (`vistaContadaPara`, no un `State`) evita contar de nuevo en cada
  recomposición o rotación, pero sí vuelve a contar si se navega otra vez al mismo
  producto más tarde (una apertura nueva). La imagen principal es un
  carrusel (`HorizontalPager`) sobre `producto.imagenes` con puntos de página si hay más de
  una; tocar la imagen abre un visor en pantalla completa (`Dialog`) que conserva el
  carrusel y agrega zoom (pinch, 1x–4x) y paneo sobre la imagen ampliada
  (`detectTransformGestures` + `graphicsLayer`, sin ninguna librería nueva). Muestra nombre, precio,
  categoría, descripción, historia y región del producto, y una fila con el artesano
  (nombre, contacto, insignia RUAC en miniatura si `tieneRuac`) que navega a su
  `Routes.artisanProfileRoute(artesanoId)`. Dos `BarraValoracion` (calidad y confianza,
  con degradado de marca y conteo de verificaciones — reemplazan a `ConfianzaDots`,
  ver "Diseño y marca"). El botón "Contactar artesano" usa `contactoPublico` (con `telefono` como
  respaldo si el artesano no lo configuró), limpia el número a solo dígitos y abre
  `https://wa.me/<numero>?text=<mensaje>` con un intent `ACTION_VIEW`; si no hay ninguna
  app que lo resuelva, cae a `ACTION_DIAL` con el número tal cual.
- **Perfil del artesano** (`ArtisanProfileScreen` → `ArtisanProfileViewModel`): cabecera
  con el mismo patrón + degradado de Entrada/Login (`PatternBackground`), avatar, chip de
  RUAC, bio y contacto público, estadísticas (cantidad de productos, suma de `likes`),
  `BarraValoracion` sobre el promedio de los productos del artesano, y su grilla de
  productos (`ProductoFeedCard`, compartida con el feed del cliente). Si
  `currentUserId == artesanoId`, aparece un ícono de editar junto al contacto que abre un
  diálogo para actualizar `bio`/`contactoPublico`
  (`AuthRepository.actualizarCamposUsuario`, un `update()` parcial genérico). Accesible
  desde el nombre del artesano en una tarjeta del feed, desde la fila del artesano en el
  Detalle, y (para el propio artesano) desde un ícono nuevo en el encabezado de
  `HomeArtesanoScreen`.
- **Perfil del cliente y Likes** (`ClientProfileScreen`, `LikesScreen`): el perfil del
  cliente es un menú (información básica — solo `nombre`, editable en un diálogo; mis
  likes; historial de compras y configuración de la cuenta como placeholders; cerrar
  sesión) sin avatar real — no existe ningún campo de foto de perfil ni flujo de subida,
  así que siempre se muestra un ícono por defecto. "Mis likes" lista los productos que el
  usuario marcó con like: primero `obtenerProductosLikeadosPorUsuario` (ids, vía
  `collectionGroup`), luego `obtenerProductosPorIds` (en bloques de 30 con
  `whereIn(FieldPath.documentId(), ...)`) para traerlos de una vez en vez de una lectura
  por producto. "Cerrar sesión" llama a `AuthRepository.cerrarSesion()` y navega a
  `Routes.WELCOME` limpiando todo el back stack (`popUpTo(0) { inclusive = true }`).
- **Insignia RUAC — solicitud y verificación (Fase 5)**: en su propio perfil, un artesano
  sin RUAC verificado ve "Agregar RUAC" (o "Volver a solicitar RUAC" si fue rechazado);
  abre `RequestRuacScreen` (número de registro, región, técnica y un documento/foto
  seleccionado con `OpenDocument()` — el mismo selector que ya usaba el RUAC del registro,
  no `ImageUploadBox`, porque el documento puede ser PDF o imagen). Al enviar,
  `AuthRepository.enviarSolicitudRuac` sube el archivo a `ruac/{uid}/documento_ruac` y deja
  `ruacEstado = "pendiente"`; mientras está así, el perfil muestra un chip no clickeable
  "RUAC en revisión" en vez del botón. `ArtisanProfileScreen` se refresca solo al volver a
  primer plano (mismo patrón `ON_RESUME` que el Panel de Publicaciones), así que el nuevo
  estado se ve sin reabrir la app.

  Un administrador (rol nuevo, ver más abajo) entra directo a `AdminRuacListScreen` al
  iniciar sesión — es su único destino, no tiene Home propio. Ahí ve todos los artesanos
  con `ruacEstado == "pendiente"` (una sola consulta de igualdad, ordenada por fecha de
  solicitud en memoria para no depender de un índice compuesto — mismo criterio que
  `obtenerTodosLosProductosActivos`); tap en una fila abre `AdminRuacReviewScreen`, con los
  datos declarados y el documento de soporte. **Aprobar** deja `ruacEstado = "verificado"`
  y llena `ruacVerificadoEn`/`ruacUltimaValidacion`/`ruacRevisadoPor`; **Rechazar** deja
  `ruacEstado = "rechazado"` (el artesano puede volver a solicitar). Ninguno de los dos
  pide confirmación aparte — mismo criterio que "Pausar/Activar publicación": cambia un
  estado, no borra nada, se puede volver a revisar. Con `ruacEstado == "verificado"`, el
  chip "Con registro RUAC" se enciende en `ArtisanProfileScreen` y en la fila del artesano
  dentro de `ProductDetailScreen`, y `RuacBadgeScreen` (antes un placeholder) muestra los
  datos reales; si alguien entra a esa ruta sin estar verificado, ve un estado vacío en vez
  de datos incompletos.

  **Registro de administrador**: desde un enlace discreto al final de `RegisterScreen`
  ("¿Eres administrador?"), `AdminRegisterScreen` pide correo, contraseña y un PIN. No pasa
  por el código de verificación de correo de 6 dígitos (a diferencia de cliente/artesano) —
  ver la advertencia de seguridad más abajo para la razón exacta y el trade-off aceptado.
- **Registrar venta, Valorar compra, Calidad y confianza (Fase 6)**: desde el menú de tres
  puntos de una fila en `MyProductsScreen`, "Registrar venta" abre `RegisterSaleScreen`
  con el producto ya elegido (de solo lectura); busca el cliente en memoria por
  nombre/correo (`AuthRepository.obtenerClientes` + `normalizar()`, mismo criterio que los
  demás buscadores del proyecto) entre los usuarios con `rol == "cliente"`, elige una
  fecha (por defecto hoy) y confirma → `SaleRepository.registrarVenta`.

  El cliente ve esa venta en `PurchaseHistoryScreen` (reemplaza el placeholder de
  "Historial de compras") como "Pendiente de valorar"; al abrirla, `RateSaleScreen` valida
  que la venta sea suya y que no esté valorada ya (si no, redirige a
  `ProductDetailScreen` en vez de mostrar el formulario) y pide Calidad y Confianza como
  dos grillas de 4 pills de selección única — **sin ningún campo de texto**. Al enviar,
  `SaleRepository.enviarValoracion` hace, en una sola transacción: crea
  `valoraciones/{ventaId}`, marca `ventas/{ventaId}.valorado = true`, y recalcula la
  calidad del producto y la confianza del artesano (`artesanoStats`).

  `QualityTrustScreen` (desde el bloque de calidad/confianza en `ProductDetailScreen`, o
  desde el indicador de confianza en `ArtisanProfileScreen` — en ese caso, como la
  confianza es del artesano completo y la ruta pide un `productId`, se usa el producto más
  reciente del artesano solo para tener un id válido; la sección de Confianza es correcta
  igual, no depende de cuál producto se pasó) muestra, por cada una: "Verificada por N
  compradores", una barra de nivel (`promedio / 4 * 100%`) y "Nivel: `<etiqueta>`" — o un
  estado neutro "Aún no hay valoraciones" si el conteo es 0.

  **Corrección de modelo importante**: hasta este ajuste, "confianza" vivía por error en
  `Product.confianzaPromedio` (nunca se calculó). La confianza es del **artesano**, no del
  producto — ahora vive en `artesanoStats/{artesanoId}` y se lee así en
  `ProductoFeedCard`, `ProductDetailScreen` y `ArtisanProfileScreen`. La calidad sí sigue
  siendo por producto (`Product.calidadPromedio`), sin cambios de fondo, solo se conectó a
  datos reales.

- **Notificaciones (campana)** (ajuste posterior a la Fase 6, reemplaza la limitación
  "pull, no push" y generaliza el push que antes solo cubría ventas nuevas): un ícono de
  campana (`ui/components/IconoNotificaciones.kt`) en el encabezado de
  `HomeClienteScreen` y `HomeArtesanoScreen` muestra un badge con el número de
  notificaciones sin leer y abre `NotificationsScreen` (`Routes.NOTIFICATIONS`). Cuatro
  eventos generan una notificación — ver el detalle de campos y triggers en "Firestore —
  colección `notificaciones/{autoId}`" (Modelo de datos):

  - **Venta registrada** → avisa al **cliente** (ya existía desde la Fase 6, ahora pasa
    por la misma colección/helper que los otros tres).
  - **Valoración recibida** → avisa al **artesano** (antes no tenía ningún aviso de esto).
  - **Me gusta nuevo** en uno de sus productos → avisa al **artesano**, salvo que se dé
    like a sí mismo.
  - **RUAC aprobado/rechazado** → avisa al **artesano**.

  Las cuatro las escriben Cloud Functions (nunca la app — ver la nota de "solo
  create: if false" en el modelo de datos), a través del helper compartido `avisar()` en
  `functions/src/index.ts`, que crea el documento en `notificaciones` y, si
  `usuarios/{destinatarioId}.fcmToken` existe, envía además el push de FCM **solo con
  `data`** (sin bloque `notification`), para que `MinkaMessagingService.onMessageReceived`
  se dispare siempre —con la app en primer plano, en segundo plano o cerrada— y construya
  la notificación del lado de la app de forma consistente (canal `"minka_avisos"`,
  "Avisos de MinkaArt" — antes se llamaba `"ventas_pendientes"`, ya no exclusivo de
  ventas; efecto secundario aceptado: a quien ya tenía la app instalada le queda el canal
  viejo huérfano en los ajustes del sistema hasta que reinstale).

  El token de FCM se guarda/actualiza en `usuarios/{uid}.fcmToken` desde
  `EfectoNotificaciones` (pide el permiso `POST_NOTIFICATIONS` en Android 13+, obtiene el
  token de `FirebaseMessaging.getInstance().token`), llamado desde `HomeClienteScreen` y
  `HomeArtesanoScreen`, y también desde `MinkaMessagingService.onNewToken` si el token
  rota. Tocar el push abre la pantalla correspondiente directamente: `MainActivity`
  (`android:launchMode="singleTop"`) lee los extras `tipo`/`referenciaId` del `Intent` (en
  `onCreate` y en `onNewIntent`, según si la app ya estaba abierta) y se los pasa a
  `AppNavigation`, que resuelve la ruta con `Routes.rutaDeNotificacion(tipo,
  referenciaId)` — la misma tabla que usa `NotificationsScreen` al tocar una fila, para no
  repetirla en dos sitios — y navega una sola vez por cada par nuevo. Tocar una fila
  dentro de `NotificationsScreen` hace lo mismo, además de marcar esa notificación como
  leída (`NotificationRepository.marcarComoLeida`); "Marcar todas" hace lo mismo en lote
  sobre todas las no leídas. El badge de la campana se refresca al volver a primer plano
  (mismo patrón `ON_RESUME` que ya usan `MyProductsScreen`/`ArtisanProfileScreen`), así se
  ve al día sin reabrir la app. **Límite aceptado**: si la sesión de Firebase Auth ya
  había expirado al tocar el push, el deep link falla silenciosamente — no hay un flujo
  de "inicia sesión y continúa" para este caso.

## Diseño y marca

Paleta de marca definida en `app/src/main/java/com/minkaart/app/ui/theme/Color.kt` y
mapeada a roles de Material 3 en `Theme.kt`:

| Token | Hex | Uso |
|---|---|---|
| `Guinda` | `#3B0F1A` | Vinotinto profundo — fondos oscuros, texto principal |
| `Borgona` | `#66172E` | Marca, botones, degradados |
| `AzulClaro` | `#B7C7D9` | Detalles, fondos suaves |
| `AzulAcero` | `#55708E` | Acentos fríos, degradado de botones |
| `Marfil` | `#F2EDE6` | Blanco cálido — fondo principal, texto sobre oscuro |

Es el brand kit v2; solo hay 5 tokens para los ~15 roles de `lightColorScheme` de
Material 3, así que `Theme.kt` reutiliza tokens entre varios roles (ver comentarios en el
archivo). `MinkaArt_btaTheme` sigue forzando `darkTheme = false` a propósito: las
pantallas no se han revisado en modo oscuro. El color dinámico (Material You) sigue
desactivado deliberadamente para no reemplazar la paleta de marca por los colores del
fondo de pantalla del usuario.

`RolePillToggle.kt` (Registro) ahora acepta 5 parámetros de color opcionales, con los
valores originales como default (cero cambio de comportamiento ahí) — el toggle de
cambio de modo de `HomeArtesanoScreen`/`HomeClienteScreen` lo reutiliza pasándole la
misma pareja de colores que ya usan los chips de categoría en todo el proyecto (activo:
fondo `Borgona` + texto `Marfil`; inactivo: fondo blanco + `TextoSecundario`), en vez de
duplicar el patrón de "pills" con un componente nuevo.

El bloque de Calidad/Confianza (`ui/components/BarraValoracion.kt`, ajuste posterior a la
Fase 6) reemplazó a `ConfianzaDots` (4 puntos sólidos): ahora es una barra con degradado
`Guinda → Borgona → AzulAcero` sobre una pista gris, con la etiqueta y el conteo de
verificaciones ("N verificaciones" / "Sin verificaciones") arriba — usado en
`ProductDetailScreen` y `ArtisanProfileScreen`; `ProductoFeedCard` usa la variante
compacta `BarraValoracionCompacta` (solo la barra, sin texto, para no hacer crecer la
tarjeta). `QualityTrustScreen` queda fuera de este rediseño a propósito y conserva su
barra plana + "Nivel: `<etiqueta>`".

`Type.kt` usa **Fraunces** (titulares, saludos, logotipo) y **Plus Jakarta Sans**
(interfaz) como fuentes reales, no como sustitutos temporales. Google Fonts no distribuye
ninguna de las dos como archivos estáticos por peso, así que cada `FontFamily` carga un
único archivo variable (`res/font/fraunces_variable.ttf`,
`res/font/plus_jakarta_sans_variable.ttf`) varias veces, una por peso, usando
`FontVariation.Settings` sobre el eje `wght` (API experimental de Compose,
`@OptIn(ExperimentalTextApi::class)` en el propio archivo). Fraunces fija además el eje
óptico (`opsz`) en un valor alto porque solo se usa en titulares.

Las pantallas de Entrada, Login y Registro usan además el patrón textil
`res/drawable/bg_pattern_tejido.jpg` como fondo decorativo, superpuesto a un degradado
diagonal de marca (`ui/components/PatternBackground.kt`).

## Configuración y ejecución

**Requisitos**: JDK 17+, Android Studio reciente (o el SDK de Android por línea de
comandos), un dispositivo o emulador con API 31 o superior. `app/google-services.json`
ya está incluido en el repositorio, no hace falta generarlo para compilar.

Comandos (PowerShell / cmd, desde la raíz del proyecto):

```powershell
.\gradlew.bat assembleDebug          # compilar el APK de debug
.\gradlew.bat installDebug            # instalar en un dispositivo/emulador conectado
.\gradlew.bat test                    # tests unitarios (JVM)
.\gradlew.bat connectedAndroidTest     # tests instrumentados (requiere dispositivo/emulador)
```

En Linux/macOS, usar `./gradlew` en lugar de `.\gradlew.bat`.

**Cloud Functions** (`functions/`, Node.js + TypeScript): requieren el CLI de Firebase
(`npm install -g firebase-tools`) y, una sola vez, una cuenta de Gmail con verificación en
dos pasos y una [contraseña de aplicación](https://myaccount.google.com/apppasswords),
registrada como par de secretos:

```powershell
firebase functions:secrets:set GMAIL_USER
firebase functions:secrets:set GMAIL_APP_PASSWORD
```

Para compilar y desplegar:

```powershell
cd functions
npm install
npm run build
firebase deploy --only functions
```

Si el deploy falla con `Cannot determine backend specification. Timeout after 10000`, no es
un error del código: el análisis inicial de las funciones tiene un límite de 10 segundos que
una máquina Windows en frío (antivirus escaneando `node_modules`) supera con facilidad. Se
resuelve ampliando el límite antes de desplegar:

```powershell
$env:FUNCTIONS_DISCOVERY_TIMEOUT=120      # en cmd.exe: set FUNCTIONS_DISCOVERY_TIMEOUT=120
firebase deploy --only functions
```

Detalle de la configuración en `functions/SETUP.md`.

## Estado actual y pendientes

**Implementado**
- Registro de cliente y artesano (con validaciones), incluido el flujo de RUAC.
- Login con Firebase Auth y enrutado según rol.
- Publicación de una artesanía con imagen, historia y datos comerciales, con el diseño del
  brand kit v2 (Fase 2).
- Home del artesano con saludo por nombre y vista previa de sus productos (Fase 2).
- Panel de Publicaciones para el artesano autenticado: listar, editar y eliminar (con
  confirmación) sus productos (Fase 2).
- Catálogo del cliente (`HomeClienteScreen`): feed de productos de todos los artesanos,
  búsqueda visual, chips de categoría, like con contador real, y Detalle de producto con
  datos del artesano y contacto por WhatsApp (Fase 3).
- Perfil del artesano (editable por su dueño: bio y contacto público), perfil del cliente
  (información básica, likes, cerrar sesión) y pantalla de Likes real (Fase 4).
- Toda la app respeta la barra de estado y la barra de navegación del sistema
  (`Modifier.windowInsetsPadding(WindowInsets.safeDrawing)` en el contenedor de contenido
  de cada pantalla) — es la convención del proyecto de aquí en adelante para cualquier
  pantalla nueva con su propia barra superior o inferior (Fase 4).
- Identidad visual del brand kit v2 (paleta guinda/borgoña/azul, Fraunces + Plus Jakarta
  Sans, patrón textil) aplicada en Entrada, Login, Registro (Fase 1), Home Artesano, Panel
  de Publicaciones, Publicar y Editar producto (Fase 2), Home Cliente y Detalle de
  producto (Fase 3), y Perfil del artesano (Fase 4) — `ClientProfileScreen`/`LikesScreen`
  usan la misma paleta con fondo claro; solo `ProfileScreen` (sin usar) sigue siendo un
  placeholder sin estilo propio.
- Verificación de correo obligatoria en el registro, por código de 6 dígitos generado y
  validado por Cloud Functions (no por el mecanismo nativo de Firebase Auth): la cuenta
  de Firebase Auth y el documento en `usuarios` solo se crean **después** de verificar
  el código (`RegisterViewModel.iniciarRegistro` + `confirmarCodigoDeVerificacion`,
  `AuthRepository.enviarCodigoVerificacion` / `verificarCodigo` sobre las Cloud
  Functions `enviarCodigoVerificacion` / `verificarCodigo`).
- El artesano puede pasar a "modo cliente" con el toggle del encabezado de su Home
  (mismo feed que ve un cliente, con el indicador persistente para volver — ver "Cambio
  de modo artesano/cliente" en Flujos principales), y busca dentro de su propio
  catálogo con chips de categoría + texto en el Panel de Publicaciones — misma lógica de
  filtro que `HomeClienteScreen`, compartida vía `util/Texto.kt` (Ajustes 2).
  `LikesScreen` ya abre sin `PERMISSION_DENIED`: hacía falta una regla de Firestore
  aparte para la consulta `collectionGroup("likes")` (ver la colección
  `productos/{autoId}/likes/{uid}` más arriba para el detalle exacto).
- Panel de Estadísticas en `HomeArtesanoScreen` (likes/vistas totales, activos/total,
  producto más visto y con más likes) y conteo real de `vistas` al abrir
  `ProductDetailScreen` — ver "Cambio de modo artesano/cliente" y "Detalle y contacto"
  en Flujos principales.
- El RUAC ahora sí se verifica de verdad (Fase 5): solicitud del artesano
  (`RequestRuacScreen`), cola de revisión y aprobación/rechazo por un tercer rol,
  `"administrador"` (`AdminRuacListScreen`/`AdminRuacReviewScreen`), e insignia real
  (`RuacBadgeScreen`) — ver "Insignia RUAC" en Flujos principales y la nota de migración
  en el modelo de datos de `usuarios`. Un RUAC ya verificado se puede editar (vuelve a
  quedar pendiente de revisión) o eliminar (el artesano lo retira sin pasar por revisión) —
  ver "RUAC editable/eliminable" en el modelo de `usuarios`.
- El formulario de Publicar/Editar producto reemplazó "Región de origen" y "Categoría" en
  texto libre por selectores estructurados: Departamento + Municipio, una categoría curada
  de 16 opciones (con "Otros" pidiendo texto libre aparte) y una Clasificación opcional —
  ver `CatalogosProducto` y la nota de migración de `regionOrigen` en el modelo de
  `productos`.
- Registrar venta, Valorar compra y Calidad y confianza (Fase 6): el artesano registra una
  venta a un cliente real, el cliente la valora con pills (sin texto libre) y eso alimenta
  la calidad real de cada producto y la confianza real de cada artesano — ver "Registrar
  venta, Valorar compra, Calidad y confianza" en Flujos principales. Corrige además un
  error de modelo de fases anteriores: la confianza vivía por error en
  `Product.confianzaPromedio`, ahora vive en `artesanoStats/{artesanoId}` (es del
  artesano, no del producto).
- El cliente recibe una notificación push del sistema cuando un artesano le registra una
  venta, con deep link directo a `RateSaleScreen` de esa venta. De paso se corrigió un
  `PERMISSION_DENIED` real al enviar una valoración (ver la nota en el modelo de
  `productos`/`ventas`/`valoraciones`).
- Campana de notificaciones (ajuste posterior a la Fase 6) en el encabezado de
  `HomeClienteScreen`/`HomeArtesanoScreen`, con badge de no leídas y centro de
  notificaciones real (`NotificationsScreen`): venta registrada, valoración recibida, me
  gusta nuevo y RUAC aprobado/rechazado generan un aviso — ver "Notificaciones (campana)"
  en Flujos principales.
- Rediseño de Calidad y Confianza (ajuste posterior a la Fase 6): barras con degradado de
  marca y conteo de verificaciones (`BarraValoracion`/`BarraValoracionCompacta`)
  reemplazan a los 4 puntos de `ConfianzaDots` en Detalle de producto, Perfil del artesano
  y la tarjeta del feed — ver "Diseño y marca".

**Pendiente / placeholder**
- `ProfileScreen.kt` sigue siendo un `Text()` de una línea — ya no lo usa ninguna
  pantalla (`ARTISAN_PROFILE`/`CLIENT_PROFILE` lo reemplazaron), se dejó sin tocar ni
  eliminar por quedar fuera del alcance de la Fase 4.
- `Routes.RECOGNITION`, `Routes.RUAC_BADGE`, `Routes.PURCHASE_HISTORY` y
  `Routes.ACCOUNT_SETTINGS` están registradas y navegables, pero apuntan a un
  `PlaceholderScreen` genérico — el reconocimiento visual (OpenCV), la insignia RUAC
  real, el historial de compras y la configuración de la cuenta son de fases futuras.
- No existe ningún campo de foto de perfil (`fotoUrl` o similar) ni flujo de subida de
  imagen de perfil — el avatar de `ClientProfileScreen` y `ArtisanProfileScreen` siempre
  muestra un ícono por defecto.
- `QualityTrustScreen` no se actualizó al rediseño de Calidad/Confianza (decisión de
  alcance): sigue con su barra plana + "Nivel: `<etiqueta>`", no usa `BarraValoracion`.
- El canal de notificaciones se renombró de `"ventas_pendientes"` a `"minka_avisos"` al
  generalizarlo a los cuatro tipos de aviso; quien ya tenía la app instalada conserva el
  canal viejo huérfano en los ajustes del sistema hasta que reinstale.
- "Información básica" del cliente solo permite editar `nombre` — el correo está atado a
  la identidad de Firebase Auth y cambiarlo requeriría un flujo de reautenticación, fuera
  de alcance.
- Tras un registro exitoso, `RegisterScreen` solo muestra un `Toast`: no navega a login
  ni a ninguna otra pantalla.
- `ProductRepository.guardarProducto()` (variante sin imagen) existe pero no lo invoca
  ninguna pantalla; solo se usa `guardarProductoConImagen()`.
- El modelo `Product.imagenes` es una lista, pero el flujo actual solo guarda una imagen
  (`actualizarProducto` sigue esa misma convención: reemplaza la lista completa por una
  sola URL cuando se sube una imagen nueva).
- `ProductRepository.eliminarProducto()` borra el documento en Firestore pero no la imagen
  asociada en Storage — queda huérfana. No se pidió limpiarla y ninguna otra función del
  proyecto lo hace tampoco hoy.
- El municipio de origen sigue siendo texto libre (no hay una lista completa de los ~1100
  municipios de Colombia dentro de la app); el departamento y la categoría ya son
  selectores de una lista fija (`CatalogosProducto`) desde el ajuste posterior a la Fase 5.
- El deep link de la notificación de venta nueva no encadena un login si la sesión de
  Firebase Auth ya había expirado al tocar la notificación — ver "Notificación push de
  venta nueva" en Flujos principales.
- Las reglas de Firestore que abren la lectura de `usuarios/{uid}` a cualquier usuario
  autenticado (ver Modelo de datos) siguen con el mismo trade-off de privacidad aceptado
  en la Fase 3.
- **Advertencia de seguridad del PIN de administrador (Fase 5)**: el proyecto no usa
  Cloud Functions, así que `AuthRepository.registrarAdministrador` valida el PIN
  comparándolo del lado del cliente contra `configuracion/adminAccess.pin` en Firestore.
  Es razonable para el alcance de esta tesis/MVP, pero **no es a prueba de un atacante
  decidido**: la regla que permite leer ese documento (`allow get: if request.auth !=
  null`) lo deja legible por cualquier usuario autenticado, no solo durante el registro de
  administrador — alguien con conocimientos técnicos podría leer el PIN directamente o
  manipular la app para saltarse la comparación. La forma "correcta" sería validar el PIN
  con una Cloud Function (que nunca expone el secreto al cliente), pero el proyecto no las
  usa para esto todavía. Tampoco hay ninguna migración de los `tieneRuac == true` que ya
  existieran antes de esta fase (ver la nota de migración en el modelo de `usuarios`).
  El documento `configuracion/adminAccess` no lo crea ninguna pantalla — hay que crearlo a
  mano en Firebase Console antes de poder registrar la primera cuenta de administrador.
- Un correo que ya tiene cuenta en Firebase Auth solo se detecta al final del registro,
  al llamar a `createUserWithEmailAndPassword` después de verificar el código (la Cloud
  Function `enviarCodigoVerificacion` no comprueba si el correo ya está registrado).
- El correo de verificación se envía por el SMTP de Gmail (remitente `@gmail.com`), con el
  límite de envío propio de Gmail de aproximadamente 500 correos/día; suficiente para
  desarrollo y pruebas, pero no para producción a gran escala. Si se necesita un remitente
  con dominio propio y sin ese límite, hay que verificar un dominio en un proveedor
  transaccional (p. ej. Resend).
- La cuenta de Firebase Auth creada al final del registro queda con `emailVerified =
  false`, porque la verificación la hace la Cloud Function contra Firestore, no el
  mecanismo nativo de Firebase Auth. `isEmailVerified` no se comprueba en ningún punto
  de la app (login, publicación, etc.), así que hoy esto no bloquea nada, pero conviene
  tenerlo presente si en el futuro se agrega alguna comprobación basada en ese campo.
- No hay tests reales: solo quedan las plantillas por defecto de Android Studio
  (`ExampleUnitTest.kt`, `ExampleInstrumentedTest.kt`).
