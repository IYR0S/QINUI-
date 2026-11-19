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


## Detailed component system — 2026-11-19

This section translates current public design guidance and visible OEM interface patterns into QINS-specific API-19 values. The dp values below are **implementation decisions for QINS**, not claims that Samsung, Google or Nothing publish a universal numeric radius for every control. A corner radius is a distance, not an angle; the apparent roundness also depends on the component's width and height.

### Shape tokens

| Component | Galaxy / One UI direction | Pixel / Material direction | Nothing direction |
| --- | ---: | ---: | ---: |
| Focus cards / notification cards | 28dp | 28dp | 18dp |
| Quick Settings tiles | 24dp | 26dp | 16dp |
| Icon treatment | Circular accent wells for compact controls | Glyph directly on tonal tile | Glyph directly on dark tile; no nested bubble |
| Slider containers | 26dp | 22dp | 10dp |
| Home app row | Transparent; no outer dock bubble | Transparent; no outer dock bubble | Transparent; no outer dock bubble |

These tokens are centralized in ThemeManager.Appearance. Older generic cornerDp remains available for legacy editor surfaces while new launcher components use role-specific tokens.

### Surface depth, wallpaper and transparency

- **Galaxy / One UI:** use a clearly separated focus block against generous negative space. Light and dark surfaces are distinct; a soft highlight and a low-contrast shadow communicate that a card sits above the wallpaper. The expanded panel reserves wide focus blocks for Wi-Fi and Bluetooth; compact shortcuts use circular icon controls without another rectangular card. A blur/dim layer is appropriate when a related panel is opened, but shadow, blur and dim should not be stacked indiscriminately.
- **Pixel / Material 3:** distinguish elevation first with tonal surface colors and only then with a subtle shadow. Active tiles use a stronger accent/tonal fill; inactive tiles remain visible as intentional containers rather than almost-transparent outlines.
- **Nothing:** use near-black or near-white monochrome surfaces, crisp borders, restrained red accent and less exaggerated rounding. The app icons themselves should remain visible without a second circle or pill behind each one.
- **QINS API 19 implementation:** Quick Settings uses a theme-matched translucent scrim over the wallpaper. There is no real-time blur; this preserves a degree of wallpaper visibility while keeping text and controls legible. Focus blocks use a restrained static gradient/highlight and a soft shadow. Galaxy keeps circular wells only for its compact shortcuts; Pixel and Nothing draw the glyph directly on the tile, without a nested badge. The launcher app row itself has no background capsule, and app icons are no longer placed in an extra Nothing-only circular badge.

### Sliders and control geometry

- **Galaxy:** horizontal brightness and volume cards, broad track, circular thumb, separate connectivity cards for Wi-Fi and Bluetooth. The controls should feel substantial but not balloon-like.
- **Pixel:** horizontal Material-style controls, with a larger rounded-rectangle thumb and a solid tonal card surface. Brightness and media volume keep different semantic cards rather than looking like duplicate widgets.
- **Nothing:** paired vertical brightness and media-volume rails, square/rectangular thumb, compact control column and a tighter card radius. Brightness fill is accent red; volume keeps its own rail and thumb treatment.
- **All themes:** the control's visible filled level, keyboard focus and current value must be distinguishable without relying on color alone. Left/right or up/down hardware keys must change values in the intuitive direction.

### Icon policy

QINS continues to use each installed app's own launcher icon for the home and app drawer, preserving app identity. UI controls use QINS-drawn glyphs so the launcher does not require AndroidX, VectorDrawable framework features newer than API 19, or a heavyweight icon dependency. Exact stock Samsung/Nothing third-party app icon matching is a separate asset-mapping task and requires a properly licensed icon pack; the current control glyphs and surface system do not depend on that pack.

### References

- Samsung One UI Design Guide — Focus Blocks and large rounded containers: https://design.samsung.com/global/contents/one-ui/download/oneui_design_guide_eng.pdf
- Samsung Developer — Visual depth (blur, dim and soft shadow; avoid exaggerated 3D): https://developer.samsung.com/one-ui/structure/visual-depth.html
- Samsung Developer — Basic layout and viewing / interaction areas: https://developer.samsung.com/one-ui/layout/basic.html
- Samsung One UI 8.5 update notes — transparent blur and floating elements: https://doc.samsungmobile.com/SM-S926B/029357240224/eng.html
- Android Developers — Material 3 shape scale and tonal elevation plus shadow elevation: https://developer.android.com/develop/ui/compose/designsystems/material3
- Nothing official community announcement — Nothing OS 4.0 refined component system, clearer Quick Settings and refreshed first-party icons: https://nothing.community/en/d/42205-introducing-nothing-os-40
- Nothing OS 4.0 general release notes: https://nothing.community/en/d/47265-nothing-os-40-general-release/2

Compatibility remains Android 4.4.4 / API 19, Java and XML. These visual choices are implemented as lightweight Canvas and Drawable operations; no AndroidX, live blur, root, or new runtime dependency is introduced.
