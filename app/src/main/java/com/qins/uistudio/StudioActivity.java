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

    @Override public void onResume() {
        super.onResume();
        applySharedTheme();
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
        ThemeManager.Palette palette = ThemeManager.getPalette(this);
        root.setBackgroundColor(palette.background);
        styleButtons(root, palette);
    }

    private void styleButtons(View view, ThemeManager.Palette palette) {
        if (view instanceof Button) {
            KeyboardSupport.styleFocusable((Button) view, palette.surface,
                palette.accent, palette.text, dp(8));
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                styleButtons(group.getChildAt(i), palette);
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
