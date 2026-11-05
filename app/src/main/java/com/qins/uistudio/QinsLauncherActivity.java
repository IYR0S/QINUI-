package com.qins.uistudio;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * QINS Home: four pinned apps per page, a separate app drawer, long-press
 * editing and an integrated Quick Panel. Built for Android 4.4 / API 19.
 */
public class QinsLauncherActivity extends Activity {
    private static final int APPS_PER_PAGE = 4;
    private static final String LAUNCHER_PREFS = "qins_launcher";
    private static final String PINS_KEY = "home_pins";

    private int page = 0;
    private int homeAppRowHeightDp = 86;
    private int selectedHomeIndex = -1;
    private boolean quickPanelVisible = false;
    private boolean quickPanelExpanded = false;
    private int homePageCount = 1;
    private static final String QUICK_PREFS = "qins_quick_tiles";
    private static final String[] QUICK_OPTIONS = new String[] {"sound", "bluetooth", "settings", "wifi", "brightness", "volume"};
    private static final String[] QUICK_LABELS = new String[] {"Sound", "Bluetooth", "Settings", "Wi-Fi", "Brightness", "Volume"};
    private boolean drawerVisible = false;
    private boolean editMode = false;
    private boolean pinSelectionMode = false;
    private View pendingSelectView;
    private boolean longPressPerformed = false;

    private LinearLayout root;
    private LinearLayout pageContent;
    private LinearLayout appRow;
    private LinearLayout editBar;
    private TextView appsNavButton;
    private TextView clockView;
    private TextView dateView;
    private TextView batteryView;
    private TextView panelClock;
    private TextView panelDate;
    private TextView panelBattery;
    private TextView wifiState;
    private TextView bluetoothState;
    private TextView soundState;
    private TextView pageDots;
    private View bottomHandle;

    private final List<ResolveInfo> installedApps = new ArrayList<ResolveInfo>();
    private final List<ResolveInfo> pinnedApps = new ArrayList<ResolveInfo>();
    private boolean receiversRegistered = false;

    private final BroadcastReceiver systemReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            updateClockAndBattery();
            updateQuickStates();
        }
    };

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private int screenWidthDp() {
        return (int) (getResources().getDisplayMetrics().widthPixels /
                getResources().getDisplayMetrics().density);
    }

    private int screenHeightDp() {
        return (int) (getResources().getDisplayMetrics().heightPixels /
                getResources().getDisplayMetrics().density);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private ThemeManager.Appearance appearance() {
        return ThemeManager.getAppearance(this);
    }

    @Override public void onCreate(Bundle state) {
        setTheme(R.style.AppTheme);
        super.onCreate(state);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        loadApps();
        buildShell();
        if (getIntent().getBooleanExtra("open_quick_panel", false)) showQuickPanel(false);
        else showHome(false);
    }

    @Override protected void onResume() {
        super.onResume();
        loadApps();
        registerSystemReceiver();
        if (quickPanelVisible && quickPanelExpanded) showQuickPanel(false);
        else if (quickPanelVisible) showCompactQuickPanel(false);
        else if (drawerVisible) showDrawer(pinSelectionMode);
        else showHome(false);
        updateClockAndBattery();
        updateQuickStates();
    }

    @Override protected void onPause() {
        unregisterSystemReceiver();
        super.onPause();
    }

    private void registerSystemReceiver() {
        if (receiversRegistered) return;
        try {
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_TIME_TICK);
            filter.addAction(Intent.ACTION_TIME_CHANGED);
            filter.addAction(Intent.ACTION_DATE_CHANGED);
            filter.addAction(Intent.ACTION_BATTERY_CHANGED);
            filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
            filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(systemReceiver, filter);
            receiversRegistered = true;
        } catch (RuntimeException ignored) { }
    }

    private void unregisterSystemReceiver() {
        if (!receiversRegistered) return;
        try { unregisterReceiver(systemReceiver); } catch (RuntimeException ignored) { }
        receiversRegistered = false;
    }

    private void buildShell() {
        ThemeManager.Appearance a = appearance();
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int sidePadding = clamp(screenWidthDp() / 14, 8, 14);
        int footerHeight = clamp((int) (screenHeightDp() * 0.17f), 30, 42);
        root.setPadding(dp(sidePadding), dp(4), dp(sidePadding), dp(3));
        root.setBackground(new QinsWallpaperDrawable(a));

        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        TextView wordmark = new TextView(this);
        wordmark.setText("qins");
        wordmark.setTextColor(a.text);
        wordmark.setTextSize(13);
        wordmark.setTypeface(a.useMonoType ? Typeface.MONOSPACE : Typeface.DEFAULT_BOLD,
                Typeface.BOLD);
        topBar.addView(wordmark, new LinearLayout.LayoutParams(0, dp(42), 1));

        appsNavButton = new TextView(this);
        appsNavButton.setGravity(Gravity.CENTER);
        appsNavButton.setTextSize(12);
        appsNavButton.setTextColor(a.text);
        appsNavButton.setPadding(dp(15), 0, dp(15), 0);
        appsNavButton.setFocusable(true);
        appsNavButton.setFocusableInTouchMode(false);
        styleSurface(appsNavButton, a.surface, a.border, a.focus, a.cornerDp);
        appsNavButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (drawerVisible || quickPanelVisible) showHome(false);
                else showDrawer(false);
            }
        });
        topBar.addView(appsNavButton, new LinearLayout.LayoutParams(-2, dp(36)));
        root.addView(topBar, new LinearLayout.LayoutParams(-1, dp(44)));

        pageContent = new LinearLayout(this);
        pageContent.setOrientation(LinearLayout.VERTICAL);
        root.addView(pageContent, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout footer = new LinearLayout(this);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, dp(3), 0, dp(2));
        TextView handle = new TextView(this);
        bottomHandle = handle;
        handle.setText("━━");
        handle.setTextSize(20);
        handle.setGravity(Gravity.CENTER);
        handle.setTextColor(a.text);
        handle.setContentDescription("Open quick settings");
        handle.setFocusable(true);
        handle.setFocusableInTouchMode(false);
        styleSurface(handle, a.surface, a.border, a.focus, a.cornerDp);
        handle.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (!quickPanelVisible) showCompactQuickPanel(true);
                else if (!quickPanelExpanded) showQuickPanel(true);
                else showHome(false);
            }
        });
        footer.addView(handle, new LinearLayout.LayoutParams(dp(76), dp(footerHeight - 3)));
        root.addView(footer, new LinearLayout.LayoutParams(-1, dp(footerHeight)));
        setContentView(root);
        updateNavigationButton();
    }

    private void updateNavigationButton() {
        if (appsNavButton == null) return;
        ThemeManager.Appearance a = appearance();
        appsNavButton.setText(drawerVisible || quickPanelVisible
                ? getString(R.string.launcher_home) : getString(R.string.launcher_apps));
        appsNavButton.setTextColor(a.text);
        styleSurface(appsNavButton, a.surface, a.border, a.focus, a.cornerDp);
        TextView handle = (TextView) bottomHandle;
        handle.setText(quickPanelVisible ? "⌃" : "━━");
        handle.setTextColor(a.text);
        styleSurface(handle, a.surface, a.border, a.focus, a.cornerDp);
        root.setBackground(new QinsWallpaperDrawable(a));
    }

    private void styleSurface(final View view, final int fill, final int border,
                              final int focus, final float cornerDp) {
        view.setBackground(makeShape(fill, border, cornerDp, false));
        view.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override public void onFocusChange(View v, boolean hasFocus) {
                ThemeManager.Appearance a = appearance();
                v.setBackground(makeShape(hasFocus ? focus : fill,
                        hasFocus ? a.accent : border, cornerDp, hasFocus));
            }
        });
    }

    private GradientDrawable makeShape(int fill, int stroke, float radiusDp, boolean focused) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(radiusDp));
        d.setStroke(dp(focused ? 2 : 1), stroke);
        return d;
    }

    /** A small procedural wallpaper gives each preset a distinct identity without large assets. */
    private final class QinsWallpaperDrawable extends Drawable {
        private final ThemeManager.Appearance style;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        QinsWallpaperDrawable(ThemeManager.Appearance style) {
            this.style = style;
        }

        @Override public void draw(Canvas canvas) {
            android.graphics.Rect bounds = getBounds();
            float w = bounds.width();
            float h = bounds.height();
            paint.setAlpha(255);
            paint.setColorFilter(null);
            paint.setShader(new LinearGradient(0, 0, w, h,
                    style.backgroundAlt, style.background, Shader.TileMode.CLAMP));
            canvas.drawRect(bounds, paint);
            paint.setShader(null);

            if (style.monochrome) {
                paint.setColor(style.text);
                paint.setAlpha(16);
                float step = dp(11);
                for (float y = dp(22); y < h; y += step) {
                    for (float x = dp(9); x < w; x += step) {
                        canvas.drawCircle(x, y, dp(0.65f), paint);
                    }
                }
                paint.setAlpha(20);
                canvas.drawCircle(w * 0.83f, h * 0.22f, Math.min(w, h) * 0.20f, paint);
            } else if (ThemeManager.APPEARANCE_PIXEL.equals(style.id)) {
                paint.setColor(style.accent);
                paint.setAlpha(25);
                canvas.drawCircle(w * 0.96f, h * 0.16f, Math.min(w, h) * 0.26f, paint);
                paint.setColor(0xFFB2A0D6);
                paint.setAlpha(22);
                canvas.drawCircle(w * 0.08f, h * 0.76f, Math.min(w, h) * 0.22f, paint);
                paint.setColor(0xFFE7B7A5);
                paint.setAlpha(20);
                canvas.drawCircle(w * 0.80f, h * 0.82f, Math.min(w, h) * 0.17f, paint);
            } else {
                paint.setColor(style.accent);
                paint.setAlpha(18);
                canvas.drawCircle(w * 0.95f, h * 0.13f, Math.min(w, h) * 0.23f, paint);
                paint.setColor(0xFF8DA9D7);
                paint.setAlpha(18);
                Path wave = new Path();
                wave.moveTo(0, h * 0.69f);
                wave.cubicTo(w * 0.30f, h * 0.53f, w * 0.62f, h * 0.88f, w, h * 0.63f);
                wave.lineTo(w, h);
                wave.lineTo(0, h);
                wave.close();
                canvas.drawPath(wave, paint);
            }
        }

        @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); invalidateSelf(); }
        @Override public void setColorFilter(ColorFilter colorFilter) { paint.setColorFilter(colorFilter); invalidateSelf(); }
        @Override public int getOpacity() { return PixelFormat.OPAQUE; }
    }

    private String componentKey(ResolveInfo info) {
        return info.activityInfo.packageName + "/" + info.activityInfo.name;
    }

    private void loadApps() {
        Intent query = new Intent(Intent.ACTION_MAIN, null);
        query.addCategory(Intent.CATEGORY_LAUNCHER);
        PackageManager pm = getPackageManager();
        installedApps.clear();
        List<ResolveInfo> found = pm.queryIntentActivities(query, 0);
        for (ResolveInfo info : found) {
            if (!getPackageName().equals(info.activityInfo.packageName)) installedApps.add(info);
        }
        Collections.sort(installedApps, new Comparator<ResolveInfo>() {
            @Override public int compare(ResolveInfo a, ResolveInfo b) {
                return a.loadLabel(getPackageManager()).toString()
                        .compareToIgnoreCase(b.loadLabel(getPackageManager()).toString());
            }
        });
        loadPinnedApps();
        int count = pageCount();
        if (page >= count) page = Math.max(0, count - 1);
    }

    private void loadPinnedApps() {
        homePageCount = Math.max(1, getSharedPreferences(LAUNCHER_PREFS, MODE_PRIVATE).getInt("home_pages", 1));
        pinnedApps.clear();
        SharedPreferences prefs = getSharedPreferences(LAUNCHER_PREFS, MODE_PRIVATE);
        if (!prefs.contains(PINS_KEY)) {
            seedDefaultPins();
            savePinnedApps();
            return;
        }
        String saved = prefs.getString(PINS_KEY, "");
        if (saved.length() == 0) return;
        String[] keys = saved.split("\\n");
        for (String key : keys) {
            ResolveInfo info = findApp(key);
            if (info != null && !isPinned(info)) pinnedApps.add(info);
        }
    }

    private ResolveInfo findApp(String key) {
        for (ResolveInfo info : installedApps) {
            if (componentKey(info).equals(key)) return info;
        }
        return null;
    }

    private boolean isPinned(ResolveInfo target) {
        String key = componentKey(target);
        for (ResolveInfo info : pinnedApps) {
            if (componentKey(info).equals(key)) return true;
        }
        return false;
    }

    private void seedDefaultPins() {
        String[][] groups = new String[][] {
                {"phone", "dialer", "טלפון"},
                {"message", "sms", "הודעות"},
                {"browser", "chrome", "internet", "דפדפן"},
                {"camera", "מצלמה", "settings", "הגדרות"}
        };
        for (String[] group : groups) {
            if (pinnedApps.size() >= APPS_PER_PAGE) break;
            ResolveInfo match = null;
            for (ResolveInfo info : installedApps) {
                if (isPinned(info)) continue;
                String haystack = (info.loadLabel(getPackageManager()).toString() + " " +
                        info.activityInfo.packageName).toLowerCase(Locale.ROOT);
                for (String term : group) {
                    if (haystack.contains(term.toLowerCase(Locale.ROOT))) {
                        match = info;
                        break;
                    }
                }
                if (match != null) break;
            }
            if (match != null) pinnedApps.add(match);
        }
        for (ResolveInfo info : installedApps) {
            if (pinnedApps.size() >= APPS_PER_PAGE) break;
            if (!isPinned(info)) pinnedApps.add(info);
        }
    }

    private void savePinnedApps() {
        StringBuilder value = new StringBuilder();
        for (ResolveInfo info : pinnedApps) {
            if (value.length() > 0) value.append('\n');
            value.append(componentKey(info));
        }
        getSharedPreferences(LAUNCHER_PREFS, MODE_PRIVATE).edit()
                .putString(PINS_KEY, value.toString()).commit();
    }

    private int pageCount() {
        return Math.max(homePageCount, Math.max(1, (pinnedApps.size() + APPS_PER_PAGE - 1) / APPS_PER_PAGE));
    }

    private void addHomePage() {
        homePageCount = Math.min(12, pageCount() + 1);
        getSharedPreferences(LAUNCHER_PREFS, MODE_PRIVATE).edit().putInt("home_pages", homePageCount).commit();
        page = homePageCount - 1;
        editMode = false;
        showHome(false);
    }

    private void updateClockAndBattery() {
        String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
        String date = new SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(new Date());
        String battery = batterySummary();
        if (clockView != null) clockView.setText(time);
        if (dateView != null) dateView.setText(date);
        if (batteryView != null) batteryView.setText(battery);
        if (panelClock != null) panelClock.setText(time);
        if (panelDate != null) panelDate.setText(date);
        if (panelBattery != null) panelBattery.setText(battery);
    }

    private String batterySummary() {
        try {
            Intent battery = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (battery == null) return "";
            int level = battery.getIntExtra("level", -1);
            int scale = battery.getIntExtra("scale", 100);
            if (level < 0 || scale <= 0) return "";
            return "▰ " + (level * 100 / scale) + "%";
        } catch (RuntimeException ignored) { return ""; }
    }

    private void showHome(boolean focusHandle) {
        quickPanelVisible = false;
        quickPanelExpanded = false;
        drawerVisible = false;
        pinSelectionMode = false;
        pageContent.removeAllViews();
        ThemeManager.Appearance a = appearance();
        updateNavigationButton();

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        int smallScreen = screenWidthDp() < 210 || screenHeightDp() < 280 ? 1 : 0;
        int topSide = smallScreen == 1 ? 10 : 18;
        int topVertical = smallScreen == 1 ? 6 : 12;
        top.setPadding(dp(topSide), dp(topVertical), dp(topSide), dp(topVertical));
        top.setBackground(makeShape(a.surface, a.border, a.cornerDp + 4, false));

        LinearLayout timeBlock = new LinearLayout(this);
        timeBlock.setOrientation(LinearLayout.VERTICAL);
        timeBlock.setGravity(Gravity.CENTER_VERTICAL);
        clockView = new TextView(this);
        clockView.setTextColor(a.text);
        clockView.setTextSize(smallScreen == 1 ? 29 : 42);
        clockView.setIncludeFontPadding(false);
        clockView.setTypeface(a.useMonoType ? Typeface.MONOSPACE : Typeface.DEFAULT, Typeface.NORMAL);
        timeBlock.addView(clockView, new LinearLayout.LayoutParams(-1, -2));

        dateView = new TextView(this);
        dateView.setTextColor(a.muted);
        dateView.setTextSize(smallScreen == 1 ? 9 : 11);
        dateView.setSingleLine(true);
        timeBlock.addView(dateView, new LinearLayout.LayoutParams(-1, -2));
        top.addView(timeBlock, new LinearLayout.LayoutParams(0, -2, 1));

        batteryView = new TextView(this);
        batteryView.setTextColor(a.muted);
        batteryView.setTextSize(12);
        batteryView.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        top.addView(batteryView, new LinearLayout.LayoutParams(-2, -1));
        LinearLayout.LayoutParams topParams = new LinearLayout.LayoutParams(-1, -2);
        topParams.topMargin = dp(smallScreen == 1 ? 3 : 9);
        topParams.bottomMargin = dp(smallScreen == 1 ? 5 : 10);
        pageContent.addView(top, topParams);

        View spacer = new View(this);
        pageContent.addView(spacer, new LinearLayout.LayoutParams(1, 0, 1));

        appRow = new LinearLayout(this);
        appRow.setOrientation(LinearLayout.HORIZONTAL);
        appRow.setGravity(Gravity.CENTER);
        homeAppRowHeightDp = clamp((int) (screenHeightDp() * 0.34f), 64, 112);
        pageContent.addView(appRow, new LinearLayout.LayoutParams(-1, dp(homeAppRowHeightDp)));
        populateHomeApps(a);

        LinearLayout pages = new LinearLayout(this);
        pages.setGravity(Gravity.CENTER);
        pages.setOrientation(LinearLayout.HORIZONTAL);
        QinsGlyphView previous = pageArrow("left", a);
        QinsGlyphView next = pageArrow("right", a);
        pageDots = new TextView(this);
        pageDots.setTextColor(a.muted);
        pageDots.setTextSize(14);
        pageDots.setGravity(Gravity.CENTER);
        pages.addView(previous, new LinearLayout.LayoutParams(dp(38), dp(32)));
        pages.addView(pageDots, new LinearLayout.LayoutParams(dp(100), dp(32)));
        pages.addView(next, new LinearLayout.LayoutParams(dp(38), dp(32)));
        QinsGlyphView addPage = pageArrow("add", a);
        pages.addView(addPage, new LinearLayout.LayoutParams(dp(34), dp(32)));
        addPage.setContentDescription("Add home page");
        addPage.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { addHomePage(); }
        });
        LinearLayout.LayoutParams pageParams = new LinearLayout.LayoutParams(-1,
                dp(screenHeightDp() < 280 ? 26 : 36));
        pageParams.topMargin = dp(screenHeightDp() < 280 ? 2 : 5);
        pageContent.addView(pages, pageParams);
        previous.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { changePage(-1); }
        });
        next.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { changePage(1); }
        });
        previous.setEnabled(page > 0);
        next.setEnabled(page + 1 < pageCount());
        previous.setAlpha(page > 0 ? 1f : 0.28f);
        next.setAlpha(page + 1 < pageCount() ? 1f : 0.28f);
        updatePageDots();

        if (editMode) buildEditBar(a);
        updateClockAndBattery();
        updateNavigationButton();

        if (focusHandle) bottomHandle.requestFocus();
        else if (editMode && selectedHomeIndex >= 0) focusHomeIndex(selectedHomeIndex);
        else if (appRow.getChildCount() > 0) appRow.getChildAt(0).requestFocus();
        else appsNavButton.requestFocus();
    }

    /** Compact quick controls stay on top of the home page and leave most of it visible. */
    private void showCompactQuickPanel(boolean focusFirst) {
        showHome(false);
        quickPanelVisible = true;
        quickPanelExpanded = false;
        ThemeManager.Appearance a = appearance();

        LinearLayout compact = new LinearLayout(this);
        compact.setOrientation(LinearLayout.VERTICAL);
        compact.setPadding(dp(5), dp(4), dp(5), dp(4));
        styleSurface(compact, a.surface, a.border, a.focus, a.cornerDp + 3);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = new TextView(this);
        title.setText("Quick controls");
        title.setTextColor(a.text);
        title.setTextSize(10);
        header.addView(title, new LinearLayout.LayoutParams(0, dp(22), 1));
        TextView edit = new TextView(this);
        edit.setText("Edit");
        edit.setTextColor(a.accent);
        edit.setTextSize(10);
        edit.setGravity(Gravity.CENTER);
        edit.setFocusable(true);
        edit.setFocusableInTouchMode(false);
        header.addView(edit, new LinearLayout.LayoutParams(dp(48), dp(24)));
        edit.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showQuickTileEditor(0); }
        });
        compact.addView(header);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        String[] keys = getQuickTileKeys();
        for (int i = 0; i < 4; i++) {
            final String key = keys[i];
            View tile = compactQuickTile(key, a);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(50), 1);
            if (i > 0) p.leftMargin = dp(4);
            row.addView(tile, p);
            tile.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { performQuickAction(key); }
            });
        }
        compact.addView(row, new LinearLayout.LayoutParams(-1, dp(52)));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(80));
        params.topMargin = dp(4);
        pageContent.addView(compact, params);
        updateNavigationButton();
        if (focusFirst && row.getChildCount() > 0) row.getChildAt(0).requestFocus();
    }

    private String[] getQuickTileKeys() {
        String saved = getSharedPreferences(QUICK_PREFS, MODE_PRIVATE).getString("order", "");
        String[] defaults = new String[] {"sound", "bluetooth", "settings", "wifi"};
        if (saved.length() == 0) return defaults;
        String[] parts = saved.split(",");
        String[] result = new String[4];
        for (int i = 0; i < 4; i++) result[i] = i < parts.length && isQuickOption(parts[i]) ? parts[i] : defaults[i];
        return result;
    }

    private boolean isQuickOption(String key) {
        for (String option : QUICK_OPTIONS) if (option.equals(key)) return true;
        return false;
    }

    private void saveQuickTileKeys(String[] keys) {
        StringBuilder value = new StringBuilder();
        for (int i = 0; i < 4; i++) { if (i > 0) value.append(','); value.append(keys[i]); }
        getSharedPreferences(QUICK_PREFS, MODE_PRIVATE).edit().putString("order", value.toString()).commit();
    }

    private View compactQuickTile(String key, ThemeManager.Appearance a) {
        String label = quickLabel(key);
        String glyph = "settings".equals(key) ? "apps" : ("brightness".equals(key) ? "brightness" : ("volume".equals(key) ? "volume" : key));
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER);
        tile.setPadding(dp(2), dp(2), dp(2), dp(2));
        QinsGlyphView icon = new QinsGlyphView(this, glyph, a.accent);
        tile.addView(icon, new LinearLayout.LayoutParams(dp(23), dp(23)));
        TextView text = new TextView(this);
        text.setText(label);
        text.setTextColor(a.text);
        text.setTextSize(8);
        text.setGravity(Gravity.CENTER);
        tile.addView(text);
        tile.setFocusable(true);
        tile.setFocusableInTouchMode(false);
        styleSurface(tile, a.tile, a.border, a.tileActive, a.cornerDp);
        return tile;
    }

    private String quickLabel(String key) {
        for (int i = 0; i < QUICK_OPTIONS.length; i++) if (QUICK_OPTIONS[i].equals(key)) return QUICK_LABELS[i];
        return key;
    }

    private void showQuickTileEditor(final int index) {
        final String[] keys = getQuickTileKeys();
        new android.app.AlertDialog.Builder(this)
                .setTitle("Choose shortcut " + (index + 1))
                .setItems(QUICK_LABELS, new android.content.DialogInterface.OnClickListener() {
                    @Override public void onClick(android.content.DialogInterface dialog, int which) {
                        keys[index] = QUICK_OPTIONS[which];
                        saveQuickTileKeys(keys);
                        showCompactQuickPanel(true);
                    }
                }).setNegativeButton("Cancel", null).show();
    }

    private void openSystemSettings() {
        try { startActivity(new Intent(Settings.ACTION_SETTINGS)); }
        catch (RuntimeException ignored) {
            android.widget.Toast.makeText(this, "Settings unavailable", android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    private void performQuickAction(String key) {
        if ("wifi".equals(key)) toggleWifi();
        else if ("bluetooth".equals(key)) toggleBluetooth();
        else if ("sound".equals(key)) toggleSound();
        else if ("settings".equals(key)) openSystemSettings();
        else if ("brightness".equals(key) || "volume".equals(key)) showQuickPanel(true);
    }

    private QinsGlyphView pageArrow(String symbol, ThemeManager.Appearance a) {
        QinsGlyphView button = new QinsGlyphView(this, symbol, a.muted);
        button.setFocusable(true);
        button.setFocusableInTouchMode(false);
        styleSurface(button, a.backgroundAlt, a.border, a.focus, a.cornerDp);
        return button;
    }

    private void populateHomeApps(ThemeManager.Appearance a) {
        appRow.removeAllViews();
        int start = page * APPS_PER_PAGE;
        for (int slot = 0; slot < APPS_PER_PAGE; slot++) {
            final int absoluteIndex = start + slot;
            if (absoluteIndex >= pinnedApps.size()) {
                TextView add = new TextView(this);
                add.setText("+");
                add.setTextSize(27);
                add.setGravity(Gravity.CENTER);
                add.setTextColor(a.muted);
                add.setFocusable(true);
                add.setFocusableInTouchMode(false);
                add.setTag("home-empty:" + absoluteIndex);
                styleSurface(add, 0x00000000, 0x00000000, a.focus, a.cornerDp);
                add.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) { showDrawer(true); }
                });
                appRow.addView(add, new LinearLayout.LayoutParams(0, dp(homeAppRowHeightDp - 2), 1));
                continue;
            }

            final ResolveInfo info = pinnedApps.get(absoluteIndex);
            final Intent launch = launchIntent(info);
            LinearLayout tile = new LinearLayout(this);
            tile.setOrientation(LinearLayout.VERTICAL);
            tile.setGravity(Gravity.CENTER);
            tile.setPadding(dp(2), dp(4), dp(2), dp(4));
            tile.setClickable(true);
            tile.setFocusable(true);
            tile.setFocusableInTouchMode(false);
            tile.setLongClickable(true);
            tile.setTag("home:" + absoluteIndex);

            ImageView icon = new ImageView(this);
            icon.setImageDrawable(info.loadIcon(getPackageManager()));
            icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            if (a.monochrome) {
                ColorMatrix matrix = new ColorMatrix();
                matrix.setSaturation(0f);
                icon.setColorFilter(new ColorMatrixColorFilter(matrix));
            }
            int iconFrameSize = clamp(screenWidthDp() / 4 - 12, 36, 54);
            int iconSize = iconFrameSize - 11;
            FrameLayout iconFrame = new FrameLayout(this);
            GradientDrawable iconBackground = new GradientDrawable();
            if (ThemeManager.APPEARANCE_PIXEL.equals(a.id) ||
                    ThemeManager.APPEARANCE_NOTHING.equals(a.id)) {
                iconBackground.setShape(GradientDrawable.OVAL);
            } else {
                iconBackground.setShape(GradientDrawable.RECTANGLE);
                iconBackground.setCornerRadius(dp(14));
            }
            iconBackground.setColor(a.tile);
            iconBackground.setStroke(dp(1), a.monochrome ? a.border : a.surfaceAlt);
            iconFrame.setBackgroundDrawable(iconBackground);
            FrameLayout.LayoutParams iconParams = new FrameLayout.LayoutParams(
                    dp(iconSize), dp(iconSize), Gravity.CENTER);
            iconFrame.addView(icon, iconParams);
            LinearLayout.LayoutParams frameParams = new LinearLayout.LayoutParams(
                    dp(iconFrameSize), dp(iconFrameSize));
            frameParams.gravity = Gravity.CENTER_HORIZONTAL;
            frameParams.bottomMargin = dp(4);
            tile.addView(iconFrame, frameParams);

            TextView label = new TextView(this);
            label.setText(info.loadLabel(getPackageManager()));
            tile.setContentDescription(label.getText() + ". Long press to edit.");
            label.setTextColor(a.text);
            label.setTextSize(screenWidthDp() < 210 ? 8 : 10);
            label.setGravity(Gravity.CENTER);
            label.setSingleLine(true);
            label.setEllipsize(android.text.TextUtils.TruncateAt.END);
            tile.addView(label, new LinearLayout.LayoutParams(-1, -2));

            boolean selected = editMode && selectedHomeIndex == absoluteIndex;
            styleSurface(tile, selected ? a.tileActive : 0x00000000,
                    selected ? a.accent : 0x00000000, a.focus,
                    a.monochrome ? 5f : 17f);
            tile.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    if (editMode) {
                        selectedHomeIndex = absoluteIndex;
                        showHome(false);
                    } else {
                        try { startActivity(launch); }
                        catch (RuntimeException ignored) {
                            android.widget.Toast.makeText(QinsLauncherActivity.this,
                                    "App unavailable", android.widget.Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });
            tile.setOnLongClickListener(new View.OnLongClickListener() {
                @Override public boolean onLongClick(View v) {
                    enterEditMode(absoluteIndex);
                    return true;
                }
            });
            appRow.addView(tile, new LinearLayout.LayoutParams(0, dp(homeAppRowHeightDp - 2), 1));
        }
    }

    private Intent launchIntent(ResolveInfo info) {
        Intent launch = new Intent(Intent.ACTION_MAIN);
        launch.addCategory(Intent.CATEGORY_LAUNCHER);
        launch.setClassName(info.activityInfo.packageName, info.activityInfo.name);
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return launch;
    }

    private void updatePageDots() {
        if (pageDots == null) return;
        StringBuilder dots = new StringBuilder();
        int count = pageCount();
        int visible = Math.min(7, count);
        int start = Math.max(0, Math.min(page - 3, count - visible));
        for (int i = 0; i < visible; i++) {
            int p = start + i;
            dots.append(p == page ? "●" : "○");
            if (i < visible - 1) dots.append("  ");
        }
        pageDots.setText(dots.toString());
    }

    private void changePage(int delta) {
        int target = page + delta;
        if (target < 0 || target >= pageCount()) return;
        page = target;
        editMode = false;
        selectedHomeIndex = -1;
        showHome(false);
    }

    private void enterEditMode(int absoluteIndex) {
        if (absoluteIndex < 0 || absoluteIndex >= pinnedApps.size()) return;
        editMode = true;
        selectedHomeIndex = absoluteIndex;
        page = absoluteIndex / APPS_PER_PAGE;
        showHome(false);
        focusHomeIndex(absoluteIndex);
    }

    private void focusHomeIndex(int absoluteIndex) {
        if (appRow == null) return;
        int slot = absoluteIndex - page * APPS_PER_PAGE;
        if (slot >= 0 && slot < appRow.getChildCount()) appRow.getChildAt(slot).requestFocus();
    }

    private void buildEditBar(ThemeManager.Appearance a) {
        editBar = new LinearLayout(this);
        editBar.setOrientation(LinearLayout.HORIZONTAL);
        editBar.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(-1, dp(44));
        barParams.topMargin = dp(7);
        pageContent.addView(editBar, barParams);

        addEditAction("left", a, "Move app left", new View.OnClickListener() {
            @Override public void onClick(View v) { moveSelectedApp(-1); }
        });
        addEditAction("right", a, "Move app right", new View.OnClickListener() {
            @Override public void onClick(View v) { moveSelectedApp(1); }
        });
        addEditAction("add", a, "Add applications", new View.OnClickListener() {
            @Override public void onClick(View v) { showDrawer(true); }
        });
        addEditAction("remove", a, "Remove app from Home", new View.OnClickListener() {
            @Override public void onClick(View v) { removeSelectedApp(); }
        });
        addEditAction("done", a, "Finish editing", new View.OnClickListener() {
            @Override public void onClick(View v) {
                editMode = false;
                selectedHomeIndex = -1;
                showHome(false);
            }
        });
    }

    private void addEditAction(String glyph, ThemeManager.Appearance a, String description,
                               View.OnClickListener listener) {
        QinsGlyphView button = new QinsGlyphView(this, glyph, a.text);
        button.setContentDescription(description);
        button.setFocusable(true);
        button.setFocusableInTouchMode(false);
        styleSurface(button, a.surface, a.border, a.focus, a.cornerDp);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -1, 1);
        params.setMargins(dp(3), 0, dp(3), 0);
        editBar.addView(button, params);
        button.setOnClickListener(listener);
    }

    private void moveSelectedApp(int delta) {
        int target = selectedHomeIndex + delta;
        if (selectedHomeIndex < 0 || target < 0 || target >= pinnedApps.size()) return;
        Collections.swap(pinnedApps, selectedHomeIndex, target);
        selectedHomeIndex = target;
        page = target / APPS_PER_PAGE;
        savePinnedApps();
        showHome(false);
        focusHomeIndex(target);
    }

    private void removeSelectedApp() {
        if (selectedHomeIndex < 0 || selectedHomeIndex >= pinnedApps.size()) return;
        pinnedApps.remove(selectedHomeIndex);
        savePinnedApps();
        if (selectedHomeIndex >= pinnedApps.size()) selectedHomeIndex = pinnedApps.size() - 1;
        if (selectedHomeIndex < 0) page = 0;
        else page = selectedHomeIndex / APPS_PER_PAGE;
        editMode = false;
        showHome(false);
    }

    private void showDrawer(boolean chooseToPin) {
        drawerVisible = true;
        quickPanelVisible = false;
        pinSelectionMode = chooseToPin;
        editMode = false;
        pageContent.removeAllViews();
        ThemeManager.Appearance a = appearance();
        updateNavigationButton();

        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dp(4), dp(5), dp(4), dp(8));
        scroll.addView(list, new ScrollView.LayoutParams(-1, -2));
        pageContent.addView(scroll, new LinearLayout.LayoutParams(-1, -1));

        for (final ResolveInfo info : installedApps) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(9), dp(4), dp(10), dp(4));
            row.setFocusable(true);
            row.setFocusableInTouchMode(false);
            row.setClickable(true);
            row.setLongClickable(true);
            row.setTag("drawer:" + componentKey(info));
            row.setContentDescription(info.loadLabel(getPackageManager()) + ". Press to open, long press to pin.");
            styleSurface(row, a.surface, a.border, a.focus, a.cornerDp);

            ImageView icon = new ImageView(this);
            icon.setImageDrawable(info.loadIcon(getPackageManager()));
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(34), dp(34));
            iconParams.rightMargin = dp(10);
            row.addView(icon, iconParams);

            TextView label = new TextView(this);
            label.setText(info.loadLabel(getPackageManager()));
            label.setTextColor(a.text);
            label.setTextSize(12);
            label.setSingleLine(true);
            label.setEllipsize(android.text.TextUtils.TruncateAt.END);
            row.addView(label, new LinearLayout.LayoutParams(0, -2, 1));

            TextView mark = new TextView(this);
            mark.setText(isPinned(info) ? "●" : "＋");
            mark.setTextColor(isPinned(info) ? a.accent : a.muted);
            mark.setTextSize(13);
            row.addView(mark, new LinearLayout.LayoutParams(dp(25), -2));

            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, dp(48));
            rowParams.bottomMargin = dp(4);
            list.addView(row, rowParams);
            row.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    if (pinSelectionMode) {
                        pinApp(info, true);
                    } else {
                        try { startActivity(launchIntent(info)); }
                        catch (RuntimeException ignored) { }
                    }
                }
            });
            row.setOnLongClickListener(new View.OnLongClickListener() {
                @Override public boolean onLongClick(View v) {
                    pinApp(info, false);
                    return true;
                }
            });
        }
        if (list.getChildCount() > 0) list.getChildAt(0).requestFocus();
        else appsNavButton.requestFocus();
    }

    private void pinApp(ResolveInfo info, boolean returnHome) {
        if (isPinned(info)) {
            android.widget.Toast.makeText(this, R.string.launcher_already_pinned,
                    android.widget.Toast.LENGTH_SHORT).show();
            if (returnHome) showHome(false);
            else showDrawer(false);
            return;
        }
        pinnedApps.add(info);
        savePinnedApps();
        if (returnHome) {
            selectedHomeIndex = pinnedApps.size() - 1;
            page = selectedHomeIndex / APPS_PER_PAGE;
            editMode = true;
            showHome(false);
            focusHomeIndex(selectedHomeIndex);
        } else {
            showDrawer(false);
            android.widget.Toast.makeText(this, R.string.launcher_added,
                    android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    private void showQuickPanel(boolean focusFirst) {
        quickPanelVisible = true;
        quickPanelExpanded = true;
        drawerVisible = false;
        pinSelectionMode = false;
        editMode = false;
        pageContent.removeAllViews();
        ThemeManager.Appearance a = appearance();
        pageContent.setBackground(new QinsWallpaperDrawable(a));
        updateNavigationButton();

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(2), dp(4), dp(2), dp(10));
        scroll.addView(panel, new ScrollView.LayoutParams(-1, -2));
        pageContent.addView(scroll, new LinearLayout.LayoutParams(-1, -1));

        LinearLayout heading = new LinearLayout(this);
        heading.setOrientation(LinearLayout.HORIZONTAL);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        panelClock = new TextView(this);
        panelClock.setTextColor(a.text);
        panelClock.setTextSize(screenWidthDp() < 210 ? 29 : 34);
        panelClock.setIncludeFontPadding(false);
        panelClock.setTypeface(a.useMonoType ? Typeface.MONOSPACE : Typeface.DEFAULT, Typeface.NORMAL);
        heading.addView(panelClock, new LinearLayout.LayoutParams(0, -2, 1));

        panelBattery = new TextView(this);
        panelBattery.setTextColor(a.muted);
        panelBattery.setTextSize(12);
        heading.addView(panelBattery, new LinearLayout.LayoutParams(-2, -2));
        panel.addView(heading);

        panelDate = new TextView(this);
        panelDate.setTextColor(a.muted);
        panelDate.setTextSize(12);
        LinearLayout.LayoutParams dateParams = new LinearLayout.LayoutParams(-1, -2);
        dateParams.bottomMargin = dp(14);
        panel.addView(panelDate, dateParams);

        LinearLayout firstRow = new LinearLayout(this);
        firstRow.setOrientation(LinearLayout.HORIZONTAL);
        int quickTileHeight = clamp((int) (screenHeightDp() * 0.27f), 64, 78);
        panel.addView(firstRow, new LinearLayout.LayoutParams(-1, dp(quickTileHeight)));
        View wifi = quickTile("wifi", "Wi-Fi", a);
        firstRow.addView(wifi, new LinearLayout.LayoutParams(0, -1, 1));
        View bluetooth = quickTile("bluetooth", "Bluetooth", a);
        LinearLayout.LayoutParams btParams = new LinearLayout.LayoutParams(0, -1, 1);
        btParams.leftMargin = dp(7);
        firstRow.addView(bluetooth, btParams);
        wifi.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { toggleWifi(); }
        });
        bluetooth.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { toggleBluetooth(); }
        });

        LinearLayout secondRow = new LinearLayout(this);
        secondRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams secondParams = new LinearLayout.LayoutParams(-1, dp(quickTileHeight));
        secondParams.topMargin = dp(7);
        panel.addView(secondRow, secondParams);
        View sound = quickTile("sound", "Sound", a);
        secondRow.addView(sound, new LinearLayout.LayoutParams(0, -1, 1));
        View media = quickTile("apps", "Settings", a);
        LinearLayout.LayoutParams mediaParams = new LinearLayout.LayoutParams(0, -1, 1);
        mediaParams.leftMargin = dp(7);
        secondRow.addView(media, mediaParams);
        sound.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { toggleSound(); }
        });
        media.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { openSystemSettings(); }
        });

        updateQuickStates();

        addSlider(panel, "☼", "Brightness", readBrightness(), 255,
                new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int value, boolean fromUser) {
                if (fromUser) writeBrightness(Math.max(1, value));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override public void onStopTrackingTouch(SeekBar seekBar) { }
        }, a);

        AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int maxVolume = audio == null ? 15 : Math.max(1, audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
        int currentVolume = audio == null ? 0 : audio.getStreamVolume(AudioManager.STREAM_MUSIC);
        addSlider(panel, "♪", "Volume", currentVolume, maxVolume,
                new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int value, boolean fromUser) {
                if (fromUser) {
                    AudioManager manager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                    if (manager != null) manager.setStreamVolume(AudioManager.STREAM_MUSIC, value, 0);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override public void onStopTrackingTouch(SeekBar seekBar) { }
        }, a);

        TextView styleTitle = new TextView(this);
        styleTitle.setText("Style");
        styleTitle.setTextColor(a.muted);
        styleTitle.setTextSize(11);
        LinearLayout.LayoutParams styleTitleParams = new LinearLayout.LayoutParams(-1, -2);
        styleTitleParams.topMargin = dp(10);
        styleTitleParams.bottomMargin = dp(4);
        panel.addView(styleTitle, styleTitleParams);

        LinearLayout styles = new LinearLayout(this);
        styles.setOrientation(LinearLayout.HORIZONTAL);
        panel.addView(styles, new LinearLayout.LayoutParams(-1, dp(42)));
        addStyleButton(styles, "GALAXY", ThemeManager.APPEARANCE_GALAXY, a);
        addStyleButton(styles, "PIXEL", ThemeManager.APPEARANCE_PIXEL, a);
        addStyleButton(styles, "NOTHING", ThemeManager.APPEARANCE_NOTHING, a);

        updateClockAndBattery();
        if (focusFirst) wifi.requestFocus();
    }

    private View quickTile(String symbol, String label, ThemeManager.Appearance a) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER);
        tile.setPadding(dp(3), dp(3), dp(3), dp(3));
        QinsGlyphView icon = new QinsGlyphView(this, symbol, a.accent);
        tile.addView(icon, new LinearLayout.LayoutParams(-1, dp(31)));

        LinearLayout textBlock = new LinearLayout(this);
        textBlock.setOrientation(LinearLayout.VERTICAL);
        textBlock.setGravity(Gravity.CENTER);
        TextView name = new TextView(this);
        name.setText(label);
        name.setTextColor(a.text);
        name.setTextSize(10);
        textBlock.addView(name);
        TextView state = new TextView(this);
        state.setTextColor(a.muted);
        state.setTextSize(8);
        textBlock.addView(state);
        tile.addView(textBlock, new LinearLayout.LayoutParams(-1, -2));
        tile.setFocusable(true);
        tile.setFocusableInTouchMode(false);
        tile.setClickable(true);
        styleSurface(tile, a.tile, a.border, a.tileActive, a.cornerDp);
        if ("Wi-Fi".equals(label)) wifiState = state;
        else if ("Bluetooth".equals(label)) bluetoothState = state;
        else if ("Sound".equals(label)) soundState = state;
        return tile;
    }

    private void addSlider(LinearLayout panel, String symbol, String label, int value, int max,
                           SeekBar.OnSeekBarChangeListener listener, ThemeManager.Appearance a) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(8), dp(14), dp(7));
        styleSurface(card, a.surface, a.border, a.focus, a.cornerDp);
        LinearLayout heading = new LinearLayout(this);
        heading.setOrientation(LinearLayout.HORIZONTAL);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        TextView icon = new TextView(this);
        icon.setText(symbol);
        icon.setTextColor(a.accent);
        icon.setTextSize(18);
        heading.addView(icon, new LinearLayout.LayoutParams(dp(28), -2));
        TextView title = new TextView(this);
        title.setText(label);
        title.setTextColor(a.text);
        title.setTextSize(12);
        heading.addView(title);
        card.addView(heading);

        SeekBar seek = new SeekBar(this);
        seek.setMax(max);
        seek.setProgress(Math.max(0, Math.min(max, value)));
        seek.setFocusable(true);
        seek.setFocusableInTouchMode(false);
        seek.setOnSeekBarChangeListener(listener);
        LinearLayout.LayoutParams seekParams = new LinearLayout.LayoutParams(-1, dp(34));
        seekParams.topMargin = dp(2);
        card.addView(seek, seekParams);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
        cardParams.topMargin = dp(8);
        panel.addView(card, cardParams);
    }

    private void addStyleButton(LinearLayout parent, final String label, final String id,
                                ThemeManager.Appearance current) {
        final ThemeManager.Appearance style = ThemeManager.getAppearanceById(id);
        TextView button = new TextView(this);
        button.setText(label);
        button.setTextSize(8);
        button.setGravity(Gravity.CENTER);
        button.setTextColor(style.text);
        button.setFocusable(true);
        button.setFocusableInTouchMode(false);
        styleSurface(button, id.equals(current.id) ? style.tileActive : style.surface,
                style.border, style.focus, style.cornerDp);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -1, 1);
        if (parent.getChildCount() > 0) params.leftMargin = dp(6);
        parent.addView(button, params);
        button.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                ThemeManager.applyAppearance(QinsLauncherActivity.this, id);
                showQuickPanel(true);
            }
        });
    }

    private void updateQuickStates() {
        if (wifiState != null) {
            try {
                WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                wifiState.setText(wifi != null && wifi.isWifiEnabled() ? "On" : "Off");
            } catch (RuntimeException ignored) { wifiState.setText("—"); }
        }
        if (bluetoothState != null) {
            try {
                BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
                bluetoothState.setText(adapter != null && adapter.isEnabled() ? "On" : "Off");
            } catch (RuntimeException ignored) { bluetoothState.setText("—"); }
        }
        if (soundState != null) {
            try {
                AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                soundState.setText(audio != null && audio.getRingerMode() == AudioManager.RINGER_MODE_SILENT
                        ? "Silent" : "On");
            } catch (RuntimeException ignored) { soundState.setText("—"); }
        }
    }

    private void toggleWifi() {
        try {
            WifiManager manager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (manager != null) manager.setWifiEnabled(!manager.isWifiEnabled());
            updateQuickStates();
        } catch (RuntimeException error) {
            android.widget.Toast.makeText(this, "Wi-Fi unavailable", android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleBluetooth() {
        try {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter == null) return;
            if (adapter.isEnabled()) adapter.disable();
            else adapter.enable();
            updateQuickStates();
        } catch (RuntimeException error) {
            android.widget.Toast.makeText(this, "Bluetooth unavailable", android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleSound() {
        try {
            AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            if (audio == null) return;
            audio.setRingerMode(audio.getRingerMode() == AudioManager.RINGER_MODE_SILENT
                    ? AudioManager.RINGER_MODE_NORMAL : AudioManager.RINGER_MODE_SILENT);
            updateQuickStates();
        } catch (RuntimeException error) {
            android.widget.Toast.makeText(this, "Sound unavailable", android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    private int readBrightness() {
        try {
            return Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
        } catch (Settings.SettingNotFoundException ignored) { return 128; }
        catch (RuntimeException ignored) { return 128; }
    }

    private void writeBrightness(int value) {
        try {
            Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
            Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS,
                    Math.max(1, Math.min(255, value)));
        } catch (RuntimeException ignored) { }
    }

    private void openStudio() {
        try { startActivity(new Intent(this, MainActivity.class)); }
        catch (RuntimeException error) {
            android.widget.Toast.makeText(this, "Studio unavailable", android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isSelectKey(int keyCode) {
        return keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER ||
                keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER || keyCode == KeyEvent.KEYCODE_BUTTON_A;
    }

    private boolean performLongPressAction(View target) {
        if (target == null || !(target.getTag() instanceof String)) return false;
        String tag = (String) target.getTag();
        if (tag.startsWith("home:")) {
            try { enterEditMode(Integer.parseInt(tag.substring(5))); return true; }
            catch (NumberFormatException ignored) { return false; }
        }
        if (tag.startsWith("drawer:")) {
            ResolveInfo info = findApp(tag.substring(7));
            if (info != null) { pinApp(info, false); return true; }
        }
        return false;
    }

    @Override public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        if (isSelectKey(keyCode) && performLongPressAction(
                pendingSelectView != null ? pendingSelectView : getCurrentFocus())) {
            longPressPerformed = true;
            return true;
        }
        return super.onKeyLongPress(keyCode, event);
    }

    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (isSelectKey(keyCode)) {
            View focused = getCurrentFocus();
            if (focused != null && focused.getTag() instanceof String &&
                    (((String) focused.getTag()).startsWith("home:") ||
                     ((String) focused.getTag()).startsWith("drawer:"))) {
                if (event.getRepeatCount() == 0) {
                    pendingSelectView = focused;
                    longPressPerformed = false;
                    event.startTracking();
                    return true;
                }
                if (!longPressPerformed && (event.isLongPress() || event.getRepeatCount() >= 1)) {
                    longPressPerformed = performLongPressAction(pendingSelectView);
                }
                return true;
            }
        }
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            openStudio();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (editMode) {
                editMode = false;
                selectedHomeIndex = -1;
                showHome(false);
                return true;
            }
            if (drawerVisible || quickPanelVisible) {
                showHome(false);
                return true;
            }
        }
        if (!quickPanelVisible && !drawerVisible && (keyCode == KeyEvent.KEYCODE_DPAD_LEFT ||
                keyCode == KeyEvent.KEYCODE_DPAD_RIGHT)) {
            View focused = getCurrentFocus();
            Object tag = focused == null ? null : focused.getTag();
            if (tag instanceof String && ((String) tag).startsWith("home:")) {
                try {
                    int index = Integer.parseInt(((String) tag).substring(5));
                    if (editMode) {
                        moveSelectedApp(keyCode == KeyEvent.KEYCODE_DPAD_LEFT ? -1 : 1);
                        return true;
                    }
                    int slot = index % APPS_PER_PAGE;
                    if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT && slot == 0 && page > 0) {
                        changePage(-1);
                        return true;
                    }
                    if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT && slot == APPS_PER_PAGE - 1 &&
                            page + 1 < pageCount()) {
                        changePage(1);
                        return true;
                    }
                } catch (NumberFormatException ignored) { }
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (isSelectKey(keyCode) && pendingSelectView != null) {
            View target = pendingSelectView;
            pendingSelectView = null;
            if (!longPressPerformed) target.performClick();
            longPressPerformed = false;
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override public void onBackPressed() {
        if (editMode) {
            editMode = false;
            selectedHomeIndex = -1;
            showHome(false);
        } else if (drawerVisible || quickPanelVisible) {
            showHome(false);
        }
    }
}