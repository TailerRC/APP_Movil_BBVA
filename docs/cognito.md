# Manual 1 · Amazon Cognito (solo con la consola de AWS)

**Objetivo:** crear el User Pool que identifica a los usuarios, el app client que permite iniciar sesión, los grupos `cliente` y `negocio`, y 2 usuarios de prueba.

- **Región:** `us-east-2` (Ohio). Toda la arquitectura va en esta región.
- **No se usa CloudShell ni línea de comandos.** Todo se hace con clics en la consola.
- **Al terminar tendrás anotados:** `POOL_ID` y `CLIENT_ID`.

| Elemento | Valor |
|---|---|
| User Pool | `bian-discount-pricing-users` (inicio de sesión con **correo**) |
| App client | `bian-discount-pricing-app` (**sin secreto**, flujo usuario + contraseña) |
| Atributo personalizado | `customerReference` (en el token aparece como `custom:customerReference`) |
| Grupos | `cliente`, `negocio` |
| Usuario cliente | `cliente1@example.com` · contraseña final `Cliente2026` · `customerReference` = `PARTY-000024` · grupo `cliente` |
| Usuario negocio | `negocio1@example.com` · contraseña final `Negocio2026` · grupo `negocio` |

> Credenciales de demostración, solo para este laboratorio. Los nombres de botones pueden variar un poco según la versión de la consola; la idea de cada paso es la misma.

---

## PASO 0 · Entrar a Cognito y verificar la región

1. Inicia sesión en la consola de AWS.
2. Arriba a la derecha, en el selector de región, elige **US East (Ohio) us-east-2**.
3. En la barra de búsqueda superior escribe **Cognito** y entra a **Amazon Cognito**.
4. En el menú izquierdo entra a **User pools** (Grupos de usuarios). Confirma que la región sigue siendo Ohio.

## PASO 1 · Crear el User Pool

1. Pulsa **Create user pool** (Crear grupo de usuarios).
2. **Define your application** (Define tu aplicación):
   - **Application type:** `Single-page application (SPA)`. Esta opción **no genera secreto** en el app client, que es lo que necesitamos.
   - **Name your application:** `bian-discount-pricing-app`. Ese nombre será el del app client.
3. **Configure options** (Configurar opciones):
   - **Options for sign-in identifiers:** marca **solo `Email`**. Desmarca cualquier otra (Username, Phone number).
   - **Self-registration:** desmárcalo (los usuarios los creamos nosotros).
   - **Required attributes for sign-up:** deja `email`.
4. Si te pide un **Return URL**, puedes dejarlo vacío o poner `http://localhost`. No se usa.
5. Pulsa **Create user directory** (Crear directorio de usuarios).
6. Cuando termine, entra al pool que se creó (click en su nombre).

> **Nombre del pool:** si la consola permite editarlo (Settings / Configuración → Edit), ponle `bian-discount-pricing-users`. Si no, no pasa nada: lo importante es el **User pool ID**. Más adelante en API Gateway reconocerás el pool por ese ID.

## PASO 2 · Anotar el POOL_ID

1. En la pantalla **Overview** del pool copia el **User pool ID**. Se ve así: `us-east-2_AbCdEfGhI`.
2. Pégalo en un bloc de notas como `POOL_ID`.

## PASO 3 · Ajustar la política de contraseñas

Las contraseñas de prueba (`Cliente2026`, `Negocio2026`) **no llevan símbolo**, y la política por defecto de Cognito sí lo exige. Hay que relajarla.

1. Menú izquierdo → **Authentication** → **Authentication methods** (Métodos de autenticación). Si en tu consola aparece en **Sign-in**, busca ahí.
2. Busca **Password policy** (Política de contraseñas) → **Edit** (Editar).
3. **Password policy mode:** `Custom`.
4. **Minimum length:** `8`.
5. Marca: **Contains at least 1 number**, **Contains at least 1 uppercase letter**, **Contains at least 1 lowercase letter**.
6. **Desmarca** `Contains at least 1 special character`.
7. **Save changes**.

## PASO 4 · Crear el atributo personalizado `customerReference`

1. Menú izquierdo → **Authentication** → **Sign-up** (Registro).
2. Baja hasta **Custom attributes** (Atributos personalizados) → **Add custom attributes**.
3. Completa:
   - **Attribute name:** `customerReference` (exactamente así, respetando mayúsculas).
   - **Type:** `String`.
   - **Min length:** `1` · **Max length:** `256`.
   - **Mutable:** marcado (Sí).
4. **Save changes**.

> Los atributos personalizados **no se pueden borrar ni cambiar de tipo** luego. Por eso se escribe con cuidado.

## PASO 5 · Configurar el app client (muy importante)

### 5.1 Activar el login con usuario y contraseña

1. Menú izquierdo → **Applications** → **App clients**.
2. Entra al client `bian-discount-pricing-app`.
3. Pestaña **App client information** → **Edit**.
4. En **Authentication flows** marca:
   - `Sign in with username and password: ALLOW_USER_PASSWORD_AUTH`
   - `Get new user tokens from existing authenticated sessions: ALLOW_REFRESH_TOKEN_AUTH`
5. **Save changes**.

> Si esto no se activa, Postman responderá `USER_PASSWORD_AUTH flow not enabled for this client`.

### 5.2 Permitir que el token incluya `customerReference`

El atributo se creó **después** del app client, así que el client todavía no tiene permiso para leerlo. Sin esto, la Lambda no verá el cliente en el token y siempre responderá 403.

1. Dentro del mismo app client, busca **Attribute permissions** (Permisos de atributos) → **Edit**.
2. En **Read attributes** (Atributos de lectura) marca `email`, `email_verified` y `custom:customerReference`.
3. En **Write attributes** puedes dejar los que vengan por defecto.
4. **Save changes**.

### 5.3 Anotar el CLIENT_ID

1. En **App client information** copia el **Client ID**.
2. Pégalo en tu bloc de notas como `CLIENT_ID`.
3. Verifica que **Client secret** diga que no hay secreto (vacío / "-").

## PASO 6 · Crear los grupos

1. Menú izquierdo → **User management** → **Groups**.
2. **Create group**:
   - **Group name:** `cliente`
   - **Description:** `Clientes del banco`
   - Deja **IAM role** vacío. (No se usa IAM.)
   - **Create group**.
3. Repite: **Create group**:
   - **Group name:** `negocio`
   - **Description:** `Usuarios de negocio`
   - **Create group**.

## PASO 7 · Crear el usuario cliente

1. Menú izquierdo → **User management** → **Users** → **Create user**.
2. **Invitation message:** elige `Don't send an invitation` (no enviar invitación).
3. **Email address:** `cliente1@example.com`.
4. Marca **Mark email address as verified**.
5. **Temporary password / Password:** elige **Set a password** y escribe una temporal: `Temporal2026`.
6. Si el formulario muestra el campo **customerReference**, escribe `PARTY-000024`. Si no aparece, se agrega después (paso 7.2).
7. **Create user**.

### 7.1 Añadirlo al grupo

1. Entra al usuario `cliente1@example.com`.
2. Sección **Group memberships** → **Add user to group** → elige `cliente` → **Add**.

### 7.2 Asignar `customerReference` (si no se pudo al crear)

1. En el usuario, sección **User attributes** → **Edit**.
2. Agrega `customerReference` = `PARTY-000024`.
3. **Save changes**. Debe verse como `custom:customerReference`.

## PASO 8 · Crear el usuario de negocio

1. **Users** → **Create user**.
2. **Invitation message:** `Don't send an invitation`.
3. **Email address:** `negocio1@example.com` y marca **Mark email address as verified**.
4. **Set a password:** `Temporal2026`.
5. **Create user**.
6. Entra al usuario → **Group memberships** → **Add user to group** → `negocio` → **Add**.

## PASO 9 · Estado "Force change password" (qué es y qué hacer)

En la lista **Users** verás que ambos usuarios están en estado **Force change password** (Forzar cambio de contraseña). Es normal: la consola crea la contraseña como **temporal**.

- Ese estado se resuelve **la primera vez que se hace login**, enviando la contraseña definitiva (`Cliente2026` / `Negocio2026`).
- Eso se hace desde Postman en las peticiones **PRE-1 a PRE-4** del manual `postman.md`. Es una sola vez por usuario.
- Si tu consola ya los muestra como **Confirmed**, no necesitas el paso PRE.

## PASO 10 · Qué lleva el token

Cuando el login funciona, Cognito devuelve un **ID token** (JWT) con:

| Claim | Valor |
|---|---|
| `email` | correo del usuario |
| `cognito:groups` | `cliente` o `negocio` |
| `custom:customerReference` | `PARTY-000024` (solo el cliente) |

- Dura **1 hora**. Al vencer, se repite el login (00A / 00B).
- La API recibe el **ID token**, no el *access token*.

---

## Checklist de salida

- [ ] User Pool creado en **us-east-2** y `POOL_ID` anotado
- [ ] Sign-in solo con correo
- [ ] Política de contraseñas sin exigir símbolo
- [ ] Atributo `customerReference` creado
- [ ] App client **sin secreto** con `ALLOW_USER_PASSWORD_AUTH` activo y `CLIENT_ID` anotado
- [ ] El app client puede **leer** `custom:customerReference`
- [ ] Grupos `cliente` y `negocio`
- [ ] `cliente1@example.com` en `cliente`, con `customerReference` = `PARTY-000024`
- [ ] `negocio1@example.com` en `negocio`

**Capturas para entregar:** pantalla Overview del pool, lista de grupos, lista de usuarios con sus grupos, y el app client con sus flujos de autenticación.

## Errores comunes

| Error | Solución |
|---|---|
| No aparece el pool en otra pantalla | Región equivocada. Debe ser **us-east-2**. |
| `USER_PASSWORD_AUTH flow not enabled for this client` | Paso 5.1: activar `ALLOW_USER_PASSWORD_AUTH`. |
| `Password does not conform to policy` al crear usuario | Paso 3: la política sigue exigiendo símbolo. Usa `Temporal2026` y revisa la política. |
| `Incorrect username or password` | Se usa el correo completo como usuario. Revisa la contraseña definitiva. |
| El token no trae `custom:customerReference` | Paso 5.2 (permiso de lectura) y paso 7.2 (el usuario no tiene el atributo). Vuelve a hacer login para un token nuevo. |
| El token no trae `cognito:groups` | El usuario no está en ningún grupo (pasos 7.1 y 8.6). |

**Siguiente manual:** `lambda.md` (si aún no lo hiciste) y luego `apigateway.md`.
