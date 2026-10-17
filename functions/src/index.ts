import { initializeApp } from "firebase-admin/app";
import { getFirestore, FieldValue, Timestamp } from "firebase-admin/firestore";
import { getAuth } from "firebase-admin/auth";
import { getMessaging } from "firebase-admin/messaging";
import { onCall, HttpsError } from "firebase-functions/v2/https";
import { onDocumentCreated, onDocumentUpdated, onDocumentWritten } from "firebase-functions/v2/firestore";
import { defineSecret } from "firebase-functions/params";
import * as logger from "firebase-functions/logger";
import * as nodemailer from "nodemailer";
import { GoogleAuth } from "google-auth-library";

initializeApp();
const db = getFirestore();

// Se configura una sola vez con:
// firebase functions:secrets:set RESEND_API_KEY
// (la API key de https://resend.com/api-keys, empieza por "re_"). Nunca va en el código.
const resendKey = defineSecret("RESEND_API_KEY");

// Remitente. Resend NO permite enviar desde direcciones @gmail.com: el "from" tiene que ser
// un dominio verificado en Resend. Mientras no haya dominio propio se usa el remitente de
// pruebas de Resend (onboarding@resend.dev), que SOLO entrega al correo con el que se
// registró la cuenta de Resend. Cuando se verifique un dominio, cambiar REMITENTE por
// p. ej. "MinkaArt <no-responder@tudominio.com>".
const REMITENTE = "MinkaArt <onboarding@resend.dev>";

// Las respuestas de los usuarios llegan a esta cuenta de Gmail.
const RESPONDER_A = "minka.art.app@gmail.com";

async function enviarConResend(para: string, asunto: string, html: string): Promise<void> {
  const respuesta = await fetch("https://api.resend.com/emails", {
    method: "POST",
    headers: {
      Authorization: `Bearer ${resendKey.value()}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      from: REMITENTE,
      to: [para],
      reply_to: RESPONDER_A,
      subject: asunto,
      html,
    }),
  });

  if (!respuesta.ok) {
    throw new Error(`Resend respondió ${respuesta.status}: ${await respuesta.text()}`);
  }
}

// Proveedor de correo. "gmail" (SMTP de Gmail con contraseña de aplicación) entrega a cualquier
// destinatario, con un límite de ~500 correos/día. "resend" solo entrega a cualquier
// destinatario cuando hay un dominio verificado en Resend; sin él, solo al correo de la
// cuenta de Resend. Cambiar a "resend" cuando exista el dominio (y ajustar REMITENTE).
// Secretos de Gmail: firebase functions:secrets:set GMAIL_USER / GMAIL_APP_PASSWORD
// (GMAIL_USER = minka.art.app@gmail.com; la contraseña es de aplicación, no la normal).
const PROVEEDOR_CORREO: "gmail" | "resend" = "gmail";
const gmailUser = defineSecret("GMAIL_USER");
const gmailPassword = defineSecret("GMAIL_APP_PASSWORD");
const SECRETOS_CORREO = [gmailUser, gmailPassword, resendKey];

async function enviarCorreo(para: string, asunto: string, html: string): Promise<void> {
  if (PROVEEDOR_CORREO === "resend") {
    await enviarConResend(para, asunto, html);
    return;
  }

  const transporter = nodemailer.createTransport({
    host: "smtp.gmail.com",
    port: 465,
    secure: true,
    auth: { user: gmailUser.value(), pass: gmailPassword.value() },
  });

  await transporter.sendMail({
    from: `MinkaArt <${gmailUser.value()}>`,
    to: para,
    replyTo: RESPONDER_A,
    subject: asunto,
    html,
  });
}

const COLECCION = "codigosVerificacion";
const EXPIRACION_MINUTOS = 10;
const MAX_INTENTOS = 5;

function esCorreoValido(correo: unknown): correo is string {
  return typeof correo === "string" && /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(correo);
}

function generarCodigo(): string {
  return Math.floor(100000 + Math.random() * 900000).toString();
}

function plantillaCorreo(codigo: string): string {
  return `
    <div style="font-family: sans-serif; max-width: 480px; margin: 0 auto; padding: 24px;">
      <h2 style="color: #722F37;">MinkaArt</h2>
      <p>Tu código de verificación es:</p>
      <p style="font-size: 32px; font-weight: bold; letter-spacing: 4px; color: #722F37;">${codigo}</p>
      <p>Este código expira en ${EXPIRACION_MINUTOS} minutos.</p>
      <p style="color: #888; font-size: 12px;">Si no solicitaste este código, ignora este correo.</p>
    </div>
  `;
}

function plantillaCambioContrasena(enlace: string): string {
  return `
    <div style="font-family: sans-serif; max-width: 480px; margin: 0 auto; padding: 24px;">
      <h2 style="color: #722F37;">MinkaArt</h2>
      <p>Recibimos una solicitud para cambiar la contraseña de tu cuenta.</p>
      <p><a href="${enlace}" style="display: inline-block; background: #66172E; color: #F2EDE6; padding: 12px 24px; border-radius: 10px; text-decoration: none; font-weight: bold;">Cambiar mi contraseña</a></p>
      <p style="color: #888; font-size: 12px;">Si no pediste este cambio, ignora este correo: tu contraseña no se modificará.</p>
    </div>
  `;
}

/**
 * Genera un código de 6 dígitos, lo envía por correo (API de Resend) y solo si el envío
 * fue exitoso lo guarda en Firestore.
 * Se llama desde la app antes de crear la cuenta de Firebase Auth.
 */
export const enviarCodigoVerificacion = onCall(
  { secrets: SECRETOS_CORREO, invoker: "public" },
  async (request) => {
    const correo = request.data?.correo;

    if (!esCorreoValido(correo)) {
      throw new HttpsError("invalid-argument", "El correo no es válido.");
    }

    const codigo = generarCodigo();
    const expiracion = Timestamp.fromMillis(Date.now() + EXPIRACION_MINUTOS * 60 * 1000);

    try {
      await enviarCorreo(correo, "Tu código de verificación - MinkaArt", plantillaCorreo(codigo));
    } catch (error) {
      logger.error("Fallo al enviar el código de verificación", error);
      throw new HttpsError("internal", "No se pudo enviar el correo. Intenta de nuevo.");
    }

    await db.collection(COLECCION).doc(correo).set({
      codigo,
      expiracion,
      intentos: 0,
      creado: FieldValue.serverTimestamp(),
    });

    return { enviado: true };
  }
);

const COLECCION_LIMITES_RECUPERACION = "limitesRecuperacion";
const ESPERA_RECUPERACION_SEGUNDOS = 60;

/**
 * Envía en español, por Resend, el enlace para cambiar la contraseña (reemplaza a
 * sendPasswordResetEmail de Firebase, cuyo correo salía en inglés y caía en spam).
 * El enlace lo genera Firebase Auth (Admin SDK) y abre su página de restablecimiento en
 * español. No revela si el correo existe: responde igual en ambos casos. Limita a un envío
 * por correo cada ESPERA_RECUPERACION_SEGUNDOS.
 */
export const enviarEnlaceCambioContrasena = onCall(
  { secrets: SECRETOS_CORREO, invoker: "public" },
  async (request) => {
    const correo = request.data?.correo;

    if (!esCorreoValido(correo)) {
      throw new HttpsError("invalid-argument", "El correo no es válido.");
    }

    const limiteRef = db.collection(COLECCION_LIMITES_RECUPERACION).doc(correo);
    const limiteSnap = await limiteRef.get();
    const ultimo = limiteSnap.get("ultimoEnvio") as Timestamp | undefined;

    if (ultimo && Date.now() - ultimo.toMillis() < ESPERA_RECUPERACION_SEGUNDOS * 1000) {
      throw new HttpsError("resource-exhausted", "Espera un minuto antes de pedir otro correo.");
    }

    let enlace: string;

    try {
      enlace = await getAuth().generatePasswordResetLink(correo);
    } catch (error) {
      // Sin cuenta con ese correo: no se envía nada, pero tampoco se avisa a quien pregunta.
      logger.info("Cambio de contraseña pedido para un correo sin cuenta");
      return { enviado: true };
    }

    const enlaceEspanol = enlace + (enlace.includes("?") ? "&" : "?") + "lang=es";

    try {
      await enviarCorreo(
        correo,
        "Cambia tu contraseña - MinkaArt",
        plantillaCambioContrasena(enlaceEspanol)
      );
    } catch (error) {
      logger.error("Fallo al enviar el enlace de cambio de contraseña", error);
      throw new HttpsError("internal", "No se pudo enviar el correo. Intenta de nuevo.");
    }

    await limiteRef.set({ ultimoEnvio: Timestamp.now() });

    return { enviado: true };
  }
);

/**
 * Verifica el código ingresado por la usuaria contra el guardado en Firestore.
 * Se llama justo antes de crear la cuenta de Firebase Auth.
 */
export const verificarCodigo = onCall({ invoker: "public" }, async (request) => {
  const correo = request.data?.correo;
  const codigo = request.data?.codigo;

  if (!esCorreoValido(correo) || typeof codigo !== "string") {
    throw new HttpsError("invalid-argument", "Faltan datos para verificar el código.");
  }

  const ref = db.collection(COLECCION).doc(correo);
  const snap = await ref.get();

  if (!snap.exists) {
    throw new HttpsError("not-found", "No se encontró un código para este correo. Solicita uno nuevo.");
  }

  const datos = snap.data()!;

  if (datos.intentos >= MAX_INTENTOS) {
    throw new HttpsError("resource-exhausted", "Demasiados intentos. Solicita un código nuevo.");
  }

  if (datos.expiracion.toMillis() < Date.now()) {
    throw new HttpsError("deadline-exceeded", "El código expiró. Solicita uno nuevo.");
  }

  if (datos.codigo !== codigo) {
    await ref.update({ intentos: FieldValue.increment(1) });
    throw new HttpsError("invalid-argument", "Código incorrecto.");
  }

  // Código correcto: se borra para que no pueda reutilizarse.
  await ref.delete();

  return { valido: true };
});

/**
 * Escribe un documento en `notificaciones/{autoId}` y, si el destinatario tiene un
 * `fcmToken` guardado, le envía además el push correspondiente — los cuatro triggers de
 * abajo (venta, valoración, like, RUAC) comparten esta función en vez de repetir la
 * misma lógica de escritura + push cuatro veces. Solo las Cloud Functions crean
 * documentos en `notificaciones` (ver las reglas de Firestore): la app nunca escribe ahí
 * directamente, así ningún usuario puede fabricar un aviso falso para otro.
 *
 * El push se envía SOLO con `data` (sin bloque `notification`): así
 * `MinkaMessagingService.onMessageReceived`, del lado de la app, se dispara siempre
 * (primer plano, segundo plano o cerrada) y construye la notificación de forma
 * consistente, en vez de depender del comportamiento por defecto de FCM.
 */
async function avisar(
  destinatarioId: string,
  tipo: string,
  titulo: string,
  mensaje: string,
  referenciaId: string
): Promise<void> {
  const notificacionRef = db.collection("notificaciones").doc();

  await notificacionRef.set({
    id: notificacionRef.id,
    destinatarioId,
    tipo,
    titulo,
    mensaje,
    referenciaId,
    leida: false,
    creadoEn: FieldValue.serverTimestamp(),
  });

  const destinatarioSnap = await db.collection("usuarios").doc(destinatarioId).get();
  const token = destinatarioSnap.get("fcmToken") as string | undefined;

  if (!token) {
    // El destinatario todavía no abrió la app en este dispositivo desde que existe esta
    // función (o negó el permiso de notificaciones) — no es un error, el aviso ya quedó
    // guardado en Firestore y se ve en la campana la próxima vez que abra la app.
    return;
  }

  await getMessaging().send({
    token,
    data: { tipo, referenciaId, titulo, cuerpo: mensaje },
  });
}

/**
 * Notifica al cliente cuando un artesano le registra una venta nueva (Fase 6 + ajuste
 * posterior). Disparador de Firestore, no `onCall` — nadie espera una respuesta, así que
 * cualquier error solo se registra con logger.error, nunca se relanza (mismo criterio en
 * los otros tres triggers de notificaciones).
 */
export const notificarNuevaVenta = onDocumentCreated(
  "ventas/{ventaId}",
  async (event) => {
    const venta = event.data?.data();

    if (!venta) {
      logger.error("notificarNuevaVenta: el evento no trae datos de la venta");
      return;
    }

    const clienteId = venta.clienteId as string | undefined;
    const productoId = venta.productoId as string | undefined;

    if (!clienteId || !productoId) {
      logger.error("notificarNuevaVenta: falta clienteId o productoId en la venta");
      return;
    }

    try {
      const productoSnap = await db.collection("productos").doc(productoId).get();
      const nombreProducto = (productoSnap.get("nombre") as string | undefined) ?? "tu producto";

      await avisar(
        clienteId,
        "venta",
        "Nueva compra registrada",
        `Tienes "${nombreProducto}" pendiente de valorar`,
        event.params.ventaId
      );
    } catch (error) {
      logger.error("notificarNuevaVenta: fallo al enviar la notificación", error);
    }
  }
);

/**
 * Notifica al artesano cuando un cliente valora una de sus ventas (Fase 6). El artesano
 * hoy no tiene ningún otro aviso de este evento.
 */
export const notificarNuevaValoracion = onDocumentCreated(
  "valoraciones/{ventaId}",
  async (event) => {
    const valoracion = event.data?.data();

    if (!valoracion) {
      logger.error("notificarNuevaValoracion: el evento no trae datos de la valoración");
      return;
    }

    const artesanoId = valoracion.artesanoId as string | undefined;
    const productoId = valoracion.productoId as string | undefined;

    if (!artesanoId || !productoId) {
      logger.error("notificarNuevaValoracion: falta artesanoId o productoId en la valoración");
      return;
    }

    try {
      const productoSnap = await db.collection("productos").doc(productoId).get();
      const nombreProducto = (productoSnap.get("nombre") as string | undefined) ?? "tu producto";

      await avisar(
        artesanoId,
        "valoracion",
        "Recibiste una valoración",
        `Un cliente valoró su compra de "${nombreProducto}"`,
        productoId
      );
    } catch (error) {
      logger.error("notificarNuevaValoracion: fallo al enviar la notificación", error);
    }
  }
);

/**
 * Notifica al artesano cuando uno de sus productos recibe un "me gusta" nuevo. Se ignora
 * si el propio artesano le da like a su producto (no tiene sentido avisarle de su propia
 * acción).
 */
export const notificarLikeNuevo = onDocumentCreated(
  "productos/{productId}/likes/{userId}",
  async (event) => {
    const productId = event.params.productId;
    const userId = event.params.userId;

    try {
      const productoSnap = await db.collection("productos").doc(productId).get();
      const artesanoId = productoSnap.get("artesanoId") as string | undefined;

      if (!artesanoId) {
        logger.error("notificarLikeNuevo: el producto no tiene artesanoId");
        return;
      }

      if (artesanoId === userId) {
        return;
      }

      const nombreProducto = (productoSnap.get("nombre") as string | undefined) ?? "tu producto";

      await avisar(
        artesanoId,
        "like",
        "Nuevo me gusta",
        `A alguien le gustó "${nombreProducto}"`,
        productId
      );
    } catch (error) {
      logger.error("notificarLikeNuevo: fallo al enviar la notificación", error);
    }
  }
);

/**
 * Notifica al artesano cuando alguien empieza a seguirlo. `referenciaId` es el uid del
 * propio artesano: al tocar el aviso se abre su perfil.
 */
export const notificarNuevoSeguidor = onDocumentCreated(
  "usuarios/{artesanoId}/seguidores/{seguidorId}",
  async (event) => {
    const artesanoId = event.params.artesanoId;
    const seguidorId = event.params.seguidorId;

    try {
      if (artesanoId === seguidorId) {
        return;
      }

      const seguidorSnap = await db.collection("usuarios").doc(seguidorId).get();
      const nombre = (seguidorSnap.get("nombre") as string | undefined) ?? "Alguien";

      await avisar(
        artesanoId,
        "seguidor",
        "Nuevo seguidor",
        `${nombre} empezó a seguirte`,
        artesanoId
      );
    } catch (error) {
      logger.error("notificarNuevoSeguidor: fallo al enviar la notificación", error);
    }
  }
);

/**
 * Notifica a los seguidores de un artesano cuando publica un producto nuevo. Los
 * seguidores viven en `usuarios/{artesanoId}/seguidores/{seguidorId}`. Un error con un
 * seguidor no impide avisar a los demás.
 */
export const notificarNuevaPublicacion = onDocumentCreated(
  "productos/{productId}",
  async (event) => {
    const productId = event.params.productId;
    const datos = event.data?.data();

    try {
      const artesanoId = datos?.artesanoId as string | undefined;

      if (!artesanoId) {
        return;
      }

      if (datos?.activo === false) {
        return;
      }

      const seguidoresSnap = await db
        .collection("usuarios")
        .doc(artesanoId)
        .collection("seguidores")
        .get();

      if (seguidoresSnap.empty) {
        return;
      }

      const artesanoSnap = await db.collection("usuarios").doc(artesanoId).get();
      const nombreArtesano = (artesanoSnap.get("nombre") as string | undefined) ?? "Un artesano";
      const nombreProducto = (datos?.nombre as string | undefined) ?? "un producto nuevo";

      for (const seguidor of seguidoresSnap.docs) {
        try {
          await avisar(
            seguidor.id,
            "publicacion",
            "Nueva publicación",
            `${nombreArtesano} publicó "${nombreProducto}"`,
            productId
          );
        } catch (error) {
          logger.error(`notificarNuevaPublicacion: fallo con el seguidor ${seguidor.id}`, error);
        }
      }
    } catch (error) {
      logger.error("notificarNuevaPublicacion: fallo al enviar las notificaciones", error);
    }
  }
);

/**
 * Notifica al artesano cuando un administrador aprueba o rechaza su solicitud de RUAC
 * (Fase 5). Solo actúa si `ruacEstado` cambió y el nuevo valor es uno de los dos estados
 * finales de una revisión — evita disparar en cualquier otro update del documento de
 * usuario (editar bio, contacto, etc.).
 */
export const notificarCambioRuac = onDocumentUpdated(
  "usuarios/{uid}",
  async (event) => {
    const antes = event.data?.before.data();
    const despues = event.data?.after.data();

    if (!antes || !despues) {
      logger.error("notificarCambioRuac: el evento no trae datos del usuario");
      return;
    }

    const estadoAntes = antes.ruacEstado as string | undefined;
    const estadoDespues = despues.ruacEstado as string | undefined;

    if (estadoAntes === estadoDespues) {
      return;
    }

    try {
      if (estadoDespues === "verificado") {

        await avisar(
          event.params.uid,
          "ruac_aprobado",
          "RUAC aprobado",
          "Tu solicitud de RUAC fue verificada",
          event.params.uid
        );

      } else if (estadoDespues === "rechazado") {

        await avisar(
          event.params.uid,
          "ruac_rechazado",
          "RUAC rechazado",
          "Tu solicitud de RUAC fue rechazada, puedes volver a intentarlo",
          event.params.uid
        );
      }
    } catch (error) {
      logger.error("notificarCambioRuac: fallo al enviar la notificación", error);
    }
  }
);

// =========================================================================
// BÚSQUEDA VISUAL (Vertex AI + Firestore vector search)
// =========================================================================
//
// Reemplaza el motor híbrido ORB/MediaPipe que corría en el dispositivo (ver
// README, "Búsqueda visual"): el embedding de cada foto ahora lo calcula
// Vertex AI (multimodalembedding@001, 1408 dimensiones, ya normalizado a
// norma 1 — verificado con una llamada real antes de escribir este código)
// y la comparación la hace Firestore con un índice vectorial (findNearest),
// no el celular. OpenCV en la app queda solo para preprocesar la foto antes
// de enviarla (recorte, CLAHE, detección de borrosidad), nunca para
// comparar contra el catálogo.

const PROJECT_ID = process.env.GCLOUD_PROJECT ?? "minka-art";
const REGION_VERTEX = "us-central1";
const MODELO_EMBEDDING = "multimodalembedding@001";
const DIMENSION_EMBEDDING = 1408;
const COLECCION_EMBEDDINGS = "embeddingsVisuales";
const VERSION_EMBEDDING = 1;

const LIMITE_BUSQUEDAS_DIARIAS = 30;
// ~1.5 MB en base64 — generoso para un JPEG de lado máximo 800px (el límite
// real de tamaño lo impone PreprocesadorImagen del lado de la app; esto es
// solo una segunda barrera contra un cliente que no lo respete).
const MAX_BYTES_IMAGEN_CONSULTA = 1_500_000;
const LIMITE_RESULTADOS_FIND_NEAREST = 30;

const authVertex = new GoogleAuth({ scopes: ["https://www.googleapis.com/auth/cloud-platform"] });

/**
 * Pide a Vertex AI el embedding de una imagen (JPEG en base64, sin el
 * prefijo "data:image/...;base64,"). El vector que devuelve ya viene
 * normalizado a norma 1, así que DOT_PRODUCT en Firestore equivale a
 * similitud coseno sin costo adicional.
 */
async function obtenerEmbeddingDeImagen(imagenBase64: string): Promise<number[]> {
  const cliente = await authVertex.getClient();
  const { token } = await cliente.getAccessToken();

  const uri =
    `https://${REGION_VERTEX}-aiplatform.googleapis.com/v1/projects/${PROJECT_ID}` +
    `/locations/${REGION_VERTEX}/publishers/google/models/${MODELO_EMBEDDING}:predict`;

  const respuesta = await fetch(uri, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      instances: [{ image: { bytesBase64Encoded: imagenBase64 } }],
      parameters: { dimension: DIMENSION_EMBEDDING },
    }),
  });

  if (!respuesta.ok) {
    throw new Error(`Vertex AI respondió ${respuesta.status}: ${await respuesta.text()}`);
  }

  const datos = (await respuesta.json()) as {
    predictions?: Array<{ imageEmbedding?: number[] }>;
  };
  const vector = datos.predictions?.[0]?.imageEmbedding;

  if (!Array.isArray(vector) || vector.length !== DIMENSION_EMBEDDING) {
    throw new Error("Vertex AI no devolvió un vector de imagen válido");
  }

  return vector;
}

/**
 * Descarga la imagen principal de un producto (la URL de descarga que ya
 * está guardada en Product.imagenes[0]) y la devuelve como JPEG en base64,
 * listo para mandar a Vertex AI.
 */
async function descargarImagenComoBase64(url: string): Promise<string> {
  const respuesta = await fetch(url);
  if (!respuesta.ok) {
    throw new Error(`No se pudo descargar la imagen principal (${respuesta.status})`);
  }
  const buffer = Buffer.from(await respuesta.arrayBuffer());
  return buffer.toString("base64");
}

/**
 * Mantiene "embeddingsVisuales" sincronizada con "productos", siempre en
 * automático — mismo criterio que GeneradorHuellas, al que reemplaza (ver
 * README): nadie tiene que pulsar nada.
 *   - Producto nuevo, o con imágenes distintas a las que tenía: se
 *     descarga la imagen principal (imagenes[0]) y se recalcula el
 *     embedding con Vertex AI.
 *   - Solo cambió "activo": se actualiza ese único campo, sin volver a
 *     llamar a Vertex AI.
 *   - Cualquier otro cambio (precio, descripción, etc.) sin que cambien las
 *     imágenes: no hace nada.
 *   - Producto borrado: se borra su documento de "embeddingsVisuales".
 * Cualquier error se registra en logs y nunca se relanza — un fallo aquí no
 * debe romper la publicación/edición del producto.
 */
export const sincronizarEmbeddingVisual = onDocumentWritten(
  { document: "productos/{productId}", region: REGION_VERTEX, memory: "512MiB", timeoutSeconds: 120 },
  async (event) => {
    const productId = event.params.productId;
    const antes = event.data?.before.exists ? event.data.before.data() : null;
    const despues = event.data?.after.exists ? event.data.after.data() : null;

    try {
      if (!despues) {
        await db.collection(COLECCION_EMBEDDINGS).doc(productId).delete();
        return;
      }

      const imagenesAntes = (antes?.imagenes as string[] | undefined) ?? [];
      const imagenesDespues = (despues.imagenes as string[] | undefined) ?? [];
      const imagenesCambiaron =
        !antes || JSON.stringify(imagenesAntes) !== JSON.stringify(imagenesDespues);

      if (imagenesCambiaron) {

        if (imagenesDespues.length === 0) {
          logger.warn(
            `sincronizarEmbeddingVisual: producto ${productId} sin imágenes, no se genera embedding`
          );
          return;
        }

        const imagenBase64 = await descargarImagenComoBase64(imagenesDespues[0]);
        const vector = await obtenerEmbeddingDeImagen(imagenBase64);

        await db.collection(COLECCION_EMBEDDINGS).doc(productId).set({
          productId,
          artesanoId: (despues.artesanoId as string | undefined) ?? "",
          activo: (despues.activo as boolean | undefined) ?? true,
          modelo: MODELO_EMBEDDING,
          version: VERSION_EMBEDDING,
          actualizadoEn: FieldValue.serverTimestamp(),
          embedding: FieldValue.vector(vector),
        });

      } else if (antes && antes.activo !== despues.activo) {

        await db.collection(COLECCION_EMBEDDINGS).doc(productId).update({
          activo: (despues.activo as boolean | undefined) ?? true,
        });
      }
    } catch (error) {
      logger.error(`sincronizarEmbeddingVisual: fallo con el producto ${productId}`, error);
    }
  }
);

/**
 * true si el usuario ya alcanzó su cupo de búsquedas visuales del día
 * ("limitesBusquedaVisual/{uid}"), y de paso incrementa el contador si
 * todavía tiene cupo — todo en una sola transacción para que dos búsquedas
 * simultáneas del mismo usuario no se cuelen ambas.
 */
async function usuarioExcedioLimiteDeBusquedas(uid: string): Promise<boolean> {
  const hoy = new Date().toISOString().slice(0, 10);
  const ref = db.collection("limitesBusquedaVisual").doc(uid);

  return db.runTransaction(async (tx) => {
    const snap = await tx.get(ref);
    const datos = snap.exists ? (snap.data() as { fecha: string; conteo: number }) : null;
    const conteoActual = datos && datos.fecha === hoy ? datos.conteo : 0;

    if (conteoActual >= LIMITE_BUSQUEDAS_DIARIAS) {
      return true;
    }

    tx.set(ref, { fecha: hoy, conteo: conteoActual + 1 });
    return false;
  });
}

/**
 * Búsqueda visual real (reemplaza la comparación local del dispositivo, ver
 * README): recibe una foto ya preprocesada por la app (JPEG en base64,
 * lado máximo ~800px), calcula su embedding con Vertex AI y busca los
 * productos más parecidos con Firestore findNearest sobre
 * "embeddingsVisuales". Descarta los productos inactivos y devuelve como
 * máximo LIMITE_RESULTADOS_FIND_NEAREST candidatos —
 * RecognitionViewModel resuelve los datos completos de cada producto con
 * ProductRepository.obtenerProductosPorIds.
 */
export const buscarPorImagen = onCall(
  { region: REGION_VERTEX, memory: "512MiB", timeoutSeconds: 60 },
  async (request) => {

    if (!request.auth) {
      throw new HttpsError("unauthenticated", "Debes iniciar sesión para buscar por imagen.");
    }

    const imagenBase64 = request.data?.imagenBase64;
    if (typeof imagenBase64 !== "string" || imagenBase64.length === 0) {
      throw new HttpsError("invalid-argument", "Falta la imagen a buscar.");
    }

    const tamanoBytesAprox = Math.ceil((imagenBase64.length * 3) / 4);
    if (tamanoBytesAprox > MAX_BYTES_IMAGEN_CONSULTA) {
      throw new HttpsError(
        "invalid-argument",
        "La imagen es demasiado grande. Usa una foto de lado máximo 800px."
      );
    }

    // El administrador queda exento del límite diario: lo necesita para correr la
    // evaluación comparativa (AdminVisualEvalScreen, ver README) contra decenas de
    // fotos de prueba en una sola corrida — el límite existe para frenar abuso de
    // un cliente normal, no el uso interno de quien ya tiene ese rol.
    const usuarioSnap = await db.collection("usuarios").doc(request.auth.uid).get();
    const esAdministrador = usuarioSnap.get("rol") === "administrador";

    if (!esAdministrador && (await usuarioExcedioLimiteDeBusquedas(request.auth.uid))) {
      throw new HttpsError(
        "resource-exhausted",
        `Alcanzaste el límite de ${LIMITE_BUSQUEDAS_DIARIAS} búsquedas visuales por día. Intenta mañana.`
      );
    }

    let vectorConsulta: number[];
    try {
      vectorConsulta = await obtenerEmbeddingDeImagen(imagenBase64);
    } catch (error) {
      logger.error("buscarPorImagen: fallo al calcular el embedding de consulta", error);
      throw new HttpsError("internal", "No se pudo analizar la imagen. Intenta de nuevo.");
    }

    let snapshot;
    try {
      const vectorQuery = db.collection(COLECCION_EMBEDDINGS).findNearest({
        vectorField: "embedding",
        queryVector: vectorConsulta,
        limit: LIMITE_RESULTADOS_FIND_NEAREST,
        distanceMeasure: "DOT_PRODUCT",
        distanceResultField: "similitud",
      });
      snapshot = await vectorQuery.get();
    } catch (error) {
      logger.error("buscarPorImagen: falló la búsqueda vectorial", error);
      throw new HttpsError("internal", "No se pudo completar la búsqueda. Intenta de nuevo.");
    }

    const resultados = snapshot.docs
      .map((doc) => {
        const datos = doc.data();
        return {
          productId: datos.productId as string,
          artesanoId: datos.artesanoId as string,
          activo: (datos.activo as boolean | undefined) ?? true,
          similitud: datos.similitud as number,
        };
      })
      .filter((resultado) => resultado.activo)
      .map(({ productId, artesanoId, similitud }) => ({ productId, artesanoId, similitud }));

    return { resultados };
  }
);

// Se reexportan para que el script de relleno único (src/scripts/rellenarEmbeddings.ts)
// reutilice exactamente la misma lógica que el trigger, sin duplicarla.
export {
  db,
  obtenerEmbeddingDeImagen,
  descargarImagenComoBase64,
  COLECCION_EMBEDDINGS,
  MODELO_EMBEDDING,
  VERSION_EMBEDDING,
};
