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
