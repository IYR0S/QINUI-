API-19 icon packs use one compact PNG atlas per visual theme plus appfilter.xml.

Atlas layout: 6 columns × 4 rows, 32 × 32 pixels per cell. The runtime resolver crops the matching cell and caches the resulting bitmap. No VectorDrawable, AdaptiveIconDrawable, AndroidX, or runtime dependency is required.

Drawable IDs, in atlas order:
phone, contacts, settings, wifi, bluetooth, camera,
gallery, messages, calendar, music, browser, clock,
files, email, store, video, maps, calculator,
downloads, recorder, flashlight, hotspot, sound, notes.

Pack folders: one-ui, pixel, nothing-os.
The PNGs are original artwork; see ASSET_LICENSES.txt. App mappings are package-level fallbacks and can be extended with exact component mappings.
