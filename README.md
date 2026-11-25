# BBVA Bonifica

App Android (Kotlin · Jetpack Compose · Material 3) de **descuentos en
comisiones** para clientes BBVA, con vista adicional para personal de negocio.
Package: `com.bbva.bonifica` · minSdk 26.

- Diseño completo: [`docs/diseno-app.md`](docs/diseno-app.md)

## Cómo empezar

1. Clona el repo y ábrelo con Android Studio (carpeta raíz del repo). Espera el
   Gradle Sync.
2. Ejecuta la app en un emulador (ej. Pixel 7). Verás un **menú de desarrollo**
   para saltar a cada pantalla.
3. `AppConfig.USE_MOCK = true`: la app funciona **sin internet ni AWS**, con
   datos simulados.

## Reglas de trabajo

- Una rama por módulo: `feature/modulo-N-nombre`. PR a `main`, revisado por otra
  persona.
- Las pantallas y ViewModels **solo usan interfaces de `domain/repository`**.
  Nunca importan `data.mock` ni `data.remote`.
- Textos visibles en `strings.xml`; colores solo desde `ui/theme/Color.kt`.
- Errores al usuario siempre con `AppError.toMessage()`.
- No subir `.idea/`, `local.properties`, contraseñas ni tokens.

## Reparto (ver §15 del diseño)

| Persona   | Módulo                                | Rama sugerida                             |
| --------- | ------------------------------------- | ----------------------------------------- |
| A (líder) | Base, componentes, integración AWS    | `feature/base`                            |
| B         | 1 · Acceso y Perfil                   | `feature/modulo-1-acceso`                 |
| C         | 2 · Catálogo y 5 · Operaciones        | `feature/modulo-2-5-catalogo-operaciones` |
| D         | 3 · Descuentos (flujo central)        | `feature/modulo-3-descuentos`             |
| E         | 4 · Promociones y 6 · Pagos/Historial | `feature/modulo-4-6-promos-pagos`         |
| F         | 7 · Panel de Negocio y `RoleGuard`    | `feature/modulo-7-negocio`                |

Cada persona completa su carpeta en `ui/<modulo>/` (Screen + ViewModel +
UiState) y los `Fake*Repository` marcados con `TODO` en `data/mock/`.
