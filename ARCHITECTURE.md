# Qvacell :: Architecture

For contribution workflow see [CONTRIBUTING.md](CONTRIBUTING.md).

This document describes the technical architecture of Qvacell for Android, a native Kotlin/Jetpack Compose app that lets users dial ETECSA (Cubacel) USSD service codes (`*222#` style dial strings) without needing to remember them.

It is an Android port of [qvacell-ios](https://github.com/albertolicea00/qvacell-ios). App features remain local and require no Qvacell backend: the catalog is bundled, contacts are read on-device. Firebase Analytics is the exception: it sends app usage to Firebase (§19); contacts, phone numbers, USSD codes/responses, and the transfer PIN are not included in custom telemetry.

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
               Home, Compras, Options)
        ┌──────────┬──────────┬──────────┬──────────┐
        ▼          ▼          ▼          ▼          ▼
   CategoryListScreen (×2, helplines/purchase)   OptionsScreen
   (per-category code list · dials/prompts)      (nested routes)
        │                                              │
        ▼                                              ▼
   dial/dialog/SMS-intent,                    pushes: Reminders, SMS
   depending on UssdCode.type                 services, WiFi rooms,
                                               Directory search (hidden),
                                               Transfer PIN
                                               role request, Help
        │
        ▼
   DialService (ACTION_DIAL / ACTION_SENDTO)


          CatalogRepository ── decodes ──▶ UssdCatalog
                  │
                  └── loads assets/codes.json (bundled, read-only)
```

There is no MVVM view-model layer and no cross-screen navigation state machine beyond Compose Navigation's own back stack. `CatalogRepository` (the catalog) and `SettingsDataStore` (theme/default tab/accent color) are the two pieces of app-wide state; `ContactsRepository`, `TransferPinStore`, `ReminderRepository`, and `DirectoryDatabase` are created where they're used instead of injected globally — there is no dependency-injection framework (no Hilt/Koin), matching the iOS app's equally minimal-DI approach. State flows one way — repositories/stores (read-only or self-contained) → composables. There is no unified "code detail" screen — each `UssdCode.type` (`USSD`, `CALL`, `SMS`) drives a different tap behavior directly (dial, dialog-for-input-then-dial, or open an SMS intent). Persisted state includes `SettingsDataStore` (Preferences DataStore: theme mode, default tab, accent color hex, debug-search unlock flag, stale-days threshold §15.10), a Room database (`reminders`, `dashboard_value_snapshots`, `source_anchors` tables), and the transfer PIN in `EncryptedSharedPreferences` (`TransferPinStore`, §12).

---

## 2. Source Layout

| File/Package                                       | Responsibility                                                                                                                                                                                                                                                                                     |
| -------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `QvacellApplication.kt`                            | `Application` subclass. Creates the `reminders` notification channel at process start.                                                                                                                                                                                            |
| `MainActivity.kt`                                  | Single-`Activity` host. Requests `POST_NOTIFICATIONS` on API 33+ at launch, reads the persisted theme from `SettingsDataStore` and applies it, hosts `QvacellNavHost`.                                                                                                                             |
| `model/Models.kt`                                  | `@Serializable` catalog models (`UssdCatalog`, `UssdCategory`, `UssdCodeGroup`, `UssdCode`, `UssdActionType`, `SmsVariant`) and the WiFi navigation-room models (`WifiProvince`, `WifiRoom`, `WifiHotspotGroup`).                                                                                  |
| `model/CubanPhoneNumber.kt`                        | The one place that validates/normalizes a Cuban mobile number — ported 1:1 from the iOS app's `CubanPhoneNumber`.                                                                                                                                                                                  |
| `model/Reminder.kt`                                | Room `@Entity` for a scheduled reminder, plus `ReminderRecurrence` (`NONE`/`DAILY`/`WEEKLY`/`MONTHLY`/`CUSTOM`).                                                                                                                                                                                   |
| `data/CatalogRepository.kt`                        | Loads/decodes `assets/codes.json` and `assets/wifi_navigation_rooms.json` via `kotlinx.serialization`.                                                                                                                                                                                             |
| `data/QvacellDatabase.kt`                          | Room database (`reminders` tables).                                                                                                                                                                                                                                             |
| `data/ReminderDao.kt` | Room DAOs.                                                                                                                                                                                                                                                                                         |
| `data/Converters.kt`                               | Room `TypeConverter`s (e.g. `ReminderRecurrence` enum ↔ `String`).                                                                                                                                                                                                                                 |
| `data/SettingsDataStore.kt`                        | Preferences DataStore: theme mode, default launch tab, accent color hex, hidden debug-search unlock flag, global SIM slot preference (§16), stale-days threshold for dashboard timestamp color coding (§15.10).                                                                                    |
| `service/DialService.kt`                           | `dial()` builds/opens `tel:` URIs via `ACTION_DIAL`; `dialDirect()` uses `ACTION_CALL`. Both accept an optional `simSlot` parameter for dual-SIM routing (§16) — resolves `PhoneAccountHandle` via `TelecomManager.callCapablePhoneAccounts`. Also SMS intents via `ACTION_SENDTO` and Google Maps search URLs via `ACTION_VIEW`. |
| `service/CellularMonitor.kt`                       | Wraps `TelephonyManager` to expose the current network type (5G/LTE/3G/2G/none) as a `StateFlow<String>`, for an optional weak/no-signal warning banner.                                                                                                                                           |
| `service/ContactsRepository.kt`                    | Queries `ContactsContract` for device contacts (including phone type labels), normalizes Cuban numbers via `CubanPhoneNumber`, provides `flattenToEntries()` to expand multi-number contacts into per-number `ContactEntry` rows (§17).        |
| `service/SimUtils.kt`                              | Shared dual-SIM utilities: `getActiveSubscriptions()`, `getPhoneNumber()` (API 33+ aware), `simLabel()`, `simColor()` — used by the SIM selector chip, options screen, and widget configuration (§16).                                                                                             |
| `service/TransferPinStore.kt`                      | `EncryptedSharedPreferences`-backed transfer PIN store (§12) — the Android equivalent of the iOS app's Keychain-backed `TransferPinStore`.                                                                                                                                                         |
| `service/ReminderRepository.kt`                    | CRUD over `Reminder` via `ReminderDao`, as a `Flow`.                                                                                                                                                                                                                                               |
| `service/ReminderScheduler.kt`                     | Schedules/cancels `AlarmManager.setExactAndAllowWhileIdle` alarms per reminder; computes the next occurrence for recurring reminders (§6).                                                                                                                                                         |
| `service/DirectoryDatabase.kt`                     | Read-only SQLite reverse-lookup over a user-imported `.db` file (§13) — the Android equivalent of the iOS app's `DirectoryDatabase`.                                                                                                                                                               |
| `receiver/ReminderAlarmReceiver.kt`                | `BroadcastReceiver` fired by `AlarmManager`; shows the reminder notification and reschedules if recurring.                                                                                                                                                                                         |
| `receiver/ReminderActionReceiver.kt`               | Handles the "Mark done" / "Snooze 1 day" notification actions.                                                                                                                                                                                                                                     |
| `receiver/BootCompletedReceiver.kt`                | Re-registers all enabled reminders after `ACTION_BOOT_COMPLETED`, since `AlarmManager` alarms are cleared on reboot (§6) — a correctness step iOS local notifications don't need.                                                                                                                  |
| `ui/AndroidIconResolver.kt`                        | Maps Android Material icon identifiers in this repo's `codes.json` (e.g. `"Filled.Home"`) to `ImageVector` references directly, without reflection; falls back to a generic circle for null or unknown identifiers. Explicit references let R8 retain used icons while removing unused ones (§20). |
| `ui/components/`                                   | Reusable composables: `CodeRow`, `ContactRow` (per-number entry with optional type label), `ContactOptionsSheet`, `TransferBottomSheet`, `DirectoryEntryRow`, `ConnectionBanner` (includes `hasCellularService()` signal check), `SimSelectorChip` (§16), `SearchableTopAppBar` (with `extraActions` slot), `CodeActionHandler`, and dashboard cards — `MainBalanceCard`, `DataUsageCard`, `NationalBonusCard`, `VoiceSmsRow`, `RechargeLimitCard`, `ConsultCardsRow`, plus `TimeAgoText`/`LocalStaleDaysThreshold` (§15.10). |
| `ui/navigation/NavGraph.kt`                        | `BottomTab` sealed class (the 5 fixed tabs) and `Routes` (nested route constants for screens pushed from Options).                                                                                                                                                                                 |
| `ui/navigation/QvacellNavHost.kt`                  | Compose `NavHost` wiring bottom-tab routes and nested routes together.                                                                                                                                                                                                                             |
| `ui/screens/`                                      | `CategoryListScreen` (Ayuda/Compras), `ContactsListScreen` (per-number flat list, §17), `HomeQuickActionsScreen`, `ManualDashboardContent`/`DynamicDashboardContent` (live dashboard with signal gating, pull-to-refresh, `LoadingCardBorder` animation — §15.9–§15.13), `ReminderListScreen`/`ReminderEditScreen`, `FriendsPlanManageScreen`, `DirectorySearchScreen`, `WifiRoomsScreen`, `OptionsScreen` (includes SIM selector, §16), `HelpScreen`, `SmsServicesScreen`. |
| `widget/`                                          | `QuickActionsWidget` (AppWidgetProvider), `QuickActionsWidgetConfigure` (config activity with code + SIM picker), `WidgetPrefs` (per-widget SharedPreferences: code ID, SIM slot, icon shape, colors), `DialTrampolineActivity` (reads per-widget SIM and dials via `DialService`).                |
| `app/src/main/assets/codes.json`                   | Static, bundled dataset: version, carrier, categories → groups → codes, each with its dial string and presentation metadata (§3). Catalog structure is kept in sync with iOS; platform-specific presentation values such as icons may differ. |
| `app/src/main/assets/wifi_navigation_rooms.json`   | Static, bundled dataset: one entry per province with its navigation rooms and free WiFi hotspots. Byte-identical to the iOS app's copy.                                                                                                                                                            |

No separate networking layer or dependency-injection container exists — the `service/` package _is_ the service layer, and each service is instantiated where it's needed (typically inside a `remember { }` in the composable that uses it).

---

## 3. Configuration Data: `codes.json`

`codes.json` is the single source of truth for **what USSD/call/SMS codes exist** and is treated as read-only, bundled data. Its catalog structure is kept in sync with iOS; platform-specific presentation values such as `icon` may differ (iOS uses SF Symbols, Android uses Material icon identifiers). It decodes into:

```
UssdCatalog
 ├─ version, carrier
 └─ categories: [UssdCategory]
     ├─ id, name, icon (Android Material identifier, e.g. `"Filled.Home"` — resolved via AndroidIconResolver.kt; iOS uses SF Symbols)
     └─ groups: [UssdCodeGroup]        (a named sub-heading, or nil-named for no header)
         ├─ name: String?
         └─ codes: [UssdCode]
             ├─ id, code, title, details
             ├─ icon (Android Material identifier, price, compact, showsNumber — optional presentation hints; icon may differ from iOS's SF Symbol)
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

`QvacellNavHost` hosts a bottom navigation bar with 5 explicit tabs — Ayuda, Contactos, Home, Compras, Options (`BottomTab` in `NavGraph.kt`) — not a generic loop over every catalog category (the tab order is fixed on purpose: Home sits in the middle). Ayuda and Compras each host a `CategoryListScreen` bound to the matching `UssdCategory` (`helplines`/`purchase`); adding a _code or group_ to either category in `codes.json` updates that tab automatically, but adding a whole new top-level category does **not** grow the bottom bar — the tab set itself is fixed, same constraint as iOS.

Tapping a row in `CategoryListScreen` dials/prompts/sends an SMS intent directly depending on `UssdCode.type` and `requiresInput`, via the shared `CodeActionHandler` — there is no single "code detail" screen type.

`OptionsScreen` pushes nested routes (`Routes` in `NavGraph.kt`) for Reminders, SMS services, WiFi rooms, Directory search (hidden by default), Transfer PIN management, and Help — same structure as the iOS app's `SettingsView` push destinations, minus Siri/Shortcuts (iOS-only, not ported).

Home starts a fresh session with the manual dashboard (Marcación Rápida). Its top bar switches between manual and dynamic; `rememberSaveable` retains this choice in the Home navigation entry when opening another destination, returning with Back, or recreating the activity. The store flavor shows an availability dialog when dynamic is selected. Onboarding does not ask for a dashboard type, and Options exposes settings for each dashboard without a persistent type selector. An older saved `dashboard_mode` preference is ignored.

Home uses one overflow menu anchored to its three-dot button in both dashboard modes. Its common actions remain in the same order: transfer balance, recharge balance, manage Friends Plan, and reminders. Transfer and recharge appear only when their corresponding manual dashboard sections are hidden (`transferir` / `recargar` in `manualHiddenSections`); both modes use the same visibility rule. Events appears in both modes only in the `unlocked` flavor. Dashboard Options and Help are appended only while the dynamic dashboard is active. The menu closes before opening another screen, a sheet, or changing dashboard mode.

The Options root, Friends Plan management, transfer PIN management, and offline database search expose the shared `SimSelectorIcon` in their top bars. It appears when multiple active SIMs are available and preserves the configured SIM appearance and switch behavior. Selection updates the shared `SettingsDataStore` preference; calls and USSD actions continue through `DialService`, which reads that selected slot when attaching the phone account to each intent.

---

## 5. Code Execution Path

There is no single "code detail" screen — what a tap does depends on `UssdCode.type`, handled by `CodeActionHandler`:

1. **Row tap, no input required** (`requiresInput == false`): dials/sends immediately — see steps 3/4 below for what that means per type.
2. **Row tap, input required** (`requiresInput == true`): a dialog collects a text value and disables the confirm button until it is non-empty; `UssdCode.resolvedCode(input)` (or `resolvedSmsBody(input)` for SMS codes) substitutes the `{input}` placeholder.
3. **`USSD`/`CALL`**: outside Compras, `DialService.dial(context, code)` URI-encodes the code, builds a `tel:` URI, and starts an `ACTION_DIAL` intent — Android's system dialer opens with the code prefilled, requiring the same explicit user confirmation `tel://` gets on iOS. Compras codes are the one exception: `CodeActionHandler` always shows an in-app "Confirmar compra" bottom sheet first (title, price, details, and the exact dial string that will run), and only on "Aceptar" does it call `DialService.dialDirect()` (`ACTION_CALL`, places the call immediately, no dialer step) — requesting `CALL_PHONE` at that point if not already granted, and falling back to the normal `ACTION_DIAL` path if the user denies it.
4. **`SMS`**: `DialService.sendSms(context, number, body)` opens the device's SMS app pre-filled via `ACTION_SENDTO` with `smsto:` + the code as the recipient and the resolved `smsBody` as the message — never sent silently, same one-more-tap-to-confirm shape as a dial. Codes with `options` or `variants` show a picker first.
5. **Quick Purchase, no confirmation** (opt-in, Compras only): when the "Acción sin Confirmación" setting is on and the code has a `noConfirmCode`, that string is dialed instead of `code` — it auto-selects ETECSA's own "¿Confirma su compra? 1. Sí" step in one dial instead of stopping there. See README § Direct dial vs. confirmation.

Separately, the Home Transferir flow and the offline Database search prefill a PIN field from `TransferPinStore` (§12), but that's local to those specific screens, not a general mechanism.

---

## 5b. Launcher Shortcuts

Static shortcuts declared in `res/xml/shortcuts.xml`, referenced from `AndroidManifest.xml` via `android.app.shortcuts` meta-data. Each fires `ACTION_DIAL_USSD` with a `ussd_code` extra, handled by `MainActivity`.

| Order | ID | USSD | Icon drawable | Label (ES) |
|---|---|---|---|---|
| 1 | `saldo_corporativo` | `*111#` | `ic_shortcut_corporate` (account_balance) | Saldo Corporativo |
| 2 | `consulta_saldo` | `*222#` | `ic_shortcut_balance` (account_balance_wallet) | Saldo Personal |
| 3 | `bonos_planes` | `*222*266#` | `ic_shortcut_plans` (data_usage) | Bonos y Planes |
| 4 | `comprar_plan` | `*133#` | `ic_shortcut_purchase` (shopping_cart) | Comprar Plan |
| 5 | `llamar_cobrar` | `*99` | `ic_shortcut_call` (phone) | Llamar *99 |
| 6 | `llamar_oculto` | `#31#` | `ic_shortcut_hidden` (visibility_off) | Llamar Oculto |

All icon drawables use tint `#0099CC` (app primary cyan) with white glyph fill. Most launchers show ≤ 4 shortcuts; order in the XML controls priority. Labels are localized in `values/strings.xml` (ES) and `values-en/strings.xml` (EN).

---

## 6. Reminders & Local Notifications

Options › Utilidades › Recordatorios schedules local notifications for a purchase/recharge/transfer the user needs to make — entirely on-device, with no push infrastructure or Qvacell backend. Firebase telemetry (§19) is independent of reminder delivery.

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

- Android's `ACTION_DIAL` always opens the system dialer for user confirmation before a call is placed — outside Compras the app never dials silently (matches iOS's `tel://` confirmation behavior). Compras codes trade the dialer's confirmation for an in-app one instead (§9) — the phone call itself still happens via `ACTION_CALL`/`CALL_PHONE`, just confirmed inside the app rather than by the system dialer.
- Interactive multi-step USSD menus (`*133#`, `*234#`) surface through the carrier's own dialer session; there is no in-app fallback for reading that response beyond dialing again.
- Emulators without a real telephony/SIM stack cannot place calls; testing USSD dialing requires a physical device with a Cubacel SIM.
- Some OEM/carrier dialer apps may intercept `ACTION_DIAL` intents before they reach the modem — outside this app's control.

---

## 9. Notable Constraints & Trade-offs (for future contributors)

- **No dependency injection / testability seams**: services are constructed directly (typically via `remember { }` in a composable) — there is no Hilt/Koin graph. Acceptable given the app's size, mirroring the iOS app's equally minimal-DI approach.
- **Silent failure on decode errors**: a malformed `codes.json` or `wifi_navigation_rooms.json` would throw during `CatalogRepository` init — acceptable only because both files are bundled and never user-supplied.
- **No data migrations**: `codes.json` has a `version` field that nothing currently reads; adding a new optional field to `UssdCode` is safe, but renaming/retyping an existing field will break decoding for the exact build that ships it.
- **`codes.json`/`wifi_navigation_rooms.json` are compiled-in**: adding a new code, category, or WiFi room requires a new app build and Play Store review — there is no remote-config or in-app update path.
- **Android catalog icons have an explicit resolver**: when adding an icon identifier to this repo's `codes.json`, add the matching `Icons.Filled.*` or `Icons.AutoMirrored.Filled.*` case to `AndroidIconResolver.kt`. Unknown or missing identifiers fall back to `Circle`; `AndroidIconResolverTest` checks every catalog icon and catches omissions.

---

## 10. Extension Points

- **New code, group, or category** → edit `codes.json`; when it introduces a new Android icon, also add it to `AndroidIconResolver.kt` (§20). UI adapts automatically for a new code/group. A whole new top-level category needs a matching tab wired into `NavGraph.kt`/`QvacellNavHost.kt` (§4 — the tab set is not a generic loop).
- **Search** → `CategoryListScreen`/`SmsServicesScreen` would need local as-you-type filtering added; no `CatalogRepository` search API exists yet.
- **Favorites / recents** → `TransferPinStore` (§12) is already a precedent for small encrypted local state beside the read-only catalog; a DataStore-backed store would work the same way for something non-secret.
- **Remote catalog updates** → replace `CatalogRepository`'s asset-loading with a cached-remote strategy; the `version` field in the JSON exists for this.
- **Localization** — UI copy is Spanish (not English); catalog `title`/`details` would move to localized variants keyed by the same `id`.

---

## 11. Caller Identification — Removed

Qvacell previously included an implementation for identifying incoming `*99` collect calls. This feature was removed to avoid maintaining duplicate functionality across Qvacell and [LlamaCon99](https://github.com/albertolicea00/LlamaCon99). Incoming-call identification is now exclusively the responsibility of LlamaCon99 and is no longer part of Qvacell's scope.

The removed implementation included the call-screening service, contact synchronization, identification notifications, and the settings entry. The legacy Room `wrapped_callers` table/entity remains only to preserve compatibility with existing installed databases; it no longer has an active identification service or synchronization path.

Outgoing `*99` collect-call dialing remains available in Qvacell. This decision concerns incoming-call identification only.

---

## 12. Transfer PIN Store

`TransferPinStore` (`service/TransferPinStore.kt`) persists the user's ETECSA transfer PIN using `EncryptedSharedPreferences` (`androidx.security.crypto`), backed by a `MasterKey` in the Android Keystore (AES256-GCM), with AES256-SIV key encryption and AES256-GCM value encryption — the Android equivalent of the iOS app's Keychain-backed store. `.save(_)`/`.load()`/`.delete()` are thin wrappers over the encrypted `SharedPreferences`.

Consumers: the Home Transferir flow and the offline Database search's PIN field both call `TransferPinStore.load()` to prefill themselves instead of asking the user to retype the PIN every time; the Transfer PIN settings screen (Options › Cuenta) is the only place that writes to it.

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

---

## 15. Dashboard Dynamic Data Pipeline

### 15.1 Why this exists

The Home dashboard cards (`MainBalanceCard`, `RechargeLimitCard`, `DataUsageCard`, `NationalBonusCard`, `VoiceSmsRow`, in `ui/components/HomeDashboardSection.kt`) started as pure display components fed 100% hardcoded placeholder strings from `HomeQuickActionsScreen.kt` — there was no data source at all. This pipeline replaces those placeholders with real, persisted, dynamically-updated values, sourced from three places: (1) parsing a USSD dial response for a dashboard-relevant code, (2) parsing ETECSA SMS messages (both new incoming and the user's existing SMS history), and (3) a background estimate that fills the gap between real readings by measuring calls/minutes/SMS sent since the last confirmed anchor (Cuban numbers only). All history is kept forever (never pruned) for future in-app trend graphs — this is a deliberate design constraint, not an oversight.

Because two of those three sources need genuinely sensitive Android permissions (`READ_SMS`, `RECEIVE_SMS`, `READ_CALL_LOG`), and because a companion app that isn't the user's default SMS/dialer handler is a real Play Store restricted-permissions review risk, the whole capture layer is split out behind product flavors (§15.6) rather than shipped unconditionally.

### 15.2 Data model (Room)

Two new tables, added in `data/QvacellDatabase.kt` via a hand-written `Migration(1, 2)` (version bumped 1→2, no destructive fallback — existing data is preserved):

- **`dashboard_value_snapshots`** (`model/DashboardValueSnapshot.kt`, `data/DashboardValueSnapshotDao.kt`) — append-only fact table, one row per reading. `fieldType` (a plain string key, see `data/FieldTypes.kt`), `numericValue`/`textValue`/`dateValue` (sparse — only the relevant one is set per field), `unit`, `capturedAt`, `sourceSignalId`, `sourceKind` (`USSD_REAL` / `SMS_REAL` / `ESTIMATED`, `model/SourceKind`). No row-level update/delete is exposed; a single explicit `deleteAll()` supports the user's dashboard-data reset.
- **`source_anchors`** (`model/SourceAnchor.kt`, `data/SourceAnchorDao.kt`) — one row per independent signal (e.g. `ussd:main-balance`, `sms:<parser-id>`), **not** a single global "last known date". `lastSeenAt`/`lastSeenRawText` update whenever raw text arrives for that signal; `lastSuccessfulParseAt`/`lastSuccessfulRawText` only update once a parse actually succeeds. This split is what lets the estimation engine recover a correct anchor even if the user clears their call log or SMS history — the anchor lives in Room, not derived from a live device query with no fallback.

`DashboardValueSnapshotDao.observeAllLatestPerField()` groups by `(fieldType, isReal)`, not `fieldType` alone, and by `MAX(capturedAt)`, not `MAX(id)` — two easy-to-miss correctness details:
- Grouping by `fieldType` alone would return only the single most-recent row overall; if an `ESTIMATED` row happened to be newer than the last `*_REAL` one, the real row would never reach the repository at all, silently breaking "real always wins" (§15.4). Grouping by `(fieldType, isReal)` returns up to two rows per field — latest real *and* latest estimated — so the repository can choose correctly.
- Grouping by `MAX(id)` instead of `MAX(capturedAt)` would break under SMS historical backfill: an old message gets inserted (with a higher `id`) *after* a real-time capture that already exists with an earlier `id`, so `MAX(id)` would wrongly surface the backfilled row as "latest."

### 15.3 Parser contracts (deliberately stubbed)

`parsing/ParseResult.kt` (`Success<T>` / `Unresolved` / `Unrecognized`), plus `UssdParsers`/`SmsParsers` registry objects keyed by catalog code id. **Every parser implementation currently returns `Unresolved`/never-matches** — the real USSD response text and SMS body formats from ETECSA are not yet known; these are placeholders that unblock the surrounding repository/persistence/capture code without guessing at a format that would likely be wrong. Swapping in a real parser later touches exactly one registry entry, nothing else. `UssdParserCoverageTest` guards against catalog/registry drift (a renamed catalog code id silently going unparsed forever) by asserting every no-input `type: "ussd"` entry in `codes.json` has a matching registry key.

### 15.4 Repository & reconciliation

`service/DashboardDataRepository.kt` — constructed the same way every other repository in this app is (`remember { DashboardDataRepository(context) }`, no DI framework, matching §9's established pattern). `observeCurrentValues(): Flow<Map<String, DashboardValueSnapshot>>` is what the dashboard UI will eventually collect from; `recordUssdParse`/`recordSmsBody` are the confirmed-data write paths, `recordEstimate` is a distinctly-named separate write path so a caller can never mistake an estimate for a confirmed reading.

**Reconciliation rule**: a real (`USSD_REAL`/`SMS_REAL`) value always wins over an `ESTIMATED` one, **regardless of which is newer** — an estimate only fills a field that has no real value at all yet. This is a deliberate simplification over "most-recent-wins across both": estimates are heuristic (call-log/SMS-log deltas), so a confirmed reading should never be silently displaced by a guess just because the guess is newer.

`recordSmsBody`'s `capturedAt` parameter must be the SMS's own timestamp (e.g. `Telephony.Sms.DATE`), never wall-clock "now" — a historical backfill import would otherwise collapse all imported history onto the import date, corrupting both the reconciliation comparison above and any future trend graph.

### 15.5 Capture mechanism (flavor-gated, `unlocked` only)

- **`UssdCaptureService`** (`app/src/unlocked/`) — wraps `TelephonyManager.sendUssdRequest` (API 26+) to capture a USSD response's text programmatically, with a 15-second timeout. Dynamic-dashboard cards use `DashboardRefreshService.refreshCode()` and never fall back to direct dialing on failure. The sequential batch refresh stops on a capture failure or timeout; it never dials the remaining codes. Manual-dashboard actions still use `DashboardCapture.captureOrDial()` and preserve their configured dialing behavior. A system or carrier dialog raised by the phone's telephony implementation itself remains outside the app's control.
- **`EtecsaSmsReceiver`**/**`EtecsaSmsFilter`** (`app/src/unlocked/`) — live incoming-SMS capture plus a historical backfill query, filtered by a stub "is this an ETECSA message" heuristic pending real sender/format examples.
- **`EstimationEngine`**/**`EstimationScheduler`**/**`EstimationAlarmReceiver`** (`app/src/unlocked/`) — the background estimate: queries `CallLog.Calls` and (once live) `Telephony.Sms.Sent` since the last confirmed anchor, filtered to Cuban numbers via the existing `model/CubanPhoneNumber.kt` (no new heuristic written — reused what already existed), and writes the delta via `recordEstimate`. Scheduled via `AlarmManager.setExactAndAllowWhileIdle`, mirroring `ReminderScheduler`'s existing pattern (§6) rather than introducing WorkManager as a second background-work paradigm in the same app.
- **Dashboard settings** — "Consulta automática de saldo (experimental)" (silent USSD), "Detectar nuevos SMS" (live ETECSA messages, with call-log estimates when permitted), and "Leer historial de SMS" (one-time inbox import) are independent controls in `DashboardSettingsScreen.kt`, all **default OFF**. The SMS receiver checks the live setting before processing; the history import requests `READ_SMS` separately and does not enable live capture. Existing users who already completed the import retain its enabled state. Silent USSD capture reuses the same `CALL_PHONE` permission already granted for Compras, but its blast radius is now wider (silent background use vs. an explicit in-app confirm-then-call), so it's opt-in behind an explainer rather than silently repurposing an existing grant. The same settings screen offers a confirmed "Eliminar datos recolectados" action; it transactionally clears all dashboard snapshots and raw response anchors across SIMs without disabling capture or revoking permissions. The reliability of `sendUssdRequest` against ETECSA's actual gateway is unverified — this is a real spike/validate-early risk, not just a formality, since some carriers route USSD replies as an OS-level dialog the app's callback never sees.

### 15.6 `store`/`unlocked` flavor split

The entire §15.5 capture layer, and only that layer, lives in flavor-specific source sets — `app/src/main` (both flavors) has the inert Room persistence/parser/repository half, which requests no permissions on its own:

```
app/src/main/AndroidManifest.xml     — shared baseline, none of the new permissions
app/src/store/AndroidManifest.xml    — no additions (Play Store build)
app/src/unlocked/AndroidManifest.xml — adds RECEIVE_SMS, READ_SMS, READ_CALL_LOG,
                                        registers EtecsaSmsReceiver + EstimationAlarmReceiver
app/src/store/.../DashboardCapture.kt    — no-op: always falls back to plain DialService.dial
app/src/unlocked/.../DashboardCapture.kt — real capture-or-dial decision (§15.5)
```

`BuildConfig.DASHBOARD_CAPTURE_ENABLED` (a per-flavor `buildConfigField`) gates whether the two Options toggles render at all — the `store` flavor never shows a UI affordance for a feature it can't perform. See [README.md § Build Flavors & Permissions](README.md#-build-flavors--permissions) for the actual `./gradlew` commands to build/install/test each flavor.

### 15.7 Testing

First-ever test infrastructure for this project (`app/src/test/java/com/qvacell/app/{parsing,data,service}/`) — JUnit4 + Robolectric (in-memory Room) + Turbine for `Flow` assertions. Notable environment caveats hit while setting this up, in case they resurface:
- Robolectric 4.13's bundled ASM cannot parse class files compiled for JDK 25 ("Unsupported class file major version 69") — unit tests are pinned to JDK 17 via a `javaLauncher` toolchain override in `app/build.gradle.kts`'s `tasks.withType<Test>`, independent of whatever JDK invokes Gradle itself.
- `UssdParserCoverageTest` reads `codes.json` directly off disk (`File("src/main/assets/codes.json")`) rather than through Robolectric's `AssetManager` shadow — this test only needs the JSON content, not real Android asset-resolution behavior, so a plain file read sidesteps Robolectric's resource/asset shadow layer entirely for a case that doesn't need it.

Coverage: append-only enforcement at the DAO surface, `MAX(capturedAt)`-not-`MAX(id)` grouping correctness under out-of-order backfill inserts, anchor `lastSeenAt`/`lastSuccessfulParseAt` independence (the regression test for "no single global last-known-date"), real-vs-estimate reconciliation precedence (§15.4), invalid-MMI response classification, Cuban mobile-number normalization, provisional ETECSA SMS filtering, and recurring-reminder date calculation. Exact OS behavior—telephony routing, provider permissions, alarm delivery, and receiver lifecycle—remains outside these local unit tests; see §21.

### 15.8 Parser Rules

Full parsing rules, verbatim ETECSA response samples, regex patterns, and field mappings live in [`app/src/main/java/com/qvacell/app/parsing/ARCHITECTURE.md`](app/src/main/java/com/qvacell/app/parsing/ARCHITECTURE.md). That document covers both USSD and SMS parsers. This section only tracks implementation status.

**Implemented** (7 of 9 USSD codes): `main-balance` (13 tests), `national-recharge-limit` (12 tests), `voice-balance` (10 tests), `sms-balance` (9 tests), `bonus-usd-plans` (14 tests), `data-plan` (17 tests, including KB balances), and `corp-balance` (5 tests, partial data-only and full corporate snapshots). Total: 80 parser tests.
**Awaiting ETECSA samples**: `tfa` (`*118#`, [#10](https://github.com/albertolicea00/Qvacell-apk/issues/10)), `friends-plan` (`*222*264#`, [#11](https://github.com/albertolicea00/Qvacell-apk/issues/11)) — no response text captured yet, UI wired to manual dial only.
**Stubbed**: all SMS parsers.

---

### 15.9 Dashboard UI — Live Cards & Data States

The dashboard cards (`MainBalanceCard`, `DataUsageCard`, `NationalBonusCard`, `VoiceSmsRow`, `RechargeLimitCard`) in `DynamicDashboardContent.kt` (unlocked flavor) collect live data from `DashboardDataRepository.observeCurrentValues()` and display three distinct states per card:

The corporate balance is one grouped card with voice, SMS, data, and roaming values from the same `*111#` response. Voice and SMS each pair the available amount with a compact consumed-CUP line; data follows, then roaming. Section icons and subtle dividers distinguish the groups, and the main values use larger primary-colored type. A rounded primary button below the card opens the corporate portal externally. TFA and Plan Amigo still use full-width consultation cards below the data cards and show the "UI Próximamente" badge.

1. **Sin consultar** (never fetched) — `hasFetched = false`. Shown when no USSD query has populated the card's fields yet. Cards display a placeholder message ("Actualice el dashboard para ver la información." or "Aún no consultado.") instead of data. `DataUsageCard` also hides its "Tarifa por consumo" section and divider in this state.
2. **Sin plan / No cuenta con...** (fetched, no active plan) — `hasFetched = true`, values are `"--"`. Voice/SMS cards show "Sin plan"; Data card shows "No cuenta con paquetes disponibles"; Bonus card shows "No cuenta con bonos disponibles".
3. **Real values** — `hasFetched = true`, values populated from parsed USSD response.

**`hasFetched` derivation**: `MainBalanceCard` checks `MAIN_BALANCE.capturedAt != null`. Voice, SMS, and Data cards check their own field's `capturedAt` **OR** `MAIN_BALANCE.capturedAt` — because the `MainBalanceParser` (*222#) already extracts `VOICE_MINUTES_REMAINING`, `SMS_COUNT_REMAINING`, and `DATA_DATOS_MB` when those plans exist in the response. If main-balance was fetched and those fields are absent, the user genuinely has no plan — the card correctly shows "Sin plan" immediately without needing a separate USSD query.

**Conditional field hiding**: `MainBalanceCard` hides "Activa hasta" / "Vence el" lines when their values are `"--"` (even after fetching), rather than showing placeholder dashes.

### 15.10 Timestamp Color Coding (`TimeAgoText`)

Every dashboard card displays a `TimeAgoText` composable (`ui/components/HomeDashboardSection.kt`) showing when its data was last updated ("Hace 2 horas", "Hace 3 días", etc.). The text color encodes freshness:

- **Red** (`#E53935`) — `epochMillis == null`, labeled "No actualizado" (never updated).
- **Amber** (`#FB8C00`) — data older than `staleDaysThreshold` days (default 14). The threshold is parameterizable via `SettingsDataStore.staleDaysThreshold` and distributed to all cards through `LocalStaleDaysThreshold` (`compositionLocalOf { 14 }`), provided by `CompositionLocalProvider` in `DynamicDashboardContent`.
- **Gray** (50% alpha `onSurfaceVariant`) — data is fresh (within threshold).

When voice/sms/data cards derive `hasFetched` from `mainBalanceFetched` and have no dedicated query timestamp, `lastUpdatedAt` falls back to `MAIN_BALANCE.capturedAt` so the timestamp reflects when the data was actually determined.

### 15.11 Loading Animation (`LoadingCardBorder`)

`LoadingCardBorder` (`ui/components/LoadingCardBorder.kt`) wraps each dashboard card while the corresponding USSD code is being queried (`DashboardRefreshService.currentCodeId` matches the card's code ID). Voice and SMS use separate wrappers, so only the queried card animates. `DashboardSettingsScreen` persists `dashboard_loading_style` in `SettingsDataStore` and `DynamicDashboardContent` supplies it through `LocalLoadingAnimationStyle`. The default `sweep` retains the horizontal gradient border. `bottom` animates a line from the center toward both bottom corners; `perimeter` moves a single segment around the rounded outline in one direction; `skeleton` hides the card content temporarily and draws shimmering placeholder bars. All styles animate only during an active query. The border stroke is 3dp with a 20dp corner radius matching the card shape.

`DashboardRefreshService.refreshCode()` sets/clears `currentCodeId` around each individual dynamic-dashboard USSD request, so tapping a single card also triggers the loading border for that card alone.

### 15.12 Signal Gating & Warning Banner

Dashboard refresh and individual card queries require cellular service. `hasCellularService()` (`ui/components/ConnectionBanner.kt`) checks `voiceNetworkType` first and falls back to `dataNetworkType` and `ServiceState` where possible. An unknown network type or missing `READ_PHONE_STATE` permission is treated as **unknown**, not proof of no service; this avoids disabling USSD and showing a false warning when mobile data is off or the app cannot read the network type. `DynamicDashboardContent` polls this every 5 seconds via `LaunchedEffect`.

When `!hasSignal`:
- **Pull-to-refresh**: `activePull` is gated on `hasSignal` — pulling down produces zero visual feedback, preventing a misleading refresh gesture.
- **Reload button** (`HomeQuickActionsScreen`): `enabled = !isRefreshing && hasSignal`.
- **Individual card taps**: `requestQuery()` returns early.
- **Signal warning** (`QvacellNavHost`): an amber warning appears immediately above the bottom navigation bar when the dynamic dashboard is visible and service is known to be unavailable. It does not appear on the manual dashboard. The optional `ConnectionBanner` controlled by `showNetworkStatus` remains in the bottom bar.
- **SIM warning** (`QvacellNavHost`): the same amber area shows an informational warning on every screen with bottom navigation when no active SIM is detected or the selected SIM is not Cubacel. Missing phone-state permission and an unknown operator have separate messages. `CubacelStatus` checks the selected subscription's SIM operator (`36801`, [ETECSA MCC/MNC assignment](https://www.itu.int/dms_pub/itu-t/opb/sp/T-SP-E.212B-2016-PDF-E.pdf)), rather than its editable display name or the current roaming network. `rememberCubacelStatus` rechecks every two seconds and on SIM selection changes. SIM warnings take priority over dashboard errors. Outside the dynamic dashboard these warnings are informational and do not disable actions. Only confirmed missing or non-Cubacel SIMs block dynamic-dashboard updates (reload button, pull-to-refresh, and individual card queries). Unknown operators and missing phone-state permission do not introduce an additional block. `DashboardRefreshService` also checks before starting and before each USSD query, covering automatic refresh and SIM removal during a batch. Existing cellular-service checks remain unchanged.


### 15.13 Smart Refresh — Skip Unnecessary USSD Queries

`DashboardRefreshService.refresh()` queries codes sequentially (USSD is serial on the modem). The refresh order is: `main-balance` → `bonus-usd-plans` → `data-plan` → `national-recharge-limit` → `voice-balance` → `sms-balance`.

If any request returns a carrier failure or reaches the 15-second timeout, the refresh aborts immediately, clears the active-code/loading state, and publishes a warning through `refreshFailure`; starting another refresh clears that warning. Individual dynamic-dashboard card queries use the same warning and never launch a dial intent on failure. The service checks cellular availability before starting and before each USSD request, using explicit out-of-service and radio-off states when available; airplane mode is one way this can happen, but ordinary signal loss is handled by the same path. This prevents subsequent batch codes after service is lost, but cannot cancel a request already submitted to Android's telephony service. `QvacellNavHost` shows the failure in the same amber strip immediately above the bottom navigation bar, only while the dynamic dashboard is visible. On failure, when `READ_PHONE_STATE` is granted, the service checks the observed voice network type (or data type if voice is unknown). If it still reports LTE/NR, the warning suggests switching a forced 4G setting to automatic network selection. This is a **heuristic**: Android does not expose a reliable public indicator of the user's forced-network preference, and LTE alone does not prove USSD cannot work. A successful USSD response is accepted even if the device reports LTE.

After the `main-balance` query completes, the service reads the current values from `DashboardDataRepository.getCurrentValues()` and checks whether `VOICE_MINUTES_REMAINING`, `SMS_COUNT_REMAINING`, and `DATA_DATOS_MB` were populated. If a field is absent, the corresponding dedicated query is skipped entirely:
- No `VOICE_MINUTES_REMAINING` → skip `voice-balance` (*222*869*1#)
- No `SMS_COUNT_REMAINING` → skip `sms-balance` (*222*767*1#)
- No `DATA_DATOS_MB` → skip `data-plan` (*222*328#)

This optimization avoids wasting USSD requests (and the ~3–5 second modem round-trip per query) for plans the user doesn't have. The DAO exposes a `suspend fun getLatestPerField()` one-shot query alongside the existing `Flow`-based `observeAllLatestPerField()` to support this synchronous check mid-refresh.

For the selected line's balance code only, a returned text response that names MMI and an invalid-code/connection-problem phrase is classified as a possible line-type mismatch. `MmiErrorClassifier` normalizes accents and case and is covered by positive and negative unit cases. This is a prompt for user confirmation, not proof: wording varies by Android/carrier, while a generic failure callback does not reveal why the network rejected the request.

### 15.14 Navigation — Dashboard Settings Tab Affinity

Dashboard-related nested routes are organized by their parent context:
- `DASHBOARD_SETTINGS`, `DASHBOARD_HELP` → `OPTIONS_NESTED_ROUTES` (highlighted under Options tab with corresponding dynamic icon, since both are configuration and documentation destinations).
- `DASHBOARD_EVENTS` → `HOME_NESTED_ROUTES` (highlighted under Home tab).

This parent context controls the selected bottom tab and its icon; the destinations remain in the shared navigation graph. Opening a destination from either dashboard's overflow menu uses `navigate()` without removing Home from the back stack. Both the toolbar Back button and system Back pop to the previous destination, and Home restores its saved manual/dynamic choice. Opening Help from Dashboard Settings (or Settings from Help) likewise returns to the immediately preceding screen first.

Tapping a bottom tab is a separate action: nested Home/Options screens return to that tab's root when it is present; other tab switches use `popUpTo(validStartRoute)` with state saving and restoration.

### 15.15 Status — done vs. planned

**Done and verified** (both flavors compile, full test suite green, merged-manifest permission split confirmed):
- Room schema, migration, DAOs, append-only enforcement.
- All 6 dashboard USSD parsers implemented with 68 tests (see parsing/ARCHITECTURE.md).
- `DashboardDataRepository` with the real-beats-estimate reconciliation rule, plus `getCurrentValues()` one-shot query.
- Capture services, estimation engine, AlarmManager scheduling (`unlocked` flavor).
- `store`/`unlocked` flavor split, manifest/permission separation, `BuildConfig`-gated Options UI.
- First-ever test suite for this project.
- Dashboard cards wired to `DashboardDataRepository.observeCurrentValues()` with three-state display (§15.9).
- Tap-to-refresh on every dynamic-dashboard card via `DashboardRefreshService.refreshCode`, with optional confirmation dialog (`cardTapMode` setting) and no direct-dial fallback.
- `LoadingCardBorder` animated gradient stroke during USSD queries (§15.11).
- `TimeAgoText` with red/amber/gray color coding and parameterizable stale threshold (§15.10).
- Signal gating: pull-to-refresh, reload button, and card taps disabled when cellular service is known to be absent; warning limited to the dynamic dashboard (§15.12).
- Smart refresh: main-balance response determines which subsequent queries to skip (§15.13).
- Batch refresh aborts on the first USSD failure/timeout and gives LTE-specific guidance when the observed network still reports 4G/5G (§15.13).
- `PullToRefreshBox` (Material3 1.3.1) with custom pull indicator (dual arrows + "Arrastra/Suelta para refrescar" text).

**Awaiting ETECSA samples**:
- 2 consultation-card USSD codes (`tfa` `*118#`, `friends-plan` `*222*264#`) — no response text captured yet, cannot write parsers without real samples. `corp-balance` now has partial real-sample parsing documented in §15.8 and the parsing architecture.
- Personal-line real response samples now cover nocturnal/Datos.cu bonuses, Datos balances in KB, and national voice minutes; parser fixtures and conversion rules are documented in `parsing/ARCHITECTURE.md`.
- Parsers normalize accents and Unicode spaces and tolerate optional punctuation at labels, with fixtures for case, punctuation, whitespace, and decimal-separator variations.
- Every `SmsBodyParser` implementation — same reason.
- `EtecsaSmsFilter`'s "is this an ETECSA message" sender/content heuristic — unit tests cover the current short-code/keyword rule, but do not validate it against real sender IDs or message formats. Keep the heuristic explicitly provisional until representative ETECSA samples are available.

**Not yet done / open before this ships to real users**:
- The `sendUssdRequest` reliability spike against a real ETECSA SIM (§15.5) — a go/no-go gate for whether silent USSD capture works at all on this carrier, not yet run.
- A Play Store restricted-permissions business decision for the `unlocked` flavor's distribution (moot for `store`, which never requests them) — flagged, not resolved, since it's a business/policy call rather than a code one.

---

## 16. Dual-SIM Selector

Devices with two or more active SIM subscriptions (`SubscriptionManager.getActiveSubscriptionInfoList()`) show a SIM selector chip in the top app bar of all four main tabs (Home, Compras, Contactos, Ayuda) via `SimSelectorIcon()` inserted into `SearchableTopAppBar`'s `extraActions` slot. The interaction is configurable in Options › SIM y tipo de línea: open the current selection sheet, tap to cycle through active SIMs, or tap the badge to open a dropdown list. The list includes each SIM's configured name and personal/corporate line type.

**Global preference** — `SettingsDataStore` persists the selected slot and the Android subscription ID for the chosen SIM. The subscription ID keeps the same SIM selected if the system reorders active slots; the slot remains a fallback for legacy preferences or subscription ID changes. The selector waits until slot and SIM appearance preferences are read together before rendering, preventing an uncustomized carrier label from flashing at launch. It is configurable from Options › SIM y tipo de línea when multiple SIMs are active.

**Dashboard isolation and loading** — Dynamic dashboard snapshots are scoped to the selected subscription (`sim:<subscriptionId>:`). When the selected SIM changes, the Compose collector is recreated and animated skeleton cards remain visible until that SIM's first database emission arrives. A missing subscription never falls back to unscoped/global snapshot values, so one SIM's values cannot appear under another SIM while data loads.

**Per-widget override** — `WidgetPrefs.saveSimSlot()/getSimSlot()` per widget ID. Widget configuration screen shows the SIM picker only when 2+ SIMs are active. `DialTrampolineActivity` reads the per-widget SIM slot and passes it to `DialService`.

**SIM routing** — `DialService.dial()`/`dialDirect()` accept an optional `simSlot: Int?`. When set, `attachSimAccount()` resolves the matching `PhoneAccountHandle` from `TelecomManager.callCapablePhoneAccounts` by index and attaches it to the dial intent via `PHONE_ACCOUNT_HANDLE` extra. Falls back to global `SettingsDataStore` preference when no explicit slot is given.

**Key files**: `SimUtils.kt`, `SimSelectorChip.kt`, `SettingsDataStore.kt`, `DialService.kt`, `WidgetPrefs.kt`, `DialTrampolineActivity.kt`.

---

## 17. Contacts Per-Number Entries

The Contactos tab lists each phone number as a **separate row**, matching the iOS app's behavior. Contacts with a single Cuban number show just the number as subtitle; contacts with multiple Cuban numbers show each as its own row with a phone type label (e.g. "Móvil", "Trabajo", "Casa") prepended to the subtitle.

**Data model** — `ContactPhoneNumber` carries `raw`, `normalized`, and `label` (Spanish phone type string derived from `ContactsContract.CommonDataKinds.Phone.TYPE`/`LABEL`). `ContactEntry` is the flat view-layer model: one `(DeviceContact, number, label?)` per row. `ContactsRepository.flattenToEntries()` expands the contact list.

**UI** — `ContactRow` accepts a `ContactEntry`. `ContactOptionsSheet` operates on the specific number from the entry (not `cubanNumbers.firstOrNull()`). Search matches both name and number. `ContactsListScreen` starts in an explicit loading state when contact permission is granted; it shows a progress indicator until `loadContacts()` finishes, then either the entries or the "No se encontraron números cubanos" empty state. The empty state must not flash while contacts are still loading.

**Known limitation** — The system contact picker (`ACTION_PICK`) used in transfer and Plan Amigo flows shows all phone numbers (not just Cuban). When a non-Cuban number is selected, an `AlertDialog` shows an error explaining only Cuban numbers are accepted. This affects `ManualDashboardContent`, `TransferBottomSheet`, and `FriendsPlanManageScreen`.

---

## 18. Release Strategy & Parity-Based Version Codes

Qvacell distributes two flavors (`store` and `unlocked`) under the **exact same `applicationId`** (`com.qvacell.app`). To support side-by-side migration where a user can install the `unlocked` APK directly over an existing Google Play install without losing local data (Room database, preferences DataStore, transfer PIN):

1. **Shared Keystore**: Both APKs are signed with the same release signing key (`keys/release.jks`). In Google Play Console, Play App Signing must be configured using the existing uploaded key rather than a Google-generated key.
2. **Even/Odd Parity Scheme**:
   - `store` (Google Play): `baseVersionCode * 2` (Always **even**).
   - `unlocked` (GitHub / sideload): `baseVersionCode * 2 + 1` (Always **odd**).
   
| Release | Base Version Code | `store` (Even) | `unlocked` (Odd) | Upgrade Rule |
| :--- | :---: | :---: | :---: | :---: |
| `v1.0.2` | 3 | 6 | 7 | 7 > 6 $\rightarrow$ Sideload replaces Play Store build |
| `v1.0.3` | 4 | 8 | 9 | 9 > 8 $\rightarrow$ Sideload replaces Play Store build |
| `v1.1.0` | 5 | 10 | 11 | 11 > 10 $\rightarrow$ Sideload replaces Play Store build |

**Invariants guaranteed**:
- Because even numbers can never equal odd numbers, `store` and `unlocked` version codes **never collide** across past, present, or future releases.
- For any release version, `unlocked` is strictly greater than `store` ($2n+1 > 2n$), satisfying Android package manager's upgrade requirement.
- Release bumps via `make bump-patch`, `make bump-minor`, and `make bump-major` only manage the single `baseVersionCode`, with Gradle computing the flavors dynamically.

---

## 19. Firebase Analytics

`app/google-services.json` registers the Android package `com.qvacell.app` with Firebase. Both `store` and `unlocked` use that same package and Firebase Android app. The root Gradle build declares Google Services 4.5.0; the app module applies it and uses Firebase BoM 34.19.0 with `firebase-analytics`. BoM 35.0.0 currently resolves Google measurement 24.0.0 with Kotlin metadata 2.4, which this project's Kotlin 2.2.10 compiler cannot read; upgrade Kotlin/tooling before raising the BoM.

Analytics automatically reports first opens, sessions, engagement, and other SDK events. `AppTelemetry.screen()` logs a named `screen_view` for each Compose Navigation destination; Home uses `dashboard_manual` or `dashboard_dynamic` instead of the single Activity name. Automatic Activity screen reporting is disabled to avoid counting the same visit twice. `AppTelemetry.initialize()` tags Analytics with the `distribution` (`store`/`unlocked`) so reports can be filtered by build. Custom telemetry does not log phone numbers, contact names, PINs, USSD strings, or response text.

`first_open` measures the first launch after installation or reinstallation, **not raw downloads/installations**. Google Play Console remains the source for actual Play install counts; Firebase covers first launches across both distribution channels. Verify screen events in Analytics DebugView.

Crashlytics and Performance Monitoring are temporarily disabled by commenting out their Gradle plugins, dependencies, imports, initialization, and contacts-loading trace. The code is preserved for reactivation when delivery from Cuba becomes available. Qvacell retains only Analytics based on the Cuba network observations below. Crash, ANR, and performance reports are no longer collected.

### 19.1 Cuba network limitation (2026-10-22)

Qvacell targets Cuban users, so telemetry delivery from Cuba is a known operational concern.
On 2026-10-22, a Samsung SM-A155M running LlamaCon99, another Android app using Firebase
Analytics, Crashlytics, and Performance Monitoring, produced these results:

| Service | Without VPN | With VPN |
| --- | --- | --- |
| Analytics | Test event accepted: HTTP 204 | Test event accepted: HTTP 204 |
| Crashlytics | Report upload rejected: HTTP 403 | Synthetic nonfatal report accepted: HTTP 200 |
| Performance Monitoring | Metric upload rejected: HTTP 403; Remote Config reported an authorization failure | Remote Config recovered; test trace upload accepted: HTTP 200 |

The VPN retest required no code or Firebase configuration changes. Crashlytics uploads went to
`crashlyticsreports-pa.googleapis.com`; Performance uploads went to
`firebaselogging-pa.googleapis.com`. Server acceptance was confirmed; processed data in Firebase
Console was not verified.

These observations come from LlamaCon99, not a Qvacell delivery test. Verification specifically
for Qvacell (`com.qvacell.app`) and other Cuban networks remains pending. Successful local logging
or enqueueing alone does not confirm Analytics delivery. When validating Qvacell, compare the same test device with and without VPN,
check transport HTTP responses, and confirm the matching data in Firebase Console.

### 19.2 Reactivating Crashlytics and Performance Monitoring

Only Analytics is currently active. The disabled integration is preserved as `//` comments marked with a reference to this section. Once uploads work from Cuba without VPN, uncomment the following together:

1. `build.gradle.kts`: the Crashlytics 3.0.8 and Performance Monitoring 2.0.2 plugin declarations.
2. `app/build.gradle.kts`: both plugin applications and the `firebase-crashlytics` / `firebase-perf` dependencies.
3. `AppTelemetry.kt`: the three Firebase imports, the Crashlytics `distribution` custom key, and the complete `startTrace()` function.
4. `ContactsListScreen.kt`: the `AppTelemetry` import, `loadTrace` creation, and `loadTrace?.stop()` in `finally`.

Sync Gradle, build both distributions, and run the unit tests. On a test device in Cuba without VPN, verify a controlled Crashlytics report and a Performance trace: check successful transport responses and confirm the data in Firebase Console. Recheck Analytics and update both READMEs to reflect the services actually enabled. Re-enabling the Crashlytics plugin also restores release mapping-file uploads.

---

## 20. Release APK Optimization (R8)

The `release` build type enables `isMinifyEnabled = true` and `isShrinkResources = true` in `app/build.gradle.kts`. R8 removes unused app and dependency code, optimizes bytecode, and shortens names where possible; resource shrinking removes resources that are no longer referenced. Debug builds remain unminified. Release uses the optimized default ProGuard configuration plus project rules from `app/proguard-rules.pro`.

The catalog stores icon names as strings, so R8 cannot infer references from `codes.json`. `AndroidIconResolver.kt` maps every catalog icon to a direct Compose icon reference. This avoids reflection and tells R8 which icon accessors to retain. Avoid a broad keep rule for all Material icons because it would retain unused icons and reduce the size savings. An icon missing from the resolver falls back to `Icons.Filled.Circle`; `AndroidIconResolverTest` reads `codes.json` and fails if any catalog icon resolves to that fallback. When adding or changing Android icon identifiers, update the resolver and run the app unit tests.

R8 can change or remove code reached only through reflection, JNI, or external names. New dynamic access needs a narrow keep rule and a test against the minified release build. Keep the R8 mapping file associated with each distributed APK to allow manual deobfuscation of stack traces; mapping files are not uploaded to Crashlytics while its plugin is commented out.

APK size depends on dependencies, resources, tool versions, and build variant. In the local comparison made for v1.0.6, the store APK measured 3.76 MiB and unlocked measured 3.81 MiB, compared with the unminified v1.0.5 APKs at 16.72 MiB and 16.74 MiB. These are observed outputs, not guaranteed future sizes; compare the same variant and output type when reviewing size changes.

Build both release APKs and run the icon coverage and app unit tests with:

```sh
./gradlew :app:assembleStoreRelease :app:assembleUnlockedRelease
./gradlew :app:testStoreDebugUnitTest :app:testUnlockedDebugUnitTest
```

Release APKs are written under `app/build/outputs/apk/release/`.

---

## 21. Unit-Test Coverage and Limits

Shared unit tests live under `app/src/test/java/com/qvacell/app/`; flavor-specific tests use Android Gradle's `app/src/testUnlocked/` source set. They run against both flavors with Robolectric/JUnit where Android APIs are needed. Parser behavior and dashboard persistence have the broadest coverage; service, receiver, widget, and Compose interaction behavior is not comprehensively tested. There is no configured coverage-report task, so a passing test suite must not be interpreted as a measured coverage percentage.

Current focused behavior tests include:
- `parsing/` — representative and edge-case samples for supported USSD parsers and snapshot parsing; see [parsing/ARCHITECTURE.md](app/src/main/java/com/qvacell/app/parsing/ARCHITECTURE.md).
- `service/MmiErrorClassifierTest` — Spanish/English invalid-MMI text, accent/case variations, and negative examples. This only classifies returned response text; Android's generic telephony failure callback does not prove the selected SIM's line type is wrong.
- `testUnlocked/service/EtecsaSmsFilterTest` — boundary checks for the provisional short-code/keyword heuristic; real traffic samples remain required before claiming production accuracy.
- `model/CubanPhoneNumberTest` — local/country-prefixed Cuban mobile number normalization and invalid prefixes/lengths.
- `service/ReminderSchedulerTest` — one-time, daily, weekly, monthly, and custom recurrence date calculations. Alarm delivery, reboot recovery, notification actions, and exact-alarm permission behavior still need device/integration coverage.
- `service/UsageEstimateAccumulatorTest` — estimate counting rules: outgoing Cuban mobile calls only, whole elapsed minutes, sent-SMS Cuban addresses only, and empty logs.
- `data/SettingsDataStoreTest` — SIM switch action default, persistence, and invalid-value rejection. It does not test telephony-dependent selected-SIM resolution.
- `service/ContactsRepositoryTest` — flattening contacts into one row per valid Cuban number and applying labels only when a contact has multiple valid numbers.
- `widget/WidgetPrefsTest` — per-widget setting isolation, defaults, global-style fallback, and clearing a widget's overrides.
- DAO/repository tests — Room persistence and confirmed-value-versus-estimate behavior; they do not exercise the operating-system telephony, contacts, SMS, or call-log providers.

Before shipping changes to Android platform integrations, add or run device-level checks for actual SIM routing and USSD callbacks, SMS/call-log permissions and provider queries, exact alarm delivery after reboot, and widget actions. These require emulator/device capabilities that ordinary local unit tests cannot faithfully model. When adding a behavior test, document its invariant here and in the owning feature section; do not replace missing carrier samples with invented parser fixtures.
