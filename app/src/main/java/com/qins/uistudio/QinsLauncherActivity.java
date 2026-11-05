package com.qins.uistudio;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Gravity;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * No-root QINS home screen. Android launches this Activity for HOME requests
 * after the user selects QINS Home as the default launcher.
 */
public class QinsLauncherActivity extends Activity {
    private int columns = 3;
    private GridLayout appGrid;
    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override public void onCreate(Bundle state) {
        setTheme(android.R.style.Theme_Holo_Light_NoActionBar);
        super.onCreate(state);
        columns = getSharedPreferences("HomeStudioActivity", MODE_PRIVATE)
                .getInt("home_columns", 3);
        if (columns < 2 || columns > 4) columns = 3;
        buildHome();
    }

    @Override protected void onResume() {
        super.onResume();
        int savedColumns = getSharedPreferences("HomeStudioActivity", MODE_PRIVATE)
                .getInt("home_columns", 3);
        if (savedColumns >= 2 && savedColumns <= 4) columns = savedColumns;
        if (appGrid != null) {
            appGrid.setColumnCount(columns);
            populateApps();
        }
    }

    private void buildHome() {
        final ThemeManager.Palette palette = ThemeManager.getPalette(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(14), dp(12), dp(14), dp(8));
        root.setBackgroundColor(palette.background);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setOrientation(LinearLayout.HORIZONTAL);
        TextView title = new TextView(this);
        title.setText("QINS");
        title.setTextColor(palette.text);
        title.setTextSize(25);
        title.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(title, new LinearLayout.LayoutParams(0, dp(48), 1));

        TextView studio = new TextView(this);
        studio.setText("Studio");
        studio.setTextSize(14);
        studio.setTextColor(palette.accent);
        studio.setGravity(Gravity.CENTER);
        KeyboardSupport.makeFocusable(studio, palette.surface, palette.accent, palette.text, dp(7));
        studio.setPadding(dp(12), dp(6), dp(12), dp(6));
        studio.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                try {
                    startActivity(new Intent(QinsLauncherActivity.this, MainActivity.class));
                } catch (RuntimeException error) {
                    android.widget.Toast.makeText(QinsLauncherActivity.this,
                            "Could not open QINS Studio: " + error.getClass().getSimpleName(),
                            android.widget.Toast.LENGTH_LONG).show();
                }
            }
        });
        header.addView(studio, new LinearLayout.LayoutParams(-2, dp(44)));
        root.addView(header);

        TextView hint = new TextView(this);
        hint.setText("Select an app  •  MENU: Studio");
        hint.setTextColor(palette.muted);
        hint.setTextSize(12);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, -2);
        hp.bottomMargin = dp(8);
        root.addView(hint, hp);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        appGrid = new GridLayout(this);
        appGrid.setColumnCount(columns);
        appGrid.setAlignmentMode(GridLayout.ALIGN_BOUNDS);
        scroll.addView(appGrid, new ScrollView.LayoutParams(-1, -2));
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
        populateApps();
    }

    private void populateApps() {
        if (appGrid == null) return;
        appGrid.removeAllViews();
        final ThemeManager.Palette palette = ThemeManager.getPalette(this);
        Intent query = new Intent(Intent.ACTION_MAIN, null);
        query.addCategory(Intent.CATEGORY_LAUNCHER);
        PackageManager pm = getPackageManager();
        List<ResolveInfo> apps = pm.queryIntentActivities(query, 0);
        Collections.sort(apps, new Comparator<ResolveInfo>() {
            @Override public int compare(ResolveInfo a, ResolveInfo b) {
                return a.loadLabel(getPackageManager()).toString()
                        .compareToIgnoreCase(b.loadLabel(getPackageManager()).toString());
            }
        });

        for (int i = 0; i < apps.size(); i++) {
            final ResolveInfo info = apps.get(i);
            final Intent launch = new Intent(Intent.ACTION_MAIN);
            launch.addCategory(Intent.CATEGORY_LAUNCHER);
            launch.setClassName(info.activityInfo.packageName, info.activityInfo.name);
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            LinearLayout tile = new LinearLayout(this);
            tile.setOrientation(LinearLayout.VERTICAL);
            tile.setGravity(Gravity.CENTER);
            tile.setFocusable(true);
            tile.setFocusableInTouchMode(true);
            tile.setClickable(true);
            tile.setPadding(dp(4), dp(8), dp(4), dp(8));
            tile.setBackgroundColor(palette.surface);

            ImageView icon = new ImageView(this);
            Drawable drawable = info.loadIcon(pm);
            icon.setImageDrawable(drawable);
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(42), dp(42));
            iconParams.gravity = Gravity.CENTER;
            tile.addView(icon, iconParams);

            TextView label = new TextView(this);
            label.setText(info.loadLabel(pm));
            label.setTextColor(palette.text);
            label.setTextSize(12);
            label.setGravity(Gravity.CENTER);
            label.setSingleLine(true);
            label.setEllipsize(android.text.TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(-1, -2);
            labelParams.topMargin = dp(5);
            tile.addView(label, labelParams);

            KeyboardSupport.makeFocusable(tile, palette.surface, palette.text,
                    palette.accent, dp(7));
            tile.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    try { startActivity(launch); }
                    catch (Exception ignored) { }
                }
            });

            GridLayout.LayoutParams cell = new GridLayout.LayoutParams();
            cell.width = 0;
            cell.height = dp(92);
            cell.columnSpec = GridLayout.spec(i % columns, 1f);
            cell.rowSpec = GridLayout.spec(i / columns);
            cell.setMargins(dp(3), dp(3), dp(3), dp(3));
            appGrid.addView(tile, cell);
        }

        if (appGrid.getChildCount() > 0) appGrid.getChildAt(0).requestFocus();
    }

    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            startActivity(new Intent(this, MainActivity.class));
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override public void onBackPressed() {
        // The home screen remains the home screen when Back is pressed.
    }
}
