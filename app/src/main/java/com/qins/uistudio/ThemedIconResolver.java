package com.qins.uistudio;

import android.content.Context;
import android.content.pm.ResolveInfo;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;

import java.io.InputStream;

/**
 * API-19 icon-pack bridge. Theme packs may provide appfilter.xml plus PNGs;
 * when no licensed override exists, the installed app icon is retained and
 * Nothing receives a restrained monochrome treatment.
 */
public final class ThemedIconResolver {
    private ThemedIconResolver() {}

    private static String packForTheme(String theme) {
        if (ThemeManager.APPEARANCE_GALAXY.equals(theme)) return "one-ui";
        if (ThemeManager.APPEARANCE_PIXEL.equals(theme)) return "pixel";
        return "nothing-os";
    }

    public static Drawable loadMappedIcon(Context context, ResolveInfo info, String theme) {
        if (context == null || info == null || info.activityInfo == null) return null;
        String pack = packForTheme(theme);
        String component = "ComponentInfo{" + info.activityInfo.packageName + "/" +
                info.activityInfo.name + "}";
        String drawableName = null;
        InputStream input = null;
        try {
            input = context.getAssets().open("iconpacks/" + pack + "/appfilter.xml");
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(input, "UTF-8");
            int event;
            while ((event = parser.next()) != XmlPullParser.END_DOCUMENT) {
                if (event != XmlPullParser.START_TAG || !"item".equals(parser.getName())) continue;
                String mappedComponent = parser.getAttributeValue(null, "component");
                if (component.equals(mappedComponent)) {
                    drawableName = parser.getAttributeValue(null, "drawable");
                    break;
                }
            }
        } catch (Exception ignored) {
            return null;
        } finally {
            if (input != null) try { input.close(); } catch (Exception ignored) { }
        }
        if (drawableName == null || !drawableName.matches("[A-Za-z0-9_]+")) return null;

        InputStream png = null;
        try {
            png = context.getAssets().open("iconpacks/" + pack +
                    "/drawable-nodpi/" + drawableName + ".png");
            Bitmap bitmap = BitmapFactory.decodeStream(png);
            if (bitmap == null) return null;
            return new BitmapDrawable(context.getResources(), bitmap);
        } catch (Exception ignored) {
            return null;
        } finally {
            if (png != null) try { png.close(); } catch (Exception ignored) { }
        }
    }

    /** Apply theme treatment to the normalized app logo without adding an outer icon frame. */
    public static Bitmap applyTheme(Bitmap source, String theme) {
        if (source == null || source.isRecycled() ||
                !ThemeManager.APPEARANCE_NOTHING.equals(theme)) return source;

        Bitmap result = Bitmap.createBitmap(source.getWidth(), source.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        ColorMatrix grayscale = new ColorMatrix();
        grayscale.setSaturation(0f);
        // Slightly increase contrast while retaining the original alpha channel.
        float contrast = 1.12f;
        float offset = 128f * (1f - contrast);
        ColorMatrix contrastMatrix = new ColorMatrix(new float[] {
                contrast, 0, 0, 0, offset,
                0, contrast, 0, 0, offset,
                0, 0, contrast, 0, offset,
                0, 0, 0, 1, 0
        });
        grayscale.postConcat(contrastMatrix);
        paint.setColorFilter(new ColorMatrixColorFilter(grayscale));
        canvas.drawBitmap(source, 0, 0, paint);
        paint.setColorFilter(null);
        return result;
    }
}
