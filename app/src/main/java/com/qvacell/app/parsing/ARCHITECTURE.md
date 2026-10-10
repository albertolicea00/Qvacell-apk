# Parsing Architecture

ETECSA USSD response and SMS body parsing rules for the dashboard data pipeline.
Each code/signal gets its own section below with verbatim response samples, regex patterns, and field mappings.

Root ARCHITECTURE.md §15 covers the pipeline, persistence, and capture mechanism.
This document covers only the parsing rules themselves.

---

## Contracts

### USSD (`UssdResponseParser`)

- One parser per `codes.json` id (see `UssdParsers.KNOWN_CODE_IDS`).
- `parse(rawResponseText)` → `ParseResult<List<ParsedDashboardValue>>`.
- `Success` → list of `ParsedDashboardValue` with `FieldTypes.*` keys.
- `Unrecognized` → real parser, text didn't match any known pattern.
- `Unresolved` → stub, rules not yet implemented.

### SMS (`SmsBodyParser`)

- One parser per signal id (see `SmsParsers.KNOWN_SIGNAL_IDS`).
- `matches(rawSmsBody, sender)` → true if this parser handles it.
- `parse(rawSmsBody)` → same `ParseResult` contract.

### Common patterns

- Amounts: comma or dot decimal separator, with or without decimals, optional space before unit.
- Dates: dd-MM-yy (2-digit year) and dd-MM-yyyy (4-digit year). Parsed via `NationalRechargeLimitParser.parseDateDmy()`.
- `reached` field: `numericValue` 1.0 = true, 0.0 = false.

---

## USSD Parsers

### `national-recharge-limit`

Code id: `national-recharge-limit`
Parser: `NationalRechargeLimitParser.kt`
Tests: `NationalRechargeLimitParserTest.kt` (11 cases)

ETECSA returns one of three response variants:

#### Variant 1 — Full limit available, period-based

```
Ud puede recargar un monto de 360,00CUP en un plazo de 30 dias
```

User has not used any recharge quota. Full limit available for stated period.

| FieldType | Value | Unit |
|---|---|---|
| `national_recharge_limit_amount` | 360.0 | CUP |
| `national_recharge_limit_reached` | 0.0 | — |
| `national_recharge_limit_period_days` | 30.0 | — |

#### Variant 2 — Partial remaining, date-bounded

```
Ud puede recargar un monto de 120,00CUP hasta el 25-10-25
```

User has partially consumed monthly limit. 120 CUP remaining until given date.

| FieldType | Value | Unit |
|---|---|---|
| `national_recharge_limit_remaining` | 120.0 | CUP |
| `national_recharge_limit_reached` | 0.0 | — |
| `national_recharge_limit_available_from` | epoch(2025-10-25) | — |

#### Variant 3 — Limit reached

```
Ud ha alcanzado el monto de recarga permitido de 360 CUP. Puede recargar posterior al dia 24-10-26
```

Monthly limit fully consumed. Recharge blocked until stated date.

| FieldType | Value | Unit |
|---|---|---|
| `national_recharge_limit_amount` | 360.0 | CUP |
| `national_recharge_limit_reached` | 1.0 | — |
| `national_recharge_limit_available_from` | epoch(2026-10-24) | — |

#### Parser notes

- Amount accepts comma or dot as decimal separator, with or without decimals, optional space before `CUP`.
- Dates: dd-MM-yy and dd-MM-yyyy.
- Text not matching any of the three patterns → `ParseResult.Unrecognized`.

---

### `main-balance`

Code id: `main-balance`
USSD: `*222#`
Parser: `MainBalanceParser.kt`
Tests: `MainBalanceParserTest.kt` (12 cases)

**Cross-card parser** — one USSD response populates fields across multiple dashboard cards. No special event system needed: `recordUssdParse` stores all `ParsedDashboardValue` entries, and `observeCurrentValues()` emits all fields regardless of source code id.

#### Full response (all sections)

```
Saldo: 227.05 CUP. Datos: 4.03 GB. Voz: 00:45:00. SMS: 57 Linea activa hasta 17-08-27 vence 17-02-28.
Saldo: 15.01 CUP. Datos: 57.35 MB. Voz: 119:51:13. SMS: 8401 Linea activa hasta 20-08-27 vence 16-02-28.
```

| FieldType | Source section | Value | Card |
|---|---|---|---|
| `main_balance` | `Saldo:` | amount (CUP) | MainBalanceCard |
| `data_datos_mb` | `Datos:` | MB (GB auto-converted) | DataUsageCard |
| `voice_minutes_remaining` | `Voz:` | total minutes + textValue HH:MM:SS | VoiceSmsRow |
| `sms_count_remaining` | `SMS:` | count | VoiceSmsRow |
| `line_active_until` | `Linea activa hasta` | epoch(date) | MainBalanceCard |
| `account_due_date` | `vence` | epoch(date) | MainBalanceCard |

#### Minimal response (balance only, no plans)

```
Saldo: 199.35 CUP. Linea activa hasta 13-08-27 vence 09-02-28.
```

Only `main_balance`, `line_active_until`, `account_due_date` emitted. Datos/Voz/SMS fields absent.

#### Field reuse

Fields emitted by this parser are the same `FieldTypes` keys used by the dedicated parsers (`data-plan`, `voice-balance`, `sms-balance`). The reconciliation rule (§15.4) applies: whichever source produces the most recent `USSD_REAL` reading wins per field. This means dialing `*222#` alone can refresh all cards in one shot, with dedicated codes providing more detail (days remaining, plan active status, tariff) when queried individually.

#### Parser notes

- `Saldo:` required — if missing → `Unrecognized`.
- All other sections optional and extracted independently.
- Amounts: comma or dot decimal separator.
- Dates: dd-MM-yy and dd-MM-yyyy via shared `parseDateDmy()`.
- Datos/Voz/SMS sections here carry no days-remaining or active status — those come from dedicated codes only.

---

### `bonus-usd-plans`

Code id: `bonus-usd-plans`
USSD: `*222*266#`
Parser: `BonusUsdPlansParser.kt`
Tests: `BonusUsdPlansParserTest.kt` (13 cases)

Multi-bonus response — one USSD reply can contain several bonus entries. Parser extracts each independently via `findAll`.

#### Variant 1 — Datos.cu only

```
Datos.cu: 287 MB vence 31-10-26.
Datos.cu: 300 MB vence 29-10-26.
```

| FieldType | Value | Unit |
|---|---|---|
| `bonus_active` | 1.0 | — |
| `bonus_datos_cu_mb` | amount (MB) | MB |
| `bonus_datos_cu_expiry` | epoch(date) | — |

GB amounts auto-converted to MB (`1 GB` → `1024.0 MB`).

#### Variant 2 — No active bonuses

```
Usted no dispone de bonos activos.
```

| FieldType | Value | Unit |
|---|---|---|
| `bonus_active` | 0.0 | — |

#### Variant 3 — Multi-bonus (Nocturno + Datos.cu)

```
Datos: ilimitados: Nocturno vence 24-10-26. Datos.cu 285 MB vence 27-10-26.
```

| FieldType | Value | Unit |
|---|---|---|
| `bonus_active` | 1.0 | — |
| `bonus_datos_nocturno_expiry` | epoch(date) | — |
| `bonus_datos_cu_mb` | amount (MB) | MB |
| `bonus_datos_cu_expiry` | epoch(date) | — |

Nocturno-only (without Datos.cu) also valid — emits `bonus_active` + `bonus_datos_nocturno_expiry`.

#### Parser notes

- Colon after `Datos.cu` optional (seen both `Datos.cu:` and `Datos.cu ` in samples).
- Dates: dd-MM-yy and dd-MM-yyyy via shared `parseDateDmy()`.
- New bonus types (e.g., LTE, international) → add regex + FieldType when samples arrive.
- Text not matching any pattern and not "no dispone" → `ParseResult.Unrecognized`.

---

### `data-plan`

Code id: `data-plan`
USSD: `*222*328#`
Parser: `DataPlanParser.kt`
Tests: `DataPlanParserTest.kt` (16 cases)

Multi-section response. Always starts with `Tarifa:`, then zero or more independent sections. Parser extracts each via independent regex.

#### Structure

```
Tarifa: (Activa|No activa). [Diaria: ...] [toDus: ...] [Datos: ...] [Paquetes: ...]
```

All sections optional except Tarifa. Sections can appear in any combination.

#### Tarifa (always present)

```
Tarifa: No activa.
Tarifa: Activa.
```

| FieldType | Value |
|---|---|
| `data_tariff_active` | 1.0 (Activa) or 0.0 (No activa) |

#### No plan

```
Tarifa: No activa. Ud debe adquirir una oferta. Para una nueva compra marque *133#
```

| FieldType | Value |
|---|---|
| `data_tariff_active` | 0.0 |
| `data_plan_active` | 0.0 |

Only these two fields emitted. No section fields.

#### Datos section

```
Datos: 4.49 GB no activos.
Datos: 4.03 GB validos 21 dias.
Datos: 57.35 MB validos 19 dias.
```

| FieldType | Value | Unit |
|---|---|---|
| `data_datos_active` | 1.0 (validos) or 0.0 (no activos) | — |
| `data_datos_mb` | amount in MB (GB auto-converted) | MB |
| `data_datos_days` | days remaining (only when active) | — |

#### Diaria section

```
Diaria: 195 MB validos 24 horas.
Diaria: 200 MB no activos.
```

| FieldType | Value | Unit |
|---|---|---|
| `data_diaria_active` | 1.0 (validos 24h) or 0.0 (no activos) | — |
| `data_diaria_mb` | amount in MB | MB |

No days field — always "24 horas" when active.

#### toDus section

```
toDus: 1024.00 MB validos 34 dias.
```

| FieldType | Value | Unit |
|---|---|---|
| `data_todus_mb` | amount in MB | MB |
| `data_todus_days` | days remaining | — |

#### Paquetes section

```
Paquetes: No dispone de MB.
```

Informational only — no fields emitted. Presence doesn't affect other sections.

#### Parser notes

- GB→MB auto-conversion for Datos section.
- Amounts: comma or dot decimal separator via shared `parseAmount()`.
- `Tarifa:` required — if missing → `Unrecognized`.
- 10 real ETECSA samples documented, all covered by tests.

---

### `voice-balance`

Code id: `voice-balance`
USSD: `*222*869#`
Parser: `VoiceBalanceParser.kt`
Tests: `VoiceBalanceParserTest.kt` (8 cases)

#### Variant 1 & 2 — Has minutes remaining

```
Usted dispone de 119:51:13 MIN NAC validos por 19 dias
Usted dispone de 00:45:00 MIN NAC validos por 21 dias
```

User has active voice plan. Duration in HH:MM:SS format, days until expiry.

| FieldType | Value | Unit |
|---|---|---|
| `voice_plan_active` | 1.0 | — |
| `voice_minutes_remaining` | total minutes (double) | MIN NAC |
| `voice_days_remaining` | days (double) | — |

`voice_minutes_remaining` also stores raw `HH:MM:SS` in `textValue` for display.
Numeric conversion: `hours*60 + minutes + seconds/60`.

#### Variant 3 — No plan

```
Usted debe adquirir un plan de minutos. Para una nueva compra marque *133#
```

User has no active voice plan.

| FieldType | Value | Unit |
|---|---|---|
| `voice_plan_active` | 0.0 | — |

#### Parser notes

- Hours field: 1-3 digits (supports 0 to 999).
- Only `MIN NAC` (national minutes) recognized. Other minute types → future variant if observed.
- Text not matching → `ParseResult.Unrecognized`.

---

### `sms-balance`

Code id: `sms-balance`
USSD: `*222*767#`
Parser: `SmsBalanceParser.kt`
Tests: `SmsBalanceParserTest.kt` (8 cases)

#### Variant 1 & 2 — Has SMS remaining

```
Usted dispone de 8401 SMS validos por 19 dias
Usted dispone de 57 SMS validos por 19 dias
```

User has active SMS plan. Count as integer, days until expiry.

| FieldType | Value | Unit |
|---|---|---|
| `sms_plan_active` | 1.0 | — |
| `sms_count_remaining` | count (double) | SMS |
| `sms_days_remaining` | days (double) | — |

#### Variant 3 — No plan

```
Usted debe adquirir un plan de SMS. Para una nueva compra marque *133#
```

User has no active SMS plan.

| FieldType | Value | Unit |
|---|---|---|
| `sms_plan_active` | 0.0 | — |

#### Parser notes

- Count: any non-negative integer.
- Text not matching → `ParseResult.Unrecognized`.

---

## SMS Parsers

### `etecsa-balance-alert`

**Stub** — awaiting real SMS sender/body samples.

---

### `etecsa-deduction-notice`

**Stub** — awaiting real SMS sender/body samples.

---

### `etecsa-limit-date`

**Stub** — awaiting real SMS sender/body samples.
