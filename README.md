# QINS UI Studio

A modular Android UI customization platform for the Xiaomi QINS1+ (Android 4.4.4 / API 19), built with a no-root-first, reversible approach.

## Current implementation

- Android application scaffold with API 19 minimum and Java 8 source compatibility.
- **Physical-key-first UI:** navigation by D-pad / arrow keys, activation by OK / Enter, Back navigation, visible focus state, and an in-app menu. The app does not depend on a touch screen.
- **Shared app theme layer:** Ocean, Midnight and Forest palettes are stored persistently and applied to the Dashboard, Theme Studio, shared buttons and the in-app menu.
- English and Hebrew resource strings; RTL support is enabled in the manifest.
- GitHub Actions debug APK build.

## Scope boundary

The theme engine changes QINS UI Studio's own screens and menus. It does not recolor Android's system menus, notification shade, third-party apps or the device's other launcher. Those require system-level integration, firmware support or privileged permissions and are not part of this no-root build.

## Build

The CI workflow builds a debug APK using Gradle 8.2 and JDK 17. Download the artifact named `QINS-UI-Studio-debug` from the successful workflow run.

## Device validation

A successful CI build proves compilation and APK packaging only. Physical-key mapping, focus order, Hebrew rendering, installation, persistence and performance must still be validated on the actual QINS1+ running Android 4.4.4.
