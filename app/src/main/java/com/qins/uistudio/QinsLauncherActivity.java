package com.qins.uistudio;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** No-root, physical-key-first Android HOME replacement. */
public class QinsLauncherActivity extends Activity {
    private int columns = 3;
    private int rows = 2;
    private int page = 0;
    private GridLayout appGrid;
    private TextView pageLabel;
    private Button previousButton;
    private Button nextButton;
    private List<ResolveInfo> installedApps = new ArrayList<ResolveInfo>();

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override public void onCreate(Bundle state) {
        setTheme(R.style.AppTheme);
        super.onCreate(state);
        readLayout();
        buildHome();
    }

    @Override protected void onResume() {
        super.onResume();
        readLayout();
        loadApps();
        populateApps();
    }

    private void readLayout() {
        android.content.SharedPreferences prefs =
                getSharedPreferences("HomeStudioActivity", MODE_PRIVATE);
        columns = prefs.getInt("home_columns", 3);
        rows = prefs.getInt("home_rows", 2);
        if (columns < 2 || columns > 4) columns = 3;
        if (rows < 2 || rows > 3) rows = 2;
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

        Button studio = new Button(this);
        studio.setText(R.string.open_studio_short);
        KeyboardSupport.makeFocusable(studio, palette.surface, palette.accent, palette.text, dp(7));
        studio.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                openStudio();
            }
        });
        header.addView(studio, new LinearLayout.LayoutParams(-2, dp(46)));
        root.addView(header);

        TextView hint = new TextView(this);
        hint.setText(R.string.qins_home_hint);
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

        LinearLayout paging = new LinearLayout(this);
        paging.setGravity(Gravity.CENTER_VERTICAL);
        paging.setOrientation(LinearLayout.HORIZONTAL);
        previousButton = new Button(this);
        previousButton.setText(R.string.previous_page);
        nextButton = new Button(this);
        nextButton.setText(R.string.next_page);
        pageLabel = new TextView(this);
        pageLabel.setGravity(Gravity.CENTER);
        pageLabel.setTextColor(palette.text);
        pageLabel.setTextSize(12);
        paging.addView(previousButton, new LinearLayout.LayoutParams(0, dp(44), 1));
        paging.addView(pageLabel, new LinearLayout.LayoutParams(0, -2, 1));
        paging.addView(nextButton, new LinearLayout.LayoutParams(0, dp(44), 1));
        root.addView(paging);

        KeyboardSupport.makeFocusable(previousButton, palette.surface, palette.accent, palette.text, dp(7));
        KeyboardSupport.makeFocusable(nextButton, palette.surface, palette.accent, palette.text, dp(7));
        previousButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { changePage(-1); }
        });
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { changePage(1); }
        });

        setContentView(root);
        loadApps();
        populateApps();
    }

    private void loadApps() {
        Intent query = new Intent(Intent.ACTION_MAIN, null);
        query.addCategory(Intent.CATEGORY_LAUNCHER);
        PackageManager pm = getPackageManager();
        installedApps = pm.queryIntentActivities(query, 0);
        Collections.sort(installedApps, new Comparator<ResolveInfo>() {
            @Override public int compare(ResolveInfo a, ResolveInfo b) {
                return a.loadLabel(getPackageManager()).toString()
                        .compareToIgnoreCase(b.loadLabel(getPackageManager()).toString());
            }
        });
        int pageSize = Math.max(1, columns * rows);
        int pageCount = Math.max(1, (installedApps.size() + pageSize - 1) / pageSize);
        if (page >= pageCount) page = pageCount - 1;
    }

    private void populateApps() {
        if (appGrid == null) return;
        appGrid.removeAllViews();
        appGrid.setColumnCount(columns);
        final ThemeManager.Palette palette = ThemeManager.getPalette(this);
        int pageSize = Math.max(1, columns * rows);
        int start = page * pageSize;
        int end = Math.min(installedApps.size(), start + pageSize);
        PackageManager pm = getPackageManager();

        for (int i = start; i < end; i++) {
            final ResolveInfo info = installedApps.get(i);
            final Intent launch = new Intent(Intent.ACTION_MAIN);
            launch.addCategory(Intent.CATEGORY_LAUNCHER);
            launch.setClassName(info.activityInfo.packageName, info.activityInfo.name);
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            LinearLayout tile = new LinearLayout(this);
            tile.setOrientation(LinearLayout.VERTICAL);
            tile.setGravity(Gravity.CENTER);
            tile.setPadding(dp(4), dp(6), dp(4), dp(6));
            tile.setClickable(true);

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

            KeyboardSupport.makeFocusable(tile, palette.surface, palette.accent, palette.text, dp(7));
            tile.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    try { startActivity(launch); }
                    catch (RuntimeException ignored) { }
                }
            });

            int position = i - start;
            GridLayout.LayoutParams cell = new GridLayout.LayoutParams();
            cell.width = 0;
            cell.height = dp(92);
            cell.columnSpec = GridLayout.spec(position % columns, 1f);
            cell.rowSpec = GridLayout.spec(position / columns);
            cell.setMargins(dp(3), dp(3), dp(3), dp(3));
            appGrid.addView(tile, cell);
        }

        int pageCount = Math.max(1, (installedApps.size() + pageSize - 1) / pageSize);
        pageLabel.setText((page + 1) + " / " + pageCount);
        previousButton.setEnabled(page > 0);
        nextButton.setEnabled(page + 1 < pageCount);
        if (appGrid.getChildCount() > 0) appGrid.getChildAt(0).requestFocus();
        else nextButton.requestFocus();
    }

    private void openStudio() {
        try {
            startActivity(new Intent(QinsLauncherActivity.this, MainActivity.class));
        } catch (RuntimeException error) {
            String detail = error.getMessage();
            if (detail == null || detail.length() == 0) detail = error.getClass().getSimpleName();
            android.widget.Toast.makeText(QinsLauncherActivity.this,
                    "Studio launch error: " + detail, android.widget.Toast.LENGTH_LONG).show();
        }
    }

    private void changePage(int delta) {
        int pageSize = Math.max(1, columns * rows);
        int pageCount = Math.max(1, (installedApps.size() + pageSize - 1) / pageSize);
        int target = page + delta;
        if (target < 0 || target >= pageCount) return;
        page = target;
        populateApps();
    }

    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            openStudio();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override public void onBackPressed() {
        // HOME remains the home screen.
    }
}
