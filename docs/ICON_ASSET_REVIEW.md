# Icon asset source review

This review covers the icon-pack sources suggested for the QINS UI Studio OEM themes. It separates artwork licensing, Android compatibility and actual visual fit. No third-party artwork or closed-source Nothing APK assets have been copied into the launcher.

## Android 4.4 / API 19 integration rules

- Do not add a modern icon-pack APK or launcher library as a runtime dependency.
- API 19 does not natively support Android VectorDrawable, AdaptiveIconDrawable or the Android 13 monochrome adaptive-icon layer.
- Selected vector artwork must be rasterized to PNG before packaging. Keep the original package/activity mapping from appfilter.xml and fall back to the installed application's own icon when there is no match.
- Keep the icon source and renderer distinct per OEM theme. A generic monochrome pack is not a substitute for Nothing OS or Samsung One UI artwork.
- Do not download icons at runtime. Bundle only assets with clear redistribution terms and include the required license/attribution notices.

## Nothing sources

### Official Nothing Icon Pack

- Google Play: https://play.google.com/store/apps/details?id=com.nothing.icon
- APKMirror: https://www.apkmirror.com/apk/nothing-technology-limited/nothing-icon-pack/

The Play listing says Nothing Launcher 2.1.0 or newer is required and lists the package as Nothing Technology Limited's monochrome icon pack. APKMirror lists version 1.0.2 as Android 13+ (noarch, 480dpi; uploaded July 22, 2024). This is useful as a visual reference, but it is proprietary and is not compatible with QINS Android 4.4. Do not extract and redistribute its icon artwork in QINUI without permission.

### Nothing IconGen

Repository: https://github.com/linuxct/nothing-icongen

This is the closest visual reference for the newer translucent Nothing OS 5.0 direction. Its README requires a Nothing phone on Android 17 / API 37 with Nothing Launcher 5.0 or newer, plus ADB or InstallerX Revived with Shizuku for the installation path. It creates and applies a generated icon pack, but it is not a drop-in QINS library. The source is AGPL-3.0; do not copy its implementation into QINUI without explicitly addressing the copyleft obligations. Use it as a behavior/visual reference only.

### Nothing Icon Lab

Repository: https://github.com/Anndy999/Nothing-Icon-Lab

Apache-2.0 source for a local monochrome icon renderer and exporter. The project requires Android 13 / API 33 or newer. It exports a ZIP containing appfilter.xml and per-app PNGs (final, native monochrome, forced monochrome, and fallback folders). Its documented rendering target is a white circular plate with a black glyph, rather than the translucent OS 5.0 IconGen appearance. Its app code is not runnable on QINS, but the renderer/export format is the best permissively licensed technical reference for a future API-19 PNG icon-pack import path. The repository's curated icon PNG set is not included in Git; do not assume those artwork files are available under the source-code license.

### NothingMuch — Nothing Brand Colors

Repository: https://github.com/TeamNothingMuch/NothingMuch

GitHub describes this as an icon pack based on Nothing Brand Colors. The repository includes an appfilter.xml, a large icon-name resource array and app drawable resources. Its README still contains the Blueprint dashboard template, so the project metadata and actual pack resources should be treated separately. The repository's LICENSE.md is CC BY-SA 4.0 and the app module has minSdk 26; its VectorDrawable resources cannot be loaded directly on API 19. Selected artwork could be rasterized to PNG, but any shared/adapted assets must retain attribution and comply with ShareAlike. This is a candidate for a distinct Nothing Brand Colors option, not a stock Nothing OS 5.0 icon set.

## Samsung / One UI sources

### OneUIProject — oneui-icons

Repository: https://github.com/OneUIProject/oneui-icons

MIT-licensed collection of 883 icons found in Samsung apps. The available resources are VectorDrawable XML and the library declares minSdk 23; this is not a complete launcher app-icon pack. It is useful for authentic Samsung-style system/control glyphs (Wi-Fi, Bluetooth, brightness, DND, flashlight, sound and settings). A curated subset can be converted to PNG and used in QINS Quick Settings without a runtime dependency. Keep the MIT notice with any copied assets.

### KuatoDev — OneUI-Icons

Repository: https://github.com/KuatoDev/OneUI-Icons

This is an app icon pack with an appfilter mapping. Its README identifies API 21+ and says v1.3 converted its PNG assets to VectorDrawable, while adaptive icons are Android 8.0+ only. No LICENSE file or explicit asset redistribution terms were found in the repository during this review. Do not bundle its artwork unless the author clarifies the license.

### Muntasir2001 — OneUI Icon Pack

Submitted URL: https://github.com/Muntasir2001/OneUI-Icon-Pack

GitHub returned 404 for this URL during review, so its files and license could not be verified.

## Other open icon packs already reviewed

### Lawnchair — Lawnicons

Repository: https://github.com/LawnchairLauncher/lawnicons

Apache-2.0, with SVG artwork and an appfilter mapping. It is a general outlined monochrome pack rather than exact Pixel stock artwork. It remains a useful optional/curated source for Pixel, provided selected SVGs are rasterized to PNG.

### Arcticons

Canonical repository: https://github.com/Arcticons-Team/Arcticons
Submitted alias: https://github.com/donnnno/Arcticons

The project contains a large SVG collection and appfilter mapping. Its README distinguishes GPL-3.0 software from CC BY-SA 4.0 icon artwork. Selected artwork therefore needs attribution and ShareAlike handling, and it must be rasterized to PNG for API 19. Its hand-drawn line style is not Nothing's dot-matrix/translucent artwork.

## Recommended source strategy

- **Nothing OS-style home icons:** use Nothing Icon Lab's permissively licensed renderer/export format as the technical starting point for a QINS-compatible PNG pipeline, while designing the translucent treatment independently for API 19. Keep NothingMuch's Brand Colors pack as an optional separate style, with CC BY-SA compliance.
- **Samsung One UI controls:** use a curated PNG subset from OneUIProject/oneui-icons under MIT; do not add its minSdk-23 library dependency.
- **Pixel app icons:** keep Lawnicons as an optional curated monochrome source; do not present it as Google's own stock icons.
- **Default/fallback:** always retain the installed app's original icon where the selected pack has no exact package/activity match.

## Current integration status

The API-19 theme-scoped resolver is integrated and uses installed icons as the safe fallback. The resolver caches each theme's mapping and decoded PNGs, supports exact component mappings plus explicit package mappings, and applies a custom monochrome treatment to Nothing icons. The four appfilter files remain intentionally empty until a specific asset set has verified redistribution rights. No proprietary Nothing APK artwork or unlicensed icon artwork has been bundled. The next step is to add a curated PNG set with per-pack license notices, then validate icon coverage and memory use on QINS1+.
