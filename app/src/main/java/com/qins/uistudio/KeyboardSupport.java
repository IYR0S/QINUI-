package com.qins.uistudio;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.view.KeyEvent;
import android.view.View;

/** API-19 physical-key focus and activation helpers. No touch input is required. */
public final class KeyboardSupport {
    private KeyboardSupport() {}

    public static void makeFocusable(View view) {
        view.setFocusable(true);
        view.setFocusableInTouchMode(false);
        view.setOnKeyListener(new View.OnKeyListener() {
            @Override public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_UP &&
                    (keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                     keyCode == KeyEvent.KEYCODE_ENTER ||
                     keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER ||
                     keyCode == KeyEvent.KEYCODE_BUTTON_A)) {
                    return v.performClick();
                }
                return false;
            }
        });
    }

    public static void makeFocusable(final View view, final int fillColor,
                                     final int focusColor, final int borderColor,
                                     final int radiusPx) {
        makeFocusable(view);
        styleFocusable(view, fillColor, borderColor, focusColor, radiusPx);
    }

    /** Shared raised surface for buttons and controls outside the main launcher canvas. */
    public static void styleFocusable(final View view, final int fillColor,
                                      final int focusColor, final int textColor,
                                      final int radiusPx) {
        makeFocusable(view);
        if (view instanceof android.widget.TextView) {
            ((android.widget.TextView) view).setTextColor(textColor);
        }
        view.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        view.setBackground(createBackground(fillColor, fillColor, focusColor, radiusPx, false));
        view.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override public void onFocusChange(View v, boolean hasFocus) {
                v.setBackground(createBackground(
                    hasFocus ? focusColor : fillColor,
                    fillColor, focusColor, radiusPx, hasFocus));
            }
        });
    }

    private static Drawable createBackground(int fill, int border, int focus,
                                               int radius, boolean focused) {
        return new RaisedControlDrawable(fill, border, focus, radius, focused);
    }

    /** Lightweight Canvas surface: a soft contact shadow, top sheen and explicit focus ring. */
    private static final class RaisedControlDrawable extends Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int fill;
        private final int border;
        private final int focus;
        private final float radius;
        private final float shadowRadius;
        private final boolean focused;

        RaisedControlDrawable(int fill, int border, int focus, int radiusPx, boolean focused) {
            this.fill = fill;
            this.border = border;
            this.focus = focus;
            this.radius = Math.max(0, radiusPx);
            this.shadowRadius = Math.max(2f, Math.min(5.5f, radiusPx * 0.18f));
            this.focused = focused;
        }

        @Override public void draw(Canvas canvas) {
            Rect bounds = getBounds();
            float inset = shadowRadius + 1f;
            RectF r = new RectF(bounds.left + inset, bounds.top + inset,
                    bounds.right - inset, bounds.bottom - inset);
            if (r.width() <= 1f || r.height() <= 1f) return;
            float corner = Math.min(radius, Math.min(r.width(), r.height()) / 2f);

            if (((fill >>> 24) & 0xFF) > 0) {
                paint.setShader(null);
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(fill);
                paint.setShadowLayer(shadowRadius, 0, shadowRadius * 0.45f, 0x35000000);
                canvas.drawRoundRect(r, corner, corner, paint);
                paint.clearShadowLayer();

                paint.setShader(new LinearGradient(r.left, r.top, r.left, r.bottom,
                        0x2AFFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
                paint.setColor(0xFFFFFFFF);
                canvas.drawRoundRect(r, corner, corner, paint);
                paint.setShader(null);
            }

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(focused ? 2.2f : 1f);
            paint.setColor(focused ? focus : border);
            if (focused) paint.setShadowLayer(shadowRadius, 0, 0, 0x554C78C2);
            if (((border >>> 24) & 0xFF) > 0 || focused) {
                canvas.drawRoundRect(r, corner, corner, paint);
            }
            paint.clearShadowLayer();
            if (focused) {
                paint.setStrokeWidth(0.8f);
                paint.setColor(0x66FFFFFF);
                RectF inner = new RectF(r.left + 2f, r.top + 2f, r.right - 2f, r.bottom - 2f);
                canvas.drawRoundRect(inner, Math.max(0, corner - 2f), Math.max(0, corner - 2f), paint);
            }
        }

        @Override public void setAlpha(int alpha) { }
        @Override public void setColorFilter(ColorFilter filter) { }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }
}
