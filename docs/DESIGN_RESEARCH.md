# QINS visual design research

## Visual references

- Samsung One UI: large, calm information blocks, rounded control surfaces, a distinct Home / Apps distinction, and a configurable Quick Panel. Reference: https://developer.samsung.com/one-ui/structure/basic.html
- One UI 9 Quick Panel: independent brightness, sound and media controls, with more layout control. Reference: https://www.samsungmobilepress.com/articles/one-ui-9-beta-launch-galaxy-s26-series
- Google Pixel / Material 3 Expressive: wallpaper-aware tonal palettes, expressive rounded surfaces, glanceable information and customizable quick settings. Reference: https://blog.google/products-and-platforms/platforms/android/material-3-expressive-android-wearos-launch/
- Nothing OS: restrained monochrome identity, intentional dot-matrix accents, strong typography and distinctive clock layouts. Reference: https://intl.nothing.tech/nothing-os

## Open-source research

- OpenLauncherTeam/openlauncher is Apache-2.0 and documents paged desktops, app drawers, icon packs and long-press / drag editing. The project is discontinued, so QINS uses the interaction model as reference rather than importing its full dependency graph. Its upstream source requires Android support-library components and was designed around touch drag; QINS instead provides keypad-first long-press and directional reordering. Reference: https://github.com/OpenLauncherTeam/openlauncher
- google/material-design-icons is Apache-2.0 and can be reused in products. Its Android vector XML exports require a compatibility layer on API 19, so QINS currently uses small original Canvas glyphs rather than loading newer VectorDrawable assets. Reference: https://github.com/google/material-design-icons
- Third-party Nothing-themed icon packs were reviewed, but their artwork is not copied into QINS. The pack source's license and attribution conditions must be checked asset-by-asset before bundling. Reference: https://github.com/TeamNothingMuch/NothingMuch

## Implementation principles for QINS1+

- No dependency on AndroidX, modern VectorDrawable runtime support, touch dragging or a large launcher framework.
- Keep four user-pinned apps per home page; keep the complete installed-app list in a separate Applications drawer.
- Support physical-key short press to launch, long press to edit, and directional controls to reorder. Use the same flow with touch long-press when a touch digitizer exists.
- Keep Samsung / Pixel / Nothing presets as inspired visual directions. Do not copy brand logos, proprietary wallpapers, system UI assets or paid icon artwork.
- Validate icon size, typography, focus visibility and keypad long-press timing on the actual QINS1+ device before calling the interaction complete.


## Current Android UI research

Reviewed 2026-11-06 against current official platform and design references.

- **One UI 9 — configurable glance cards:** Samsung's editable Now Brief cards let users choose which information is surfaced. QINS adaptation: a small, user-ordered set of local home cards (media, missed calls, battery, timer and transfer progress), with one or two visible depending on screen height. Reference: https://api.samsungmobilepress.com/press-releases/one-ui-9-official-rollout-latest-galaxy-experiences/
- **Android 16 — Live Updates and notification grouping:** Google describes persistent real-time updates for compatible activities and grouping notifications from the same app. QINS adaptation: app-owned activity cards and locally grouped notification lists; do not depend on Android 16's OS-level Live Updates API. Reference: https://blog.google/products-and-platforms/platforms/android/android-16/
- **Android 16 — icon personalization:** custom icon shapes and themed icons provide a more cohesive home screen. QINS adaptation: API-19-safe Canvas masks (circle / squircle / rounded square) and an optional monochrome mode, with original app icons preserved as the default. Reference: https://blog.google/products-and-platforms/platforms/android/android-16-december/
- **One UI quick panel:** frequently used controls can be added, removed and reordered. QINS already has editable shortcuts; the next step is size variants and an explicit keypad-driven reorder mode. Reference: https://developer.samsung.com/one-ui/structure/basic.html
- **One UI accessibility — focus order:** focus should move logically, decorative and empty items should not receive focus, and related controls can be grouped. QINS adaptation: use a single focus stop for each card, clear directional movement and no accidental wrap at screen edges. Reference: https://developer.samsung.com/one-ui/accessibility/focus-order.html
- **One UI accessibility — interaction and control:** provide alternatives to complex gestures, clear feedback and a consistent route back. This supports QINS's physical-key-first approach. Reference: https://developer.samsung.com/one-ui/accessibility/interaction-and-control.html

Compatibility note: these are design patterns, not a promise that Android 4.4.4 can expose the corresponding modern system services. Every implementation must remain inside the existing no-root API-19 boundary and degrade gracefully when a system capability is unavailable.
