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
    private static final Map<String, Map<String, String>> ICON_PACK_CACHE =
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
        synchronized (ICON_PACK_CACHE) {
            Map<String, String> cached = ICON_PACK_CACHE.get(pack);
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
        synchronized (ICON_PACK_CACHE) { ICON_PACK_CACHE.put(pack, loaded); }
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

    public static Drawable loadMappedIcon(Context context, ResolveInfo info, String theme) {
        if (context == null || info == null || info.activityInfo == null) return null;
        String pack = packForTheme(theme);
        String packageName = info.activityInfo.packageName;
        String component = "ComponentInfo{" + packageName + "/" + info.activityInfo.name + "}";
        Map<String, String> mappings = loadMappings(context, pack);
        String drawableName = mappings.get("package:" + packageName);
        if (drawableName == null) drawableName = mappings.get(component);
        int index = iconIndex(drawableName);
        if (index < 0) return null;

        String cacheKey = pack + "/" + drawableName;
        Bitmap bitmap;
        synchronized (ICONS) { bitmap = ICONS.get(cacheKey); }
        if (bitmap == null || bitmap.isRecycled()) {
            Bitmap atlas = loadAtlas(context, pack);
            if (atlas == null || atlas.getWidth() < 192 || atlas.getHeight() < 128) return null;
            try {
                bitmap = Bitmap.createBitmap(atlas, (index % 6) * 32, (index / 6) * 32, 32, 32);
                synchronized (ICONS) { ICONS.put(cacheKey, bitmap); }
            } catch (RuntimeException ignored) { return null; }
        }
        return new BitmapDrawable(context.getResources(), bitmap);
    }

    /** Monochrome fallback treatment for installed icons without a matching PNG. */
    public static Bitmap applyTheme(Bitmap source, String theme) {
        if (source == null || source.isRecycled() ||
                !ThemeManager.APPEARANCE_NOTHING.equals(theme)) return source;
        Bitmap mono = Bitmap.createBitmap(source.getWidth(), source.getHeight(), Bitmap.Config.ARGB_8888);
        android.graphics.Canvas canvas = new android.graphics.Canvas(mono);
        android.graphics.Paint paint = new android.graphics.Paint(
                android.graphics.Paint.ANTI_ALIAS_FLAG | android.graphics.Paint.FILTER_BITMAP_FLAG);
        android.graphics.ColorMatrix matrix = new android.graphics.ColorMatrix();
        matrix.setSaturation(0f);
        paint.setColorFilter(new android.graphics.ColorMatrixColorFilter(matrix));
        canvas.drawBitmap(source, 0f, 0f, paint);
        return mono;
    }
}
