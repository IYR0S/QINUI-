package com.qins.uistudio;

import android.content.Context;
import android.content.SharedPreferences;

/** Shared color palette and launcher appearance presets; API 19 compatible. */
public final class ThemeManager {
    private static final String PREFS = "qins_theme";
    private static final String KEY = "selected_theme";
    private static final String APPEARANCE_PREFS = "qins_interface";
    private static final String APPEARANCE_KEY = "appearance";
    public static final String APPEARANCE_GALAXY = "galaxy";
    public static final String APPEARANCE_PIXEL = "pixel";
    public static final String APPEARANCE_NOTHING = "nothing";

    private ThemeManager() {}

    public static final class Palette {
        public final String id;
        public final String title;
        public final int background;
        public final int surface;
        public final int primary;
        public final int accent;
        public final int text;
        public final int muted;

        Palette(String id, String title, int background, int surface, int primary, int accent, int text, int muted) {
            this.id = id; this.title = title; this.background = background; this.surface = surface;
            this.primary = primary; this.accent = accent; this.text = text; this.muted = muted;
        }
    }

    /** Visual tokens for the actual QINS home screen and quick panel. */
    public static final class Appearance {
        public final String id;
        public final String title;
        public final int background;
        public final int backgroundAlt;
        public final int surface;
        public final int surfaceAlt;
        public final int tile;
        public final int tileActive;
        public final int accent;
        public final int text;
        public final int muted;
        public final int border;
        public final int focus;
        public final float cornerDp;
        public final boolean monochrome;
        public final boolean useMonoType;

        Appearance(String id, String title, int background, int backgroundAlt,
                   int surface, int surfaceAlt, int tile, int tileActive,
                   int accent, int text, int muted, int border, int focus,
                   float cornerDp, boolean monochrome, boolean useMonoType) {
            this.id=id; this.title=title; this.background=background; this.backgroundAlt=backgroundAlt;
            this.surface=surface; this.surfaceAlt=surfaceAlt; this.tile=tile; this.tileActive=tileActive;
            this.accent=accent; this.text=text; this.muted=muted; this.border=border; this.focus=focus;
            this.cornerDp=cornerDp; this.monochrome=monochrome; this.useMonoType=useMonoType;
        }
    }

    private static final Palette[] PALETTES = new Palette[] {
        new Palette("ocean", "Ocean", 0xFFF3F5F8, 0xFFFFFFFF, 0xFF263B63, 0xFF4C78C2, 0xFF172033, 0xFF586579),
        new Palette("midnight", "Midnight", 0xFF151B27, 0xFF222B3B, 0xFF8EADEB, 0xFF75A7FF, 0xFFF2F5FC, 0xFFB2BED0),
        new Palette("forest", "Forest", 0xFFF0F5F1, 0xFFFFFFFF, 0xFF245B45, 0xFF398565, 0xFF183328, 0xFF587267)
    };

    private static final Appearance[] APPEARANCES = new Appearance[] {
        // One UI-inspired: soft blue accent, generous rounded surfaces and a calm neutral canvas.
        new Appearance(APPEARANCE_GALAXY, "Galaxy / One UI",
            0xFFF1F3F8, 0xFFE8ECF4, 0xFFFCFCFE, 0xFFE9EDF5,
            0xFFE6EAF2, 0xFFDCE7FB, 0xFF356FE5, 0xFF171A22, 0xFF697386,
            0xFFD7DDE8, 0xFF9DBAF4, 26f, false, false),
        // Pixel-inspired Material 3 Expressive: tonal lavender surfaces and a vivid violet accent.
        new Appearance(APPEARANCE_PIXEL, "Pixel / Material",
            0xFFF4F0F8, 0xFFEAE3F2, 0xFFFFFBFF, 0xFFEDE6F5,
            0xFFEAE3F2, 0xFFDCD0F0, 0xFF6750A4, 0xFF211B2B, 0xFF6E6578,
            0xFFD8CDE3, 0xFFBDA9E8, 30f, false, false),
        // Nothing-inspired: monochrome, high contrast and tight geometry with restrained red detail.
        new Appearance(APPEARANCE_NOTHING, "Nothing",
            0xFF101112, 0xFF191B1D, 0xFF202224, 0xFF292C2E,
            0xFF242729, 0xFF383B3D, 0xFFE3343A, 0xFFF4F4F2, 0xFFA4A7A8,
            0xFF3C4042, 0xFF777B7D, 8f, true, true)
    };

    public static Palette[] getPalettes() { return PALETTES.clone(); }

    public static Palette getPalette(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String selected = prefs.getString(KEY, "ocean");
        for (Palette palette : PALETTES) if (palette.id.equals(selected)) return palette;
        return PALETTES[0];
    }

    public static boolean apply(Context context, String id) {
        for (Palette palette : PALETTES) {
            if (palette.id.equals(id)) {
                return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .edit().putString(KEY, id).commit();
            }
        }
        return false;
    }

    public static Appearance[] getAppearances() { return APPEARANCES.clone(); }

    public static Appearance getAppearanceById(String id) {
        for (Appearance appearance : APPEARANCES) if (appearance.id.equals(id)) return appearance;
        return APPEARANCES[0];
    }

    public static Appearance getAppearance(Context context) {
        String selected = context.getSharedPreferences(APPEARANCE_PREFS, Context.MODE_PRIVATE)
                .getString(APPEARANCE_KEY, APPEARANCE_GALAXY);
        for (Appearance appearance : APPEARANCES) {
            if (appearance.id.equals(selected)) return appearance;
        }
        return APPEARANCES[0];
    }

    public static boolean applyAppearance(Context context, String id) {
        for (Appearance appearance : APPEARANCES) {
            if (appearance.id.equals(id)) {
                return context.getSharedPreferences(APPEARANCE_PREFS, Context.MODE_PRIVATE)
                    .edit().putString(APPEARANCE_KEY, id).commit();
            }
        }
        return false;
    }
}
