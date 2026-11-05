package com.qins.uistudio;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.AudioManager;
import android.net.wifi.WifiManager;
import android.bluetooth.BluetoothAdapter;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.graphics.drawable.Drawable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * QINS no-root home interface. Home and Quick Panel are pages in the same
 * Activity so users do not leave the QINS interface to open the panel.
 * Uses LinearLayouts instead of GridLayout for compatibility with the
 * Android 4.4 firmware on QINS1+.
 */
public class QinsLauncherActivity extends Activity {
    private int columns = 3;
    private int rows = 2;
    private int page = 0;
    private boolean quickPanelVisible = false;

    private LinearLayout root;
    private LinearLayout pageContent;
    private LinearLayout appRows;
    private TextView pageLabel;
    private Button previousButton;
    private Button nextButton;
    private Button wifiButton;
    private Button bluetoothButton;
    private Button soundButton;
    private TextView brightnessLabel;
    private List<ResolveInfo> installedApps = new ArrayList<ResolveInfo>();
    private boolean receiversRegistered = false;

    private final BroadcastReceiver stateReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            refreshControlLabels();
        }
    };

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override public void onCreate(Bundle state) {
        setTheme(R.style.AppTheme);
        super.onCreate(state);
        readLayout();
        buildShell();
        loadApps();
        if (getIntent().getBooleanExtra("open_quick_panel", false)) showQuickPanel();
        else showHome();
    }

    @Override protected void onResume() {
        super.onResume();
        readLayout();
        loadApps();
        if (quickPanelVisible) refreshControlLabels();
        else showHome();
        registerStateReceivers();
    }

    @Override protected void onPause() {
        unregisterStateReceivers();
        super.onPause();
    }

    private void registerStateReceivers() {
        if (receiversRegistered) return;
        try {
            IntentFilter filter = new IntentFilter();
            filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
            filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(stateReceiver, filter);
            receiversRegistered = true;
        } catch (RuntimeException ignored) { }
    }

    private void unregisterStateReceivers() {
        if (!receiversRegistered) return;
        try { unregisterReceiver(stateReceiver); } catch (RuntimeException ignored) { }
        receiversRegistered = false;
    }

    private void readLayout() {
        android.content.SharedPreferences prefs =
                getSharedPreferences("HomeStudioActivity", MODE_PRIVATE);
        columns = prefs.getInt("home_columns", 3);
        rows = prefs.getInt("home_rows", 2);
        if (columns < 2 || columns > 4) columns = 3;
        if (rows < 2 || rows > 3) rows = 2;
    }

    private void buildShell() {
        final ThemeManager.Palette palette = ThemeManager.getPalette(this);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(12), dp(8), dp(12), dp(8));
        root.setBackgroundColor(palette.background);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(this);
        title.setText("QINS");
        title.setTextColor(palette.text);
        title.setTextSize(24);
        title.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(title, new LinearLayout.LayoutParams(0, dp(48), 1));

        Button home = navButton("HOME", palette);
        home.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showHome(); }
        });
        header.addView(home, new LinearLayout.LayoutParams(-2, dp(44)));

        Button quick = navButton("QUICK", palette);
        quick.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showQuickPanel(); }
        });
        header.addView(quick, new LinearLayout.LayoutParams(-2, dp(44)));

        Button studio = navButton("STUDIO", palette);
        studio.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { openStudio(); }
        });
        header.addView(studio, new LinearLayout.LayoutParams(-2, dp(44)));
        root.addView(header);

        pageContent = new LinearLayout(this);
        pageContent.setOrientation(LinearLayout.VERTICAL);
        root.addView(pageContent, new LinearLayout.LayoutParams(-1, 0, 1));

        setContentView(root);
    }

    private Button navButton(String label, ThemeManager.Palette palette) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(11);
        KeyboardSupport.makeFocusable(b, palette.surface, palette.accent, palette.text, dp(6));
        return b;
    }

    private Button panelButton(String label, ThemeManager.Palette palette) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(14);
        KeyboardSupport.makeFocusable(b, palette.surface, palette.accent, palette.text, dp(8));
        return b;
    }

    private void showHome() {
        quickPanelVisible = false;
        pageContent.removeAllViews();
        final ThemeManager.Palette palette = ThemeManager.getPalette(this);

        LinearLayout intro = new LinearLayout(this);
        intro.setOrientation(LinearLayout.VERTICAL);
        TextView heading = new TextView(this);
        heading.setText("My apps");
        heading.setTextColor(palette.text);
        heading.setTextSize(18);
        intro.addView(heading);
        TextView hint = new TextView(this);
        hint.setText("Choose an app with the arrow keys and press OK");
        hint.setTextColor(palette.muted);
        hint.setTextSize(12);
        intro.addView(hint);
        LinearLayout.LayoutParams introParams = new LinearLayout.LayoutParams(-1, -2);
        introParams.bottomMargin = dp(6);
        pageContent.addView(intro, introParams);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        appRows = new LinearLayout(this);
        appRows.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(appRows, new ScrollView.LayoutParams(-1, -2));
        pageContent.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout paging = new LinearLayout(this);
        paging.setGravity(Gravity.CENTER_VERTICAL);
        paging.setOrientation(LinearLayout.HORIZONTAL);
        previousButton = panelButton("PREVIOUS", palette);
        nextButton = panelButton("NEXT", palette);
        pageLabel = new TextView(this);
        pageLabel.setGravity(Gravity.CENTER);
        pageLabel.setTextColor(palette.text);
        pageLabel.setTextSize(12);
        paging.addView(previousButton, new LinearLayout.LayoutParams(0, dp(42), 1));
        paging.addView(pageLabel, new LinearLayout.LayoutParams(0, -2, 1));
        paging.addView(nextButton, new LinearLayout.LayoutParams(0, dp(42), 1));
        pageContent.addView(paging);

        previousButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { changePage(-1); }
        });
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { changePage(1); }
        });

        populateApps();
    }

    private void populateApps() {
        if (appRows == null || quickPanelVisible) return;
        appRows.removeAllViews();
        final ThemeManager.Palette palette = ThemeManager.getPalette(this);
        int pageSize = Math.max(1, columns * rows);
        int start = page * pageSize;
        int end = Math.min(installedApps.size(), start + pageSize);
        PackageManager pm = getPackageManager();

        for (int row = 0; row < rows; row++) {
            LinearLayout appRow = new LinearLayout(this);
            appRow.setOrientation(LinearLayout.HORIZONTAL);
            appRows.addView(appRow, new LinearLayout.LayoutParams(-1, -2));

            for (int column = 0; column < columns; column++) {
                int index = start + row * columns + column;
                if (index >= end) {
                    View empty = new View(this);
                    appRow.addView(empty, new LinearLayout.LayoutParams(0, dp(92), 1));
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
                tile.setPadding(dp(3), dp(5), dp(3), dp(5));
                tile.setClickable(true);

                ImageView icon = new ImageView(this);
                Drawable drawable = info.loadIcon(pm);
                icon.setImageDrawable(drawable);
                LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(40), dp(40));
                iconParams.gravity = Gravity.CENTER;
                tile.addView(icon, iconParams);

                TextView label = new TextView(this);
                label.setText(info.loadLabel(pm));
                label.setTextColor(palette.text);
                label.setTextSize(11);
                label.setGravity(Gravity.CENTER);
                label.setSingleLine(true);
                label.setEllipsize(android.text.TextUtils.TruncateAt.END);
                tile.addView(label, new LinearLayout.LayoutParams(-1, -2));

                KeyboardSupport.makeFocusable(tile, palette.surface, palette.accent, palette.text, dp(7));
                tile.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        try { startActivity(launch); }
                        catch (RuntimeException error) {
                            android.widget.Toast.makeText(QinsLauncherActivity.this,
                                    "Cannot open this app", android.widget.Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                appRow.addView(tile, new LinearLayout.LayoutParams(0, dp(88), 1));
            }
        }

        int pageCount = Math.max(1, (installedApps.size() + pageSize - 1) / pageSize);
        pageLabel.setText((page + 1) + " / " + pageCount);
        previousButton.setEnabled(page > 0);
        nextButton.setEnabled(page + 1 < pageCount);
        if (appRows.getChildCount() > 0 && ((LinearLayout) appRows.getChildAt(0)).getChildCount() > 0) {
            ((LinearLayout) appRows.getChildAt(0)).getChildAt(0).requestFocus();
        } else {
            nextButton.requestFocus();
        }
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

    private void showQuickPanel() {
        quickPanelVisible = true;
        pageContent.removeAllViews();
        final ThemeManager.Palette palette = ThemeManager.getPalette(this);

        TextView title = new TextView(this);
        title.setText("QUICK PANEL");
        title.setTextColor(palette.text);
        title.setTextSize(20);
        pageContent.addView(title);

        TextView status = new TextView(this);
        status.setText(new SimpleDateFormat("EEE, d MMM  •  HH:mm", Locale.getDefault()).format(new Date())
                + "     " + batterySummary());
        status.setTextColor(palette.muted);
        status.setTextSize(12);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(-1, -2);
        statusParams.bottomMargin = dp(10);
        pageContent.addView(status, statusParams);

        LinearLayout rowOne = new LinearLayout(this);
        rowOne.setOrientation(LinearLayout.HORIZONTAL);
        pageContent.addView(rowOne, new LinearLayout.LayoutParams(-1, dp(62)));

        wifiButton = panelButton("", palette);
        rowOne.addView(wifiButton, new LinearLayout.LayoutParams(0, -1, 1));
        wifiButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { toggleWifi(); }
        });

        bluetoothButton = panelButton("", palette);
        LinearLayout.LayoutParams bluetoothParams = new LinearLayout.LayoutParams(0, -1, 1);
        bluetoothParams.leftMargin = dp(6);
        rowOne.addView(bluetoothButton, bluetoothParams);
        bluetoothButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { toggleBluetooth(); }
        });

        LinearLayout rowTwo = new LinearLayout(this);
        rowTwo.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rowTwoParams = new LinearLayout.LayoutParams(-1, dp(62));
        rowTwoParams.topMargin = dp(6);
        pageContent.addView(rowTwo, rowTwoParams);

        soundButton = panelButton("", palette);
        rowTwo.addView(soundButton, new LinearLayout.LayoutParams(0, -1, 1));
        soundButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { toggleSound(); }
        });

        Button volumeDown = panelButton("VOLUME −", palette);
        LinearLayout.LayoutParams downParams = new LinearLayout.LayoutParams(0, -1, 1);
        downParams.leftMargin = dp(6);
        rowTwo.addView(volumeDown, downParams);
        volumeDown.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { adjustVolume(false); }
        });

        Button volumeUp = panelButton("VOLUME +", palette);
        LinearLayout.LayoutParams upParams = new LinearLayout.LayoutParams(0, -1, 1);
        upParams.leftMargin = dp(6);
        rowTwo.addView(volumeUp, upParams);
        volumeUp.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { adjustVolume(true); }
        });

        TextView brightnessTitle = new TextView(this);
        brightnessTitle.setText("SCREEN BRIGHTNESS");
        brightnessTitle.setTextColor(palette.text);
        brightnessTitle.setTextSize(14);
        LinearLayout.LayoutParams brightnessTitleParams = new LinearLayout.LayoutParams(-1, -2);
        brightnessTitleParams.topMargin = dp(16);
        pageContent.addView(brightnessTitle, brightnessTitleParams);

        brightnessLabel = new TextView(this);
        brightnessLabel.setTextColor(palette.muted);
        brightnessLabel.setTextSize(12);
        pageContent.addView(brightnessLabel);

        SeekBar brightness = new SeekBar(this);
        brightness.setMax(255);
        brightness.setProgress(readBrightness());
        KeyboardSupport.makeFocusable(brightness);
        pageContent.addView(brightness, new LinearLayout.LayoutParams(-1, dp(44)));
        brightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int value = Math.max(1, progress);
                brightnessLabel.setText(value + " / 255");
                if (fromUser) writeBrightness(value);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        TextView footnote = new TextView(this);
        footnote.setText("Direct controls: Wi-Fi, Bluetooth, sound and brightness. Some firmware may restrict individual controls.");
        footnote.setTextColor(palette.muted);
        footnote.setTextSize(11);
        LinearLayout.LayoutParams footParams = new LinearLayout.LayoutParams(-1, -2);
        footParams.topMargin = dp(8);
        pageContent.addView(footnote, footParams);

        refreshControlLabels();
        wifiButton.requestFocus();
    }

    private String batterySummary() {
        try {
            Intent battery = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (battery == null) return "";
            int level = battery.getIntExtra("level", -1);
            int scale = battery.getIntExtra("scale", 100);
            if (level < 0 || scale <= 0) return "";
            return "BATTERY " + (level * 100 / scale) + "%";
        } catch (RuntimeException ignored) { return ""; }
    }

    private int readBrightness() {
        try {
            return Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
        } catch (Settings.SettingNotFoundException ignored) {
            return 128;
        } catch (RuntimeException ignored) {
            return 128;
        }
    }

    private void writeBrightness(int value) {
        try {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
            boolean saved = Settings.System.putInt(getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS, Math.max(1, Math.min(255, value)));
            if (!saved) brightnessLabel.setText("Brightness change was rejected by Android");
        } catch (RuntimeException error) {
            brightnessLabel.setText("Brightness control is blocked by device firmware");
        }
    }

    private void toggleWifi() {
        try {
            WifiManager manager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (manager == null) return;
            boolean accepted = manager.setWifiEnabled(!manager.isWifiEnabled());
            if (!accepted) android.widget.Toast.makeText(this, "Wi-Fi change was rejected", android.widget.Toast.LENGTH_SHORT).show();
            refreshControlLabels();
        } catch (RuntimeException error) {
            android.widget.Toast.makeText(this, "Wi-Fi control unavailable", android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleBluetooth() {
        try {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter == null) {
                android.widget.Toast.makeText(this, "Bluetooth unavailable", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            if (adapter.isEnabled()) adapter.disable();
            else adapter.enable();
            refreshControlLabels();
        } catch (RuntimeException error) {
            android.widget.Toast.makeText(this, "Bluetooth control unavailable", android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleSound() {
        try {
            AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            if (audio == null) return;
            audio.setRingerMode(audio.getRingerMode() == AudioManager.RINGER_MODE_SILENT
                    ? AudioManager.RINGER_MODE_NORMAL : AudioManager.RINGER_MODE_SILENT);
            refreshControlLabels();
        } catch (RuntimeException error) {
            android.widget.Toast.makeText(this, "Sound control unavailable", android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    private void adjustVolume(boolean up) {
        try {
            AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            if (audio != null) audio.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                    up ? AudioManager.ADJUST_RAISE : AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
        } catch (RuntimeException ignored) { }
    }

    private void refreshControlLabels() {
        if (wifiButton == null || bluetoothButton == null || soundButton == null) return;
        try {
            WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            wifiButton.setText("Wi-Fi\n" + (wifi != null && wifi.isWifiEnabled() ? "ON" : "OFF"));
        } catch (RuntimeException error) { wifiButton.setText("Wi-Fi\nUnavailable"); }
        try {
            BluetoothAdapter bluetooth = BluetoothAdapter.getDefaultAdapter();
            bluetoothButton.setText("Bluetooth\n" + (bluetooth != null && bluetooth.isEnabled() ? "ON" : "OFF"));
        } catch (RuntimeException error) { bluetoothButton.setText("Bluetooth\nUnavailable"); }
        try {
            AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            soundButton.setText("Sound\n" + (audio != null && audio.getRingerMode() == AudioManager.RINGER_MODE_SILENT
                    ? "SILENT" : "ON"));
        } catch (RuntimeException error) { soundButton.setText("Sound\nUnavailable"); }
        if (brightnessLabel != null) brightnessLabel.setText(readBrightness() + " / 255");
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
        if (keyCode == KeyEvent.KEYCODE_BACK && quickPanelVisible) {
            showHome();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override public void onBackPressed() {
        if (quickPanelVisible) showHome();
        // The HOME screen intentionally remains open on Back.
    }
}
