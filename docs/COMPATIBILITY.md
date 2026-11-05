# Android 4.4.4 / API 19 compatibility notes

The target device is reported as Android 4.4.4 (API 19). The first app version therefore uses `minSdk 19` and `targetSdk 19` and avoids AndroidX and newer framework APIs.

## Confirmed platform facts
- Android 4.4 is API level 19. See [Android 4.4 APIs](https://android.googlesource.com/platform/frameworks/base/+/0facf021ea1a0399d956372b9d3ad9025a9a04d2/docs/html/about/versions/android-4.4.jd).
- Android 4.4 requires `READ_EXTERNAL_STORAGE` to read shared external-storage files; app-specific external directories do not require it. Avoid requesting broad storage access until a feature needs it.
- Overlay windows use the legacy `TYPE_PHONE` / `TYPE_SYSTEM_ALERT` model on API 19; newer `TYPE_APPLICATION_OVERLAY` is not available. Any overlay must be opt-in, checked on-device, and removable.
- Notification access is a user-enabled special service protected by `BIND_NOTIFICATION_LISTENER_SERVICE`; it must not be silently assumed.
- A third-party app cannot replace privileged SystemUI components or the secure keyguard merely by drawing a screen. Treat these as unsupported unless a device-specific, user-approved public mechanism is demonstrated.

## Build choices
- Java 8 language compatibility.
- Android Gradle Plugin 8.2.2, Gradle 8.2+, JDK 17, compile SDK 34; min/target API 19.
- No runtime dependencies in the initial scaffold.

## Device validation still required
- Exact Android build fingerprint and OEM modifications.
- Available RAM, screen size/density and navigation mode.
- Whether the firmware allows overlay permission and whether background services remain alive.
- Actual Bluetooth/Wi-Fi controls: API availability does not imply a normal app can toggle protected settings on every firmware.

This document is a feasibility baseline, not a claim that all modules are already implemented.
