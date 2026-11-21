# API 19 theme-scoped icon pipeline

## Goal

Add optional OEM-specific launcher icons without changing the launcher architecture, adding a runtime dependency, or breaking Android 4.4.4 (API 19). Installed application icons remain the fallback and remain available as the default style.

## Pack layout

Keep each OEM pack isolated so a theme switch cannot accidentally reuse another theme's artwork:

```
app/src/main/assets/iconpacks/
  nothing-os/
    appfilter.xml
    drawable-nodpi/
      ic_com_example_app.png
    LICENSES.txt
  nothing-brand/
    appfilter.xml
    drawable-nodpi/
      ic_com_example_app.png
    LICENSES.txt
  one-ui/
    appfilter.xml
    drawable-nodpi/
      ic_com_example_app.png
    LICENSES.txt
  pixel/
    appfilter.xml
    drawable-nodpi/
      ic_com_example_app.png
    LICENSES.txt
```

Only add a pack directory when it contains artwork with verified redistribution rights. Do not place proprietary Nothing APK artwork or unlicensed OneUI-Icons artwork in the repository.

## Mapping format

Use a small, documented XML subset rather than depending on a modern icon-pack SDK. Each entry maps an exact Android package and optional activity class to a PNG filename. Example:

```xml
<resources>
    <item component="ComponentInfo{com.example.app/com.example.app.MainActivity}"
          drawable="ic_com_example_app" />
</resources>
```

The importer should normalize component names, prefer an exact package/activity match, then use a package-level match only when the pack explicitly supplies one. Unknown or malformed entries must be ignored safely.

## Runtime behavior

1. Resolve the active OEM icon style from the existing theme preference.
2. Look up the launcher's ResolveInfo component in that pack's mapping.
3. Decode the matching PNG from the pack's assets with BitmapFactory.
4. If no exact mapping exists, return PackageManager's installed application icon unchanged.
5. Cache decoded bitmaps by theme + asset path; clear or invalidate the theme-specific cache when the icon style changes.
6. Never download icons, request root, or attempt to load VectorDrawable / AdaptiveIconDrawable on API 19.

Keep bitmap dimensions bounded (a practical launcher target is 96–144 px depending on the device density) and decode only the selected icon rather than loading every pack image into memory. Handle missing assets, corrupt PNGs and out-of-memory conditions by falling back to the installed icon.

## Visual treatment by theme

- **Nothing OS:** create an original translucent/soft monochrome treatment inspired by the OS 5 visual language. Do not imitate or redistribute the official proprietary icon pack. Dot-matrix styling belongs in selected accents, not inside every app glyph.
- **Nothing Brand Colors:** keep this as a separate optional style from Nothing OS. Any adapted NothingMuch artwork must retain CC BY-SA 4.0 attribution and ShareAlike terms.
- **Samsung One UI:** use only a curated subset of OneUIProject/oneui-icons for system/control glyphs, rasterized to PNG. Preserve the MIT notice. These are not a complete launcher app-icon pack.
- **Pixel:** retain original app colors by default. An optional curated monochrome treatment can use permissively licensed artwork, but must not be described as Google's official icon set.

## Asset conversion and licensing gate

Convert selected SVG/VectorDrawable artwork to PNG outside the Android runtime, then commit the resulting PNGs and a per-pack license/attribution file. Before adding any asset, record its source URL, author/project, exact asset path, license, required attribution and any ShareAlike condition. Reject assets with no explicit redistribution license.

The official Nothing Icon Pack is visual reference only. nothing-icongen is AGPL-3.0 and targets a much newer Nothing platform; do not copy its implementation into this project as part of this pipeline. Nothing Icon Lab's Apache-2.0 renderer/export structure is a useful technical reference, but its app itself is not API-19 compatible.

## Acceptance tests

- Build with minSdk 19 and no new runtime dependencies.
- Launch with no icon-pack assets installed: every app still displays its installed icon.
- Switch all three OEM themes repeatedly: mappings and caches remain isolated.
- Test exact component match, package fallback, unmapped app, malformed XML, missing PNG and corrupt PNG.
- Confirm no touch-only controls are introduced; existing D-pad focus and launch behavior are unchanged.
- Compare memory use with the stock-icon fallback and verify scrolling remains responsive on QINS1+ hardware.

## Current status

This document defines the next implementation contract. It does not claim that the importer or third-party artwork has already been added. The current launcher remains safe with installed-icon fallback until the runtime resolver and a licensed, curated PNG set are implemented and tested.
