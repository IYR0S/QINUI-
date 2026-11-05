# QINS UI Studio

A modular Android UI customization platform for the Xiaomi QINS1+ (Android 4.4.4 / API 19), built with a no-root-first, reversible approach.

## Current implementation

- Android application scaffold with API 19 minimum and Java 8 source compatibility.
- Dashboard with honest maturity labels for each planned module.
- **Theme Studio v0.1:** three palettes (Ocean, Midnight, Forest), live in-screen preview, persistent selection using SharedPreferences, and app-wide palette application across the dashboard and Theme Studio.
- English and Hebrew (RTL locale resource) strings.
- GitHub Actions debug APK build.

## Important scope boundary

Theme Studio currently themes QINS UI Studio itself. It does not modify Android System UI, third-party apps, launcher, notification shade, or secure lock screen. Those require separate capability and compatibility work. The project is designed to work without Root; Root is not required and is not currently used.

## Build

The CI workflow builds a debug APK using Gradle 8.2 and JDK 17. Download the artifact named `QINS-UI-Studio-debug` from the successful workflow run.

## Device validation

A successful CI build proves compilation and APK packaging only. Installation, rendering, RTL behavior, persistence, and performance still need validation on the actual QINS1+ running Android 4.4.4.
