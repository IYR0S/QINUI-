package com.qins.uistudio;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Typeface;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
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
 * QINS Home: minimal, four-app-per-page launcher with an integrated Quick Panel.
 * Visual tokens can switch between Galaxy, Pixel and Nothing-inspired styles.
 * All layouts and APIs are compatible with Android 4.4 / API 19.
 */
public class QinsLauncherActivity extends Activity {
    private static final int APPS_PER_PAGE = 4;
    private int page = 0;
    private boolean quickPanelVisible = false;

    private LinearLayout root;
    private LinearLayout pageContent;
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
    private LinearLayout appRow;
    private View bottomHandle;
    private final List<ResolveInfo> installedApps = new ArrayList<ResolveInfo>();
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

    private ThemeManager.Appearance appearance() {
        return ThemeManager.getAppearance(this);
    }

    @Override public void onCreate(Bundle state) {
        setTheme(R.style.AppTheme);
        super.onCreate(state);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        loadApps();
        buildShell();
        quickPanelVisible = getIntent().getBooleanExtra("open_quick_panel", false);
        if (quickPanelVisible) showQuickPanel(false);
        else showHome(false);
    }

    @Override protected void onResume() {
        super.onResume();
        loadApps();
        registerSystemReceiver();
        if (quickPanelVisible) showQuickPanel(false);
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
        root.setPadding(dp(14), dp(8), dp(14), dp(6));
        root.setBackground(makeBackdrop(a));

        pageContent = new LinearLayout(this);
        pageContent.setOrientation(LinearLayout.VERTICAL);
        root.addView(pageContent, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout footer = new LinearLayout(this);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, dp(4), 0, dp(2));
        bottomHandle = new TextView(this);
        TextView handle = (TextView) bottomHandle;
        handle.setText("⌄");
        handle.setTextSize(23);
        handle.setGravity(Gravity.CENTER);
        handle.setTextColor(a.text);
        handle.setContentDescription("Open quick settings");
        handle.setFocusable(true);
        handle.setFocusableInTouchMode(false);
        styleSurface(handle, a.surface, a.border, a.focus, a.cornerDp);
        handle.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (quickPanelVisible) showHome(true);
                else showQuickPanel(true);
            }
        });
        footer.addView(handle, new LinearLayout.LayoutParams(dp(78), dp(38)));
        root.addView(footer, new LinearLayout.LayoutParams(-1, dp(44)));
        setContentView(root);
    }

    private void applyAppearanceToShell() {
        ThemeManager.Appearance a = appearance();
        root.setBackground(makeBackdrop(a));
        TextView handle = (TextView) bottomHandle;
        handle.setText(quickPanelVisible ? "⌃" : "⌄");
        handle.setTextColor(a.text);
        styleSurface(handle, a.surface, a.border, a.focus, a.cornerDp);
    }

    private void styleSurface(final View view, final int fill, final int border,
                              final int focus, final float cornerDp) {
        view.setBackground(makeShape(fill, border, cornerDp, false));
        view.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override public void onFocusChange(View v, boolean hasFocus) {
                v.setBackground(makeShape(hasFocus ? focus : fill,
                        hasFocus ? appearance().accent : border, cornerDp, hasFocus));
            }
        });
    }

    private Drawable makeBackdrop(ThemeManager.Appearance a) {
        if (a.monochrome) return new GradientDrawable(
                GradientDrawable.Orientation.TL_BR, new int[] {a.background, a.background});
        return new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[] {a.backgroundAlt, a.background});
    }

    private GradientDrawable makeShape(int fill, int stroke, float radiusDp, boolean focused) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(radiusDp));
        d.setStroke(dp(focused ? 2 : 1), stroke);
        return d;
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

    private void loadApps() {
        Intent query = new Intent(Intent.ACTION_MAIN, null);
        query.addCategory(Intent.CATEGORY_LAUNCHER);
        PackageManager pm = getPackageManager();
        installedApps.clear();
        List<ResolveInfo> found = pm.queryIntentActivities(query, 0);
        for (ResolveInfo info : found) {
            // Studio and diagnostics are opened through the physical MENU key, not mixed into the app pages.
            if (!getPackageName().equals(info.activityInfo.packageName)) installedApps.add(info);
        }
        Collections.sort(installedApps, new Comparator<ResolveInfo>() {
            @Override public int compare(ResolveInfo a, ResolveInfo b) {
                return a.loadLabel(getPackageManager()).toString()
                        .compareToIgnoreCase(b.loadLabel(getPackageManager()).toString());
            }
        });
        int count = pageCount();
        if (page >= count) page = Math.max(0, count - 1);
    }

    private int pageCount() {
        return Math.max(1, (installedApps.size() + APPS_PER_PAGE - 1) / APPS_PER_PAGE);
    }

    private void showHome(boolean focusHandle) {
        quickPanelVisible = false;
        pageContent.removeAllViews();
        ThemeManager.Appearance a = appearance();
        pageContent.setBackground(makeBackdrop(a));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(dp(18), dp(12), dp(18), dp(12));
        top.setBackground(makeShape(a.surface, a.border, a.cornerDp + 4, false));

        LinearLayout timeBlock = new LinearLayout(this);
        timeBlock.setOrientation(LinearLayout.VERTICAL);
        timeBlock.setGravity(Gravity.CENTER_VERTICAL);
        clockView = new TextView(this);
        clockView.setTextColor(a.text);
        clockView.setTextSize(42);
        clockView.setIncludeFontPadding(false);
        clockView.setTypeface(a.useMonoType ? Typeface.MONOSPACE : Typeface.DEFAULT, Typeface.NORMAL);
        timeBlock.addView(clockView, new LinearLayout.LayoutParams(-1, -2));

        dateView = new TextView(this);
        dateView.setTextColor(a.muted);
        dateView.setTextSize(11);
        dateView.setSingleLine(true);
        timeBlock.addView(dateView, new LinearLayout.LayoutParams(-1, -2));
        top.addView(timeBlock, new LinearLayout.LayoutParams(0, -2, 1));

        batteryView = new TextView(this);
        batteryView.setTextColor(a.muted);
        batteryView.setTextSize(12);
        batteryView.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        top.addView(batteryView, new LinearLayout.LayoutParams(-2, -1));
        LinearLayout.LayoutParams topParams = new LinearLayout.LayoutParams(-1, -2);
        topParams.topMargin = dp(9);
        topParams.bottomMargin = dp(10);
        pageContent.addView(top, topParams);

        View spacer = new View(this);
        pageContent.addView(spacer, new LinearLayout.LayoutParams(1, 0, 1));

        appRow = new LinearLayout(this);
        appRow.setOrientation(LinearLayout.HORIZONTAL);
        appRow.setGravity(Gravity.CENTER);
        pageContent.addView(appRow, new LinearLayout.LayoutParams(-1, dp(112)));
        populateHomeApps(a);

        LinearLayout pages = new LinearLayout(this);
        pages.setGravity(Gravity.CENTER);
        pages.setOrientation(LinearLayout.HORIZONTAL);
        TextView previous = pageArrow("‹", a);
        TextView next = pageArrow("›", a);
        pageDots = new TextView(this);
        pageDots.setTextColor(a.muted);
        pageDots.setTextSize(14);
        pageDots.setGravity(Gravity.CENTER);
        pages.addView(previous, new LinearLayout.LayoutParams(dp(38), dp(32)));
        pages.addView(pageDots, new LinearLayout.LayoutParams(dp(100), dp(32)));
        pages.addView(next, new LinearLayout.LayoutParams(dp(38), dp(32)));
        LinearLayout.LayoutParams pageParams = new LinearLayout.LayoutParams(-1, dp(36));
        pageParams.topMargin = dp(5);
        pageContent.addView(pages, pageParams);
        previous.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { changePage(-1); }
        });
        next.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { changePage(1); }
        });
        updatePageDots();
        updateClockAndBattery();
        applyAppearanceToShell();
        if (focusHandle) bottomHandle.requestFocus();
        else if (appRow.getChildCount() > 0) appRow.getChildAt(0).requestFocus();
    }

    private TextView pageArrow(String symbol, ThemeManager.Appearance a) {
        TextView button = new TextView(this);
        button.setText(symbol);
        button.setTextSize(25);
        button.setGravity(Gravity.CENTER);
        button.setTextColor(a.muted);
        button.setFocusable(true);
        button.setFocusableInTouchMode(false);
        styleSurface(button, a.backgroundAlt, a.border, a.focus, a.cornerDp);
        return button;
    }

    private void populateHomeApps(ThemeManager.Appearance a) {
        appRow.removeAllViews();
        int start = page * APPS_PER_PAGE;
        for (int slot = 0; slot < APPS_PER_PAGE; slot++) {
            int index = start + slot;
            if (index >= installedApps.size()) {
                View empty = new View(this);
                appRow.addView(empty, new LinearLayout.LayoutParams(0, dp(100), 1));
                continue;
            }
            final ResolveInfo info = installedApps.get(index);
            final Intent launch = new Intent(Intent.ACTION_MAIN);
            launch.addCategory(Intent.CATEGORY_LAUNCHER);
            launch.setClassName(info.activityInfo.packageName, info.activityInfo.name);
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            LinearLayout tile = new LinearLayout(this);
            tile.setOrientation(LinearLayout.VERTICAL);
            tile.setGravity(Gravity.CENTER);
            tile.setPadding(dp(2), dp(5), dp(2), dp(5));
            tile.setClickable(true);
            tile.setFocusable(true);
            tile.setFocusableInTouchMode(false);
            tile.setTag(Integer.valueOf(slot));

            ImageView icon = new ImageView(this);
            icon.setImageDrawable(info.loadIcon(getPackageManager()));
            icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            if (a.monochrome) {
                ColorMatrix matrix = new ColorMatrix();
                matrix.setSaturation(0f);
                icon.setColorFilter(new ColorMatrixColorFilter(matrix));
            }
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(40), dp(40));
            iconParams.bottomMargin = dp(5);
            tile.addView(icon, iconParams);

            TextView label = new TextView(this);
            label.setText(info.loadLabel(getPackageManager()));
            label.setTextColor(a.text);
            label.setTextSize(10);
            label.setGravity(Gravity.CENTER);
            label.setSingleLine(true);
            label.setEllipsize(android.text.TextUtils.TruncateAt.END);
            tile.addView(label, new LinearLayout.LayoutParams(-1, -2));

            styleSurface(tile, 0x00000000, 0x00000000, a.focus, a.monochrome ? 5f : 17f);
            tile.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    try { startActivity(launch); }
                    catch (RuntimeException ignored) {
                        android.widget.Toast.makeText(QinsLauncherActivity.this,
                                "App unavailable", android.widget.Toast.LENGTH_SHORT).show();
                    }
                }
            });
            appRow.addView(tile, new LinearLayout.LayoutParams(0, dp(100), 1));
        }
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
        showHome(false);
    }

    private void showQuickPanel(boolean focusFirst) {
        quickPanelVisible = true;
        pageContent.removeAllViews();
        ThemeManager.Appearance a = appearance();
        pageContent.setBackgroundColor(a.background);

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
        panelClock.setTextSize(34);
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
        panel.addView(firstRow, new LinearLayout.LayoutParams(-1, dp(78)));
        View wifi = quickTile("◉", "Wi-Fi", a);
        firstRow.addView(wifi, new LinearLayout.LayoutParams(0, -1, 1));
        View bluetooth = quickTile("ᛒ", "Bluetooth", a);
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
        LinearLayout.LayoutParams secondParams = new LinearLayout.LayoutParams(-1, dp(78));
        secondParams.topMargin = dp(7);
        panel.addView(secondRow, secondParams);
        View sound = quickTile("♪", "Sound", a);
        secondRow.addView(sound, new LinearLayout.LayoutParams(0, -1, 1));
        View media = quickTile("−  +", "Volume", a);
        LinearLayout.LayoutParams mediaParams = new LinearLayout.LayoutParams(0, -1, 1);
        mediaParams.leftMargin = dp(7);
        secondRow.addView(media, mediaParams);
        sound.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { toggleSound(); }
        });
        media.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                if (audio != null) audio.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
            }
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

        LinearLayout styleHeader = new LinearLayout(this);
        styleHeader.setGravity(Gravity.CENTER_VERTICAL);
        styleHeader.setOrientation(LinearLayout.HORIZONTAL);
        TextView styleTitle = new TextView(this);
        styleTitle.setText("Appearance");
        styleTitle.setTextColor(a.muted);
        styleTitle.setTextSize(12);
        LinearLayout.LayoutParams styleHeaderParams = new LinearLayout.LayoutParams(-1, -2);
        styleHeaderParams.topMargin = dp(12);
        styleHeaderParams.bottomMargin = dp(6);
        panel.addView(styleTitle, styleHeaderParams);

        LinearLayout styles = new LinearLayout(this);
        styles.setOrientation(LinearLayout.HORIZONTAL);
        panel.addView(styles, new LinearLayout.LayoutParams(-1, dp(48)));
        addStyleButton(styles, "GALAXY", ThemeManager.APPEARANCE_GALAXY, a);
        addStyleButton(styles, "PIXEL", ThemeManager.APPEARANCE_PIXEL, a);
        addStyleButton(styles, "NOTHING", ThemeManager.APPEARANCE_NOTHING, a);

        updateClockAndBattery();
        applyAppearanceToShell();
        if (focusFirst) wifi.requestFocus();
    }

    private View quickTile(String symbol, String label, ThemeManager.Appearance a) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.HORIZONTAL);
        tile.setGravity(Gravity.CENTER_VERTICAL);
        tile.setPadding(dp(12), dp(7), dp(8), dp(7));
        TextView icon = new TextView(this);
        icon.setText(symbol);
        icon.setTextColor(a.accent);
        icon.setTextSize(24);
        icon.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(34), dp(40));
        tile.addView(icon, iconParams);

        LinearLayout textBlock = new LinearLayout(this);
        textBlock.setOrientation(LinearLayout.VERTICAL);
        textBlock.setGravity(Gravity.CENTER_VERTICAL);
        TextView name = new TextView(this);
        name.setText(label);
        name.setTextColor(a.text);
        name.setTextSize(12);
        textBlock.addView(name);
        TextView state = new TextView(this);
        state.setTextColor(a.muted);
        state.setTextSize(10);
        textBlock.addView(state);
        tile.addView(textBlock, new LinearLayout.LayoutParams(0, -2, 1));
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
        button.setTextSize(9);
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

    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            openStudio();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_BACK && quickPanelVisible) {
            showHome(true);
            return true;
        }
        if (!quickPanelVisible && (keyCode == KeyEvent.KEYCODE_DPAD_LEFT ||
                keyCode == KeyEvent.KEYCODE_DPAD_RIGHT)) {
            View focused = getCurrentFocus();
            Object tag = focused == null ? null : focused.getTag();
            if (tag instanceof Integer) {
                int slot = ((Integer) tag).intValue();
                if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT && slot == 0 && page > 0) {
                    changePage(-1);
                    return true;
                }
                if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT && slot == APPS_PER_PAGE - 1 &&
                        page + 1 < pageCount()) {
                    changePage(1);
                    return true;
                }
            }
        }
        if (!quickPanelVisible && keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            showQuickPanel(true);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override public void onBackPressed() {
        if (quickPanelVisible) showHome(true);
    }
}
