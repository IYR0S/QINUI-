package com.qins.uistudio;

import android.graphics.drawable.GradientDrawable;
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

    /** Shared button/card appearance used by the app-wide theme layer. */
    public static void styleFocusable(final View view, final int fillColor,
                                      final int focusColor, final int textColor,
                                      final int radiusPx) {
        makeFocusable(view);
        if (view instanceof android.widget.TextView) {
            ((android.widget.TextView) view).setTextColor(textColor);
        }
        view.setBackground(createBackground(fillColor, fillColor, radiusPx, false));
        view.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override public void onFocusChange(View v, boolean hasFocus) {
                v.setBackground(createBackground(
                    hasFocus ? focusColor : fillColor,
                    focusColor, radiusPx, hasFocus));
            }
        });
    }

    private static GradientDrawable createBackground(int fill, int stroke,
                                                       int radius, boolean focused) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(radius);
        drawable.setStroke(focused ? 3 : 1, stroke);
        return drawable;
    }
}
