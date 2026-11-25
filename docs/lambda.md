# Manual 2 · AWS Lambda — `DiscountPricingHandler` (solo con la consola de AWS)

**Objetivo:** verificar las 8 tablas de DynamoDB, crear **una sola** función Lambda con enrutador interno y comprobar que escribe en DynamoDB.

- **Región:** `us-east-2` (Ohio). Tablas, Lambda, Cognito y API Gateway deben estar todos en la misma región.
- **No se usa IAM:** la Lambda usa un **rol que ya existe** (por ejemplo `LabRole`). No se crea ningún rol ni política.
- **Condición:** ese rol debe tener permisos de DynamoDB (`GetItem`, `PutItem`, `UpdateItem`, `Query`) sobre las 8 tablas y sus índices.
- **Termina cuando:** la prueba `testInitiate` da `statusCode` 201, `policyId` POL-022 y `netAmount` 303.6.

---

## PARTE A · Verificar DynamoDB (Fase 0)

1. En la barra de búsqueda de la consola escribe **DynamoDB** y entra al servicio.
2. Arriba a la derecha confirma **US East (Ohio) us-east-2**.
3. Menú izquierdo → **Tables** (Tablas). Deben aparecer **8 tablas** con estado **Active** (Activa):

| Tabla | Índice (GSI) |
|---|---|
| `bian-customer` | `segment-index` |
| `bian-point-of-sale` | `customer-index` |
| `bian-product-directory` | `category-index` |
| `bian-discount-pricing-policy` | `status-index` |
| `bian-discount-campaign` | `status-index` |
| `bian-discount-pricing-transaction` | `pointOfSale-index` |
| `bian-discount-pricing-event-log` | — (sin índice) |
| `bian-payment-order` | `pointOfSale-index` |

4. **Revisar cada índice:** entra a la tabla → pestaña **Indexes** (Índices). El índice debe estar en estado **Active** y con **Projected attributes = All** (Todos).
5. **Revisar que los importes sean Número:** entra a `bian-product-directory` → **Explore table items** → abre un elemento. Junto a `listPrice` el tipo debe ser **Number**. Igual para `grossAmount`, `discountAmount`, `netAmount`, `amount`, `discountValue`, `minimumAmount`, `budget`.
6. **Revisar que hay una política vigente:**
   1. Abre `bian-discount-pricing-policy` → **Explore table items**.
   2. Marca **Query**.
   3. **Select a table or index:** `status-index`.
   4. **status (Partition key):** `ACTIVE` → **Run**.
   5. Deben salir políticas con `validFrom` ≤ fecha de hoy ≤ `validTo`.
7. **Anotar datos de prueba** (los necesitarás en Postman):
   1. `bian-customer` → **Explore table items** → busca `PARTY-000024` y anota su `name`.
   2. `bian-point-of-sale` → **Explore table items** → **Query** → índice `customer-index` → `customerReference` = `PARTY-000024` → **Run**. Anota cuántas operaciones salen (`N`).

---

## PARTE B · Crear la función

1. En la barra de búsqueda escribe **Lambda** y entra a **AWS Lambda**.
2. Confirma la región **us-east-2**.
3. Pulsa **Create function** (Crear función).
4. Elige **Author from scratch** (Crear desde cero).
5. **Basic information:**
   - **Function name:** `DiscountPricingHandler`
   - **Runtime:** `Python 3.12`
   - **Architecture:** `x86_64`
6. Expande **Change default execution role** (Cambiar el rol de ejecución predeterminado).
7. Elige **Use an existing role** (Usar un rol existente).
8. En **Existing role** abre la lista y elige un rol, por ejemplo `LabRole`. **No elijas "Create a new role".**
9. Pulsa **Create function**.
10. Espera el aviso verde de que la función fue creada.

> Si la lista de roles está vacía, o la primera prueba da `AccessDeniedException`, el rol no sirve. Sin IAM no se puede arreglar desde esta cuenta: prueba otro rol de la lista o pide a quien administra la cuenta que agregue el acceso a DynamoDB al rol.

## PARTE C · Variables de entorno

1. Pestaña **Configuration** (Configuración) → menú izquierdo **Environment variables** → **Edit**.
2. Pulsa **Add environment variable** 8 veces y completa:

| Clave | Valor |
|---|---|
| `POLICY_TABLE` | `bian-discount-pricing-policy` |
| `TRANSACTION_TABLE` | `bian-discount-pricing-transaction` |
| `EVENT_LOG_TABLE` | `bian-discount-pricing-event-log` |
| `CAMPAIGN_TABLE` | `bian-discount-campaign` |
| `PRODUCT_TABLE` | `bian-product-directory` |
| `POINT_OF_SALE_TABLE` | `bian-point-of-sale` |
| `CUSTOMER_TABLE` | `bian-customer` |
| `PAYMENT_TABLE` | `bian-payment-order` |

3. **Save**.

## PARTE D · Tiempo de espera a 10 segundos

1. **Configuration** → **General configuration** → **Edit**.
2. **Timeout:** `0` min `10` sec.
3. Deja memoria en 128 MB.
4. **Save**.

> Si te saltas esto, verás `Task timed out after 3.00 seconds`.

## PARTE E · Pegar el código (4 bloques en orden)

1. Pestaña **Code** (Código). En el explorador de archivos abre `lambda_function.py`.
2. Click dentro del editor → `Ctrl + A` → `Delete` para borrar el código de ejemplo.
3. Pega el **Bloque 1**. Debajo, pega el **Bloque 2**, luego el **3** y por último el **4**. Todos van en el **mismo archivo**, uno tras otro.
4. Usa siempre 4 espacios por nivel de indentación. No mezcles tabuladores.
5. Pulsa **Deploy** (botón arriba del editor). Espera el mensaje *"Successfully updated the function"*.
6. Verifica que ya no aparezca el aviso de *cambios sin implementar*.

> En los bloques copia **solo lo que está dentro** de las cajas de código, sin las líneas de las comillas invertidas.

### Bloque 1 de 4 — importaciones, tablas y utilidades

```python
import json
import os
import re
import uuid
from datetime import datetime, timezone
from decimal import Decimal, ROUND_HALF_UP

import boto3
from boto3.dynamodb.conditions import Key

dynamodb = boto3.resource("dynamodb")


def tabla(var, default):
    return dynamodb.Table(os.environ.get(var, default))


policy_table = tabla("POLICY_TABLE", "bian-discount-pricing-policy")
tx_table = tabla("TRANSACTION_TABLE", "bian-discount-pricing-transaction")
event_table = tabla("EVENT_LOG_TABLE", "bian-discount-pricing-event-log")
campaign_table = tabla("CAMPAIGN_TABLE", "bian-discount-campaign")
product_table = tabla("PRODUCT_TABLE", "bian-product-directory")
pos_table = tabla("POINT_OF_SALE_TABLE", "bian-point-of-sale")
customer_table = tabla("CUSTOMER_TABLE", "bian-customer")
payment_table = tabla("PAYMENT_TABLE", "bian-payment-order")

BASE = "/v1/DiscountPricing"
TWO = Decimal("0.01")
METODOS_PAGO = {"EFECTIVO_VENTANILLA", "CARGO_EN_CUENTA", "TARJETA_DEBITO"}
ESTADOS_CAMPANA = {"ACTIVE", "SCHEDULED", "FINISHED", "PAUSED"}
ACTOR_CANAL = {"MOBILE_APP": "app-movil", "WEB": "banca-internet",
               "BRANCH": "oficina", "ATM": "cajero", "AGENT": "agente"}


class NotFound(Exception):
    pass


class Forbidden(Exception):
    pass


# Datos de quien llama; los llena lambda_handler a partir del token de Cognito
CALLER = {"groups": set(), "customer": ""}


def solo_negocio():
    if "negocio" not in CALLER["groups"]:
        raise Forbidden("Solo el grupo negocio puede realizar esta acción")


def solo_propio(customer_ref):
    if "negocio" in CALLER["groups"]:
        return
    if CALLER["customer"] != customer_ref:
        raise Forbidden("Solo puedes consultar tus propios datos")


def money(value):
    return Decimal(str(value)).quantize(TWO, rounding=ROUND_HALF_UP)


def now_iso():
    return datetime.now(timezone.utc).isoformat(timespec="seconds")


def get_or_404(table, key_name, key_value, label):
    item = table.get_item(Key={key_name: key_value}).get("Item")
    if not item:
        raise NotFound(f"No existe {label} {key_value}")
    return item


def query_all(table, **kwargs):
    items = []
    while True:
        page = table.query(**kwargs)
        items += page["Items"]
        if "LastEvaluatedKey" not in page:
            return items
        kwargs["ExclusiveStartKey"] = page["LastEvaluatedKey"]
```

### Bloque 2 de 4 — reglas de descuento y acción Initiate

```python
def is_applicable(p, req, gross, today):
    if p.get("currency", "PEN") != req["currency"]:
        return False
    if p.get("channel", "ALL") not in ("ALL", req["channel"]):
        return False
    if gross < money(p.get("minimumAmount", 0)):
        return False
    if not (p.get("validFrom", "0000-01-01") <= today <= p.get("validTo", "9999-12-31")):
        return False
    scope = p.get("scope", "ALL")
    ref = str(p.get("scopeReference", "*")).upper()
    if scope == "PRODUCT":
        return ref == req["productReference"].upper()
    if scope == "CATEGORY":
        return ref == req["category"]
    return scope == "ALL"


def discount_for(p, gross):
    value = money(p["discountValue"])
    if p["discountType"] == "PERCENTAGE":
        amount = (gross * value / Decimal("100")).quantize(TWO, rounding=ROUND_HALF_UP)
    else:
        amount = value
    return min(amount, gross)


def log_event(tx, previous, new):
    """Historial de estados (event-log): se agrega un evento por cada cambio."""
    event_table.put_item(Item={
        "discountPricingId": tx["discountPricingId"],
        "eventTimestamp": datetime.now(timezone.utc).isoformat(timespec="milliseconds"),
        "eventType": new,
        "previousStatus": previous,
        "newStatus": new,
        "actor": ACTOR_CANAL.get(tx["channel"], "sistema"),
        "channel": tx["channel"],
        "policyId": tx["policyId"],
        "details": f"{tx['discountPricingId']} pasó de {previous} a {new} "
                   f"(neto {tx['currency']} {tx['netAmount']})",
    })


def initiate(params, query, body):
    required = ["pointOfSaleReference", "productReference", "quantity"]
    missing = [f for f in required if f not in body]
    if missing:
        raise ValueError("Faltan campos obligatorios: " + ", ".join(missing))
    quantity = int(body["quantity"])
    if quantity <= 0:
        raise ValueError("quantity debe ser mayor que cero")

    # Datos de los dominios relacionados
    pos = get_or_404(pos_table, "pointOfSaleReference", body["pointOfSaleReference"], "la operación")
    product = get_or_404(product_table, "productReference", body["productReference"], "el producto")
    get_or_404(customer_table, "customerReference", pos["customerReference"], "el cliente")
    if product.get("status") != "AVAILABLE":
        raise ValueError(f"El producto {product['productReference']} no está disponible")

    req = {
        "pointOfSaleReference": pos["pointOfSaleReference"],
        "customerReference": pos["customerReference"],
        "productReference": product["productReference"],
        "category": product["category"],
        "channel": pos["channel"],
        "currency": product["currency"],
    }
    list_price = money(product["listPrice"])
    gross = (list_price * quantity).quantize(TWO)
    today = datetime.now(timezone.utc).date().isoformat()

    policies = query_all(
        policy_table,
        IndexName="status-index",
        KeyConditionExpression=Key("status").eq("ACTIVE"),
    )
    best, best_amount = None, Decimal("0.00")
    for p in policies:
        if is_applicable(p, req, gross, today):
            amount = discount_for(p, gross)
            if amount > best_amount:
                best, best_amount = p, amount

    item = {
        "discountPricingId": "DP-" + datetime.now(timezone.utc).strftime("%Y%m%d")
                             + "-" + uuid.uuid4().hex[:6].upper(),
        **req,
        "listPrice": list_price,
        "quantity": quantity,
        "grossAmount": gross,
        "policyId": best["policyId"] if best else "NONE",
        "policyName": best["name"] if best else "Sin descuento aplicable",
        "discountAmount": best_amount,
        "netAmount": gross - best_amount,
        "status": "INITIATED",
        "createdAt": now_iso(),
        "updatedAt": now_iso(),
    }
    tx_table.put_item(Item=item)
    log_event(item, "NONE", "INITIATED")
    return response(201, item)
```

### Bloque 3 de 4 — Retrieve, Update (con pago), historial y por operación

```python
def retrieve(params, query, body):
    item = get_or_404(tx_table, "discountPricingId", params["discountpricingid"], "el cálculo")
    return response(200, item)


def update(params, query, body):
    dp_id = params["discountpricingid"]
    new_status = body.get("status", "")
    if new_status not in ("APPLIED", "CANCELLED"):
        raise ValueError("status debe ser APPLIED o CANCELLED")
    method = body.get("paymentMethod", "CARGO_EN_CUENTA")
    if method not in METODOS_PAGO:
        raise ValueError("paymentMethod debe ser uno de: " + ", ".join(sorted(METODOS_PAGO)))
    try:
        result = tx_table.update_item(
            Key={"discountPricingId": dp_id},
            UpdateExpression="SET #s = :new, updatedAt = :now",
            ConditionExpression="attribute_exists(discountPricingId) AND #s = :initiated",
            ExpressionAttributeNames={"#s": "status"},
            ExpressionAttributeValues={
                ":new": new_status, ":now": now_iso(), ":initiated": "INITIATED"},
            ReturnValues="ALL_NEW",
        )
    except tx_table.meta.client.exceptions.ConditionalCheckFailedException:
        return response(409, {"error": f"{dp_id} no existe o ya no está en estado INITIATED"})

    tx = result["Attributes"]
    log_event(tx, "INITIATED", new_status)
    out = dict(tx)
    if new_status == "APPLIED":  # se cobra el monto neto
        pago = {
            "paymentOrderId": "PAY-" + uuid.uuid4().hex[:8].upper(),
            "discountPricingId": dp_id,
            "customerReference": tx["customerReference"],
            "pointOfSaleReference": tx["pointOfSaleReference"],
            "amount": tx["netAmount"],
            "currency": tx["currency"],
            "paymentMethod": method,
            "status": "PAID",
            "paymentDate": now_iso(),
        }
        payment_table.put_item(Item=pago)
        out["paymentOrderId"] = pago["paymentOrderId"]
    return response(200, out)


def retrieve_events(params, query, body):
    dp_id = params["discountpricingid"]
    get_or_404(tx_table, "discountPricingId", dp_id, "el cálculo")
    items = query_all(
        event_table,
        KeyConditionExpression=Key("discountPricingId").eq(dp_id),
        ScanIndexForward=True,
    )
    return response(200, {"count": len(items), "events": items})


def retrieve_by_pointofsale(params, query, body):
    ref = params["pointofsalereference"]
    items = query_all(
        tx_table,
        IndexName="pointOfSale-index",
        KeyConditionExpression=Key("pointOfSaleReference").eq(ref),
    )
    return response(200, {"count": len(items), "transactions": items})
```

### Bloque 4 de 4 — políticas, campañas, dominios relacionados, enrutador y respuesta HTTP

```python
def register_policy(params, query, body):
    solo_negocio()
    required = ["policyId", "name", "discountType", "discountValue", "scope"]
    missing = [f for f in required if f not in body]
    if missing:
        raise ValueError("Faltan campos obligatorios: " + ", ".join(missing))
    if body["discountType"] not in ("PERCENTAGE", "FIXED_AMOUNT"):
        raise ValueError("discountType debe ser PERCENTAGE o FIXED_AMOUNT")
    if body["scope"] not in ("PRODUCT", "CATEGORY", "ALL"):
        raise ValueError("scope debe ser PRODUCT, CATEGORY o ALL")
    item = {
        "policyId": body["policyId"],
        "name": body["name"],
        "discountType": body["discountType"],
        "discountValue": money(body["discountValue"]),
        "scope": body["scope"],
        "scopeReference": body.get("scopeReference", "*"),
        "minimumAmount": money(body.get("minimumAmount", 0)),
        "currency": body.get("currency", "PEN"),
        "channel": body.get("channel", "ALL"),
        "validFrom": body.get("validFrom", "2026-01-01"),
        "validTo": body.get("validTo", "2026-12-31"),
        "status": body.get("status", "ACTIVE"),
    }
    try:
        policy_table.put_item(Item=item, ConditionExpression="attribute_not_exists(policyId)")
    except policy_table.meta.client.exceptions.ConditionalCheckFailedException:
        return response(409, {"error": f"La política {item['policyId']} ya existe"})
    return response(201, item)


def retrieve_policies(params, query, body):
    items = query_all(
        policy_table,
        IndexName="status-index",
        KeyConditionExpression=Key("status").eq("ACTIVE"),
    )
    return response(200, {"count": len(items), "policies": items})


def retrieve_campaigns(params, query, body):
    status = (query.get("status") or "ACTIVE").upper()
    if status not in ESTADOS_CAMPANA:
        raise ValueError("status debe ser uno de: " + ", ".join(sorted(ESTADOS_CAMPANA)))
    items = query_all(
        campaign_table,
        IndexName="status-index",
        KeyConditionExpression=Key("status").eq(status),
    )
    return response(200, {"count": len(items), "campaigns": items})


CATEGORIAS = {"CUENTAS", "TARJETAS", "PRESTAMOS", "SEGUROS", "TRANSFERENCIAS"}


def list_products(params, query, body):
    category = (query.get("category") or "").upper()
    if category not in CATEGORIAS:
        raise ValueError("category es obligatorio y debe ser uno de: " + ", ".join(sorted(CATEGORIAS)))
    items = query_all(
        product_table,
        IndexName="category-index",
        KeyConditionExpression=Key("category").eq(category),
    )
    return response(200, {"count": len(items), "products": items})


def list_operations_by_customer(params, query, body):
    ref = params["customerreference"]
    solo_propio(ref)
    get_or_404(customer_table, "customerReference", ref, "el cliente")
    items = query_all(
        pos_table,
        IndexName="customer-index",
        KeyConditionExpression=Key("customerReference").eq(ref),
    )
    return response(200, {"count": len(items), "operations": items})


def list_payments_by_pointofsale(params, query, body):
    ref = params["pointofsalereference"]
    get_or_404(pos_table, "pointOfSaleReference", ref, "la operación")
    items = query_all(
        payment_table,
        IndexName="pointOfSale-index",
        KeyConditionExpression=Key("pointOfSaleReference").eq(ref),
    )
    return response(200, {"count": len(items), "payments": items})


def retrieve_customer(params, query, body):
    ref = params["customerreference"]
    solo_propio(ref)
    return response(200, get_or_404(customer_table, "customerReference", ref, "el cliente"))


def retrieve_one(table, key_name, param, label):
    def handler(params, query, body):
        return response(200, get_or_404(table, key_name, params[param], label))
    return handler


def to_json(value):
    if isinstance(value, Decimal):
        return int(value) if value == value.to_integral_value() else float(value)
    raise TypeError(f"Tipo no serializable: {type(value)}")


def response(code, payload):
    return {
        "statusCode": code,
        "headers": {
            "Content-Type": "application/json",
            "Access-Control-Allow-Origin": "*",
        },
        "body": json.dumps(payload, default=to_json, ensure_ascii=False),
    }


# (método HTTP, resource de API Gateway) -> función
ROUTES = {
    # Control Record: Discount Pricing Transaction
    ("POST", BASE + "/Initiate"): initiate,
    ("GET", BASE + "/{discountpricingid}/Retrieve"): retrieve,
    ("PUT", BASE + "/{discountpricingid}/Update"): update,
    ("GET", BASE + "/{discountpricingid}/EventLog/Retrieve"): retrieve_events,
    ("GET", BASE + "/PointOfSale/{pointofsalereference}/Retrieve"): retrieve_by_pointofsale,
    # Behavior Qualifier: Policy y campañas
    ("POST", BASE + "/Policy/Register"): register_policy,
    ("GET", BASE + "/Policy/Retrieve"): retrieve_policies,
    ("GET", BASE + "/Campaign/Retrieve"): retrieve_campaigns,
    # Dominios relacionados (solo lectura)
    ("GET", "/v1/ProductDirectory/{productreference}/Retrieve"):
        retrieve_one(product_table, "productReference", "productreference", "el producto"),
    ("GET", "/v1/PointOfSale/{pointofsalereference}/Retrieve"):
        retrieve_one(pos_table, "pointOfSaleReference", "pointofsalereference", "la operación"),
    ("GET", "/v1/Customer/{customerreference}/Retrieve"): retrieve_customer,
    ("GET", "/v1/PaymentOrder/{paymentorderid}/Retrieve"):
        retrieve_one(payment_table, "paymentOrderId", "paymentorderid", "la orden de pago"),
    # Listados para la app del cliente
    ("GET", "/v1/ProductDirectory/Retrieve"): list_products,
    ("GET", "/v1/Customer/{customerreference}/PointOfSale/Retrieve"): list_operations_by_customer,
    ("GET", "/v1/PointOfSale/{pointofsalereference}/PaymentOrder/Retrieve"): list_payments_by_pointofsale,
}


def lambda_handler(event, context):
    method = event.get("httpMethod", "")
    resource = event.get("resource", "")
    params = event.get("pathParameters") or {}
    query = event.get("queryStringParameters") or {}
    try:
        body = json.loads(event.get("body") or "{}")
    except json.JSONDecodeError:
        return response(400, {"error": "El cuerpo no es un JSON válido"})

    # Claims del token de Cognito (los agrega el autorizador de API Gateway)
    claims = ((event.get("requestContext") or {}).get("authorizer") or {}).get("claims") or {}
    CALLER["groups"] = set(re.split(r"[\s,\[\]]+", str(claims.get("cognito:groups", "")))) - {""}
    CALLER["customer"] = claims.get("custom:customerReference", "")

    handler = ROUTES.get((method, resource))
    if handler is None:
        return response(404, {"error": f"Ruta no soportada: {method} {resource}"})
    try:
        return handler(params, query, body)
    except NotFound as e:
        return response(404, {"error": str(e)})
    except Forbidden as e:
        return response(403, {"error": str(e)})
    except (ValueError, TypeError, ArithmeticError) as e:
        return response(400, {"error": str(e)})
    except Exception as e:
        print("ERROR:", repr(e))
        return response(500, {"error": "Error interno", "detail": str(e)})
```

---

## PARTE F · Probar la Lambda desde la consola

1. Pestaña **Test** (Probar).
2. **Test event action:** `Create new event`.
3. **Event name:** `testInitiate`.
4. **Template - optional:** busca y elige `apigateway-aws-proxy` (API Gateway AWS Proxy).
5. **Event JSON:** borra todo y pega esto:

```json
{
  "httpMethod": "POST",
  "resource": "/v1/DiscountPricing/Initiate",
  "pathParameters": null,
  "body": "{\"pointOfSaleReference\":\"SALE-SEED-0001\",\"productReference\":\"PROD-0025\",\"quantity\":2}"
}
```

6. Pulsa **Save** y luego **Test**.
7. Aparece el panel **Execution result**. Debe decir **Succeeded** (Correcto). Expande **Details**.
8. En la respuesta revisa:

| Campo | Esperado |
|---|---|
| `statusCode` | `201` |
| `customerReference` | `PARTY-000024` |
| `channel` | `BRANCH` |
| `grossAmount` | `345` |
| `policyId` | `POL-022` |
| `discountAmount` | `41.4` |
| `netAmount` | `303.6` |
| `status` | `INITIATED` |

> El `body` se muestra como texto con `\"` escapados. Es normal: es el formato que exige API Gateway.
>
> Los valores salen con las políticas cargadas y la fecha 15-11-2026. Si cambiaron o vencieron políticas, el descuento puede variar. Lo importante es que `netAmount = grossAmount − discountAmount`.
>
> La consola no envía token de Cognito, por eso aquí solo se prueba `Initiate`. Las rutas con reglas de grupo se prueban en Postman.

## PARTE G · Comprobar que escribió en DynamoDB

1. Ve a **DynamoDB** → `bian-discount-pricing-transaction` → **Explore table items**. Debe haber un registro nuevo con `discountPricingId` que empieza con `DP-2026…` y estado `INITIATED`.
2. Ve a `bian-discount-pricing-event-log` → **Explore table items**. Debe haber un evento nuevo con el mismo `discountPricingId`.

## PARTE H · Ver los logs si algo falla (CloudWatch)

1. En Lambda → pestaña **Monitor** → **View CloudWatch logs**.
2. Abre el *log stream* más reciente (arriba de la lista).
3. Busca líneas que empiecen con `ERROR:` o el nombre de la excepción.

---

## Errores comunes

| Error | Causa y solución |
|---|---|
| `AccessDeniedException … dynamodb:Query` / `GetItem` | El rol existente no tiene permisos en DynamoDB. Prueba otro rol de la lista o pide acceso al administrador. |
| `ResourceNotFoundException` | Lambda en otra región distinta a las tablas, o nombre de tabla mal escrito en las variables de entorno. |
| `The table does not have the specified index` | El GSI aún se está creando o tiene otro nombre. Espera a **Active**. |
| `Task timed out after 3.00 seconds` | Falta la Parte D. |
| `IndentationError` / `SyntaxError` | Se mezclaron espacios y tabuladores, o falta pegar algún bloque. Revisa que estén los 4 bloques. |
| `policyId = NONE` | Ninguna política aplica: revisa que haya políticas `ACTIVE`, canal, moneda, monto mínimo y vigencia. |
| `Runtime.ImportModuleError` | El archivo no se llama `lambda_function.py` o no se pulsó **Deploy**. |

## Checklist de salida

- [ ] 8 tablas **Active**, índices **Active** con proyección **All**, importes en **Number**
- [ ] Función `DiscountPricingHandler` en **us-east-2**, Python 3.12, x86_64
- [ ] Rol **existente** asignado (sin crear nada en IAM)
- [ ] 8 variables de entorno y timeout de 10 s
- [ ] 4 bloques pegados y **Deploy** hecho
- [ ] `testInitiate`: 201 · POL-022 · neto 303.6
- [ ] Registro nuevo en `transaction` y en `event-log`

**Capturas para entregar:** variables de entorno, rol asignado, resultado de `testInitiate`.

**Siguiente manual:** `apigateway.md` (necesita que ya existan Lambda y Cognito).
