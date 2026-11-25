# Manual 3 · Amazon API Gateway — `bian-discount-pricing-api` (solo con la consola de AWS)

**Objetivo:** crear la API REST, el autorizador de Cognito, los recursos, los **15 métodos** conectados a la Lambda y desplegar la etapa `dev`.

- **Región:** `us-east-2` (Ohio).
- **Requisitos previos:** haber terminado `lambda.md` (función `DiscountPricingHandler`) y `cognito.md` (User Pool con usuarios).
- **Termina cuando:** tienes la URL de invocación de `dev` y una llamada sin token responde `{"message":"Unauthorized"}`.

| Elemento | Valor |
|---|---|
| API REST (Regional) | `bian-discount-pricing-api` |
| Etapa | `dev` |
| Autorizador | `CognitoBian` (tipo Cognito, origen del token `Authorization`) |
| Integración | Lambda `DiscountPricingHandler` con **Integración de proxy de Lambda** |

> **Regla de oro de los nombres:** los recursos con llaves se escriben con las llaves y en **minúsculas**: `{discountpricingid}`, `{pointofsalereference}`, `{productreference}`, `{customerreference}`, `{paymentorderid}`. La Lambda enruta por ese texto exacto. Si cambia una letra, responde `Ruta no soportada`.

---

## PASO 1 · Crear la API

1. En la barra de búsqueda de la consola escribe **API Gateway** y entra al servicio.
2. Confirma la región **us-east-2**.
3. Pulsa **Create API** (Crear API).
4. Busca la tarjeta **REST API** (la que **no** dice "Private" ni "HTTP") y pulsa **Build** (Crear).
5. **API details:**
   - Elige **New API** (Nueva API).
   - **API name:** `bian-discount-pricing-api`
   - **Description:** `API BIAN Discount Pricing`
   - **API endpoint type:** `Regional`
6. Pulsa **Create API**.
7. Quedas dentro de la pantalla **Resources** (Recursos) con un solo recurso `/`.

## PASO 2 · Crear el autorizador de Cognito

Se crea **antes** de los métodos para poder elegirlo al crearlos.

1. Menú izquierdo de la API → **Authorizers** (Autorizadores).
2. Pulsa **Create authorizer**.
3. Completa:
   - **Authorizer name:** `CognitoBian`
   - **Authorizer type:** `Cognito`
   - **Cognito user pool:** haz clic en el campo y elige tu pool (región us-east-2). Si no lo reconoces por el nombre, es el que tiene tu `POOL_ID`.
   - **Token source:** `Authorization`
   - **Token validation:** déjalo vacío.
4. Pulsa **Create authorizer**.
5. Debe aparecer `CognitoBian` en la lista.

## PASO 3 · Crear los recursos (el árbol)

### Cómo crear un recurso (se repite varias veces)

1. Menú izquierdo → **Resources**.
2. **Haz clic en el recurso padre** en el árbol (el que contendrá al nuevo).
3. Pulsa **Create resource** (Crear recurso).
4. **Proxy resource:** déjalo **apagado**.
5. **Resource path:** debe mostrar la ruta del padre (por ejemplo `/v1/`). No lo cambies.
6. **Resource name:** escribe el nombre (por ejemplo `DiscountPricing`). Para los de llaves escribe `{discountpricingid}` con llaves.
7. **CORS:** déjalo sin marcar (se hace en el Paso 6).
8. Pulsa **Create resource**.

### Orden exacto de creación

Sigue esta tabla de arriba hacia abajo. La columna **Padre** es el recurso que debes seleccionar antes de pulsar *Create resource*.

| # | Padre | Resource name |
|---|---|---|
| 1 | `/` | `v1` |
| 2 | `/v1` | `DiscountPricing` |
| 3 | `/v1` | `ProductDirectory` |
| 4 | `/v1` | `PointOfSale` |
| 5 | `/v1` | `Customer` |
| 6 | `/v1` | `PaymentOrder` |
| 7 | `/v1/DiscountPricing` | `Initiate` |
| 8 | `/v1/DiscountPricing` | `Policy` |
| 9 | `/v1/DiscountPricing/Policy` | `Register` |
| 10 | `/v1/DiscountPricing/Policy` | `Retrieve` |
| 11 | `/v1/DiscountPricing` | `Campaign` |
| 12 | `/v1/DiscountPricing/Campaign` | `Retrieve` |
| 13 | `/v1/DiscountPricing` | `PointOfSale` |
| 14 | `/v1/DiscountPricing/PointOfSale` | `{pointofsalereference}` |
| 15 | `/v1/DiscountPricing/PointOfSale/{pointofsalereference}` | `Retrieve` |
| 16 | `/v1/DiscountPricing` | `{discountpricingid}` |
| 17 | `/v1/DiscountPricing/{discountpricingid}` | `Retrieve` |
| 18 | `/v1/DiscountPricing/{discountpricingid}` | `Update` |
| 19 | `/v1/DiscountPricing/{discountpricingid}` | `EventLog` |
| 20 | `/v1/DiscountPricing/{discountpricingid}/EventLog` | `Retrieve` |
| 21 | `/v1/ProductDirectory` | `Retrieve` |
| 22 | `/v1/ProductDirectory` | `{productreference}` |
| 23 | `/v1/ProductDirectory/{productreference}` | `Retrieve` |
| 24 | `/v1/PointOfSale` | `{pointofsalereference}` |
| 25 | `/v1/PointOfSale/{pointofsalereference}` | `Retrieve` |
| 26 | `/v1/PointOfSale/{pointofsalereference}` | `PaymentOrder` |
| 27 | `/v1/PointOfSale/{pointofsalereference}/PaymentOrder` | `Retrieve` |
| 28 | `/v1/Customer` | `{customerreference}` |
| 29 | `/v1/Customer/{customerreference}` | `Retrieve` |
| 30 | `/v1/Customer/{customerreference}` | `PointOfSale` |
| 31 | `/v1/Customer/{customerreference}/PointOfSale` | `Retrieve` |
| 32 | `/v1/PaymentOrder` | `{paymentorderid}` |
| 33 | `/v1/PaymentOrder/{paymentorderid}` | `Retrieve` |

### Cómo debe quedar el árbol (con el método de cada hoja)

```
/
└── v1
    ├── DiscountPricing
    │   ├── Initiate                                POST
    │   ├── Policy
    │   │   ├── Register                            POST
    │   │   └── Retrieve                            GET
    │   ├── Campaign
    │   │   └── Retrieve                            GET   (?status=ACTIVE)
    │   ├── PointOfSale
    │   │   └── {pointofsalereference}
    │   │       └── Retrieve                        GET
    │   └── {discountpricingid}
    │       ├── Retrieve                            GET
    │       ├── Update                              PUT
    │       └── EventLog
    │           └── Retrieve                        GET
    ├── ProductDirectory
    │   ├── Retrieve                                GET   (?category=TARJETAS)
    │   └── {productreference}
    │       └── Retrieve                            GET
    ├── PointOfSale
    │   └── {pointofsalereference}
    │       ├── Retrieve                            GET
    │       └── PaymentOrder
    │           └── Retrieve                        GET
    ├── Customer
    │   └── {customerreference}
    │       ├── Retrieve                            GET
    │       └── PointOfSale
    │           └── Retrieve                        GET
    └── PaymentOrder
        └── {paymentorderid}
            └── Retrieve                            GET
```

Compara tu árbol con este dibujo antes de seguir. Un nombre mal escrito es el error más común.

## PASO 4 · Crear los 15 métodos

### Cómo crear un método (se repite 15 veces)

1. En el árbol de **Resources**, haz clic en el **recurso hoja** indicado en la tabla.
2. Pulsa **Create method** (Crear método).
3. **Method type:** el de la tabla (`GET`, `POST` o `PUT`).
4. **Integration type:** `Lambda function`.
5. **Lambda proxy integration:** **actívalo** (interruptor encendido). Es imprescindible.
6. **Lambda function:** elige la región `us-east-2` y escribe o selecciona `DiscountPricingHandler`.
7. **Default timeout:** déjalo activado.
8. Expande **Method request settings** (Configuración de la solicitud de método).
9. **Authorization:** abre la lista y elige **`CognitoBian`**.
10. Deja el resto como está y pulsa **Create method**.
11. Si aparece un aviso o ventana pidiendo permiso para que API Gateway invoque la Lambda, **acéptalo**.

### Los 15 métodos

| # | Recurso hoja | Método |
|---|---|---|
| 1 | `/v1/DiscountPricing/Initiate` | `POST` |
| 2 | `/v1/DiscountPricing/{discountpricingid}/Retrieve` | `GET` |
| 3 | `/v1/DiscountPricing/{discountpricingid}/Update` | `PUT` |
| 4 | `/v1/DiscountPricing/{discountpricingid}/EventLog/Retrieve` | `GET` |
| 5 | `/v1/DiscountPricing/Policy/Register` | `POST` |
| 6 | `/v1/DiscountPricing/Policy/Retrieve` | `GET` |
| 7 | `/v1/DiscountPricing/Campaign/Retrieve` | `GET` |
| 8 | `/v1/DiscountPricing/PointOfSale/{pointofsalereference}/Retrieve` | `GET` |
| 9 | `/v1/ProductDirectory/{productreference}/Retrieve` | `GET` |
| 10 | `/v1/PointOfSale/{pointofsalereference}/Retrieve` | `GET` |
| 11 | `/v1/Customer/{customerreference}/Retrieve` | `GET` |
| 12 | `/v1/PaymentOrder/{paymentorderid}/Retrieve` | `GET` |
| 13 | `/v1/ProductDirectory/Retrieve` | `GET` |
| 14 | `/v1/Customer/{customerreference}/PointOfSale/Retrieve` | `GET` |
| 15 | `/v1/PointOfSale/{pointofsalereference}/PaymentOrder/Retrieve` | `GET` |

Marca cada fila al terminarla.

### Revisar que ninguno quedó sin autorizador

1. Haz clic en un método del árbol → pestaña **Method request** → mira **Authorization**: debe decir `CognitoBian`.
2. Si alguno dice `NONE`: **Edit** → **Authorization** → `CognitoBian` → **Save**.
3. Repite la revisión con los 15.

## PASO 5 · Revisar la integración (una muestra)

1. Haz clic en cualquier método → pestaña **Integration request**.
2. Debe decir **Integration type: Lambda function** y **Lambda proxy integration: Enabled**, con la función `DiscountPricingHandler`.

## PASO 6 · CORS (solo si usarás un navegador o app web)

Para probar con **Postman no se necesita CORS**. Puedes saltarlo y volver cuando construyas la app.

1. Haz clic en un recurso (por ejemplo `/v1/DiscountPricing`).
2. Pulsa **Enable CORS** (Habilitar CORS).
3. **Access-Control-Allow-Methods:** marca `GET`, `POST`, `PUT` y `OPTIONS`.
4. **Access-Control-Allow-Headers:** debe incluir `Authorization` y `Content-Type`.
5. **Access-Control-Allow-Origin:** `*`.
6. **Save**.
7. Repite en `/v1/ProductDirectory`, `/v1/PointOfSale`, `/v1/Customer` y `/v1/PaymentOrder`.

> El método `OPTIONS` que crea la consola queda **sin autorizador**, y así debe ser. El navegador hace esa pregunta previa sin enviar token.
>
> Cada `OPTIONS` solo cubre el recurso donde se creó. Si la app llama a una ruta más profunda (por ejemplo `/v1/Customer/{customerreference}/Retrieve`), habilita CORS también en ese recurso.

## PASO 7 · Implementar (desplegar) la etapa `dev`

1. Arriba a la derecha pulsa **Deploy API** (Implementar API).
2. **Stage:** elige **\*New stage\*** (Nueva etapa).
3. **Stage name:** `dev`.
4. Pulsa **Deploy**.
5. Quedas en **Stage details**. Copia la **Invoke URL** (URL de invocación). Se ve así:

```
https://{api-id}.execute-api.us-east-2.amazonaws.com/dev
```

6. Guárdala como `baseUrl`, **sin `/` al final**. Se usa en Postman.

> **Cada vez que crees o cambies un recurso o método, hay que volver a pulsar Deploy API → etapa `dev`.** Si no, los cambios no se ven.

## PASO 8 · Comprobar que la API responde

1. Abre una pestaña del navegador y pega:

```
https://{api-id}.execute-api.us-east-2.amazonaws.com/dev/v1/DiscountPricing/Policy/Retrieve
```

2. Debe responder `{"message":"Unauthorized"}`. **Eso es correcto**: la ruta existe y exige token.
3. Si responde `{"message":"Missing Authentication Token"}`, la ruta o el método no existen en `dev`. Revisa nombres y repite el Deploy.

## PASO 9 · Probar el autorizador (después de tener un token)

Hazlo cuando ya tengas el ID token de la petición 00A de Postman.

1. Menú izquierdo → **Authorizers** → entra a `CognitoBian`.
2. Pulsa **Test authorizer** (Probar autorizador).
3. En **Token value** pega el ID token (solo el token, sin la palabra `Bearer`).
4. Debe responder **200** y mostrar los *claims* (`email`, `cognito:groups`, `custom:customerReference`).

---

## Errores comunes

| Error | Solución |
|---|---|
| `{"message":"Missing Authentication Token"}` | La ruta o el método no existen en `dev`. Revisa mayúsculas, método HTTP y vuelve a **Deploy API**. |
| `Internal server error` (502) | La Lambda no devolvió formato proxy o falló. Verifica que **Lambda proxy integration** esté activo y mira CloudWatch (`/aws/lambda/DiscountPricingHandler`). |
| `Ruta no soportada` (404 desde la Lambda) | Integración sin proxy, o recurso escrito distinto (por ejemplo `{discountPricingId}` en vez de `{discountpricingid}`). |
| `{"message":"Unauthorized"}` (401) con token | Se envió el *access token* en vez del **ID token**, el token venció (1 h) o el autorizador apunta a otro pool. |
| El pool no aparece al crear el autorizador | El pool está en otra región. Debe ser **us-east-2**. |
| Cambié algo y no se refleja | Falta **Deploy API** sobre `dev`. |
| 500 `AccessDeniedException` | El rol de la Lambda no tiene permisos en DynamoDB (ver `lambda.md`). |

## Checklist de salida

- [ ] API `bian-discount-pricing-api`, Regional, en **us-east-2**
- [ ] Autorizador `CognitoBian` apuntando al User Pool correcto
- [ ] 33 recursos creados, con nombres de llaves en minúscula
- [ ] 15 métodos con **proxy de Lambda** y autorizador `CognitoBian`
- [ ] Etapa `dev` desplegada y URL copiada
- [ ] Llamada sin token → `Unauthorized`

**Capturas para entregar:** árbol de recursos, el autorizador y la etapa `dev` con su URL.

**Siguiente manual:** `postman.md`.
