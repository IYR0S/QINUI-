package com.qins.uistudio;

import android.graphics.drawable.GradientDrawable;
import android.view.KeyEvent;
import android.view.View;

/** API-19-compatible keyboard focus and activation helpers for physical controls. */
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
        view.setBackground(createBackground(fillColor, borderColor, radiusPx, false));
        view.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override public void onFocusChange(View v, boolean hasFocus) {
                v.setBackground(createBackground(
                    hasFocus ? focusColor : fillColor,
                    hasFocus ? borderColor : fillColor,
                    radiusPx, hasFocus));
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
