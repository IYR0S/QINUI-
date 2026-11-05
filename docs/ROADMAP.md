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
- [x] QINS Home: use saved column/row settings and provide previous/next pages for the installed-app grid.\n- [x] Unify Home and Quick Panel as pages inside one QINS HOME activity; no separate Quick Panel Activity launch required.\n- [x] Replace GridLayout-based launcher app grid with API-19-safe LinearLayout rows.
- [x] Stabilize Studio dashboard as a standalone Activity and make its launcher shortcut keyboard-activatable.
- [ ] QINS Home: test default-home selection, boot return, firmware-specific Home key mapping and focus behavior on QINS1+.
- [ ] Home Studio: configurable pinned apps, folders, tile ordering and multiple home pages.
- [x] Quick Panel first direct-control prototype: Wi-Fi, Bluetooth, ringer mode, media volume and screen brightness from inside QINS HOME (no-root; exact support must be validated on QINS1+ firmware).\n- [ ] Add media transport, battery/date widgets, quick tile customization and firmware-specific fallback states to Quick Panel.
- [ ] Edge Panel: opt-in overlay with clear permission checks and graceful fallback.
- [ ] Theme Studio: expanded design tokens, typography, spacing, component styles and export/import.
- [ ] Lock Studio: safe companion/customization options within public APIs.

## Release criteria
A module is not marked complete until it builds, survives relaunch, handles denied permissions, and is tested on Android 4.4.4 hardware or a matching emulator. The release APK must be produced by CI or a documented local build and its checksum recorded.
