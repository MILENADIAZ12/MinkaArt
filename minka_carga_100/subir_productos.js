// Sube productos_100.json + imagenes/ a Firebase con el MISMO esquema que usa la app
// (colección "productos", data class Product.kt) a nombre de un artesano existente.
//
// Uso:
//   node subir_productos.js --kt <ruta a CatalogosProducto.kt> [--dry-run] [--limite N]
//
// Ejemplo (prueba con 3, sin escribir nada):
//   node subir_productos.js --kt ..\MinkaArt\app\src\main\java\com\minkaart\app\data\CatalogosProducto.kt --dry-run --limite 3

// firebase-admin@14 ya no expone admin.credential/admin.firestore() en la raíz
// (API antigua) — usa los imports modulares.
const { initializeApp, applicationDefault } = require('firebase-admin/app');
const { getFirestore } = require('firebase-admin/firestore');
const { getStorage } = require('firebase-admin/storage');
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

// ---------- Configuración ----------
const ARTESANO_UID = 'qvaAlN9MpiXkO7iD4qnhM6iolFx2';      // milenadiaz1210@gmail.com
const BUCKET = 'minka-art.firebasestorage.app';

// Categoría del catálogo ADC -> candidatas en CatalogosProducto.CATEGORIAS (en orden de preferencia).
// Si ninguna existe en la app, el producto queda en "Otros" con categoriaOtro = categoría original.
const MAPEO_CATEGORIAS = {
  'Joyería': ['Joyería', 'Joyeria', 'Bisutería', 'Joyas'],
  'Bolsos y cestería': ['Cestería', 'Bolsos', 'Mochilas', 'Tejeduría', 'Tejidos'],
  'Ropa': ['Textiles', 'Ropa', 'Moda', 'Tejeduría', 'Tejidos'],
  'Sombreros': ['Sombreros', 'Sombrerería', 'Tejeduría'],
  'Accesorios': ['Accesorios', 'Bisutería'],
  // Los 3 productos de esta categoría son "Cuadro ..." en Filigrana/Plata 950: arte
  // decorativo en orfebrería, no joyería para vestir, pero el material/técnica coincide
  // con "Joyería y orfebrería" (contiene "orfebrería").
  'Decoración': ['Decoración', 'Hogar', 'Decoración y hogar', 'Orfebrería'],
};

// ---------- Argumentos ----------
const args = process.argv.slice(2);
const dryRun = args.includes('--dry-run');
const ktPath = args[args.indexOf('--kt') + 1];
const limite = args.includes('--limite') ? parseInt(args[args.indexOf('--limite') + 1], 10) : Infinity;
if (!args.includes('--kt') || !ktPath || !fs.existsSync(ktPath)) {
  console.error('Falta --kt <ruta a CatalogosProducto.kt> (o la ruta no existe).');
  process.exit(1);
}

// ---------- Leer las listas reales de la app ----------
const normalizar = (s) => (s || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().trim();

function leerLista(kt, nombre) {
  // No se puede usar una regex no-greedy hasta el primer ")": varias categorías
  // reales (p. ej. "Joyería y orfebrería (metales y piedras preciosas)") tienen
  // paréntesis dentro del texto y truncarían la lista en el primer elemento.
  // En su lugar, se busca "listOf(" y se cuenta paréntesis balanceados hasta el
  // cierre real de la llamada.
  const inicio = kt.match(new RegExp(nombre + '[^=]*=\\s*listOf\\s*\\('));
  if (!inicio) return null;
  const desde = inicio.index + inicio[0].length;
  let profundidad = 1;
  let i = desde;
  while (i < kt.length && profundidad > 0) {
    if (kt[i] === '(') profundidad++;
    else if (kt[i] === ')') profundidad--;
    i++;
  }
  const contenido = kt.slice(desde, i - 1);
  return [...contenido.matchAll(/"([^"]*)"/g)].map((x) => x[1]);
}
const kt = fs.readFileSync(ktPath, 'utf8');
const CATEGORIAS = leerLista(kt, 'CATEGORIAS');
const DEPARTAMENTOS = leerLista(kt, 'DEPARTAMENTOS');
if (!CATEGORIAS || !DEPARTAMENTOS) {
  console.error('No pude leer CATEGORIAS o DEPARTAMENTOS de CatalogosProducto.kt. Revisa el archivo.');
  process.exit(1);
}
const OTROS = CATEGORIAS.find((c) => normalizar(c) === 'otros') || 'Otros';

function mapearCategoria(original) {
  for (const cand of MAPEO_CATEGORIAS[original] || [original]) {
    const exacta = CATEGORIAS.find((c) => normalizar(c) === normalizar(cand));
    if (exacta) return { categoria: exacta, categoriaOtro: null };
  }
  for (const cand of MAPEO_CATEGORIAS[original] || [original]) {
    const parcial = CATEGORIAS.find((c) => normalizar(c).includes(normalizar(cand)));
    if (parcial) return { categoria: parcial, categoriaOtro: null };
  }
  return { categoria: OTROS, categoriaOtro: original };
}

function mapearDepartamento(original) {
  const n = normalizar(original);
  return DEPARTAMENTOS.find((d) => normalizar(d) === n)
      || DEPARTAMENTOS.find((d) => normalizar(d).startsWith(n) || n.startsWith(normalizar(d)))
      || null;
}

// ---------- Firebase ----------
const app = initializeApp({ credential: applicationDefault(), storageBucket: BUCKET });
const db = getFirestore(app);
const bucket = getStorage(app).bucket();

async function subirImagen(local, destino) {
  const token = crypto.randomUUID();
  await bucket.upload(local, {
    destination: destino,
    metadata: { contentType: 'image/jpeg', metadata: { firebaseStorageDownloadTokens: token } },
  });
  return `https://firebasestorage.googleapis.com/v0/b/${bucket.name}/o/${encodeURIComponent(destino)}?alt=media&token=${token}`;
}

(async () => {
  // 1. El usuario debe existir y ser artesano
  const usuario = await db.collection('usuarios').doc(ARTESANO_UID).get();
  if (!usuario.exists) throw new Error(`No existe usuarios/${ARTESANO_UID}`);
  if (normalizar(usuario.get('rol')) !== 'artesano') {
    throw new Error(`usuarios/${ARTESANO_UID} tiene rol "${usuario.get('rol')}", no "artesano".`);
  }
  console.log(`Artesano: ${usuario.get('nombre')} <${usuario.get('correo')}>`);

  const { productos } = JSON.parse(fs.readFileSync(path.join(__dirname, 'productos_100.json'), 'utf8'));
  const lote = productos.slice(0, limite);

  // 2. Resumen del mapeo antes de escribir nada
  const resumen = {};
  const sinDepto = new Set();
  for (const p of lote) {
    const { categoria, categoriaOtro } = mapearCategoria(p.categoriaCatalogo);
    const clave = `${p.categoriaCatalogo} -> ${categoria}${categoriaOtro ? ` (Otros: ${categoriaOtro})` : ''}`;
    resumen[clave] = (resumen[clave] || 0) + 1;
    if (!mapearDepartamento(p.departamento)) sinDepto.add(p.departamento);
  }
  console.log('\nMapeo de categorías:'); console.table(resumen);
  if (sinDepto.size) console.warn('⚠ Departamentos sin equivalente en la app (se guardan tal cual):', [...sinDepto]);

  // 3. Subir
  let creados = 0, omitidos = 0;
  const ahora = Date.now();
  for (const [i, p] of lote.entries()) {
    const productId = `catalogo_${p.catalogoId}`;           // id fijo: volver a correr el script no duplica
    const ref = db.collection('productos').doc(productId);
    if (!dryRun && (await ref.get()).exists) { omitidos++; continue; }

    const { categoria, categoriaOtro } = mapearCategoria(p.categoriaCatalogo);
    const destino = `productos/${ARTESANO_UID}/${productId}/imagen_principal`;   // misma ruta que la app
    const url = dryRun ? `(dry-run) ${destino}` : await subirImagen(path.join(__dirname, p.imagen), destino);

    const doc = {
      // --- campos de Product.kt ---
      id: productId,
      artesanoId: ARTESANO_UID,
      nombre: p.nombre,
      descripcion: p.descripcion,
      precio: p.precio,
      historiaProceso: p.historiaProceso,
      // regionOrigen: campo heredado — Product.kt indica que ningún código nuevo debe
      // volver a escribirlo, así que se omite (queda "" por el valor por defecto al leer).
      departamento: mapearDepartamento(p.departamento) || p.departamento,
      municipio: p.municipio || '',
      clasificacion: null,
      imagenes: [url],
      vistas: 0,
      likes: 0,
      activo: true,
      fechaPublicacion: ahora - i * 60000,                     // escalonado: conserva el orden del feed
      categoria,
      categoriaOtro,
      calidadSumaPuntos: 0,
      calidadConteo: 0,
      calidadPromedio: null,
      // --- extras (la app los ignora al leer; sirven para rastrear el origen) ---
      origen: 'catalogo_adc',
      catalogoId: p.catalogoId,
      artesanoOriginal: p.artesanoOriginal,
      codigoAdc: p.codigoAdc,
      tecnica: p.tecnica,
      materiaPrima: p.materiaPrima,
      dimensiones: p.dimensiones,
    };

    if (dryRun) { if (i < 3) console.log(JSON.stringify(doc, null, 2)); }
    else await ref.set(doc);
    creados++;
    if (creados % 10 === 0) console.log(`${creados}/${lote.length}`);
  }
  console.log(`\nListo: ${creados} ${dryRun ? 'simulados (nada escrito)' : 'creados'}, ${omitidos} ya existían.`);
})().catch((e) => { console.error('❌', e.message || e); process.exit(1); });
