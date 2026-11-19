Theme icon assets are optional and are loaded at runtime on Android API 19.

Each pack directory may contain appfilter.xml and drawable-nodpi/<name>.png.
appfilter.xml uses <resources><item component="ComponentInfo{package/class}" drawable="name" /></resources>.
Only exact component mappings are accepted. Keep all image assets as PNG; VectorDrawable and AdaptiveIconDrawable are not supported by API 19.

The launcher currently keeps PackageManager icons as fallback. Nothing OS applies a monochrome treatment to those installed icons when no licensed override is present. No proprietary Nothing artwork or unlicensed icon artwork is bundled.

Pack IDs: nothing-os, one-ui, pixel. nothing-brand is reserved for a separately selected CC BY-SA pack and is not currently enabled as a runtime theme.