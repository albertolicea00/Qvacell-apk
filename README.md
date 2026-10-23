# 🇨🇺 Qvacell

> Your cell in Cuba. We changed the letter, kept the sound.

[![Platform](https://img.shields.io/badge/platform-Android%208.0%2B-blue.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/kotlin-2.0%2B-orange.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-BOM%202024.09-blue.svg)](https://developer.android.com/jetpack/compose)
![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen)
[![iOS sync](https://github.com/albertolicea00/Qvacell-apk/actions/workflows/cross-platform-sync-check.yml/badge.svg)](https://github.com/albertolicea00/Qvacell-apk/actions/workflows/cross-platform-sync-check.yml)

An Android app to quickly access the **USSD service codes of ETECSA (Cubacel)**: check your balance, buy data/voice/SMS plans, transfer credit and more — all from a clean, organized list that hands the code straight to the system dialer.

[Mira la versión en español](README.es.md)

## ⚠️ Disclaimer

> [!WARNING]
> This is an independent, community-made app. It is **not affiliated with, endorsed by, or sponsored by ETECSA**.
> Codes may change at any time at the carrier's discretion.

## ✨ Features

- 📋 **Full USSD Catalog** — Organized by categories (Balance & Plans, Purchases, Helplines, Utilities) with input-aware prompts.
- 📞 **One-Tap Dialing** — Opens the system dialer via `ACTION_DIAL`, requiring the same explicit user confirmation `tel://` gets on iOS.
- 👤 **Contact Integration** — Reads the device contacts to call, transfer credit, or place `*99` collect calls directly.
- 🛜 **Wi-Fi & Navigation Directory** — Offline search for ETECSA navigation rooms and public Wi-Fi hotspots by province.
- ✉️ **SMS Services Catalog** — Browse and prefill SMS service queries (news, weather, sports, utility rates) without silent sending.
- 👥 **Account & PIN Management** — Store transfer PIN in an encrypted local store and manage Plan Amigo numbers.
- 🔔 **Local Reminders** — Schedule recurring alerts for plan purchases, balance top-ups, or transfers, with a 1-tap dial action, rescheduled automatically after a device reboot.
- 🔍 **Offline Directory Search** — Reverse phone lookup over a user-supplied SQLite dump (hidden by default, see below).
- 🌗 **Customization & Settings** — Light/Dark theme support, custom accent color, and configurable launch tab.

> [!NOTE]
> **Native Android App**
> This app is built natively for Android to leverage platform-specific capabilities (like advanced background services and SMS integration) that could not be fully replicated on iOS. This is why it exists as a separate, dedicated native repository.

## 🛠️ Requirements & Compatibility

- 🤖 Android Studio (latest stable for development)
- 📱 **Supported Android Versions:**
  - **Minimum required:** Android 8.0 Oreo (API 26)
  - **Target SDK:** Android 14 (API 34)
  - **Tested & compatible:** Android 8.0, 9 (Pie), 10 (Q), 11, 12, 13, 14, and 15.
  - **OEM Skins tested:** One UI (Samsung), HyperOS / MIUI (Xiaomi/Redmi), Android AOSP / Stock, etc.
- 🔑 **APK Signing:** Dual V1 (JAR) and V2 (Full APK Signature) schemes enabled for maximum compatibility across legacy Android versions and custom vendor installers.
- 💻 Kotlin 2.0+, Jetpack Compose

## 📦 Build Flavors & Permissions

This project uses two Gradle product flavors (dimension `distribution`) to ship two distinct versions from the same codebase:

| Flavor | Distribution | `versionCode` Parity | Sensitive permissions | Dashboard auto-refresh |
| --- | --- | --- | --- | --- |
| **`store`** | Google Play | **Even** (`base * 2`) | None of the three below | Off — manual dial only, exactly like every other code in the app |
| **`unlocked`** | GitHub / manual APK sideload | **Odd** (`base * 2 + 1`) | `RECEIVE_SMS`, `READ_SMS`, `READ_CALL_LOG` (plus `CALL_PHONE`) | On (opt-in toggles in Options) — background capture & estimation |

> [!TIP]
> **Version Parity Strategy (Even/Odd `versionCode`)**:
> Both flavors share the exact same `applicationId` (`com.qvacell.app`). To ensure users can download the full `unlocked` APK from GitHub and **install it directly over** the Play Store build without uninstalling or losing data:
> - The `store` flavor gets an even number (e.g. base 3 $\rightarrow$ code 6).
> - The `unlocked` flavor gets the next odd number (e.g. base 3 $\rightarrow$ code 7).
> Because $7 > 6$, the Android package manager treats it as a legitimate upgrade. Furthermore, version codes will never collide across past, present, or future releases.

`store` exists specifically so the Play Store build never has to clear Google's restricted-permissions review for `READ_SMS`/`READ_CALL_LOG` — it's built from the exact same source, just missing the capture source sets (`app/src/store/`) instead of `unlocked`'s (`app/src/unlocked/`). See [ARCHITECTURE.md § 15](ARCHITECTURE.md#15-dashboard-dynamic-data-pipeline) for how the split works and what each flavor's capture layer does.

**Why each permission exists** (`unlocked` only — `store` requests none of these):

| Permission | Why |
| --- | --- |
| `CALL_PHONE` | Already existed for Compras' purchase-confirmation flow (`ACTION_CALL`, dials directly once the user accepts the in-app confirm sheet). `unlocked` also reuses it for silent USSD capture (`TelephonyManager.sendUssdRequest`) — same permission, wider use, which is why that path is opt-in behind an explainer rather than silently repurposing the existing grant. |
| `READ_SMS` | One-time historical scan of the SMS inbox, so a fresh install can backfill the dashboard from ETECSA messages the user already received, not just ones that arrive from that point on. |
| `RECEIVE_SMS` | Live capture of new incoming ETECSA SMS (balance, deductions, limit dates) as they arrive, via a broadcast receiver — the "keep the dashboard fresh going forward" half of SMS capture. |
| `READ_CALL_LOG` | Feeds the background estimation engine: counts/durations of calls to Cuban numbers since the last confirmed reading, to estimate usage between real USSD/SMS syncs. |

None of these are requested at install time — each is asked for at runtime, right before the specific feature that needs it (Options toggles, both default OFF), with an in-app explanation of why.

**Build & run:**

```bash
# Debug builds
./gradlew :app:assembleStoreDebug       # Play-Store-safe build, no sensitive permissions
./gradlew :app:assembleUnlockedDebug    # full build with the dashboard capture pipeline

# Install straight to a connected device/emulator
./gradlew :app:installStoreDebug
./gradlew :app:installUnlockedDebug

# Release builds (unsigned unless a signing config is set up)
./gradlew :app:assembleStoreRelease
./gradlew :app:assembleUnlockedRelease

# Run the test suite for a given flavor
./gradlew :app:testStoreDebugUnitTest
./gradlew :app:testUnlockedDebugUnitTest
```

In Android Studio, use the **Build Variants** panel (bottom-left) to switch between `storeDebug`/`unlockedDebug`/etc. before running ▶️.

## 🚀 Getting Started

```bash
git clone https://github.com/albertolicea00/qvacell-apk.git
cd qvacell-apk
```

Open the project in Android Studio and let it sync — the Gradle wrapper regenerates automatically on first open. Build and run on a device or emulator.

**USSD dialing requires a physical device with a Cubacel SIM** 📲 — the emulator has no real telephony stack and cannot place calls.

## 🗂️ Project Structure

```
app/src/main/java/com/qvacell/app/
├── QvacellApplication.kt        # Application entry point, notification channels
├── MainActivity.kt              # Single-Activity host, applies theme, requests notification permission
├── model/                       # USSDCode/Category catalog, CubanPhoneNumber, Reminder
├── data/                        # Room database, DAOs, settings DataStore
├── service/                     # DialService, ContactsRepository, TransferPinStore,
│                                 # ReminderRepository/Scheduler, DirectoryDatabase
├── receiver/                    # Reminder alarm/action receivers, boot rescheduling
└── ui/                          # Compose screens, navigation, shared components

app/src/main/assets/
├── codes.json                   # Bundled USSD code catalog
└── wifi_navigation_rooms.json   # Bundled ETECSA navigation-room/hotspot directory
```

_The full USSD code catalog is loaded from the bundled [`codes.json`](app/src/main/assets/codes.json)._ 📁

## ☎️ Direct Dial vs. Confirmation

Free query codes dial immediately. Paid purchase codes stop at ETECSA's confirmation menu by default; an optional **Acción sin Confirmación** setting substitutes a code variant that auto-confirms, with a visible UI safety warning.

## 📊 Firebase

The app uses Firebase Analytics for first opens, sessions, and screen views (including the Manual and Dynamic dashboards). Reports include the `store` or `unlocked` distribution. Custom telemetry does not send contact names, phone numbers, PINs, USSD codes, or USSD responses. Crashlytics and Performance Monitoring are temporarily commented out because their uploads were rejected from Cuba in the test described below. Their integration is preserved for reactivation; see [architecture instructions](ARCHITECTURE.md#192-reactivating-crashlytics-and-performance-monitoring).

To build, register the Android app `com.qvacell.app` in a Firebase project with Analytics enabled and place the downloaded configuration at `app/google-services.json`. This file is ignored by Git; keep it local and check that its API key is restricted to the Firebase APIs the app uses. Both flavors share it. Check **Analytics › DebugView** in the Firebase console to validate reporting. `first_open` counts first launches after install or reinstall; Google Play download counts are available in Play Console.

## 🔍 Phone Directory & Offline Database Search

Under **Options › Utilidades**:

- **Buscar en Database**: Advanced offline reverse phone lookup over a user-supplied SQLite (`.db`) file, imported via the system file picker. For security and privacy, this feature is hidden by default (unlocked by tapping the app version 5 times in _Acerca de_) and searches strictly by phone number (no name lookup).

## 📶 LTE-only network mode on Android

Under **Options › Utilidades › Forzar red a solo LTE (4G)**, Qvacell provides a shortcut to Android's radio information menu. The app does not change the network mode itself: the user must manually choose **LTE Only / LTE_ONLY** in that menu. Access may be blocked or unavailable depending on the phone brand and model.

Before changing modes, note the current preferred network type so you can restore it. Some phones show options such as **LTE/TD-SCDMA/UMTS** or **LTE/TD-SCDMA/WCDMA**. **LTE Only** may prevent incoming and outgoing calls and SMS; restore the original mode when finished.

Do not confuse Android's radio menu with iPhone **Field Test Mode**: dialing `*3001#12345#*` displays live technical signal statistics (for example, RSRP, bands, and frequencies), but it does not allow band locking or LTE-only selection. On Android, `*#*#4636#*#*` may open phone/network information on some models, and some Samsung devices provide a separate band-selection menu through `*#2263#`; availability depends on the manufacturer and device.

## 🛜 Navigation Rooms & Public Wi-Fi

Includes an offline directory of official ETECSA navigation rooms and public Wi-Fi hotspots by province.

## 🔄 Cross-Platform Catalog Sync

[`cross-platform-sync-check.yml`](.github/workflows/cross-platform-sync-check.yml) runs on every push to `main` that touches `codes.json` or `wifi_navigation_rooms.json`, and compares this repo's copy against [qvacell-ios](https://github.com/albertolicea00/Qvacell-ios)'s. If they've drifted, it opens (or updates) an issue on the _other_ repo so the missed platform gets updated. Only **structure** is compared for `codes.json` (ids, dial strings, action type, input handling, category/group placement) — cosmetic fields (icon, price, title wording, etc.) are allowed to differ per platform. See [ARCHITECTURE.md § 14](ARCHITECTURE.md#14-cross-platform-catalog-sync-check) for exactly what's compared and how.

## 🚧 Known Limitations

- **Firebase telemetry may be limited from Cuba (2026-10-22).** Qvacell targets Cuban users. In a test with LlamaCon99, another Android app using the same Firebase services, Analytics successfully sent the test event without VPN (HTTP 204), while Crashlytics and Performance Monitoring received HTTP 403. With VPN enabled, both services accepted their test data (HTTP 200). Qvacell therefore retains only Analytics; Crashlytics and Performance Monitoring remain commented out for future reactivation. This test covered a Samsung SM-A155M and one connection; verification specifically in Qvacell and on other Cuban networks remains pending. See [architecture details](ARCHITECTURE.md#191-cuba-network-limitation-2026-10-22).
- **SIM phone numbers may not be detected automatically.** Most SIM cards (especially Cubacel / ETECSA) do not have the phone number (MSISDN) written directly onto the SIM chip by the carrier. In such cases, Android's telephony APIs return `null` or empty, so the app falls back to displaying the carrier name or slot index (e.g. `SIM 1` / `SIM 2`).
- **Carrier/OEM dialer apps may intercept USSD codes** before this app's `ACTION_DIAL` intent reaches the modem, depending on device and ROM. This is an Android platform/OEM behavior outside the app's control.
- **A system USSD error dialog may appear if cellular service drops during a dashboard query.** Qvacell stops the remaining requests and shows its own status, but Android's public `sendUssdRequest` API cannot cancel a request already sent to the telephony service or dismiss a dialog shown by the system or carrier. Airplane mode during a query reproduces the same limitation.

## 🤝 Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md). Please follow the [Code of Conduct](CODE_OF_CONDUCT.md).

> ⚠️ **Issues, PR descriptions, and commit messages must be written in English.**
> The app UI is intentionally in Spanish — it targets Cuban users. All technical communication follows English conventions.

## 📚 Sources

The codes were saved from the following sites:

- https://galixpay.com/recargas-a-cuba/
- https://www.fonoma.com/blog/codigos-ussd-cuba
- https://www.etecsa.cu/es/taxonomy/term/1445
- https://www.etecsa.cu/en/rooms-public-spaces
- https://www.etecsa.cu/es/empresas/movil-combos
- https://www.ecured.cu/Entumovil
- https://www.escambray.cu/2017/etecsa-informa-sobre-nuevos-servicios-de-telefonia-movil-para-clientes-prepago-infografia/
- https://www.entumovil.cu/#:~:text=Para%20activar%20las%20siguientes%20prestaciones%2C,portal%20el%20de%20su%20preferencia.

---

_Developed by @albertolicea00 — Android port of [qvacell-ios](https://github.com/albertolicea00/qvacell-ios)._
