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
| Backend | `functions/src/index.ts` | Cloud Functions `onCall` (Node.js + TypeScript): generan, guardan y validan el código de verificación, y lo envían por correo con `nodemailer` sobre el SMTP de Gmail. |

## Estructura del proyecto

> ⚠️ El paquete Kotlin es `com.minkaart2.app`, pero las carpetas físicas en disco son
> `com/minkaart/app/` (sin el `2`). Es intencional en el sentido de que compila así, pero
> es fácil de pasar por alto: si buscas código por ruta de carpeta y no aparece, revisa
> también por nombre de paquete.

```
app/src/main/java/com/minkaart/app/
├── MainActivity.kt
├── data/model/
│   ├── Product.kt          # documento de la colección "productos"
│   └── User.kt              # documento de la colección "usuarios"
├── navigation/
│   ├── AppNavigation.kt      # NavHost con todas las rutas registradas
│   └── Routes.kt             # constantes de ruta (String)
├── repository/
│   ├── AuthRepository.kt     # login, rol de usuario, recuperar contraseña, datos de usuarios, actualizar campos, cerrar sesión
│   └── ProductRepository.kt  # guardar/editar/eliminar producto, consultas por id/artesano/lote de ids, like con transacción
├── viewModel/                 # ¡ojo con mayúscula/minúscula, ver más abajo!
│   ├── ArtisanProfileViewModel.kt
│   ├── ClientProfileViewModel.kt
│   ├── HomeArtesanoViewModel.kt
│   ├── LikesViewModel.kt
│   ├── LoginViewModel.kt
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
    ├── components/                # Composables reutilizables del brand kit v2
    │   ├── PatternBackground.kt   # degradado + patrón textil, con blur opcional
    │   ├── GlassCard.kt           # tarjeta "de vidrio esmerilado"
    │   ├── GlassTextField.kt      # campo con ícono y línea inferior (fondo oscuro)
    │   ├── GradientButton.kt      # botón con degradado + variante outline
    │   ├── RolePillToggle.kt      # selector Artesano/Cliente en pills
    │   ├── ImageUploadBox.kt      # caja de selección/preview de imagen (Publicar/Editar)
    │   ├── MinkaFormField.kt      # campo con etiqueta + caja (fondo claro, artesano)
    │   ├── PlaceholderScreen.kt   # relleno para rutas de fases futuras (recognition, RUAC, etc.)
    │   ├── ConfianzaDots.kt       # fila de 4 puntos (calidad/confianza), gris si no hay dato
    │   ├── ProductoFeedCard.kt    # tarjeta de producto (feed del cliente y perfil del artesano)
    │   └── BottomNavBar.kt        # barra inferior (Home cliente y Home artesano)
    ├── home/
    │   ├── HomeArtesanoScreen.kt
    │   └── HomeClienteScreen.kt   # catálogo del cliente: búsqueda, chips, feed, bottom nav
    ├── login/LoginScreen.kt
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
├── src/index.ts             # Cloud Functions onCall: enviarCodigoVerificacion, verificarCodigo
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
| `HOME_CLIENTE` | `home_cliente` | `HomeClienteScreen` (catálogo real) | Sí | `LoginScreen` (rol `cliente`) |
| `HOME_ARTESANO` | `home_artesano` | `HomeArtesanoScreen` | Sí | `LoginScreen` (rol `artesano`) |
| `MY_PRODUCTS` | `my_products` | `MyProductsScreen` (Panel de Publicaciones) | Sí | `HomeArtesanoScreen` |
| `PUBLICATION` | `publication` | `PublicationScreen` | Sí | `HomeArtesanoScreen`, `MyProductsScreen` (chip "Nueva") |
| `EDIT_PRODUCT` | `edit_product/{productId}` | `EditProductScreen` | Sí, con argumento de ruta | `MyProductsScreen` (menú ⋮ → Editar) |
| `PRODUCT_DETAIL` | `product_detail/{productId}` | `ProductDetailScreen` | Sí, con argumento de ruta | `HomeClienteScreen` (tap en una tarjeta del feed) |
| `ARTISAN_PROFILE` | `artisan_profile/{artesanoId}` | `ArtisanProfileScreen` | Sí, con argumento de ruta | `HomeClienteScreen` (nombre del artesano en una tarjeta), `ProductDetailScreen` (fila del artesano), `HomeArtesanoScreen` (ícono de perfil, propio) |
| `CLIENT_PROFILE` | `client_profile` | `ClientProfileScreen` | Sí | `HomeClienteScreen` (bottom nav "Perfil") |
| `LIKES` | `likes` | `LikesScreen` | Sí | `HomeClienteScreen` (bottom nav "Likes"), `ClientProfileScreen` ("Mis likes") |
| `PURCHASE_HISTORY` | `purchase_history` | `PlaceholderScreen` | Sí | `ClientProfileScreen` ("Historial de compras") |
| `ACCOUNT_SETTINGS` | `account_settings` | `PlaceholderScreen` | Sí | `ClientProfileScreen` ("Configuración de la cuenta") |
| `PROFILE` | `profile` | `ProfileScreen` (placeholder) | Sí | Nadie — reemplazado por `ARTISAN_PROFILE`/`CLIENT_PROFILE`; queda sin usar |
| `RUAC_BADGE` | `ruac_badge/{artesanoId}` | `RuacBadgeScreen` (real desde Fase 5) | Sí, con argumento de ruta | `ArtisanProfileScreen` (chip de insignia RUAC verificado) |
| `RECOGNITION` | `recognition` | `PlaceholderScreen` | Sí | `HomeClienteScreen` (ícono de la barra de búsqueda) |
| `ADMIN_REGISTER` | `admin_register` | `AdminRegisterScreen` | Sí | `RegisterScreen` (enlace "¿Eres administrador?") |
| `ADMIN_RUAC_LIST` | `admin_ruac_list` | `AdminRuacListScreen` | Sí | `LoginScreen` (rol `administrador`) |
| `ADMIN_RUAC_REVIEW` | `admin_ruac_review/{artesanoId}` | `AdminRuacReviewScreen` | Sí, con argumento de ruta | `AdminRuacListScreen` (tap en una solicitud) |
| `REQUEST_RUAC` | `request_ruac` | `RequestRuacScreen` | Sí | `ArtisanProfileScreen` (botón "Agregar RUAC"/"Volver a solicitar RUAC", solo el propio artesano) |

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
| `regionOrigen` | `String` | |
| `imagenes` | `List<String>` | URLs de Storage; en la práctica el flujo actual solo guarda una |
| `vistas` | `Long` | Por defecto `0`; el campo existe pero todavía no hay ningún flujo (detalle de producto, vista del cliente) que lo incremente |
| `likes` | `Long` | Por defecto `0`. Se incrementa/decrementa con `ProductRepository.alternarLike`, en una transacción junto con `productos/{id}/likes/{uid}` (ver más abajo) |
| `activo` | `Boolean` | Por defecto `true`. El artesano lo cambia desde el Panel de Publicaciones (menú ⋮ → Pausar/Activar); el catálogo del cliente (`HomeClienteScreen`) solo muestra los productos con `activo == true` |
| `fechaPublicacion` | `Long` | Epoch millis, con el reloj del dispositivo (no `FieldValue.serverTimestamp()`); `0` en productos publicados antes de agregar este campo |
| `categoria` | `String` | Texto libre (ej. "Tejidos", "Cerámica"); alimenta los chips de filtro del catálogo de cliente. Puede quedar vacío |
| `calidadPromedio` | `Double?` | `null` hasta que exista la fase de "Valorar compra"; la UI lo trata como "sin valoraciones aún" en vez de inventar un valor |
| `confianzaPromedio` | `Double?` | Mismo caso que `calidadPromedio` |

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
| `productos/{artesanoId}/{productoId}/imagen_principal` | Foto principal de una artesanía |
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
  valida todos los campos y que haya una imagen seleccionada, sube la imagen a Storage,
  obtiene su `downloadUrl` y solo entonces escribe el producto en Firestore — el orden
  importa, si la subida falla no se crea el documento.
- **Gestión de productos** (`HomeArtesanoScreen` → `MyProductsScreen` "Panel de
  Publicaciones" → `EditProductScreen`): el Home del artesano saluda por nombre (vía
  `HomeArtesanoViewModel.cargarDatosArtesano` → `AuthRepository.obtenerUsuarioActual`) y
  muestra una vista previa de hasta 4 productos; el botón "Ver publicaciones" lleva al
  Panel, que lista todos los productos del artesano con un menú de tres puntos por
  tarjeta. El Home del artesano tiene su propio bottom nav (`BottomNavBar`, compartido
  con `HomeClienteScreen`): Inicio (la propia pantalla), **Explorar** → navega a
  `Routes.HOME_CLIENTE` (el artesano ve el mismo catálogo de todos los artesanos que ve
  un cliente, con búsqueda, chips, like y detalle — no hay una pantalla de catálogo
  separada para el rol artesano; dentro de esa pantalla "prestada" el ítem "Perfil" de su
  bottom nav sigue llevando a `Routes.CLIENT_PROFILE`, límite conocido y no resuelto),
  RUAC → `Routes.ruacBadgeRoute` (placeholder), Likes → `Routes.LIKES`, Perfil →
  `Routes.artisanProfileRoute` con el uid del propio artesano (mismo destino que el
  ícono de perfil del encabezado, que se dejó también por no estorbar):
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
    vistas · `{likes}` likes" (`vistas` siempre en 0 por ahora; `likes` sí sube cuando un
    cliente le da "me gusta" al producto, ver más abajo) y, si el producto tiene
    `fechaPublicacion`, "Publicado el dd/MM/yyyy".
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
    una su propia copia. `HomeArtesanoScreen` en sí (el saludo + vista previa de 4
    productos) no tiene buscador propio — se decidió ponerlo aquí, en el Panel, por ser la
    única pantalla del lado artesano con una lista real de productos sobre la que buscar.
- **Catálogo del cliente** (`HomeClienteScreen` → `HomeClienteViewModel`): barra de
  búsqueda real (el ícono de cámara navega a `Routes.RECOGNITION`, todavía un
  `PlaceholderScreen` — el reconocimiento visual con OpenCV es de otra fase) y chips de
  categoría armados dinámicamente a partir de las categorías distintas presentes en los
  productos cargados, no de una lista fija. El feed (`obtenerTodosLosProductosActivos`)
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
  con **AND** junto al chip de categoría activo — los dos filtros se aplican en memoria
  sobre los productos ya cargados, sin ninguna consulta nueva a Firestore por cada tecla:
  Firestore no tiene búsqueda de texto completo nativa y el tamaño del catálogo no
  justifica un servicio externo (Algolia, extensiones, etc.). El texto escrito se
  "asienta" 300ms después de la última tecla (`LaunchedEffect(textoBusqueda) { delay(300);
  ... }`) antes de recalcular el filtro, y tanto el texto escrito como
  `nombre`/`descripcion` de cada producto se normalizan (sin tildes, minúsculas, vía
  `java.text.Normalizer` del propio JDK) para que "ceramica" encuentre "Cerámica".
- **Detalle y contacto** (`ProductDetailScreen` → `ProductDetailViewModel`): encadena dos
  lecturas (`obtenerProductoPorId` y, con el `artesanoId` del resultado,
  `obtenerUsuarioPorId`) porque Firestore no tiene joins. Muestra nombre, precio,
  categoría, descripción, historia y región del producto, y una fila con el artesano
  (nombre, contacto, insignia RUAC en miniatura si `tieneRuac`) que navega a su
  `Routes.artisanProfileRoute(artesanoId)`. Dos filas de `ConfianzaDots` para calidad y
  confianza. El botón "Contactar artesano" usa `contactoPublico` (con `telefono` como
  respaldo si el artesano no lo configuró), limpia el número a solo dígitos y abre
  `https://wa.me/<numero>?text=<mensaje>` con un intent `ACTION_VIEW`; si no hay ninguna
  app que lo resuelva, cae a `ACTION_DIAL` con el número tal cual.
- **Perfil del artesano** (`ArtisanProfileScreen` → `ArtisanProfileViewModel`): cabecera
  con el mismo patrón + degradado de Entrada/Login (`PatternBackground`), avatar, chip de
  RUAC, bio y contacto público, estadísticas (cantidad de productos, suma de `likes`),
  `ConfianzaDots` sobre el promedio de los productos del artesano, y su grilla de
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
- El artesano puede explorar el catálogo general (mismo feed que ve un cliente) desde
  "Explorar" en su propio bottom nav, y busca dentro de su propio catálogo con chips de
  categoría + texto en el Panel de Publicaciones — misma lógica de filtro que
  `HomeClienteScreen`, compartida vía `util/Texto.kt` (Ajustes 2). `LikesScreen` ya abre
  sin `PERMISSION_DENIED`: hacía falta una regla de Firestore aparte para la consulta
  `collectionGroup("likes")` (ver la colección `productos/{autoId}/likes/{uid}` más
  arriba para el detalle exacto).
- El RUAC ahora sí se verifica de verdad (Fase 5): solicitud del artesano
  (`RequestRuacScreen`), cola de revisión y aprobación/rechazo por un tercer rol,
  `"administrador"` (`AdminRuacListScreen`/`AdminRuacReviewScreen`), e insignia real
  (`RuacBadgeScreen`) — ver "Insignia RUAC" en Flujos principales y la nota de migración
  en el modelo de datos de `usuarios`.

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
- La región de origen sigue siendo un campo de texto libre (en Publicar y Editar); el
  mockup la dibuja como un selector, pero no hay ninguna lista de regiones definida en el
  proyecto. Lo mismo aplica a `categoria`: es texto libre, sin una taxonomía fija.
- `vistas` se queda en 0: no hay pantalla que registre visitas a un producto. `likes` sí es
  real (el corazón del feed y del Detalle lo incrementan/decrementan de verdad).
- `calidadPromedio`/`confianzaPromedio` existen en el modelo pero nunca se calculan — la
  UI los muestra como "sin valoraciones aún" (puntos grises) hasta que exista la fase de
  "Valorar compra".
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
