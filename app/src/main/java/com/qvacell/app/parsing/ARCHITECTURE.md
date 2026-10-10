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

**Stub** — awaiting ETECSA USSD response samples.

---

### `bonus-usd-plans`

**Stub** — awaiting ETECSA USSD response samples.

---

### `data-plan`

**Stub** — awaiting ETECSA USSD response samples.

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

**Stub** — awaiting ETECSA USSD response samples.

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
