# QINS UI Studio

A modular Android UI customization platform for the Xiaomi QINS1+ (Android 4.4.4 / API 19), developed with a no-root, no-ROM, reversible-first approach.

## Project status

**Bootstrap / research phase.** This repository is being initialized before implementation. No Android APK is available yet.

## Goals

- Java + XML Android application targeting API 19 compatibility.
- Central dashboard and modular engines for Home, Quick Panel, Edge Panel, Themes, and Lock Studio.
- Shared design system, settings, localization (Hebrew RTL and English LTR), and module management.
- Offline-first operation and lightweight performance.
- No root access or modification of system files in the initial releases.

## Safety and platform constraints

Android 4.4.4 does not expose all modern System UI customization APIs. Features such as a true replacement of SystemUI or a fully custom secure lock screen cannot be assumed to work as ordinary third-party apps. Each feature must be classified by required permissions and verified on the actual device.

## Planned first milestone

1. Audit the repository and build environment.
2. Research API 19-compatible open-source components and licenses.
3. Document architecture and platform limitations.
4. Create the Android project skeleton.
5. Implement the dashboard, theme engine, settings, and module manager.
6. Build and validate a debug APK.

See `docs/ROADMAP.md` and `docs/COMPATIBILITY.md` as they are added.