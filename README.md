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
│   ├── AuthRepository.kt     # login, rol de usuario, recuperar contraseña, datos del usuario actual
│   └── ProductRepository.kt  # guardar/editar/eliminar producto (con/sin imagen), listar "mis productos", consultar uno por id
├── viewModel/                 # ¡ojo con mayúscula/minúscula, ver más abajo!
│   ├── HomeArtesanoViewModel.kt
│   ├── LoginViewModel.kt
│   ├── ProductViewModel.kt
│   └── RegisterViewModel.kt
└── ui/
    ├── components/                # Composables reutilizables del brand kit v2
    │   ├── PatternBackground.kt   # degradado + patrón textil, con blur opcional
    │   ├── GlassCard.kt           # tarjeta "de vidrio esmerilado"
    │   ├── GlassTextField.kt      # campo con ícono y línea inferior (fondo oscuro)
    │   ├── GradientButton.kt      # botón con degradado + variante outline
    │   ├── RolePillToggle.kt      # selector Artesano/Cliente en pills
    │   ├── ImageUploadBox.kt      # caja de selección/preview de imagen (Publicar/Editar)
    │   ├── MinkaFormField.kt      # campo con etiqueta + caja (fondo claro, artesano)
    │   ├── PlaceholderScreen.kt   # relleno para rutas de fases futuras (recognition, likes, RUAC)
    │   └── ConfianzaDots.kt       # fila de 4 puntos (calidad/confianza), gris si no hay dato
    ├── home/
    │   ├── HomeArtesanoScreen.kt
    │   └── HomeClienteScreen.kt   # catálogo del cliente: búsqueda, chips, feed, bottom nav
    ├── login/LoginScreen.kt
    ├── product/ProductDetailScreen.kt  # detalle, artesano, calidad/confianza, contacto WhatsApp
    ├── profile/ProfileScreen.kt   # placeholder
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
| `PROFILE` | `profile` | `ProfileScreen` (placeholder) | Sí | `HomeClienteScreen` (bottom nav "Perfil") |
| `LIKES` | `likes` | `PlaceholderScreen` | Sí | `HomeClienteScreen` (bottom nav "Likes") |
| `RUAC_BADGE` | `ruac_badge/{artesanoId}` | `PlaceholderScreen` | Sí, con argumento de ruta | `ProductDetailScreen` (tap en la fila del artesano) |
| `RECOGNITION` | `recognition` | `PlaceholderScreen` | Sí | `HomeClienteScreen` (ícono de la barra de búsqueda) |

## Modelo de datos

### Firestore — colección `usuarios/{uid}`

Definido en `app/src/main/java/com/minkaart/app/data/model/User.kt`. El documento id es
el `uid` de Firebase Auth.

| Campo | Tipo | Notas |
|---|---|---|
| `uid` | `String` | |
| `nombre` | `String` | |
| `correo` | `String` | |
| `rol` | `String` | `"cliente"` o `"artesano"` (se compara en minúsculas al hacer login) |
| `telefono` | `String` | Solo artesano |
| `fechaNacimiento` | `String` | Solo artesano, formato `dd/MM/yyyy` |
| `region` | `String` | Solo artesano |
| `productosQueVende` | `String` | Solo artesano, texto libre |
| `categoriasArtesanales` | `List<String>` | Solo artesano; se captura como texto separado por comas y se parte en el registro |
| `tieneRuac` | `Boolean` | Solo artesano |
| `numeroRuac` | `String` | Solo si `tieneRuac == true` |
| `ruacUrl` | `String` | URL de descarga en Storage, solo si `tieneRuac == true` |

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
  tarjeta:
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
- **Catálogo del cliente** (`HomeClienteScreen` → `HomeClienteViewModel`): barra de
  búsqueda (el ícono de cámara navega a `Routes.RECOGNITION`, todavía un
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
  reutiliza `Routes.PROFILE` para "Perfil" y navega a `Routes.LIKES` (placeholder) desde
  "Likes"; "Buscar" no navega a ningún lado, solo hay una barra de búsqueda en esta misma
  pantalla.
- **Detalle y contacto** (`ProductDetailScreen` → `ProductDetailViewModel`): encadena dos
  lecturas (`obtenerProductoPorId` y, con el `artesanoId` del resultado,
  `obtenerUsuarioPorId`) porque Firestore no tiene joins. Muestra nombre, precio,
  categoría, descripción, historia y región del producto, y una fila con el artesano
  (nombre, teléfono, insignia RUAC en miniatura si `tieneRuac`) que navega a
  `Routes.ruacBadgeRoute(artesanoId)` (placeholder). Dos filas de `ConfianzaDots` para
  calidad y confianza. El botón "Contactar artesano" limpia el teléfono a solo dígitos y
  abre `https://wa.me/<numero>?text=<mensaje>` con un intent `ACTION_VIEW`; si no hay
  ninguna app que lo resuelva, cae a `ACTION_DIAL` con el número tal cual.

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
- Identidad visual del brand kit v2 (paleta guinda/borgoña/azul, Fraunces + Plus Jakarta
  Sans, patrón textil) aplicada en Entrada, Login, Registro (Fase 1), Home Artesano, Panel
  de Publicaciones, Publicar y Editar producto (Fase 2), y Home Cliente y Detalle de
  producto (Fase 3) — solo `ProfileScreen` sigue siendo un placeholder sin estilo propio.
- Verificación de correo obligatoria en el registro, por código de 6 dígitos generado y
  validado por Cloud Functions (no por el mecanismo nativo de Firebase Auth): la cuenta
  de Firebase Auth y el documento en `usuarios` solo se crean **después** de verificar
  el código (`RegisterViewModel.iniciarRegistro` + `confirmarCodigoDeVerificacion`,
  `AuthRepository.enviarCodigoVerificacion` / `verificarCodigo` sobre las Cloud
  Functions `enviarCodigoVerificacion` / `verificarCodigo`).

**Pendiente / placeholder**
- `ProfileScreen.kt` sigue siendo un `Text()` de una línea, sin diseño ni datos reales —
  el bottom nav del cliente ya navega hacia ella, pero la pantalla en sí es de una fase
  futura.
- `Routes.RECOGNITION`, `Routes.LIKES` y `Routes.RUAC_BADGE` están registradas y navegables
  desde `HomeClienteScreen`/`ProductDetailScreen`, pero apuntan a un `PlaceholderScreen`
  genérico — el reconocimiento visual (OpenCV), la lista de likes y la insignia RUAC real
  son de fases futuras.
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
- Las reglas de Firestore permiten leer el documento completo de `usuarios/{uid}` a
  cualquier usuario autenticado (antes solo el propio uid podía leerlo) — necesario para
  que el cliente vea nombre/teléfono/RUAC del artesano en el Detalle de producto. Efecto
  secundario aceptado: campos no pensados para mostrarse (correo, fecha de nacimiento,
  número de RUAC) también quedan visibles para cualquier usuario con sesión; no existe un
  documento de "perfil público" separado.
- No hay pantalla de "Perfil del cliente" ni "Perfil del artesano" reales — ambas
  reutilizan hoy el mismo `ProfileScreen.kt` placeholder.
- El RUAC se recibe y se guarda, pero no hay ninguna verificación real del documento ni
  del número.
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
