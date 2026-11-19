package com.qins.uistudio;

import android.content.Context;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * API-19 icon-pack bridge. A theme may provide an appfilter.xml plus licensed PNGs.
 * Unmapped apps retain their installed icon; Nothing receives an original,
 * high-contrast monochrome treatment rather than a dependency on modern adaptive icons.
 */
public final class ThemedIconResolver {
    private static final Map<String, Map<String, String>> MAPPINGS =
            new HashMap<String, Map<String, String>>();
    private static final Map<String, Bitmap> ICONS = new HashMap<String, Bitmap>();

    private ThemedIconResolver() {}

    private static String packForTheme(String theme) {
        if (ThemeManager.APPEARANCE_GALAXY.equals(theme)) return "one-ui";
        if (ThemeManager.APPEARANCE_PIXEL.equals(theme)) return "pixel";
        return "nothing-os";
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
                if (drawable == null || !drawable.matches("[A-Za-z0-9_]+")) continue;
                if (component != null && component.length() > 0) loaded.put(component, drawable);
                if (packageName != null && packageName.matches("[A-Za-z0-9_.]+")) {
                    loaded.put("package:" + packageName, drawable);
                }
            }
        } catch (Exception ignored) {
            // An absent or malformed pack is a normal fallback condition.
        } finally {
            if (input != null) try { input.close(); } catch (Exception ignored) { }
        }
        synchronized (MAPPINGS) {
            MAPPINGS.put(pack, loaded);
        }
        return loaded;
    }

    public static Drawable loadMappedIcon(Context context, ResolveInfo info, String theme) {
        if (context == null || info == null || info.activityInfo == null) return null;
        String pack = packForTheme(theme);
        String packageName = info.activityInfo.packageName;
        String component = "ComponentInfo{" + packageName + "/" + info.activityInfo.name + "}";
        Map<String, String> mappings = loadMappings(context, pack);
        String drawableName = mappings.get(component);
        if (drawableName == null) drawableName = mappings.get("package:" + packageName);
        if (drawableName == null) return null;

        String cacheKey = pack + "/" + drawableName;
        Bitmap bitmap;
        synchronized (ICONS) {
            bitmap = ICONS.get(cacheKey);
        }
        if (bitmap == null || bitmap.isRecycled()) {
            InputStream png = null;
            try {
                png = context.getAssets().open("iconpacks/" + pack +
                        "/drawable-nodpi/" + drawableName + ".png");
                bitmap = BitmapFactory.decodeStream(png);
                if (bitmap == null) return null;
                synchronized (ICONS) {
                    ICONS.put(cacheKey, bitmap);
                }
            } catch (Exception ignored) {
                return null;
            } finally {
                if (png != null) try { png.close(); } catch (Exception ignored) { }
            }
        }
        return new BitmapDrawable(context.getResources(), bitmap);
    }

    /** Keep Galaxy and Pixel app-brand colors; Nothing uses a restrained monochrome glyph treatment. */
    public static Bitmap applyTheme(Bitmap source, String theme) {
        if (source == null || source.isRecycled() ||
                !ThemeManager.APPEARANCE_NOTHING.equals(theme)) return source;
        Bitmap mono = Bitmap.createBitmap(source.getWidth(), source.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(mono);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        float contrast = 1.18f;
        float offset = 128f * (1f - contrast) + 4f;
        float red = 0.2126f * contrast;
        float green = 0.7152f * contrast;
        float blue = 0.0722f * contrast;
        float[] matrix = new float[] {
                red, green, blue, 0f, offset,
                red, green, blue, 0f, offset,
                red, green, blue, 0f, offset,
                0f, 0f, 0f, 1f, 0f
        };
        paint.setColorFilter(new ColorMatrixColorFilter(new android.graphics.ColorMatrix(matrix)));
        canvas.drawBitmap(source, 0f, 0f, paint);
        return mono;
    }
}
