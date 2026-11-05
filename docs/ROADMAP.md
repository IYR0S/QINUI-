# Roadmap

## Milestone 0 — repository and feasibility
- [x] Initialize repository documentation and Android module skeleton.
- [ ] Verify SDK/JDK/Gradle availability and build a debug APK.
- [ ] Inventory the QINS1+ firmware, launcher, screen dimensions and permissions.
- [ ] Research reusable open-source components and record license decisions.

## Milestone 1 — working foundation
- [ ] Dashboard navigation and module registry.
- [ ] Theme engine backed by persistent settings.
- [ ] Hebrew RTL / English LTR localization.
- [ ] Settings and module enable/disable controls.
- [ ] Automated build workflow and installable debug APK.

## Milestone 2 — independently testable modules
- [ ] Home Studio: launcher activity and home-screen interactions.
- [ ] Quick Panel Studio: in-app/overlay companion, never a claim to replace System UI.
- [ ] Edge Panel: opt-in overlay with clear permission checks and graceful fallback.
- [ ] Theme Studio: token editor, previews and export/import.
- [ ] Lock Studio: safe companion/customization options within public APIs.

## Release criteria
A module is not marked complete until it builds, survives rotation/relaunch, handles denied permissions, and is tested on Android 4.4.4 hardware or a matching emulator. The release APK must be produced by CI or a documented local build and its checksum recorded.
