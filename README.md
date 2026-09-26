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
- 🆔 **Caller ID (`*99` collect calls)** — A `CallScreeningService` unwraps ETECSA's wrapped collect-call number and surfaces the real contact's name via a heads-up notification. See [Known Limitations](#-known-limitations) — Android does not let a non-default-dialer app inject a name into the system in-call UI the way iOS CallKit does.
- 🛜 **Wi-Fi & Navigation Directory** — Offline search for ETECSA navigation rooms and public Wi-Fi hotspots by province.
- ✉️ **SMS Services Catalog** — Browse and prefill SMS service queries (news, weather, sports, utility rates) without silent sending.
- 👥 **Account & PIN Management** — Store transfer PIN in an encrypted local store and manage Plan Amigo numbers.
- 🔔 **Local Reminders** — Schedule recurring alerts for plan purchases, balance top-ups, or transfers, with a 1-tap dial action, rescheduled automatically after a device reboot.
- 🔍 **Offline Directory Search** — Reverse phone lookup over a user-supplied SQLite dump (hidden by default, see below).
- 🌗 **Customization & Settings** — Light/Dark theme support, custom accent color, and configurable launch tab.

> [!NOTE]
> **Native Android App**
> This app is built natively for Android to leverage platform-specific capabilities (like advanced background services, call screening, and SMS integration) that could not be fully replicated on iOS. This is why it exists as a separate, dedicated native repository.

## 🛠️ Requirements

- 🤖 Android Studio (latest stable)
- 📱 Android 8.0+ (API 26)
- Kotlin 2.0+, Jetpack Compose

## 📦 Build Flavors & Permissions

**Two Versions Available**
This project uses build flavors to offer two distinct versions of the app:
- **App Store Version**: A restricted build designed to comply with strict store policies regarding sensitive permissions (such as SMS and Accessibility).
- **Standalone Version (Manual Install)**: The full, unrestricted experience. This version utilizes SMS and Accessibility permissions to automatically read carrier messages and USSD responses in the background. This allows the app to keep your data and balance dashboard updated in near real-time.

## 🚀 Getting Started

```bash
git clone https://github.com/albertolicea00/qvacell-apk.git
cd qvacell-apk
```

Open the project in Android Studio and let it sync — the Gradle wrapper regenerates automatically on first open. Build and run on a device or emulator.

**USSD dialing requires a physical device with a Cubacel SIM** 📲 — the emulator has no real telephony stack and cannot place calls.

To get Caller ID working for `*99` collect calls, open **Ajustes › Acerca de › Identificador de Llamadas** in the app and grant the call-screening role when prompted (`RoleManager.ROLE_CALL_SCREENING`). This is a one-time, manual Android setting — no app can enable it automatically. See [ARCHITECTURE.md § 11](ARCHITECTURE.md#11-caller-id-callscreeningservice-99-collect-call-identification) for why the result looks different from iOS's CallKit extension.

## 🗂️ Project Structure

```
app/src/main/java/com/qvacell/app/
├── QvacellApplication.kt        # Application entry point, notification channels
├── MainActivity.kt              # Single-Activity host, applies theme, requests notification permission
├── model/                       # USSDCode/Category catalog, CubanPhoneNumber, Reminder, WrappedCaller
├── data/                        # Room database, DAOs, settings DataStore
├── service/                     # DialService, ContactsRepository, TransferPinStore,
│                                 # ReminderRepository/Scheduler, DirectoryDatabase, CallerIdScreeningService
├── receiver/                    # Reminder alarm/action receivers, boot rescheduling
└── ui/                          # Compose screens, navigation, shared components

app/src/main/assets/
├── codes.json                   # Bundled USSD code catalog
└── wifi_navigation_rooms.json   # Bundled ETECSA navigation-room/hotspot directory
```

_The full USSD code catalog is loaded from the bundled [`codes.json`](app/src/main/assets/codes.json)._ 📁

## ☎️ Direct Dial vs. Confirmation

Free query codes dial immediately. Paid purchase codes stop at ETECSA's confirmation menu by default; an optional **Acción sin Confirmación** setting substitutes a code variant that auto-confirms, with a visible UI safety warning.

## 🔍 Phone Directory & Offline Database Search

Under **Ajustes › Utilidades**:

- **Buscar en Database**: Advanced offline reverse phone lookup over a user-supplied SQLite (`.db`) file, imported via the system file picker. For security and privacy, this feature is hidden by default (unlocked by tapping the app version 5 times in _Acerca de_) and searches strictly by phone number (no name lookup).

## 🛜 Navigation Rooms & Public Wi-Fi

Includes an offline directory of official ETECSA navigation rooms and public Wi-Fi hotspots by province.

## 🔄 Cross-Platform Catalog Sync

[`cross-platform-sync-check.yml`](.github/workflows/cross-platform-sync-check.yml) runs on every push to `main` that touches `codes.json` or `wifi_navigation_rooms.json`, and compares this repo's copy against [qvacell-ios](https://github.com/albertolicea00/Qvacell-ios)'s. If they've drifted, it opens (or updates) an issue on the _other_ repo so the missed platform gets updated. Only **structure** is compared for `codes.json` (ids, dial strings, action type, input handling, category/group placement) — cosmetic fields (icon, price, title wording, etc.) are allowed to differ per platform. See [ARCHITECTURE.md § 14](ARCHITECTURE.md#14-cross-platform-catalog-sync-check) for exactly what's compared and how.

## 🚧 Known Limitations

- **Caller ID cannot show a custom name in the system in-call UI.** Unlike iOS's CallKit Call Directory Extension, Android's `CallScreeningService` API does not let a non-default-dialer app inject a caller name into the system's own incoming-call screen. This app instead surfaces the resolved name via a heads-up notification when a wrapped `*99` collect call rings. Becoming the user's default dialer app to get full name injection was deliberately not pursued — it's a much larger commitment (replacing core phone UI) for one feature.
- **Carrier/OEM dialer apps may intercept USSD codes** before this app's `ACTION_DIAL` intent reaches the modem, depending on device and ROM. This is an Android platform/OEM behavior outside the app's control.

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
- https://www.ecured.cu/Entumovil
- https://www.escambray.cu/2017/etecsa-informa-sobre-nuevos-servicios-de-telefonia-movil-para-clientes-prepago-infografia/
- https://www.entumovil.cu/#:~:text=Para%20activar%20las%20siguientes%20prestaciones%2C,portal%20el%20de%20su%20preferencia.

---

_Developed by @albertolicea00 — Android port of [qvacell-ios](https://github.com/albertolicea00/qvacell-ios)._
