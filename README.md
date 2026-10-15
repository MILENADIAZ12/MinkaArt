# MinkaArt

App Android nativa (Kotlin + Jetpack Compose) para un marketplace de artesanías
colombianas: conecta artesanos con compradores, poniendo en primer plano la historia y
el proceso de elaboración de cada pieza, no solo el producto.

## Contexto del producto

MinkaArt tiene dos tipos de usuario, definidos por el campo `rol` (`"cliente"` o
`"artesano"`) en el documento de usuario de Firestore:

- **Artesano**: se registra con datos adicionales (teléfono, fecha de nacimiento,
  departamentos donde trabaja —selección múltiple—, qué productos vende, categorías
  artesanales —selección múltiple—) y puede publicar artesanías con foto,
  precio, descripción, historia/proceso de elaboración y región de origen.
- **Cliente**: se registra con los datos básicos y consume el catálogo de todos los
  artesanos, con búsqueda, filtro por categoría, "me gusta" y detalle de producto con
  contacto por WhatsApp (ver [Flujos principales](#flujos-principales)).

Todo registro (cliente, artesano, administrador y Google) exige aceptar los **Términos y
Condiciones y la Política de Tratamiento de Datos Personales** (Ley 1581 de 2012, Decreto
1377 de 2013 y Ley 1480 de 2011) — ver "Términos y condiciones" en [Flujos
principales](#flujos-principales). La app está en **español (por defecto) e inglés**,
elegible en Configuración de la cuenta.

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
| Envío de correo | nodemailer (SMTP de Gmail, cuenta `minka.art.app@gmail.com`; secretos `GMAIL_USER`/`GMAIL_APP_PASSWORD`) en Cloud Functions; Resend (`RESEND_API_KEY`) queda como alternativa con `PROVEEDOR_CORREO` en `functions/src/index.ts` | 6.9.15 |
| Carga de imágenes | Coil (`coil-compose`) | 2.7.0 |
| Idioma por app (español / inglés) | `androidx.appcompat:appcompat` — solo para `AppCompatDelegate.setApplicationLocales` y `AppLocalesMetadataHolderService`; `MainActivity` extiende `AppCompatActivity` y el tema `Theme.MinkaArt_bta` pasó a heredar de `Theme.AppCompat.Light.NoActionBar` (AppCompat lo exige). La UI sigue siendo Compose | 1.7.0 |
| Reconocimiento visual — preprocesamiento en el dispositivo | OpenCV (`org.opencv:opencv`, Maven Central, sin ML Kit) — CLAHE para poca luz, detección de foto borrosa/muy oscura (varianza del Laplaciano / brillo medio), recorte manual antes de enviar la foto (`vision/PreprocesadorImagen.kt`) | 4.10.0 |
| Reconocimiento visual — embeddings de imagen (búsqueda real) | Google Cloud Vertex AI, modelo `multimodalembedding@001` (1408 dimensiones, ya normalizado a norma 1 — verificado con una llamada real), región `us-central1`, llamado desde Cloud Functions (nunca desde el dispositivo) | — |
| Reconocimiento visual — comparación (búsqueda real) | Firestore vector search (`Query.findNearest`, `DOT_PRODUCT`) sobre la colección `embeddingsVisuales`, con índice vectorial dedicado | — |
| Reconocimiento visual — motor anterior (en transición, ver "Estado actual y pendientes") | MediaPipe Tasks Vision (`com.google.mediapipe:tasks-vision`, Image Embedder con MobileNet-V3 Large) + OpenCV/ORB — **ya no participan en la búsqueda real** de `RecognitionScreen`; siguen solo como línea base de comparación en "Evaluar búsqueda visual" (solo administrador), pendientes de eliminar tras confirmar esa comparación con fotos reales | 1.0.0 |
| Cámara | CameraX (`camera-core`/`camera-camera2`/`camera-lifecycle`/`camera-view`) | 1.6.2 |
| Inicio de sesión con Google | Credential Manager (`androidx.credentials` + `credentials-play-services-auth`) y `com.google.android.libraries.identity.googleid`, con `GoogleAuthProvider` de Firebase Auth — no se usa `GoogleSignInClient` (deprecado). Requiere `default_web_client_id` (ver "Inicio de sesión con Google" en Configuración y ejecución) | 1.3.0 / 1.3.0 / 1.1.1 |
| `minSdk` / `targetSdk` / `compileSdk` | — | 31 / 36 / 36 |
| JVM target | — | 11 |

`app/build.gradle.kts` filtra los binarios nativos de OpenCV y de MediaPipe a
`arm64-v8a` y `x86_64` (`ndk { abiFilters }`); además `androidResources { noCompress +=
"tflite" }` deja el modelo sin comprimir en el APK (MediaPipe necesita mapearlo en
memoria tal cual, comprimido falla al abrirlo). Es una combinación pesada: un APK de
debug limpio (sin filtro de ABI) llegaba a ~112 MB solo con OpenCV (4 ABIs); filtrado a
2 ABIs quedaba en ~55-60 MB; con MediaPipe + el modelo agregados, el mismo APK de debug
filtrado a 2 ABIs mide **~158 MB** (medido en esta sesión) — el modelo (10,9 MB) y
`libmediapipe_tasks_jni.so` (~11 MB en arm64-v8a, ~13,7 MB en x86_64) explican parte del
salto, pero la mayor parte es el árbol de dependencias Java de `tasks-core` (Guava
completo, `protobuf-javalite`, Flogger, `com.google.android.datatransport`), que infla
bastante los `.dex`. Si el peso del APK es un problema real, la alternativa más simple es
cambiar `MotorHuellaVisual.MODELO_EMBEDDER` a `mobilenet_v3_small.tflite` (agregar ese
archivo a `assets/` y subir `VERSION_HUELLA`) — el modelo en sí pesa menos (~4 MB), pero
no reduce el peso de las dependencias Java de MediaPipe, que es la parte más grande.
**Este peso es temporal**: MediaPipe/OpenCV-matching ya no participan en la búsqueda
real (ver "Búsqueda visual" en Flujos principales) y están pendientes de eliminar del
todo (con eso baja también el APK) en cuanto se confirme la comparación de la Fase 4
con fotos reales — ver "Estado actual y pendientes".

Proyecto Firebase: `minka-art` (bucket de Storage `minka-art.firebasestorage.app`),
configurado en `app/google-services.json`, que está versionado en el repositorio.
Además de Firebase, el proyecto usa **Google Cloud Vertex AI** (mismo proyecto
`minka-art`, plan de facturación Blaze) para los embeddings de imagen de la búsqueda
visual — ver "Configuración de Google Cloud (búsqueda visual)" más abajo para el
detalle exacto de lo que hay que habilitar/configurar.

Además del proyecto Android, el repositorio incluye `functions/`: un backend de Cloud
Functions en Node.js + TypeScript (`onCall`/`onDocumentWritten`, v2) que genera y valida
el código de verificación de correo y lo envía por [Resend](https://resend.com) (y también el enlace de cambio de contraseña), y que además calcula los embeddings de la búsqueda
visual con Vertex AI (`google-auth-library`) y los compara con Firestore vector search
(`@google-cloud/firestore`, empaquetado dentro de `firebase-admin`). Ver
[Modelo de datos](#modelo-de-datos) y [Flujos principales](#flujos-principales).

## Arquitectura

MVVM por capas, con Firebase llamado directamente desde el repositorio (no hay una capa
de red intermedia). **Todo el código asíncrono usa callbacks `onSuccess` / `onError`**,
no corrutinas ni `Flow` — es la convención del proyecto, ver `CLAUDE.md`.

```
MainActivity → MinkaArt_btaTheme → AppNavigation (NavHost)
    Screen (Compose)  →  ViewModel  →  Repository  →  Firebase (Auth / Firestore / Storage)
                                                    →  Cloud Functions → Firestore / Resend
                                                                       → Vertex AI (búsqueda visual)
```

| Capa | Paquete | Responsabilidad |
|---|---|---|
| Entrada | `com.minkaart2.app` (`MainActivity.kt`) | Activity única (`AppCompatActivity`, necesaria para los idiomas por app), `enableEdgeToEdge()`, monta el tema y `AppNavigation`. |
| Navegación | `com.minkaart2.app.navigation` | `Routes` (constantes de ruta) y `AppNavigation` (`NavHost`). |
| UI | `com.minkaart2.app.ui.*` | Un paquete por pantalla (Composables), sin lógica de negocio. |
| Estado | `com.minkaart2.app.viewModel` / `viewmodel` | Validación de formularios y estado observable (`State<T>`). |
| Datos | `com.minkaart2.app.repository` | Llamadas directas a Firebase Auth/Firestore/Storage, y a las Cloud Functions de verificación de correo (`AuthRepository.enviarCodigoVerificacion` / `verificarCodigo`). |
| Modelo | `com.minkaart2.app.data.model` | `data class` que se serializan/deserializan con Firestore. |
| Tema | `com.minkaart2.app.ui.theme` | Paleta de marca, tipografía, `MaterialTheme`. |
| Backend | `functions/src/index.ts` | Cloud Functions `onCall` (Node.js + TypeScript): generan, guardan y validan el código de verificación, y lo envían por correo con la API de Resend; `enviarEnlaceCambioContrasena` envía en español el enlace para cambiar la contraseña (lo genera Firebase Auth con el Admin SDK; 1 envío/minuto por correo, control en `limitesRecuperacion/{correo}`). Cuatro triggers de Firestore (`notificarNuevaVenta`, `notificarNuevaValoracion`, `notificarLikeNuevo`, `notificarCambioRuac`) escriben en `notificaciones` y envían el push de FCM. Para la búsqueda visual: `sincronizarEmbeddingVisual` (trigger en `productos/{productId}`) calcula el embedding con Vertex AI y lo guarda en `embeddingsVisuales`, y `buscarPorImagen` (callable) hace la búsqueda real con Firestore `findNearest` — ver Modelo de datos y "Búsqueda visual". |

## Estructura del proyecto

> ⚠️ El paquete Kotlin es `com.minkaart2.app`, pero las carpetas físicas en disco son
> `com/minkaart/app/` (sin el `2`). Es intencional en el sentido de que compila así, pero
> es fácil de pasar por alto: si buscas código por ruta de carpeta y no aparece, revisa
> también por nombre de paquete.

```
app/src/main/
├── assets/
│   └── mobilenet_v3_large.tflite # modelo del Image Embedder de MediaPipe (ver Stack técnico)
└── java/com/minkaart/app/
├── MainActivity.kt              # MotorHuellaVisual.inicializar(applicationContext) aquí
├── data/
│   ├── CatalogosProducto.kt # departamentos, categorías, materias primas, clasificaciones,
│   │                         # colores y tamaños (listas fijas, compartidas por Publicar/
│   │                         # Editar producto, el registro de artesano y los filtros) +
│   │                         # mapas valor-en-español → @StringRes para mostrarlos
│   │                         # traducidos (lo guardado en Firestore nunca se traduce)
│   ├── Terminos.kt          # VERSION_TERMINOS ("1.0"): versión del texto de TermsScreen
│   ├── EscalaValoracion.kt  # niveles de calidad/confianza <-> puntos <-> etiqueta (Fase 6)
│   └── model/
│       ├── ArtesanoStats.kt # documento de "artesanoStats" — confianza del artesano
│       ├── InfoVersion.kt  # documento "configuracion/version" (aviso de actualización)
│       ├── HuellaVisual.kt # documento de "huellasVisuales" — motor ANTERIOR de
│       │                     # reconocimiento visual, en transición (ver Stack técnico
│       │                     # y "Estado actual y pendientes"); ya no lo usa la
│       │                     # búsqueda real, solo "Evaluar búsqueda visual"
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
│   ├── ConfiguracionRepository.kt # lee "configuracion/version" (aviso de actualización)
│   ├── NotificationRepository.kt # leer notificaciones y marcarlas como leídas (nunca las crea)
│   ├── ProductRepository.kt  # guardar/editar/eliminar producto, consultas por id/artesano/lote de ids, like con transacción
│   └── VisualSearchRepository.kt # CRUD de "huellasVisuales" — motor ANTERIOR, en
│                                   # transición, solo lo lee "Evaluar búsqueda visual"
├── vision/
│   ├── PreprocesadorImagen.kt # NUEVO — CLAHE (poca luz), rechaza fotos borrosas/muy
│   │                           # oscuras, redimensiona y da el JPEG que se manda a
│   │                           # buscarPorImagen (Cloud Functions). Es lo único de
│   │                           # vision/ que usa la búsqueda real hoy
│   ├── MotorHuellaVisual.kt   # Motor ANTERIOR (en transición): ORB + histograma HSV
│   │                           # (comparar()/puntajeOrb()) + embedding de MediaPipe
│   │                           # Image Embedder (similitudCoseno()) — ya no lo usa
│   │                           # RecognitionScreen, solo AdminVisualEvalScreen como
│   │                           # línea base de comparación
│   └── GeneradorHuellas.kt    # Motor ANTERIOR: genera huellas pendientes en lote —
│                               # sigue corriendo (Publicar/Editar + autocompletado de
│                               # HomeArtesanoScreen) solo para alimentar esa línea base
├── viewModel/                 # ¡ojo con mayúscula/minúscula, ver más abajo!
│   ├── ActualizacionViewModel.kt # decide si hay versión nueva (opcional/obligatoria), una vez por arranque
│   ├── AdminRuacListViewModel.kt # solicitudes de RUAC pendientes y cerrar sesión
│   ├── ArtisanProfileViewModel.kt
│   ├── ClientProfileViewModel.kt
│   ├── HomeArtesanoViewModel.kt
│   ├── LikesViewModel.kt
│   ├── GoogleSignInViewModel.kt # Credential Manager + Firebase Auth: continuarConGoogle,
│   │                             # crear cliente nuevo, cancelar (cierra la sesión sin documento)
│   ├── LoginViewModel.kt
│   ├── NotificationsViewModel.kt # estado de la campana: lista + conteo de no leídas
│   ├── ProductViewModel.kt
│   ├── RecognitionViewModel.kt # búsqueda real: estados Captura/Recortando/Analizando/
│   │                             # Resultados/SinResultados/Error — llama al callable
│   │                             # "buscarPorImagen" (Vertex AI), ya no compara nada local
│   ├── RegisterViewModel.kt
│   ├── SesionViewModel.kt # destinoInicial(): Home del rol si hay sesión guardada, o Bienvenida;
│   │                        # recuperarRol() para sesiones anteriores al almacén local
│   └── VisualEvalViewModel.kt # evaluación de 3 métodos: Solo ORB / Híbrido (motor
│                                 # anterior, local) vs. Nuevo — Vertex AI (real, separado
│                                 # buena/poca luz) — solo administrador
└── ui/
    ├── admin/                     # Rol "administrador" (Fase 5)
    │   ├── AdminRegisterScreen.kt  # formulario mínimo: correo, contraseña, PIN
    │   ├── AdminRuacListScreen.kt  # solicitudes de RUAC pendientes + botones "Cerrar sesión" y "Evaluar
    │   │                           # búsqueda visual" (a la derecha, bajo el encabezado; no
    │   │                           # genera huellas, solo evalúa)
    │   ├── AdminRuacReviewScreen.kt # aprobar/rechazar una solicitud
    │   └── AdminVisualEvalScreen.kt # evaluación con fotos de prueba: Solo ORB / Híbrido
    │                                 # (anterior) vs. Nuevo — Vertex AI (buena/poca luz)
    ├── ruac/                      # Solicitud e insignia de RUAC (Fase 5)
    │   ├── RequestRuacScreen.kt   # el artesano solicita/re-solicita su RUAC
    │   └── RuacBadgeScreen.kt     # insignia real, 3 bloques (encabezado con patrón, datos del registro, "¿por qué es importante?")
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
    │   ├── RolePillToggle.kt      # selector Artesano/Cliente en pills: cambio de modo de los
    │   │                         # dos Home, con colores propios vía parámetros opcionales
    │   │                         # (ya NO se usa en el Registro: el rol se elige antes,
    │   │                         # en RoleSelectionScreen)
    │   ├── BotonOjoContraseña.kt  # ojo mostrar/ocultar contraseña (trailingIcon de
    │   │                         # GlassTextField); usa ic_visibility(_off) propios porque
    │   │                         # no hay material-icons-extended
    │   ├── ImageUploadBox.kt      # selector de varias imágenes con miniaturas + eliminar
    │   │                         # (Publicar/Editar), hasta ProductViewModel.MAX_IMAGENES
    │   ├── MinkaFormField.kt      # campo con etiqueta + caja (fondo claro, artesano)
    │   ├── PlaceholderScreen.kt   # relleno para rutas de fases futuras (hoy ninguna ruta lo
    │   │                         # usa: ACCOUNT_SETTINGS ya es una pantalla real)
    │   ├── MinkaMultiSelectField.kt # selección múltiple de una lista fija (diálogo con
    │   │                         # casillas + chips con lo elegido): departamentos y categorías
    │   │                         # del registro de artesano
    │   ├── CheckboxTerminos.kt    # casilla obligatoria "Acepto los Términos y Condiciones…" con
    │   │                         # enlace a TermsScreen (registros)
    │   ├── SelectorIdioma.kt      # pills "ES | EN" (WelcomeScreen)
    │   ├── BarraValoracion.kt     # barra con degradado de marca (calidad/confianza), reemplaza a ConfianzaDots
    │   ├── ProductoFeedCard.kt    # tarjeta de producto (feed del cliente y perfil del artesano)
    │   ├── DialogoActualizacion.kt # ventana "Actualización nueva" (logo, mensaje, Actualizar / Más tarde); la monta AppNavigation
    │   ├── BottomNavBar.kt        # barra inferior (Home cliente y Home artesano)
    │   ├── LogoMinka.kt           # LogoCircular (logo1 recortado en círculo), LogoSinFondo (logo2 sin fondo,
    │   │                         # teñido; Bienvenida), LogoMinka (+ eslogan, hoy sin uso) y LogoMinkaEncabezado (logo + eslogan, título opcional; Home cliente/artesano
    │   │                         # y panel del administrador)
    │   ├── IconoNotificaciones.kt # IconoAccion (círculo vinotinto + pictograma blanco + punto/píldora de novedad azul claro) y la campana que lo usa; compartido por los dos Home
    │   ├── MinkaDropdownField.kt  # selector de una lista fija (Departamento/Categoría/Clasificación)
    │   └── EfectoNotificaciones.kt # pide permiso de notificaciones + guarda el token FCM
    ├── home/
    │   ├── HomeArtesanoScreen.kt  # encabezado con campana + avatar de perfil
    │   ├── HomeClienteScreen.kt   # encabezado con campana, búsqueda, chips de categoría sincronizados con un HorizontalPager (deslizar cambia de categoría), feed, bottom nav
    │   └── PanelFiltros.kt        # bottom sheet de filtros (material, ubicación, color, tamaño; selección múltiple) + FiltrosCatalogo
    ├── login/LoginScreen.kt
    ├── notifications/NotificationsScreen.kt # centro de notificaciones, abierto desde la campana
    ├── product/ProductDetailScreen.kt  # detalle, artesano, calidad/confianza, contacto WhatsApp
    ├── profile/
    │   ├── ArtisanProfileScreen.kt  # perfil público del artesano, editable por su dueño
    │   ├── ClientProfileScreen.kt   # menú del cliente: info básica, likes, cerrar sesión
    │   ├── EditProfileScreen.kt     # "Editar información básica" (cliente y artesano): foto, nombre, teléfono, ciudad/región
    │   ├── LikesScreen.kt           # lista de productos con like del usuario actual
    │   └── ProfileScreen.kt         # placeholder genérico, sin usar desde esta fase
    ├── publication/
    │   ├── MyProductsScreen.kt    # "Panel de Publicaciones": lista, editar/eliminar (menú ⋮)
    │   ├── PublicationScreen.kt
    │   └── EditProductScreen.kt
    ├── recognition/RecognitionScreen.kt # búsqueda visual real (cámara/galería, flash,
    │                                       # recorte ajustable estilo Google Lens)
    ├── register/RegisterScreen.kt
    ├── roleselection/RoleSelectionScreen.kt # "¿Cómo quieres usar MinkaArt?": Soy artesano / Soy cliente
    │                                         # (tarjetas blancas con ícono y texto vinotinto)
    ├── settings/AccountSettingsScreen.kt    # Configuración. Cliente: solo nombre y correo, idioma y ayuda y soporte. Artesano: además foto, teléfono, ciudad, seguridad, notificaciones, cerrar sesión, "Mi insignia RUAC" y "Mi taller"
    ├── terms/TermsScreen.kt                 # Términos y Condiciones + Política de Datos (es/en)
    ├── welcome/WelcomeScreen.kt
    └── theme/
        ├── Color.kt
        ├── Theme.kt
        └── Type.kt
├── service/
│   └── MinkaMessagingService.kt   # FirebaseMessagingService: recibe el push, construye
│                                   # la notificación, guarda el token si rota
└── util/
    ├── Texto.kt                    # normalizar(): quita tildes/mayúsculas para
    │                                # búsqueda de texto; compartida por HomeClienteScreen
    │                                # y MyProductsScreen (Ajustes 2)
    ├── Etiquetas.kt                # etiquetaCategoria/Color/Tamano/MateriaPrima/Clasificacion():
    │                                # texto traducido de un valor guardado en Firestore
    ├── Idioma.kt                   # Idioma.actual()/cambiar("es"|"en") sobre AppCompatDelegate
    └── SesionLocal.kt              # SharedPreferences "sesion_minka": rol + "Recuérdame" para la
                                     # sesión persistente (ver "Sesión persistente")

app/src/main/res/
├── values/strings.xml              # textos en español (idioma por defecto)
├── values-en/strings.xml           # textos en inglés — toda clave nueva va en los DOS
├── xml/locales_config.xml          # idiomas ofrecidos (es, en) en Ajustes del sistema
├── mipmap-anydpi/ic_launcher.xml, ic_launcher_round.xml  # ícono adaptativo de la app (ver "Ícono de la app" en Diseño y marca)
└── drawable/logo1.png (logo oficial MINKA ART), logo2.png (el mismo logo en 1024×1024, solo para el ícono de la app), ic_launcher_background.xml (color sólido #4B1022), ic_launcher_foreground_minka.xml (logo2 con inset 4 %), ic_artesano.xml, ic_cliente.xml, ic_filtros.xml  # vectores propios; img_rol_artesano.jpg / img_rol_cliente.jpg: fotos de las tarjetas de RoleSelectionScreen (no hay
                                    # material-icons-extended)
```

Aparte, `app/src/debug/assets/evaluacion/` (solo se empaqueta en el APK de debug) guarda las
fotos de prueba y un `etiquetas.csv` para "Evaluar búsqueda visual" — ver "Evaluación" en
Flujos principales; el formato exacto está en el `README.txt` de esa misma carpeta.

Además, en la raíz del repositorio (fuera de `app/`):

```
firebase.json               # configuración del proyecto Firebase (Functions, Firestore)
.firebaserc                  # alias del proyecto Firebase (minka-art)
firestore.indexes.json       # índices compuestos, incluido el índice vectorial de
                              # embeddingsVisuales.embedding (dimension 1408, flat)
functions/
├── src/index.ts             # Cloud Functions:
│                             #  - onCall enviarCodigoVerificacion / verificarCodigo
│                             #  - triggers que alimentan "notificaciones"
│                             #    (notificarNuevaVenta, notificarNuevaValoracion,
│                             #    notificarLikeNuevo, notificarCambioRuac)
│                             #  - sincronizarEmbeddingVisual (trigger en "productos"):
│                             #    calcula el embedding con Vertex AI y lo guarda en
│                             #    "embeddingsVisuales"
│                             #  - buscarPorImagen (onCall): la búsqueda real —
│                             #    embedding de la foto + Firestore findNearest
├── src/scripts/rellenarEmbeddings.ts # script (no Cloud Function) para el relleno
│                             # único de productos existentes — se corre a mano, una
│                             # sola vez, con Application Default Credentials locales:
│                             # npm run rellenar-embeddings
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
| `WELCOME` | `welcome` | `WelcomeScreen` | Sí (destino inicial si no hay sesión guardada; con sesión guardada la app arranca directo en el Home del rol — ver "Sesión persistente") | — |
| `LOGIN` | `login` | `LoginScreen` | Sí | `WelcomeScreen`, `LoginScreen` (recuperar contraseña) |
| `ROLE_SELECTION` | `role_selection?google={google}` | `RoleSelectionScreen` | Sí, con argumento opcional `google` (Bool, por defecto `false`) | `WelcomeScreen` ("Registrarse"), `LoginScreen` ("Regístrate" y usuario nuevo de Google, con `google=true`) |
| `REGISTER` | `register/{rol}?google={google}` | `RegisterScreen` | Sí, con argumento `rol` (`cliente`/`artesano`; otro valor no renderiza nada) y `google` opcional | `RoleSelectionScreen` (`Routes.registerRoute(rol, google)`) |
| `TERMS` | `terms` | `TermsScreen` (Términos y Condiciones + Política de Tratamiento de Datos) | Sí | Enlace "Términos y Condiciones" de `CheckboxTerminos` en `RegisterScreen`, `AdminRegisterScreen` y `RoleSelectionScreen` (solo en modo Google) |
| `HOME_CLIENTE` | `home_cliente` | `HomeClienteScreen` (catálogo real) | Sí | `LoginScreen` (rol `cliente`), arranque con sesión guardada (`AppNavigation`), toggle "Cliente" del encabezado de `HomeArtesanoScreen` (cambio de modo) |
| `HOME_ARTESANO` | `home_artesano` | `HomeArtesanoScreen` | Sí | `LoginScreen` (rol `artesano`), arranque con sesión guardada (`AppNavigation`), toggle "Artesano" del encabezado de `HomeClienteScreen` y fila "Cambiar a modo artesano" de `ClientProfileScreen` (ambos solo si el usuario es artesano — vuelven al modo artesano) |
| `MY_PRODUCTS` | `my_products` | `MyProductsScreen` (Panel de Publicaciones) | Sí | `HomeArtesanoScreen` |
| `PUBLICATION` | `publication` | `PublicationScreen` | Sí | `HomeArtesanoScreen`, `MyProductsScreen` (chip "Nueva") |
| `EDIT_PRODUCT` | `edit_product/{productId}` | `EditProductScreen` | Sí, con argumento de ruta | `MyProductsScreen` (menú ⋮ → Editar) |
| `PRODUCT_DETAIL` | `product_detail/{productId}` | `ProductDetailScreen` | Sí, con argumento de ruta | `HomeClienteScreen` (tap en una tarjeta del feed) |
| `ARTISAN_PROFILE` | `artisan_profile/{artesanoId}` | `ArtisanProfileScreen` | Sí, con argumento de ruta | `HomeClienteScreen` (nombre del artesano en una tarjeta), `ProductDetailScreen` (fila del artesano), `HomeArtesanoScreen` (ícono de perfil, propio) |
| `CLIENT_PROFILE` | `client_profile` | `ClientProfileScreen` | Sí | `HomeClienteScreen` (bottom nav "Perfil") — también la ve un artesano "viendo como cliente" (rol real, no la ruta), con contenido algo distinto (ver Flujos principales) |
| `LIKES` | `likes` | `LikesScreen` | Sí | `HomeClienteScreen` (bottom nav "Likes"), `ClientProfileScreen` ("Mis likes") |
| `PURCHASE_HISTORY` | `purchase_history` | `PurchaseHistoryScreen` (real desde Fase 6) | Sí | `ClientProfileScreen` ("Historial de compras") |
| `ACCOUNT_SETTINGS` | `account_settings` | `AccountSettingsScreen` (real; antes `PlaceholderScreen`) — selector de idioma Español / English | Sí | `ClientProfileScreen` ("Configuración de la cuenta"), `ArtisanProfileScreen` (ícono de engranaje en la cabecera, solo cuando lo ve su dueño) |
| `EDIT_PROFILE` | `edit_profile` | `EditProfileScreen` (nombre, teléfono, ciudad/región, foto; el correo es solo lectura) | Sí | `ClientProfileScreen` ("Información básica"), `AccountSettingsScreen` (filas de "Información básica") |
| `PROFILE` | `profile` | `ProfileScreen` (placeholder) | Sí | Nadie — reemplazado por `ARTISAN_PROFILE`/`CLIENT_PROFILE`; queda sin usar |
| `RUAC_BADGE` | `ruac_badge/{artesanoId}` | `RuacBadgeScreen` (real desde Fase 5) | Sí, con argumento de ruta | `ArtisanProfileScreen` (chip de insignia RUAC verificado) |
| `RECOGNITION` | `recognition` | `RecognitionScreen` (búsqueda visual real, ya no placeholder) | Sí | `HomeClienteScreen` (ícono de la barra de búsqueda) |
| `ADMIN_REGISTER` | `admin_register` | `AdminRegisterScreen` | Sí | `RoleSelectionScreen` (texto "¿Eres administrador? Regístrate aquí" bajo las dos tarjetas; solo fuera del modo Google), `RegisterScreen` (mismo enlace) |
| `ADMIN_RUAC_LIST` | `admin_ruac_list` | `AdminRuacListScreen` | Sí | `LoginScreen` (rol `administrador`; también al entrar con Google siendo administrador), arranque con sesión guardada (`AppNavigation`) |
| `ADMIN_RUAC_REVIEW` | `admin_ruac_review/{artesanoId}` | `AdminRuacReviewScreen` | Sí, con argumento de ruta | `AdminRuacListScreen` (tap en una solicitud) |
| `REQUEST_RUAC` | `request_ruac` | `RequestRuacScreen` | Sí | `ArtisanProfileScreen` (botón "Agregar RUAC"/"Volver a solicitar RUAC", solo el propio artesano) |
| `ADMIN_VISUAL_EVAL` | `admin_visual_eval` | `AdminVisualEvalScreen` | Sí | `AdminRuacListScreen` (botón "Evaluar búsqueda visual", a la derecha bajo el encabezado) |
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
| `region` | `String` | Ciudad/región en texto libre. Heredado de antes de `departamentos` (ver "Nota de migración `region` → `departamentos`" abajo), pero **`EditProfileScreen` vuelve a escribirlo** (campo "Ciudad / región" de Editar información básica, cliente y artesano); no sustituye a `departamentos` |
| `telefono` | `String` | Solo artesano |
| `fechaNacimiento` | `String` | Solo artesano, formato `dd/MM/yyyy` |
| `region` | `String` | **Heredado, solo lectura** (ver "Nota de migración `region` → `departamentos`" abajo): ningún código nuevo lo vuelve a escribir, reemplazado por `departamentos` |
| `departamentos` | `List<String>` | Solo artesano; selección múltiple (mínimo 1) de `CatalogosProducto.DEPARTAMENTOS`, los mismos 33 de Publicar/Editar producto |
| `productosQueVende` | `String` | Solo artesano, texto libre |
| `categoriasArtesanales` | `List<String>` | Solo artesano; selección múltiple de las 16 opciones de `CatalogosProducto.CATEGORIAS` (mínimo 1). Los artesanos registrados antes de este ajuste tienen aquí el texto libre que escribieron (se partía por comas) y se leen igual: nada en la app lo reinterpreta |
| `categoriasArtesanalesOtro` | `String?` | Solo cuando `categoriasArtesanales` incluye `"Otros"`: lo escrito en "Especifica cuál" (obligatorio en ese caso). `null` en cualquier otro caso — mismo patrón que `Product.categoriaOtro` |
| `aceptoTerminos` | `Boolean` | `true` si se aceptaron los Términos y Condiciones al registrarse; `false` en usuarios anteriores a este ajuste |
| `terminosAceptadosEn` | `Timestamp?` | Cuándo se aceptaron (reloj del dispositivo, `Timestamp.now()`); `null` en usuarios anteriores |
| `versionTerminos` | `String` | Versión del texto aceptado (`VERSION_TERMINOS`, hoy `"1.0"`); `""` en usuarios anteriores |
| `fotoUrl` | `String?` | URL de la foto de perfil (cliente o artesano) en Storage `usuarios/{uid}/foto_perfil`; `null` hasta que la sube — se muestra un ícono por defecto |
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

**Nota de migración `region` → `departamentos`**: un artesano registrado antes de este
ajuste solo tiene `region` (texto libre, p. ej. "Bogotá, Boyacá"); `departamentos` queda
vacío (`emptyList()`). Hoy ninguna pantalla muestra ni lee `region`/`departamentos` de un
artesano (solo el registro los escribe), así que no hay nada que se rompa ni nada que
migrar para que la app funcione; **no hay ningún script de migración** que convierta el
texto libre en departamentos. Si más adelante una pantalla muestra los departamentos del
artesano, debe caer a `region` cuando `departamentos` esté vacío (mismo criterio que
`Product.regionOrigen`). Igual con `categoriasArtesanales` (texto libre en los artesanos
antiguos) y con los campos de términos (los usuarios anteriores quedan con
`aceptoTerminos == false`; la app no les pide aceptar el texto después).

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

### Firestore — documento `configuracion/version` (aviso de actualización)

Un único documento que **se crea y se edita a mano** en Firebase Console (ninguna pantalla lo
escribe). Lo lee `ConfiguracionRepository.obtenerInfoVersion` al arrancar la app; modelo
`data/model/InfoVersion.kt`. Reglas: `allow get: if true` (pública, porque la ventana también
sale en Bienvenida, sin sesión) y `allow write: if false` — **hay que desplegar las reglas**
(`firebase deploy --only firestore:rules`).

| Campo | Tipo | Notas |
|---|---|---|
| `versionActual` | `Number` | `versionCode` de la última versión publicada. Si el instalado es menor, sale la ventana (se puede cerrar con "Más tarde") |
| `versionMinima` | `Number` | Si el instalado es menor, la ventana es obligatoria (sin "Más tarde", no se cierra con atrás). `0` = nunca obligatoria |
| `mensaje` | `String` | Texto de la ventana; si está vacío se usa uno por defecto |
| `urlDescarga` | `String` | Enlace al APK (Drive, Storage, etc.). Si está vacío no se muestra ninguna ventana |

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
| `color` | `String` | Opcional. Uno de `CatalogosProducto.COLORES` (Natural/crudo, Blanco, Negro, Café, Rojo, Naranja, Amarillo, Verde, Azul, Morado, Rosado, Gris, Multicolor) o `""` si no se especificó. Alimenta el filtro "Color" del bottom sheet de `HomeClienteScreen`. `""` también en productos publicados antes de este campo: no aparecen al filtrar por color, pero sí sin filtro |
| `tamano` | `String` | Opcional. Uno de `CatalogosProducto.TAMANOS` (Pequeño (menos de 15 cm) / Mediano (15–40 cm) / Grande (más de 40 cm)) o `""`. Mismo comportamiento que `color` con el filtro "Tamaño" |
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

### Firestore — colección `embeddingsVisuales/{productId}` (Búsqueda visual — motor real)

Solo la escriben/leen Cloud Functions (`allow read, write: if false` en las reglas) — la
app nunca la toca directamente, solo recibe `{productId, artesanoId, similitud}` de
vuelta del callable `buscarPorImagen`. No hay una `data class` de Kotlin para esto
(igual que `codigosVerificacion`): no hace falta, ningún código de la app la deserializa.

| Campo | Tipo | Notas |
|---|---|---|
| `productId` | `String` | Igual al id del documento |
| `artesanoId` | `String` | Copiado de `Product.artesanoId` |
| `activo` | `Boolean` | Copiado de `Product.activo` — `buscarPorImagen` descarta los inactivos después de `findNearest` (no antes: el filtro se aplica en memoria sobre los ~30 resultados, no como pre-filtro del índice) |
| `modelo` | `String` | `"multimodalembedding@001"` — por si en el futuro conviven embeddings de más de un modelo/dimensión |
| `version` | `Int` | `1` — sube si cambia cómo se calcula el embedding (p. ej. otro modelo o dimensión), mismo criterio que `HuellaVisual.version` en el motor anterior |
| `actualizadoEn` | `Timestamp` | `FieldValue.serverTimestamp()` |
| `embedding` | Firestore `Vector` (`FieldValue.vector()`, no un `Blob`) | 1408 valores `float32`, ya normalizado a norma 1 por Vertex AI — con eso, `DOT_PRODUCT` en `findNearest` equivale a similitud coseno sin costo extra de normalizar |

**Cómo se mantiene al día, siempre en automático** (reemplaza a "Huellas visuales" del
motor anterior, ver más abajo): la Cloud Function `sincronizarEmbeddingVisual`
(`onDocumentWritten` sobre `productos/{productId}`) descarga `imagenes[0]` (la imagen
principal) de Storage, le pide el embedding a Vertex AI (`multimodalembedding@001`,
región `us-central1`) y lo guarda — solo cuando `imagenes` cambió (producto nuevo o
imágenes editadas). Si solo cambió `activo`, se actualiza nada más ese campo, sin volver
a llamar a Vertex AI. Si se borra el producto, se borra su documento aquí. Cualquier
error se registra en logs y nunca rompe la publicación/edición del producto.

**Índice vectorial**: `embeddingsVisuales.embedding`, `dimension: 1408`, tipo `flat`,
`queryScope: COLLECTION` — declarado en `firestore.indexes.json` y creado con
`gcloud firestore indexes composite create` (ver "Configuración de Google Cloud" más
abajo). El máximo permitido por Firestore es 2048 dimensiones.

**Relleno único de productos existentes**: `functions/src/scripts/rellenarEmbeddings.ts`
(no es una Cloud Function, es un script que se corre a mano una sola vez) recorre todos
los productos y genera el embedding de los que no lo tenían — se usó para los ~103
productos que ya existían al momento de agregar la búsqueda visual con Vertex AI
(incluidos los del catálogo cargado por script, origen `"catalogo_adc"`), con 0 errores.

### Firestore — colección `limitesBusquedaVisual/{uid}` (control de costos)

Solo la escribe la Cloud Function `buscarPorImagen`, para limitar cuántas búsquedas
visuales puede hacer un mismo usuario por día (evita que alguien agote el presupuesto de
Vertex AI a propósito o por error). Un administrador queda exento de este límite (lo
necesita para correr "Evaluar búsqueda visual" contra muchas fotos de prueba de una
sola vez).

| Campo | Tipo | Notas |
|---|---|---|
| `fecha` | `String` | `YYYY-MM-DD`, se reinicia el conteo cuando cambia |
| `conteo` | `Int` | Búsquedas hechas ese día — tope actual: `LIMITE_BUSQUEDAS_DIARIAS = 30` |

### Firestore — colección `huellasVisuales/{productId}` (Búsqueda visual — motor ANTERIOR, en transición)

**Este motor ya no participa en la búsqueda real** (ver "Búsqueda visual" en Flujos
principales y "Estado actual y pendientes") — sigue existiendo únicamente porque
`AdminVisualEvalScreen` todavía lo usa como línea base de comparación ("Solo ORB" /
"Híbrido") contra el motor nuevo, y está pendiente de eliminar (junto con
`GeneradorHuellas`, `VisualSearchRepository`, `MotorHuellaVisual` y las dependencias de
MediaPipe/OpenCV-matching) una vez esa comparación se confirme con fotos reales.

Definido en `data/model/HuellaVisual.kt`. **Colección aparte de `productos` a
propósito**: no debía romper `productoBaseIgual` (que ya compara todos los campos
"base" de un producto) ni hacer más pesado el `get()` completo que trae el feed del
catálogo (`obtenerTodosLosProductosActivos`) — las huellas solo las necesita
`AdminVisualEvalScreen` (antes también `RecognitionScreen`, ya no).

| Campo | Tipo | Notas |
|---|---|---|
| `productId` | `String` | Igual al id del documento |
| `artesanoId` | `String` | Copiado de `Product.artesanoId` — lo usan las reglas para verificar dueño |
| `version` | `Int` | Se compara contra `MotorHuellaVisual.VERSION_HUELLA` (actualmente **2** — subió al agregar el embedding semántico de MediaPipe); si el algoritmo cambia, sube la constante y `GeneradorHuellas` regenera cualquier huella con una versión menor. Una huella `v1` (sin `embedding`) sigue funcionando: da similitud coseno `0.0` hasta que se regenera — nunca aparece en "Artesanías parecidas", pero sí puede seguir apareciendo en "Coincidencia exacta" si ORB la encuentra |
| `creadoEn` | `Timestamp?` | `Timestamp.now()`, puesto por `VisualSearchRepository.guardarHuella` |
| `imagenes` | `List<HuellaImagen>` | Una entrada por cada imagen de `Product.imagenes` en el momento de calcular la huella |

`HuellaImagen`: `indiceImagen: Int` (posición en `Product.imagenes`), `descriptores: Blob`
(descriptores binarios de ORB, ~500 keypoints × 32 bytes ≈ 16 KB), `filas`/`columnas: Int`
(dimensiones de la matriz de descriptores, necesarias para reconstruirla),
`keypointsX`/`keypointsY: List<Double>` (posición de cada keypoint, en el mismo orden que
los descriptores — necesarias para `findHomography` + RANSAC, no solo para el matching de
descriptores), `histograma: List<Double>` (histograma HSV normalizado, canales H y S) y,
desde `version = 2`, `embedding: Blob` — el embedding semántico de MediaPipe Image
Embedder, cuantizado a 1 byte por dimensión y L2-normalizado (`quantize = true`,
`l2Normalize = true`): con MobileNet-V3 Large, el vector tiene 1280 valores, es decir
**1280 bytes** (~1,25 KB) por imagen. Es la pieza más liviana de la huella con
diferencia: apenas engorda el documento (~1,25 KB de 1280 bytes sobre los ~32 KB que ya
pesaba cada imagen entre ORB e histograma, un ~4% más). El documento completo sigue muy
por debajo del límite de 1 MB de Firestore incluso con las 15 imágenes de
`ProductViewModel.MAX_IMAGENES` (≈ 500 KB en el peor caso).

**Cómo se generan, siempre en automático, sin que nadie tenga que pulsar nada** (ver
"Huellas visuales" en Flujos principales): al publicar/editar, y con un autocompletado
silencioso en `HomeArtesanoScreen` — ambos caminos comparten el mismo caso de uso,
`vision/GeneradorHuellas.kt`, así que ninguno duplica la lógica de "qué está pendiente" ni
cómo se calcula cada huella. El administrador **no** participa en generar huellas — su
único vínculo con la búsqueda visual es la pantalla de evaluación para el documento de
grado (ver "Evaluación de la búsqueda visual"), que no escribe ninguna huella.

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

**`color`/`tamano` en las reglas de `productos`**: entraron a `productoBaseIgual` (archivo
`y`) con `.get('color', '')` / `.get('tamano', '')`, por la misma razón documentada arriba
(un producto anterior a estos campos no los tiene escritos en Firestore, y acceder con `.`
a un campo ausente daría `PERMISSION_DENIED`). **Hay que desplegar las reglas de Firestore**
(`firebase deploy --only firestore:rules`) para que un cliente pueda dar like/valorar/sumar
vista a un producto que ya tenga `color`/`tamano` sin que la regla los vea como "cambiados".

**Reglas de `huellasVisuales`** (ver el modelo de datos más abajo): lectura para
cualquier autenticado; `create`/`update`/`delete` para el artesano dueño
(`artesanoId == request.auth.uid` **y** que ese mismo uid sea el dueño real de
`productos/{productId}` — un `get()` cruzado, para que un artesano no pueda escribir la
huella de un producto ajeno con su propio `artesanoId`) o para un usuario con
`rol == "administrador"` — mismo patrón `.get(campo, default)` que el resto del archivo.

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
| `usuarios/{uid}/foto_perfil` | Foto de perfil (cliente o artesano), un solo archivo — cada subida nueva reemplaza la anterior; su URL queda en `usuarios/{uid}.fotoUrl`. Reglas en `storage.rules`: cualquier autenticado puede leerla, solo el propio `uid` puede escribirla. (`storage.rules` está en la raíz y `firebase.json` ya lo referencia, pero hay que desplegarlo: `firebase deploy --only storage`) |

## Flujos principales

- **Registro** (`RoleSelectionScreen` → `RegisterScreen` → `RegisterViewModel`): primero
  `WelcomeScreen` → "Registrarse" lleva a `RoleSelectionScreen`, que pregunta "¿Cómo quieres
  usar MinkaArt?" ("Soy artesano" / "Soy cliente") y navega a `register/{rol}`.
  `RegisterScreen` ya no tiene selector de rol: muestra "Te registras como …" con un botón
  "Cambiar rol" que vuelve atrás. Valida nombre, correo, contraseña (mínimo 6
  caracteres) y **"Confirmar contraseña"** (si no coinciden: "Las contraseñas no
  coinciden", sin enviar el código; `AdminRegisterScreen` valida lo mismo) para cualquier
  rol; todos los campos de contraseña tienen un ojo para mostrarla/ocultarla; si el rol es `artesano`, exige
  además teléfono, fecha de nacimiento, **al menos un departamento** (selección múltiple,
  `MinkaMultiSelectField`), productos que vende (texto libre) y **al menos una categoría**
  (selección múltiple de las 16 de `CatalogosProducto.CATEGORIAS`; si incluye "Otros"
  aparece "Especifica cuál", obligatorio, que se guarda en `categoriasArtesanalesOtro`).
  Si `tieneRuac == true`, exige número y documento. Debajo del campo de contraseña
  aparece la leyenda "🔒 Tu contraseña se guarda cifrada" (cierta: la contraseña solo la
  guarda Firebase Authentication con hash; ver más abajo). El registro se completa
  en dos pasos, con verificación de correo por código de 6 dígitos generado por Cloud
  Functions (no por el mecanismo nativo de Firebase Auth): **la cuenta de Firebase Auth
  se crea solo después de verificar el código**, así nunca queda una cuenta huérfana en
  Auth si el usuario no termina el proceso.
  1. **`RegisterViewModel.iniciarRegistro`**: tras las validaciones locales, llama a
     `AuthRepository.enviarCodigoVerificacion` (Cloud Function `enviarCodigoVerificacion`),
     que genera un código de 6 dígitos, lo guarda en `codigosVerificacion/{correo}` con
     expiración de 10 minutos y lo envía por correo con la API de Resend.
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

  **Términos y condiciones** (Ley 1581 de 2012 / Decreto 1377 de 2013 / Ley 1480 de 2011):
  todos los registros llevan la casilla obligatoria `CheckboxTerminos` ("Acepto los
  Términos y Condiciones y la Política de Tratamiento de Datos Personales *"; el enlace
  abre `TermsScreen`, `Routes.TERMS`). Sin marcarla no se envía el código de verificación
  ni se crea ninguna cuenta: la validación vive en los ViewModels
  (`RegisterViewModel.iniciarRegistro`/`completarRegistroGoogle`,
  `AdminRegisterViewModel.registrar`, `GoogleSignInViewModel.crearClienteConGoogle`) y
  muestra "Debes aceptar los términos y condiciones para continuar". Al crear el
  documento de `usuarios` se guardan `aceptoTerminos = true`, `terminosAceptadosEn` y
  `versionTerminos` (`VERSION_TERMINOS`) en todos los flujos: correo, administrador,
  cliente con Google y artesano con Google. En el flujo de Google la casilla aparece en
  `RoleSelectionScreen` (antes de "Soy cliente", que crea el documento ahí mismo) y en
  `RegisterScreen` (artesano); si alguien entra con Google desde `RegisterScreen` como
  cliente nuevo sin marcarla, se le avisa y se cierra la sesión de Auth que quedó sin
  documento. `TermsScreen` (texto en `strings.xml`, es/en) cubre: datos que se recogen,
  finalidad, derechos del titular (conocer, actualizar, rectificar, suprimir, revocar),
  canal de contacto y responsable (**placeholders `[correo de contacto]` y `[nombre del
  responsable]` por completar**), el alcance de MinkaArt frente a la Ley 1480 (conecta
  artesanos y compradores, la venta se acuerda por WhatsApp, cada artesano responde por
  información, calidad y precio) y la seguridad de la contraseña.
  **Verificado en el código**: la contraseña nunca se escribe en Firestore — `User` no
  tiene ningún campo de contraseña, `RegisterViewModel` solo la mantiene en memoria
  (`datosPendientes`) hasta llamar a `createUserWithEmailAndPassword`, y las Cloud
  Functions solo manejan el código de verificación (el "password" de
  `functions/src/index.ts` es la API key de Resend, no la de los usuarios).
  Como detalle de implementación, los errores de validación de esos ViewModels llegan a
  la pantalla por `onErrorRes: (Int) -> Unit` (id de recurso, para mostrarlos en el idioma
  de la app sin guardar un `Context` en el ViewModel); los errores de Firebase siguen
  llegando como texto por `onError`.

  **Registro con Google** (botón "Continuar con Google" en `RegisterScreen` y
  `LoginScreen`): `GoogleSignInViewModel` → `AuthRepository.obtenerTokenGoogle` (Credential
  Manager, `getCredentialAsync` con callback) → `iniciarSesionConGoogle` (`GoogleAuthProvider`)
  → `consultarRolSiExiste`. Si `usuarios/{uid}` ya existe, se enruta por rol como el login
  normal. Si no existe (usuario nuevo): **cliente** → se crea el documento con el nombre y
  correo de Google y entra a `HOME_CLIENTE`; **artesano** → `RegisterScreen` pasa a "modo
  Google" (sin contraseña ni código de verificación — Google ya verificó el correo), pide
  los mismos datos obligatorios del artesano con la misma validación
  (`RegisterViewModel.validarDatosArtesano`, compartida con el registro por correo) y
  `completarRegistroGoogle` crea el documento. Con Google **nunca** se crea un
  administrador (`crearClienteConGoogle` solo escribe `rol = "cliente"`; el artesano lo
  escribe `RegisterViewModel` con `rol = "artesano"`). Si el correo ya existe con
  contraseña y Firebase lanza `FirebaseAuthUserCollisionException`, se muestra "Este correo
  ya está registrado con contraseña, inicia sesión con tu contraseña". Quien abandona el
  registro en modo Google queda con cuenta en Auth pero sin documento: "Cambiar rol"/
  "Volver" cierran esa sesión, y si igual vuelve a entrar con Google se le pide el rol de
  nuevo.
- **Login** (`LoginScreen` → `LoginViewModel` → `AuthRepository`): autentica con
  Firebase Auth y luego consulta el campo `rol` en `usuarios/{uid}` para decidir el
  destino (`HOME_CLIENTE`, `HOME_ARTESANO` o `ADMIN_RUAC_LIST`), limpiando `LOGIN` del back
  stack. El campo de contraseña tiene ojo para mostrarla. "Continuar con Google" usa el
  mismo enrutado (`irSegunRol`); si la cuenta de Google es nueva (sin documento) lleva a
  `RoleSelectionScreen` con `google=true` antes de crear nada.
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
  un dropdown opcional de 3 valores (o "Sin especificar" → `null`). También hay dos
  dropdowns opcionales, **Color** (`CatalogosProducto.COLORES`) y **Tamaño**
  (`CatalogosProducto.TAMANOS`), con "Sin especificar" → `""`; alimentan los filtros del
  catálogo del cliente. `EditProductScreen`
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
  búsqueda real (el ícono de cámara navega a `Routes.RECOGNITION`, la búsqueda visual
  real — ver "Búsqueda visual" más abajo), una fila de chips de categoría en un
  `LazyRow` con scroll horizontal (armados dinámicamente a partir de los valores
  distintos de `categoria` presentes en los productos cargados, no de una lista fija) y, a
  la derecha de la barra de búsqueda, un **botón de filtros** (`ic_filtros`) con un badge
  con la cantidad de filtros activos (material + color + tamaño; la categoría no cuenta,
  tiene sus propios chips). Reemplaza a la antigua segunda fila de chips de materia
  prima. El botón abre un `ModalBottomSheet` con tres secciones de selección única
  (volver a tocar el elegido lo deselecciona): **Material** (`materiaPrima`), **Color**
  (`color`) y **Tamaño** (`tamano`), todas derivadas de los valores reales presentes en
  los productos cargados (si ningún producto tiene color o tamaño todavía, la sección
  dice "Aún no hay artesanías con este dato"), y dos botones: "Aplicar" (los cambios del
  sheet son un borrador hasta pulsarlo) y "Limpiar filtros". Texto, categoría, material,
  color y tamaño se combinan con AND, en memoria, sin consultas nuevas; un producto
  antiguo sin `color`/`tamano` no aparece al filtrar por ellos pero sí sin filtro.
  Los valores se muestran traducidos (`util/Etiquetas.kt`) pero se comparan siempre por
  el valor en español guardado en Firestore. El feed (`obtenerTodosLosProductosActivos`)
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
  con **AND** junto a la categoría y a los filtros del bottom sheet (material, color,
  tamaño) activos — todos se aplican en memoria
  sobre los productos ya cargados, sin ninguna consulta nueva a Firestore por cada tecla:
  Firestore no tiene búsqueda de texto completo nativa y el tamaño del catálogo no
  justifica un servicio externo (Algolia, extensiones, etc.). El texto escrito se
  "asienta" 300ms después de la última tecla (`LaunchedEffect(textoBusqueda) { delay(300);
  ... }`) antes de recalcular el filtro, y tanto el texto escrito como
  `nombre`/`descripcion` de cada producto se normalizan (sin tildes, minúsculas, vía
  `java.text.Normalizer` del propio JDK) para que "ceramica" encuentre "Cerámica".
- **Búsqueda visual** (`RecognitionScreen` → `RecognitionViewModel`, ícono de cámara del
  buscador de `HomeClienteScreen` — ya no un `PlaceholderScreen`): desde este ajuste, el
  embedding lo calcula **Vertex AI en la nube** y la comparación la hace **Firestore
  vector search**, no el celular — reemplaza al motor híbrido anterior (ORB + MediaPipe
  local, ver más abajo "Motor anterior, en transición"). **No** es clasificación de
  objetos, no reconoce "qué es" una pieza: solo qué tan parecida es (similitud coseno vía
  `DOT_PRODUCT`, porque Vertex AI ya devuelve el vector normalizado a norma 1) a las
  fotos principales del catálogo. Un artesano "viendo como cliente" llega aquí sin ningún
  cambio adicional — la ruta no distingue rol. Cinco estados:
  1. **Captura**: `PreviewView` (CameraX) envuelto en `AndroidView` con un marco guía
     superpuesto, botón de disparo circular, botón de **flash/torch** (arriba a la
     derecha — el proyecto no tiene `material-icons-extended`, así que es un botón de
     texto, mismo estilo que "Galería") o "Galería"
     (`ActivityResultContracts.GetContent()`, mismo patrón que `PublicationScreen`). El
     permiso `CAMERA` se pide al entrar (mismo patrón que `EfectoNotificaciones` con
     `POST_NOTIFICATIONS`); si lo niegan, la vista previa no aparece pero la galería
     sigue disponible. La foto se captura en memoria
     (`ImageCapture.OnImageCapturedCallback` → bytes JPEG → `Bitmap`, corregido por
     `imageInfo.rotationDegrees`) — no hay ningún archivo temporal ni `FileProvider`.
  2. **Recortando** (nuevo, estilo Google Lens): rectángulo ajustable sobre la foto
     tomada/elegida, con una manija en cada esquina (arrastrable) y la opción de mover la
     caja completa — implementado a mano con gestos de Compose
     (`detectDragGestures`), sin ninguna librería de recorte nueva. "Usar foto" recorta el
     `Bitmap` real (`Bitmap.createBitmap`) al rectángulo elegido y continúa; "Cancelar"
     vuelve a Captura.
  3. **Analizando**: miniatura de la foto recortada + `CircularProgressIndicator`,
     mientras corren en cadena: `PreprocesadorImagen.procesar` (en el dispositivo) →
     callable `buscarPorImagen` (Cloud Functions).
  4. **Resultados**: agrupados por artesano (`GrupoArtesano`), cada `ProductoFeedCard`
     (sin tocar — la sigue compartiendo el feed del cliente) con un chip discreto de
     nivel — "Muy parecido" (similitud ≥ `RecognitionViewModel.UMBRAL_MUY_PARECIDO`) o
     "Parecido" — nunca un porcentaje crudo. El nombre del artesano navega a
     `Routes.artisanProfileRoute`, cada producto a `Routes.productDetailRoute`. Los
     candidatos con similitud por debajo de `UMBRAL_MINIMO_COSENO` se descartan del lado
     de la app (el backend ya devuelve como máximo 30, sin filtrar por umbral).
  5. **SinResultados**: "No encontramos artesanías parecidas" + botón para volver a
     Captura.

  **Preprocesamiento en el dispositivo** (`vision/PreprocesadorImagen.kt`, OpenCV, clase
  separada — ya no hace ningún matching, solo prepara la imagen):
  - **Nitidez**: varianza del Laplaciano; por debajo de `UMBRAL_VARIANZA_LAPLACIANO`, la
    foto se rechaza y se pide repetirla ("La foto salió borrosa...").
  - **Brillo**: por debajo de `UMBRAL_BRILLO_MUY_OSCURO`, se rechaza pidiendo repetir con
    flash o más luz; entre ese umbral y `UMBRAL_BRILLO_POCA_LUZ` (poca luz, pero
    salvable), se corrige con **CLAHE** sobre el canal L en espacio LAB (contraste, sin
    tocar el color) — por encima de `UMBRAL_BRILLO_POCA_LUZ` no se toca nada.
  - Redimensiona al lado máximo esperado por el backend (800px) y devuelve un JPEG en
    bytes. Los tres umbrales son un punto de partida documentado, sin calibrar todavía
    con fotos reales — ver "Evaluación" más abajo.

  **Backend** (`functions/src/index.ts`, ver Modelo de datos —
  `embeddingsVisuales`/`limitesBusquedaVisual` — para el detalle de colecciones): el
  callable `buscarPorImagen` (`onCall`, solo autenticados) recibe el JPEG en base64
  (rechaza si pesa más de lo esperado para un lado de 800px), aplica un límite de 30
  búsquedas/día por usuario (el rol `"administrador"` queda exento, lo necesita para la
  evaluación), le pide el embedding a Vertex AI y hace `Query.findNearest` sobre
  `embeddingsVisuales` (`DOT_PRODUCT`, límite 30), descarta los productos inactivos y
  devuelve `[{productId, artesanoId, similitud}]` — `RecognitionViewModel` resuelve los
  datos completos con `ProductRepository.obtenerProductosPorIds`. Todo esto requiere
  internet: sin conexión, la pantalla muestra un mensaje claro en vez de quedarse
  cargando.

  **Diagrama del flujo** (búsqueda en vivo, cliente):
  ```
  RecognitionScreen (Captura)
      │ foto (cámara/galería)
      ▼
  PantallaRecorte — el usuario ajusta el rectángulo
      │ Bitmap recortado
      ▼
  PreprocesadorImagen (OpenCV, en el dispositivo)
      │  ├─ borrosa / muy oscura → "repite la foto" (no llega al backend)
      │  └─ ok (con CLAHE si hace falta) → JPEG en bytes
      ▼
  buscarPorImagen (Cloud Function, onCall)
      │  ├─ límite diario / tamaño de imagen → error claro
      │  ├─ Vertex AI multimodalembedding@001 → vector de 1408 dimensiones
      │  └─ Firestore findNearest sobre embeddingsVisuales (DOT_PRODUCT, límite 30)
      │        → descarta inactivos → [{productId, artesanoId, similitud}]
      ▼
  RecognitionViewModel
      │  ├─ filtra por UMBRAL_MINIMO_COSENO
      │  ├─ ProductRepository.obtenerProductosPorIds (datos completos)
      │  └─ AuthRepository.obtenerUsuarioPorId (nombre de cada artesano)
      ▼
  RecognitionScreen (Resultados) — agrupados por artesano, con chip
  "Muy parecido"/"Parecido"
  ```

  Y el flujo que mantiene `embeddingsVisuales` al día (automático, sin acción del
  artesano ni del administrador):
  ```
  ProductViewModel.publicarProducto / actualizarProducto
      │ escribe/actualiza productos/{productId}
      ▼
  sincronizarEmbeddingVisual (trigger onDocumentWritten)
      │  ├─ imagenes cambiaron → descarga imagenes[0] de Storage → Vertex AI → guarda
      │  │    el vector en embeddingsVisuales/{productId}
      │  ├─ solo cambió "activo" → actualiza ese campo nada más (sin llamar a Vertex AI)
      │  └─ producto borrado → borra embeddingsVisuales/{productId}
      ▼
  embeddingsVisuales/{productId} listo para findNearest
  ```

- **Motor anterior, en transición (`huellasVisuales`, `MotorHuellaVisual`,
  `GeneradorHuellas`)**: **ya no participa en la búsqueda real** — se mantiene solo
  porque `AdminVisualEvalScreen` todavía lo usa como línea base de comparación contra el
  motor nuevo, y está pendiente de eliminar (junto con las dependencias de MediaPipe y
  el matching de OpenCV/ORB) en cuanto esa comparación se confirme con fotos reales (ver
  "Estado actual y pendientes"). Mientras tanto sigue generándose exactamente igual que
  antes, siempre en automático, sin que nadie tenga que pulsar nada — dos caminos
  alimentan `huellasVisuales` a través de `vision/GeneradorHuellas.kt`:
  - **Publicar/editar** (`ProductViewModel.publicarProducto`/`actualizarProducto`): calcula
    y guarda la huella en segundo plano justo después de guardar el producto — si falla,
    el producto se publica/actualiza igual, solo se registra el error (`Log.w`).
  - **Autocompletado del artesano** (`HomeArtesanoScreen`, silencioso): al abrir el Home,
    calcula la huella de sus productos que no la tengan (o con `version` vieja).

  `MotorHuellaVisual.kt` combina ORB + histograma HSV (para "Solo ORB", la línea base
  original) con un embedding de MediaPipe Image Embedder (MobileNet-V3, cuantizado a 1
  byte/dimensión) para "Híbrido" — ver el código para el detalle completo de pesos y
  umbrales (`PESO_INLIERS_ORB`, `UMBRAL_MINIMO_COSENO` de este motor, etc.), documentado
  ahí mismo. Todo corre en un `Executor` único, sin corrutinas/Flow, mismo patrón que el
  resto de `vision/`.

- **Evaluación de la búsqueda visual** (`AdminVisualEvalScreen` →
  `VisualEvalViewModel`, solo rol administrador — botón "Evaluar búsqueda visual" bajo el
  encabezado de `AdminRuacListScreen`): compara **tres** métodos sobre el mismo conjunto
  de fotos de prueba, midiendo si el producto correcto o uno de su misma categoría
  aparece en el top 1/5/10 de cada uno — pensada para el sustento del documento de grado,
  no para uso en producción:
  - **Solo ORB** y **Híbrido**: el motor anterior, comparado localmente en el
    dispositivo (sin red), igual que siempre.
  - **Nuevo — Vertex AI**: el mismo camino real que usa `RecognitionScreen` —
    `PreprocesadorImagen.procesar` + una llamada real al callable `buscarPorImagen` por
    cada foto (consume presupuesto de Vertex AI, ~$0.0001/foto). Una foto que el
    preprocesador rechaza (borrosa/muy oscura) cuenta como fallo de este método para esa
    foto, no se omite — así el número refleja lo que de verdad vería un usuario. Se
    reporta además **separado por condición de luz** (buena luz / poca luz), detectada
    automáticamente por `PreprocesadorImagen` según el brillo de cada foto — no hace
    falta etiquetarla a mano en `etiquetas.csv`.

  **No genera ni toca ningún embedding/huella** (nunca escribe en `huellasVisuales` ni en
  `embeddingsVisuales`): solo lee lo que ya existe y compara, así que no es una forma
  alternativa de "activar" el reconocimiento — eso ya es automático (huellas) o
  automático del lado del backend (embeddings, ver `sincronizarEmbeddingVisual`).
  - Las fotos y el archivo `etiquetas.csv` (formato exacto en su propio `README.txt`) van
    en `app/src/debug/assets/evaluacion/` — solo se empaquetan en el APK de **debug**
    (`assembleDebug`/`installDebug`), nunca en el de release. A la fecha de este ajuste,
    esa carpeta no tiene fotos reales todavía (queda pendiente que se agreguen).
  - Además de la tabla top 1/5/10 por método (y por condición de luz para el método
    nuevo), mide el tiempo promedio de cada método (embedding/búsqueda locales del motor
    anterior vs. el viaje completo — preprocesar + red + Vertex AI — del nuevo) y
    conserva el **chequeo de cuantización** del motor anterior (compara el coseno con el
    embedding `float` de MediaPipe sin cuantizar contra el cuantizado que se guarda,
    para justificar o no `quantize = true` — no aplica al motor nuevo, que no cuantiza
    nada: Vertex AI ya devuelve `float32` y Firestore lo guarda tal cual).
  - El reporte se muestra en pantalla y además se registra completo en Logcat (tag
    `EvalVisual`) en formato Markdown, listo para copiar al documento de grado.
- **Aviso de actualización** (`ActualizacionViewModel` + `DialogoActualizacion`, montados en
  `AppNavigation`, sin ruta propia): la app se distribuye por APK, no por Play Store. Al
  arrancar lee `configuracion/version` (una vez por arranque) y compara con el `versionCode`
  instalado (`PackageManager`). Si `versionCode < versionMinima` la ventana es **obligatoria**;
  si solo es menor que `versionActual`, es **opcional** ("Más tarde"). El botón "Actualizar"
  abre `urlDescarga` y el usuario instala el APK a mano (Android no deja hacerlo solo). Sin
  documento, sin URL, sin internet o con error de lectura no se muestra nada.
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
  (**foto de perfil del artesano** con recorte circular vía Coil si `fotoUrl` no es
  `null`, el ícono por defecto si no; nombre, contacto, insignia RUAC en miniatura si
  `ruacEstado == "verificado"`) que navega a su
  `Routes.artisanProfileRoute(artesanoId)`. Dos `BarraValoracion` (calidad y confianza,
  con degradado de marca y conteo de verificaciones — reemplazan a `ConfianzaDots`,
  ver "Diseño y marca"). El botón "Contactar artesano" usa `contactoPublico` (con `telefono` como
  respaldo si el artesano no lo configuró), limpia el número a solo dígitos y abre
  `https://wa.me/<numero>?text=<mensaje>` con un intent `ACTION_VIEW`; si no hay ninguna
  app que lo resuelva, cae a `ACTION_DIAL` con el número tal cual. El mensaje incluye el
  **nombre y el correo** de quien contacta (`ProductDetailViewModel.obtenerDatosDelContacto`,
  que lee su documento de `usuarios`; si falla, usa el correo de Firebase Auth) para que el
  artesano pueda registrar la venta en `RegisterSaleScreen` con ese correo.
- **Campos obligatorios**: las etiquetas de los campos obligatorios llevan ` *` y cada
  formulario muestra la leyenda "* Campos obligatorios": Registro (incluido el de
  administrador), Publicar producto y Editar producto (todo salvo "Clasificación (opcional)";
  también el contador de imágenes). Las validaciones siguen viviendo en los ViewModel.
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
  `HomeArtesanoScreen`. **Foto de perfil**: cuando lo ve su dueño, el avatar lleva una
  insignia de cámara que abre el selector de imágenes (`GetContent()`); la foto se sube
  con `AuthRepository.subirFotoPerfil` a `usuarios/{uid}/foto_perfil` y su URL se guarda
  en `usuarios/{uid}.fotoUrl` (ver Modelo de datos y Storage); sin foto se muestra el
  ícono por defecto. También solo para el dueño, un ícono de engranaje en la cabecera
  abre `Routes.ACCOUNT_SETTINGS` (idioma).
- **Perfil del cliente y Likes** (`ClientProfileScreen`, `LikesScreen`): el perfil del
  cliente es un menú (información básica — solo `nombre`, editable en un diálogo; mis
  likes; historial de compras; configuración de la cuenta —idioma—; cerrar sesión) con
  el mismo avatar con foto de perfil y la misma insignia de cámara que el artesano
  (`ClientProfileViewModel.actualizarFotoPerfil` reutiliza `AuthRepository.subirFotoPerfil`).
  "Mis likes" lista los productos que el
  usuario marcó con like: primero `obtenerProductosLikeadosPorUsuario` (ids, vía
  `collectionGroup`), luego `obtenerProductosPorIds` (en bloques de 30 con
  `whereIn(FieldPath.documentId(), ...)`) para traerlos de una vez en vez de una lectura
  por producto. "Cerrar sesión" llama a `AuthRepository.cerrarSesion()` (que además
  limpia `SesionLocal`) y navega a `Routes.WELCOME` limpiando todo el back stack
  (`popUpTo(0) { inclusive = true }`).
- **Sesión persistente**: Firebase Auth ya conserva la sesión en el dispositivo; lo que
  faltaba era usarla al arrancar. `AppNavigation` obtiene su `startDestination` de
  `SesionViewModel.destinoInicial()`: sin `currentUser` → `WELCOME`; con usuario pero
  "Recuérdame" desmarcado → cierra la sesión y `WELCOME`; con rol guardado →
  `Routes.homeDeRol(rol)` al instante (funciona sin internet). Con usuario sin rol guardado
  (sesión anterior a este cambio) arranca en `WELCOME` y un `LaunchedEffect` llama a
  `recuperarRol`: una consulta a `usuarios/{uid}` vía `consultarRolSiExiste` que guarda el
  rol y pasa al Home (solo si la persona sigue en `WELCOME`), o —si no hay documento
  (registro con Google a medias) o falla— cierra la sesión. El "almacén local" es `util/SesionLocal.kt`: `SharedPreferences`
  `sesion_minka` con `rol` y `recordar` (sin Room ni DataStore, sin dependencias nuevas),
  inicializado en `MainActivity`. Lo escriben `LoginScreen.irSegunRol` (con el valor de la
  casilla "Recuérdame") y los registros que entran directo a un Home (`RegisterScreen`,
  `RoleSelectionScreen` en modo Google, siempre con `recordar = true`). El registro de
  administrador vuelve a Login, y el registro de cliente/artesano por correo no guarda
  nada: en ambos casos el primer arranque usa la consulta de respaldo.
- **Configuración de idioma (español / inglés)**: la app usa los "idiomas por app" de
  Android con `AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"
  | "es"))` (envuelto en `util/Idioma.kt`). Se cambia desde `AccountSettingsScreen`
  (`Routes.ACCOUNT_SETTINGS`, "Idioma / Language": accesible desde `ClientProfileScreen`
  → "Configuración de la cuenta" y, para el dueño, desde el engranaje de la cabecera de
  `ArtisanProfileScreen`) o desde el selector "ES | EN" de `WelcomeScreen`, que se ve antes
  de iniciar sesión. Cambiarlo recrea la Activity (`MainActivity` extiende
  `AppCompatActivity`; el tema pasó a `Theme.AppCompat.Light.NoActionBar`, que AppCompat
  exige — Compose y `enableEdgeToEdge()` siguen igual); `AppLocalesMetadataHolderService`
  (`autoStoreLocales=true` en el manifest) hace que se recuerde, y
  `res/xml/locales_config.xml` lo ofrece también en Ajustes del sistema > Idioma por app.
  Los textos viven en `res/values/strings.xml` (español, por defecto) y
  `res/values-en/strings.xml`; los Composables usan `stringResource(...)`. **Lo que se
  guarda en Firestore sigue en español** (categorías, departamentos, materias primas,
  colores, tamaños, clasificaciones, niveles de valoración, `rol`): solo se traduce la
  etiqueta que se muestra, con mapas valor → `@StringRes` en `CatalogosProducto`
  (`etiquetaCategoria`, `etiquetaMateriaPrima`, `etiquetaColor`, `etiquetaTamano`,
  `etiquetaClasificacion`) y los helpers de `util/Etiquetas.kt`; un valor sin traducción
  (texto libre de un producto antiguo, un departamento) se muestra tal cual. Los ViewModels
  de registro no guardan ningún `Context`: reportan sus errores de validación con ids de
  recurso (`onErrorRes`). El contenido que escriben los artesanos no se traduce (la propia
  `AccountSettingsScreen` lo avisa). Qué pantallas faltan por traducir: ver "Estado actual
  y pendientes".
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
  iniciar sesión — es su único destino, no tiene Home propio; ahí mismo tiene el botón
  "Cerrar sesión" (`AdminRuacListViewModel.cerrarSesion`, vuelve a `Routes.WELCOME`). Ahí ve todos los artesanos
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
  con el producto ya elegido (de solo lectura). **Ya no hay buscador de clientes**: el
  artesano escribe el **correo completo** del cliente y la pantalla lo compara (sin
  distinguir mayúsculas) contra los usuarios con `rol == "cliente"`
  (`AuthRepository.obtenerClientes`, cargados una vez al abrir). Debajo del campo muestra
  "Correo correcto · <nombre>" en verde (`VerdeConfirmacion`) o "No existe" en rojo
  (`colorScheme.error`; solo cuando la lista de clientes ya cargó). "Confirmar venta" queda
  deshabilitado hasta que el correo exista; luego elige una fecha (por defecto hoy) y
  confirma → `SaleRepository.registrarVenta`. El correo llega al artesano en el mensaje de
  WhatsApp que arma "Contactar artesano" (ver Detalle de producto).

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
| `VerdeConfirmacion` | `#2E7D32` | Mensajes de validación correcta (p. ej. "Correo correcto" en Registrar venta) |
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

`RoleSelectionScreen` ya no usa `GlassCard` para las dos opciones: son tarjetas de **fondo
blanco sólido** con texto e ícono en vinotinto (`Guinda` el título, `Borgona` la
descripción y el ícono) para que se lean sobre el patrón textil. Los íconos son vectores
propios, `res/drawable/ic_artesano.xml` (vasija) e `ic_cliente.xml` (bolsa de compras),
tintados desde el `Icon`; el filtro del catálogo usa `ic_filtros.xml` — no hay
`material-icons-extended`, así que todo ícono que no esté en el set básico va como vector
propio (igual que `ic_visibility`).

`Type.kt` usa **Fraunces** (titulares, saludos, logotipo) y **Plus Jakarta Sans**
(interfaz) como fuentes reales, no como sustitutos temporales. Google Fonts no distribuye
ninguna de las dos como archivos estáticos por peso, así que cada `FontFamily` carga un
único archivo variable (`res/font/fraunces_variable.ttf`,
`res/font/plus_jakarta_sans_variable.ttf`) varias veces, una por peso, usando
`FontVariation.Settings` sobre el eje `wght` (API experimental de Compose,
`@OptIn(ExperimentalTextApi::class)` en el propio archivo). Fraunces fija además el eje
óptico (`opsz`) en un valor alto porque solo se usa en titulares.

**Logo y eslogan**: el logo oficial es `res/drawable/logo1.png` y el eslogan es
"Cultura viva a tu alcance" (`R.string.eslogan`, con traducción al inglés; el
`contentDescription` es `logo_descripcion`). En Bienvenida el logo va **sin fondo** (`LogoSinFondo`: usa `logo2.png` y su luminosidad como transparencia, letras en `Marfil` sobre la foto). En los demás sitios el logo se muestra **recortado en círculo**
(`LogoCircular`, con una ampliación de 1,3× dentro del círculo para que "MINKA ART" se lea
en tamaños pequeños; el PNG es blanco sobre vinotinto, no se le quitó el fondo). Aparece en
Bienvenida (`LogoSinFondo` + eslogan debajo) y en el encabezado de Home cliente y Home artesano (solo logo + eslogan) y del
panel del administrador "Solicitudes de RUAC" (con su título) — `LogoMinkaEncabezado`.
Login, Selección de rol y Registro no llevan logo. No hay Splash Screen.

**Ícono de la app**: ícono adaptativo (`mipmap-anydpi/ic_launcher.xml` y
`ic_launcher_round.xml`) con `ic_launcher_foreground_minka.xml` (`logo2.png`, el logo en
1024×1024, con `inset` del 4 %: las letras quedan dentro de la zona segura de 66 dp)
sobre `ic_launcher_background.xml`, un color sólido `#4B1022` muestreado de los bordes del
logo. No tiene capa `monochrome` (con íconos
temáticos de Android 13+ el sistema usa el ícono normal). `ic_launcher_foreground.xml` (el
vector heredado) **se conserva solo como ícono pequeño de las notificaciones**
(`MinkaMessagingService.setSmallIcon`, que debe ser monocromo con transparencia). Los
`.webp` de `mipmap-mdpi…xxxhdpi` son heredados y no se usan (`minSdk` 31 siempre toma el
ícono adaptativo). `logo1.png` (el que usan las pantallas) mide solo 416×431 px, por eso el
ícono usa `logo2.png` (1024×1024, convertido desde un `.jfif`; el original quedó en la raíz
del repositorio como `logo2_original.jfif`, fuera de `res/` porque Android no admite esa
extensión). Si se quiere, `LogoCircular` puede pasar a usar `logo2` también.

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
(`npm install -g firebase-tools`) y, una sola vez, una cuenta de [Resend](https://resend.com) y su API key
(`re_...`), registrada como secreto:

```powershell
firebase functions:secrets:set RESEND_API_KEY
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

### Publicar una actualización (APK)

El nombre que se ve bajo el ícono es **Minka Art** (`app_name` en los dos `strings.xml`; el
tema y `rootProject.name` conservan `MinkaArt_bta` como identificador interno).

**Firma de release**: la llave es `C:\Users\elkin\MinkaArt-llave\minkaart.jks` (alias
`minkaart`), **fuera del repositorio y con copia de seguridad** — si se pierde, nadie podrá
instalar la siguiente versión encima de la anterior. `app/build.gradle.kts` la lee de
`keystore.properties` en la raíz (ignorado por git, junto con `*.jks`; la plantilla es
`keystore.properties.example`); sin ese archivo el release sale sin firmar. Se compila con
`.\gradlew.bat assembleRelease` → `app/build/outputs/apk/release/app-release.apk` (~149 MB).
SHA-1 de la llave: `BA:A0:7F:76:82:FB:E3:D4:1C:58:74:6A:15:D8:9D:18:62:25:83:E8`
(SHA-256 `b533d3cf…626367`). **Hay que registrar ese SHA-1 en Firebase** (Project settings →
app Android → *Add fingerprint*) y bajar el `google-services.json` nuevo, o "Continuar con
Google" falla en el APK de release. Quien tenga instalada una versión *debug* debe
desinstalarla una vez, porque la firma cambia. La firma no elimina el aviso de "orígenes
desconocidos" de Android: eso solo lo quita Play Store.

Cada versión nueva:
1. Subir `versionCode` (y `versionName`) en `app/build.gradle.kts` (hoy `1` / `"1.0"`).
2. Generar el APK firmado con la llave de release.
3. Subir el APK (Drive con "cualquiera con el enlace", Storage, etc.) y copiar el enlace.
4. En Firestore, editar `configuracion/version`: `versionActual` = el nuevo `versionCode` y
   `urlDescarga` = el enlace; subir `versionMinima` solo si debe ser obligatoria. Ver
   "Firestore — documento `configuracion/version`" y "Aviso de actualización".

### Inicio de sesión con Google

Pasos manuales en Firebase Console (el código ya está, pero no funciona sin esto):

1. **Authentication → Sign-in method**: activar el proveedor **Google** (con un correo de
   soporte) y guardar.
2. **Project settings → tu app Android**: agregar la huella **SHA-1** (y SHA-256) del
   keystore con el que se firma — para debug, `.gradlew.bat signingReport`; si se publica
   un release, también la de ese keystore.
3. **Descargar de nuevo `google-services.json`** y reemplazar `app/google-services.json`.
   Debe traer un `oauth_client` con `client_type: 3`: de ahí el plugin de google-services
   genera `R.string.default_web_client_id`, que usa `GoogleSignInViewModel`. **Sin ese
   archivo actualizado el proyecto no compila** (`default_web_client_id` no existe).
4. El dispositivo/emulador necesita una cuenta de Google y Google Play Services.

### Configuración de Google Cloud (búsqueda visual)

El proyecto `minka-art` necesita, además de lo que ya configura Firebase, lo siguiente en
Google Cloud (ya hecho en este proyecto; se documenta para poder reproducirlo en otro):

1. **Plan de facturación Blaze** (pago por uso) — Vertex AI no funciona en el plan
   gratuito Spark.
2. **API de Vertex AI habilitada**: `gcloud services enable aiplatform.googleapis.com
   --project=minka-art`.
3. **Permiso IAM** para la cuenta de servicio que usan las Cloud Functions
   (`{numero_de_proyecto}-compute@developer.gserviceaccount.com`, la que usa Cloud
   Functions v2/Cloud Run por defecto si no se configura una propia):
   `gcloud projects add-iam-policy-binding minka-art
   --member="serviceAccount:{numero_de_proyecto}-compute@developer.gserviceaccount.com"
   --role="roles/aiplatform.user"`.
4. **Alerta de presupuesto**: 12.000 COP/mes (~3 USD, la cuenta de facturación de este
   proyecto está en pesos colombianos, no dólares), avisos al 50/90/100% —
   `gcloud billing budgets create` (API `billingbudgets.googleapis.com`).
5. **Índice vectorial de Firestore**: ver "Firestore —
   `embeddingsVisuales/{productId}`" en Modelo de datos — `gcloud firestore indexes
   composite create` (la sintaxis exacta del flag `--field-config` con `vector-config`
   es particular, ver el historial de este ajuste o la documentación oficial de
   Firestore vector search si hay que recrearlo).
6. **Application Default Credentials locales**, solo para correr
   `functions/src/scripts/rellenarEmbeddings.ts` desde una máquina de desarrollo (las
   Cloud Functions desplegadas no lo necesitan, usan su propia cuenta de servicio):
   `gcloud auth application-default login`.

**Costos y límites honestos**: Vertex AI cobra ~$0.0001 por imagen
(`multimodalembedding@001`) — publicar/editar un producto y cada búsqueda visual de un
usuario cuestan eso. El callable `buscarPorImagen` limita a 30 búsquedas/día por usuario
(el rol `"administrador"` queda exento, para poder correr la evaluación). La búsqueda
visual **requiere internet** (antes comparaba en el dispositivo, ahora no) y **solo
encuentra artesanías activas publicadas en Minka Art** — no es reconocimiento de objetos
genérico, no sabe "qué es" una pieza fuera de ese catálogo.

## Estado actual y pendientes

**Implementado**
- Aviso de actualización sin Play Store (`configuracion/version`, ventana opcional u
  obligatoria), botón "Cerrar sesión" del administrador, nombre visible "Minka Art", logo
  sin fondo en Bienvenida y barra de búsqueda más compacta en el Home del cliente.
- Términos y Condiciones obligatorios en todos los registros (con `aceptoTerminos`,
  `terminosAceptadosEn` y `versionTerminos` guardados), registro de artesano con
  departamentos y categorías de selección múltiple (`MinkaMultiSelectField`), tarjetas
  blancas en `RoleSelectionScreen`, idioma español/inglés (parcial, ver pendientes),
  filtros de material/color/tamaño en un bottom sheet del catálogo del cliente, campos
  `color`/`tamano` en Publicar/Editar producto, y foto de perfil del artesano en el
  Detalle de producto — ver "Términos y condiciones", "Configuración de idioma" y
  "Catálogo del cliente" en Flujos principales.
- Selección de rol antes del registro (`RoleSelectionScreen`), ojo para ver la contraseña,
  "Confirmar contraseña" (Registro y Admin) y "Continuar con Google" en Login y Registro.
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
- Búsqueda visual con **Vertex AI + Firestore vector search** (`RecognitionScreen`, ya
  no un `PlaceholderScreen`): reemplaza al motor híbrido anterior que corría en el
  dispositivo (ORB + MediaPipe) — ahora OpenCV solo preprocesa la foto en el celular
  (`PreprocesadorImagen`: CLAHE, detección de borrosidad/oscuridad, recorte ajustable
  estilo Google Lens, más el botón de flash) y Vertex AI (`multimodalembedding@001`)
  calcula el embedding en la nube, comparado con Firestore `findNearest` desde la Cloud
  Function `buscarPorImagen` (ver Modelo de datos y "Búsqueda visual" en Flujos
  principales). Los embeddings de cada producto se generan solos al publicar/editar
  (trigger `sincronizarEmbeddingVisual`); se corrió además un relleno único para los 103
  productos que ya existían (0 errores). Incluye una pantalla de evaluación comparativa
  para el rol administrador con **tres** métodos (Solo ORB / Híbrido, el motor anterior,
  vs. Nuevo — Vertex AI, separado por condición de luz) — ver "Evaluación de la búsqueda
  visual" en Flujos principales.

**Pendiente / placeholder**
- **Aviso de actualización por desplegar**: la regla de `configuracion/version` (archivo `y`)
  hay que desplegarla (`firebase deploy --only firestore:rules`) y el documento crearlo a
  mano en Firebase Console; sin eso no sale ninguna ventana. La instalación del APK la hace
  el usuario a mano (descarga + "orígenes desconocidos"); solo se compiló, no se probó en
  dispositivo.
- **Google Sign-In: configurado en el repo pero sin probar en dispositivo**:
  `app/google-services.json` ya trae los `oauth_client` (tipos 1 y 3), así que
  `R.string.default_web_client_id` existe y `assembleDebug` compila (verificado en el
  ajuste de términos/idiomas). Aun así los pasos de Firebase Console de "Inicio de sesión
  con Google" (proveedor activado, SHA-1) deben estar hechos para que funcione de verdad, y
  queda por verificar el caso de un correo que ya existe con contraseña — como el registro
  por código no marca `emailVerified` en Firebase Auth, Firebase podría reemplazar ese
  proveedor en vez de lanzar la colisión.
- **Regla de Firestore permisiva en `usuarios`** (archivo `y`): `allow create` solo exige
  `request.auth.uid == userId`, sin restringir `rol`, así que desde fuera de la app se
  podría crear un documento propio con `rol = "administrador"` (también antes de Google).
  La app nunca lo hace con Google, pero la regla no lo impide; cerrarlo exige mover la
  validación del PIN de administrador al servidor.
- `ProfileScreen.kt` sigue siendo un `Text()` de una línea — ya no lo usa ninguna
  pantalla (`ARTISAN_PROFILE`/`CLIENT_PROFILE` lo reemplazaron), se dejó sin tocar ni
  eliminar por quedar fuera del alcance de la Fase 4.
- `Routes.ACCOUNT_SETTINGS` ya no es un placeholder: `AccountSettingsScreen` es la Configuración de cliente (solo nombre/correo, idioma y ayuda) y de artesano (completa: info básica → `EDIT_PROFILE`, cambio de contraseña  por correo de Firebase, switches de notificaciones push, idioma, ayuda, cerrar sesión; el  artesano suma "Mi insignia RUAC" y "Mi taller", que lleva a su perfil público). Los  switches de notificaciones se guardan solo en `SharedPreferences` del dispositivo  (`preferencias_notificaciones`) y los respeta `MinkaMessagingService`; no se guardan en  Firestore. "Ayuda y soporte" abre la app de correo hacia `CORREO_SOPORTE` (`minka.art.app@gmail.com`, en `AccountSettingsScreen.kt`); si no hay app de correo, muestra un diálogo con el correo y un botón para copiarlo. No hay borrado  de cuenta. `PlaceholderScreen.kt` quedó sin ningún uso, se dejó sin eliminar.
  (`Routes.RECOGNITION`, `RUAC_BADGE` y `PURCHASE_HISTORY` tampoco son placeholders.)
- **El texto legal de `TermsScreen` es una plantilla, no está revisada por un abogado**, y
  conserva los placeholders `[nombre del responsable]` y `[correo de contacto]` (en
  `terms_s1_cuerpo` y `terms_s4_cuerpo` de los dos `strings.xml`). Hay que completarlos y
  validar el texto (Ley 1581 de 2012, Decreto 1377 de 2013, Ley 1480 de 2011; el aviso de
  privacidad y el registro de bases de datos ante la SIC, si aplican, tampoco están
  hechos) antes de publicar la app. Además: la regla de Firestore deja que cualquier
  usuario autenticado lea el documento completo de cualquier otro (incluidos `correo`,
  `fechaNacimiento` y datos de RUAC — ver Modelo de datos); `TermsScreen` solo dice que
  "los demás usuarios con sesión iniciada pueden ver los datos de tu perfil", y conviene
  revisar si eso es compatible con la política real antes de publicar. Los usuarios
  anteriores a este ajuste no aceptaron los términos (`aceptoTerminos == false`) y la app
  no se los pide.
- **Traducción español/inglés incompleta**. Traducidas (todo el texto de UI, errores de
  validación de registro y etiquetas de catálogos): `WelcomeScreen` (con selector ES|EN),
  `LoginScreen`, `RoleSelectionScreen`, `RegisterScreen`, `TermsScreen`,
  `AccountSettingsScreen`, `HomeClienteScreen` (incluido el bottom sheet de filtros y los
  chips de categoría) y `ProductDetailScreen` (más `BarraValoracion` y los textos de
  catálogo que muestran). **Siguen solo en español**: `AdminRegisterScreen` (salvo la
  casilla de términos y la leyenda de contraseña), `HomeArtesanoScreen`,
  `MyProductsScreen`, `PublicationScreen`, `EditProductScreen`, `ArtisanProfileScreen`,
  `ClientProfileScreen`, `LikesScreen`, `NotificationsScreen`, `RecognitionScreen`,
  `RequestRuacScreen`, `RuacBadgeScreen`, `RegisterSaleScreen`, `RateSaleScreen`,
  `PurchaseHistoryScreen`, `QualityTrustScreen`, `AdminRuac*Screen`,
  `AdminVisualEvalScreen`, `ProductoFeedCard`, `BottomNavBar`/`IconoNotificaciones` y las
  etiquetas de nivel de `EscalaValoracion` ("Excelente", "Muy alta"…, que sí se guardan en
  Firestore en español y habría que mapear solo al mostrarlas). También quedan en español
  los mensajes de respaldo `error.message ?: "…"` de los repositorios, el texto de las
  notificaciones push/`notificaciones` (lo escriben las Cloud Functions), los mensajes de
  validación de `ProductViewModel` y el mensaje prellenado de WhatsApp que se
  envía al artesano. Los nombres de departamentos son nombres propios y no se traducen. El
  contenido que escriben los artesanos (nombre, descripción e historia de producto, bio)
  **no se traduce**, por diseño. Cambiar de idioma recrea la Activity; el idioma se
  recuerda (AppCompat `autoStoreLocales`). Solo se probó que compila: no se ejecutó en
  dispositivo ni emulador.
- **Reglas de Firestore y Storage por desplegar**: `y` (nuevos `color`/`tamano` en
  `productoBaseIgual`) y `storage.rules` (ya existían los de `usuarios/{uid}/…`) no se
  despliegan solos — sin desplegar `y`, un cliente no podrá dar like/valorar un producto que
  ya tenga `color`/`tamano` guardado.
- **Límites honestos del motor ANTERIOR (`MotorHuellaVisual`, en transición — ya no
  participa en la búsqueda real, ver "Búsqueda visual")**: sigue sin ser reconocimiento
  de objetos —
  no sabe "qué es" una pieza, solo qué tan parecida es (coseno) o qué tan probable es que
  sea la misma (ORB). El modelo del embedder (MobileNet-V3) es **genérico**, entrenado
  sobre ImageNet — no fue afinado (fine-tuning) con fotos de artesanías colombianas, así
  que categorías visualmente parecidas entre sí pero conceptualmente distintas (p. ej.
  ropa, tejidos y joyería con texturas/colores similares) pueden confundirse o
  aparecer como "parecidas" sin serlo. Sigue siendo sensible a la luz, el fondo y el
  ángulo de la foto de consulta (más el histograma/ORB que el embedding, que es más
  robusto a esos cambios — es justamente lo que motivó agregarlo). Todo se compara en el
  dispositivo (no hay índice del lado del servidor), así que el tiempo de búsqueda crece
  con el tamaño del catálogo. Los pesos (`PESO_INLIERS_ORB`/`PESO_HISTOGRAMA`, usados solo
  por la línea base `comparar()`) y los umbrales con nombre
  (`UMBRAL_MINIMO_SIMILITUD`, `UMBRAL_MINIMO_COSENO`, `UMBRAL_MUY_PARECIDO`,
  `UMBRAL_ORB_COINCIDENCIA_EXACTA`) en `MotorHuellaVisual.kt` son un punto de partida
  documentado, no un valor definitivo — la pantalla "Evaluar búsqueda visual" (solo
  administrador, ver Flujos principales) existe justamente para calibrarlos con fotos
  reales; en esta sesión no se corrió con fotos propias (no hay fotos de prueba
  incluidas en el repositorio, ver `app/src/debug/assets/evaluacion/README.txt`) ni se
  midió el tiempo por imagen en un dispositivo o emulador real (no había ninguno
  conectado) — el modelo elegido (MobileNet-V3 **Large**, no Small) es la opción por
  defecto razonada en "Stack técnico", pendiente de confirmar con esa pantalla.
  **Confirmado con pruebas reales, en tres rondas**:
  1. `UMBRAL_MINIMO_COSENO = 0.5` resultó demasiado estricto — dos piezas parecidas pero
     no idénticas (p. ej. dos aretes de iraca distintos) dieron un coseno real por debajo
     de ese umbral, y `buscarMejores` las descartaba por completo en vez de mostrarlas
     como "parecidas". Se corrigió con un mecanismo de **"mejor esfuerzo"**, no con otro
     número adivinado sin poder probarlo: si ningún candidato llega al umbral "de
     confianza", se muestran los candidatos más parecidos que sí tengan embedding
     calculado — nunca se deja la búsqueda vacía mientras el catálogo tenga algo
     remotamente parecido.
  2. Con ese modo ya mostrando resultados, apareció el problema de fondo que se venía
     anunciando: al fotografiar una mochila no publicada, "mejor esfuerzo" también
     mostraba un sombrero y un arete como "parecidos" — categorías sin relación. La
     causa **no es un bug de lógica, es del modelo**: MobileNet-V3 es un clasificador
     genérico entrenado sobre ImageNet, no fue afinado (fine-tuning) ni entrenado con
     una función de pérdida de tipo contraste/tripletas para que categorías distintas
     queden "lejos" en similitud coseno — es un fenómeno conocido de los embeddings de
     clasificadores genéricos: dos fotos de producto cualquiera comparten suficientes
     estadísticas visuales de bajo nivel (fondo, iluminación, composición) para dar un
     coseno "moderado" sin relación semántica real. Se mitigó (no se resolvió de raíz, a
     pedido explícito de resolverlo del lado de la app por ahora) combinando el coseno
     con el histograma HSV de la huella (peso 0.75/0.25,
     `UMBRAL_MINIMO_MEJOR_ESFUERZO`) para descartar los casos más evidentes de mezcla de
     categorías — el color no distingue todo, pero ayuda.
  3. El histograma seguía sin ser suficiente — el usuario pidió explícitamente
     "identificar el objeto y relacionarlo con la categoría". Se probó una **votación de
     categoría por k-vecinos** (sin entrenar ningún modelo nuevo: los candidatos más
     parecidos por coseno votaban con la `categoria` que ya tiene cada producto,
     ponderado por su propio coseno, y si la votación era clara se descartaba cualquier
     candidato de otra categoría) — **se revirtió**, porque en la práctica empeoró los
     resultados: con un embedder genérico y ruidoso, los k-vecinos más cercanos por
     coseno no son necesariamente de la categoría correcta, así que la votación podía
     "confirmar" con aparente seguridad una categoría **equivocada** y de ahí descartaba
     precisamente los candidatos que sí eran correctos — peor que no filtrar nada. Queda
     como lección documentada: una votación k-NN solo es tan confiable como el espacio
     de embeddings sobre el que vota, y este embedder genérico no separa bien las
     categorías de artesanía como para sostener ese nivel de confianza. `buscarMejores`
     volvió a depender solo del puntaje combinado coseno + histograma (punto 2). **La
     solución de fondo sigue pendiente**: entrenar o afinar (transfer learning) un
     embedder propio con fotos de artesanía reales, categorizadas, con una función de
     pérdida de tipo métrico/tripletas (no una votación sobre un embedding genérico) —
     un proyecto de Python/TensorFlow aparte de este repositorio Android, con un dataset
     etiquetado más grande que las ~100 fotos actuales y luego exportado a `.tflite` con
     el formato de metadatos que espera el Image Embedder de MediaPipe — no se emprendió
     en este ajuste, a pedido explícito de resolverlo del lado de la app por ahora.

  Sigue pendiente además ajustar el valor de `UMBRAL_MINIMO_COSENO` en sí con la pantalla
  de evaluación y fotos reales; hasta entonces, prácticamente toda búsqueda sin una
  coincidencia exacta clara cae en el modo "mejor esfuerzo".
- **Límites honestos del motor NUEVO (Vertex AI + Firestore vector search)**: sigue sin
  ser reconocimiento de objetos, mismo alcance que el motor anterior — solo similitud
  visual, no "qué es" una pieza. `multimodalembedding@001` es un modelo genérico de
  Google (no afinado con fotos de artesanías colombianas), así que el mismo fenómeno
  documentado arriba (categorías visualmente parecidas pero distintas confundiéndose)
  puede seguir dándose — no se descarta hasta correr la evaluación con fotos reales
  (pendiente, ver el punto siguiente). Los umbrales de `RecognitionViewModel`
  (`UMBRAL_MINIMO_COSENO = 0.5`, `UMBRAL_MUY_PARECIDO = 0.75`) son los mismos valores que
  ya traía el motor anterior, copiados como punto de partida — **no** están calibrados
  para el espacio de embeddings de Vertex AI, que es un modelo distinto; tampoco lo están
  los tres umbrales de `PreprocesadorImagen`
  (`UMBRAL_VARIANZA_LAPLACIANO`/`UMBRAL_BRILLO_MUY_OSCURO`/`UMBRAL_BRILLO_POCA_LUZ`).
  Solo encuentra la **imagen principal** de cada producto (`imagenes[0]`) — si la mejor
  coincidencia visual está en una foto secundaria, no participa en la comparación
  (decisión de costo/alcance, ver "Firestore — `embeddingsVisuales`" en Modelo de
  datos). Requiere internet siempre (el motor anterior comparaba en el dispositivo, este
  no) y tiene un límite de 30 búsquedas/día por usuario.
- **Limpieza pendiente del motor anterior**: `HuellaVisual.kt`, `VisualSearchRepository`,
  `MotorHuellaVisual`, `GeneradorHuellas`, la colección `huellasVisuales` y sus reglas, y
  las dependencias de MediaPipe (`com.google.mediapipe:tasks-vision`,
  `assets/mobilenet_v3_large.tflite`) siguen en el repositorio a propósito — se dejaron
  para que `AdminVisualEvalScreen` pudiera seguir comparando el motor nuevo contra el
  anterior. Están pendientes de eliminar (con eso baja también el peso del APK, ver
  "Stack técnico") en cuanto esa comparación se corra con fotos reales y se confirme que
  el motor nuevo es al menos igual de bueno — a la fecha de este ajuste, esa comparación
  todavía no se corrió (no hay fotos de prueba en el repositorio, ver
  `app/src/debug/assets/evaluacion/README.txt`).
- Foto de perfil: implementada para cliente y artesano (`User.fotoUrl`, Storage
  `usuarios/{uid}/foto_perfil`), se muestra en `ClientProfileScreen`, `ArtisanProfileScreen`
  y en la fila del artesano de `ProductDetailScreen`. Pendiente: no se recorta ni se
  comprime la imagen antes de subirla, no se borra la foto anterior al reemplazarla (se
  sobrescribe el mismo archivo) y no se muestra en el feed ni en las notificaciones.
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
- Los correos (código de verificación y cambio de contraseña) se envían por el SMTP de Gmail
  (`PROVEEDOR_CORREO = "gmail"`, ~500 correos/día). Resend está cableado pero **sin dominio
  verificado solo entrega a `minka.art.app@gmail.com`** (y no deja enviar desde `@gmail.com`);
  para usarlo hay que verificar un dominio (DNS SPF/DKIM), cambiar `REMITENTE` y poner
  `PROVEEDOR_CORREO = "resend"`. Las respuestas van a
  `minka.art.app@gmail.com` (`RESPONDER_A`).
- La cuenta de Firebase Auth creada al final del registro queda con `emailVerified =
  false`, porque la verificación la hace la Cloud Function contra Firestore, no el
  mecanismo nativo de Firebase Auth. `isEmailVerified` no se comprueba en ningún punto
  de la app (login, publicación, etc.), así que hoy esto no bloquea nada, pero conviene
  tenerlo presente si en el futuro se agrega alguna comprobación basada en ese campo.
- No hay tests unitarios/instrumentados reales: solo quedan las plantillas por defecto de
  Android Studio (`ExampleUnitTest.kt`, `ExampleInstrumentedTest.kt`). La única
  herramienta de verificación real del proyecto es la pantalla "Evaluar búsqueda visual"
  (solo administrador, ver Flujos principales) — mide el motor nuevo (Vertex AI) contra
  el anterior (Solo ORB/Híbrido) con fotos de prueba, pero es una pantalla de la app, no
  un test que corra en CI ni con `.\gradlew.bat test`.
