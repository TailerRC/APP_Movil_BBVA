# BBVA Bonifica — Diseño de la app Android (Kotlin · Jetpack Compose)

App móvil de **descuentos en comisiones** (BIAN v12 · Discount Pricing) para clientes del banco, con una vista adicional para el personal de negocio. Consume el backend serverless ya construido: **Cognito + API Gateway + Lambda `DiscountPricingHandler` + DynamoDB** (región `us-east-2`), descrito en `cognito.md`, `lambda.md`, `apigateway.md`, `dynamobd.md` y `postman.md`.

> **Objetivo de este documento:** que el equipo pueda construir toda la app **sin depender de AWS** (con datos simulados) y que la integración final se reduzca a **cambiar un interruptor y dos constantes**.

---

## 1. Identidad de la app

| Elemento | Valor |
|---|---|
| **Nombre** | **BBVA Bonifica** (bonificación = descuento; corto y fácil de recordar) |
| Eslogan | *Paga menos comisión.* |
| Package | `com.bbva.bonifica` |
| IDE | Android Studio (proyecto ya creado: `APP_Movil_BBVA`) |
| Lenguaje / UI | Kotlin · Jetpack Compose · Material 3 |
| minSdk / targetSdk | 26 / 35 |
| Idioma | Español (Perú). Moneda: `S/` (PEN) y `US$` (USD) |

Alternativas de nombre si el equipo prefiere otro: *BBVA Tarifa Justa*, *BBVA Menos Comisión*. El nombre solo cambia en `strings.xml` (`app_name`).

## 2. Alcance

| Incluye | No incluye |
|---|---|
| Login real con **Cognito** (correo + contraseña), primer ingreso y renovación de sesión | Registro de usuarios nuevos (se crean en la consola de Cognito) |
| Dos roles según grupo de Cognito: `cliente` y `negocio` | Creación de operaciones comerciales (la app usa las existentes) |
| Las **15 rutas** de la API consumidas | Notificaciones push, biometría |
| Cálculo, confirmación y anulación de descuentos | Pasarela de pago real (el pago lo genera la Lambda) |
| Modo simulado (*mock*) para trabajar sin AWS | |

**Nada es opcional:** las 15 rutas, los 2 roles y todos los flujos de Cognito (login, nueva contraseña, refresh, logout) están en el alcance.

---

## 3. Identidad visual (colores BBVA)

Paleta basada en la marca BBVA. Verificar los hex exactos contra la guía de marca vigente; están centralizados en un solo archivo (`ui/theme/Color.kt`) para ajustarlos en un minuto.

| Token | Hex | Uso |
|---|---|---|
| `BbvaNavy` | `#072146` | Texto principal, barras superiores oscuras |
| `BbvaBlue` | `#004481` | **Color primario**: botones, AppBar, íconos activos |
| `BbvaBlueMedium` | `#1464A5` | Estados presionados, enlaces |
| `BbvaBlueLight` | `#1973B8` | Acentos, chips seleccionados |
| `BbvaAqua` | `#2DCCCD` | Acento secundario, resaltar ahorro/descuento |
| `BbvaSky` | `#5BBEFF` | Fondos suaves, ilustraciones |
| `BbvaWhite` | `#FFFFFF` | Fondo de tarjetas |
| `BbvaGray50` | `#F4F4F4` | Fondo de pantallas |
| `BbvaGray300` | `#C9CED6` | Bordes, divisores |
| `BbvaGray700` | `#5C6670` | Texto secundario |
| `Success` | `#48AE64` | Pago `PAID`, estado `APPLIED` |
| `Warning` | `#F8CD51` | `PENDING`, `INITIATED` |
| `Error` | `#DA3851` | Errores, `CANCELLED` |

**Reglas de diseño**
- Fondo claro (`BbvaGray50`), tarjetas blancas con esquinas de 16 dp y elevación suave.
- Botón primario: `BbvaBlue`, texto blanco, alto 52 dp, esquinas de 26 dp. Botón secundario: contorno `BbvaBlue`.
- El **ahorro** (monto del descuento) siempre se muestra en `BbvaAqua`/`Success` para que destaque.
- El **monto neto** es el número más grande de la pantalla de resultado.
- Tipografía: la de BBVA (*Benton Sans*) es propietaria; usar `Roboto` (por defecto) o `Inter` como reemplazo.
- Chips de estado con color fijo (ver §8.3).

---

## 4. Arquitectura

### 4.1 Vista general

```
┌──────────────────────── App Android (Kotlin) ────────────────────────┐
│  UI (Compose)  ──►  ViewModel (StateFlow)  ──►  Repository (interfaz) │
│                                                    │                   │
│                                  ┌─────────────────┴───────────────┐   │
│                                  ▼                                 ▼   │
│                       Implementación MOCK              Implementación REMOTE
│                       (datos de la demo)               (Retrofit + Cognito REST)
└───────────────────────────────────────────────────────┬────────────────┘
                                                        │ HTTPS · JSON
                                                        ▼
                         Cognito  ·  API Gateway (dev)  ·  Lambda  ·  DynamoDB
```

**Patrón:** MVVM + Clean ligera (3 capas: `ui` → `domain` → `data`).
**Regla de oro:** la UI y los ViewModels **solo conocen interfaces de Repository**. Que detrás haya datos simulados o AWS real es invisible para ellos. Por eso la integración final no toca pantallas.

### 4.2 Estructura de paquetes

```
app/src/main/java/com/bbva/bonifica/
├── BonificaApp.kt                  Application (crea el AppContainer)
├── MainActivity.kt                 setContent { BonificaTheme { AppNavHost() } }
├── AppConfig.kt                    ★ ÚNICO archivo que se edita al integrar AWS
├── di/
│   └── AppContainer.kt             Inyección manual: elige Mock o Remote
│
├── core/
│   ├── Result.kt                   sealed AppResult<T> { Success, Failure(AppError) }
│   ├── AppError.kt                 BadRequest, Unauthorized, Forbidden, NotFound, Conflict, Server, Network
│   ├── Formatters.kt               formatMoney(), formatDate(), maskDni()
│   └── UiState.kt                  Loading / Content / Empty / Error
│
├── domain/
│   ├── model/                      Modelos puros (ver §6)
│   └── repository/                 Interfaces (ver §7)
│
├── data/
│   ├── mock/                       FakeAuthRepository, FakeCustomerRepository, ... + MockData.kt
│   ├── remote/
│   │   ├── api/                    BonificaApi.kt (Retrofit), CognitoApi.kt
│   │   ├── dto/                    DTOs JSON + mappers a domain
│   │   └── interceptor/            AuthInterceptor, TokenAuthenticator
│   ├── auth/                       SessionManager, TokenStore, JwtDecoder
│   └── repository/                 RemoteAuthRepository, RemoteCustomerRepository, ...
│
└── ui/
    ├── theme/                      Color.kt, Type.kt, Shape.kt, Theme.kt
    ├── components/                 Componentes reutilizables (ver §9)
    ├── navigation/                 Routes.kt, AppNavHost.kt, BottomBars.kt
    ├── acceso/                     [Módulo 1]
    ├── catalogo/                   [Módulo 2]
    ├── descuentos/                 [Módulo 3]
    ├── promociones/                [Módulo 4]
    ├── operaciones/                [Módulo 5]
    ├── pagos/                      [Módulo 6]
    └── negocio/                    [Módulo 7]
```

Cada carpeta de módulo contiene: `XxxScreen.kt` (Composable), `XxxViewModel.kt` y `XxxUiState.kt`.

### 4.3 Dependencias (Gradle, `libs.versions.toml` / `build.gradle.kts`)

| Librería | Para qué |
|---|---|
| `androidx.compose` BOM + `material3` + `material-icons-extended` | UI |
| `androidx.navigation:navigation-compose` | Navegación |
| `androidx.lifecycle:lifecycle-viewmodel-compose`, `lifecycle-runtime-compose` | ViewModel / `collectAsStateWithLifecycle` |
| `org.jetbrains.kotlinx:kotlinx-coroutines-android` | Corrutinas |
| `com.squareup.retrofit2:retrofit` + `okhttp` + `logging-interceptor` | Red |
| `org.jetbrains.kotlinx:kotlinx-serialization-json` + `retrofit2-kotlinx-serialization-converter` | JSON |
| `androidx.security:security-crypto` | `EncryptedSharedPreferences` (refresh token) |
| `io.coil-kt:coil-compose` | Imágenes (logo/ilustraciones) |
| Test: `junit`, `kotlinx-coroutines-test`, `mockk`, `androidx.compose.ui:ui-test-junit4` | Pruebas |

Permiso en `AndroidManifest.xml`: solo `<uses-permission android:name="android.permission.INTERNET" />`.

> **Sin Hilt a propósito:** se usa un `AppContainer` manual para reducir configuración y que todos entiendan el cableado. Si el equipo ya domina Hilt, se puede migrar sin cambiar interfaces.

### 4.4 `AppConfig.kt` (el punto de integración con AWS)

```kotlin
object AppConfig {
    /** true = datos simulados (sin internet). false = AWS real. */
    const val USE_MOCK = true

    // Se rellenan al integrar (valores de apigateway.md y cognito.md)
    const val BASE_URL = "https://REEMPLAZAR.execute-api.us-east-2.amazonaws.com/dev/" // con "/" al final
    const val COGNITO_URL = "https://cognito-idp.us-east-2.amazonaws.com/"
    const val COGNITO_CLIENT_ID = "REEMPLAZAR"   // CLIENT_ID del app client (sin secreto)

    const val TOKEN_REFRESH_MARGIN_SECONDS = 300 // renovar 5 min antes de vencer
}
```

### 4.5 `AppContainer.kt` (elige la implementación)

```kotlin
class AppContainer(context: Context) {
    private val session = SessionManager(TokenStore(context))

    val authRepository: AuthRepository =
        if (AppConfig.USE_MOCK) FakeAuthRepository(session) else RemoteAuthRepository(cognitoApi, session)
    val customerRepository: CustomerRepository =
        if (AppConfig.USE_MOCK) FakeCustomerRepository() else RemoteCustomerRepository(api)
    // ... igual para Catalog, Operations, Discount, Promotion, Payment, Admin
}
```

Los ViewModels reciben los repositorios por una `ViewModelProvider.Factory` que lee de `BonificaApp.container`.

---

## 5. Autenticación con Cognito (módulo 1)

Se usa **Cognito por REST** (igual que las peticiones de Postman 00A/PRE-1/PRE-2): sin SDK pesado, con el app client sin secreto y `ALLOW_USER_PASSWORD_AUTH` activado.

Todas las llamadas a Cognito: `POST https://cognito-idp.us-east-2.amazonaws.com/` con cabeceras
`Content-Type: application/x-amz-json-1.1` y `X-Amz-Target: AWSCognitoIdentityProviderService.<Operación>`.

> ⚠️ En Retrofit el `Content-Type` debe ser exactamente `application/x-amz-json-1.1`. Crear el `RequestBody` con ese `MediaType` (no usar el converter por defecto, que pondría `application/json`).

| Función | Operación (`X-Amz-Target`) | Cuerpo |
|---|---|---|
| Login | `InitiateAuth` | `{"AuthFlow":"USER_PASSWORD_AUTH","ClientId":"…","AuthParameters":{"USERNAME":"…","PASSWORD":"…"}}` |
| Primer ingreso | `RespondToAuthChallenge` | `{"ChallengeName":"NEW_PASSWORD_REQUIRED","ClientId":"…","Session":"…","ChallengeResponses":{"USERNAME":"…","NEW_PASSWORD":"…"}}` |
| Renovar | `InitiateAuth` | `{"AuthFlow":"REFRESH_TOKEN_AUTH","ClientId":"…","AuthParameters":{"REFRESH_TOKEN":"…"}}` |

**Flujo de login**
1. Usuario escribe correo y contraseña → `InitiateAuth`.
2. Si responde `ChallengeName = NEW_PASSWORD_REQUIRED` (usuario en "Force change password") → la app navega a `NuevaContrasenaScreen`, guarda el `Session` (vale ~3 min) y llama a `RespondToAuthChallenge`.
3. Con `AuthenticationResult` se obtienen `IdToken`, `AccessToken`, `RefreshToken`, `ExpiresIn` (3600 s).
4. `JwtDecoder` decodifica el **IdToken** (Base64 URL, parte central) y extrae:

| Claim | Uso en la app |
|---|---|
| `email` | Mostrar en el perfil |
| `cognito:groups` | **Rol**: `cliente` → vista cliente · `negocio` → vista negocio |
| `custom:customerReference` | Código del cliente (`PARTY-000024`) para las rutas propias |
| `exp` | Saber cuándo renovar |

5. Se guarda la sesión: IdToken y datos en **memoria**; RefreshToken en `EncryptedSharedPreferences`.

**Reglas**
- La API recibe el **IdToken** (no el AccessToken). Cabecera: `Authorization: <IdToken>`.
- `AuthInterceptor` agrega la cabecera a cada llamada a API Gateway.
- `TokenAuthenticator` (OkHttp `Authenticator`): ante un **401**, intenta `REFRESH_TOKEN_AUTH` una sola vez y reintenta; si falla, limpia la sesión y navega a Login.
- Al abrir la app, si hay RefreshToken guardado, se intenta restaurar sesión (pantalla *Splash*).
- Logout: borra memoria y `EncryptedSharedPreferences`.
- **No escribir contraseñas de demo en el código.** Se escriben en el login.

**Errores de Cognito a mensajes**

| Respuesta de Cognito | Mensaje en la app |
|---|---|
| `NotAuthorizedException` | "Correo o contraseña incorrectos" |
| `UserNotFoundException` | "Correo o contraseña incorrectos" (no revelar cuál) |
| `InvalidPasswordException` | "La contraseña no cumple: mín. 8, mayúscula, minúscula y número" |
| `PasswordResetRequiredException` | "Debes restablecer tu contraseña con el banco" |
| Sin conexión | "Sin conexión a internet" |

**Usuarios de prueba** (de `cognito.md`): `cliente1@example.com` (grupo `cliente`, `PARTY-000024`) y `negocio1@example.com` (grupo `negocio`).

---

## 6. Modelos de dominio (`domain/model`)

Basados en las 8 tablas de `dynamobd.md`. Los importes son `Double` (la Lambda los devuelve como número JSON).

```kotlin
enum class Role { CLIENTE, NEGOCIO }

data class Session(
    val email: String, val role: Role, val customerReference: String?, // null si negocio
    val idToken: String, val expiresAtEpochSec: Long
)

data class Customer(
    val customerReference: String, val name: String, val documentType: String,
    val documentNumber: String, val email: String, val phone: String,
    val segment: String,        // PERSONAL | PREFERENTE | PREMIUM | JOVEN | PYME
    val city: String, val status: String, // ACTIVE | INACTIVE
    val customerSince: String
)

data class Product(
    val productReference: String, val name: String,
    val category: String,       // CUENTAS | TARJETAS | PRESTAMOS | SEGUROS | TRANSFERENCIAS
    val listPrice: Double, val currency: String,       // PEN | USD
    val billingFrequency: String, // MONTHLY | ANNUAL | PER_OPERATION | ONE_TIME
    val status: String          // AVAILABLE | DISCONTINUED
)

data class PointOfSale(
    val pointOfSaleReference: String, val customerReference: String,
    val channel: String,        // MOBILE_APP | WEB | BRANCH | ATM | AGENT
    val officeCode: String, val officeName: String, val city: String,
    val status: String,         // OPEN | CLOSED
    val createdAt: String
)

data class DiscountPricing(           // Control Record: Discount Pricing Transaction
    val discountPricingId: String, val pointOfSaleReference: String,
    val customerReference: String, val productReference: String,
    val category: String, val channel: String, val currency: String,
    val listPrice: Double, val quantity: Int, val grossAmount: Double,
    val policyId: String,       // "NONE" si no hubo descuento
    val policyName: String, val discountAmount: Double, val netAmount: Double,
    val status: String,         // INITIATED | APPLIED | CANCELLED
    val createdAt: String, val updatedAt: String,
    val paymentOrderId: String? = null // solo viene en la respuesta de Update→APPLIED
)

data class Policy(
    val policyId: String, val name: String,
    val discountType: String,   // PERCENTAGE | FIXED_AMOUNT
    val discountValue: Double,
    val scope: String,          // PRODUCT | CATEGORY | ALL
    val scopeReference: String, val minimumAmount: Double, val currency: String,
    val channel: String,        // ALL | MOBILE_APP | WEB | BRANCH | ATM | AGENT
    val validFrom: String, val validTo: String, val status: String
)

data class Campaign(
    val campaignId: String, val policyId: String, val name: String,
    val targetSegment: String, val channel: String, val budget: Double,
    val currency: String, val startDate: String, val endDate: String,
    val status: String          // ACTIVE | SCHEDULED | FINISHED | PAUSED
)

data class PaymentOrder(
    val paymentOrderId: String, val discountPricingId: String,
    val customerReference: String, val pointOfSaleReference: String,
    val amount: Double, val currency: String,
    val paymentMethod: String,  // CARGO_EN_CUENTA | TARJETA_DEBITO | EFECTIVO_VENTANILLA
    val status: String,         // PAID | PENDING | CANCELLED
    val paymentDate: String
)

data class EventLogEntry(
    val discountPricingId: String, val eventTimestamp: String,
    val eventType: String, val previousStatus: String?, val newStatus: String?,
    val actor: String, val channel: String?, val policyId: String, val details: String
)

data class NewPolicy(                  // formulario del módulo 7 (ruta 5)
    val policyId: String, val name: String, val discountType: String,
    val discountValue: Double, val scope: String, val scopeReference: String = "*",
    val minimumAmount: Double = 0.0, val currency: String = "PEN",
    val channel: String = "ALL", val validFrom: String = "2026-01-01",
    val validTo: String = "2026-12-31"
)
```

> Los campos opcionales del JSON (`previousStatus`, `newStatus`, `channel` en el historial, `paymentOrderId`) pueden faltar en datos sembrados: declararlos `nullable` y usar `ignoreUnknownKeys = true` en `Json {}`.

---

## 7. Interfaces de repositorio (`domain/repository`)

Todas las funciones son `suspend` y devuelven `AppResult<T>`.

```kotlin
interface AuthRepository {
    suspend fun login(email: String, password: String): AppResult<LoginOutcome>
    suspend fun setNewPassword(email: String, newPassword: String, session: String): AppResult<Session>
    suspend fun restoreSession(): Session?          // usa refresh token guardado
    suspend fun refresh(): AppResult<Session>
    fun currentSession(): Session?
    fun logout()
}
sealed interface LoginOutcome { data class Ok(val s: Session): LoginOutcome
                                data class NewPasswordRequired(val cognitoSession: String): LoginOutcome }

interface CustomerRepository {
    suspend fun getCustomer(ref: String): AppResult<Customer>                       // ruta 11
    suspend fun getOperations(customerRef: String): AppResult<List<PointOfSale>>    // ruta 14
}
interface CatalogRepository {
    suspend fun getByCategory(category: String): AppResult<List<Product>>           // ruta 13
    suspend fun getProduct(ref: String): AppResult<Product>                          // ruta 9
}
interface OperationRepository {
    suspend fun getOperation(ref: String): AppResult<PointOfSale>                    // ruta 10
    suspend fun getDiscountsByOperation(ref: String): AppResult<List<DiscountPricing>> // ruta 8
}
interface DiscountRepository {
    suspend fun initiate(posRef: String, productRef: String, quantity: Int): AppResult<DiscountPricing> // ruta 1
    suspend fun get(id: String): AppResult<DiscountPricing>                           // ruta 2
    suspend fun update(id: String, status: String, paymentMethod: String?): AppResult<DiscountPricing> // ruta 3
}
interface PromotionRepository {
    suspend fun getActivePolicies(): AppResult<List<Policy>>                          // ruta 6
    suspend fun getCampaigns(status: String): AppResult<List<Campaign>>              // ruta 7
}
interface PaymentRepository {
    suspend fun getPayment(id: String): AppResult<PaymentOrder>                      // ruta 12
    suspend fun getPaymentsByOperation(posRef: String): AppResult<List<PaymentOrder>> // ruta 15
    suspend fun getHistory(discountId: String): AppResult<List<EventLogEntry>>       // ruta 4
}
interface AdminRepository {
    suspend fun registerPolicy(p: NewPolicy): AppResult<Policy>                      // ruta 5
}
```

---

## 8. Contrato de la API (para la capa remota)

`BASE_URL` termina en `/` y las rutas **no** llevan `/` inicial.

### 8.1 Las 15 rutas

| # | Método y ruta | Quién | Respuesta | Repo |
|---|---|---|---|---|
| 1 | `POST v1/DiscountPricing/Initiate` | ambos | 201 `DiscountPricing` | Discount |
| 2 | `GET v1/DiscountPricing/{id}/Retrieve` | ambos | 200 `DiscountPricing` | Discount |
| 3 | `PUT v1/DiscountPricing/{id}/Update` | ambos | 200 `DiscountPricing` (+`paymentOrderId`) | Discount |
| 4 | `GET v1/DiscountPricing/{id}/EventLog/Retrieve` | ambos | 200 `{count, events[]}` | Payment |
| 5 | `POST v1/DiscountPricing/Policy/Register` | **solo negocio** | 201 `Policy` · 409 existe | Admin |
| 6 | `GET v1/DiscountPricing/Policy/Retrieve` | ambos | 200 `{count, policies[]}` | Promotion |
| 7 | `GET v1/DiscountPricing/Campaign/Retrieve?status=` | ambos | 200 `{count, campaigns[]}` | Promotion |
| 8 | `GET v1/DiscountPricing/PointOfSale/{ref}/Retrieve` | ambos | 200 `{count, transactions[]}` | Operation |
| 9 | `GET v1/ProductDirectory/{ref}/Retrieve` | ambos | 200 `Product` | Catalog |
| 10 | `GET v1/PointOfSale/{ref}/Retrieve` | ambos | 200 `PointOfSale` | Operation |
| 11 | `GET v1/Customer/{ref}/Retrieve` | cliente: solo el suyo · negocio: cualquiera | 200 `Customer` · 403 | Customer |
| 12 | `GET v1/PaymentOrder/{id}/Retrieve` | ambos | 200 `PaymentOrder` | Payment |
| 13 | `GET v1/ProductDirectory/Retrieve?category=` | ambos | 200 `{count, products[]}` · 400 sin categoría | Catalog |
| 14 | `GET v1/Customer/{ref}/PointOfSale/Retrieve` | cliente: solo el suyo · negocio: cualquiera | 200 `{count, operations[]}` | Customer |
| 15 | `GET v1/PointOfSale/{ref}/PaymentOrder/Retrieve` | ambos | 200 `{count, payments[]}` | Payment |

### 8.2 Cuerpos de petición

```json
// Ruta 1 · Initiate
{ "pointOfSaleReference": "SALE-SEED-0001", "productReference": "PROD-0025", "quantity": 2 }

// Ruta 3 · Update  (paymentMethod solo aplica a APPLIED; por defecto CARGO_EN_CUENTA)
{ "status": "APPLIED", "paymentMethod": "CARGO_EN_CUENTA" }      // o { "status": "CANCELLED" }

// Ruta 5 · Policy/Register
{ "policyId":"POL-101", "name":"5% en transferencias por web", "discountType":"PERCENTAGE",
  "discountValue":5, "scope":"CATEGORY", "scopeReference":"TRANSFERENCIAS", "channel":"WEB" }
```

### 8.3 Estados y colores (chips)

| Entidad | Estado | Color |
|---|---|---|
| Descuento | `INITIATED` | `Warning` |
| Descuento | `APPLIED` | `Success` |
| Descuento | `CANCELLED` | `Error` |
| Pago | `PAID` | `Success` |
| Pago | `PENDING` | `Warning` |
| Pago | `CANCELLED` | `Error` |
| Operación | `OPEN` / `CLOSED` | `BbvaBlueLight` / `BbvaGray700` |
| Campaña | `ACTIVE` / `SCHEDULED` / `FINISHED` / `PAUSED` | `Success` / `BbvaBlueLight` / `BbvaGray700` / `Warning` |

### 8.4 Errores (mapeo a `AppError`)

El cuerpo de error de la Lambda es `{"error": "mensaje"}`. API Gateway devuelve `{"message": "..."}` en 401.

| HTTP | `AppError` | Mensaje al usuario |
|---|---|---|
| 400 | `BadRequest(msg)` | Muestra el `error` de la Lambda (ej. "quantity debe ser mayor que cero", "El producto … no está disponible") |
| 401 | `Unauthorized` | Intenta refresh; si falla → Login con "Tu sesión venció" |
| 403 | `Forbidden(msg)` | "No tienes permiso: solo puedes consultar tus propios datos" |
| 404 | `NotFound(msg)` | "No se encontró …" |
| 409 | `Conflict` | "Este descuento ya fue confirmado o anulado" |
| 5xx | `Server` | "Error interno. Intenta más tarde" (reintentar) |
| IOException | `Network` | "Sin conexión a internet" |

Un único `safeApiCall { }` convierte excepciones y códigos HTTP en `AppResult`.

---

## 9. Componentes reutilizables (`ui/components`)

| Componente | Descripción |
|---|---|
| `BonificaTopBar` | Barra azul `BbvaBlue` con título y botón atrás |
| `PrimaryButton` / `SecondaryButton` | Botones de marca, con estado *loading* |
| `MoneyText(amount, currency)` | `S/ 303.60` / `US$ 40.80` |
| `StatusChip(status)` | Chip de color según §8.3 |
| `InfoCard` / `SectionHeader` | Tarjeta blanca y encabezado de sección |
| `ProductCard`, `OperationCard`, `PolicyCard`, `PaymentCard` | Tarjetas de listas |
| `TimelineItem` | Punto + línea para el historial |
| `LoadingView`, `EmptyView(msg)`, `ErrorView(msg, onRetry)` | Estados estándar de §10 |
| `LabeledField` | Campo de texto con etiqueta, error y teclado adecuado |
| `ConfirmDialog` | Diálogo de confirmar/anular |
| `RoleGuard(role) { … }` | Oculta contenido según el rol |

## 10. Manejo de estado

Cada pantalla expone un único `StateFlow<XxxUiState>`:

```kotlin
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Content<T>(val data: T) : UiState<T>
    data object Empty : UiState<Nothing>
    data class Error(val error: AppError) : UiState<Nothing>
}
```

Toda lista debe resolver los 4 estados (cargando, con datos, vacía, error con **Reintentar**). Eventos de una sola vez (navegar, mostrar *snackbar*) van por `SharedFlow`.

---

## 11. Navegación

### 11.1 Rutas (`Routes.kt`)

| Ruta | Pantalla | Parámetros |
|---|---|---|
| `splash` | Splash (restaura sesión) | — |
| `login` | Login | — |
| `new_password` | Nueva contraseña | `email`, `cognitoSession` |
| `home_cliente` | Inicio cliente (perfil) | — |
| `catalogo` | Categorías | — |
| `catalogo/{category}` | Productos de categoría | `category` |
| `producto/{ref}` | Detalle de producto | `ref` |
| `operaciones` | Mis operaciones | — |
| `operacion/{ref}` | Detalle de operación | `ref` |
| `calcular?pos={pos}&prod={prod}` | Nuevo cálculo | `pos`, `prod` (opcionales) |
| `resultado/{dpId}` | Resultado / confirmar / anular | `dpId` |
| `pagos/{posRef}` | Pagos de una operación | `posRef` |
| `comprobante/{payId}` | Comprobante de pago | `payId` |
| `historial/{dpId}` | Línea de tiempo | `dpId` |
| `promociones` | Políticas y campañas (pestañas) | — |
| `negocio_consulta` | Consultar cliente | — |
| `negocio_cliente/{ref}` | Ficha de cliente consultado | `ref` |
| `negocio_politica` | Registrar política | — |

### 11.2 Barras inferiores por rol

- **Cliente:** `Inicio` · `Catálogo` · `Operaciones` · `Promociones`
- **Negocio:** `Consultar cliente` · `Registrar política` · `Promociones` · `Catálogo`

El rol sale **solo** del claim `cognito:groups`. Tras el login: `cliente` → `home_cliente`; `negocio` → `negocio_consulta`. `RoleGuard` + el 403 de la API son doble protección.

---

## 12. Módulos y pantallas

### Módulo 1 · Acceso y Perfil

| Pantalla | Contenido y comportamiento |
|---|---|
| `SplashScreen` | Logo, intenta `restoreSession()`; va a home según rol o a login |
| `LoginScreen` | Correo, contraseña (con ojo mostrar/ocultar), botón **Ingresar**, mensajes de §5. Sin enlace de registro |
| `NuevaContrasenaScreen` | Nueva contraseña + confirmar, con requisitos visibles (≥8, mayúscula, minúscula, número). Llama a `setNewPassword` |
| `PerfilScreen` (cliente) | Ruta 11 con `customerReference` del token: nombre, segmento, ciudad, estado, cliente desde, correo, teléfono, DNI enmascarado (`****6522`). Aviso si `INACTIVE`. Botón **Cerrar sesión** |

### Módulo 2 · Catálogo de Comisiones

| Pantalla | Contenido |
|---|---|
| `CategoriasScreen` | 5 tarjetas con ícono: Cuentas, Tarjetas, Préstamos, Seguros, Transferencias |
| `ProductosScreen` | Ruta 13: lista con nombre, comisión de lista, frecuencia, estado. Buscador local por nombre |
| `ProductoDetalleScreen` | Ruta 9. Botón **Calcular descuento** (oculto/deshabilitado si `DISCONTINUED`, con texto "Producto no disponible") |

### Módulo 3 · Descuentos (núcleo)

| Pantalla | Contenido |
|---|---|
| `CalcularScreen` | Selector de **operación** (lista de las del cliente, solo `OPEN` recomendadas), producto elegido, **cantidad** (stepper ≥ 1). Botón **Calcular** → ruta 1 |
| `ResultadoScreen` | Ruta 2. Tarjeta: bruto, política aplicada (`policyName`, o "Sin descuento aplicable" si `NONE`), descuento (resaltado), **neto** (grande), estado. Si `INITIATED`: selector de **medio de pago** (3 opciones) + **Confirmar** (`APPLIED`) y **Anular** (`CANCELLED`) con `ConfirmDialog`. Tras confirmar: botón **Ver comprobante** |

Reglas visibles: `quantity > 0`; solo se actúa sobre `INITIATED`; un 409 muestra "ya procesado" y recarga el detalle. Mientras se llama a la API, los botones se bloquean (evitar doble envío).

### Módulo 4 · Promociones

| Pantalla | Contenido |
|---|---|
| `PromocionesScreen` | Dos pestañas: **Políticas** (ruta 6) y **Campañas** (ruta 7) |
| Políticas | Tarjetas con nombre, valor (`10%` o `S/ 20`), alcance, monto mínimo, canal, moneda, vigencia. Filtros: categoría, moneda, canal. Se ocultan las vencidas (`validTo` < hoy) |
| Campañas | Selector de estado (`ACTIVE` por defecto, `SCHEDULED`, `FINISHED`, `PAUSED`); nombre, segmento, canal, fechas |

### Módulo 5 · Operaciones Comerciales

| Pantalla | Contenido |
|---|---|
| `OperacionesScreen` | Ruta 14: lista con canal (ícono), oficina, ciudad, estado, fecha |
| `OperacionDetalleScreen` | Ruta 10 + ruta 8 (descuentos de la operación, cada uno con estado y neto). Acciones: **Calcular descuento**, **Ver pagos** |

### Módulo 6 · Pagos e Historial

| Pantalla | Contenido |
|---|---|
| `PagosScreen` | Ruta 15: lista de pagos de la operación (importe, medio, estado, fecha) |
| `ComprobanteScreen` | Ruta 12: tarjeta tipo recibo — importe, moneda, medio, estado, fecha, `discountPricingId`. Botón **Ver historial** |
| `HistorialScreen` | Ruta 4: línea de tiempo `INITIATED → APPLIED/CANCELLED` con fecha, canal y detalle |

### Módulo 7 · Panel de Negocio (solo `negocio`)

| Pantalla | Contenido |
|---|---|
| `ConsultaClienteScreen` | Campo para código `PARTY-…`. Llama a rutas 11 y 14. 404 → "Cliente no encontrado" |
| `ClienteNegocioScreen` | Ficha del cliente + sus operaciones; desde cada operación se llega a pagos y descuentos (rutas 10, 15, 8) |
| `RegistrarPoliticaScreen` | Formulario de `Policy/Register` (ruta 5): `policyId` (formato `POL-###`), nombre, tipo, valor, alcance, referencia (si `PRODUCT`/`CATEGORY`), monto mínimo, canal, moneda, vigencia. Validación local; 201 → éxito y vuelve; 409 → "La política ya existe" |

### Cobertura de las 15 rutas

| Rutas | Módulo |
|---|---|
| 11, 14 + Cognito | 1 (y 7 con cualquier código) |
| 9, 13 | 2 |
| 1, 2, 3 | 3 |
| 6, 7 | 4 |
| 8, 10, 14 | 5 |
| 4, 12, 15 | 6 |
| 5 | 7 |

---

## 13. Flujo principal del cliente (comprar = contratar con descuento)

El cliente **no crea operaciones**: usa una existente, elige un producto del catálogo, calcula el descuento y lo confirma (se genera la orden de pago por el monto **neto**).

```
Login ─► Perfil ─► Mis operaciones ─► [elige SALE-SEED-0001]
   ─► Catálogo (TARJETAS) ─► [elige PROD-0025] ─► Calcular (cantidad 2)
   ─► Resultado:  bruto S/ 345.00 · POL-022 · descuento S/ 41.40 · neto S/ 303.60  (INITIATED)
        ├─ Confirmar (medio de pago) ─► APPLIED ─► Comprobante PAID ─► Historial INITIATED → APPLIED
        └─ Anular ─► CANCELLED (sin pago)
   (segundo intento sobre el mismo cálculo → 409 "ya procesado")
```

## 14. Datos de prueba para el modo *mock*

`MockData.kt` debe replicar los datos de `dynamobd.md` para que los resultados coincidan con Postman:

| Dato | Valor |
|---|---|
| Cliente | `PARTY-000024` (operaciones: incluye `SALE-SEED-0001`, canal `BRANCH`, Chiclayo) |
| Producto | `PROD-0025` · TARJETAS · S/ 172.50 · `AVAILABLE` |
| Producto descontinuado | `PROD-0008` (debe dar 400 "no está disponible") |
| Cálculo esperado | `quantity = 2` → bruto 345.00 · `POL-022` · descuento 41.40 · neto 303.60 |
| Pago esperado | `PAY-…` por 303.60 · `PAID` |
| Usuarios | `cliente1@example.com` (cliente, `PARTY-000024`) · `negocio1@example.com` (negocio) |
| Cliente ajeno | `PARTY-000001` con token de cliente → 403 |

**Comportamiento del `Fake*Repository`**
- Retrasar 400–800 ms (`delay`) para ver los estados de carga.
- `FakeDiscountRepository.initiate` aplica la misma regla que la Lambda (mejor política: moneda, canal, mínimo, vigencia, alcance) con un par de políticas de ejemplo; `update` cambia el estado en memoria y crea el pago; repetir `update` lanza `Conflict`.
- `FakeAuthRepository`: con `cliente1@example.com` devuelve primero `NewPasswordRequired` (una vez) para probar esa pantalla.
- Permitir forzar errores con un botón oculto en depuración (long-press en el logo) para probar 401/403/404/500.

---

## 15. Reparto de trabajo para el equipo

| Persona | Responsabilidad | Entregable | Depende de |
|---|---|---|---|
| **A (líder)** | Base: tema, `AppConfig`, `AppContainer`, `Result/AppError`, navegación, componentes de §9, `MockData` | Proyecto que compila con navegación vacía | — |
| **B** | Módulo 1: Splash, Login, Nueva contraseña, Perfil, `SessionManager`, `JwtDecoder` | Login mock funcionando | A |
| **C** | Módulos 2 y 5: Catálogo y Operaciones | Listas y detalles | A, B |
| **D** | Módulo 3: Calcular, Resultado, confirmar/anular | Flujo central completo | A, C |
| **E** | Módulos 6 y 4: Pagos, Comprobante, Historial, Promociones | Pantallas de consulta | A |
| **F** | Módulo 7: Panel de Negocio y `RoleGuard` | Consulta y registro de política | A, B |
| **A (al final)** | **Integración con AWS** (§16) y pruebas | App contra la API real | Todos |

**Orden sugerido (sprints)**
1. **Sprint 1:** A entrega la base. B empieza login en paralelo.
2. **Sprint 2:** C, D, E y F trabajan en paralelo sobre interfaces + mock.
3. **Sprint 3:** integración, pruebas cruzadas, pulido visual.

**Convenciones**
- Rama por módulo (`feature/modulo-3-descuentos`), PR a `main` revisado por otra persona.
- Nombres de paquetes y pantallas en español/inglés como en §4.2; texto visible siempre en `strings.xml`.
- Ninguna pantalla importa clases de `data.remote` ni de `data.mock`: solo `domain`.
- Todo texto de error pasa por `AppError.toMessage()` (un solo lugar).

---

## 16. Integración con AWS (a cargo de A, al final)

Con la base bien hecha, son pasos mecánicos:

1. **Obtener los datos** de los manuales:
   - `BASE_URL` = *Invoke URL* de la etapa `dev` (`apigateway.md`, paso 7) **más `/` final**.
   - `COGNITO_CLIENT_ID` = `CLIENT_ID` (`cognito.md`, paso 5.3).
2. En `AppConfig.kt`: poner ambas constantes y `USE_MOCK = false`.
3. Implementar las clases `Remote*Repository` (los DTOs y `BonificaApi` ya definidos según §8) y `CognitoApi` (§5).
4. Verificar en AWS (ya documentado en los manuales, **sin cambios de backend**):
   - Las 15 rutas tienen autorizador `CognitoBian` y la etapa `dev` está desplegada.
   - El app client lee `custom:customerReference` (paso 5.2) y tiene `ALLOW_USER_PASSWORD_AUTH` (5.1).
   - La Lambda tiene sus 8 variables de entorno y el rol con permisos de DynamoDB.
5. **Probar en este orden** (igual que Postman): login cliente → perfil → operaciones → catálogo → calcular → confirmar → comprobante → historial → promociones → login negocio → registrar política.
6. Comparar los resultados con los esperados de §14.

**CORS no hace falta:** las apps nativas no envían *preflight*. No hay que tocar CORS ni *Gateway Responses*.

---

## 17. Pruebas

| Tipo | Qué cubrir |
|---|---|
| Unitarias | `JwtDecoder` (extrae grupo y `customerReference`), `Formatters`, mapeo HTTP→`AppError`, cálculo en `FakeDiscountRepository` |
| ViewModel | Estados `Loading/Content/Empty/Error` de cada pantalla con repos falsos |
| UI (Compose) | Login (error y éxito), resultado con botones Confirmar/Anular, `RoleGuard` |
| Aceptación | Casos de la matriz de `postman.md` ejecutados desde la app (ver tabla) |

| Caso | Resultado esperado en la app |
|---|---|
| Calcular `PROD-0025` ×2 en `SALE-SEED-0001` | Neto S/ 303.60, `POL-022` |
| Confirmar y repetir | Segundo intento: aviso "ya procesado" (409) |
| Producto `PROD-0008` | "Producto no disponible", sin botón de calcular |
| Cantidad 0 | Error de validación (400) |
| Operación inexistente | "No se encontró la operación" (404) |
| Cliente consulta `PARTY-000001` | 403 mensaje de permiso |
| Cliente intenta registrar política | Sin acceso (botón oculto; si llega, 403) |
| Token vencido | Renueva y reintenta; si falla, vuelve a Login |
| Negocio registra `POL-101` dos veces | 201 y luego 409 |

## 18. Criterios de "terminado"

- [ ] Compila en Android Studio y corre en emulador con `USE_MOCK = true` sin internet.
- [ ] Los 2 roles ven sus propias pantallas y barras.
- [ ] Todas las listas manejan carga, vacío y error con **Reintentar**.
- [ ] Flujo completo de §13 funciona de punta a punta.
- [ ] Las 15 rutas tienen repositorio y pantalla que las usa (§12).
- [ ] Textos en `strings.xml`, colores solo desde `Color.kt`.
- [ ] Con `USE_MOCK = false` y las constantes de AWS, se repite la matriz de §17 con resultados iguales.
- [ ] Sin contraseñas ni tokens en el código ni en los *logs*.

## 19. Riesgos y decisiones abiertas

| Tema | Nota |
|---|---|
| Límite de `solo_propio` | Un cliente solo ve sus datos (la Lambda lo impone). Para ver otros códigos se usa el usuario `negocio`. Es el comportamiento esperado, no un fallo |
| Un usuario por cliente | Hoy solo existe `cliente1@example.com` (`PARTY-000024`). Para demostrar más clientes hay que crear usuarios en Cognito con su `customerReference` |
| Fuente tipográfica | La de BBVA es propietaria; usar Roboto/Inter |
| Hex de marca | Confirmar contra la guía de marca; cambiar solo en `Color.kt` |
| Fechas | Los datos usan 2026; las políticas vencen el 31-12-2026. Si caducan, el cálculo da `policyId = NONE` (comportamiento válido) |
