# DynamoDB — BIAN v12 · Discount Pricing (Banco BBVA)

Resumen de las **8 tablas**, sus **claves e índices (GSI)** y **datos de ejemplo**. Cada tabla tiene **100 registros** de prueba (sintéticos, orientados a banca: comisiones y cargos); aquí se muestran solo **10** por tabla como muestra.

- Región: `us-east-2`
- Modo de datos: los importes son **Número** (no texto); fechas en formato ISO 8601 (texto).
- Claves: cliente `PARTY-000001`, producto `PROD-0001`, política `POL-001`, campaña `CMP-0001`, operación `SALE-SEED-0001`, cálculo `DP-SEED-0001`, pago `PAY-0001`.

---

## 1. Vista general

| # | Tabla | Rol en BIAN | Partition Key | Sort Key | GSI | Registros |
|---|---|---|---|---|---|---|
| 1 | `bian-customer` | Cliente del banco (dominio relacionado Party/Customer). | `customerReference` | — | `segment-index` | 100 |
| 2 | `bian-point-of-sale` | Operación comercial (contratación/venta) hecha por un cliente en un canal u oficina. | `pointOfSaleReference` | — | `customer-index` | 100 |
| 3 | `bian-product-directory` | Catálogo de productos/cargos del banco; `listPrice` es la comisión de lista. | `productReference` | — | `category-index` | 100 |
| 4 | `bian-discount-pricing-policy` | **Behavior Qualifier: Policy.** Regla de descuento (porcentaje o monto fijo) sobre comisiones. | `policyId` | — | `status-index` | 100 |
| 5 | `bian-discount-campaign` | Campaña comercial que agrupa políticas con presupuesto y segmento objetivo. | `campaignId` | `policyId` | `status-index` | 100 |
| 6 | `bian-discount-pricing-transaction` | **Control Record: Discount Pricing Transaction.** Cada cálculo de descuento sobre la comisión de un producto. | `discountPricingId` | — | `pointOfSale-index` | 100 |
| 7 | `bian-discount-pricing-event-log` | Bitácora de auditoría: historial de estados de cada transacción (solo se agrega). | `discountPricingId` | `eventTimestamp` | — | 100 |
| 8 | `bian-payment-order` | Orden de pago del cargo ya descontado (se cobra el monto neto). | `paymentOrderId` | — | `pointOfSale-index` | 100 |

## 2. Cómo se conectan

DynamoDB no tiene claves foráneas: las relaciones son **atributos de referencia** que guardan la clave de otra tabla.

```
customer ◄── point-of-sale ◄── transaction ──► policy ◄── campaign
                 ▲                 │  │
                 │                 │  └──► product-directory
          payment-order ───────────┤
                                   └──► event-log (historial de estados)
```

| Referencia | Apunta a | Significado |
|---|---|---|
| `transaction.policyId` | `policy` | Política que dio el mejor descuento (`NONE` si ninguna aplica) |
| `transaction.productReference` | `product-directory` | Producto/cargo sobre el que se calcula |
| `transaction.pointOfSaleReference` | `point-of-sale` | Operación comercial de origen |
| `transaction.customerReference` | `customer` | Cliente |
| `point-of-sale.customerReference` | `customer` | Cliente que hizo la operación |
| `payment-order.pointOfSaleReference` | `point-of-sale` | Operación que se paga (se cobra el monto neto) |
| `payment-order.discountPricingId` | `transaction` | Cálculo que originó el pago |
| `event-log.discountPricingId` | `transaction` | Historial de estados |
| `campaign.policyId` | `policy` | Política que agrupa la campaña |

## 3. Cómo se calcula el descuento

1. Se toma el producto (`listPrice`) y la cantidad: `grossAmount = listPrice × quantity`.
2. Se buscan las políticas `ACTIVE` (vía `status-index`) que **apliquen**: misma moneda, canal igual o `ALL`, `grossAmount ≥ minimumAmount`, fecha dentro de `validFrom`–`validTo` y alcance (`PRODUCT`, `CATEGORY` o `ALL`) coincidente.
3. Se elige la política con **mayor descuento** (`PERCENTAGE` o `FIXED_AMOUNT`, nunca mayor al bruto).
4. `netAmount = grossAmount − discountAmount`. Ese neto es el `amount` del `payment-order`.

Estados de la transacción: `INITIATED` → `APPLIED` o `CANCELLED`. El pago queda `PENDING`, `PAID` o `CANCELLED` según ese estado.

---

## 4. Detalle por tabla

### 4.1 `bian-customer`

Cliente del banco (dominio relacionado Party/Customer).

- **Partition Key:** `customerReference`
- **Sort Key:** ninguna
- **GSI `segment-index`:** Partition Key `segment` — Listar clientes por segmento (PERSONAL, PREMIUM, PYME…)
- **Atributos:** name, documentType, documentNumber, email, phone, segment, city, status, customerSince
- **Ejemplo de clave:** `PARTY-000001`
- **Relaciones:** Lo referencian `point-of-sale`, `transaction` y `payment-order`.

Datos de ejemplo (10 de 100):

```json
{"customerReference": "PARTY-000001", "name": "Ana Ramírez", "documentType": "DNI", "documentNumber": "77446522", "email": "ana.ramirez1@example.com", "phone": "978695681", "segment": "PERSONAL", "city": "Arequipa", "status": "INACTIVE", "customerSince": "2023-10-14"}
{"customerReference": "PARTY-000002", "name": "Ricardo Castillo", "documentType": "DNI", "documentNumber": "75888073", "email": "ricardo.castillo2@example.com", "phone": "988721495", "segment": "JOVEN", "city": "Arequipa", "status": "ACTIVE", "customerSince": "2021-11-20"}
{"customerReference": "PARTY-000003", "name": "Luis Flores", "documentType": "DNI", "documentNumber": "48552728", "email": "luis.flores3@example.com", "phone": "923160265", "segment": "JOVEN", "city": "Lima", "status": "INACTIVE", "customerSince": "2022-08-14"}
{"customerReference": "PARTY-000004", "name": "Diego Chávez", "documentType": "DNI", "documentNumber": "63313617", "email": "diego.chavez4@example.com", "phone": "943757517", "segment": "PREMIUM", "city": "Cusco", "status": "INACTIVE", "customerSince": "2023-05-14"}
{"customerReference": "PARTY-000005", "name": "Fernando Salazar", "documentType": "DNI", "documentNumber": "20243483", "email": "fernando.salazar5@example.com", "phone": "955686986", "segment": "PERSONAL", "city": "Piura", "status": "ACTIVE", "customerSince": "2018-03-24"}
{"customerReference": "PARTY-000006", "name": "Rodrigo Rojas", "documentType": "DNI", "documentNumber": "51473104", "email": "rodrigo.rojas6@example.com", "phone": "913320452", "segment": "PREMIUM", "city": "Cusco", "status": "INACTIVE", "customerSince": "2019-09-26"}
{"customerReference": "PARTY-000007", "name": "Luis Mendoza", "documentType": "DNI", "documentNumber": "76901508", "email": "luis.mendoza7@example.com", "phone": "925629378", "segment": "JOVEN", "city": "Piura", "status": "INACTIVE", "customerSince": "2019-06-19"}
{"customerReference": "PARTY-000008", "name": "Fernando Torres", "documentType": "DNI", "documentNumber": "66366902", "email": "fernando.torres8@example.com", "phone": "985698815", "segment": "JOVEN", "city": "Piura", "status": "ACTIVE", "customerSince": "2021-06-15"}
{"customerReference": "PARTY-000009", "name": "Rosa Huamán", "documentType": "DNI", "documentNumber": "27313932", "email": "rosa.huaman9@example.com", "phone": "916561596", "segment": "PYME", "city": "Chiclayo", "status": "ACTIVE", "customerSince": "2023-01-17"}
{"customerReference": "PARTY-000010", "name": "Daniela Mendoza", "documentType": "DNI", "documentNumber": "73087744", "email": "daniela.mendoza10@example.com", "phone": "929114289", "segment": "JOVEN", "city": "Arequipa", "status": "ACTIVE", "customerSince": "2022-01-10"}
```

### 4.2 `bian-point-of-sale`

Operación comercial (contratación/venta) hecha por un cliente en un canal u oficina.

- **Partition Key:** `pointOfSaleReference`
- **Sort Key:** ninguna
- **GSI `customer-index`:** Partition Key `customerReference` — Ver todas las operaciones de un cliente
- **Atributos:** customerReference, channel, officeCode, officeName, city, status, createdAt
- **Ejemplo de clave:** `SALE-SEED-0001`
- **Relaciones:** Apunta a `customer`. Cada operación agrupa 2 transacciones de descuento.

Datos de ejemplo (10 de 100):

```json
{"pointOfSaleReference": "SALE-SEED-0001", "customerReference": "PARTY-000024", "channel": "BRANCH", "officeCode": "OF-290", "officeName": "Oficina Chiclayo Centro", "city": "Chiclayo", "status": "CLOSED", "createdAt": "2026-10-01T16:00:00+00:00"}
{"pointOfSaleReference": "SALE-SEED-0002", "customerReference": "PARTY-000020", "channel": "WEB", "officeCode": "DIG-WEB", "officeName": "Banca por internet", "city": "Piura", "status": "CLOSED", "createdAt": "2026-10-02T06:00:00+00:00"}
{"pointOfSaleReference": "SALE-SEED-0003", "customerReference": "PARTY-000063", "channel": "AGENT", "officeCode": "AGT-4926", "officeName": "Agente Miraflores", "city": "Lima", "status": "OPEN", "createdAt": "2026-10-02T20:00:00+00:00"}
{"pointOfSaleReference": "SALE-SEED-0004", "customerReference": "PARTY-000066", "channel": "MOBILE_APP", "officeCode": "DIG-APP", "officeName": "App móvil", "city": "Chiclayo", "status": "CLOSED", "createdAt": "2026-10-03T10:00:00+00:00"}
{"pointOfSaleReference": "SALE-SEED-0005", "customerReference": "PARTY-000012", "channel": "WEB", "officeCode": "DIG-WEB", "officeName": "Banca por internet", "city": "Trujillo", "status": "CLOSED", "createdAt": "2026-10-04T00:00:00+00:00"}
{"pointOfSaleReference": "SALE-SEED-0006", "customerReference": "PARTY-000053", "channel": "MOBILE_APP", "officeCode": "DIG-APP", "officeName": "App móvil", "city": "Huancayo", "status": "CLOSED", "createdAt": "2026-10-04T14:00:00+00:00"}
{"pointOfSaleReference": "SALE-SEED-0007", "customerReference": "PARTY-000019", "channel": "BRANCH", "officeCode": "OF-582", "officeName": "Oficina Miraflores", "city": "Lima", "status": "OPEN", "createdAt": "2026-10-05T04:00:00+00:00"}
{"pointOfSaleReference": "SALE-SEED-0008", "customerReference": "PARTY-000008", "channel": "MOBILE_APP", "officeCode": "DIG-APP", "officeName": "App móvil", "city": "Piura", "status": "CLOSED", "createdAt": "2026-10-05T18:00:00+00:00"}
{"pointOfSaleReference": "SALE-SEED-0009", "customerReference": "PARTY-000091", "channel": "BRANCH", "officeCode": "OF-363", "officeName": "Oficina Huancayo Centro", "city": "Huancayo", "status": "CLOSED", "createdAt": "2026-10-06T08:00:00+00:00"}
{"pointOfSaleReference": "SALE-SEED-0010", "customerReference": "PARTY-000097", "channel": "MOBILE_APP", "officeCode": "DIG-APP", "officeName": "App móvil", "city": "Piura", "status": "CLOSED", "createdAt": "2026-10-06T22:00:00+00:00"}
```

### 4.3 `bian-product-directory`

Catálogo de productos/cargos del banco; `listPrice` es la comisión de lista.

- **Partition Key:** `productReference`
- **Sort Key:** ninguna
- **GSI `category-index`:** Partition Key `category` — Listar productos por categoría (CUENTAS, TARJETAS, PRESTAMOS, SEGUROS, TRANSFERENCIAS)
- **Atributos:** name, category, listPrice, currency, billingFrequency, status
- **Ejemplo de clave:** `PROD-0001`
- **Relaciones:** Lo referencia `transaction.productReference` y `policy.scopeReference` (cuando scope = PRODUCT).

Datos de ejemplo (10 de 100):

```json
{"productReference": "PROD-0001", "name": "Mantenimiento mensual cuenta de ahorros - Plan Estándar", "category": "CUENTAS", "listPrice": 12.0, "currency": "PEN", "billingFrequency": "MONTHLY", "status": "AVAILABLE"}
{"productReference": "PROD-0002", "name": "Mantenimiento mensual cuenta corriente - Plan Estándar", "category": "CUENTAS", "listPrice": 25.0, "currency": "PEN", "billingFrequency": "MONTHLY", "status": "AVAILABLE"}
{"productReference": "PROD-0003", "name": "Emisión de estado de cuenta físico - Plan Estándar", "category": "CUENTAS", "listPrice": 8.0, "currency": "PEN", "billingFrequency": "PER_OPERATION", "status": "AVAILABLE"}
{"productReference": "PROD-0004", "name": "Emisión de chequera - Plan Estándar", "category": "CUENTAS", "listPrice": 30.0, "currency": "PEN", "billingFrequency": "PER_OPERATION", "status": "AVAILABLE"}
{"productReference": "PROD-0005", "name": "Membresía anual tarjeta de crédito clásica - Plan Estándar", "category": "TARJETAS", "listPrice": 150.0, "currency": "PEN", "billingFrequency": "ANNUAL", "status": "AVAILABLE"}
{"productReference": "PROD-0006", "name": "Membresía anual tarjeta de crédito oro - Plan Estándar", "category": "TARJETAS", "listPrice": 250.0, "currency": "PEN", "billingFrequency": "ANNUAL", "status": "AVAILABLE"}
{"productReference": "PROD-0007", "name": "Membresía anual tarjeta de crédito platinum - Plan Estándar", "category": "TARJETAS", "listPrice": 360.0, "currency": "PEN", "billingFrequency": "ANNUAL", "status": "AVAILABLE"}
{"productReference": "PROD-0008", "name": "Reposición de tarjeta de débito - Plan Estándar", "category": "TARJETAS", "listPrice": 25.0, "currency": "PEN", "billingFrequency": "PER_OPERATION", "status": "DISCONTINUED"}
{"productReference": "PROD-0009", "name": "Gastos de evaluación préstamo personal - Plan Estándar", "category": "PRESTAMOS", "listPrice": 120.0, "currency": "PEN", "billingFrequency": "ONE_TIME", "status": "AVAILABLE"}
{"productReference": "PROD-0010", "name": "Comisión por prepago parcial de préstamo - Plan Estándar", "category": "PRESTAMOS", "listPrice": 16.0, "currency": "USD", "billingFrequency": "PER_OPERATION", "status": "AVAILABLE"}
```

### 4.4 `bian-discount-pricing-policy`

**Behavior Qualifier: Policy.** Regla de descuento (porcentaje o monto fijo) sobre comisiones.

- **Partition Key:** `policyId`
- **Sort Key:** ninguna
- **GSI `status-index`:** Partition Key `status` — Obtener las políticas ACTIVE para calcular el mejor descuento
- **Atributos:** name, discountType, discountValue, scope, scopeReference, minimumAmount, currency, channel, validFrom, validTo, status
- **Ejemplo de clave:** `POL-001`
- **Relaciones:** La usan `transaction`, `event-log` y `campaign`.

Datos de ejemplo (10 de 100):

```json
{"policyId": "POL-001", "name": "Descuento 10% en membresías y cargos de tarjetas", "discountType": "PERCENTAGE", "discountValue": 10, "scope": "CATEGORY", "scopeReference": "TARJETAS", "minimumAmount": 0, "currency": "PEN", "channel": "ALL", "validFrom": "2026-01-01", "validTo": "2026-12-31", "status": "ACTIVE"}
{"policyId": "POL-002", "name": "S/ 20 de descuento en cargos desde S/ 200 por app móvil", "discountType": "FIXED_AMOUNT", "discountValue": 20, "scope": "ALL", "scopeReference": "*", "minimumAmount": 200, "currency": "PEN", "channel": "MOBILE_APP", "validFrom": "2026-01-01", "validTo": "2026-12-31", "status": "ACTIVE"}
{"policyId": "POL-003", "name": "15% en mantenimiento de cuenta de ahorros PROD-0001 (campaña)", "discountType": "PERCENTAGE", "discountValue": 15, "scope": "PRODUCT", "scopeReference": "PROD-0001", "minimumAmount": 0, "currency": "PEN", "channel": "ALL", "validFrom": "2026-01-01", "validTo": "2026-12-31", "status": "ACTIVE"}
{"policyId": "POL-004", "name": "5% de descuento base en comisiones de cuentas", "discountType": "PERCENTAGE", "discountValue": 5, "scope": "CATEGORY", "scopeReference": "CUENTAS", "minimumAmount": 0, "currency": "PEN", "channel": "ALL", "validFrom": "2026-01-01", "validTo": "2026-12-31", "status": "ACTIVE"}
{"policyId": "POL-005", "name": "5% de descuento base en membresías y cargos de tarjetas", "discountType": "PERCENTAGE", "discountValue": 5, "scope": "CATEGORY", "scopeReference": "TARJETAS", "minimumAmount": 0, "currency": "PEN", "channel": "ALL", "validFrom": "2026-01-01", "validTo": "2026-12-31", "status": "ACTIVE"}
{"policyId": "POL-006", "name": "5% de descuento base en gastos de préstamos", "discountType": "PERCENTAGE", "discountValue": 5, "scope": "CATEGORY", "scopeReference": "PRESTAMOS", "minimumAmount": 0, "currency": "PEN", "channel": "ALL", "validFrom": "2026-01-01", "validTo": "2026-12-31", "status": "ACTIVE"}
{"policyId": "POL-007", "name": "8% de descuento base en primas de seguros", "discountType": "PERCENTAGE", "discountValue": 8, "scope": "CATEGORY", "scopeReference": "SEGUROS", "minimumAmount": 0, "currency": "PEN", "channel": "ALL", "validFrom": "2026-01-01", "validTo": "2026-12-31", "status": "ACTIVE"}
{"policyId": "POL-008", "name": "8% de descuento base en transferencias y operaciones", "discountType": "PERCENTAGE", "discountValue": 8, "scope": "CATEGORY", "scopeReference": "TRANSFERENCIAS", "minimumAmount": 0, "currency": "PEN", "channel": "ALL", "validFrom": "2026-01-01", "validTo": "2026-12-31", "status": "ACTIVE"}
{"policyId": "POL-009", "name": "5% de descuento base en comisiones de cuentas (USD)", "discountType": "PERCENTAGE", "discountValue": 5, "scope": "CATEGORY", "scopeReference": "CUENTAS", "minimumAmount": 0, "currency": "USD", "channel": "ALL", "validFrom": "2026-01-01", "validTo": "2026-12-31", "status": "ACTIVE"}
{"policyId": "POL-010", "name": "8% de descuento base en membresías y cargos de tarjetas (USD)", "discountType": "PERCENTAGE", "discountValue": 8, "scope": "CATEGORY", "scopeReference": "TARJETAS", "minimumAmount": 0, "currency": "USD", "channel": "ALL", "validFrom": "2026-01-01", "validTo": "2026-12-31", "status": "ACTIVE"}
```

### 4.5 `bian-discount-campaign`

Campaña comercial que agrupa políticas con presupuesto y segmento objetivo.

- **Partition Key:** `campaignId`
- **Sort Key:** `policyId`
- **GSI `status-index`:** Partition Key `status` — Listar campañas por estado (ACTIVE, SCHEDULED, FINISHED, PAUSED)
- **Atributos:** name, targetSegment, channel, budget, currency, startDate, endDate, status
- **Ejemplo de clave:** `CMP-0001 + POL-001`
- **Relaciones:** `policyId` (Sort Key) apunta a `policy`.

Datos de ejemplo (10 de 100):

```json
{"campaignId": "CMP-0001", "policyId": "POL-001", "name": "Campaña: Descuento 10% en membresías y cargos de tarjetas", "targetSegment": "PREMIUM", "channel": "ALL", "budget": 362318.07, "currency": "PEN", "startDate": "2026-01-01", "endDate": "2026-12-31", "status": "SCHEDULED"}
{"campaignId": "CMP-0002", "policyId": "POL-002", "name": "Campaña: S/ 20 de descuento en cargos desde S/ 200 por app móvil", "targetSegment": "PREMIUM", "channel": "MOBILE_APP", "budget": 456409.91, "currency": "PEN", "startDate": "2026-01-01", "endDate": "2026-12-31", "status": "SCHEDULED"}
{"campaignId": "CMP-0003", "policyId": "POL-003", "name": "Campaña: 15% en mantenimiento de cuenta de ahorros PROD-0001 (campaña)", "targetSegment": "JOVEN", "channel": "ALL", "budget": 269413.16, "currency": "PEN", "startDate": "2026-01-01", "endDate": "2026-12-31", "status": "SCHEDULED"}
{"campaignId": "CMP-0004", "policyId": "POL-004", "name": "Campaña: 5% de descuento base en comisiones de cuentas", "targetSegment": "PREMIUM", "channel": "ALL", "budget": 346083.42, "currency": "PEN", "startDate": "2026-01-01", "endDate": "2026-12-31", "status": "SCHEDULED"}
{"campaignId": "CMP-0005", "policyId": "POL-005", "name": "Campaña: 5% de descuento base en membresías y cargos de tarjetas", "targetSegment": "PREFERENTE", "channel": "ALL", "budget": 383031.17, "currency": "PEN", "startDate": "2026-01-01", "endDate": "2026-12-31", "status": "FINISHED"}
{"campaignId": "CMP-0006", "policyId": "POL-006", "name": "Campaña: 5% de descuento base en gastos de préstamos", "targetSegment": "PREMIUM", "channel": "ALL", "budget": 420573.47, "currency": "PEN", "startDate": "2026-01-01", "endDate": "2026-12-31", "status": "ACTIVE"}
{"campaignId": "CMP-0007", "policyId": "POL-007", "name": "Campaña: 8% de descuento base en primas de seguros", "targetSegment": "PREFERENTE", "channel": "ALL", "budget": 314794.1, "currency": "PEN", "startDate": "2026-01-01", "endDate": "2026-12-31", "status": "FINISHED"}
{"campaignId": "CMP-0008", "policyId": "POL-008", "name": "Campaña: 8% de descuento base en transferencias y operaciones", "targetSegment": "PREMIUM", "channel": "ALL", "budget": 142453.63, "currency": "PEN", "startDate": "2026-01-01", "endDate": "2026-12-31", "status": "FINISHED"}
{"campaignId": "CMP-0009", "policyId": "POL-009", "name": "Campaña: 5% de descuento base en comisiones de cuentas (USD)", "targetSegment": "JOVEN", "channel": "ALL", "budget": 212523.88, "currency": "USD", "startDate": "2026-01-01", "endDate": "2026-12-31", "status": "SCHEDULED"}
{"campaignId": "CMP-0010", "policyId": "POL-010", "name": "Campaña: 8% de descuento base en membresías y cargos de tarjetas (USD)", "targetSegment": "JOVEN", "channel": "ALL", "budget": 62336.66, "currency": "USD", "startDate": "2026-01-01", "endDate": "2026-12-31", "status": "PAUSED"}
```

### 4.6 `bian-discount-pricing-transaction`

**Control Record: Discount Pricing Transaction.** Cada cálculo de descuento sobre la comisión de un producto.

- **Partition Key:** `discountPricingId`
- **Sort Key:** ninguna
- **GSI `pointOfSale-index`:** Partition Key `pointOfSaleReference` — Ver los cálculos de descuento de una operación comercial
- **Atributos:** pointOfSaleReference, customerReference, productReference, category, channel, currency, listPrice, quantity, grossAmount, policyId, policyName, discountAmount, netAmount, status, createdAt, updatedAt
- **Ejemplo de clave:** `DP-SEED-0001`
- **Relaciones:** Conecta con `policy`, `product-directory`, `point-of-sale` y `customer`. Es la tabla central.

Datos de ejemplo (10 de 100):

```json
{"discountPricingId": "DP-SEED-0001", "pointOfSaleReference": "SALE-SEED-0001", "customerReference": "PARTY-000024", "productReference": "PROD-0025", "category": "TARJETAS", "channel": "BRANCH", "currency": "PEN", "listPrice": 172.5, "quantity": 2, "grossAmount": 345.0, "policyId": "POL-022", "policyName": "12% de descuento en cargos desde S/ 100 por oficina", "discountAmount": 41.4, "netAmount": 303.6, "status": "CANCELLED", "createdAt": "2026-10-01T16:40:00+00:00", "updatedAt": "2026-10-01T16:48:00+00:00"}
{"discountPricingId": "DP-SEED-0002", "pointOfSaleReference": "SALE-SEED-0001", "customerReference": "PARTY-000024", "productReference": "PROD-0031", "category": "PRESTAMOS", "channel": "BRANCH", "currency": "PEN", "listPrice": 287.5, "quantity": 4, "grossAmount": 1150.0, "policyId": "POL-022", "policyName": "12% de descuento en cargos desde S/ 100 por oficina", "discountAmount": 138.0, "netAmount": 1012.0, "status": "CANCELLED", "createdAt": "2026-10-01T23:36:00+00:00", "updatedAt": "2026-10-01T23:47:00+00:00"}
{"discountPricingId": "DP-SEED-0003", "pointOfSaleReference": "SALE-SEED-0002", "customerReference": "PARTY-000020", "productReference": "PROD-0014", "category": "SEGUROS", "channel": "WEB", "currency": "PEN", "listPrice": 35.0, "quantity": 4, "grossAmount": 140.0, "policyId": "POL-088", "policyName": "12% de descuento en primas de seguros desde S/ 50", "discountAmount": 16.8, "netAmount": 123.2, "status": "APPLIED", "createdAt": "2026-10-02T06:57:00+00:00", "updatedAt": "2026-10-02T07:07:00+00:00"}
{"discountPricingId": "DP-SEED-0004", "pointOfSaleReference": "SALE-SEED-0002", "customerReference": "PARTY-000020", "productReference": "PROD-0021", "category": "CUENTAS", "channel": "WEB", "currency": "PEN", "listPrice": 13.8, "quantity": 3, "grossAmount": 41.4, "policyId": "POL-004", "policyName": "5% de descuento base en comisiones de cuentas", "discountAmount": 2.07, "netAmount": 39.33, "status": "CANCELLED", "createdAt": "2026-10-02T13:19:00+00:00", "updatedAt": "2026-10-02T13:28:00+00:00"}
{"discountPricingId": "DP-SEED-0005", "pointOfSaleReference": "SALE-SEED-0003", "customerReference": "PARTY-000063", "productReference": "PROD-0073", "category": "SEGUROS", "channel": "AGENT", "currency": "PEN", "listPrice": 14.4, "quantity": 1, "grossAmount": 14.4, "policyId": "POL-007", "policyName": "8% de descuento base en primas de seguros", "discountAmount": 1.15, "netAmount": 13.25, "status": "APPLIED", "createdAt": "2026-10-02T20:37:00+00:00", "updatedAt": "2026-10-02T20:46:00+00:00"}
{"discountPricingId": "DP-SEED-0006", "pointOfSaleReference": "SALE-SEED-0003", "customerReference": "PARTY-000063", "productReference": "PROD-0086", "category": "TARJETAS", "channel": "AGENT", "currency": "PEN", "listPrice": 175.0, "quantity": 1, "grossAmount": 175.0, "policyId": "POL-046", "policyName": "S/ 30 de descuento en membresías y cargos de tarjetas", "discountAmount": 30.0, "netAmount": 145.0, "status": "CANCELLED", "createdAt": "2026-10-03T03:23:00+00:00", "updatedAt": "2026-10-03T03:35:00+00:00"}
{"discountPricingId": "DP-SEED-0007", "pointOfSaleReference": "SALE-SEED-0004", "customerReference": "PARTY-000066", "productReference": "PROD-0074", "category": "SEGUROS", "channel": "MOBILE_APP", "currency": "PEN", "listPrice": 28.0, "quantity": 3, "grossAmount": 84.0, "policyId": "POL-088", "policyName": "12% de descuento en primas de seguros desde S/ 50", "discountAmount": 10.08, "netAmount": 73.92, "status": "CANCELLED", "createdAt": "2026-10-03T10:47:00+00:00", "updatedAt": "2026-10-03T11:13:00+00:00"}
{"discountPricingId": "DP-SEED-0008", "pointOfSaleReference": "SALE-SEED-0004", "customerReference": "PARTY-000066", "productReference": "PROD-0020", "category": "TRANSFERENCIAS", "channel": "MOBILE_APP", "currency": "USD", "listPrice": 16.0, "quantity": 3, "grossAmount": 48.0, "policyId": "POL-099", "policyName": "15% de descuento en Transferencia al exterior - Plan Estándar", "discountAmount": 7.2, "netAmount": 40.8, "status": "APPLIED", "createdAt": "2026-10-03T17:34:00+00:00", "updatedAt": "2026-10-03T17:43:00+00:00"}
{"discountPricingId": "DP-SEED-0009", "pointOfSaleReference": "SALE-SEED-0005", "customerReference": "PARTY-000012", "productReference": "PROD-0046", "category": "TARJETAS", "channel": "WEB", "currency": "PEN", "listPrice": 350.0, "quantity": 4, "grossAmount": 1400.0, "policyId": "POL-001", "policyName": "Descuento 10% en membresías y cargos de tarjetas", "discountAmount": 140.0, "netAmount": 1260.0, "status": "INITIATED", "createdAt": "2026-10-04T00:33:00+00:00", "updatedAt": "2026-10-04T00:33:00+00:00"}
{"discountPricingId": "DP-SEED-0010", "pointOfSaleReference": "SALE-SEED-0005", "customerReference": "PARTY-000012", "productReference": "PROD-0060", "category": "TRANSFERENCIAS", "channel": "WEB", "currency": "USD", "listPrice": 22.4, "quantity": 2, "grossAmount": 44.8, "policyId": "NONE", "policyName": "Sin descuento aplicable", "discountAmount": 0.0, "netAmount": 44.8, "status": "APPLIED", "createdAt": "2026-10-04T07:01:00+00:00", "updatedAt": "2026-10-04T07:22:00+00:00"}
```

### 4.7 `bian-discount-pricing-event-log`

Bitácora de auditoría: historial de estados de cada transacción (solo se agrega).

- **Partition Key:** `discountPricingId`
- **Sort Key:** `eventTimestamp`
- **GSI:** ninguno
- **Atributos:** eventType, actor, policyId, details
- **Ejemplo de clave:** `DP-SEED-0001 + 2026-10-01T16:48:00+00:00`
- **Relaciones:** `discountPricingId` apunta a `transaction`. Sin GSI: se consulta por PK y se ordena por fecha (SK).

Datos de ejemplo (10 de 100):

```json
{"discountPricingId": "DP-SEED-0001", "eventTimestamp": "2026-10-01T16:48:00+00:00", "eventType": "CANCELLED", "actor": "oficina", "policyId": "POL-022", "details": "DP-SEED-0001 pasó a CANCELLED (neto S/ 303.60)"}
{"discountPricingId": "DP-SEED-0002", "eventTimestamp": "2026-10-01T23:47:00+00:00", "eventType": "CANCELLED", "actor": "oficina", "policyId": "POL-022", "details": "DP-SEED-0002 pasó a CANCELLED (neto S/ 1012.00)"}
{"discountPricingId": "DP-SEED-0003", "eventTimestamp": "2026-10-02T07:07:00+00:00", "eventType": "APPLIED", "actor": "banca-internet", "policyId": "POL-088", "details": "DP-SEED-0003 pasó a APPLIED (neto S/ 123.20)"}
{"discountPricingId": "DP-SEED-0004", "eventTimestamp": "2026-10-02T13:28:00+00:00", "eventType": "CANCELLED", "actor": "banca-internet", "policyId": "POL-004", "details": "DP-SEED-0004 pasó a CANCELLED (neto S/ 39.33)"}
{"discountPricingId": "DP-SEED-0005", "eventTimestamp": "2026-10-02T20:46:00+00:00", "eventType": "APPLIED", "actor": "agente", "policyId": "POL-007", "details": "DP-SEED-0005 pasó a APPLIED (neto S/ 13.25)"}
{"discountPricingId": "DP-SEED-0006", "eventTimestamp": "2026-10-03T03:35:00+00:00", "eventType": "CANCELLED", "actor": "agente", "policyId": "POL-046", "details": "DP-SEED-0006 pasó a CANCELLED (neto S/ 145.00)"}
{"discountPricingId": "DP-SEED-0007", "eventTimestamp": "2026-10-03T11:13:00+00:00", "eventType": "CANCELLED", "actor": "app-movil", "policyId": "POL-088", "details": "DP-SEED-0007 pasó a CANCELLED (neto S/ 73.92)"}
{"discountPricingId": "DP-SEED-0008", "eventTimestamp": "2026-10-03T17:43:00+00:00", "eventType": "APPLIED", "actor": "app-movil", "policyId": "POL-099", "details": "DP-SEED-0008 pasó a APPLIED (neto US$ 40.80)"}
{"discountPricingId": "DP-SEED-0009", "eventTimestamp": "2026-10-04T00:33:00+00:00", "eventType": "INITIATED", "actor": "banca-internet", "policyId": "POL-001", "details": "DP-SEED-0009 pasó a INITIATED (neto S/ 1260.00)"}
{"discountPricingId": "DP-SEED-0010", "eventTimestamp": "2026-10-04T07:22:00+00:00", "eventType": "APPLIED", "actor": "banca-internet", "policyId": "NONE", "details": "DP-SEED-0010 pasó a APPLIED (neto US$ 44.80)"}
```

### 4.8 `bian-payment-order`

Orden de pago del cargo ya descontado (se cobra el monto neto).

- **Partition Key:** `paymentOrderId`
- **Sort Key:** ninguna
- **GSI `pointOfSale-index`:** Partition Key `pointOfSaleReference` — Ver los pagos de una operación comercial
- **Atributos:** discountPricingId, customerReference, pointOfSaleReference, amount, currency, paymentMethod, status, paymentDate
- **Ejemplo de clave:** `PAY-0001`
- **Relaciones:** `amount` = `netAmount` de su transacción; apunta a `point-of-sale`, `customer` y `transaction`.

Datos de ejemplo (10 de 100):

```json
{"paymentOrderId": "PAY-0001", "discountPricingId": "DP-SEED-0001", "customerReference": "PARTY-000024", "pointOfSaleReference": "SALE-SEED-0001", "amount": 303.6, "currency": "PEN", "paymentMethod": "CARGO_EN_CUENTA", "status": "CANCELLED", "paymentDate": "2026-10-01T16:48:00+00:00"}
{"paymentOrderId": "PAY-0002", "discountPricingId": "DP-SEED-0002", "customerReference": "PARTY-000024", "pointOfSaleReference": "SALE-SEED-0001", "amount": 1012.0, "currency": "PEN", "paymentMethod": "EFECTIVO_VENTANILLA", "status": "CANCELLED", "paymentDate": "2026-10-01T23:47:00+00:00"}
{"paymentOrderId": "PAY-0003", "discountPricingId": "DP-SEED-0003", "customerReference": "PARTY-000020", "pointOfSaleReference": "SALE-SEED-0002", "amount": 123.2, "currency": "PEN", "paymentMethod": "CARGO_EN_CUENTA", "status": "PAID", "paymentDate": "2026-10-02T07:07:00+00:00"}
{"paymentOrderId": "PAY-0004", "discountPricingId": "DP-SEED-0004", "customerReference": "PARTY-000020", "pointOfSaleReference": "SALE-SEED-0002", "amount": 39.33, "currency": "PEN", "paymentMethod": "TARJETA_DEBITO", "status": "CANCELLED", "paymentDate": "2026-10-02T13:28:00+00:00"}
{"paymentOrderId": "PAY-0005", "discountPricingId": "DP-SEED-0005", "customerReference": "PARTY-000063", "pointOfSaleReference": "SALE-SEED-0003", "amount": 13.25, "currency": "PEN", "paymentMethod": "CARGO_EN_CUENTA", "status": "PAID", "paymentDate": "2026-10-02T20:46:00+00:00"}
{"paymentOrderId": "PAY-0006", "discountPricingId": "DP-SEED-0006", "customerReference": "PARTY-000063", "pointOfSaleReference": "SALE-SEED-0003", "amount": 145.0, "currency": "PEN", "paymentMethod": "TARJETA_DEBITO", "status": "CANCELLED", "paymentDate": "2026-10-03T03:35:00+00:00"}
{"paymentOrderId": "PAY-0007", "discountPricingId": "DP-SEED-0007", "customerReference": "PARTY-000066", "pointOfSaleReference": "SALE-SEED-0004", "amount": 73.92, "currency": "PEN", "paymentMethod": "CARGO_EN_CUENTA", "status": "CANCELLED", "paymentDate": "2026-10-03T11:13:00+00:00"}
{"paymentOrderId": "PAY-0008", "discountPricingId": "DP-SEED-0008", "customerReference": "PARTY-000066", "pointOfSaleReference": "SALE-SEED-0004", "amount": 40.8, "currency": "USD", "paymentMethod": "TARJETA_DEBITO", "status": "PAID", "paymentDate": "2026-10-03T17:43:00+00:00"}
{"paymentOrderId": "PAY-0009", "discountPricingId": "DP-SEED-0009", "customerReference": "PARTY-000012", "pointOfSaleReference": "SALE-SEED-0005", "amount": 1260.0, "currency": "PEN", "paymentMethod": "CARGO_EN_CUENTA", "status": "PENDING", "paymentDate": "2026-10-04T00:33:00+00:00"}
{"paymentOrderId": "PAY-0010", "discountPricingId": "DP-SEED-0010", "customerReference": "PARTY-000012", "pointOfSaleReference": "SALE-SEED-0005", "amount": 44.8, "currency": "USD", "paymentMethod": "CARGO_EN_CUENTA", "status": "PAID", "paymentDate": "2026-10-04T07:22:00+00:00"}
```
