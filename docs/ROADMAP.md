# Roadmap

## Milestone 0 — repository and feasibility
- [x] Initialize repository documentation and Android module skeleton.
- [x] Verify a debug APK build through GitHub Actions.
- [ ] Inventory the QINS1+ firmware, physical key map, screen dimensions and permissions.
- [ ] Research reusable open-source components and record license decisions.

## Milestone 1 — working foundation
- [x] Dashboard navigation and first working module entry.
- [x] Persistent shared palette store.
- [x] Shared app shell for palette application across current screens and in-app menu.
- [x] Physical-key-first navigation foundation (D-pad, OK / Enter, Back, MENU key).
- [x] English and Hebrew resource strings; RTL support flag.
- [ ] Verify real key codes and focus order on QINS1+ hardware.
- [ ] Add module registry and settings persistence.

## Milestone 2 — independently testable modules
- [ ] Home Studio: launcher activity and home-screen interactions.
- [ ] Quick Panel Studio: in-app/overlay companion, never a claim to replace System UI.
- [ ] Edge Panel: opt-in overlay with clear permission checks and graceful fallback.
- [ ] Theme Studio: expanded design tokens, typography, spacing, component styles and export/import.
- [ ] Lock Studio: safe companion/customization options within public APIs.

## Release criteria
A module is not marked complete until it builds, survives rotation/relaunch, handles denied permissions, and is tested on Android 4.4.4 hardware or a matching emulator. The release APK must be produced by CI or a documented local build and its checksum recorded.
