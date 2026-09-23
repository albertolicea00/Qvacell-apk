# Qvacell :: Architecture

For contribution workflow see [CONTRIBUTING.md](CONTRIBUTING.md).

This document describes the technical architecture of Qvacell for Android, a native Kotlin/Jetpack Compose app that lets users dial ETECSA (Cubacel) USSD service codes (`*222#` style dial strings) without needing to remember them.

It is an Android port of [qvacell-ios](https://github.com/albertolicea00/qvacell-ios), following the same product shape: a dependency-free (beyond AndroidX/Jetpack), backend-free app with no network calls of its own. Almost everything comes from one read-only bundled JSON catalog; the two exceptions are the device's own Contacts (read live via `ContactsContract`, never sent anywhere) and a local Room table a `CallScreeningService` reads to label `*99` collect calls (§11) — there is still no server, no analytics, and no third-party dependency beyond Jetpack/AndroidX anywhere in the project.

---

## 1. High-Level Overview

```
┌──────────────────────────────────────────────────┐
│                 QvacellApplication                │
│      (creates notification channels at launch)    │
└───────────────────────────┬────────────────────────┘
                            │
                            ▼
                       MainActivity
        (single Activity · applies theme · requests
         POST_NOTIFICATIONS on API 33+ · hosts NavHost)
                            │
                            ▼
                     QvacellNavHost
              (bottom nav, 5 tabs: Ayuda, Contactos,
               Home, Compras, Ajustes)
        ┌──────────┬──────────┬──────────┬──────────┐
        ▼          ▼          ▼          ▼          ▼
   CategoryListScreen (×2, helplines/purchase)   SettingsScreen
   (per-category code list · dials/prompts)      (nested routes)
        │                                              │
        ▼                                              ▼
   dial/dialog/SMS-intent,                    pushes: Reminders, SMS
   depending on UssdCode.type                 services, WiFi rooms,
                                               Directory search (hidden),
                                               Transfer PIN, Caller-ID
                                               role request, Help
        │
        ▼
   DialService (ACTION_DIAL / ACTION_SENDTO)


          CatalogRepository ── decodes ──▶ UssdCatalog
                  │
                  └── loads assets/codes.json (bundled, read-only)
```

There is no MVVM view-model layer and no cross-screen navigation state machine beyond Compose Navigation's own back stack. `CatalogRepository` (the catalog) and `SettingsDataStore` (theme/default tab/accent color) are the two pieces of app-wide state; `ContactsRepository`, `TransferPinStore`, `ReminderRepository`, and `DirectoryDatabase` are created where they're used instead of injected globally — there is no dependency-injection framework (no Hilt/Koin), matching the iOS app's equally minimal-DI approach. State flows one way — repositories/stores (read-only or self-contained) → composables. There is no unified "code detail" screen — each `UssdCode.type` (`USSD`, `CALL`, `SMS`) drives a different tap behavior directly (dial, dialog-for-input-then-dial, or open an SMS intent). Persisted state includes `SettingsDataStore` (Preferences DataStore: theme mode, default tab, accent color hex, debug-search unlock flag), a Room database (`reminders` and `wrapped_callers` tables), and the transfer PIN in `EncryptedSharedPreferences` (`TransferPinStore`, §12).

---

## 2. Source Layout

| File/Package                                       | Responsibility                                                                                                                                                                                                                                                                                     |
| -------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `QvacellApplication.kt`                            | `Application` subclass. Creates the `reminders` and `caller_id` notification channels at process start.                                                                                                                                                                                            |
| `MainActivity.kt`                                  | Single-`Activity` host. Requests `POST_NOTIFICATIONS` on API 33+ at launch, reads the persisted theme from `SettingsDataStore` and applies it, hosts `QvacellNavHost`.                                                                                                                             |
| `model/Models.kt`                                  | `@Serializable` catalog models (`UssdCatalog`, `UssdCategory`, `UssdCodeGroup`, `UssdCode`, `UssdActionType`, `SmsVariant`) and the WiFi navigation-room models (`WifiProvince`, `WifiRoom`, `WifiHotspotGroup`).                                                                                  |
| `model/CubanPhoneNumber.kt`                        | The one place that validates/normalizes a Cuban mobile number — ported 1:1 from the iOS app's `CubanPhoneNumber`.                                                                                                                                                                                  |
| `model/Reminder.kt`                                | Room `@Entity` for a scheduled reminder, plus `ReminderRecurrence` (`NONE`/`DAILY`/`WEEKLY`/`MONTHLY`/`CUSTOM`).                                                                                                                                                                                   |
| `model/WrappedCaller.kt`                           | Room `@Entity` mapping an ETECSA `*99`-wrapped collect-call number to a contact name, plus the `wrappedNumber(localNumber)` formula (§11).                                                                                                                                                         |
| `data/CatalogRepository.kt`                        | Loads/decodes `assets/codes.json` and `assets/wifi_navigation_rooms.json` via `kotlinx.serialization`.                                                                                                                                                                                             |
| `data/QvacellDatabase.kt`                          | Room database (`reminders`, `wrapped_callers` tables).                                                                                                                                                                                                                                             |
| `data/ReminderDao.kt` / `data/WrappedCallerDao.kt` | Room DAOs.                                                                                                                                                                                                                                                                                         |
| `data/Converters.kt`                               | Room `TypeConverter`s (e.g. `ReminderRecurrence` enum ↔ `String`).                                                                                                                                                                                                                                 |
| `data/SettingsDataStore.kt`                        | Preferences DataStore: theme mode, default launch tab, accent color hex, hidden debug-search unlock flag.                                                                                                                                                                                          |
| `service/DialService.kt`                           | Builds/opens `tel:` URIs via `ACTION_DIAL` by default (`ACTION_CALL` only when the user opts into Ajustes › Identificador de Llamadas › "Marcar Directamente" and `CALL_PHONE` is granted), SMS intents via `ACTION_SENDTO`, and Google Maps search URLs via `ACTION_VIEW`.                        |
| `service/CellularMonitor.kt`                       | Wraps `TelephonyManager` to expose the current network type (5G/LTE/3G/2G/none) as a `StateFlow<String>`, for an optional weak/no-signal warning banner.                                                                                                                                           |
| `service/ContactsRepository.kt`                    | Queries `ContactsContract` for device contacts, normalizes any Cuban numbers via `CubanPhoneNumber`, and rebuilds the `wrapped_callers` Room table on every fetch (§11) — the Android equivalent of the iOS app's `ContactsService`.                                                               |
| `service/TransferPinStore.kt`                      | `EncryptedSharedPreferences`-backed transfer PIN store (§12) — the Android equivalent of the iOS app's Keychain-backed `TransferPinStore`.                                                                                                                                                         |
| `service/ReminderRepository.kt`                    | CRUD over `Reminder` via `ReminderDao`, as a `Flow`.                                                                                                                                                                                                                                               |
| `service/ReminderScheduler.kt`                     | Schedules/cancels `AlarmManager.setExactAndAllowWhileIdle` alarms per reminder; computes the next occurrence for recurring reminders (§6).                                                                                                                                                         |
| `service/DirectoryDatabase.kt`                     | Read-only SQLite reverse-lookup over a user-imported `.db` file (§13) — the Android equivalent of the iOS app's `DirectoryDatabase`.                                                                                                                                                               |
| `service/CallerIdScreeningService.kt`              | `CallScreeningService` that unwraps `*99` collect-call numbers and surfaces the resolved name via notification (§11).                                                                                                                                                                              |
| `receiver/ReminderAlarmReceiver.kt`                | `BroadcastReceiver` fired by `AlarmManager`; shows the reminder notification and reschedules if recurring.                                                                                                                                                                                         |
| `receiver/ReminderActionReceiver.kt`               | Handles the "Mark done" / "Snooze 1 day" notification actions.                                                                                                                                                                                                                                     |
| `receiver/BootCompletedReceiver.kt`                | Re-registers all enabled reminders after `ACTION_BOOT_COMPLETED`, since `AlarmManager` alarms are cleared on reboot (§6) — a correctness step iOS local notifications don't need.                                                                                                                  |
| `ui/AndroidIconResolver.kt`                        | Resolves the `icon` string in this repo's `codes.json` (e.g. `"Filled.Home"` — this repo's own copy replaces iOS's SF Symbol names with Material icon identifiers in that same field) to a `androidx.compose.material.icons` `ImageVector` via reflection; falls back to a generic circle icon when unset or unresolvable. |
| `ui/components/`                                   | Reusable, presentation-only composables: `CodeRow`, `ContactRow`, `DirectoryEntryRow`, `ConnectionBanner`, and `CodeActionHandler` (the input/variant/options dialog + dispatch logic shared by every code-tap flow).                                                                              |
| `ui/navigation/NavGraph.kt`                        | `BottomTab` sealed class (the 5 fixed tabs) and `Routes` (nested route constants for screens pushed from Ajustes).                                                                                                                                                                                 |
| `ui/navigation/QvacellNavHost.kt`                  | Compose `NavHost` wiring bottom-tab routes and nested routes together.                                                                                                                                                                                                                             |
| `ui/screens/`                                      | `CategoryListScreen` (Ayuda/Compras), `ContactsListScreen`, `HomeQuickActionsScreen`, `TransferFlowScreen`, `RechargeFlowScreen`, `ReminderListScreen`/`ReminderEditScreen`, `DirectorySearchScreen`, `WifiRoomsScreen`, `TransferPinScreen`, `SettingsScreen`, `HelpScreen`, `SmsServicesScreen`. |
| `app/src/main/assets/codes.json`                   | Static, bundled dataset: version, carrier, categories → groups → codes, each with its dial string and presentation metadata (§3). Byte-identical to the iOS app's copy.                                                                                                                            |
| `app/src/main/assets/wifi_navigation_rooms.json`   | Static, bundled dataset: one entry per province with its navigation rooms and free WiFi hotspots. Byte-identical to the iOS app's copy.                                                                                                                                                            |

No separate networking layer or dependency-injection container exists — the `service/` package _is_ the service layer, and each service is instantiated where it's needed (typically inside a `remember { }` in the composable that uses it).

---

## 3. Configuration Data: `codes.json`

`codes.json` is the single source of truth for **what USSD/call/SMS codes exist** and is treated as read-only, bundled data, shared verbatim with the iOS app. It decodes into:

```
UssdCatalog
 ├─ version, carrier
 └─ categories: [UssdCategory]
     ├─ id, name, icon (Material icon identifier, e.g. `"Filled.Home"` — resolved via AndroidIconResolver.kt; iOS's copy of this same field holds an SF Symbol name instead)
     └─ groups: [UssdCodeGroup]        (a named sub-heading, or nil-named for no header)
         ├─ name: String?
         └─ codes: [UssdCode]
             ├─ id, code, title, details
             ├─ icon (Material icon identifier, only present on codes that already had an SF Symbol on iOS), price, compact, showsNumber   (all optional — presentation hints)
             ├─ type: UssdActionType   (USSD / CALL / SMS — drives dial-vs-dialog-vs-SMS-intent behavior)
             ├─ requiresInput: Boolean, inputPlaceholder: String?
             ├─ noConfirmCode: String?             (Quick-Purchase-without-confirmation variant of `code`)
             ├─ smsBody: String?                   (for SMS codes: the message text sent to `code`)
             ├─ options: [String]?                 (for SMS codes with a fixed picker of message bodies)
             ├─ isSubscription: Boolean?
             └─ variants: [SmsVariant]?             (label + smsBody pairs, for a small fixed choice set)
```

A code carries no `category` field of its own — its category and group are entirely implied by where it sits in the nested JSON. `CatalogRepository` reads and decodes both bundled JSON files once, from `assets/`, via `kotlinx.serialization.json.Json.decodeFromString`. There is **no schema versioning enforcement and no remote fetch** — updating codes requires shipping a new app build, same as iOS.

---

## 4. Navigation Model

`QvacellNavHost` hosts a bottom navigation bar with 5 explicit tabs — Ayuda, Contactos, Home, Compras, Ajustes (`BottomTab` in `NavGraph.kt`) — not a generic loop over every catalog category (the tab order is fixed on purpose: Home sits in the middle). Ayuda and Compras each host a `CategoryListScreen` bound to the matching `UssdCategory` (`helplines`/`purchase`); adding a _code or group_ to either category in `codes.json` updates that tab automatically, but adding a whole new top-level category does **not** grow the bottom bar — the tab set itself is fixed, same constraint as iOS.

Tapping a row in `CategoryListScreen` dials/prompts/sends an SMS intent directly depending on `UssdCode.type` and `requiresInput`, via the shared `CodeActionHandler` — there is no single "code detail" screen type.

`SettingsScreen` pushes nested routes (`Routes` in `NavGraph.kt`) for Reminders, SMS services, WiFi rooms, Directory search (hidden by default), Transfer PIN management, and Help — same structure as the iOS app's `SettingsView` push destinations, minus Siri/Shortcuts (iOS-only, not ported) and with the Caller-ID settings replaced by a role-request action (§11) instead of a system-settings deep link.

---

## 5. Code Execution Path

There is no single "code detail" screen — what a tap does depends on `UssdCode.type`, handled by `CodeActionHandler`:

1. **Row tap, no input required** (`requiresInput == false`): dials/sends immediately — see steps 3/4 below for what that means per type.
2. **Row tap, input required** (`requiresInput == true`): a dialog collects a text value and disables the confirm button until it is non-empty; `UssdCode.resolvedCode(input)` (or `resolvedSmsBody(input)` for SMS codes) substitutes the `{input}` placeholder.
3. **`USSD`/`CALL`**: `DialService.dial(context, code)` URI-encodes the code, builds a `tel:` URI, and starts an `ACTION_DIAL` intent by default — Android's system dialer opens with the code prefilled, requiring the same explicit user confirmation `tel://` gets on iOS. `ACTION_DIAL` (not `ACTION_CALL`) was chosen deliberately so the app doesn't need the `CALL_PHONE` permission and doesn't place a call without the user tapping the dialer's own call button — **unless** the user has explicitly turned on "Marcar Directamente" in Ajustes › Identificador de Llamadas (§9), in which case `DialService` switches every call in the app to `ACTION_CALL` (immediate, no confirmation step), but only while `CALL_PHONE` is actually granted; if the permission is ever revoked, `dial()` silently falls back to `ACTION_DIAL` rather than crashing.
4. **`SMS`**: `DialService.sendSms(context, number, body)` opens the device's SMS app pre-filled via `ACTION_SENDTO` with `smsto:` + the code as the recipient and the resolved `smsBody` as the message — never sent silently, same one-more-tap-to-confirm shape as a dial. Codes with `options` or `variants` show a picker first.
5. **Quick Purchase, no confirmation** (opt-in, Compras only): when the "Acción sin Confirmación" setting is on and the code has a `noConfirmCode`, that string is dialed instead of `code` — it auto-selects ETECSA's own "¿Confirma su compra? 1. Sí" step in one dial instead of stopping there. See README § Direct dial vs. confirmation.

Separately, the Home Transferir flow and the offline Database search prefill a PIN field from `TransferPinStore` (§12), but that's local to those specific screens, not a general mechanism.

---

## 6. Reminders & Local Notifications

Ajustes › Utilidades › Recordatorios schedules local notifications for a purchase/recharge/transfer the user needs to make — entirely on-device, no push infrastructure, no server, consistent with §1's "no backend, no network calls."

### 6.1 Model & scheduling

- `Reminder` (`model/Reminder.kt`, Room `@Entity`): `id`, `title`, `message`, `iconName`, `ussdCodeId` (nullable, resolved via `CatalogRepository` at execute time), `phoneNumber`, `date` (epoch millis), `recurrence` (`ReminderRecurrence`: `NONE`/`DAILY`/`WEEKLY`/`MONTHLY`/`CUSTOM`), `customIntervalDays`, `isEnabled`, `templateKey`.
- `ReminderRepository` (`service/ReminderRepository.kt`): CRUD over `reminders` via `ReminderDao`, exposed as a `Flow`.
- `ReminderScheduler` (`service/ReminderScheduler.kt`): `AlarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ...)` per reminder; `nextOccurrence(reminder)` computes the next fire time for a recurring reminder using `Calendar` arithmetic (`+1 day`/`+1 week`/`+1 month`/`+N days`).
- `ReminderAlarmReceiver` (`receiver/`): fires when the alarm goes off, shows the notification (with "Mark done"/"Snooze 1 day" actions), and — for a recurring reminder — reschedules the next occurrence.
- `ReminderActionReceiver` (`receiver/`): handles the two notification actions without opening the app.
- `BootCompletedReceiver` (`receiver/`): listens for `ACTION_BOOT_COMPLETED` and re-registers every enabled reminder, since `AlarmManager` alarms do not survive a reboot — unlike iOS's `UNUserNotificationCenter`, which persists scheduled notifications across restarts automatically. This is a platform difference the Android port has to actively compensate for.

### 6.2 Known limitations

- Same `CUSTOM`-interval caveat as iOS: a "every N days" reminder is computed from the _previous fire time_, not the originally picked date, since there is no platform API for "start on this date, then repeat every N days" (Android's own inexact-repeating alarms have the same limitation `UNTimeIntervalNotificationTrigger` has on iOS).
- `MONTHLY` uses `Calendar.MONTH + 1`, which rolls over cleanly at month-end (e.g. Jan 31 → Mar 3 in a non-leap context) rather than iOS's `UNCalendarNotificationTrigger`, which simply skips a month lacking that day-of-month. This is a deliberate, documented behavior difference — flagged for anyone comparing outputs across platforms.
- No schema versioning on `Reminder`, same as `codes.json` itself.

---

## 7. Theming

- **Brand palette**: navy (`rgb(0,0,102)`) and cyan (`#09C`, the default accent) are defined as theme tokens, mirroring the iOS app's `Color.brandNavy`/`.brandCyan`.
- `SettingsDataStore` persists the user's chosen accent color as a hex string, theme mode (system/light/dark), and default launch tab — read by `MainActivity` and applied via `QvacellTheme`.
- Material 3 dynamic color is not used by default, to keep the brand palette consistent across devices — same reasoning as the iOS app's fixed `AccentColor` default.

---

## 8. Platform Constraints

These shape the UX and are not fixable in code:

- Android's `ACTION_DIAL` always opens the system dialer for user confirmation before a call is placed — by default the app doesn't dial silently (matches iOS's `tel://` confirmation behavior), unless the user opts into "Marcar Directamente" (§9), which trades that confirmation step for `ACTION_CALL` and the `CALL_PHONE` permission.
- Interactive multi-step USSD menus (`*133#`, `*234#`) surface through the carrier's own dialer session; there is no in-app fallback for reading that response beyond dialing again.
- Emulators without a real telephony/SIM stack cannot place calls; testing USSD dialing requires a physical device with a Cubacel SIM.
- Some OEM/carrier dialer apps may intercept `ACTION_DIAL` intents before they reach the modem — outside this app's control.

---

## 9. Notable Constraints & Trade-offs (for future contributors)

- **No dependency injection / testability seams**: services are constructed directly (typically via `remember { }` in a composable) — there is no Hilt/Koin graph. Acceptable given the app's size, mirroring the iOS app's equally minimal-DI approach.
- **Silent failure on decode errors**: a malformed `codes.json` or `wifi_navigation_rooms.json` would throw during `CatalogRepository` init — acceptable only because both files are bundled and never user-supplied.
- **No data migrations**: `codes.json` has a `version` field that nothing currently reads; adding a new optional field to `UssdCode` is safe, but renaming/retyping an existing field will break decoding for the exact build that ships it.
- **`codes.json`/`wifi_navigation_rooms.json` are compiled-in**: adding a new code, category, or WiFi room requires a new app build and Play Store review — there is no remote-config or in-app update path.

---

## 10. Extension Points

- **New code, group, or category** → edit `codes.json` only; UI adapts automatically for a new code/group. A whole new top-level category needs a matching tab wired into `NavGraph.kt`/`QvacellNavHost.kt` (§4 — the tab set is not a generic loop).
- **Search** → `CategoryListScreen`/`SmsServicesScreen` would need local as-you-type filtering added; no `CatalogRepository` search API exists yet.
- **Favorites / recents** → `TransferPinStore` (§12) is already a precedent for small encrypted local state beside the read-only catalog; a DataStore-backed store would work the same way for something non-secret.
- **Remote catalog updates** → replace `CatalogRepository`'s asset-loading with a cached-remote strategy; the `version` field in the JSON exists for this.
- **Localization** — UI copy is Spanish (not English); catalog `title`/`details` would move to localized variants keyed by the same `id`.

---

## 11. Caller ID (`CallScreeningService`, `*99` collect-call identification)

### 11.1 Why this exists

ETECSA's `*99` collect-call service does **not** withhold the caller's number the way `#31#` (anonymous) does — it wraps it. Dialing `*99{number}` makes the call arrive on the other end with a caller ID string of the form:

```
99 + "53" (country code) + {8-digit local number} + 99
```

e.g. a call to `51234567` shows up as `99535123456799` (14 digits) instead of the real number. The stock Phone app has no idea what to do with that, so the incoming call just shows a meaningless 14-digit string. Since the real digits genuinely reach the device (unlike a truly anonymous call, which never transmits them), it's possible to reverse the wrapping and show the real contact's name instead.

### 11.2 How it works

```
ContactsRepository.loadContacts()
  reads ContactsContract
  → List<DeviceContact>
        │
        ▼
ContactsRepository.syncWrappedCallers(...)
  WrappedCaller.wrappedNumber(localNumber)
  "99" + "53" + localNumber + "99"
        │
        ▼
QvacellDatabase.wrappedCallerDao()  ──▶  Room table "wrapped_callers"
  (rebuilt on every contacts fetch)        (wrappedNumber → name)
                                                    │
                                                    ▼
                                       CallerIdScreeningService.onScreenCall(...)
                                         unwraps the incoming number,
                                         looks it up in wrapped_callers,
                                         posts a heads-up notification
                                         with the resolved name
```

- **`ContactsRepository.syncWrappedCallers`** rebuilds the entire `wrapped_callers` table from the device's Contacts every time `ContactsListScreen` fetches them — same "no separate manual sync button" behavior as the iOS app's `ContactsService.fetch()`.
- **`CallerIdScreeningService`** (`service/CallerIdScreeningService.kt`) extends `android.telecom.CallScreeningService`. On every incoming call it unwraps the number if it matches the `99…99` pattern, looks it up against `wrapped_callers`, and — if found — posts a heads-up notification naming the caller. It **never blocks or rejects any call**; screening decisions always allow the call through.
- The service is declared with a `CallScreeningService` intent filter and requires the user to grant `RoleManager.ROLE_CALL_SCREENING` once, via Ajustes › Acerca de › Identificador de Llamadas — a one-time manual step, no API lets an app enable it for itself.
- The same Ajustes section also hosts an unrelated-but-adjacent toggle, **"Marcar Directamente"** (`DialService.isDirectDialEnabled`/`setDirectDialEnabled`, backed by a plain `SharedPreferences` flag, not the `SettingsDataStore` Preferences DataStore the rest of Ajustes uses — a synchronous read was simpler than threading a `Flow` through every `DialService.dial()` call site): off by default (`ACTION_DIAL`, dialer confirmation required, no permission needed); on, every dial in the app switches to `ACTION_CALL` (immediate, no confirmation) as long as `CALL_PHONE` is granted — turning it on requests that permission, and denial leaves the setting off rather than half-enabled.

### 11.3 Real constraints (not fixable in code) — the platform gap vs. iOS

- **Cannot inject a custom name into the system in-call UI.** This is the central difference from the iOS app's CallKit Call Directory Extension, which _can_ relabel the system's own incoming-call screen. Android's `CallScreeningService` API gives a screening app no mechanism to change the displayed caller name unless that app is also the user's **default dialer app** — a much larger commitment (replacing core phone UI, `InCallService`, etc.) that this project deliberately does not take on for one feature. The heads-up notification is the best-effort substitute: it doesn't replace the incoming-call screen, but it does surface the real name promptly.
- **Only labels contacts already in the address book** — a `*99` call from an unknown number still resolves to nothing, same limitation as the iOS app.
- **The user must grant the call-screening role once, manually** — Ajustes del sistema will show a role-holder picker; only one app can hold `ROLE_CALL_SCREENING` at a time, so enabling this may replace another call-screening/spam-blocking app the user already had.
- **Only testable on a physical device with a real telephony stack.**
- **A truly anonymous call (`#31#`) can never be identified this way** — the network never transmits the number at all in that case, so there is nothing to wrap or unwrap.

---

## 12. Transfer PIN Store

`TransferPinStore` (`service/TransferPinStore.kt`) persists the user's ETECSA transfer PIN using `EncryptedSharedPreferences` (`androidx.security.crypto`), backed by a `MasterKey` in the Android Keystore (AES256-GCM), with AES256-SIV key encryption and AES256-GCM value encryption — the Android equivalent of the iOS app's Keychain-backed store. `.save(_)`/`.load()`/`.delete()` are thin wrappers over the encrypted `SharedPreferences`.

Consumers: the Home Transferir flow and the offline Database search's PIN field both call `TransferPinStore.load()` to prefill themselves instead of asking the user to retype the PIN every time; the Transfer PIN settings screen (Ajustes › Cuenta) is the only place that writes to it.

---

## 13. Offline Database Search / DirectoryDatabase (`Buscar en Database`)

`DirectoryDatabase` (`service/DirectoryDatabase.kt`) is a read-only SQLite reader (`androidx.sqlite` / `SupportSQLiteOpenHelper`, no ORM) powering the "Buscar en Database" screen over a phone-directory dump the _user_ supplies via Android's Storage Access Framework (`ACTION_OPEN_DOCUMENT`) — the app neither bundles nor downloads it, same as the iOS app (whose download-from-URL UI is disabled for legal reasons; this port never implemented that path at all).

The imported file is copied into app-internal storage (`context.filesDir/directory.db`) and identified by querying `sqlite_master` for table names rather than trusting the filename:

- **v1**: a single `contacts(number, name, is_mobile)` table.
- **v2**: split into `movil(number, name)` and `fix(number, name)` tables — no `is_mobile` column, since which table a row came from _is_ the line type.

Notable implementation details:

- Opened via `FrameworkSQLiteOpenHelperFactory` against the copied file path directly — no `?immutable=1` URI trick is needed on Android the way it is on iOS's sandboxed SQLite, since the file lives in normal app-private storage rather than a document-provider-backed path.
- `search(prefix)` uses `LIKE 'prefix%'`, which is index-friendly on `number`; a name-only search is not implemented on Android (matching the iOS app, where it exists in the service layer but is disabled in the UI for privacy reasons — this port skipped building it at all rather than shipping a disabled code path).
- Results are capped at 200 rows per query as a basic safety bound against dumps with millions of rows.
- This screen is hidden by default and unlocked via the same 5-tap-on-version-string easter egg as iOS, gated by a flag in `SettingsDataStore`.

---

## 14. Cross-Platform Catalog Sync Check

`codes.json` and `wifi_navigation_rooms.json` are maintained as two separate files — one per platform repo — rather than a shared package, so nothing enforces at build time that this repo's copy still matches [qvacell-ios](https://github.com/albertolicea00/Qvacell-ios)'s. [`.github/workflows/cross-platform-sync-check.yml`](.github/workflows/cross-platform-sync-check.yml) is a CI guard against that drift.

### 14.1 Trigger and flow

Runs on every push to `main` touching either JSON file (plus `workflow_dispatch` for a manual run). Two independent jobs, `check-codes` and `check-wifi-rooms`, each:

1. Fetch the counterpart file from the iOS repo's raw GitHub URL (`raw.githubusercontent.com/albertolicea00/Qvacell-ios/main/...`) — no auth needed, since both repos are public.
2. Run a comparison script (`.github/scripts/check-catalog-sync.mjs` or `check-wifi-catalog-sync.mjs`) against the local copy.
3. On drift: upload the diff as a build artifact, open (or update, if one is already open) an issue **on the iOS repo** — not this one — labeled `catalog-sync`, and fail the job so it shows red in Actions.
4. On no drift: exit clean.

Opening the issue on the _other_ repo (rather than this one) requires the `CROSS_REPO_TOKEN` secret — a PAT with `Issues: write` on `Qvacell-ios`. Without that secret configured, the job still detects and reports drift (failed run + artifact), it just can't open the cross-repo issue.

### 14.2 What "structure" means for `codes.json`

Only these fields are compared, per code: `id`, `code` (the dial string), `type`, `requiresInput`, `inputPlaceholder`, `noConfirmCode`, `smsBody`, `options`, `isSubscription`, `variants`, plus which category id and group name it lives under (and the overall category order). Deliberately **ignored**: `icon` (SF Symbol names on iOS vs. Material icon identifiers on this repo's copy, §3), `price`, `compact`, `showsNumber`, `title`, `details` — all presentation/wording, not behavior. This means editing a title's phrasing or swapping an icon never trips the check; changing a dial string, adding/removing a code, or moving one to a different category does.

### 14.3 What's compared for `wifi_navigation_rooms.json`

This file has no cosmetic fields — every field is data (province name, room name/address/positions, hotspot municipality/spots) — so the check compares it in full, per province, rather than filtering a subset.

### 14.4 Relationship to `wifi-rooms-sync-check.yml` (iOS repo)

The iOS repo also has a separate, unrelated workflow (`wifi-rooms-sync-check.yml`) that checks the bundled WiFi directory against ETECSA's _own_ website for source-data drift. This cross-platform check answers a different question — "do the two apps still agree with each other" — not "is the data still accurate against ETECSA."
