# Roadmap

## Milestone 0 — repository and feasibility
- [x] Initialize repository documentation and Android module skeleton.
- [x] Verify debug APK builds through GitHub Actions.
- [ ] Inventory QINS1+ firmware, physical key map, screen dimensions and permissions.
- [ ] Research reusable open-source components and record license decisions.

## Milestone 1 — working foundation
- [x] Dashboard navigation and working module entry points.
- [x] Persistent shared palette store.
- [x] Shared app shell for palette application across current screens and in-app menu.
- [x] Physical-key-first navigation foundation (D-pad, OK / Enter, Back, MENU key).
- [x] English and Hebrew resource strings; RTL support flag.
- [ ] Verify real key codes and focus order on QINS1+ hardware.
- [ ] Add module registry and shared settings persistence.

## Milestone 2 — independently testable modules
- [x] Home Studio grid editor: choose 2–4 columns and 2–3 rows, preview and save locally.
- [x] QINS Home: register a no-root Android HOME activity, discover installed launchable apps, show icons/labels in a D-pad grid and launch selected apps.
- [x] QINS Home: use saved column/row settings and provide previous/next pages for the installed-app grid.
- [x] Stabilize Studio dashboard as a standalone Activity and make its launcher shortcut keyboard-activatable.
- [ ] QINS Home: test default-home selection, boot return, firmware-specific Home key mapping and focus behavior on QINS1+.
- [ ] Home Studio: configurable pinned apps, folders, tile ordering and multiple home pages.
- [x] Quick Panel companion: keyboard-accessible shortcuts to Android Wi-Fi, Bluetooth, sound, display, location, date/time and app settings (no-root; Android retains control of protected switches).
- [ ] Edge Panel: opt-in overlay with clear permission checks and graceful fallback.
- [ ] Theme Studio: expanded design tokens, typography, spacing, component styles and export/import.
- [ ] Lock Studio: safe companion/customization options within public APIs.

## Release criteria
A module is not marked complete until it builds, survives relaunch, handles denied permissions, and is tested on Android 4.4.4 hardware or a matching emulator. The release APK must be produced by CI or a documented local build and its checksum recorded.
