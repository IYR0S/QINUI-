# Icon asset source review

This review covers the icon-pack repositories suggested for the QINS UI Studio OEM themes. It does not copy third-party artwork into the APK.

## Android 4.4 / API 19 integration rules

- Do not add the icon-pack APK as a runtime dependency.
- Do not rely on Android VectorDrawable at runtime; API 19 does not natively support it.
- Convert selected source artwork to PNG before packaging, retain the original package/component mapping, and fall back to the installed application's icon when there is no match.
- Keep each OEM theme's icon source separate. A generic monochrome pack is not a substitute for Samsung One UI or Nothing's own artwork.
- No runtime network download is permitted for icons.

## Repositories reviewed

### Nothing Developer Programme — Nothing Icon Pack

Submitted URL: https://github.com/Nothing-Developer-Programme/Nothing-Icon-Pack

GitHub returned 404 for this repository URL during review. No assets or license terms could be verified, so nothing from this source was copied.

### Lawnchair — Lawnicons

Repository: https://github.com/LawnchairLauncher/lawnicons

The repository identifies itself as an open-source, monochrome outlined icon pack and is marked Apache-2.0. It includes SVG artwork and an appfilter mapping. This is the most straightforward of the submitted sources to adapt technically: selected SVG assets can be rasterized to PNG and their package/component mappings preserved. Its visual language is a general outlined monochrome pack, not a faithful Pixel stock-icon set, so it should be an optional/curated source rather than automatically replacing every Pixel icon.

### Arcticons

Canonical repository: https://github.com/Arcticons-Team/Arcticons
Submitted alias: https://github.com/donnnno/Arcticons

The project contains a large SVG icon collection and an appfilter mapping. Its README states that the software uses GPL-3.0 while the icon artwork is licensed separately under CC BY-SA 4.0. Any selected artwork therefore needs attribution and ShareAlike handling; the icon files must also be rasterized to PNG for API 19. The hand-drawn line style is distinct from Nothing's native dot-matrix artwork, so it should not be described as an OEM Nothing pack.

### Muntasir2001 — OneUI Icon Pack

Submitted URL: https://github.com/Muntasir2001/OneUI-Icon-Pack

GitHub returned 404 for this repository URL during review. No assets or license terms could be verified, so nothing from this source was copied. A Samsung-specific pack with an explicit asset license is still needed before bundling One UI artwork.

## Current integration status

The launcher continues to use the installed app's own icon as its safe default. No third-party app icon artwork has been bundled yet. This is intentional: the two OEM-branded URLs were unavailable, and the two accessible sources require different visual expectations and asset handling. A later asset-import step can add a curated, license-tracked PNG subset without changing the API-19 runtime dependency profile.
