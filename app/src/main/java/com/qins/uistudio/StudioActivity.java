package com.qins.uistudio;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

/**
 * Shared app shell: applies the saved palette to every Studio screen and
 * exposes the in-app menu through the physical MENU key.
 */
public abstract class StudioActivity extends Activity {
    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override protected void onResume() {
        super.onResume();
        refreshSharedTheme();
    }

    @Override public void setContentView(View view) {
        super.setContentView(view);
        applyTheme(view);
    }

    @Override public void setContentView(View view, ViewGroup.LayoutParams params) {
        super.setContentView(view, params);
        applyTheme(view);
    }

    protected final void refreshSharedTheme() {
        ViewGroup content = (ViewGroup) findViewById(android.R.id.content);
        if (content != null && content.getChildCount() > 0) {
            applyTheme(content.getChildAt(0));
        }
    }

    private void applyTheme(View root) {
        // Theme Studio intentionally previews the separate palette selector.
        // Every other studio screen follows the active Galaxy / Pixel / Nothing
        // appearance so its controls do not look like a different application.
        if (this instanceof ThemeStudioActivity) {
            ThemeManager.Palette palette = ThemeManager.getPalette(this);
            root.setBackgroundColor(palette.background);
            styleButtons(root, palette.surface, palette.accent, palette.text, dp(8));
        } else {
            ThemeManager.Appearance appearance = ThemeManager.getAppearance(this);
            root.setBackgroundColor(appearance.background);
            styleButtons(root, appearance.surface, appearance.accent, appearance.text,
                    dp(appearance.tileCornerDp));
        }
    }

    private void styleButtons(View view, int fill, int accent, int text, int radius) {
        if (view instanceof Button) {
            KeyboardSupport.styleFocusable((Button) view, fill, accent, text, radius);
            ((Button) view).setAllCaps(false);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                styleButtons(group.getChildAt(i), fill, accent, text, radius);
            }
        }
    }

    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU && this instanceof AppMenuActivity == false) {
            startActivity(new Intent(this, AppMenuActivity.class));
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
