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
- [x] QINS Home: four pinned apps per horizontal page; app drawer is separate from the home pages.
- [x] QINS Home: persistent pinned-app list with default suggestions and add/remove/reorder controls.
- [x] QINS Home: physical OK/Enter long-press enters edit mode; short press still launches the selected app.
- [x] App drawer: short press launches; long press pins an app to Home; top navigation switches Home / Apps.
- [x] Unify Home and Quick Panel as pages inside one QINS HOME activity; bottom handle opens/closes Quick Panel.
- [x] Replace GridLayout-based launcher app grid with API-19-safe LinearLayout rows.
- [x] Stabilize Studio dashboard as a standalone Activity and make its launcher shortcut keyboard-activatable.
- [ ] QINS Home: test default-home selection, boot return, firmware-specific Home key mapping and focus behavior on QINS1+.
- [ ] Verify long-press timing and focus movement on actual QINS1+ hardware; adjust to the keypad's repeat behavior.
- [ ] Tune four-app icon sizing and page spacing against the actual QINS1+ screen resolution.
- [ ] Home Studio: configurable pinned apps, folders, tile ordering and multiple home pages.
- [x] Quick Panel visual redesign: rounded control tiles, brightness and media-volume sliders, date/time/battery and direct Wi-Fi, Bluetooth, sound controls.
- [x] Add battery/date/time glance information and in-panel Galaxy / Pixel / Nothing appearance switcher.
- [ ] Add media transport, reorderable quick-tile slots, and more API-19-supported toggles (hotspot/rotation only where firmware permits).
- [ ] Edge Panel: opt-in overlay with clear permission checks and graceful fallback.
- [ ] Theme Studio: expanded design tokens, typography, spacing, component styles and export/import.
- [ ] Lock Studio: safe companion/customization options within public APIs.


## Milestone 3 — modern Android UI adaptations

These ideas borrow interaction patterns from current Android / One UI releases, but must remain usable on the QINS1+ keypad and API 19.

- [x] QINS Home: full-screen branded header and wallpaper canvas; preserve the top of user-selected images.
- [x] QINS Home: compact Now Bar-style cards for active media and missed calls, with physical-key actions.
- [x] QINS Home: per-theme clock background preference (transparent by default or solid surface).
- [ ] Now Bar cards: allow up to two compact cards (for example, media + missed call) when the available height allows; fall back to one card on short screens.
- [ ] Add local activity cards for timer / stopwatch and file-transfer progress; expose only actions that can be activated by the physical keys.
- [ ] Notification organizer: group by app, offer a simple priority / quiet filter, and keep direct actions available without touch.
- [ ] Add optional icon masks (circle, squircle, rounded square) and a monochrome icon mode for the Nothing preset; keep original colors as the default.
- [ ] Quick Panel: offer small and wide tile sizes, reorder by physical-key commands, and expose a visible focus path across both controls and notifications.
- [ ] Add a configurable glance-card order for clock, battery, media, missed calls and timer; show no more than the screen can comfortably fit.
- [ ] Add a compact visual “now playing” progress state without always-on animation; provide previous / pause / next as focusable controls.

### Device-first interaction and compatibility rules

- Keep a visible focus indicator and a predictable D-pad / OK / Back route for every action. Decorative elements and inert empty space should not receive focus.
- Do not require swipe, drag-and-drop or multi-finger gestures. Provide keypad alternatives for editing, paging and dismissing.
- Prefer static gradients and translucent surfaces over real-time blur or continuous animation on the low-power device.
- Treat Android 16 Live Updates, AI notification summaries, and global themed-icon services as design inspiration only. Implement local equivalents inside QINS rather than relying on framework APIs unavailable on API 19 or privileged system access.
- Verify the top edge of every wallpaper variant, Hebrew / RTL control direction, focus order, softkey mapping, minimum / maximum brightness and long-press behavior on the actual QINS1+ hardware before release.

## Release criteria
A module is not marked complete until it builds, survives relaunch, handles denied permissions, and is tested on Android 4.4.4 hardware or a matching emulator. The release APK must be produced by CI or a documented local build and its checksum recorded.
