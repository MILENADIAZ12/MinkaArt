# CLAUDE.md — MinkaArt

App Android (Kotlin + Jetpack Compose) para un marketplace de artesanías colombianas.
El contexto completo del producto, la arquitectura, el modelo de datos y el estado
actual del proyecto están en **`README.md`** — léelo antes de continuar.

## ⚠️ REGLA OBLIGATORIA: el README.md es parte del ciclo de planificación

Esta regla no es opcional ni una sugerencia. Aplica a **todo plan** que se elabore en
este proyecto, sin excepción:

1. **Antes de elaborar cualquier plan, lee y analiza `README.md` completo.** No
   planifiques a partir de suposiciones ni únicamente de la lectura del código: el
   README es el punto de partida obligatorio de todo plan en este proyecto, porque
   documenta decisiones, convenciones y estado real que el código por sí solo no deja
   ver (por ejemplo, qué pantallas son placeholders o qué rutas no están registradas).

2. **Todo plan debe incluir una sección explícita titulada "Actualización del
   README"** que responda: ¿estos cambios dejan el `README.md` desactualizado? Si la
   respuesta es sí, nombra las secciones concretas a modificar y el contenido nuevo —
   actualizar el README es parte del trabajo planeado, no un paso opcional ni algo para
   "después". Si la respuesta es no, el plan debe declararlo de forma expresa; nunca
   omitas el tema en silencio.

Disparadores que casi siempre exigen actualizar el README: añadir o quitar una pantalla
o una constante de `Routes`; cambiar campos de `User` o `Product`; cambiar colecciones
de Firestore o rutas de Storage; añadir o subir de versión una dependencia; convertir un
placeholder (`HomeClienteScreen`, `ProfileScreen`) en una pantalla real; cambiar los
comandos de build.

La razón: el README es el contexto que se carga al empezar a trabajar en este proyecto.
Si queda desactualizado, cada sesión nueva parte de información falsa sobre qué
funciona, qué rutas existen y cómo está estructurado el modelo de datos.

## Trampas del repositorio

- **Paquete vs. carpetas**: el paquete Kotlin es `com.minkaart2.app`, pero las carpetas
  físicas son `com/minkaart/app/` (sin el `2`). Para buscar código, guíate por el nombre
  del paquete en los imports, no asumas que la ruta de carpeta lo refleja.
- **`viewModel` vs `viewmodel`**: `LoginViewModel.kt` y `ProductViewModel.kt` declaran
  `package com.minkaart2.app.viewModel` (M mayúscula); `RegisterViewModel.kt`, en la
  misma carpeta, declara `package com.minkaart2.app.viewmodel` (minúscula). Son dos
  paquetes distintos. Al importar un ViewModel, copia el import de un archivo que ya lo
  use en vez de escribirlo de memoria.
- `HomeClienteScreen` y `ProfileScreen` son placeholders de una línea — no asumas que
  tienen lógica real detrás.
- `Routes.RECOGNITION` está declarada pero no tiene `composable {}` registrado en
  `AppNavigation`; navegar a ella provoca un crash en tiempo de ejecución.

## Convenciones de código

- Todo el código de dominio va en español: nombres de funciones, parámetros y mensajes
  de error, incluidos identificadores con tilde y `ñ` (p. ej. `contraseña`) — ya se usan
  así en todo el proyecto, sigue el mismo estilo.
- Patrón obligatorio en repositorios y ViewModels: funciones con parámetros finales
  `onSuccess: (...) -> Unit, onError: (String) -> Unit`. No introduzcas `suspend`,
  `Result` ni `Flow` — mezclar estilos asíncronos rompe la consistencia del proyecto.
- Errores de Firebase: `error.message ?: "Mensaje en español"`.
- Errores hacia el usuario: `Toast` construido con `LocalContext.current`.
- Las pantallas obtienen su ViewModel con `viewModel()` de
  `androidx.lifecycle.viewmodel.compose`.
- Toda `data class` que se guarde en Firestore necesita valores por defecto en todos sus
  campos (lo exige `DocumentSnapshot.toObject()`).
- Colores y tipografías siempre desde `MaterialTheme`, nunca `Color(0xFF...)` suelto
  dentro de una pantalla; los tokens nuevos van a `ui/theme/Color.kt`.
- La navegación usa siempre las constantes de `Routes`, nunca strings literales.
- Las validaciones de formulario viven en el ViewModel (ver
  `ProductViewModel.publicarProducto`), no en el Composable.

## Comandos

```powershell
.\gradlew.bat assembleDebug          # compilar el APK de debug
.\gradlew.bat installDebug            # instalar en un dispositivo/emulador conectado
.\gradlew.bat test                    # tests unitarios (JVM)
.\gradlew.bat connectedAndroidTest     # tests instrumentados (requiere dispositivo/emulador)
```

## Firebase

- Colecciones Firestore: `usuarios/{uid}` (modelo `User`) y `productos/{autoId}`
  (modelo `Product`). Detalle de campos en `README.md`.
- Storage: `productos/{artesanoId}/{productoId}/imagen_principal` y
  `ruac/{uid}/documento_ruac`.
- Cambiar la forma de `User` o `Product` afecta documentos ya guardados en producción;
  cualquier cambio de esquema debe ser retrocompatible o venir acompañado de un plan de
  migración explícito.

## Reglas de trabajo

- No reformatees archivos ajenos al cambio que estás haciendo.
- No toques `app/google-services.json`.
- No añadas dependencias nuevas sin que se pida explícitamente.
- Al agregar una pantalla nueva: créala en `ui/<feature>/`, añade su constante en
  `Routes.kt` y regístrala en `AppNavigation.kt` — los tres pasos son parte del mismo
  cambio, no solo el primero.
