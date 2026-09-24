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

## 5. Configura tu cuenta de Gmail como secreto
El correo se envía por el SMTP de Gmail (con `nodemailer`), así que necesitas una cuenta de
Google con verificación en dos pasos activada — puede ser tu cuenta actual o una nueva tipo
`minkaart.app@gmail.com`.

1. Activa la **verificación en dos pasos** en esa cuenta, si no la tiene.
2. Crea una **contraseña de aplicación** en <https://myaccount.google.com/apppasswords>.
   Google te da 16 caracteres — no es tu contraseña normal de Gmail, es una clave aparte y
   revocable en cualquier momento.
3. Registra los dos secretos. Nunca van en el código ni en el repo. Desde la carpeta
   `functions`:
   ```
   firebase functions:secrets:set GMAIL_USER
   firebase functions:secrets:set GMAIL_APP_PASSWORD
   ```
   Te va a pedir que pegues el valor de cada uno — pégalo ahí (en la terminal, no en el
   chat) y presiona Enter. En `GMAIL_USER` va la dirección de Gmail completa, en
   `GMAIL_APP_PASSWORD` la contraseña de aplicación de 16 caracteres.

Gmail tiene un límite de envío de aproximadamente 500 correos/día, suficiente para
desarrollo y pruebas. Si más adelante se necesita un remitente con dominio propio y sin ese
límite, hay que verificar un dominio en un proveedor transaccional (p. ej. Resend) y
cambiar el bloque de envío en `src/index.ts`.

## 6. Despliega las funciones
```
firebase deploy --only functions
```

Cuando termine, vas a tener dos funciones activas: `enviarCodigoVerificacion` y `verificarCodigo`. Con eso, el siguiente paso es conectar tu app Android a ellas (eso lo hacemos cuando me pases los archivos de Kotlin).
