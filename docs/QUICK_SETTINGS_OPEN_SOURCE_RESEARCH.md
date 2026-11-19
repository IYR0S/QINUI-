# Quick Settings research and implementation notes

Research date: 2026-11-19

This review is scoped to the QINS1+ launcher constraints: Android 4.4.4 / API 19, Java + XML, no AndroidX, no new runtime dependencies, no root, and complete D-pad / OK / Back / physical-softkey operation.

## Executive decision

Do not transplant a modern notification-shade application into QINS. The best open-source projects found are either modern Compose applications requiring Android 10+ / API 28+, or GPL/MPL projects whose code is not a clean fit for this Java/API-19 launcher. Instead, reuse their documented interaction and geometry patterns, and implement the QINS panel with small original API-19 Java views.

The Nothing Quick Settings regression is not a single corner-radius bug. The earlier implementation combined:
- a full-width square translucent panel background;
- 16dp tile corners and 18dp card corners;
- compact Nothing tiles with an 8dp radius;
- two narrow cards containing rotated framework SeekBars;
- track drawables whose intrinsic dimensions depend on the rotated SeekBar's measurement;
- focus and live-state refresh paths that can replace the initial tile surface.

That structure made the theme look boxy and made the vertical slider geometry fragile. The replacement uses one rounded outer panel, consistent round-square tiles, and original vertically drawn pill sliders whose drawing, touch axis and D-pad axis all use the same coordinate system.

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

## What is being applied in QINS

- One rounded Nothing panel surface with a consistent 28dp outer radius.
- Shared Nothing shape tokens: 28dp panel/card, 24dp tiles, 14dp icon wells, 22dp slider treatment.
- Compact Nothing Quick Settings also uses a 28dp outer surface and 24dp shortcut surfaces.
- Expanded Nothing tiles and the media card use the same rounded visual language.
- Brightness and volume are now original Java pill-slider views. They draw a vertical capsule and its fill directly; they do not rotate a horizontal SeekBar or depend on drawable intrinsic sizes.
- Touch coordinates and D-pad Up/Down use the same vertical progress direction.
- D-pad Left/Right moves between the tile grid and the two sliders; brightness and volume have explicit focus IDs.
- The two sliders have an actual visual gap and circular icon affordances.
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
