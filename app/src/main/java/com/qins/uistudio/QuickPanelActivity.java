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
    private int dp(float v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        build();
    }

    private void build() {
        final ThemeManager.Palette p = ThemeManager.getPalette(this);
        ScrollView scroll = new ScrollView(this);
        scroll.setFocusable(false);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(16), dp(18), dp(16), dp(18));
        page.setBackgroundColor(p.background);
        scroll.addView(page);

        TextView title = new TextView(this);
        title.setText("Quick Panel");
        title.setTextSize(24);
        title.setTextColor(p.text);
        page.addView(title);

        TextView note = new TextView(this);
        note.setText("Shortcuts to Android settings. Protected system switches remain controlled by Android.");
        note.setTextSize(13);
        note.setTextColor(p.muted);
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(-1, -2);
        np.bottomMargin = dp(12);
        page.addView(note, np);

        add(page, "Wi-Fi", Settings.ACTION_WIFI_SETTINGS);
        add(page, "Bluetooth", Settings.ACTION_BLUETOOTH_SETTINGS);
        add(page, "Sound", Settings.ACTION_SOUND_SETTINGS);
        add(page, "Display", Settings.ACTION_DISPLAY_SETTINGS);
        add(page, "Location", Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        add(page, "Date & time", Settings.ACTION_DATE_SETTINGS);
        add(page, "Applications", Settings.ACTION_APPLICATION_SETTINGS);
        add(page, "Device information", Settings.ACTION_DEVICE_INFO_SETTINGS);
        Button back = button("Back", p);
        back.setOnClickListener(v -> finish());
        page.addView(back);
        setContentView(scroll);
        if (page.getChildCount() > 2) page.getChildAt(2).requestFocus();
    }

    private Button button(String label, ThemeManager.Palette p) {
        Button b = new Button(this);
        b.setText(label);
        KeyboardSupport.makeFocusable(b, p.surface, p.accent, p.text, dp(7));
        return b;
    }

    private void add(LinearLayout page, String label, final String action) {
        final ThemeManager.Palette p = ThemeManager.getPalette(this);
        Button b = button(label, p);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = dp(6);
        page.addView(b, lp);
        b.setOnClickListener(v -> {
            try { startActivity(new Intent(action)); }
            catch (RuntimeException error) {
                android.widget.Toast.makeText(this, "Settings screen unavailable", android.widget.Toast.LENGTH_SHORT).show();
            }
        });
    }
}