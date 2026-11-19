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


## Quick Panel refresh — November 2026

- **Samsung One UI 9:** current research confirms independently resizable brightness and volume sliders, resizable media player, detachable sound-mode and display-mode controls, thicker sliders, and editable/reorderable controls. References: https://www.sammobile.com/news/look-one-ui-9-design-changes-new-features/ and https://9to5google.com/2026/07/14/one-ui-9-all-the-features-you-need-to-know-video/
- **Nothing OS 5:** official community release notes confirm Android 17, refreshed Geist typography, custom status icons, translucent/Lucent theme and optional separation of Notifications and Quick Settings. Independent hands-on coverage reports vertical brightness/volume sliders, a compact media player above controls, and resizable two-cell switches. QINS mirrors the vertical sliders, compact/resizable media card and tile sizing where API 19 permits; exact system blur is not available on this legacy target. References: https://nothing.community/d/61730-nothing-os-50-new-features-explained and https://www.91mobiles.com/reviews/nothing-os-5-hands-on/
- **QINS implementation constraints:** minimum Android 4.4.4/API 19 means no platform Do Not Disturb API and no native real-time background blur. The DND control currently maps to silent ringer mode; hotspot shortcut opens wireless/tether settings instead of claiming a privileged hotspot toggle. Pixel uses translucent wallpaper-aware surfaces and active-state shape changes as an API-19-compatible approximation.
- **QINS tile editor:** each expanded tile supports 1×1, 2×1 and 2×2 spans. Samsung layout defaults to 2×1 Wi-Fi/Bluetooth followed by four 1×1 shortcuts; the other themes default to two columns of 2×1 controls.


## Design integration pass — 2026-11-19

The first November UI pass had landed on an older feature branch. The current integration branch is based on the newer QINS Home feature line and reapplies the OEM-specific design work without discarding the Music Widget, launcher settings, Edge Bar and keypad guide.

### Slider behaviour is intentionally not uniform

- **Samsung / One UI:** Samsung's design guidance treats brightness and volume as setting sliders with immediate feedback. One UI 8.5 also exposes independent sizing and orientation options for panel controls. QINS uses a broad, high-contrast brightness control and a separate volume control with a rounded thumb and a thicker Galaxy track.
- **Google Pixel / Material 3 Expressive:** Android 16 QPR1 gives brightness a prominent Material container and handle. The volume UI is a separate volume panel with its own slider treatment, rather than a second copy of the brightness card. QINS keeps a prominent rectangular brightness handle and a larger rounded volume thumb.
- **Nothing OS 5:** current hands-on coverage describes paired vertical brightness and volume sliders beside a compact media player. QINS now uses its API-19-compatible vertical sliders in the Nothing preset, with compact square handles and a restrained red accent.

References:
- Samsung One UI 8.5 Quick Panel customisation: https://www.samsung.com/uk/support/mobile-devices/how-to-customise-the-quick-panel-on-the-samsung-galaxy-s26-series/
- Samsung One UI slider design guidance: https://design.samsung.com/global/contents/one-ui/download/oneui_design_guide_eng.pdf
- Pixel Android 16 QPR1 Quick Settings redesign: https://9to5google.com/2025/05/20/android-16-qpr1-quick-settings/
- Pixel Android 16 QPR1 volume/brightness slider refinement: https://9to5google.com/2025/06/11/android-16-qpr1-sliders/
- Nothing OS 5 official notes: https://nothing.community/d/61730/nothing-os-50-new-features-explained
- Nothing OS 5 hands-on (vertical sliders and compact media player): https://www.91mobiles.com/reviews/nothing-os-5-hands-on/

### Applied integration changes

- Compact Quick Settings now applies its focus state on initial creation and shows a card-level focus outline, including for Galaxy's compact controls; the icon-only focus ring was too easy to miss.
- Galaxy's expanded panel restores dedicated Wi-Fi and Bluetooth cards, followed by four compact shortcuts.
- Nothing's expanded panel now actually calls the vertical slider layout already present in the code; it no longer falls back to the shared horizontal layout.
- Brightness and volume are not forced into one visual component: the volume slider keeps its own visible thumb and per-theme geometry.
- The home pinned-app row now has a theme-aware dock surface: large soft Galaxy capsule, tonal Pixel surface, and restrained outlined Nothing dock.
- Palette tokens are aligned with the intended identities: Galaxy 26dp surfaces, Pixel's greener tonal surface, and Nothing red `#D71921` with 22dp geometry.

### Compatibility guardrails

All changes remain Java/XML and Android 4.4.4 / API 19 compatible. No AndroidX, new runtime libraries, root, live blur, or OS-level theme hooks are introduced. The focus indicator is a static drawable and the dock uses a static GradientDrawable to avoid expensive blur or continuous animation on the QINS hardware.
