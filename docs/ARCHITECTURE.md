# Architecture

## Application layers
- **App shell:** QINS HOME is the single launcher activity; Home and Quick Panel are internal pages. Studio is opened with the physical MENU key.
- **Shared UI core:** reusable API-19-compatible views and layout helpers.
- **Design tokens:** colors, spacing and typography stored centrally.
- **Theme engine:** reads and applies app-owned visual preferences; no system theme patching.
- **Module registry:** explicit module metadata, enabled state and capability status.
- **Settings/storage:** SharedPreferences for small settings; SQLite only when structured collections justify it.
- **Localization:** resource-based strings with `values/` and `values-iw/` (Hebrew RTL); test mixed Hebrew/English text.

## Module boundaries
Each studio module should have its own package and UI entry point. Modules call shared interfaces rather than each other's implementation. A disabled module must not start services or request permissions.

## Permission principles
Request no permissions at first launch. Ask only when a user activates a feature that needs one, explain why, verify the capability, and provide a no-permission fallback. No root, shell commands, hidden APIs, system-file writes, or accessibility-service abuse.

## Current scaffold
`QinsLauncherActivity` provides four-app home pages, a minimal clock/date/battery glance area, a bottom Quick Panel handle, direct quick controls, and Galaxy / Pixel / Nothing appearance presets. `MainActivity` remains a separate Studio workspace opened from the physical MENU key.
