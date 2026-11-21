package com.qins.uistudio;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * No-root quick settings companion. Opens the device's own settings panels;
 * it does not pretend to toggle protected system features directly.
 */
public class QuickPanelActivity extends StudioActivity {
    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        build();
    }

    private void build() {
        final ThemeManager.Appearance a = ThemeManager.getAppearance(this);
        ScrollView scroll = new ScrollView(this);
        scroll.setFocusable(false);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(16), dp(18), dp(16), dp(18));
        page.setBackgroundColor(a.background);
        scroll.addView(page);

        TextView title = new TextView(this);
        title.setText("Quick Panel");
        title.setTextSize(24);
        title.setTextColor(a.text);
        page.addView(title);

        TextView note = new TextView(this);
        note.setText("Shortcuts to Android settings. Protected system switches remain controlled by Android.");
        note.setTextSize(13);
        note.setTextColor(a.muted);
        LinearLayout.LayoutParams noteParams = new LinearLayout.LayoutParams(-1, -2);
        noteParams.bottomMargin = dp(12);
        page.addView(note, noteParams);

        add(page, "Wi-Fi", Settings.ACTION_WIFI_SETTINGS);
        add(page, "Bluetooth", Settings.ACTION_BLUETOOTH_SETTINGS);
        add(page, "Sound", Settings.ACTION_SOUND_SETTINGS);
        add(page, "Display", Settings.ACTION_DISPLAY_SETTINGS);
        add(page, "Location", Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        add(page, "Date & time", Settings.ACTION_DATE_SETTINGS);
        add(page, "Applications", Settings.ACTION_APPLICATION_SETTINGS);
        add(page, "Device information", Settings.ACTION_DEVICE_INFO_SETTINGS);

        Button back = button("Back", a);
        back.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { finish(); }
        });
        page.addView(back);
        setContentView(scroll);
        if (page.getChildCount() > 2) page.getChildAt(2).requestFocus();
    }

    private Button button(String label, ThemeManager.Appearance a) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        KeyboardSupport.makeFocusable(button, a.surface, a.accent, a.text, dp(a.tileCornerDp));
        return button;
    }

    private void add(LinearLayout page, String label, final String action) {
        final ThemeManager.Appearance a = ThemeManager.getAppearance(this);
        Button button = button(label, a);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(44));
        params.bottomMargin = dp(6);
        page.addView(button, params);
        button.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                try {
                    startActivity(new Intent(action));
                } catch (RuntimeException error) {
                    android.widget.Toast.makeText(QuickPanelActivity.this,
                            "Settings screen unavailable", android.widget.Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
