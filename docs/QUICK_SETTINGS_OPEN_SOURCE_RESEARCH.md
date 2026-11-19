# Quick Settings research and implementation notes

Research date: 2026-11-19

This review is scoped to the QINS1+ launcher constraints: Android 4.4.4 / API 19, Java + XML, no AndroidX, no new runtime dependencies, no root, and complete D-pad / OK / Back / physical-softkey operation.

## Executive decision

Do not transplant a modern notification-shade application into QINS. The best open-source projects found are either modern Compose applications requiring Android 10+ / API 28+, or GPL/MPL projects whose code is not a clean fit for this Java/API-19 launcher. Instead, reuse their documented interaction and geometry patterns, and implement the QINS panel with small original API-19 Java views.

The current Nothing issue is primarily a proportion and panel-surface problem, not just a slider-width problem. The earlier layout used a floating rounded outer card, exposed wallpaper gutters, and a fixed 112dp slider dock beside a weighted tile group. That prevented the slider pair from scaling with the display width.

The updated layout uses a full-width translucent scrim (API 19 cannot provide modern live blur), keeps only small internal content padding, and gives the tile group and paired slider group equal width weights. Each slider remains an original vertically drawn pill view whose drawing, touch axis and D-pad axis use the same coordinate system.

## Nothing OS 5 visual/functional reference (not source code)

Nothing's official community feature post describes:
- a broader refresh to typography, icons, spacing, materials and motion;
- the Geist system typeface;
- an option to separate Notifications and Quick Settings;
- the split-panel interaction: notifications from the left, Quick Settings from the right;
- more consistent settings and status-bar presentation.

Independent hands-on coverage of Nothing OS 5 reports:
- the media player at the top of the separate Quick Settings panel;
- vertical brightness and volume sliders;
- editable / resizable Quick Settings tiles;
- Bluetooth device interaction simplified around the connected device.

These are used only as public visual/feature references. Nothing's proprietary System UI source, APK assets, fonts and icons are not copied or redistributed.

References:
- https://nothing.community/d/61730-nothing-os-50-new-features-explained
- https://www.91mobiles.com/reviews/nothing-os-5-hands-on/
- https://gadgets.beebom.com/guides/nothing-os-5-0-features-list

## Open-source projects inspected

### 1. SuperShade — Apache-2.0

Repository: https://github.com/thejaustin/SuperShade

Relevant source inspected:
- https://github.com/thejaustin/SuperShade/blob/main/app/src/main/java/com/supershade/ui/shade/BrightnessSlider.kt
- https://github.com/thejaustin/SuperShade/blob/main/app/src/main/java/com/supershade/ui/shade/VolumeSlider.kt
- https://github.com/thejaustin/SuperShade/blob/main/app/src/main/java/com/supershade/ui/shade/TileCard.kt
- https://github.com/thejaustin/SuperShade/blob/main/app/src/main/java/com/supershade/ui/shade/QuickSettingsGrid.kt

Useful findings:
- Its README documents a 44dp pill-slider track, active fill, and clear brightness / volume affordances.
- The Nothing theme in TileCard uses rounded surfaces rather than sharp rectangles; active tiles use a bright surface with dark foreground, with a restrained red active indicator.
- Tile shapes are treated as explicit design tokens rather than being scattered across the panel.

Compatibility:
- The project is Jetpack Compose/Kotlin and requires Android 10+ (Android 12+ recommended), JDK 17, and Shizuku for some system actions.
- It is not a drop-in source for API 19. No Compose, AndroidX, Shizuku or modern dependency was added to QINS.
- Apache-2.0 permits reuse with notices, but this implementation deliberately uses the design lessons only; no SuperShade source file is copied.

### 2. OpenShade — GPL-3.0

Repository: https://github.com/EminBayramov91/OpenShade

Useful findings:
- Its compact/expanded two-stage shade is a good interaction model.
- It documents clear separation between notification shade and Quick Settings, brightness controls, settings shortcuts, and a translucent panel.
- It explicitly documents Android's restrictions on changing Wi-Fi, Bluetooth, mobile data and airplane mode from an ordinary application without elevated access.

Compatibility:
- The inspected build uses Kotlin, Jetpack Compose, minSdk 28, compileSdk 35 and Shizuku.
- Not compatible with API 19 and not suitable for direct code reuse in this project. GPL-3.0 is also a copyleft license; no OpenShade code is copied.

### 3. QuickTiles — GPL-3.0

Repository: https://github.com/flxholle/QuickTiles

Useful findings:
- It provides a broad catalogue of quick actions and links to upstream tile implementations.
- It demonstrates that quick-setting actions should be modular and have clear, single-purpose behavior.

Compatibility:
- It creates Android Quick Settings tiles through the platform tile API; it is not a custom visual shade or launcher panel.
- GPL-3.0 and its upstream dependency credits need to be respected. No QuickTiles source is copied into QINS.

### 4. OpenCamera VerticalSeekBar — MPL-1.1

Repository: https://github.com/almalence/OpenCamera
Source inspected: https://github.com/almalence/OpenCamera/blob/master/src/com/almalence/ui/VerticalSeekBar.java

Useful findings:
- It is an older Java implementation of a vertical SeekBar and shows the traditional rotated-canvas approach.

Why it is not reused:
- Rotating a horizontal SeekBar requires swapping measurement dimensions and manually mapping touch coordinates. That is precisely the fragile geometry used by the previous QINS Nothing slider.
- MPL-1.1 has file-level source obligations. QINS does not need to import this code because a small original Canvas view is simpler and avoids introducing another license surface.

### 5. Android Open Source Project SystemUI — Apache-2.0

API-19-era source inspected:
- https://android.googlesource.com/platform/frameworks/base/+/ab51002/packages/SystemUI/src/com/android/systemui/statusbar/phone/QuickSettings.java
- https://android.googlesource.com/platform/frameworks/base/+/52277428d54cc71f0cde3f1efa035f1e6742b900/packages/SystemUI/src/com/android/systemui/statusbar/phone/QuickSettingsTileView.java

Useful findings:
- The Android 4.4-era tile view is a small FrameLayout wrapper around a tile-specific content layout and supports column spans.
- QuickSettings separates tile presentation from a model/controller that refreshes the view when actual device state changes.
- The newer AOSP QS documentation makes the state lifecycle explicit: an action changes device state; the controller reports the change; the tile refreshes its state; then the view maps that state to color, icon and label. The visual view should not optimistically become the source of truth.

Compatibility:
- The 4.4 source is close to QINS's API level and is Apache-2.0, but the classes are part of privileged SystemUI and depend on internal framework services. They are not directly reusable inside a normal launcher APK.
- QINS adopts the state-to-view separation as an architectural rule, not the privileged SystemUI implementation.

Modern AOSP lifecycle reference:
- https://github.com/aospapp/aosp/blob/main/frameworks/base/packages/SystemUI/docs/qs-tiles.md

## OEM comparison and geometry decisions (2026-11-20)

### Nothing OS 5
Public hands-on coverage confirms a separate Quick Settings panel, a minimized media player near the top, vertical brightness and volume sliders, and resizable tiles. The QINS layout now gives the tile group and paired slider group equal horizontal weights (approximately 50/50), rather than a fixed 112dp dock. The paired sliders retain an explicit inter-slider gap.

The outer shade surface now spans the full viewport width. Its background is a translucent full-bleed scrim over the wallpaper, not a rounded floating card that exposes narrow wallpaper gutters at the left and right. Expanded Nothing tiles and the media card also use restrained alpha so the Lucent-style surfaces reveal a hint of the wallpaper. This is a deliberate API-19 approximation: modern live wallpaper blur is unavailable to this ordinary launcher view. Small internal content padding remains, so controls do not touch the display edge.

### Pixel / Material 3 Expressive
Android 16 QPR1 references show an updated rectangular brightness surface with a prominent upright grip. Inactive tiles remain pill-shaped; enabled tiles morph to rounded rectangles or squares. QINS keeps the horizontal rail and now draws the narrow upright grip symmetrically above and below it. Pixel-specific corner tokens were tightened to 22dp cards, 20dp tiles, 12dp icon wells and 16dp slider surfaces. These are QINS tuning values, not Google's published dp specifications.

### Samsung One UI
Samsung's One UI 8.5 documentation emphasizes customizable control layout and placing frequently used controls high in the panel. Its slider guidance specifies immediate feedback and distinct normal, focus and disabled states. QINS keeps Galaxy's own component sizing and shape tokens separate from Pixel/Nothing; the current pass does not apply Pixel's tighter radii or Nothing's half-width slider dock to Galaxy.

References for the comparison:
- Pixel Android 16 QPR1 redesign: https://9to5google.com/2025/05/20/android-16-qpr1-quick-settings/
- Pixel slider refinements: https://9to5google.com/2025/06/11/android-16-qpr1-sliders/
- Pixel final QPR1 rollout: https://9to5google.com/2025/09/03/android-16-qpr1-pixel/
- Nothing OS 5 hands-on: https://www.91mobiles.com/reviews/nothing-os-5-hands-on/
- Samsung One UI 8.5 Quick Panel customization: https://www.samsung.com/uk/support/mobile-devices/how-to-customise-the-quick-panel-on-the-samsung-galaxy-s26-series/
- Samsung One UI slider design guidance: https://design.samsung.com/global/contents/one-ui/download/oneui_design_guide_eng.pdf

## What is being applied in QINS

- Nothing expanded shade uses a full-viewport translucent scrim, with no outer rounded-card gutters; its tile and media surfaces have restrained translucency to echo the OS 5 Lucent treatment.
- Nothing tile group and the paired brightness/volume slider group share the available width equally; the pair therefore occupies approximately half of the panel width.
- The two Nothing sliders retain a real visible gap and individual icon affordances.
- Nothing's custom vertical pill sliders remain original Java views; drawing, touch mapping and D-pad Up/Down share one coordinate system.
- Pixel's horizontal rail has a slim upright grip protruding symmetrically above and below the track.
- Pixel inactive expanded tiles use a pill radius (36dp for the 72dp-high tile); active tiles morph to the tighter 20dp rounded rectangle.
- Pixel-only component tokens are 22dp cards, 20dp active tiles, 12dp icon wells and 16dp slider surfaces. These are implementation choices, not claimed OEM measurements.
- Galaxy's brightness and volume rows were checked for matched 66dp card height, 34dp SeekBar height, equal weighted card width and matching level-fill treatment. Its 44dp circular icon wells remain true circles; Galaxy tokens and layout remain isolated from Pixel and Nothing.
- No AndroidX, Compose, Shizuku, root, proprietary Nothing assets, or new runtime dependencies are introduced.

## API-19 and device validation checklist

Build checks alone do not prove the UI is correct on QINS1+. Before release, verify on the physical device:
1. The outer Nothing panel and every tile/card have visible rounded corners; no square rectangular container leaks through.
2. Compact Quick Settings matches the Nothing theme and remains rounded after Wi-Fi/Bluetooth/sound state refresh.
3. Brightness slider: Up increases, Down decreases, minimum/maximum clamp correctly, and the physical display brightness changes.
4. Volume slider: Up increases, Down decreases, minimum/maximum clamp correctly, and media volume changes.
5. If touch is available, dragging top-to-bottom decreases and bottom-to-top increases; no axis inversion.
6. Left/Right moves between the grid, brightness and volume without losing focus.
7. The panel fits the actual QINS screen without clipping; scrolling remains available for lower controls.
8. Hebrew/RTL labels remain legible and do not reverse the physical-key progress direction.
9. Repeated open/close, app resume and live state broadcasts do not reset styling or progress.
10. Pixel and Galaxy layouts remain unchanged by Nothing-specific shape changes.

## Licensing rule

Only code/assets with an explicit license compatible with the intended distribution may be copied. A public GitHub repository is not automatically open source. Record the exact repository, file, license and required notices before any future code or asset import.
