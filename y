rules_version = '2';

service cloud.firestore {

  // Campos de "productos" que un no-dueño NUNCA puede tocar, sin importar por qué rama
  // de "allow update" entró (like o valoración). Usa .get(campo, default) en vez de
  // .campo a propósito: acceder con "." a un campo que no existe en el documento no da
  // null, produce un ERROR de evaluación, y un error en cualquier parte de la condición
  // hace que toda la regla se evalúe como denegada — sin ningún detalle en el cliente,
  // solo PERMISSION_DENIED. Un producto publicado o editado por última vez ANTES de que
  // se agregara un campo nuevo a Product.kt no lo tiene escrito en Firestore todavía (el
  // default de Kotlin solo aplica después de leer el documento). Ya pasó una vez
  // (departamento/municipio/clasificacion/categoriaOtro) y volvió a pasar con
  // calidadSumaPuntos/calidadConteo — de ahí esta función, en vez de seguir parchando
  // campo por campo cada vez que el modelo crece. "materiaPrima" se agregó aquí en el
  // mismo ajuste en que se agregó a Product.kt — antes de esto había quedado afuera por
  // descuido, lo que dejaba un no-dueño cambiarlo sin que ninguna rama lo notara.
  // "vistas" salió de esta función a propósito: ahora es uno de los contadores que un
  // no-dueño SÍ puede tocar (ver la tercera rama de "allow update" de productos), así
  // que cada rama de "allow update" lo compara explícitamente en vez de por aquí.
  function productoBaseIgual(antes, despues) {
    return despues.get('artesanoId', '') == antes.get('artesanoId', '')
        && despues.get('nombre', '') == antes.get('nombre', '')
        && despues.get('descripcion', '') == antes.get('descripcion', '')
        && despues.get('precio', 0) == antes.get('precio', 0)
        && despues.get('historiaProceso', '') == antes.get('historiaProceso', '')
        && despues.get('materiaPrima', '') == antes.get('materiaPrima', '')
        && despues.get('materiaPrimaOtro', null) == antes.get('materiaPrimaOtro', null)
        && despues.get('regionOrigen', '') == antes.get('regionOrigen', '')
        && despues.get('imagenes', []) == antes.get('imagenes', [])
        && despues.get('activo', true) == antes.get('activo', true)
        && despues.get('fechaPublicacion', 0) == antes.get('fechaPublicacion', 0)
        && despues.get('categoria', '') == antes.get('categoria', '')
        && despues.get('departamento', '') == antes.get('departamento', '')
        && despues.get('municipio', '') == antes.get('municipio', '')
        && despues.get('clasificacion', null) == antes.get('clasificacion', null)
        && despues.get('categoriaOtro', null) == antes.get('categoriaOtro', null)
        && despues.get('color', '') == antes.get('color', '')
        && despues.get('tamano', '') == antes.get('tamano', '');
  }

  match /databases/{database}/documents {

    // =========================
    // USUARIOS
    // =========================
    match /usuarios/{userId} {

      allow create: if request.auth != null
                    && request.auth.uid == userId;

      // Cualquier usuario autenticado puede leer cualquier perfil (igual que ya
      // funciona "productos"): el cliente necesita leer el perfil del artesano dueño
      // de un producto para mostrarlo en el Detalle (nombre, teléfono, RUAC). No hay
      // un documento de "perfil público" separado — efecto secundario aceptado: otros
      // campos del perfil (correo, fechaNacimiento, RUAC) también quedan
      // visibles para cualquier usuario con sesión.
      allow read: if request.auth != null;

      // El propio usuario edita su documento (bio, contacto, solicitar RUAC), pero no
      // puede auto-asignarse el rol "administrador" ni marcarse su propio RUAC como
      // "verificado" — solo puede mover ruacEstado hacia "pendiente" (solicitar o
      // volver a solicitar tras un rechazo).
      // "no_solicitado" se agregó para permitir Eliminar RUAC (el artesano retirando su
      // propio dato) — sigue sin poder auto-asignarse "verificado", eso solo lo hace un
      // administrador (siguiente regla de update).
      allow update: if request.auth != null
                    && request.auth.uid == userId
                    && request.resource.data.rol == resource.data.rol
                    && (
                      request.resource.data.ruacEstado == resource.data.ruacEstado ||
                      request.resource.data.ruacEstado == "pendiente" ||
                      request.resource.data.ruacEstado == "no_solicitado"
                    );

      // Un administrador puede aprobar/rechazar el RUAC de cualquier artesano (o
      // cambiar cualquier otro campo del documento — no se restringe más porque el
      // alcance de esta fase es únicamente aprobar/rechazar RUAC, ver README).
      allow update: if request.auth != null
                    && get(/databases/$(database)/documents/usuarios/$(request.auth.uid)).data.rol == "administrador";
    }

    // =========================
    // CONFIGURACIÓN (PIN de administrador)
    // =========================
    match /configuracion/adminAccess {

      // Necesario para poder comparar el PIN al crear la cuenta de administrador
      // (AuthRepository.registrarAdministrador crea primero la cuenta de Firebase Auth,
      // así que request.auth ya existe en ese punto). ADVERTENCIA DE SEGURIDAD: esto
      // también deja el documento legible por CUALQUIER usuario autenticado (cliente o
      // artesano), no solo durante el registro de administrador — no es a prueba de un
      // atacante decidido. La forma "correcta" sería validar el PIN con una Cloud
      // Function, pero el proyecto no las usa para esto; se acepta el riesgo para el
      // alcance de esta tesis/MVP.
      allow get: if request.auth != null;
      allow write: if false; // se edita a mano desde la consola de Firebase
    }

    // =========================
    // PRODUCTOS
    // =========================
    match /productos/{productId} {

      // Usuarios autenticados pueden consultar productos
      allow read: if request.auth != null;

      // Un usuario solo puede crear productos
      // asociados a su propio UID
      allow create: if request.auth != null
                    && request.resource.data.artesanoId == request.auth.uid;

      // El propietario puede editar/eliminar cualquier campo. Dos excepciones puntuales
      // dejan que un no-dueño actualice (productoBaseIgual, declarada arriba, cubre todos
      // los campos que ninguna de las dos ramas puede tocar):
      //   1) toggle de like (alternarLike): solo "likes" cambia.
      //   2) valoración de compra (SaleRepository.enviarValoracion): solo los 3 campos de
      //      calidad cambian, con límites numéricos (conteo sube en 1, suma de puntos
      //      sube entre 1 y 4) — a propósito NO se verifica que calidadPromedio sea
      //      exactamente igual a la división, por la misma razón ya documentada en
      //      productoBaseIgual: una comparación de igualdad exacta combinada con una
      //      transacción puede fallar por razones no evidentes. Se confía en el cliente
      //      para ese cálculo derivado, mismo nivel de confianza que ya se le da a
      //      FieldValue.increment() en "likes".
      allow update: if request.auth != null
                    && (
                      resource.data.artesanoId == request.auth.uid
                      || (   // toggle de like (alternarLike): solo "likes" cambia.
                        productoBaseIgual(resource.data, request.resource.data)
                        && request.resource.data.get('calidadSumaPuntos', 0) == resource.data.get('calidadSumaPuntos', 0)
                        && request.resource.data.get('calidadConteo', 0) == resource.data.get('calidadConteo', 0)
                        && request.resource.data.get('vistas', 0) == resource.data.get('vistas', 0)
                      )
                      || (   // valoración de compra (SaleRepository.enviarValoracion): solo calidad.
                        productoBaseIgual(resource.data, request.resource.data)
                        && request.resource.data.get('likes', 0) == resource.data.get('likes', 0)
                        && request.resource.data.get('vistas', 0) == resource.data.get('vistas', 0)
                        && request.resource.data.get('calidadConteo', 0) == resource.data.get('calidadConteo', 0) + 1
                        && request.resource.data.get('calidadSumaPuntos', 0) >= resource.data.get('calidadSumaPuntos', 0) + 1
                        && request.resource.data.get('calidadSumaPuntos', 0) <= resource.data.get('calidadSumaPuntos', 0) + 4
                      )
                      || (   // ProductDetailViewModel.cargarDetalle: +1 vista, nada más.
                        productoBaseIgual(resource.data, request.resource.data)
                        && request.resource.data.get('likes', 0) == resource.data.get('likes', 0)
                        && request.resource.data.get('calidadSumaPuntos', 0) == resource.data.get('calidadSumaPuntos', 0)
                        && request.resource.data.get('calidadConteo', 0) == resource.data.get('calidadConteo', 0)
                        && request.resource.data.get('vistas', 0) == resource.data.get('vistas', 0) + 1
                      )
                    );

      allow delete: if request.auth != null
                    && resource.data.artesanoId == request.auth.uid;

      // =========================
      // LIKES (subcolección)
      // =========================
      match /likes/{userId} {

        allow read: if request.auth != null;

        // Solo el propio usuario puede crear/borrar su like — no se permite editarlo.
        allow create, delete: if request.auth != null
                              && request.auth.uid == userId;

        allow update: if false;
      }
    }

    // =========================
    // HUELLAS VISUALES (Reconocimiento visual)
    // =========================
    match /huellasVisuales/{productId} {

      // Cualquier autenticado puede leer — las necesita RecognitionScreen para comparar
      // localmente contra el catálogo completo.
      allow read: if request.auth != null;

      allow create: if request.auth != null
                    && (
                      (
                        request.resource.data.get('artesanoId', '') == request.auth.uid
                        && get(/databases/$(database)/documents/productos/$(productId)).data.get('artesanoId', '') == request.auth.uid
                      )
                      || get(/databases/$(database)/documents/usuarios/$(request.auth.uid)).data.get('rol', '') == "administrador"
                    );

      allow update: if request.auth != null
                    && (
                      (
                        resource.data.get('artesanoId', '') == request.auth.uid
                        && get(/databases/$(database)/documents/productos/$(productId)).data.get('artesanoId', '') == request.auth.uid
                      )
                      || get(/databases/$(database)/documents/usuarios/$(request.auth.uid)).data.get('rol', '') == "administrador"
                    );

      allow delete: if request.auth != null
                    && (
                      resource.data.get('artesanoId', '') == request.auth.uid
                      || get(/databases/$(database)/documents/usuarios/$(request.auth.uid)).data.get('rol', '') == "administrador"
                    );
    }

    // =========================
    // EMBEDDINGS VISUALES (búsqueda visual — reemplaza huellasVisuales)
    // =========================
    // Solo las Cloud Functions (Admin SDK, que ignora estas reglas) leen y
    // escriben esta colección: sincronizarEmbeddingVisual la mantiene al
    // día y buscarPorImagen es quien hace la búsqueda vectorial del lado
    // del servidor. La app nunca necesita leerla directamente (a
    // diferencia de huellasVisuales, que sí la comparaba en el
    // dispositivo) — RecognitionViewModel solo recibe {productId,
    // artesanoId, similitud} de vuelta del callable.
    match /embeddingsVisuales/{productId} {
      allow read, write: if false;
    }

    // =========================
    // LÍMITES DE BÚSQUEDA VISUAL (control de costos de buscarPorImagen)
    // =========================
    // Solo la Cloud Function buscarPorImagen (Admin SDK) lee/escribe el
    // contador diario de cada usuario.
    match /limitesBusquedaVisual/{uid} {
      allow read, write: if false;
    }

    // =========================
    // LIKES — collection group query (LikesScreen)
    // =========================
    // La regla anidada de arriba (match /productos/{productId}/likes/{userId}) solo
    // autoriza lecturas/escrituras dirigidas a esa ruta exacta o consultas acotadas a la
    // subcolección de UN producto puntual. NO autoriza un collectionGroup("likes") que
    // recorra los "likes" de TODOS los productos a la vez — que es justo lo que usa
    // obtenerProductosLikeadosPorUsuario() para LikesScreen. Firestore exige una regla
    // aparte con el comodín "{path=**}" para ese tipo de consulta; sin ella, la consulta
    // devuelve PERMISSION_DENIED aunque la regla anidada ya permita el mismo campo.
    match /{path=**}/likes/{likeId} {
      allow read: if request.auth != null;
    }

    // =========================
    // VENTAS (Fase 6)
    // =========================
    match /ventas/{ventaId} {

      // Solo el artesano dueño del producto puede registrar una venta de ese producto,
      // y solo a un usuario que de verdad tenga rol "cliente".
      allow create: if request.auth != null
                    && request.auth.uid == request.resource.data.artesanoId
                    && get(/databases/$(database)/documents/productos/$(request.resource.data.productoId)).data.artesanoId == request.auth.uid
                    && get(/databases/$(database)/documents/usuarios/$(request.resource.data.clienteId)).data.rol == "cliente";

      // Privado entre las dos partes de la venta (no es como "productos", que cualquiera
      // puede leer) — ni un tercer usuario ni un administrador necesitan ver esto.
      allow read: if request.auth != null
                  && (request.auth.uid == resource.data.clienteId
                      || request.auth.uid == resource.data.artesanoId);

      // Solo el cliente que compró puede marcarla como valorada, y solo hacia "true" —
      // Valoracion.kt es inmutable una vez creada, pero sin este "== true" nada impediría
      // volver a poner "valorado" en false después (no permitiría re-valorar, porque
      // valoraciones/{ventaId} ya estaría ocupado, pero sí dejaría la UI mostrando
      // "pendiente" de forma incorrecta).
      allow update: if request.auth != null
                    && request.auth.uid == resource.data.clienteId
                    && request.resource.data.diff(resource.data).affectedKeys().hasOnly(["valorado"])
                    && request.resource.data.valorado == true;
    }

    // =========================
    // VALORACIONES (Fase 6)
    // =========================
    match /valoraciones/{ventaId} {

      // El id del documento es el mismo id de la venta que valora — solo puede crearlo
      // el cliente de esa venta, y solo si esa venta todavía no fue valorada.
      allow create: if request.auth != null
                    && request.auth.uid == request.resource.data.clienteId
                    && get(/databases/$(database)/documents/ventas/$(ventaId)).data.clienteId == request.auth.uid
                    && get(/databases/$(database)/documents/ventas/$(ventaId)).data.valorado == false;

      allow read: if request.auth != null;

      // Nunca se edita ni se borra, a propósito: el puntaje no se puede manipular
      // después de aprobado.
      allow update, delete: if false;
    }

    // =========================
    // ARTESANO STATS — confianza acumulada del artesano (Fase 6)
    // =========================
    match /artesanoStats/{artesanoId} {

      allow read: if request.auth != null;

      // Solo un cliente autenticado puede crear/actualizar esto (es quien dispara
      // SaleRepository.enviarValoracion). Igual que en "productos", se validan límites
      // numéricos del incremento (conteo sube en 1, suma de puntos sube entre 1 y 4) sin
      // verificar la igualdad exacta de confianzaPromedio — mismo criterio que arriba.
      allow create: if request.auth != null
                    && get(/databases/$(database)/documents/usuarios/$(request.auth.uid)).data.rol == "cliente"
                    && request.resource.data.confianzaConteo == 1
                    && request.resource.data.confianzaSumaPuntos >= 1
                    && request.resource.data.confianzaSumaPuntos <= 4;

      allow update: if request.auth != null
                    && get(/databases/$(database)/documents/usuarios/$(request.auth.uid)).data.rol == "cliente"
                    && request.resource.data.confianzaConteo == resource.data.confianzaConteo + 1
                    && request.resource.data.confianzaSumaPuntos >= resource.data.confianzaSumaPuntos + 1
                    && request.resource.data.confianzaSumaPuntos <= resource.data.confianzaSumaPuntos + 4;
    }

    // =========================
    // CÓDIGOS DE VERIFICACIÓN (nuevo)
    // =========================
    match /codigosVerificacion/{correo} {
      // Solo las Cloud Functions (Admin SDK) pueden leer/escribir esto.
      // Bloqueado para la app y cualquier acceso directo.
      allow read, write: if false;
    }

    // =========================
    // NOTIFICACIONES (campana)
    // =========================
    match /notificaciones/{notificacionId} {

      // Solo las Cloud Functions (Admin SDK, que ignora estas reglas) crean
      // notificaciones: así ningún cliente puede fabricar un aviso falso para otro
      // usuario — a diferencia del resto de colecciones del proyecto, aquí la app nunca
      // escribe un documento nuevo.
      allow create: if false;

      allow read: if request.auth != null
                  && resource.data.destinatarioId == request.auth.uid;

      // El destinatario solo puede marcarla como leída, nada más.
      allow update: if request.auth != null
                    && resource.data.destinatarioId == request.auth.uid
                    && request.resource.data.diff(resource.data).affectedKeys().hasOnly(["leida"])
                    && request.resource.data.leida == true;

      allow delete: if false;
    }
  }
}
