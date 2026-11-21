# OEM-inspired Quick Settings layouts

The launcher keeps its existing home-screen appearance setting and maps it to one of three dedicated Quick Settings layouts:

| Launcher appearance | Quick Settings layout |
| --- | --- |
| Pixel | Android 17-inspired forest / mint layout |
| Galaxy / One UI | One UI 8-inspired navy card layout |
| Nothing | Nothing OS 5-inspired OLED / red dot-matrix layout |

## Android 17-inspired
- Mint horizontal brightness control.
- Two wide Internet and Bluetooth pills, reflecting live Android state.
- Two rows of four circular shortcuts.
- Compact music action bar.

## One UI 8-inspired
- Wi-Fi and Bluetooth pills.
- Rounded navy 4x2 shortcut card.
- Separate horizontal brightness and media-volume sliders with a side action.

## Nothing OS 5-inspired
- Four circular top shortcuts with a red mute accent.
- Asymmetric media card and two vertical dot-matrix sliders.
- Five-slot notification dock using real notification app icons when Notification Access is enabled.

## Compatibility and behavior
- Pure Java framework widgets; no AndroidX or third-party dependencies.
- Android 4.4.4 / API 19 compatible.
- D-pad focusable controls; vertical Nothing sliders use Up/Down for adjustment and Left/Right for navigation.
- Wi-Fi, Bluetooth, brightness, volume and media actions use the existing API-19 controller / notification bridge.
- Protected actions that Android does not allow a normal app to toggle directly are routed to the relevant action or settings screen rather than showing a fake enabled state.
