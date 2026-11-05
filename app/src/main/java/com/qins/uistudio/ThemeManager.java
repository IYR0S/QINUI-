package com.qins.uistudio;

import android.content.Context;
import android.content.SharedPreferences;

/** Small API-19-compatible theme store shared by every Studio screen. */
public final class ThemeManager {
    private static final String PREFS = "qins_theme";
    private static final String KEY = "selected_theme";
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

    private static final Palette[] PALETTES = new Palette[] {
        new Palette("ocean", "Ocean", 0xFFF3F5F8, 0xFFFFFFFF, 0xFF263B63, 0xFF4C78C2, 0xFF172033, 0xFF586579),
        new Palette("midnight", "Midnight", 0xFF151B27, 0xFF222B3B, 0xFF8EADEB, 0xFF75A7FF, 0xFFF2F5FC, 0xFFB2BED0),
        new Palette("forest", "Forest", 0xFFF0F5F1, 0xFFFFFFFF, 0xFF245B45, 0xFF398565, 0xFF183328, 0xFF587267)
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
}
