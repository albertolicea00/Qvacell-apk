# Contributing to Qvacell

Thanks for your interest in improving Qvacell! This document explains how to propose changes.

## Ways to Contribute

- **Report outdated or wrong USSD codes** — the catalog is only useful if it is accurate.
- **Fix bugs** in the app.
- **Improve the UI/UX** while respecting the brand palette (navy `rgb(0,0,102)`, cyan `#09C`, black, white).
- **Improve documentation.**

## Development Setup

1. Install the toolchain: Android Studio (latest stable) with the Android SDK (API 26+ / API 34 for compiling).
2. Fork and clone the repository.
3. Open the project in Android Studio and let Gradle sync (the wrapper regenerates automatically on first open).
4. Build and run on a device or emulator.

## Updating the USSD Catalog

All codes live in `app/src/main/assets/codes.json`. When adding or changing a code:

- Keep the JSON schema: `id`, `code`, `title`, `details`, `type`, `requiresInput`, and the other optional fields documented in [ARCHITECTURE.md § 3](ARCHITECTURE.md#3-configuration-data-codesjson).
- Use `{input}` inside `code` for the user-provided part.
- `type` is `ussd` for dialed sequences ending in `#`, `call` for plain numbers, `sms` for SMS-based services.
- Cite a source (ETECSA announcement, official page) in your PR description.

> [!IMPORTANT]
> `codes.json` is shared verbatim with the [iOS app](https://github.com/albertolicea00/qvacell-ios) and the web version of the project. Modifying its schema or structure without coordination can break the other platforms.

## Pull Request Process

1. Create a feature branch from `main`: `git checkout -b feat/short-description`.
2. Make focused commits following [Conventional Commits](https://www.conventionalcommits.org/):
   - `feat: add roaming status code`
   - `fix: unwrap *99 collect-call number correctly`
   - `docs: update catalog table`
3. Ensure the project builds without warnings.
4. Fill in the pull request template.
5. A maintainer will review your PR; please respond to feedback.

## Code Style

- Kotlin, Jetpack Compose-first.
- All code, comments and identifiers in **English**.
- Prefer small, focused composables and value types (data classes).
- No third-party dependencies beyond AndroidX/Jetpack unless discussed in an issue first.

## Questions

Open a [discussion issue](.github/ISSUE_TEMPLATE) — we're happy to help.
