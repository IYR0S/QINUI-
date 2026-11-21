package com.qins.uistudio;

import android.content.Context;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/** API-19 icon packs backed by compact, original PNG atlases and appfilter mappings. */
public final class ThemedIconResolver {
    private static final String[] ICON_NAMES = new String[] {
            "phone", "contacts", "settings", "wifi", "bluetooth", "camera",
            "gallery", "messages", "calendar", "music", "browser", "clock",
            "files", "email", "store", "video", "maps", "calculator",
            "downloads", "recorder", "flashlight", "hotspot", "sound", "notes"
    };
    private static final Map<String, Map<String, String>> MAPPINGS =
            new HashMap<String, Map<String, String>>();
    private static final Map<String, Bitmap> ATLAS = new HashMap<String, Bitmap>();
    private static final Map<String, Bitmap> ICONS = new HashMap<String, Bitmap>();

    private ThemedIconResolver() {}

    private static String packForTheme(String theme) {
        if (ThemeManager.APPEARANCE_GALAXY.equals(theme)) return "one-ui";
        if (ThemeManager.APPEARANCE_PIXEL.equals(theme)) return "pixel";
        return "nothing-os";
    }

    private static int iconIndex(String name) {
        for (int i = 0; i < ICON_NAMES.length; i++) if (ICON_NAMES[i].equals(name)) return i;
        return -1;
    }

    private static Map<String, String> loadMappings(Context context, String pack) {
        synchronized (MAPPINGS) {
            Map<String, String> cached = MAPPINGS.get(pack);
            if (cached != null) return cached;
        }
        Map<String, String> loaded = new HashMap<String, String>();
        InputStream input = null;
        try {
            input = context.getAssets().open("iconpacks/" + pack + "/appfilter.xml");
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(input, "UTF-8");
            int event;
            while ((event = parser.next()) != XmlPullParser.END_DOCUMENT) {
                if (event != XmlPullParser.START_TAG || !"item".equals(parser.getName())) continue;
                String component = parser.getAttributeValue(null, "component");
                String packageName = parser.getAttributeValue(null, "package");
                String drawable = parser.getAttributeValue(null, "drawable");
                if (iconIndex(drawable) < 0) continue;
                if (component != null && component.length() > 0) loaded.put(component, drawable);
                if (packageName != null && packageName.matches("[A-Za-z0-9_.]+")) {
                    loaded.put("package:" + packageName, drawable);
                }
            }
        } catch (Exception ignored) {
            // Missing or malformed mappings fall back to PackageManager icons.
        } finally {
            if (input != null) try { input.close(); } catch (Exception ignored) { }
        }
        synchronized (MAPPINGS) { MAPPINGS.put(pack, loaded); }
        return loaded;
    }

    private static Bitmap loadAtlas(Context context, String pack) {
        synchronized (ATLAS) {
            Bitmap cached = ATLAS.get(pack);
            if (cached != null && !cached.isRecycled()) return cached;
        }
        InputStream input = null;
        try {
            input = context.getAssets().open("iconpacks/" + pack + "/icons.png");
            Bitmap bitmap = BitmapFactory.decodeStream(input);
            if (bitmap == null) return null;
            synchronized (ATLAS) { ATLAS.put(pack, bitmap); }
            return bitmap;
        } catch (Exception ignored) {
            return null;
        } finally {
            if (input != null) try { input.close(); } catch (Exception ignored) { }
        }
    }

    /**
     * The supplied icon reference is a visual target, but the repository does not
     * contain a complete, verified per-app icon set matching it. Return null so
     * callers use PackageManager's original installed icon without partial packs.
     */
    public static Drawable loadMappedIcon(Context context, ResolveInfo info, String theme) {
        return null;
    }

    /** Preserve the device's original app icon until a complete licensed set exists. */
    public static Bitmap applyTheme(Bitmap source, String theme) {
        return source;
    }
}
