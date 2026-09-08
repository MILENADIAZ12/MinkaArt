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
- **Cliente**: se registra con los datos básicos y consume el catálogo (la pantalla de
  cliente hoy es un placeholder, ver [Estado actual y pendientes](#estado-actual-y-pendientes)).

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
| Carga de imágenes | Coil (`coil-compose`) | 2.7.0 |
| `minSdk` / `targetSdk` / `compileSdk` | — | 31 / 36 / 36 |
| JVM target | — | 11 |

Proyecto Firebase: `minka-art` (bucket de Storage `minka-art.firebasestorage.app`),
configurado en `app/google-services.json`, que está versionado en el repositorio.

## Arquitectura

MVVM por capas, con Firebase llamado directamente desde el repositorio (no hay una capa
de red intermedia). **Todo el código asíncrono usa callbacks `onSuccess` / `onError`**,
no corrutinas ni `Flow` — es la convención del proyecto, ver `CLAUDE.md`.

```
MainActivity → MinkaArt_btaTheme → AppNavigation (NavHost)
    Screen (Compose)  →  ViewModel  →  Repository  →  Firebase (Auth / Firestore / Storage)
```

| Capa | Paquete | Responsabilidad |
|---|---|---|
| Entrada | `com.minkaart2.app` (`MainActivity.kt`) | Activity única, `enableEdgeToEdge()`, monta el tema y `AppNavigation`. |
| Navegación | `com.minkaart2.app.navigation` | `Routes` (constantes de ruta) y `AppNavigation` (`NavHost`). |
| UI | `com.minkaart2.app.ui.*` | Un paquete por pantalla (Composables), sin lógica de negocio. |
| Estado | `com.minkaart2.app.viewModel` / `viewmodel` | Validación de formularios y estado observable (`State<T>`). |
| Datos | `com.minkaart2.app.repository` | Llamadas directas a Firebase Auth/Firestore/Storage. |
| Modelo | `com.minkaart2.app.data.model` | `data class` que se serializan/deserializan con Firestore. |
| Tema | `com.minkaart2.app.ui.theme` | Paleta de marca, tipografía, `MaterialTheme`. |

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
│   ├── AuthRepository.kt     # login, rol de usuario, recuperar contraseña
│   └── ProductRepository.kt  # guardar producto (con/sin imagen), listar "mis productos"
├── viewModel/                 # ¡ojo con mayúscula/minúscula, ver más abajo!
│   ├── LoginViewModel.kt
│   ├── ProductViewModel.kt
│   └── RegisterViewModel.kt
└── ui/
    ├── home/
    │   ├── HomeArtesanoScreen.kt
    │   └── HomeClienteScreen.kt   # placeholder
    ├── login/LoginScreen.kt
    ├── profile/ProfileScreen.kt   # placeholder
    ├── publication/
    │   ├── MyProductsScreen.kt
    │   └── PublicationScreen.kt
    ├── register/RegisterScreen.kt
    ├── welcome/WelcomeScreen.kt
    └── theme/
        ├── Color.kt
        ├── Theme.kt
        └── Type.kt
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
| `HOME_CLIENTE` | `home_cliente` | `HomeClienteScreen` (placeholder) | Sí | `LoginScreen` (rol `cliente`) |
| `HOME_ARTESANO` | `home_artesano` | `HomeArtesanoScreen` | Sí | `LoginScreen` (rol `artesano`) |
| `MY_PRODUCTS` | `my_products` | `MyProductsScreen` | Sí | `HomeArtesanoScreen` |
| `PUBLICATION` | `publication` | `PublicationScreen` | Sí | `HomeArtesanoScreen` |
| `PROFILE` | `profile` | `ProfileScreen` (placeholder) | Sí | Nadie navega hacia ella todavía |
| `RECOGNITION` | `recognition` | — | **No** — declarada en `Routes` pero sin `composable {}` | Nadie |

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

Ambos `data class` declaran valores por defecto en todos los campos: es un requisito de
`DocumentSnapshot.toObject()` de Firestore, no un estilo opcional.

### Firebase Storage

| Ruta | Contenido |
|---|---|
| `productos/{artesanoId}/{productoId}/imagen_principal` | Foto principal de una artesanía |
| `ruac/{uid}/documento_ruac` | Documento RUAC adjuntado en el registro |

## Flujos principales

- **Registro** (`RegisterScreen` → `RegisterViewModel`): valida nombre, correo y
  contraseña (mínimo 6 caracteres) para cualquier rol; si el rol es `artesano`, exige
  además teléfono, fecha de nacimiento, región, productos que vende y al menos una
  categoría. Si `tieneRuac == true`, exige número y documento. Crea el usuario en
  Firebase Auth y, si aplica, sube primero el RUAC a Storage antes de escribir el
  documento en `usuarios`. Al terminar solo muestra un `Toast`; no navega a ninguna
  pantalla.
- **Login** (`LoginScreen` → `LoginViewModel` → `AuthRepository`): autentica con
  Firebase Auth y luego consulta el campo `rol` en `usuarios/{uid}` para decidir el
  destino (`HOME_CLIENTE` o `HOME_ARTESANO`), limpiando `LOGIN` del back stack.
- **Publicar artesanía** (`PublicationScreen` → `ProductViewModel` → `ProductRepository`):
  valida todos los campos y que haya una imagen seleccionada, sube la imagen a Storage,
  obtiene su `downloadUrl` y solo entonces escribe el producto en Firestore — el orden
  importa, si la subida falla no se crea el documento.
- **Mis publicaciones** (`MyProductsScreen`): al entrar, dispara
  `cargarMisProductos()`, que filtra `productos` por `artesanoId == uid actual` y
  muestra tarjetas con imagen, nombre, precio y región.

## Diseño y marca

Paleta de marca definida en `app/src/main/java/com/minkaart/app/ui/theme/Color.kt` y
mapeada a roles de Material 3 en `Theme.kt`:

| Token | Hex | Uso |
|---|---|---|
| `Terracotta` | `#C6704E` | Color primario, botones de acción |
| `Beige` | `#F1E3D0` | Fondo principal |
| `Cream` | `#FBF5EA` | Superficies, tarjetas |
| `WarmWhite` | `#FFFDF9` | Campos de texto, superficies elevadas |
| `BrownDeep` | `#4A3122` | Texto principal |
| `PinkClay` / `PinkClaySoft` | `#E7BFB6` / `#F3DCD5` | Acento suave (chips, etiquetas) |
| `Espresso` | `#2B1B10` | Fondos oscuros / modo oscuro |

`Theme.kt` define esquemas claro y oscuro completos, pero `MinkaArt_btaTheme` fuerza
`darkTheme = false` a propósito: el brand kit está diseñado sobre fondo beige y las
pantallas no se han revisado en modo oscuro. El color dinámico (Material You) está
desactivado deliberadamente para no reemplazar la paleta de marca por los colores del
fondo de pantalla del usuario.

`Type.kt` usa `FontFamily.Serif` en cursiva como sustituto temporal de **Fraunces**
(titulares, saludos) y `FontFamily.SansSerif` como sustituto de **Manrope** (interfaz),
manteniendo la escala de tamaños del brand kit. Cambiar a las fuentes reales solo
requiere reemplazar esas dos referencias por familias cargadas desde `res/font`.

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

## Estado actual y pendientes

**Implementado**
- Registro de cliente y artesano (con validaciones), incluido el flujo de RUAC.
- Login con Firebase Auth y enrutado según rol.
- Publicación de una artesanía con imagen, historia y datos comerciales.
- Listado de "mis publicaciones" para el artesano autenticado.
- Tema e identidad visual de marca aplicados vía Material 3.

**Pendiente / placeholder**
- `HomeClienteScreen.kt` y `ProfileScreen.kt` son literalmente un `Text()` de una línea;
  no hay catálogo para el cliente ni pantalla de perfil real.
- `Routes.RECOGNITION` está declarada pero no tiene `composable {}` registrado — navegar
  a ella provocaría un crash.
- Nada navega hacia `Routes.PROFILE` todavía, aunque la ruta sí está registrada.
- Tras un registro exitoso, `RegisterScreen` solo muestra un `Toast`: no navega a login
  ni a ninguna otra pantalla.
- No existe ningún flujo de catálogo/compra para el cliente: `ProductRepository` solo
  sabe leer los productos del artesano autenticado (`obtenerMisProductos`), no hay una
  consulta que traiga productos de todos los artesanos.
- `ProductRepository.guardarProducto()` (variante sin imagen) existe pero no lo invoca
  ninguna pantalla; solo se usa `guardarProductoConImagen()`.
- El modelo `Product.imagenes` es una lista, pero el flujo actual solo guarda una imagen.
- El RUAC se recibe y se guarda, pero no hay ninguna verificación real del documento ni
  del número.
- No hay tests reales: solo quedan las plantillas por defecto de Android Studio
  (`ExampleUnitTest.kt`, `ExampleInstrumentedTest.kt`).
- El directorio del proyecto no es un repositorio git todavía.
