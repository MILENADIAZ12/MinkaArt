# Cómo instalar esto en tu proyecto MinkaArt

## 1. Instala Firebase CLI (si no la tienes)
```
npm install -g firebase-tools
firebase login
```

## 2. Inicializa Functions en la raíz de tu proyecto
Desde la carpeta raíz (donde está `settings.gradle`):
```
firebase init functions
```
- Selecciona tu proyecto de Firebase existente.
- Lenguaje: **TypeScript**.
- ESLint: no hace falta, puedes decir que no.
- Cuando pregunte si sobrescribir archivos, dile que **no** todavía.

Esto crea una carpeta `functions/` con `package.json`, `tsconfig.json` y `src/index.ts` genéricos.

## 3. Reemplaza los archivos generados
Copia estos 4 archivos (los que te di) dentro de tu carpeta `functions/`, reemplazando los que creó el `init`:
- `package.json`
- `tsconfig.json`
- `.gitignore`
- `src/index.ts`

## 4. Instala las dependencias
```
cd functions
npm install
```

## 5. Configura Resend como secreto
El correo se envía con la API de [Resend](https://resend.com) (código de verificación y enlace de cambio de contraseña).

1. Crea la cuenta de Resend con `minka.art.app@gmail.com` y genera una API key en <https://resend.com/api-keys> (empieza por `re_`).
2. Desde la carpeta `functions`: `firebase functions:secrets:set RESEND_API_KEY` y pega la key en la terminal.
3. Resend no permite enviar desde `@gmail.com`. Sin dominio propio solo sirve `onboarding@resend.dev` (ya configurado), que únicamente entrega al correo de tu cuenta de Resend. Para enviar a cualquier usuario: Resend → Domains → Add Domain, crea los registros DNS (SPF/DKIM) y cambia `REMITENTE` en `src/index.ts`.

## 6. Despliega las funciones
```
firebase deploy --only functions
```

Cuando termine, vas a tener dos funciones activas: `enviarCodigoVerificacion` y `verificarCodigo`. Con eso, el siguiente paso es conectar tu app Android a ellas (eso lo hacemos cuando me pases los archivos de Kotlin).
