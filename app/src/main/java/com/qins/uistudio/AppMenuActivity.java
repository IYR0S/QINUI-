package com.qins.uistudio;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Physical-key navigable in-app menu. */
public class AppMenuActivity extends StudioActivity {
    private TextView homeStatus;
    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        buildMenu();
    }

    @Override protected void onResume() {
        super.onResume();
        if (homeStatus != null) updateHomeStatus();
    }

    private void updateHomeStatus() {
        homeStatus.setText(isQinsDefaultHome() ?
                "Status: QINS is the default Home app." :
                "Status: QINS is not the default Home app.");
    }

    private void buildMenu() {
        final ThemeManager.Palette palette = ThemeManager.getPalette(this);
        ScrollView scroll = new ScrollView(this);
        scroll.setFocusable(false);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(20), dp(24), dp(20), dp(24));
        page.setBackgroundColor(palette.background);
        scroll.addView(page);

        TextView title = new TextView(this);
        title.setText(R.string.app_menu_title);
        title.setTextSize(26);
        title.setTextColor(palette.text);
        page.addView(title);

        TextView hint = new TextView(this);
        hint.setText(R.string.app_menu_hint);
        hint.setTextSize(14);
        hint.setTextColor(palette.muted);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, -2);
        hp.bottomMargin = dp(18);
        page.addView(hint, hp);

        addItem(page, R.string.menu_dashboard, new View.OnClickListener() {
            @Override public void onClick(View v) {
                Intent intent = new Intent(AppMenuActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }
        });
        addItem(page, "Quick Panel", new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(AppMenuActivity.this, QinsLauncherActivity.class).putExtra("open_quick_panel", true));
                finish();
            }
        });
        addItem(page, "Set QINS as default Home app", new View.OnClickListener() {
            @Override public void onClick(View v) {
                try {
                    Intent home = new Intent(Intent.ACTION_MAIN);
                    home.addCategory(Intent.CATEGORY_HOME);
                    home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(Intent.createChooser(home, "Choose QINS as your Home app"));
                } catch (RuntimeException error) {
                    android.widget.Toast.makeText(AppMenuActivity.this,
                            "Open Android Settings > Apps > Default apps > Home app, then choose QINS UI.",
                            android.widget.Toast.LENGTH_LONG).show();
                }
            }
        });
        addItem(page, "Edit Music Widget", new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(AppMenuActivity.this, MusicWidgetSettingsActivity.class));
            }
        });
        TextView homeHeading = new TextView(this);
        homeHeading.setText("Default Home Launcher");
        homeHeading.setTextSize(18);
        homeHeading.setTextColor(palette.text);
        homeHeading.setPadding(0, dp(14), 0, dp(4));
        page.addView(homeHeading);
        homeStatus = new TextView(this);
        updateHomeStatus();
        homeStatus.setTextSize(13);
        homeStatus.setTextColor(palette.muted);
        page.addView(homeStatus);
        addItem(page, "Choose default Home app", new View.OnClickListener() {
            @Override public void onClick(View v) { openDefaultHomePicker(); }
        });
        addItem(page, R.string.menu_home, new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(AppMenuActivity.this, HomeStudioActivity.class));
                finish();
            }
        });
        addItem(page, R.string.menu_themes, new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(AppMenuActivity.this, ThemeStudioActivity.class));
                finish();
            }
        });
        addItem(page, R.string.menu_close, new View.OnClickListener() {
            @Override public void onClick(View v) { finish(); }
        });
        setContentView(scroll);
        if (page.getChildCount() > 2) page.getChildAt(2).requestFocus();
    }

    private boolean isQinsDefaultHome() {
        try {
            Intent home = new Intent(Intent.ACTION_MAIN);
            home.addCategory(Intent.CATEGORY_HOME);
            android.content.pm.ResolveInfo resolved = getPackageManager().resolveActivity(home,
                    android.content.pm.PackageManager.MATCH_DEFAULT_ONLY);
            return resolved != null && resolved.activityInfo != null &&
                    getPackageName().equals(resolved.activityInfo.packageName);
        } catch (RuntimeException ignored) { return false; }
    }

    private void openDefaultHomePicker() {
        try {
            // Android 4.4 has Settings > Home, but the public ACTION_HOME_SETTINGS
            // constant was only added later. Use the action string and fall back safely.
            Intent settings = new Intent("android.settings.HOME_SETTINGS");
            if (settings.resolveActivity(getPackageManager()) != null) {
                startActivity(settings);
                return;
            }
        } catch (RuntimeException ignored) { }
        try {
            Intent settings = new Intent(Intent.ACTION_SETTINGS);
            startActivity(settings);
            android.widget.Toast.makeText(this,
                    "Android 4.4: in Settings, open Home and select QINS Home. Then return here to verify the status.",
                    android.widget.Toast.LENGTH_LONG).show();
        } catch (RuntimeException error) {
            android.widget.Toast.makeText(this,
                    "Open Android Settings > Home and select QINS Home. Press the physical Home key if the Home screen is not listed.",
                    android.widget.Toast.LENGTH_LONG).show();
        }
    }

    private void addItem(LinearLayout page, String label, View.OnClickListener listener) {
        addItemView(page, label, listener);
    }

    private void addItem(LinearLayout page, int label, View.OnClickListener listener) {
        addItemView(page, getString(label), listener);
    }

    private void addItemView(LinearLayout page, String label, View.OnClickListener listener) {
        Button item = new Button(this);
        item.setText(label);
        item.setFocusable(true);
        item.setFocusableInTouchMode(false);
        item.setOnClickListener(listener);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = dp(8);
        page.addView(item, params);
    }
}
