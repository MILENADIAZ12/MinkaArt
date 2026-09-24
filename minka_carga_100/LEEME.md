# Carga de 100 productos a Minka Art

Contenido: `productos_100.json` (ya adaptado al modelo `Product.kt`), `imagenes/` (100 fotos)
y `subir_productos.js`. Todos quedan publicados a nombre del artesano
`milenadiaz1210@gmail.com` (uid `qvaAlN9MpiXkO7iD4qnhM6iolFx2`).

## Pasos (PowerShell, dentro de esta carpeta)

1. Clave de cuenta de servicio: Firebase Console → ⚙ Configuración del proyecto →
   Cuentas de servicio → "Generar nueva clave privada". Guárdala aquí como
   `serviceAccountKey.json`. **Nunca la subas a GitHub.**
2. Instalar y apuntar a la clave:
   ```powershell
   npm init -y
   npm install firebase-admin
   $env:GOOGLE_APPLICATION_CREDENTIALS=".\serviceAccountKey.json"
   ```
3. Simulación con 3 productos (no escribe nada; revisa la tabla de categorías):
   ```powershell
   node subir_productos.js --kt "RUTA\app\src\main\java\com\minkaart\app\data\CatalogosProducto.kt" --dry-run --limite 3
   ```
4. Prueba real con 3 y revísalos en la app:
   ```powershell
   node subir_productos.js --kt "RUTA\...\CatalogosProducto.kt" --limite 3
   ```
5. Todo: el mismo comando sin `--limite`. Los ya subidos se omiten (no se duplican).

## Notas
- Precios: el catálogo original no los trae; son valores de referencia estimados por
  categoría. Edítalos en `productos_100.json` antes del paso 5 si quieres.
- Cada producto guarda `origen: "catalogo_adc"` y `artesanoOriginal` para dar crédito
  al artesano real y poder borrarlos o filtrarlos después.
- Las huellas visuales NO las genera este script: se generan con el mecanismo de
  "recalcular huellas" de la app (ver el prompt para Claude Code).
