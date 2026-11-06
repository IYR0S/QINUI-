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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.widget.EditText;
import android.widget.Button;
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
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
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
    private static final String DRAWER_PREFS = "qins_drawer";
    private int drawerPage = 0;
    private static final int REQUEST_PICK_WALLPAPER = 7301;
    private static final String LAUNCHER_PREFS = "qins_launcher";
    private static final String PINS_KEY = "home_pins";
    private static final String FOLDER_PREFS = "qins_folders";
    private static final String BACKGROUND_PREFS = "qins_backgrounds";
    private static final String BACKGROUND_STYLE_KEY = "style";

    private int page = 0;
    private int homeAppRowHeightDp = 86;
    private int selectedHomeIndex = -1;
    private boolean quickPanelVisible = false;
    private boolean quickPanelExpanded = false;
    private int quickPanelPage = 0;
    private boolean awaitingRightKeyBind = false;
    private static final String SOFTKEY_PREFS = "qins_softkeys";
    private int homePageCount = 1;
    private static final String QUICK_PREFS = "qins_quick_tiles";
    private static final String[] QUICK_OPTIONS = new String[] {"sound", "bluetooth", "settings", "theme", "wifi", "brightness", "volume"};
    
    private boolean drawerVisible = false;
    private boolean editMode = false;
    private boolean pinSelectionMode = false;
    private View pendingSelectView;
    private boolean longPressPerformed = false;
     private View folderOverlay;
    private String drawerSearchQuery = "";
    private TextView drawerSearchStatus;
    private SignalStrengthIndicatorView signalIndicator;
    private TelephonyManager telephonyManager;
    private PhoneStateListener phoneStateListener;
    private int cellularSignalLevel = -1;
    private boolean cellularServiceKnown = false;
    private boolean cellularServiceAvailable = false;

    private LinearLayout root;
    private FrameLayout pageContainer;
    private LinearLayout pageContent;
    private View compactPanelView;
    private LinearLayout appRow;
    private LinearLayout editBar;
    private TextView appsNavButton;
    private TextView wallpaperButton;
    private TextView themeButton;
    private Bitmap wallpaperBitmap;
    private TextView clockView;
    private android.content.BroadcastReceiver notificationReceiver = new android.content.BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (quickPanelVisible && quickPanelExpanded && quickPanelPage == 1) showQuickPanel(false);
        }
    };
    private TextView dateView;
    private TextView batteryView;
    private TextView panelClock;
    private TextView panelDate;
    private TextView panelBattery;
    private TextView wifiState;
    private TextView bluetoothState;
    private TextView soundState;
    private View wifiTile;
    private View bluetoothTile;
    private View soundTile;
    private LinearLayout pageDots;

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
        try { registerReceiver(notificationReceiver, new IntentFilter(NotificationBridgeService.ACTION_CHANGED)); }
        catch (RuntimeException ignored) { }
        registerTelephonyMonitor();
        if (quickPanelVisible && quickPanelExpanded) showQuickPanel(false);
        else if (quickPanelVisible) showCompactQuickPanel(false);
        else if (drawerVisible) showDrawer(pinSelectionMode);
        else showHome(false);
        updateClockAndBattery();
        updateQuickStates();
        updateSignalIndicator();
    }

    @Override protected void onPause() {
        unregisterSystemReceiver();
        try { unregisterReceiver(notificationReceiver); } catch (RuntimeException ignored) { }
        unregisterTelephonyMonitor();
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

    private void registerTelephonyMonitor() {
        try {
            if (telephonyManager == null) {
                telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            }
            if (telephonyManager == null) {
                updateSignalIndicator();
                return;
            }
            int simState = telephonyManager.getSimState();
            if (simState == TelephonyManager.SIM_STATE_ABSENT ||
                    simState == TelephonyManager.SIM_STATE_UNKNOWN) {
                cellularServiceKnown = true;
                cellularServiceAvailable = false;
                cellularSignalLevel = 0;
            }
            if (phoneStateListener == null) {
                phoneStateListener = new PhoneStateListener() {
                    @Override public void onSignalStrengthsChanged(SignalStrength signal) {
                        super.onSignalStrengthsChanged(signal);
                        cellularSignalLevel = signalLevel(signal);
                        if (cellularSignalLevel >= 0 && !cellularServiceKnown) {
                            cellularServiceKnown = true;
                            cellularServiceAvailable = true;
                        }
                        updateSignalIndicator();
                    }

                    @Override public void onServiceStateChanged(ServiceState state) {
                        super.onServiceStateChanged(state);
                        cellularServiceKnown = true;
                        cellularServiceAvailable = state != null &&
                                state.getState() == ServiceState.STATE_IN_SERVICE;
                        if (!cellularServiceAvailable) cellularSignalLevel = 0;
                        updateSignalIndicator();
                    }
                };
            }
            telephonyManager.listen(phoneStateListener,
                    PhoneStateListener.LISTEN_SIGNAL_STRENGTHS |
                            PhoneStateListener.LISTEN_SERVICE_STATE);
        } catch (SecurityException denied) {
            cellularSignalLevel = -1;
            cellularServiceKnown = false;
            updateSignalIndicator();
        } catch (RuntimeException unavailable) {
            cellularSignalLevel = -1;
            cellularServiceKnown = false;
            updateSignalIndicator();
        }
    }

    private void unregisterTelephonyMonitor() {
        try {
            if (telephonyManager != null && phoneStateListener != null) {
                telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
            }
        } catch (RuntimeException ignored) { }
    }

    private int signalLevel(SignalStrength signal) {
        if (signal == null) return -1;
        try {
            if (signal.isGsm()) {
                int asu = signal.getGsmSignalStrength();
                if (asu < 0 || asu == 99) return -1;
                if (asu <= 2) return 0;
                if (asu <= 7) return 1;
                if (asu <= 12) return 2;
                if (asu <= 17) return 3;
                return 4;
            }
            int dbm = signal.getCdmaDbm();
            if (dbm >= -85) return 4;
            if (dbm >= -95) return 3;
            if (dbm >= -105) return 2;
            if (dbm >= -115) return 1;
            return 0;
        } catch (RuntimeException ignored) { return -1; }
    }

    private void updateSignalIndicator() {
        if (signalIndicator == null) return;
        ThemeManager.Appearance a = appearance();
        boolean noService = cellularServiceKnown && !cellularServiceAvailable;
        signalIndicator.setSignal(cellularSignalLevel, noService, a.accent, a.muted);
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

        signalIndicator = new SignalStrengthIndicatorView(this);
        signalIndicator.setSignal(cellularSignalLevel,
                cellularServiceKnown && !cellularServiceAvailable, a.accent, a.muted);
        signalIndicator.setContentDescription("Cellular signal strength");
        topBar.addView(signalIndicator, new LinearLayout.LayoutParams(dp(23), dp(22)));

         themeButton = new TextView(this);
        themeButton.setText("◉");
        themeButton.setTextSize(18);
        themeButton.setGravity(Gravity.CENTER);
        themeButton.setFocusable(true);
        themeButton.setFocusableInTouchMode(false);
        themeButton.setContentDescription(getString(R.string.launcher_theme));
        styleSurface(themeButton, a.surface, a.border, a.focus, a.cornerDp);
        themeButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showThemePicker(); }
        });
        LinearLayout.LayoutParams themeButtonParams = new LinearLayout.LayoutParams(dp(34), dp(36));
        themeButtonParams.rightMargin = dp(5);
        topBar.addView(themeButton, themeButtonParams);

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
                else { drawerSearchQuery = ""; drawerPage = 0; showDrawer(false); }
            }
        });
        wallpaperButton = new TextView(this);
        wallpaperButton.setText("▧");
        wallpaperButton.setTextSize(19);
        wallpaperButton.setGravity(Gravity.CENTER);
        wallpaperButton.setTextColor(a.text);
        wallpaperButton.setContentDescription(getString(R.string.launcher_wallpaper));
        wallpaperButton.setFocusable(true);
        wallpaperButton.setFocusableInTouchMode(false);
        styleSurface(wallpaperButton, a.surface, a.border, a.focus, a.cornerDp);
        wallpaperButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showWallpaperOptions(); }
        });
        LinearLayout.LayoutParams wallpaperParams = new LinearLayout.LayoutParams(dp(36), dp(36));
        wallpaperParams.rightMargin = dp(5);
        topBar.addView(wallpaperButton, wallpaperParams);
        topBar.addView(appsNavButton, new LinearLayout.LayoutParams(-2, dp(36)));
        root.addView(topBar, new LinearLayout.LayoutParams(-1, dp(44)));

        pageContainer = new FrameLayout(this);
        pageContent = new LinearLayout(this);
        pageContent.setOrientation(LinearLayout.VERTICAL);
        pageContainer.addView(pageContent, new FrameLayout.LayoutParams(-1, -1));
        root.addView(pageContainer, new LinearLayout.LayoutParams(-1, 0, 1));

        // Quick Settings is opened with the physical D-pad DOWN key.
        // No oversized bottom handle: keep the home page clean and full-height.
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
        if (wallpaperButton != null) {
            wallpaperButton.setTextColor(a.text);
            styleSurface(wallpaperButton, a.surface, a.border, a.focus, a.cornerDp);
        }
        if (themeButton != null) {
            themeButton.setTextColor(a.text);
            styleSurface(themeButton, a.surface, a.border, a.focus, a.cornerDp);
        }
        root.setBackground(new QinsWallpaperDrawable(a));
    }

    private File wallpaperFile() {
        return new File(getFilesDir(), "qins_home_wallpaper.jpg");
    }

    private Bitmap loadWallpaperBitmap() {
        if (wallpaperBitmap != null && !wallpaperBitmap.isRecycled()) return wallpaperBitmap;
        File file = wallpaperFile();
        if (!file.isFile()) return null;
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), bounds);
        int targetWidth = Math.max(1, getResources().getDisplayMetrics().widthPixels * 2);
        int targetHeight = Math.max(1, getResources().getDisplayMetrics().heightPixels * 2);
        int sample = 1;
        while (bounds.outWidth / (sample * 2) >= targetWidth &&
                bounds.outHeight / (sample * 2) >= targetHeight) sample *= 2;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sample;
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        wallpaperBitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        return wallpaperBitmap;
    }

    private void showWallpaperOptions() {
        final String[] choices = new String[] {
                getString(R.string.launcher_wallpaper_gallery),
                getString(R.string.launcher_wallpaper_reset)
        };
        new android.app.AlertDialog.Builder(this)
                .setTitle(R.string.launcher_wallpaper)
                .setItems(choices, new android.content.DialogInterface.OnClickListener() {
                    @Override public void onClick(android.content.DialogInterface dialog, int which) {
                        if (which == 0) pickWallpaperFromGallery();
                        else resetWallpaper();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void pickWallpaperFromGallery() {
        try {
            Intent documentPick = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            documentPick.setType("image/*");
            documentPick.addCategory(Intent.CATEGORY_OPENABLE);
            documentPick.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            try {
                startActivityForResult(documentPick, REQUEST_PICK_WALLPAPER);
                return;
            } catch (RuntimeException noDocumentPicker) {
                // Some Android 4.4 firmware builds omit DocumentsUI. Fall back to
                // the older content chooser rather than making wallpaper selection unavailable.
            }

            Intent contentPick = new Intent(Intent.ACTION_GET_CONTENT);
            contentPick.setType("image/*");
            contentPick.addCategory(Intent.CATEGORY_OPENABLE);
            contentPick.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(Intent.createChooser(contentPick,
                    getString(R.string.launcher_wallpaper_gallery)), REQUEST_PICK_WALLPAPER);
        } catch (RuntimeException error) {
            android.widget.Toast.makeText(this, R.string.launcher_wallpaper_unavailable,
                    android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap decodeSelectedWallpaper(Uri uri) throws java.io.IOException {
        InputStream stream = null;
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try {
            stream = getContentResolver().openInputStream(uri);
            if (stream == null) return null;
            BitmapFactory.decodeStream(stream, null, bounds);
        } finally {
            if (stream != null) try { stream.close(); } catch (java.io.IOException ignored) { }
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null;

        int targetWidth = Math.max(1, getResources().getDisplayMetrics().widthPixels * 2);
        int targetHeight = Math.max(1, getResources().getDisplayMetrics().heightPixels * 2);
        int sample = 1;
        while (bounds.outWidth / (sample * 2) >= targetWidth &&
                bounds.outHeight / (sample * 2) >= targetHeight) sample *= 2;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sample;
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        try {
            stream = getContentResolver().openInputStream(uri);
            return stream == null ? null : BitmapFactory.decodeStream(stream, null, options);
        } finally {
            if (stream != null) try { stream.close(); } catch (java.io.IOException ignored) { }
        }
    }

    private void saveWallpaper(Bitmap bitmap) throws java.io.IOException {
        File target = wallpaperFile();
        FileOutputStream output = new FileOutputStream(target);
        try {
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 88, output)) {
                throw new java.io.IOException("Could not encode wallpaper");
            }
            output.flush();
        } finally {
            output.close();
        }
        wallpaperBitmap = null;
        getSharedPreferences(BACKGROUND_PREFS, MODE_PRIVATE).edit()
                .putString(BACKGROUND_STYLE_KEY, "photo").commit();
    }

    private void resetWallpaper() {
        File file = wallpaperFile();
        if (file.exists()) file.delete();
        wallpaperBitmap = null;
        getSharedPreferences(BACKGROUND_PREFS, MODE_PRIVATE).edit()
                .putString(BACKGROUND_STYLE_KEY, "theme").commit();
        refreshLauncherSurface();
    }

    private void refreshLauncherSurface() {
        if (quickPanelVisible && quickPanelExpanded) showQuickPanel(false);
        else if (quickPanelVisible) showCompactQuickPanel(false);
        else if (drawerVisible) showDrawer(pinSelectionMode);
        else showHome(false);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_PICK_WALLPAPER || resultCode != RESULT_OK || data == null) return;
        Uri selectedUri = data.getData();
        if (selectedUri == null && data.getClipData() != null &&
                data.getClipData().getItemCount() > 0) {
            selectedUri = data.getClipData().getItemAt(0).getUri();
        }
        if (selectedUri == null) {
            android.widget.Toast.makeText(this, R.string.launcher_wallpaper_failed,
                    android.widget.Toast.LENGTH_LONG).show();
            return;
        }
        Bitmap selected = null;
        try {
            selected = decodeSelectedWallpaper(selectedUri);
            if (selected == null) throw new java.io.IOException("Unsupported image");
            saveWallpaper(selected);
            selected.recycle();
            selected = null;
            refreshLauncherSurface();
            android.widget.Toast.makeText(this, R.string.launcher_wallpaper_saved,
                    android.widget.Toast.LENGTH_SHORT).show();
        } catch (Exception error) {
            android.widget.Toast.makeText(this, R.string.launcher_wallpaper_failed,
                    android.widget.Toast.LENGTH_LONG).show();
        } finally {
            if (selected != null && !selected.isRecycled()) selected.recycle();
        }
    }

    private void styleSurface(final View view, final int fill, final int border,
                              final int focus, final float cornerDp) {
        view.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        view.setBackgroundDrawable(new FocusSurfaceDrawable(fill, border, focus, cornerDp, false));
        view.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override public void onFocusChange(View v, boolean hasFocus) {
                v.setBackgroundDrawable(new FocusSurfaceDrawable(fill, border, focus, cornerDp, hasFocus));
            }
        });
    }

    /** Keeps the original fill and draws a luminous outline only; focus never adds a grey plate. */
    private final class FocusSurfaceDrawable extends Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int fill;
        private final int border;
        private final int glow;
        private final float radius;
        private final boolean focused;

        FocusSurfaceDrawable(int fill, int border, int glow, float radiusDp, boolean focused) {
            this.fill = fill; this.border = border; this.glow = glow;
            this.radius = dp(radiusDp); this.focused = focused;
        }

        @Override public void draw(Canvas canvas) {
            android.graphics.Rect b = getBounds();
            android.graphics.RectF r = new android.graphics.RectF(
                    b.left + dp(focused ? 3 : 1), b.top + dp(focused ? 3 : 1),
                    b.right - dp(focused ? 3 : 1), b.bottom - dp(focused ? 3 : 1));
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(fill);
            if (focused) paint.setShadowLayer(dp(7), 0, 0, glow);
            canvas.drawRoundRect(r, radius, radius, paint);
            paint.clearShadowLayer();
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(focused ? 2 : 1));
            paint.setColor(focused ? glow : border);
            canvas.drawRoundRect(r, radius, radius, paint);
            if (focused) {
                paint.setStrokeWidth(dp(1));
                paint.setColor(0x66FFFFFF);
                android.graphics.RectF inner = new android.graphics.RectF(r.left + dp(2), r.top + dp(2),
                        r.right - dp(2), r.bottom - dp(2));
                canvas.drawRoundRect(inner, Math.max(0, radius - dp(2)), Math.max(0, radius - dp(2)), paint);
            }
        }
        @Override public void setAlpha(int alpha) { }
        @Override public void setColorFilter(ColorFilter filter) { }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }

    private GradientDrawable makeShape(int fill, int stroke, float radiusDp, boolean focused) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(radiusDp));
        d.setStroke(dp(focused ? 2 : 1), stroke);
        return d;
    }

    /** Nothing-inspired 5x7 dot-matrix digits; text remains available to accessibility services. */
    private final class NothingDotMatrixClockView extends TextView {
        private final Paint dots = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final boolean stacked;
        private final String[][] digits = new String[][] {
                {"01110","10001","10011","10101","11001","10001","01110"},
                {"00100","01100","00100","00100","00100","00100","01110"},
                {"01110","10001","00001","00010","00100","01000","11111"},
                {"11110","00001","00001","01110","00001","00001","11110"},
                {"00010","00110","01010","10010","11111","00010","00010"},
                {"11111","10000","10000","11110","00001","00001","11110"},
                {"00110","01000","10000","11110","10001","10001","01110"},
                {"11111","00001","00010","00100","01000","01000","01000"},
                {"01110","10001","10001","01110","10001","10001","01110"},
                {"01110","10001","10001","01111","00001","00010","11100"}
        };

        NothingDotMatrixClockView(Context context, boolean stacked) {
            super(context);
            this.stacked = stacked;
            setIncludeFontPadding(false);
            setSingleLine(false);
            setGravity(Gravity.CENTER);
            setWillNotDraw(false);
        }

        @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int width = resolveSize(dp(220), widthMeasureSpec);
            int height = resolveSize(dp(stacked ? 94 : 76), heightMeasureSpec);
            setMeasuredDimension(width, height);
        }

        @Override protected void onDraw(Canvas canvas) {
            String raw = getText() == null ? "" : getText().toString();
            StringBuilder normalized = new StringBuilder();
            for (int i = 0; i < raw.length(); i++) {
                char ch = raw.charAt(i);
                int digit = Character.digit(ch, 10);
                if (digit >= 0) normalized.append((char) ('0' + digit));
                else if (ch == ':' || ch == '\n') normalized.append(ch);
            }
            String[] lines = normalized.toString().split("\\n");
            if (lines.length == 0 || lines[0].length() == 0) return;

            float maxUnits = 1f;
            for (String line : lines) {
                float lineUnits = 0f;
                for (int i = 0; i < line.length(); i++) {
                    lineUnits += line.charAt(i) == ':' ? 1f : 5f;
                    if (i + 1 < line.length()) lineUnits += 1f;
                }
                maxUnits = Math.max(maxUnits, lineUnits);
            }
            float verticalUnits = lines.length * 7f + (lines.length - 1) * 1.5f;
            float available = Math.max(1f, getWidth() - getPaddingLeft() - getPaddingRight());
            float verticalRoom = Math.max(dp(7), getHeight() - getPaddingTop() - getPaddingBottom());
            float pitch = Math.min(dp(6.2f), Math.min(available / maxUnits, verticalRoom / verticalUnits));
            pitch = Math.max(dp(1.4f), pitch);
            float lineHeight = 7f * pitch;
            float startY = getPaddingTop() + Math.max(0f,
                    (verticalRoom - (lineHeight * lines.length + dp(1.5f) * (lines.length - 1))) / 2f);
            float radius = pitch * 0.28f;
            dots.setStyle(Paint.Style.FILL);

            for (int lineIndex = 0; lineIndex < lines.length; lineIndex++) {
                String value = lines[lineIndex];
                float units = 0f;
                for (int i = 0; i < value.length(); i++) {
                    units += value.charAt(i) == ':' ? 1f : 5f;
                    if (i + 1 < value.length()) units += 1f;
                }
                float x = getPaddingLeft() + Math.max(0f, (available - units * pitch) / 2f);
                float y = startY + lineIndex * (lineHeight + dp(1.5f));
                dots.setColor(stacked ? appearance().accent : getCurrentTextColor());
                for (int i = 0; i < value.length(); i++) {
                    char ch = value.charAt(i);
                    if (ch == ':') {
                        dots.setColor(appearance().accent);
                        canvas.drawCircle(x + pitch * 0.5f, y + pitch * 2.5f, radius, dots);
                        canvas.drawCircle(x + pitch * 0.5f, y + pitch * 4.5f, radius, dots);
                        dots.setColor(stacked ? appearance().accent : getCurrentTextColor());
                        x += pitch;
                    } else {
                        String[] rows = digits[ch - '0'];
                        for (int row = 0; row < 7; row++) {
                            for (int col = 0; col < 5; col++) {
                                if (rows[row].charAt(col) == '1') {
                                    canvas.drawCircle(x + (col + 0.5f) * pitch,
                                            y + (row + 0.5f) * pitch, radius, dots);
                                }
                            }
                        }
                        x += 5f * pitch;
                    }
                    if (i + 1 < value.length()) x += pitch;
                }
            }
        }
    }

    /** API-19-safe analog clock face; no external fonts or clock assets required. */
    private final class AnalogClockTextView extends TextView {
        private final Paint dial = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint hand = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final ThemeManager.Appearance colors;

        AnalogClockTextView(Context context, ThemeManager.Appearance appearance) {
            super(context);
            colors = appearance;
            setWillNotDraw(false);
            setIncludeFontPadding(false);
        }

        @Override protected void onDraw(Canvas canvas) {
            float size = Math.min(getWidth(), getHeight());
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            float radius = Math.max(dp(10), size / 2f - dp(4));
            dial.setStyle(Paint.Style.FILL);
            dial.setColor(colors.surface);
            canvas.drawCircle(cx, cy, radius, dial);
            dial.setStyle(Paint.Style.STROKE);
            dial.setStrokeWidth(dp(1.4f));
            dial.setColor(colors.accent);
            canvas.drawCircle(cx, cy, radius, dial);

            dial.setStrokeWidth(dp(1.2f));
            for (int i = 0; i < 12; i++) {
                double angle = Math.PI * i / 6.0 - Math.PI / 2.0;
                float inner = radius * (i % 3 == 0 ? 0.78f : 0.85f);
                float outer = radius * 0.92f;
                canvas.drawLine(cx + (float)Math.cos(angle) * inner,
                        cy + (float)Math.sin(angle) * inner,
                        cx + (float)Math.cos(angle) * outer,
                        cy + (float)Math.sin(angle) * outer, dial);
            }

            java.util.Calendar now = java.util.Calendar.getInstance();
            float minute = now.get(java.util.Calendar.MINUTE) + now.get(java.util.Calendar.SECOND) / 60f;
            float hour = (now.get(java.util.Calendar.HOUR) % 12) + minute / 60f;
            drawHand(canvas, cx, cy, radius * 0.48f, hour * 30f, colors.text, dp(2.7f));
            drawHand(canvas, cx, cy, radius * 0.70f, minute * 6f, colors.accent, dp(1.8f));
            dial.setStyle(Paint.Style.FILL);
            dial.setColor(colors.accent);
            canvas.drawCircle(cx, cy, dp(2.6f), dial);
        }

        private void drawHand(Canvas canvas, float cx, float cy, float length,
                              float degrees, int color, float width) {
            double angle = Math.toRadians(degrees - 90f);
            hand.setColor(color);
            hand.setStrokeWidth(width);
            hand.setStrokeCap(Paint.Cap.ROUND);
            canvas.drawLine(cx, cy, cx + (float)Math.cos(angle) * length,
                    cy + (float)Math.sin(angle) * length, hand);
        }
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
            String backgroundStyle = getSharedPreferences(BACKGROUND_PREFS, MODE_PRIVATE)
                    .getString(BACKGROUND_STYLE_KEY, "theme");
            Bitmap photo = "photo".equals(backgroundStyle) ? loadWallpaperBitmap() : null;
            if (photo != null && !photo.isRecycled()) {
                float scale = Math.max(w / (float) photo.getWidth(), h / (float) photo.getHeight());
                float sourceWidth = w / scale;
                float sourceHeight = h / scale;
                int left = Math.max(0, (int) ((photo.getWidth() - sourceWidth) / 2f));
                int top = Math.max(0, (int) ((photo.getHeight() - sourceHeight) / 2f));
                android.graphics.Rect source = new android.graphics.Rect(left, top,
                        Math.min(photo.getWidth(), left + Math.round(sourceWidth)),
                        Math.min(photo.getHeight(), top + Math.round(sourceHeight)));
                paint.setAlpha(255);
                paint.setColorFilter(null);
                paint.setShader(null);
                paint.setStyle(Paint.Style.FILL);
                paint.setFilterBitmap(true);
                canvas.drawBitmap(photo, source, bounds, paint);
                paint.setShader(new LinearGradient(0, bounds.top, 0, bounds.bottom,
                        0x22000000, 0x55000000, Shader.TileMode.CLAMP));
                canvas.drawRect(bounds, paint);
                paint.setShader(null);
                return;
            }
            paint.setAlpha(255);
            paint.setColorFilter(null);
            paint.setStyle(Paint.Style.FILL);
            if ("minimal".equals(backgroundStyle) || "gradient".equals(backgroundStyle) ||
                    "dots".equals(backgroundStyle)) {
                int startColor = "minimal".equals(backgroundStyle) ? style.background : style.backgroundAlt;
                paint.setShader(new LinearGradient(0, 0, w, h, startColor,
                        style.background, Shader.TileMode.CLAMP));
                canvas.drawRect(bounds, paint);
                paint.setShader(null);
                if ("dots".equals(backgroundStyle)) {
                    paint.setColor(0x1FFFFFFF);
                    float step = dp(13);
                    for (float y = dp(7); y < h; y += step) {
                        for (float x = dp(7); x < w; x += step) {
                            canvas.drawCircle(x, y, dp(0.75f), paint);
                        }
                    }
                } else if ("gradient".equals(backgroundStyle)) {
                    Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
                    glow.setShader(new android.graphics.RadialGradient(w * 0.78f, h * 0.18f,
                            Math.max(w, h) * 0.72f,
                            new int[] {0x35FFFFFF, 0x00FFFFFF},
                            new float[] {0f, 1f}, Shader.TileMode.CLAMP));
                    canvas.drawRect(bounds, glow);
                    glow.setShader(null);
                }
                return;
            }
            paint.setShader(new LinearGradient(0, 0, w, h,
                    style.backgroundAlt, style.background, Shader.TileMode.CLAMP));
            canvas.drawRect(bounds, paint);
            paint.setShader(null);

            int wallpaperVariant = "theme_2".equals(backgroundStyle) ? 2 :
                    ("theme_3".equals(backgroundStyle) ? 3 : 1);
            drawThemeWallpaper(canvas, w, h, wallpaperVariant);
        }

        private void drawThemeWallpaper(Canvas canvas, float w, float h, int variant) {
            canvas.save();
            if (variant == 2) canvas.rotate(-9f, w * 0.5f, h * 0.5f);
            if (variant == 3) canvas.scale(1.16f, 1.16f, w * 0.5f, h * 0.5f);
            if (style.monochrome) drawNothingWallpaper(canvas, w, h);
            else if (ThemeManager.APPEARANCE_PIXEL.equals(style.id)) drawPixelWallpaper(canvas, w, h);
            else drawGalaxyWallpaper(canvas, w, h);
            canvas.restore();

            if (style.monochrome) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(dp(variant == 2 ? 5 : 2));
                paint.setColor(variant == 2 ? 0x99E3343A : 0x44FFFFFF);
                if (variant == 2) {
                    canvas.drawLine(-w * 0.1f, h * 0.78f, w * 0.72f, h * 0.12f, paint);
                    canvas.drawLine(w * 0.20f, h * 1.08f, w * 1.05f, h * 0.40f, paint);
                } else if (variant == 3) {
                    canvas.drawCircle(w * 0.50f, h * 0.48f, Math.min(w, h) * 0.31f, paint);
                    canvas.drawCircle(w * 0.50f, h * 0.48f, Math.min(w, h) * 0.23f, paint);
                }
                paint.setStyle(Paint.Style.FILL);
            } else if (ThemeManager.APPEARANCE_PIXEL.equals(style.id) && variant == 2) {
                paint.setColor(0x337B61B5);
                canvas.drawCircle(w * 0.82f, h * 0.78f, Math.min(w, h) * 0.28f, paint);
            } else if (!ThemeManager.APPEARANCE_PIXEL.equals(style.id) && variant == 3) {
                paint.setColor(0x33FFFFFF);
                canvas.drawRect(w * 0.08f, h * 0.12f, w * 0.92f, h * 0.15f, paint);
                canvas.drawRect(w * 0.08f, h * 0.84f, w * 0.72f, h * 0.87f, paint);
            }
        }

        private void drawNothingWallpaper(Canvas canvas, float w, float h) {
            paint.setShader(new LinearGradient(0, 0, w, h,
                    style.backgroundAlt, style.background, Shader.TileMode.CLAMP));
            paint.setStyle(Paint.Style.FILL);
            canvas.drawRect(0, 0, w, h, paint);
            paint.setShader(null);
            paint.setColor(0x18FFFFFF);
            float step = dp(10);
            for (float y = dp(8); y < h; y += step) {
                for (float x = dp(8); x < w; x += step) {
                    canvas.drawCircle(x, y, dp(0.65f), paint);
                }
            }
            paint.setColor(0x1FFFFFFF);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(1));
            canvas.drawCircle(w * 0.84f, h * 0.25f, Math.min(w, h) * 0.22f, paint);
            paint.setColor(0x55E3343A);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(w * 0.84f, h * 0.25f, dp(2), paint);
            paint.setStyle(Paint.Style.FILL);
        }

        private void drawPixelWallpaper(Canvas canvas, float w, float h) {
            paint.setStyle(Paint.Style.FILL);
            paint.setShader(new LinearGradient(0, 0, w, h,
                    new int[] {0xFF142820, 0xFF263F32, 0xFF101B1C},
                    new float[] {0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, paint);
            paint.setShader(null);

            // Current Pixel-inspired green/teal organic forms: soft, sculpted and luminous.
            Path upperLeaf = new Path();
            upperLeaf.moveTo(-w * 0.12f, h * 0.17f);
            upperLeaf.cubicTo(w * 0.02f, -h * 0.13f, w * 0.42f, -h * 0.12f, w * 0.61f, h * 0.08f);
            upperLeaf.cubicTo(w * 0.83f, h * 0.31f, w * 0.57f, h * 0.47f, w * 0.31f, h * 0.43f);
            upperLeaf.cubicTo(w * 0.10f, h * 0.40f, -w * 0.05f, h * 0.35f, -w * 0.12f, h * 0.17f);
            upperLeaf.close();
            drawPixelLayer(canvas, upperLeaf, 0xFFE1F8B7, 0xFF6DCB83, 0xFF176C59);

            Path tealForm = new Path();
            tealForm.moveTo(w * 0.40f, -h * 0.10f);
            tealForm.cubicTo(w * 0.66f, -h * 0.02f, w * 1.04f, h * 0.12f, w * 1.12f, h * 0.40f);
            tealForm.cubicTo(w * 1.18f, h * 0.64f, w * 0.89f, h * 0.72f, w * 0.68f, h * 0.59f);
            tealForm.cubicTo(w * 0.46f, h * 0.46f, w * 0.25f, h * 0.18f, w * 0.40f, -h * 0.10f);
            tealForm.close();
            drawPixelLayer(canvas, tealForm, 0xFFB7F2E7, 0xFF37B7A0, 0xFF155E68);

            Path lowerForm = new Path();
            lowerForm.moveTo(-w * 0.10f, h * 0.75f);
            lowerForm.cubicTo(w * 0.11f, h * 0.54f, w * 0.41f, h * 0.62f, w * 0.57f, h * 0.82f);
            lowerForm.cubicTo(w * 0.75f, h * 1.06f, w * 0.99f, h * 0.89f, w * 1.12f, h * 0.78f);
            lowerForm.lineTo(w * 1.12f, h * 1.12f);
            lowerForm.lineTo(-w * 0.10f, h * 1.12f);
            lowerForm.close();
            drawPixelLayer(canvas, lowerForm, 0xFFC7F38A, 0xFF5DAD56, 0xFF244E3B);

            Paint sheen = new Paint(Paint.ANTI_ALIAS_FLAG);
            sheen.setStyle(Paint.Style.STROKE);
            sheen.setStrokeCap(Paint.Cap.ROUND);
            sheen.setStrokeWidth(dp(1.2f));
            sheen.setColor(0x66E9FFE6);
            Path highlight = new Path();
            highlight.moveTo(w * 0.04f, h * 0.19f);
            highlight.cubicTo(w * 0.19f, h * 0.03f, w * 0.41f, h * 0.06f, w * 0.53f, h * 0.20f);
            canvas.drawPath(highlight, sheen);
            sheen.setColor(0x557AF0D0);
            Path highlight2 = new Path();
            highlight2.moveTo(w * 0.59f, h * 0.18f);
            highlight2.cubicTo(w * 0.84f, h * 0.26f, w * 0.91f, h * 0.48f, w * 0.78f, h * 0.59f);
            canvas.drawPath(highlight2, sheen);
        }

        private void drawPixelLayer(Canvas canvas, Path path, int light, int middle, int dark) {
            Paint layer = new Paint(Paint.ANTI_ALIAS_FLAG);
            layer.setStyle(Paint.Style.FILL);
            layer.setShader(new LinearGradient(0, 0, getBounds().width() * 0.82f,
                    getBounds().height(), new int[] {light, middle, dark},
                    new float[] {0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawPath(path, layer);
            layer.setShader(null);
        }

        private void drawGalaxyWallpaper(Canvas canvas, float w, float h) {
            paint.setStyle(Paint.Style.FILL);
            paint.setShader(new LinearGradient(0, 0, w, h,
                    new int[] {0xFF333943, 0xFFBFC5CD, 0xFF777F89},
                    new float[] {0f, 0.48f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, paint);
            paint.setShader(null);

            // Galaxy S-series-inspired metallic S ribbon on a soft silver/graphite canvas.
            Path ribbon = new Path();
            ribbon.moveTo(w * 0.70f, -h * 0.10f);
            ribbon.cubicTo(w * 0.35f, h * 0.02f, w * 0.24f, h * 0.23f, w * 0.50f, h * 0.42f);
            ribbon.cubicTo(w * 0.79f, h * 0.62f, w * 0.75f, h * 0.80f, w * 0.28f, h * 1.08f);
            Paint shadow = new Paint(Paint.ANTI_ALIAS_FLAG);
            shadow.setStyle(Paint.Style.STROKE);
            shadow.setStrokeCap(Paint.Cap.ROUND);
            shadow.setStrokeJoin(Paint.Join.ROUND);
            shadow.setStrokeWidth(w * 0.30f);
            shadow.setColor(0x55323A46);
            canvas.save();
            canvas.translate(dp(4), dp(7));
            canvas.drawPath(ribbon, shadow);
            canvas.restore();

            Paint metal = new Paint(Paint.ANTI_ALIAS_FLAG);
            metal.setStyle(Paint.Style.STROKE);
            metal.setStrokeCap(Paint.Cap.ROUND);
            metal.setStrokeJoin(Paint.Join.ROUND);
            metal.setStrokeWidth(w * 0.28f);
            metal.setShader(new LinearGradient(0, 0, w, h,
                    new int[] {0xFFF9FBFD, 0xFFADB6C1, 0xFFE7EBF0, 0xFF586271},
                    new float[] {0f, 0.34f, 0.68f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawPath(ribbon, metal);
            metal.setShader(null);
            metal.setStrokeWidth(dp(1.6f));
            metal.setColor(0x99FFFFFF);
            canvas.drawPath(ribbon, metal);

            Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
            glow.setShader(new android.graphics.RadialGradient(w * 0.48f, h * 0.43f,
                    Math.max(w, h) * 0.75f,
                    new int[] {0x33FFFFFF, 0x00FFFFFF}, null, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, glow);
        }

        private void drawGalaxyOrb(Canvas canvas, float cx, float cy, float radius,
                                   int highlight, int midtone, int shadow) {
            Paint orb = new Paint(Paint.ANTI_ALIAS_FLAG);
            orb.setShader(new android.graphics.RadialGradient(
                    cx - radius * 0.30f, cy - radius * 0.34f, radius * 1.48f,
                    new int[] {highlight, midtone, shadow},
                    new float[] {0f, 0.58f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawCircle(cx, cy, radius, orb);
            orb.setShader(null);
            orb.setStyle(Paint.Style.STROKE);
            orb.setStrokeWidth(dp(0.8f));
            orb.setColor(0x30FFFFFF);
            canvas.drawArc(new android.graphics.RectF(cx - radius * 0.83f,
                    cy - radius * 0.83f, cx + radius * 0.83f, cy + radius * 0.83f),
                    205, 112, false, orb);
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

    private boolean isFolder(ResolveInfo info) {
        if (info == null) return false;
        String raw = getSharedPreferences(FOLDER_PREFS, MODE_PRIVATE)
                .getString("apps:" + componentKey(info), "");
        return raw.length() > 0 && raw.split("\\n").length > 1;
    }

    private String getFolderName(ResolveInfo info) {
        if (info == null) return getString(R.string.launcher_folder);
        return getSharedPreferences(FOLDER_PREFS, MODE_PRIVATE)
                .getString("name:" + componentKey(info), getString(R.string.launcher_folder));
    }

    private List<ResolveInfo> getFolderApps(ResolveInfo folder) {
        List<ResolveInfo> result = new ArrayList<ResolveInfo>();
        if (folder == null) return result;
        String raw = getSharedPreferences(FOLDER_PREFS, MODE_PRIVATE)
                .getString("apps:" + componentKey(folder), "");
        if (raw.length() == 0) return result;
        for (String key : raw.split("\\n")) {
            ResolveInfo info = findApp(key);
            if (info != null && !containsComponent(result, key)) result.add(info);
        }
        return result;
    }

    private boolean containsComponent(List<ResolveInfo> list, String key) {
        for (ResolveInfo info : list) if (componentKey(info).equals(key)) return true;
        return false;
    }

    private boolean isPinned(ResolveInfo target) {
        if (target == null) return false;
        String key = componentKey(target);
        for (ResolveInfo info : pinnedApps) {
            if (componentKey(info).equals(key)) return true;
            if (isFolder(info) && containsComponent(getFolderApps(info), key)) return true;
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
        if (clockView != null) clockView.setText(formatClockForTheme(time));
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

    private void removeCompactPanelOverlay() {
        if (pageContainer != null && compactPanelView != null) {
            pageContainer.removeView(compactPanelView);
        }
        compactPanelView = null;
    }

    private void showHome(boolean focusHandle) {
        closeHomeFolder();
        removeCompactPanelOverlay();
        quickPanelVisible = false;
        quickPanelExpanded = false;
        drawerVisible = false;
        pinSelectionMode = false;
        pageContent.removeAllViews();
        ThemeManager.Appearance a = appearance();
        updateNavigationButton();

        final String selectedClockStyle = clockStyle(a);
        final boolean nothingMatrixClock = ThemeManager.APPEARANCE_NOTHING.equals(a.id) &&
                ("dot".equals(selectedClockStyle) || "stacked".equals(selectedClockStyle));
        final boolean analogClock = "analog".equals(selectedClockStyle);
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(nothingMatrixClock ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        top.setGravity(nothingMatrixClock ? Gravity.CENTER_HORIZONTAL : Gravity.CENTER_VERTICAL);
        int smallScreen = screenWidthDp() < 210 || screenHeightDp() < 280 ? 1 : 0;
        int topSide = smallScreen == 1 ? 10 : 18;
        int topVertical = smallScreen == 1 ? 6 : 12;
        top.setPadding(dp(topSide), dp(topVertical), dp(topSide), dp(topVertical));
        top.setBackground(makeShape(a.surface, a.border, a.cornerDp + 4, false));

        LinearLayout timeBlock = new LinearLayout(this);
        timeBlock.setOrientation(LinearLayout.VERTICAL);
        timeBlock.setGravity(nothingMatrixClock ? Gravity.CENTER : Gravity.CENTER_VERTICAL);
        clockView = nothingMatrixClock ? new NothingDotMatrixClockView(this, "stacked".equals(selectedClockStyle)) :
                (analogClock ? new AnalogClockTextView(this, a) : new TextView(this));
        if (nothingMatrixClock) {
            clockView.setGravity(Gravity.CENTER);
            clockView.setMinHeight(dp("stacked".equals(selectedClockStyle) ? (smallScreen == 1 ? 78 : 92) : (smallScreen == 1 ? 58 : 70)));
            clockView.setMinWidth(dp(154));
        } else if (analogClock) {
            clockView.setGravity(Gravity.CENTER);
            clockView.setMinWidth(dp(68));
            clockView.setMinHeight(dp(68));
            clockView.setPadding(0, 0, 0, 0);
            clockView.setTextSize(1);
        }
        clockView.setTextColor(nothingMatrixClock ? ("stacked".equals(selectedClockStyle) ? a.accent : 0xFFF7F7F5) :
                (analogClock ? 0x00000000 : a.text));
        clockView.setTextSize(clockSizeForTheme(a, smallScreen == 1));
        if (analogClock) clockView.setTextSize(1);
        clockView.setIncludeFontPadding(false);
        clockView.setTypeface(clockTypefaceForTheme(a), clockTypefaceStyle());
        clockView.setFocusable(true);
        clockView.setFocusableInTouchMode(false);
        clockView.setTextDirection(View.TEXT_DIRECTION_LTR);
        styleSurface(clockView, 0x00000000, 0x00000000, a.focus, a.cornerDp);
        clockView.setPadding(dp(7), dp(4), dp(7), dp(4));
        clockView.setTag("home:clock");
        clockView.setContentDescription("Clock style. Press to customize.");
        clockView.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showClockStylePicker(); }
        });
        clockView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override public boolean onLongClick(View v) { showClockStylePicker(); return true; }
        });
        if (analogClock) timeBlock.addView(clockView, new LinearLayout.LayoutParams(dp(68), dp(68)));
        else timeBlock.addView(clockView, new LinearLayout.LayoutParams(-1, -2));

        dateView = new TextView(this);
        dateView.setTextColor(a.muted);
        dateView.setTextSize(smallScreen == 1 ? 9 : 11);
        dateView.setSingleLine(true);
        timeBlock.addView(dateView, new LinearLayout.LayoutParams(-1, -2));
        if (nothingMatrixClock) {
            top.addView(timeBlock, new LinearLayout.LayoutParams(-1, -2));
        } else {
            top.addView(timeBlock, new LinearLayout.LayoutParams(0, -2, 1));
        }

        batteryView = new TextView(this);
        batteryView.setTextColor(a.muted);
        batteryView.setTextSize(12);
        batteryView.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        top.addView(batteryView, nothingMatrixClock
                ? new LinearLayout.LayoutParams(-1, dp(17))
                : new LinearLayout.LayoutParams(-2, -1));
        LinearLayout.LayoutParams topParams = new LinearLayout.LayoutParams(-1, -2);
        topParams.topMargin = dp(smallScreen == 1 ? 3 : 9);
        topParams.bottomMargin = dp(smallScreen == 1 ? 5 : 10);
        pageContent.addView(top, topParams);

        View spacer = new View(this);
        spacer.setFocusable(true);
        spacer.setFocusableInTouchMode(false);
        spacer.setTag("home:drawer-trigger");
        spacer.setContentDescription(getString(R.string.launcher_open_all_apps));
        spacer.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { drawerPage = 0; showDrawer(false); }
        });
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
        pageDots = new LinearLayout(this);
        pageDots.setGravity(Gravity.CENTER);
        pageDots.setOrientation(LinearLayout.HORIZONTAL);
        pages.addView(pageDots, new LinearLayout.LayoutParams(-2, dp(32)));
        LinearLayout.LayoutParams pageParams = new LinearLayout.LayoutParams(-1,
                dp(screenHeightDp() < 280 ? 26 : 36));
        pageParams.topMargin = dp(screenHeightDp() < 280 ? 2 : 5);
        pageContent.addView(pages, pageParams);
        
        updatePageDots();

        if (editMode) buildEditBar(a);
        updateClockAndBattery();
        updateNavigationButton();

        if (editMode && selectedHomeIndex >= 0) focusHomeIndex(selectedHomeIndex);
        else spacer.requestFocus();
    }

    /** Compact quick controls stay on top of the home page and leave most of it visible. */
    private void showCompactQuickPanel(boolean focusFirst) {
        wifiState = null; bluetoothState = null; soundState = null;
        wifiTile = null; bluetoothTile = null; soundTile = null;
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
        title.setText(getString(R.string.launcher_quick_controls));
        title.setTextColor(a.text);
        title.setTextSize(10);
        header.addView(title, new LinearLayout.LayoutParams(0, dp(22), 1));
        TextView edit = new TextView(this);
        edit.setText(getString(R.string.launcher_edit_shortcuts));
        edit.setTextColor(a.accent);
        edit.setTextSize(10);
        edit.setGravity(Gravity.CENTER);
        edit.setFocusable(true);
        edit.setFocusableInTouchMode(false);
        header.addView(edit, new LinearLayout.LayoutParams(dp(48), dp(24)));
        edit.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showQuickSlotPicker(); }
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
        compactPanelView = compact;
        FrameLayout.LayoutParams overlayParams = new FrameLayout.LayoutParams(
                -1, dp(80), Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        pageContainer.addView(compact, overlayParams);
        compact.startAnimation(new android.view.animation.TranslateAnimation(
                0, 0, -dp(80), 0) {{
            setDuration(180);
            setFillAfter(false);
        }});
        updateNavigationButton();
        updateQuickStates();
        if (focusFirst && row.getChildCount() > 0) row.getChildAt(0).requestFocus();
    }

    private String[] getQuickTileKeys() {
        String saved = getSharedPreferences(QUICK_PREFS, MODE_PRIVATE).getString("order", "");
        String[] defaults = new String[] {"sound", "bluetooth", "settings", "theme"};
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

    private String glyphForQuickKey(String key) {
        if ("settings".equals(key)) return "settings";
        if ("theme".equals(key)) return "palette";
        if ("brightness".equals(key)) return "brightness";
        if ("volume".equals(key)) return "volume";
        return key;
    }

    private View compactQuickTile(String key, ThemeManager.Appearance a) {
        String label = quickLabel(key);
        String glyph = glyphForQuickKey(key);
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER);
        tile.setPadding(dp(2), dp(2), dp(2), dp(2));
        QinsGlyphView icon = new QinsGlyphView(this, glyph, a.accent);
        icon.setVisualStyle(a.id);
        icon.setTag("quick-glyph");
        tile.addView(icon, new LinearLayout.LayoutParams(dp(23), dp(23)));
        TextView text = new TextView(this);
        text.setText(label);
        text.setTextColor(a.text);
        text.setTextSize(8);
        text.setGravity(Gravity.CENTER);
        tile.addView(text);
        TextView state = new TextView(this);
        state.setTextColor(a.muted);
        state.setTextSize(7);
        state.setGravity(Gravity.CENTER);
        tile.addView(state);
        tile.setFocusable(true);
        tile.setFocusableInTouchMode(false);
        styleSurface(tile, a.tile, a.border, a.focus, a.cornerDp);
        registerQuickTile(key, tile, state);
        return tile;
    }

    private String[] quickLabels() {
        return new String[] {getString(R.string.launcher_sound), getString(R.string.launcher_bluetooth),
                getString(R.string.launcher_settings), getString(R.string.launcher_theme), getString(R.string.launcher_wifi),
                getString(R.string.launcher_brightness), getString(R.string.launcher_volume)};
    }

    private String quickLabel(String key) {
        for (int i = 0; i < QUICK_OPTIONS.length; i++) if (QUICK_OPTIONS[i].equals(key)) return quickLabels()[i];
        return key;
    }

    private void showQuickSlotPicker() {
        final String[] slots = new String[] {"Shortcut 1", "Shortcut 2", "Shortcut 3", "Shortcut 4"};
        new android.app.AlertDialog.Builder(this)
                .setTitle(getString(R.string.launcher_edit_compact_controls))
                .setItems(slots, new android.content.DialogInterface.OnClickListener() {
                    @Override public void onClick(android.content.DialogInterface dialog, int which) {
                        showQuickTileEditor(which);
                    }
                }).setNegativeButton("Done", null).show();
    }

    private void showQuickTileEditor(final int index) {
        final String[] keys = getQuickTileKeys();
        new android.app.AlertDialog.Builder(this)
                .setTitle(getString(R.string.launcher_choose_shortcut, index + 1))
                .setItems(quickLabels(), new android.content.DialogInterface.OnClickListener() {
                    @Override public void onClick(android.content.DialogInterface dialog, int which) {
                        keys[index] = QUICK_OPTIONS[which];
                        saveQuickTileKeys(keys);
                        showCompactQuickPanel(true);
                    }
                }).setNegativeButton("Cancel", null).show();
    }

    private boolean openQuickSetting(String key) {
        try {
            Intent intent;
            if ("bluetooth".equals(key)) intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
            else if ("wifi".equals(key)) intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
            else if ("sound".equals(key) || "volume".equals(key)) intent = new Intent(Settings.ACTION_SOUND_SETTINGS);
            else if ("brightness".equals(key)) intent = new Intent(Settings.ACTION_DISPLAY_SETTINGS);
            else if ("theme".equals(key)) { showThemePicker(); return true; }
            else intent = new Intent(Settings.ACTION_SETTINGS);
            startActivity(intent);
            return true;
        } catch (RuntimeException error) {
            android.widget.Toast.makeText(this, R.string.launcher_settings_unavailable,
                    android.widget.Toast.LENGTH_SHORT).show();
            return true;
        }
    }

    private void updateSoundGlyph(View tile, int mode) {
        if (tile == null) return;
        QinsGlyphView glyph = findQuickGlyph(tile);
        if (glyph == null) return;
        glyph.setGlyph(mode == AudioManager.RINGER_MODE_SILENT ? "sound-muted" :
                (mode == AudioManager.RINGER_MODE_VIBRATE ? "sound-vibrate" : "sound"));
    }

    private QinsGlyphView findQuickGlyph(View view) {
        if (view instanceof QinsGlyphView && "quick-glyph".equals(view.getTag())) return (QinsGlyphView) view;
        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                QinsGlyphView found = findQuickGlyph(group.getChildAt(i));
                if (found != null) return found;
            }
        }
        return null;
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
        else if ("theme".equals(key)) cycleAppearance();
        else if ("brightness".equals(key) || "volume".equals(key)) showQuickPanel(true);
    }

    private QinsGlyphView pageArrow(String symbol, ThemeManager.Appearance a) {
        QinsGlyphView button = new QinsGlyphView(this, symbol, a.muted);
        button.setVisualStyle(a.id);
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
                add.setText("");
                add.setContentDescription("Empty home slot");
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

            float iconScale = getSharedPreferences("qins_home_layout", MODE_PRIVATE)
                    .getFloat("icon_scale", 1.0f);
            int iconFrameSize = clamp(Math.round((screenWidthDp() / 4 - 12) * iconScale), 36, 72);
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
            iconBackground.setColor(isFolder(info) ? 0x55000000 : a.tile);
            iconBackground.setStroke(dp(1), a.monochrome ? a.border : a.surfaceAlt);
            iconFrame.setBackgroundDrawable(iconBackground);
            if (isFolder(info)) {
                LinearLayout miniIcons = new LinearLayout(this);
                miniIcons.setGravity(Gravity.CENTER);
                miniIcons.setOrientation(LinearLayout.HORIZONTAL);
                List<ResolveInfo> folderApps = getFolderApps(info);
                int previewCount = Math.min(3, folderApps.size());
                int miniSize = previewCount >= 3 ? 12 : 15;
                for (int mini = 0; mini < previewCount; mini++) {
                    ImageView smallIcon = new ImageView(this);
                    smallIcon.setImageDrawable(folderApps.get(mini).loadIcon(getPackageManager()));
                    smallIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    if (a.monochrome) {
                        ColorMatrix matrix = new ColorMatrix();
                        matrix.setSaturation(0f);
                        smallIcon.setColorFilter(new ColorMatrixColorFilter(matrix));
                    }
                    LinearLayout.LayoutParams miniParams = new LinearLayout.LayoutParams(dp(miniSize), dp(miniSize));
                    miniParams.setMargins(dp(1), 0, dp(1), 0);
                    miniIcons.addView(smallIcon, miniParams);
                }
                iconFrame.addView(miniIcons, new FrameLayout.LayoutParams(
                        -1, dp(22), Gravity.CENTER));
            } else {
                ImageView icon = new ImageView(this);
                icon.setImageDrawable(info.loadIcon(getPackageManager()));
                icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
                if (a.monochrome) {
                    ColorMatrix matrix = new ColorMatrix();
                    matrix.setSaturation(0f);
                    icon.setColorFilter(new ColorMatrixColorFilter(matrix));
                }
                FrameLayout.LayoutParams iconParams = new FrameLayout.LayoutParams(
                        dp(iconSize), dp(iconSize), Gravity.CENTER);
                iconFrame.addView(icon, iconParams);
            }
            LinearLayout.LayoutParams frameParams = new LinearLayout.LayoutParams(
                    dp(iconFrameSize), dp(iconFrameSize));
            frameParams.gravity = Gravity.CENTER_HORIZONTAL;
            frameParams.bottomMargin = dp(4);
            tile.addView(iconFrame, frameParams);

            TextView label = new TextView(this);
            label.setText(isFolder(info) ? getFolderName(info) : info.loadLabel(getPackageManager()));
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
                        if (isFolder(info)) openHomeFolder(info);
                        else launchApp(info);
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

    private void launchApp(ResolveInfo info) {
        if (info == null) return;
        try {
            getSharedPreferences("qins_usage", MODE_PRIVATE).edit()
                    .putInt("count:" + componentKey(info),
                            getSharedPreferences("qins_usage", MODE_PRIVATE)
                                    .getInt("count:" + componentKey(info), 0) + 1)
                    .putLong("last:" + componentKey(info), System.currentTimeMillis())
                    .commit();
            startActivity(launchIntent(info));
        } catch (RuntimeException ignored) {
            android.widget.Toast.makeText(this, "App unavailable", android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    private List<ResolveInfo> getFrequentlyUsedApps(int limit) {
        final SharedPreferences usage = getSharedPreferences("qins_usage", MODE_PRIVATE);
        List<ResolveInfo> frequent = new ArrayList<ResolveInfo>();
        for (ResolveInfo info : installedApps) {
            if (usage.getInt("count:" + componentKey(info), 0) > 0 && !isDrawerFolderMember(info)) frequent.add(info);
        }
        Collections.sort(frequent, new Comparator<ResolveInfo>() {
            @Override public int compare(ResolveInfo left, ResolveInfo right) {
                int lc = usage.getInt("count:" + componentKey(left), 0);
                int rc = usage.getInt("count:" + componentKey(right), 0);
                if (lc != rc) return rc - lc;
                long lt = usage.getLong("last:" + componentKey(left), 0L);
                long rt = usage.getLong("last:" + componentKey(right), 0L);
                return lt == rt ? 0 : (lt > rt ? -1 : 1);
            }
        });
        if (frequent.size() > limit) {
            return new ArrayList<ResolveInfo>(frequent.subList(0, limit));
        }
        return frequent;
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
        pageDots.removeAllViews();
        int count = pageCount();
        int visible = Math.min(7, count);
        int start = Math.max(0, Math.min(page - 3, count - visible));
        for (int i = 0; i < visible; i++) {
            final int targetPage = start + i;
            TextView dot = new TextView(this);
            dot.setText(targetPage == page ? "●" : "○");
            dot.setTextColor(targetPage == page ? appearance().accent : appearance().muted);
            dot.setTextSize(targetPage == page ? 14 : 12);
            dot.setGravity(Gravity.CENTER);
            dot.setFocusable(true);
            dot.setFocusableInTouchMode(false);
            dot.setTag("page-dot:" + targetPage);
            dot.setContentDescription("Page " + (targetPage + 1));
            dot.setPadding(dp(5), 0, dp(5), 0);
            styleSurface(dot, 0x00000000, 0x00000000, appearance().accent, 12);
            dot.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    page = targetPage;
                    editMode = false;
                    showHome(false);
                }
            });
            dot.setOnLongClickListener(new View.OnLongClickListener() {
                @Override public boolean onLongClick(View v) { showAddPageDialog(); return true; }
            });
            pageDots.addView(dot, new LinearLayout.LayoutParams(dp(26), -1));
        }
    }

    private void focusPageDot(int targetPage) {
        if (pageDots == null) return;
        for (int i = 0; i < pageDots.getChildCount(); i++) {
            View dot = pageDots.getChildAt(i);
            if (dot.getTag() instanceof String &&
                    ("page-dot:" + targetPage).equals(dot.getTag())) {
                dot.requestFocus();
                return;
            }
        }
    }

    private boolean isRtlLayout() {
        return android.os.Build.VERSION.SDK_INT >= 17 &&
                getResources().getConfiguration().getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
    }

    private void changePage(int delta, int focusSlot) {
        int target = page + delta;
        if (target < 0 || target >= pageCount()) return;
        page = target;
        editMode = false;
        selectedHomeIndex = -1;
        showHome(false);
        // Keep the D-pad on the matching edge of the new page instead of
        // unexpectedly jumping to the first icon on every page transition.
        if (appRow != null && appRow.getChildCount() > 0) {
            int slot = Math.max(0, Math.min(focusSlot, appRow.getChildCount() - 1));
            appRow.getChildAt(slot).requestFocus();
        }
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
        addEditAction("scale", a, "Home icon size", new View.OnClickListener() {
            @Override public void onClick(View v) { showHomeIconSizePicker(); }
        });
        addEditAction("folder", a, "Create or manage folder", new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (selectedHomeIndex >= 0 && selectedHomeIndex < pinnedApps.size()) {
                    ResolveInfo selected = pinnedApps.get(selectedHomeIndex);
                    if (isFolder(selected)) showFolderActions(selected);
                    else showCreateFolderPicker();
                }
            }
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
        button.setVisualStyle(a.id);
        button.setContentDescription(description);
        button.setFocusable(true);
        button.setFocusableInTouchMode(false);
        styleSurface(button, a.surface, a.border, a.focus, a.cornerDp);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -1, 1);
        params.setMargins(dp(3), 0, dp(3), 0);
        editBar.addView(button, params);
        button.setOnClickListener(listener);
    }

    private void showHomeIconSizePicker() {
        final String[] labels = new String[] {"Default", "Large · 115%", "Larger · 130%", "Maximum · 145%"};
        final float[] scales = new float[] {1.0f, 1.15f, 1.30f, 1.45f};
        float current = getSharedPreferences("qins_home_layout", MODE_PRIVATE).getFloat("icon_scale", 1.0f);
        int selected = 0;
        for (int i = 0; i < scales.length; i++) if (Math.abs(scales[i] - current) < 0.02f) selected = i;
        new android.app.AlertDialog.Builder(this).setTitle("Home icon size")
                .setSingleChoiceItems(labels, selected, new android.content.DialogInterface.OnClickListener() {
                    @Override public void onClick(android.content.DialogInterface dialog, int which) {
                        getSharedPreferences("qins_home_layout", MODE_PRIVATE).edit()
                                .putFloat("icon_scale", scales[which]).commit();
                        dialog.dismiss();
                        showHome(false);
                    }
                }).setNegativeButton(android.R.string.cancel, null).show();
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

    private void showCreateFolderPicker() {
        if (selectedHomeIndex < 0 || selectedHomeIndex >= pinnedApps.size()) return;
        final ResolveInfo first = pinnedApps.get(selectedHomeIndex);
        if (isFolder(first)) { showFolderActions(first); return; }
        final List<ResolveInfo> candidates = new ArrayList<ResolveInfo>();
        for (ResolveInfo candidate : installedApps) {
            if (candidate == first || isFolder(candidate)) continue;
            boolean alreadyInAnotherFolder = isPinned(candidate) && !pinnedApps.contains(candidate);
            if (!alreadyInAnotherFolder) candidates.add(candidate);
        }
        if (candidates.isEmpty()) {
            android.widget.Toast.makeText(this, R.string.launcher_folder_empty,
                    android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        String[] labels = new String[candidates.size()];
        for (int i = 0; i < candidates.size(); i++) {
            labels[i] = candidates.get(i).loadLabel(getPackageManager()).toString();
        }
        new android.app.AlertDialog.Builder(this)
                .setTitle(R.string.launcher_choose_folder_app)
                .setItems(labels, new android.content.DialogInterface.OnClickListener() {
                    @Override public void onClick(android.content.DialogInterface dialog, int which) {
                        createHomeFolder(candidates.get(which));
                    }
                }).setNegativeButton(android.R.string.cancel, null).show();
    }

    private void createHomeFolder(ResolveInfo second) {
        if (selectedHomeIndex < 0 || selectedHomeIndex >= pinnedApps.size() || second == null) return;
        ResolveInfo first = pinnedApps.get(selectedHomeIndex);
        if (isFolder(first) || isFolder(second)) return;
        String firstKey = componentKey(first);
        String secondKey = componentKey(second);
        if (firstKey.equals(secondKey)) return;
        int secondIndex = pinnedApps.indexOf(second);
        SharedPreferences.Editor editor = getSharedPreferences(FOLDER_PREFS, MODE_PRIVATE).edit();
        editor.putString("apps:" + firstKey, firstKey + "\n" + secondKey);
        editor.putString("name:" + firstKey, getString(R.string.launcher_folder));
        editor.commit();
        if (secondIndex >= 0) {
            pinnedApps.remove(secondIndex);
            if (secondIndex < selectedHomeIndex) selectedHomeIndex--;
        }
        page = selectedHomeIndex / APPS_PER_PAGE;
        savePinnedApps();
        showHome(false);
        focusHomeIndex(selectedHomeIndex);
    }

    private void closeHomeFolder() {
        if (pageContainer != null && folderOverlay != null) {
            pageContainer.removeView(folderOverlay);
        }
        folderOverlay = null;
    }

    private void openHomeFolder(final ResolveInfo folder) {
        closeHomeFolder();
        final List<ResolveInfo> apps = getFolderApps(folder);
        if (apps.isEmpty()) return;
        final ThemeManager.Appearance a = appearance();

        FrameLayout scrim = new FrameLayout(this);
        scrim.setBackgroundColor(0x66000000);
        scrim.setClickable(true);
        scrim.setFocusable(true);
        scrim.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { closeHomeFolder(); }
        });

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setGravity(Gravity.CENTER_HORIZONTAL);
        panel.setPadding(dp(12), dp(10), dp(12), dp(10));
        GradientDrawable panelBackground = new GradientDrawable();
        panelBackground.setColor(0x99151A21);
        panelBackground.setCornerRadius(dp(23));
        panelBackground.setStroke(dp(1), 0x66FFFFFF);
        panel.setBackgroundDrawable(panelBackground);
        panel.setClickable(true);
        panel.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { /* consume taps inside the folder surface */ }
        });

        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = new TextView(this);
        title.setText(getFolderName(folder));
        title.setTextColor(0xFFF6F6F6);
        title.setTextSize(13);
        title.setGravity(Gravity.CENTER);
        titleRow.addView(title, new LinearLayout.LayoutParams(0, dp(32), 1));
        TextView close = new TextView(this);
        close.setText("×");
        close.setTextColor(0xCCFFFFFF);
        close.setTextSize(19);
        close.setGravity(Gravity.CENTER);
        close.setFocusable(true);
        close.setFocusableInTouchMode(false);
        styleSurface(close, 0x00000000, 0x00000000, a.accent, 16);
        close.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { closeHomeFolder(); }
        });
        titleRow.addView(close, new LinearLayout.LayoutParams(dp(30), dp(30)));
        panel.addView(titleRow, new LinearLayout.LayoutParams(-1, dp(34)));

        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        int columns = 3;
        int rows = (apps.size() + columns - 1) / columns;
        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        grid.setPadding(0, dp(4), 0, dp(2));
        View first = null;
        for (int rowIndex = 0; rowIndex < rows; rowIndex++) {
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER);
            for (int col = 0; col < columns; col++) {
                final int index = rowIndex * columns + col;
                if (index >= apps.size()) {
                    row.addView(new View(this), new LinearLayout.LayoutParams(0, dp(66), 1));
                    continue;
                }
                final ResolveInfo child = apps.get(index);
                LinearLayout item = new LinearLayout(this);
                item.setOrientation(LinearLayout.VERTICAL);
                item.setGravity(Gravity.CENTER);
                item.setFocusable(true);
                item.setFocusableInTouchMode(false);
                item.setClickable(true);
                ImageView icon = new ImageView(this);
                icon.setImageDrawable(child.loadIcon(getPackageManager()));
                icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
                item.addView(icon, new LinearLayout.LayoutParams(dp(32), dp(32)));
                TextView label = new TextView(this);
                label.setText(child.loadLabel(getPackageManager()));
                label.setTextColor(0xFFF6F6F6);
                label.setTextSize(9);
                label.setGravity(Gravity.CENTER);
                label.setSingleLine(true);
                label.setEllipsize(android.text.TextUtils.TruncateAt.END);
                item.addView(label, new LinearLayout.LayoutParams(-1, dp(22)));
                styleSurface(item, 0x00000000, 0x00000000, a.accent, 12);
                item.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        closeHomeFolder();
                        launchApp(child);
                    }
                });
                LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(0, dp(66), 1);
                itemParams.setMargins(dp(1), dp(1), dp(1), dp(1));
                row.addView(item, itemParams);
                if (first == null) first = item;
            }
            grid.addView(row, new LinearLayout.LayoutParams(-1, dp(68)));
        }
        scroll.addView(grid, new ScrollView.LayoutParams(-1, -2));
        int gridHeight = Math.min(dp(238), dp(Math.max(1, rows) * 68 + 8));
        panel.addView(scroll, new LinearLayout.LayoutParams(-1, gridHeight));

        int panelWidth = Math.min(dp(280), Math.max(dp(174),
                getResources().getDisplayMetrics().widthPixels - dp(28)));
        FrameLayout.LayoutParams panelParams = new FrameLayout.LayoutParams(
                panelWidth, -2, Gravity.CENTER);
        scrim.addView(panel, panelParams);
        pageContainer.addView(scrim, new FrameLayout.LayoutParams(-1, -1));
        folderOverlay = scrim;
        if (first != null) first.requestFocus();
    }

    private void showFolderActions(final ResolveInfo folder) {
        final String[] actions = new String[] {
                "Open folder", "Add app", "Rename folder", "Ungroup apps"
        };
        new android.app.AlertDialog.Builder(this)
                .setTitle(getFolderName(folder))
                .setItems(actions, new android.content.DialogInterface.OnClickListener() {
                    @Override public void onClick(android.content.DialogInterface dialog, int which) {
                        if (which == 0) openHomeFolder(folder);
                        else if (which == 1) showFolderAddPicker(folder);
                        else if (which == 2) renameHomeFolder(folder);
                        else unpackHomeFolder(folder);
                    }
                }).setNegativeButton(android.R.string.cancel, null).show();
    }

    private void showFolderAddPicker(final ResolveInfo folder) {
        final List<ResolveInfo> candidates = new ArrayList<ResolveInfo>();
        for (ResolveInfo info : installedApps) {
            if (!isPinned(info) && !isFolder(info)) candidates.add(info);
        }
        if (candidates.isEmpty()) {
            android.widget.Toast.makeText(this, R.string.launcher_no_results,
                    android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        String[] labels = new String[candidates.size()];
        for (int i = 0; i < candidates.size(); i++) labels[i] = candidates.get(i).loadLabel(getPackageManager()).toString();
        new android.app.AlertDialog.Builder(this)
                .setTitle(R.string.launcher_choose_folder_app)
                .setItems(labels, new android.content.DialogInterface.OnClickListener() {
                    @Override public void onClick(android.content.DialogInterface dialog, int which) {
                        String key = componentKey(folder);
                        SharedPreferences prefs = getSharedPreferences(FOLDER_PREFS, MODE_PRIVATE);
                        String raw = prefs.getString("apps:" + key, key);
                        String addKey = componentKey(candidates.get(which));
                        if (!containsComponent(getFolderApps(folder), addKey)) {
                            prefs.edit().putString("apps:" + key, raw + "\n" + addKey).commit();
                        }
                        showHome(false);
                    }
                }).setNegativeButton(android.R.string.cancel, null).show();
    }

    private void renameHomeFolder(final ResolveInfo folder) {
        final EditText name = new EditText(this);
        name.setSingleLine(true);
        name.setText(getFolderName(folder));
        new android.app.AlertDialog.Builder(this)
                .setTitle(R.string.launcher_folder)
                .setView(name)
                .setPositiveButton(android.R.string.ok, new android.content.DialogInterface.OnClickListener() {
                    @Override public void onClick(android.content.DialogInterface dialog, int which) {
                        String value = name.getText().toString().trim();
                        if (value.length() == 0) value = getString(R.string.launcher_folder);
                        getSharedPreferences(FOLDER_PREFS, MODE_PRIVATE).edit()
                                .putString("name:" + componentKey(folder), value).commit();
                        showHome(false);
                    }
                }).setNegativeButton(android.R.string.cancel, null).show();
    }

    private void unpackHomeFolder(ResolveInfo folder) {
        List<ResolveInfo> apps = getFolderApps(folder);
        int position = pinnedApps.indexOf(folder);
        if (position < 0) return;
        String folderKey = componentKey(folder);
        pinnedApps.remove(position);
        getSharedPreferences(FOLDER_PREFS, MODE_PRIVATE).edit()
                .remove("apps:" + folderKey)
                .remove("name:" + folderKey).commit();
        for (ResolveInfo info : apps) {
            if (!isPinned(info)) pinnedApps.add(Math.min(position++, pinnedApps.size()), info);
        }
        savePinnedApps();
        selectedHomeIndex = -1;
        editMode = false;
        page = Math.min(page, Math.max(0, pageCount() - 1));
        showHome(false);
    }

    private void removeSelectedApp() {
        if (selectedHomeIndex < 0 || selectedHomeIndex >= pinnedApps.size()) return;
        ResolveInfo removed = pinnedApps.get(selectedHomeIndex);
        if (isFolder(removed)) {
            SharedPreferences folders = getSharedPreferences(FOLDER_PREFS, MODE_PRIVATE);
            folders.edit().remove("apps:" + componentKey(removed))
                    .remove("name:" + componentKey(removed)).commit();
        }
        pinnedApps.remove(selectedHomeIndex);
        savePinnedApps();
        if (selectedHomeIndex >= pinnedApps.size()) selectedHomeIndex = pinnedApps.size() - 1;
        if (selectedHomeIndex < 0) page = 0;
        else page = selectedHomeIndex / APPS_PER_PAGE;
        editMode = false;
        showHome(false);
    }

    private int drawerColumns() {
        return getSharedPreferences(DRAWER_PREFS, MODE_PRIVATE).getInt("columns", 4);
    }

    private int drawerRows() {
        return getSharedPreferences(DRAWER_PREFS, MODE_PRIVATE).getInt("rows", 4);
    }

    private void showDrawerGridSettings() {
        final String[] choices = new String[] {"3 × 4", "4 × 4", "4 × 5", "5 × 5"};
        new android.app.AlertDialog.Builder(this)
                .setTitle(R.string.launcher_grid_layout)
                .setItems(choices, new android.content.DialogInterface.OnClickListener() {
                    @Override public void onClick(android.content.DialogInterface dialog, int which) {
                        int columns = which == 0 ? 3 : (which == 3 ? 5 : 4);
                        int rows = which == 0 || which == 1 ? 4 : 5;
                        getSharedPreferences(DRAWER_PREFS, MODE_PRIVATE).edit()
                                .putInt("columns", columns).putInt("rows", rows).commit();
                        drawerPage = 0;
                        showDrawer(pinSelectionMode);
                    }
                }).setNegativeButton(android.R.string.cancel, null).show();
    }

    private View createDrawerAppTile(final ResolveInfo info, ThemeManager.Appearance a) {
        final boolean folder = isFolder(info);
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER);
        tile.setPadding(dp(2), dp(3), dp(2), dp(3));
        tile.setFocusable(true);
        tile.setFocusableInTouchMode(false);
        tile.setClickable(true);
        tile.setLongClickable(true);
        tile.setTag("drawer:" + componentKey(info));
        tile.setContentDescription((folder ? getFolderName(info) : info.loadLabel(getPackageManager())) +
                (folder ? ". Folder. Press to open, long press for options." : ". Press to open, long press to pin."));
        styleSurface(tile, a.surface, a.border, a.focus, a.cornerDp);

        int iconSize = clamp(screenWidthDp() / Math.max(5, drawerColumns()) - 15, 24, 40);
        if (folder) {
            LinearLayout preview = new LinearLayout(this);
            preview.setOrientation(LinearLayout.HORIZONTAL);
            preview.setGravity(Gravity.CENTER);
            List<ResolveInfo> children = getFolderApps(info);
            int count = Math.min(3, children.size());
            int mini = count >= 3 ? 15 : 19;
            for (int i = 0; i < count; i++) {
                ImageView childIcon = new ImageView(this);
                childIcon.setImageDrawable(children.get(i).loadIcon(getPackageManager()));
                childIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);
                LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(dp(mini), dp(mini));
                cp.setMargins(dp(1), 0, dp(1), 0);
                preview.addView(childIcon, cp);
            }
            tile.addView(preview, new LinearLayout.LayoutParams(-1, dp(iconSize)));
        } else {
            ImageView icon = new ImageView(this);
            icon.setImageDrawable(info.loadIcon(getPackageManager()));
            icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            tile.addView(icon, new LinearLayout.LayoutParams(dp(iconSize), dp(iconSize)));
        }

        TextView label = new TextView(this);
        label.setText(folder ? getFolderName(info) : info.loadLabel(getPackageManager()));
        label.setTextColor(a.text);
        label.setTextSize(screenWidthDp() < 210 ? 7 : 9);
        label.setGravity(Gravity.CENTER);
        label.setSingleLine(true);
        label.setEllipsize(android.text.TextUtils.TruncateAt.END);
        tile.addView(label, new LinearLayout.LayoutParams(-1, dp(19)));

        TextView mark = new TextView(this);
        mark.setText(isPinned(info) ? "●" : "");
        mark.setTextColor(a.accent);
        mark.setTextSize(8);
        mark.setGravity(Gravity.CENTER);
        tile.addView(mark, new LinearLayout.LayoutParams(-1, dp(10)));

        tile.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (pinSelectionMode) pinApp(info, true);
                else if (isFolder(info)) openHomeFolder(info);
                else launchApp(info);
            }
        });
        tile.setOnLongClickListener(new View.OnLongClickListener() {
            @Override public boolean onLongClick(View v) {
                if (isFolder(info)) showDrawerFolderActions(info);
                else pinApp(info, false);
                return true;
            }
        });
        return tile;
    }

    private void showDrawer(boolean chooseToPin) {
        removeCompactPanelOverlay();
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

        List<ResolveInfo> drawerApps = getDrawerApps();
        LinearLayout searchRow = new LinearLayout(this);
        searchRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView searchText = new TextView(this);
        searchText.setText(drawerSearchQuery.length() == 0
                ? getString(R.string.launcher_search_hint) : "T9  ·  " + drawerSearchQuery);
        searchText.setTextColor(drawerSearchQuery.length() == 0 ? a.muted : a.text);
        searchText.setTextSize(10);
        searchText.setPadding(dp(8), 0, dp(4), 0);
        searchRow.addView(searchText, new LinearLayout.LayoutParams(0, dp(30), 1));
        TextView clearSearch = new TextView(this);
        clearSearch.setText(getString(R.string.launcher_search_clear));
        clearSearch.setTextColor(a.accent);
        clearSearch.setTextSize(9);
        clearSearch.setGravity(Gravity.CENTER);
        clearSearch.setFocusable(true);
        clearSearch.setFocusableInTouchMode(false);
        clearSearch.setTag("drawer-search-clear");
        clearSearch.setPadding(dp(7), 0, dp(7), 0);
        styleSurface(clearSearch, a.surface, a.border, a.focus, a.cornerDp);
        clearSearch.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { drawerSearchQuery = ""; drawerPage = 0; showDrawer(pinSelectionMode); }
        });
        searchRow.addView(clearSearch, new LinearLayout.LayoutParams(-2, dp(28)));
        list.addView(searchRow, new LinearLayout.LayoutParams(-1, dp(34)));

        List<ResolveInfo> frequentApps = drawerSearchQuery.length() == 0
                ? getFrequentlyUsedApps(4) : Collections.<ResolveInfo>emptyList();
        if (!frequentApps.isEmpty()) {
            TextView frequentTitle = new TextView(this);
            frequentTitle.setText(getString(R.string.launcher_frequent));
            frequentTitle.setTextColor(a.muted);
            frequentTitle.setTextSize(10);
            frequentTitle.setPadding(dp(5), dp(4), dp(5), dp(5));
            list.addView(frequentTitle, new LinearLayout.LayoutParams(-1, dp(22)));

            LinearLayout frequentRow = new LinearLayout(this);
            frequentRow.setOrientation(LinearLayout.HORIZONTAL);
            frequentRow.setGravity(Gravity.CENTER);
            for (final ResolveInfo frequentInfo : frequentApps) {
                View item = createDrawerAppTile(frequentInfo, a);
                LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(0, dp(62), 1);
                itemParams.setMargins(dp(2), 0, dp(2), dp(4));
                frequentRow.addView(item, itemParams);
            }
            list.addView(frequentRow, new LinearLayout.LayoutParams(-1, dp(66)));
        }

        LinearLayout gridHeading = new LinearLayout(this);
        gridHeading.setGravity(Gravity.CENTER_VERTICAL);
        TextView allTitle = new TextView(this);
        allTitle.setText(getString(R.string.launcher_all_apps));
        allTitle.setTextColor(a.text);
        allTitle.setTextSize(12);
        gridHeading.addView(allTitle, new LinearLayout.LayoutParams(0, dp(32), 1));
        TextView createFolder = new TextView(this);
        createFolder.setText("Folder +");
        createFolder.setTextColor(a.accent);
        createFolder.setTextSize(9);
        createFolder.setGravity(Gravity.CENTER);
        createFolder.setFocusable(true);
        createFolder.setFocusableInTouchMode(false);
        createFolder.setPadding(dp(6), 0, dp(6), 0);
        styleSurface(createFolder, a.surface, a.border, a.focus, a.cornerDp);
        createFolder.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showDrawerCreateFolderDialog(); }
        });
        LinearLayout.LayoutParams createFolderParams = new LinearLayout.LayoutParams(-2, dp(28));
        createFolderParams.rightMargin = dp(5);
        gridHeading.addView(createFolder, createFolderParams);
        TextView editGrid = new TextView(this);
        editGrid.setText(getString(R.string.launcher_edit_grid));
        editGrid.setTextColor(a.accent);
        editGrid.setTextSize(9);
        editGrid.setGravity(Gravity.CENTER);
        editGrid.setFocusable(true);
        editGrid.setFocusableInTouchMode(false);
        editGrid.setPadding(dp(7), 0, dp(7), 0);
        styleSurface(editGrid, a.surface, a.border, a.focus, a.cornerDp);
        editGrid.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showDrawerGridSettings(); }
        });
        gridHeading.addView(editGrid, new LinearLayout.LayoutParams(-2, dp(28)));
        list.addView(gridHeading, new LinearLayout.LayoutParams(-1, dp(34)));

        final int columns = drawerColumns();
        final int rows = drawerRows();
        int pageSize = columns * rows;
        int pageCount = Math.max(1, (drawerApps.size() + pageSize - 1) / pageSize);
        drawerPage = Math.max(0, Math.min(drawerPage, pageCount - 1));
        int first = drawerPage * pageSize;
        int last = Math.min(drawerApps.size(), first + pageSize);
        int cellHeight = clamp((screenHeightDp() - 100) / rows, 48, 75);
        int firstGridChild = list.getChildCount();
        for (int rowIndex = 0; rowIndex < rows; rowIndex++) {
            LinearLayout gridRow = new LinearLayout(this);
            gridRow.setOrientation(LinearLayout.HORIZONTAL);
            gridRow.setGravity(Gravity.CENTER);
            for (int col = 0; col < columns; col++) {
                int appIndex = first + rowIndex * columns + col;
                View cell;
                if (appIndex < last) cell = createDrawerAppTile(drawerApps.get(appIndex), a);
                else cell = new View(this);
                LinearLayout.LayoutParams cellParams = new LinearLayout.LayoutParams(0, dp(cellHeight), 1);
                cellParams.setMargins(dp(2), dp(2), dp(2), dp(2));
                gridRow.addView(cell, cellParams);
            }
            list.addView(gridRow, new LinearLayout.LayoutParams(-1, dp(cellHeight + 4)));
        }

        LinearLayout pager = new LinearLayout(this);
        pager.setGravity(Gravity.CENTER);
        TextView previous = new TextView(this);
        previous.setText("‹");
        previous.setTextSize(23);
        previous.setTextColor(drawerPage > 0 ? a.accent : a.muted);
        previous.setGravity(Gravity.CENTER);
        previous.setFocusable(drawerPage > 0);
        previous.setFocusableInTouchMode(false);
        previous.setTag("drawer-page-prev");
        styleSurface(previous, a.surface, a.border, a.focus, a.cornerDp);
        previous.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { if (drawerPage > 0) { drawerPage--; showDrawer(pinSelectionMode); } }
        });
        pager.addView(previous, new LinearLayout.LayoutParams(dp(48), dp(34)));
        TextView pageLabel = new TextView(this);
        pageLabel.setText((drawerPage + 1) + " / " + pageCount);
        pageLabel.setTextColor(a.muted);
        pageLabel.setGravity(Gravity.CENTER);
        pager.addView(pageLabel, new LinearLayout.LayoutParams(dp(65), dp(34)));
        TextView next = new TextView(this);
        next.setText("›");
        next.setTextSize(23);
        next.setTextColor(drawerPage + 1 < pageCount ? a.accent : a.muted);
        next.setGravity(Gravity.CENTER);
        next.setFocusable(drawerPage + 1 < pageCount);
        next.setFocusableInTouchMode(false);
        next.setTag("drawer-page-next");
        styleSurface(next, a.surface, a.border, a.focus, a.cornerDp);
        next.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { if (drawerPage + 1 < pageCount) { drawerPage++; showDrawer(pinSelectionMode); } }
        });
        pager.addView(next, new LinearLayout.LayoutParams(dp(48), dp(34)));
        list.addView(pager, new LinearLayout.LayoutParams(-1, dp(38)));

        if (last > first) {
            int focusIndex = first;
            for (int i = first; i < last; i++) {
                if (isPinned(drawerApps.get(i))) { focusIndex = i; break; }
            }
            int rowIndex = (focusIndex - first) / columns;
            int colIndex = (focusIndex - first) % columns;
            View focusRow = list.getChildAt(firstGridChild + rowIndex);
            if (focusRow instanceof LinearLayout) {
                LinearLayout row = (LinearLayout) focusRow;
                if (colIndex < row.getChildCount()) row.getChildAt(colIndex).requestFocus();
            }
        } else if (drawerApps.isEmpty()) {
            TextView noResults = new TextView(this);
            noResults.setText(R.string.launcher_no_results);
            noResults.setTextColor(a.muted);
            noResults.setGravity(Gravity.CENTER);
            noResults.setTextSize(11);
            list.addView(noResults, new LinearLayout.LayoutParams(-1, dp(52)));
            clearSearch.requestFocus();
        } else editGrid.requestFocus();
    }

    private boolean isDrawerFolderMember(ResolveInfo info) {
        if (info == null) return false;
        String key = componentKey(info);
        for (ResolveInfo possibleFolder : installedApps) {
            if (possibleFolder != info && isFolder(possibleFolder) &&
                    containsComponent(getFolderApps(possibleFolder), key)) return true;
        }
        return false;
    }

    private List<ResolveInfo> getDrawerApps() {
        List<ResolveInfo> result = new ArrayList<ResolveInfo>();
        String query = drawerSearchQuery;
        for (ResolveInfo info : installedApps) {
            if (isDrawerFolderMember(info)) continue;
            String label = isFolder(info) ? getFolderName(info) :
                    info.loadLabel(getPackageManager()).toString();
            if (T9Matcher.matches(label, query)) result.add(info);
        }
        return result;
    }

    private void showDrawerCreateFolderDialog() {
        final List<ResolveInfo> candidates = new ArrayList<ResolveInfo>();
        final List<String> labels = new ArrayList<String>();
        for (ResolveInfo info : installedApps) {
            if (!isFolder(info) && !isDrawerFolderMember(info)) {
                candidates.add(info);
                labels.add(info.loadLabel(getPackageManager()).toString());
            }
        }
        if (candidates.size() < 2) {
            android.widget.Toast.makeText(this, "Select at least two apps to create a folder", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(12), dp(6), dp(12), dp(6));
        final EditText folderName = new EditText(this);
        folderName.setSingleLine(true);
        folderName.setHint("Folder name");
        form.addView(folderName, new LinearLayout.LayoutParams(-1, dp(48)));
        TextView hint = new TextView(this);
        hint.setText("Select two or more apps");
        hint.setTextSize(11);
        hint.setPadding(0, dp(5), 0, dp(5));
        form.addView(hint);
        final android.widget.ListView choices = new android.widget.ListView(this);
        choices.setChoiceMode(android.widget.ListView.CHOICE_MODE_MULTIPLE);
        choices.setAdapter(new android.widget.ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_multiple_choice, labels));
        form.addView(choices, new LinearLayout.LayoutParams(-1, dp(250)));
        new android.app.AlertDialog.Builder(this).setTitle("Create app folder")
                .setView(form)
                .setPositiveButton("Create", new android.content.DialogInterface.OnClickListener() {
                    @Override public void onClick(android.content.DialogInterface dialog, int which) {
                        android.util.SparseBooleanArray checked = choices.getCheckedItemPositions();
                        List<ResolveInfo> selected = new ArrayList<ResolveInfo>();
                        for (int i = 0; i < candidates.size(); i++) if (checked.get(i)) selected.add(candidates.get(i));
                        if (selected.size() < 2) {
                            android.widget.Toast.makeText(QinsLauncherActivity.this, "Choose at least two apps", android.widget.Toast.LENGTH_SHORT).show();
                            return;
                        }
                        String name = folderName.getText().toString().trim();
                        if (name.length() == 0) name = "Folder";
                        StringBuilder members = new StringBuilder();
                        for (ResolveInfo info : selected) {
                            if (members.length() > 0) members.append('\n');
                            members.append(componentKey(info));
                        }
                        String anchorKey = componentKey(selected.get(0));
                        getSharedPreferences(FOLDER_PREFS, MODE_PRIVATE).edit()
                                .putString("apps:" + anchorKey, members.toString())
                                .putString("name:" + anchorKey, name).commit();
                        drawerPage = 0;
                        showDrawer(false);
                    }
                }).setNegativeButton(android.R.string.cancel, null).show();
    }

    private void showDrawerFolderActions(final ResolveInfo folder) {
        final String[] actions = new String[] {"Open folder", "Add apps", "Rename", "Ungroup", "Add folder to Home"};
        new android.app.AlertDialog.Builder(this).setTitle(getFolderName(folder))
                .setItems(actions, new android.content.DialogInterface.OnClickListener() {
                    @Override public void onClick(android.content.DialogInterface dialog, int which) {
                        if (which == 0) openHomeFolder(folder);
                        else if (which == 1) showDrawerFolderAddPicker(folder);
                        else if (which == 2) renameDrawerFolder(folder);
                        else if (which == 3) {
                            getSharedPreferences(FOLDER_PREFS, MODE_PRIVATE).edit()
                                    .remove("apps:" + componentKey(folder))
                                    .remove("name:" + componentKey(folder)).commit();
                            drawerPage = 0;
                            showDrawer(false);
                        } else pinApp(folder, false);
                    }
                }).setNegativeButton(android.R.string.cancel, null).show();
    }

    private void showDrawerFolderAddPicker(final ResolveInfo folder) {
        final List<ResolveInfo> candidates = new ArrayList<ResolveInfo>();
        for (ResolveInfo info : installedApps) {
            if (info != folder && !isFolder(info) && !isDrawerFolderMember(info)) candidates.add(info);
        }
        if (candidates.isEmpty()) return;
        String[] names = new String[candidates.size()];
        for (int i = 0; i < candidates.size(); i++) names[i] = candidates.get(i).loadLabel(getPackageManager()).toString();
        new android.app.AlertDialog.Builder(this).setTitle("Add apps to " + getFolderName(folder))
                .setItems(names, new android.content.DialogInterface.OnClickListener() {
                    @Override public void onClick(android.content.DialogInterface dialog, int which) {
                        String key = componentKey(folder);
                        String raw = getSharedPreferences(FOLDER_PREFS, MODE_PRIVATE).getString("apps:" + key, key);
                        String add = componentKey(candidates.get(which));
                        getSharedPreferences(FOLDER_PREFS, MODE_PRIVATE).edit()
                                .putString("apps:" + key, raw + "\n" + add).commit();
                        showDrawer(false);
                    }
                }).setNegativeButton(android.R.string.cancel, null).show();
    }

    private void renameDrawerFolder(final ResolveInfo folder) {
        final EditText name = new EditText(this);
        name.setSingleLine(true);
        name.setText(getFolderName(folder));
        new android.app.AlertDialog.Builder(this).setTitle("Rename folder")
                .setView(name).setPositiveButton(android.R.string.ok, new android.content.DialogInterface.OnClickListener() {
                    @Override public void onClick(android.content.DialogInterface dialog, int which) {
                        String value = name.getText().toString().trim();
                        if (value.length() == 0) value = "Folder";
                        getSharedPreferences(FOLDER_PREFS, MODE_PRIVATE).edit()
                                .putString("name:" + componentKey(folder), value).commit();
                        showDrawer(false);
                    }
                }).setNegativeButton(android.R.string.cancel, null).show();
    }

    private int digitFromKeyCode(int keyCode) {
        if (keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_9) {
            return keyCode - KeyEvent.KEYCODE_0;
        }
        if (keyCode >= KeyEvent.KEYCODE_NUMPAD_0 && keyCode <= KeyEvent.KEYCODE_NUMPAD_9) {
            return keyCode - KeyEvent.KEYCODE_NUMPAD_0;
        }
        return -1;
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

    private void showPagePicker() {
        final String[] labels = new String[pageCount()];
        for (int i = 0; i < labels.length; i++) labels[i] = "Page " + (i + 1) + (i == page ? "  •" : "");
        new android.app.AlertDialog.Builder(this).setTitle("Go to page")
                .setItems(labels, new android.content.DialogInterface.OnClickListener() {
                    @Override public void onClick(android.content.DialogInterface dialog, int which) {
                        page = which; editMode = false; showHome(false);
                    }
                }).setNegativeButton("Cancel", null).show();
    }

    private void showAddPageDialog() {
        if (pageCount() >= 12) {
            android.widget.Toast.makeText(this, "Maximum 12 pages", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        new android.app.AlertDialog.Builder(this).setTitle("Add a home page?")
                .setMessage("Create another page for your apps.")
                .setPositiveButton("＋ Add page", new android.content.DialogInterface.OnClickListener() {
                    @Override public void onClick(android.content.DialogInterface dialog, int which) { addHomePage(); }
                }).setNegativeButton("Cancel", null).show();
    }

    private void showThemePicker() {
        final ThemeManager.Appearance[] options = ThemeManager.getAppearances();
        String[] labels = new String[options.length];
        int selected = 0;
        for (int i = 0; i < options.length; i++) {
            labels[i] = options[i].title + (options[i].id.equals(appearance().id) ? "  ✓" : "");
            if (options[i].id.equals(appearance().id)) selected = i;
        }
        new android.app.AlertDialog.Builder(this)
                .setTitle(R.string.launcher_choose_theme)
                .setSingleChoiceItems(labels, selected, new android.content.DialogInterface.OnClickListener() {
                    @Override public void onClick(android.content.DialogInterface dialog, int which) {
                        ThemeManager.applyAppearance(QinsLauncherActivity.this, options[which].id);
                        dialog.dismiss();
                        refreshLauncherSurface();
                    }
                }).setNegativeButton(android.R.string.cancel, null).show();
    }

    private void cycleAppearance() {
        String[] ids = new String[] {ThemeManager.APPEARANCE_GALAXY, ThemeManager.APPEARANCE_PIXEL, ThemeManager.APPEARANCE_NOTHING};
        String current = appearance().id;
        int next = 0;
        for (int i = 0; i < ids.length; i++) if (ids[i].equals(current)) next = (i + 1) % ids.length;
        ThemeManager.applyAppearance(this, ids[next]);
        if (quickPanelExpanded) showQuickPanel(false);
        else showCompactQuickPanel(false);
    }

    private String clockStyleKey(ThemeManager.Appearance a) { return "clock_" + a.id; }
    private String clockStyle(ThemeManager.Appearance a) {
        return getSharedPreferences("qins_clock", MODE_PRIVATE).getString(clockStyleKey(a), defaultClockStyle(a));
    }
    private String defaultClockStyle(ThemeManager.Appearance a) {
        if (ThemeManager.APPEARANCE_NOTHING.equals(a.id)) return "dot";
        if (ThemeManager.APPEARANCE_PIXEL.equals(a.id)) return "material";
        return "oneui";
    }
    private String[] clockStyleOptions(ThemeManager.Appearance a) {
        if (ThemeManager.APPEARANCE_NOTHING.equals(a.id)) return new String[] {"dot", "mono", "digital", "stacked", "compact", "analog"};
        if (ThemeManager.APPEARANCE_PIXEL.equals(a.id)) return new String[] {"material", "bold", "stacked", "compact", "analog"};
        return new String[] {"oneui", "stacked", "compact", "analog"};
    }
    private String[] clockStyleLabels(ThemeManager.Appearance a) {
        if (ThemeManager.APPEARANCE_NOTHING.equals(a.id)) return new String[] {"Nothing dot matrix", "Geist-style mono", "Digital bold", "Stacked red dots", "Compact", "Analog"};
        if (ThemeManager.APPEARANCE_PIXEL.equals(a.id)) return new String[] {"Material", "Bold digital", "Stacked clock", "Compact", "Analog"};
        return new String[] {"One UI large", "Stacked digital", "Compact", "Analog"};
    }
    private void showClockStylePicker() {
        final ThemeManager.Appearance a = appearance();
        final String[] values = clockStyleOptions(a);
        new android.app.AlertDialog.Builder(this).setTitle("Clock design · " + a.title)
                .setItems(clockStyleLabels(a), new android.content.DialogInterface.OnClickListener() {
                    @Override public void onClick(android.content.DialogInterface dialog, int which) {
                        getSharedPreferences("qins_clock", MODE_PRIVATE).edit()
                                .putString(clockStyleKey(a), values[which]).commit();
                        showHome(false);
                    }
                }).setNegativeButton("Cancel", null).show();
    }
    private float clockSizeForTheme(ThemeManager.Appearance a, boolean small) {
        String style = clockStyle(a);
        if ("compact".equals(style)) return small ? 23 : 31;
        if ("stacked".equals(style)) return small ? 27 : 37;
        if ("bold".equals(style) || "digital".equals(style)) return small ? 30 : 43;
        if ("mono".equals(style)) return small ? 27 : 38;
        return small ? 29 : 42;
    }
    private Typeface clockTypefaceForTheme(ThemeManager.Appearance a) {
        String style = clockStyle(a);
        if ("mono".equals(style) || "dot".equals(style) || a.useMonoType) return Typeface.MONOSPACE;
        if ("digital".equals(style)) return Typeface.create("monospace", Typeface.BOLD);
        if ("bold".equals(style) || "material".equals(style)) return Typeface.create("sans-serif-medium", Typeface.NORMAL);
        return Typeface.create("sans-serif-light", Typeface.NORMAL);
    }
    private int clockTypefaceStyle() {
        String style = clockStyle(appearance());
        return ("bold".equals(style) || "digital".equals(style)) ? Typeface.BOLD : Typeface.NORMAL;
    }
    private String formatClockForTheme(String time) {
        String style = clockStyle(appearance());
        if ("stacked".equals(style)) return time.replace(":", "\n");
        if ("dot".equals(style)) return time;
        return time;
    }

    private final class ThemePreview extends LinearLayout {
        private final TextView heading;
        private final TextView body;
        private final TextView activeButton;
        private final TextView inactiveButton;
        ThemePreview(Context context) {
            super(context);
            setOrientation(LinearLayout.VERTICAL);
            setPadding(dp(12), dp(10), dp(12), dp(10));
            heading = new TextView(context);
            heading.setText("QINS HOME  ·  LIVE PREVIEW");
            heading.setTextSize(10);
            addView(heading, new LinearLayout.LayoutParams(-1, dp(24)));
            body = new TextView(context);
            body.setText("12:45\nFriday  ·  Battery 82%");
            body.setTextSize(18);
            body.setPadding(dp(10), dp(8), dp(10), dp(8));
            addView(body, new LinearLayout.LayoutParams(-1, dp(70)));
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            activeButton = new TextView(context);
            activeButton.setText("Wi-Fi  ·  ON");
            activeButton.setGravity(Gravity.CENTER);
            activeButton.setTextSize(10);
            inactiveButton = new TextView(context);
            inactiveButton.setText("Bluetooth  ·  OFF");
            inactiveButton.setGravity(Gravity.CENTER);
            inactiveButton.setTextSize(10);
            LinearLayout.LayoutParams cell = new LinearLayout.LayoutParams(0, dp(42), 1);
            row.addView(activeButton, cell);
            LinearLayout.LayoutParams second = new LinearLayout.LayoutParams(0, dp(42), 1);
            second.leftMargin = dp(6);
            row.addView(inactiveButton, second);
            addView(row);
        }
        void update(int[] colors) {
            setBackgroundColor(colors[1]);
            heading.setTextColor(colors[3]);
            body.setBackgroundDrawable(makeShape(colors[2], colors[2], 14, false));
            body.setTextColor(colors[3]);
            activeButton.setBackgroundDrawable(makeShape(colors[0], colors[0], 10, false));
            activeButton.setTextColor(0xFFFFFFFF);
            inactiveButton.setBackgroundDrawable(makeShape(colors[2], colors[2], 10, false));
            inactiveButton.setTextColor(colors[3]);
        }
    }

    private int[] themeDefaultColors(ThemeManager.Appearance base) {
        return new int[] {base.accent, base.background, base.surface, base.text};
    }

    private String backgroundStyleTitle(String value) {
        if ("theme_2".equals(value)) return "Theme artwork 02";
        if ("theme_3".equals(value)) return "Theme artwork 03";
        if ("gradient".equals(value)) return "Soft gradient";
        if ("minimal".equals(value)) return "Minimal";
        if ("dots".equals(value)) return "Dot pattern";
        if ("photo".equals(value)) return "Photo";
        return "Theme artwork";
    }

    private void showThemeEditor() {
        final ThemeManager.Appearance base = ThemeManager.getAppearanceById(appearance().id);
        final int[] colors = themeDefaultColors(appearance());
        final String[] roles = new String[] {
                "Accent color", "Screen background", "Cards and panels", "Text and clock"
        };
        final String[] names = new String[] {
                "Theme default", "Soft blue", "Violet", "Coral", "Mint", "Amber", "White", "Charcoal", "Custom"
        };
        final int[] choices = new int[] {
                0, 0xFF4D83E6, 0xFF8B65C8, 0xFFE56B5F,
                0xFF4C9B7C, 0xFFD49A35, 0xFFF4F4F4, 0xFF242629
        };
        final int[] defaults = new int[] {base.accent, base.background, base.surface, base.text};
        final int[] selectedChoices = new int[4];
        final TextView[] roleViews = new TextView[4];
        for (int i = 0; i < 4; i++) {
            selectedChoices[i] = 8;
            if (colors[i] == defaults[i]) selectedChoices[i] = 0;
            for (int j = 1; j < choices.length; j++) {
                if (colors[i] == choices[j]) selectedChoices[i] = j;
            }
        }
        final String[] backgroundStyles = new String[] {"theme", "theme_2", "theme_3", "gradient", "minimal", "dots", "photo"};
        final String themeName = appearance().id.equals(ThemeManager.APPEARANCE_NOTHING) ? "Nothing" :
                (appearance().id.equals(ThemeManager.APPEARANCE_PIXEL) ? "Google Pixel" : "Samsung One UI");
        final String[] backgroundLabels = new String[] {
                themeName + " · Original", themeName + " · Artwork 02", themeName + " · Artwork 03",
                "Soft gradient", "Minimal", "Dot pattern", "Photo wallpaper"
        };
        final String[] selectedBackground = new String[] {
                getSharedPreferences(BACKGROUND_PREFS, MODE_PRIVATE).getString(BACKGROUND_STYLE_KEY, "theme")
        };

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(10), dp(4), dp(10), dp(4));
        final ThemePreview preview = new ThemePreview(this);
        preview.update(colors);
        content.addView(preview, new LinearLayout.LayoutParams(-1, dp(126)));
        TextView hint = new TextView(this);
        hint.setText("Choose colors and a background. Preview the palette, then save.");
        hint.setTextSize(11);
        hint.setTextColor(base.muted);
        hint.setPadding(dp(2), dp(8), dp(2), dp(8));
        content.addView(hint);

        for (int i = 0; i < roles.length; i++) {
            final int roleIndex = i;
            TextView option = new TextView(this);
            roleViews[i] = option;
            option.setText(roles[i] + "  ·  " + names[selectedChoices[i]]);
            option.setTextSize(13);
            option.setTextColor(colors[3]);
            option.setGravity(Gravity.CENTER_VERTICAL);
            option.setPadding(dp(12), 0, dp(10), 0);
            option.setFocusable(true);
            option.setFocusableInTouchMode(false);
            GradientDrawable swatch = new GradientDrawable();
            swatch.setShape(GradientDrawable.OVAL);
            swatch.setColor(colors[i]);
            swatch.setSize(dp(13), dp(13));
            option.setCompoundDrawables(swatch, null, null, null);
            option.setCompoundDrawablePadding(dp(9));
            styleSurface(option, colors[2], base.border, base.focus, 10f);
            LinearLayout.LayoutParams op = new LinearLayout.LayoutParams(-1, dp(44));
            op.topMargin = dp(5);
            content.addView(option, op);
            option.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    new android.app.AlertDialog.Builder(QinsLauncherActivity.this)
                            .setTitle(roles[roleIndex])
                            .setItems(names, new android.content.DialogInterface.OnClickListener() {
                                @Override public void onClick(android.content.DialogInterface dialog, int which) {
                                    if (which == 0) colors[roleIndex] = defaults[roleIndex];
                                    else if (which < choices.length) colors[roleIndex] = choices[which];
                                    selectedChoices[roleIndex] = which;
                                    preview.update(colors);
                                    roleViews[roleIndex].setText(roles[roleIndex] + "  ·  " + names[which]);
                                    GradientDrawable dot = new GradientDrawable();
                                    dot.setShape(GradientDrawable.OVAL);
                                    dot.setColor(colors[roleIndex]);
                                    dot.setSize(dp(13), dp(13));
                                    roleViews[roleIndex].setCompoundDrawables(dot, null, null, null);
                                }
                            }).setNegativeButton("Cancel", null).show();
                }
            });
        }

        TextView backgroundsTitle = new TextView(this);
        backgroundsTitle.setText("Backgrounds");
        backgroundsTitle.setTextSize(14);
        backgroundsTitle.setTextColor(base.text);
        backgroundsTitle.setPadding(dp(2), dp(12), dp(2), dp(3));
        content.addView(backgroundsTitle);
        final TextView backgroundChoice = new TextView(this);
        backgroundChoice.setText("Background style  ·  " + backgroundStyleTitle(selectedBackground[0]));
        backgroundChoice.setTextSize(13);
        backgroundChoice.setTextColor(colors[3]);
        backgroundChoice.setGravity(Gravity.CENTER_VERTICAL);
        backgroundChoice.setPadding(dp(12), 0, dp(10), 0);
        backgroundChoice.setFocusable(true);
        backgroundChoice.setFocusableInTouchMode(false);
        styleSurface(backgroundChoice, colors[2], base.border, base.focus, 10f);
        content.addView(backgroundChoice, new LinearLayout.LayoutParams(-1, dp(44)));
        backgroundChoice.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                int selected = 0;
                for (int i = 0; i < backgroundStyles.length; i++) {
                    if (backgroundStyles[i].equals(selectedBackground[0])) selected = i;
                }
                new android.app.AlertDialog.Builder(QinsLauncherActivity.this)
                        .setTitle("Choose background")
                        .setSingleChoiceItems(backgroundLabels, selected,
                                new android.content.DialogInterface.OnClickListener() {
                            @Override public void onClick(android.content.DialogInterface dialog, int which) {
                                selectedBackground[0] = backgroundStyles[which];
                                backgroundChoice.setText("Background style  ·  " + backgroundLabels[which]);
                                dialog.dismiss();
                            }
                        }).setNegativeButton("Cancel", null).show();
            }
        });

        ScrollView editorScroll = new ScrollView(this);
        editorScroll.setFillViewport(false);
        editorScroll.setVerticalScrollBarEnabled(true);
        editorScroll.addView(content, new ScrollView.LayoutParams(-1, -2));
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setTitle("Personalize theme  ·  " + appearance().title)
                .setView(editorScroll)
                .setPositiveButton("Save", new android.content.DialogInterface.OnClickListener() {
                    @Override public void onClick(android.content.DialogInterface d, int which) {
                        ThemeManager.setCustomColor(QinsLauncherActivity.this, appearance().id, "accent", colors[0]);
                        ThemeManager.setCustomColor(QinsLauncherActivity.this, appearance().id, "background", colors[1]);
                        ThemeManager.setCustomColor(QinsLauncherActivity.this, appearance().id, "surface", colors[2]);
                        ThemeManager.setCustomColor(QinsLauncherActivity.this, appearance().id, "text", colors[3]);
                        getSharedPreferences(BACKGROUND_PREFS, MODE_PRIVATE).edit()
                                .putString(BACKGROUND_STYLE_KEY, selectedBackground[0]).commit();
                        if (quickPanelExpanded) showQuickPanel(false);
                        else if (quickPanelVisible) showCompactQuickPanel(false);
                        else showHome(false);
                    }
                })
                .setNegativeButton("Cancel", null)
                .create();
        dialog.show();
    }

    private String[] getExpandedQuickTileKeys() {
        String saved = getSharedPreferences(QUICK_PREFS, MODE_PRIVATE).getString("expanded_order", "");
        String[] defaults = new String[] {"wifi", "bluetooth", "sound", "settings"};
        if (saved.length() == 0) return defaults;
        String[] parts = saved.split(",");
        String[] result = new String[4];
        for (int i = 0; i < 4; i++) result[i] = i < parts.length && isQuickOption(parts[i]) ? parts[i] : defaults[i];
        return result;
    }

    private void saveExpandedQuickTileKeys(String[] keys) {
        StringBuilder value = new StringBuilder();
        for (int i = 0; i < 4; i++) { if (i > 0) value.append(','); value.append(keys[i]); }
        getSharedPreferences(QUICK_PREFS, MODE_PRIVATE).edit().putString("expanded_order", value.toString()).commit();
    }

    private void showExpandedQuickTileEditor(final int index) {
        final String[] keys = getExpandedQuickTileKeys();
        new android.app.AlertDialog.Builder(this)
                .setTitle("Choose expanded tile " + (index + 1))
                .setItems(quickLabels(), new android.content.DialogInterface.OnClickListener() {
                    @Override public void onClick(android.content.DialogInterface dialog, int which) {
                        keys[index] = QUICK_OPTIONS[which];
                        saveExpandedQuickTileKeys(keys);
                        showQuickPanel(true);
                    }
                }).setNegativeButton("Cancel", null).show();
    }

    private void showQuickSettingsEditor() {
        final String[] slots = new String[] {"Compact shortcut 1", "Compact shortcut 2", "Compact shortcut 3", "Compact shortcut 4", "Expanded tile 1", "Expanded tile 2", "Expanded tile 3", "Expanded tile 4", "Theme colors", "Clock design"};
        new android.app.AlertDialog.Builder(this).setTitle("Customize quick settings")
                .setItems(slots, new android.content.DialogInterface.OnClickListener() {
                    @Override public void onClick(android.content.DialogInterface dialog, int which) {
                        if (which < 4) showQuickTileEditor(which);
                        else if (which < 8) showExpandedQuickTileEditor(which - 4);
                        else if (which == 8) showThemeEditor();
                        else showClockStylePicker();
                    }
                }).setNegativeButton("Done", null).show();
    }

    private void switchQuickPanelPage(int page) {
        if (!quickPanelVisible || !quickPanelExpanded) return;
        int target = Math.max(0, Math.min(1, page));
        if (quickPanelPage == target) return;
        quickPanelPage = target;
        showQuickPanel(false);
    }

    private void openNotificationAccessSettings() {
        Intent listenerSettings = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
        try {
            if (getPackageManager().resolveActivity(listenerSettings, 0) != null) {
                startActivity(listenerSettings);
            } else {
                // Android 4.4 keeps Notification access under Security settings.
                startActivity(new Intent(Settings.ACTION_SECURITY_SETTINGS));
                android.widget.Toast.makeText(this,
                        "Open Notification access in the Security settings", android.widget.Toast.LENGTH_LONG).show();
            }
        } catch (RuntimeException error) {
            try {
                startActivity(new Intent(Settings.ACTION_SECURITY_SETTINGS));
                android.widget.Toast.makeText(this,
                        "Open Notification access in the Security settings", android.widget.Toast.LENGTH_LONG).show();
            } catch (RuntimeException unavailable) {
                android.widget.Toast.makeText(this, "Notification access settings unavailable", android.widget.Toast.LENGTH_LONG).show();
            }
        }
    }

    private void requestRightKeyBinding() {
        awaitingRightKeyBind = true;
        android.widget.Toast.makeText(this, "Press the physical upper-right key once", android.widget.Toast.LENGTH_LONG).show();
    }

    private void showNotificationPage(LinearLayout panel, ThemeManager.Appearance a) {
        boolean enabled = NotificationBridgeService.isAccessEnabled(this);
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        Button access = new Button(this);
        access.setText(enabled ? "Notification access · ON" : "Enable notification access");
        access.setTextSize(9);
        actions.addView(access, new LinearLayout.LayoutParams(0, dp(42), 1));
        access.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { openNotificationAccessSettings(); }
        });
        Button bind = new Button(this);
        bind.setText("Bind right key");
        bind.setTextSize(9);
        LinearLayout.LayoutParams bindParams = new LinearLayout.LayoutParams(0, dp(42), 1);
        bindParams.leftMargin = dp(6);
        actions.addView(bind, bindParams);
        bind.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { requestRightKeyBinding(); }
        });
        panel.addView(actions);

        TextView explanation = new TextView(this);
        explanation.setText(enabled
                ? "Active notifications from the device appear here."
                : "Allow QINS UI Studio to read notifications once in Android Settings. Notification text stays on this device.");
        explanation.setTextColor(a.muted);
        explanation.setTextSize(10);
        explanation.setPadding(dp(4), dp(8), dp(4), dp(10));
        panel.addView(explanation);

        if (!enabled) {
            TextView empty = new TextView(this);
            empty.setText("No notification access yet");
            empty.setTextColor(a.text);
            empty.setGravity(Gravity.CENTER);
            empty.setTextSize(13);
            panel.addView(empty, new LinearLayout.LayoutParams(-1, dp(78)));
            return;
        }

        List<NotificationBridgeService.NotificationItem> items = NotificationBridgeService.snapshot();
        if (items.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No active notifications");
            empty.setTextColor(a.muted);
            empty.setGravity(Gravity.CENTER);
            empty.setTextSize(13);
            panel.addView(empty, new LinearLayout.LayoutParams(-1, dp(78)));
            return;
        }
        for (final NotificationBridgeService.NotificationItem item : items) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(10), dp(7), dp(10), dp(7));
            styleSurface(card, a.surface, a.border, a.focus, a.cornerDp);
            TextView app = new TextView(this);
            app.setText(notificationAppLabel(item.packageName));
            app.setTextColor(a.accent);
            app.setTextSize(9);
            card.addView(app);
            TextView title = new TextView(this);
            title.setText(item.title);
            title.setTextColor(a.text);
            title.setTextSize(12);
            title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            title.setMaxLines(2);
            card.addView(title);
            if (item.text != null && item.text.length() > 0) {
                TextView body = new TextView(this);
                body.setText(item.text);
                body.setTextColor(a.muted);
                body.setTextSize(10);
                body.setMaxLines(3);
                card.addView(body);
            }
            TextView when = new TextView(this);
            when.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(item.postedAt)));
            when.setTextColor(a.muted);
            when.setTextSize(8);
            card.addView(when);
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
            cardParams.topMargin = dp(6);
            panel.addView(card, cardParams);
            card.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    try {
                        Intent launch = getPackageManager().getLaunchIntentForPackage(item.packageName);
                        if (launch != null) startActivity(launch);
                    } catch (RuntimeException ignored) { }
                }
            });
        }
    }

    private String notificationAppLabel(String packageName) {
        try {
            android.content.pm.ApplicationInfo info = getPackageManager().getApplicationInfo(packageName, 0);
            return getPackageManager().getApplicationLabel(info).toString();
        } catch (RuntimeException ignored) { return packageName; }
    }

    private void showQuickPanel(boolean focusFirst) {
        removeCompactPanelOverlay();
        wifiState = null; bluetoothState = null; soundState = null;
        wifiTile = null; bluetoothTile = null; soundTile = null;
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
        dateParams.bottomMargin = dp(8);
        panel.addView(panelDate, dateParams);

        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams tabsParams = new LinearLayout.LayoutParams(-1, dp(36));
        tabsParams.bottomMargin = dp(9);
        panel.addView(tabs, tabsParams);
        final TextView controlsTab = new TextView(this);
        controlsTab.setText("CONTROLS");
        controlsTab.setTextSize(10);
        controlsTab.setGravity(Gravity.CENTER);
        controlsTab.setTextColor(quickPanelPage == 0 ? a.text : a.muted);
        controlsTab.setFocusable(true);
        controlsTab.setFocusableInTouchMode(false);
        styleSurface(controlsTab, quickPanelPage == 0 ? a.tileActive : a.surface, a.border, a.focus, a.cornerDp);
        tabs.addView(controlsTab, new LinearLayout.LayoutParams(0, -1, 1));
        controlsTab.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { switchQuickPanelPage(0); }
        });
        final TextView notificationsTab = new TextView(this);
        int notificationCount = NotificationBridgeService.snapshot().size();
        notificationsTab.setText("NOTIFICATIONS  " + (notificationCount > 0 ? "(" + notificationCount + ")" : ""));
        notificationsTab.setTextSize(10);
        notificationsTab.setGravity(Gravity.CENTER);
        notificationsTab.setTextColor(quickPanelPage == 1 ? a.text : a.muted);
        notificationsTab.setFocusable(true);
        notificationsTab.setFocusableInTouchMode(false);
        styleSurface(notificationsTab, quickPanelPage == 1 ? a.tileActive : a.surface, a.border, a.focus, a.cornerDp);
        LinearLayout.LayoutParams notificationsParams = new LinearLayout.LayoutParams(0, -1, 1);
        notificationsParams.leftMargin = dp(6);
        tabs.addView(notificationsTab, notificationsParams);
        notificationsTab.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { switchQuickPanelPage(1); }
        });

        if (quickPanelPage == 1) {
            showNotificationPage(panel, a);
            updateClockAndBattery();
            if (focusFirst) notificationsTab.requestFocus();
            return;
        }

        String[] expandedKeys = getExpandedQuickTileKeys();
        LinearLayout firstRow = new LinearLayout(this);
        firstRow.setOrientation(LinearLayout.HORIZONTAL);
        int quickTileHeight = clamp((int) (screenHeightDp() * 0.27f), 64, 78);
        panel.addView(firstRow, new LinearLayout.LayoutParams(-1, dp(quickTileHeight)));
        for (int i = 0; i < 2; i++) {
            final String key = expandedKeys[i];
            View tile = quickTile(glyphForQuickKey(key), quickLabel(key), a);
            LinearLayout.LayoutParams tileParams = new LinearLayout.LayoutParams(0, -1, 1);
            if (i > 0) tileParams.leftMargin = dp(7);
            firstRow.addView(tile, tileParams);
            tile.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { performQuickAction(key); }
            });
        }

        LinearLayout secondRow = new LinearLayout(this);
        secondRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams secondParams = new LinearLayout.LayoutParams(-1, dp(quickTileHeight));
        secondParams.topMargin = dp(7);
        panel.addView(secondRow, secondParams);
        for (int i = 2; i < 4; i++) {
            final String key = expandedKeys[i];
            View tile = quickTile(glyphForQuickKey(key), quickLabel(key), a);
            LinearLayout.LayoutParams tileParams = new LinearLayout.LayoutParams(0, -1, 1);
            if (i > 2) tileParams.leftMargin = dp(7);
            secondRow.addView(tile, tileParams);
            tile.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { performQuickAction(key); }
            });
        }

        updateQuickStates();

        addSlider(panel, "☼", getString(R.string.launcher_brightness), readBrightness(), 255,
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
        addSlider(panel, "♪", getString(R.string.launcher_volume), currentVolume, maxVolume,
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

        LinearLayout tools = new LinearLayout(this);
        tools.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams toolsParams = new LinearLayout.LayoutParams(-1, dp(42));
        toolsParams.topMargin = dp(10);
        panel.addView(tools, toolsParams);
        TextView customize = new TextView(this);
        customize.setText(getString(R.string.launcher_customize_quick));
        customize.setTextColor(a.text);
        customize.setGravity(Gravity.CENTER);
        customize.setTextSize(10);
        customize.setFocusable(true);
        customize.setFocusableInTouchMode(false);
        styleSurface(customize, a.surface, a.border, a.focus, a.cornerDp);
        tools.addView(customize, new LinearLayout.LayoutParams(0, -1, 1));
        customize.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showQuickSettingsEditor(); }
        });
        TextView themeSettings = new TextView(this);
        themeSettings.setText(getString(R.string.launcher_theme_settings));
        themeSettings.setTextColor(a.accent);
        themeSettings.setGravity(Gravity.CENTER);
        themeSettings.setTextSize(10);
        themeSettings.setFocusable(true);
        themeSettings.setFocusableInTouchMode(false);
        styleSurface(themeSettings, a.surface, a.border, a.focus, a.cornerDp);
        LinearLayout.LayoutParams themeParams = new LinearLayout.LayoutParams(0, -1, 1);
        themeParams.leftMargin = dp(6);
        tools.addView(themeSettings, themeParams);
        themeSettings.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showThemeEditor(); }
        });

        updateClockAndBattery();
        if (focusFirst && firstRow.getChildCount() > 0) firstRow.getChildAt(0).requestFocus();
    }

    private View quickTile(String symbol, String label, ThemeManager.Appearance a) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER);
        tile.setPadding(dp(3), dp(3), dp(3), dp(3));
        QinsGlyphView icon = new QinsGlyphView(this, symbol, a.accent);
        icon.setVisualStyle(a.id);
        icon.setTag("quick-glyph");
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
        styleSurface(tile, a.tile, a.border, a.focus, a.cornerDp);
        registerQuickTile(symbol, tile, state);
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

    private void registerQuickTile(final String key, final View tile, TextView state) {
        if ("wifi".equals(key)) { wifiTile = tile; wifiState = state; }
        else if ("bluetooth".equals(key)) { bluetoothTile = tile; bluetoothState = state; }
        else if ("sound".equals(key)) { soundTile = tile; soundState = state; }
        tile.setTag("quick:" + key);
        tile.setOnLongClickListener(new View.OnLongClickListener() {
            @Override public boolean onLongClick(View v) { return openQuickSetting(key); }
        });
        tile.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override public void onFocusChange(View v, boolean hasFocus) {
                updateQuickTileVisual(key, tile, quickKeyIsActive(key));
            }
        });
    }

    private boolean quickKeyIsActive(String key) {
        try {
            if ("wifi".equals(key)) {
                WifiManager manager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                return manager != null && manager.isWifiEnabled();
            }
            if ("bluetooth".equals(key)) {
                BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
                return adapter != null && adapter.isEnabled();
            }
            if ("sound".equals(key)) {
                AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                return audio != null && audio.getRingerMode() != AudioManager.RINGER_MODE_SILENT;
            }
        } catch (RuntimeException ignored) { }
        return false;
    }

    private void updateQuickTileVisual(String key, View tile, boolean active) {
        if (tile == null) return;
        ThemeManager.Appearance a = appearance();
        int fill = active ? a.tileActive : a.tile;
        int stroke = (active || tile.hasFocus()) ? a.accent : a.border;
        tile.setBackgroundDrawable(new FocusSurfaceDrawable(fill, stroke, a.accent,
                a.cornerDp, tile.hasFocus()));
    }

    private void setQuickState(String key, TextView state, View tile, String label, boolean active) {
        if (state != null) {
            state.setText(label);
            state.setTextColor(active ? appearance().accent : appearance().muted);
        }
        updateQuickTileVisual(key, tile, active);
    }

    private void updateQuickStates() {
        try {
            WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            boolean on = wifi != null && wifi.isWifiEnabled();
            setQuickState("wifi", wifiState, wifiTile, on ? "ON" : "OFF", on);
        } catch (RuntimeException ignored) { setQuickState("wifi", wifiState, wifiTile, "N/A", false); }
        try {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            boolean on = adapter != null && adapter.isEnabled();
            setQuickState("bluetooth", bluetoothState, bluetoothTile, on ? "ON" : "OFF", on);
        } catch (RuntimeException ignored) { setQuickState("bluetooth", bluetoothState, bluetoothTile, "N/A", false); }
        try {
            AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            int mode = audio == null ? AudioManager.RINGER_MODE_NORMAL : audio.getRingerMode();
            setQuickState("sound", soundState, soundTile, "", mode != AudioManager.RINGER_MODE_SILENT);
            updateSoundGlyph(soundTile, mode);
        } catch (RuntimeException ignored) { setQuickState("sound", soundState, soundTile, "", false); }
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
            int mode = audio.getRingerMode();
            int next = mode == AudioManager.RINGER_MODE_NORMAL ? AudioManager.RINGER_MODE_VIBRATE :
                    (mode == AudioManager.RINGER_MODE_VIBRATE ? AudioManager.RINGER_MODE_SILENT :
                            AudioManager.RINGER_MODE_NORMAL);
            audio.setRingerMode(next);
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

    private void openDialer() { openDialer(""); }

    private void openDialer(String initialNumber) {
        try {
            Intent dial = new Intent(Intent.ACTION_DIAL);
            if (initialNumber != null && initialNumber.length() > 0) {
                dial.setData(Uri.fromParts("tel", initialNumber, null));
            }
            startActivity(dial);
        } catch (RuntimeException unavailable) {
            ResolveInfo phone = null;
            for (ResolveInfo info : installedApps) {
                String label = info.loadLabel(getPackageManager()).toString().toLowerCase(Locale.ROOT);
                String packageName = info.activityInfo.packageName.toLowerCase(Locale.ROOT);
                if (label.contains("phone") || label.contains("dialer") ||
                        label.contains("טלפון") || packageName.contains("dialer")) {
                    phone = info;
                    break;
                }
            }
            if (phone != null) launchApp(phone);
            else android.widget.Toast.makeText(this, R.string.launcher_settings_unavailable,
                    android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isSelectKey(int keyCode) {
        return keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER ||
                keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER || keyCode == KeyEvent.KEYCODE_BUTTON_A;
    }

    private boolean performLongPressAction(View target) {
        if (target == null || !(target.getTag() instanceof String)) return false;
        String tag = (String) target.getTag();
        if ("home:clock".equals(tag)) { showClockStylePicker(); return true; }
        if ("pages".equals(tag) || tag.startsWith("page-dot:")) { showAddPageDialog(); return true; }
        if (tag.startsWith("home:")) {
            try { enterEditMode(Integer.parseInt(tag.substring(5))); return true; }
            catch (NumberFormatException ignored) { return false; }
        }
        if (tag.startsWith("drawer:")) {
            ResolveInfo info = findApp(tag.substring(7));
            if (info != null) { pinApp(info, false); return true; }
        }
        if (tag.startsWith("quick:")) return openQuickSetting(tag.substring(6));
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

    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
            if (awaitingRightKeyBind) {
                int keyCode = event.getKeyCode();
                if (keyCode != KeyEvent.KEYCODE_VOLUME_UP &&
                        keyCode != KeyEvent.KEYCODE_VOLUME_DOWN) {
                    getSharedPreferences(SOFTKEY_PREFS, MODE_PRIVATE).edit()
                            .putInt("right_key_code", keyCode)
                            .putInt("right_key_scan", event.getScanCode()).commit();
                    awaitingRightKeyBind = false;
                    android.widget.Toast.makeText(this, "Upper-right key saved", android.widget.Toast.LENGTH_SHORT).show();
                    quickPanelPage = 1;
                    showQuickPanel(false);
                    return true;
                }
            }
            if (quickPanelVisible && quickPanelExpanded) {
                SharedPreferences keys = getSharedPreferences(SOFTKEY_PREFS, MODE_PRIVATE);
                int boundCode = keys.getInt("right_key_code", Integer.MIN_VALUE);
                int boundScan = keys.getInt("right_key_scan", 0);
                if (boundCode != Integer.MIN_VALUE && event.getKeyCode() == boundCode &&
                        (boundScan == 0 || event.getScanCode() == boundScan)) {
                    switchQuickPanelPage(1);
                    return true;
                }
            }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (drawerVisible && !quickPanelVisible) {
            int digit = digitFromKeyCode(keyCode);
            if (digit >= 0) {
                drawerSearchQuery += String.valueOf(digit);
                drawerPage = 0;
                showDrawer(pinSelectionMode);
                return true;
            }
            if (keyCode == KeyEvent.KEYCODE_DEL || keyCode == KeyEvent.KEYCODE_FORWARD_DEL) {
                if (drawerSearchQuery.length() > 0) {
                    drawerSearchQuery = drawerSearchQuery.substring(0, drawerSearchQuery.length() - 1);
                    drawerPage = 0;
                    showDrawer(pinSelectionMode);
                } else showDrawer(pinSelectionMode);
                return true;
            }
        }
        if (!drawerVisible && !quickPanelVisible && !editMode && folderOverlay == null &&
                event.getRepeatCount() == 0) {
            int dialDigit = digitFromKeyCode(keyCode);
            if (dialDigit >= 0) {
                openDialer(String.valueOf(dialDigit));
                return true;
            }
            if (keyCode == KeyEvent.KEYCODE_STAR) {
                openDialer("*");
                return true;
            }
            if (keyCode == KeyEvent.KEYCODE_POUND) {
                openDialer("#");
                return true;
            }
        }
        if (isSelectKey(keyCode)) {
            View focused = getCurrentFocus();
            if (focused != null && "home:drawer-trigger".equals(focused.getTag())) {
                if (event.getRepeatCount() == 0) {
                    pendingSelectView = focused;
                    longPressPerformed = false;
                    event.startTracking();
                    return true;
                }
            }
            if (focused != null && focused.getTag() instanceof String &&
                    (((String) focused.getTag()).startsWith("home:") ||
                     ((String) focused.getTag()).startsWith("drawer:") ||
                     ((String) focused.getTag()).startsWith("page-dot:") ||
                     ((String) focused.getTag()).startsWith("quick:") ||
                     "home:drawer-trigger".equals(focused.getTag()) ||
                     "pages".equals(focused.getTag()))) {
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
        if (quickPanelVisible && quickPanelExpanded && event.getRepeatCount() == 0) {
            if (keyCode == KeyEvent.KEYCODE_SOFT_LEFT || keyCode == KeyEvent.KEYCODE_MENU ||
                    keyCode == KeyEvent.KEYCODE_F1) {
                switchQuickPanelPage(0);
                return true;
            }
            if (keyCode == KeyEvent.KEYCODE_SOFT_RIGHT || keyCode == KeyEvent.KEYCODE_F2 ||
                    keyCode == KeyEvent.KEYCODE_SEARCH) {
                switchQuickPanelPage(1);
                return true;
            }
        }
        if (keyCode == KeyEvent.KEYCODE_SOFT_LEFT && event.getRepeatCount() == 0) {
            openDialer();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_SOFT_RIGHT && event.getRepeatCount() == 0) {
            drawerSearchQuery = "";
            drawerPage = 0;
            showDrawer(false);
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_CALL && event.getRepeatCount() == 0) {
            openDialer();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_MENU && event.getRepeatCount() == 0) {
            // QINS firmware reports the left upper hardware key as MENU.
            // Never launch the developer Studio from a physical soft key.
            drawerSearchQuery = "";
            drawerPage = 0;
            showDrawer(false);
            return true;
        }
        // First DOWN opens the compact controls; a second, distinct press expands them.
        // Repeated key events from holding the button must not skip the compact stage.
        if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN && event.getRepeatCount() == 0 &&
                !drawerVisible && !editMode) {
            if (!quickPanelVisible) {
                showCompactQuickPanel(true);
                return true;
            }
            if (!quickPanelExpanded) {
                showQuickPanel(true);
                return true;
            }
        }
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (folderOverlay != null) {
                closeHomeFolder();
                return true;
            }
            if (drawerVisible && drawerSearchQuery.length() > 0) {
                drawerSearchQuery = drawerSearchQuery.substring(0, drawerSearchQuery.length() - 1);
                drawerPage = 0;
                showDrawer(pinSelectionMode);
                return true;
            }
            if (editMode) {
                editMode = false;
                selectedHomeIndex = -1;
                showHome(false);
                return true;
            }
            if (quickPanelVisible && quickPanelExpanded) {
                showCompactQuickPanel(true);
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
            boolean rtl = isRtlLayout();
            if ("home:drawer-trigger".equals(tag) &&
                    (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) &&
                    appRow != null && appRow.getChildCount() > 0) {
                boolean physicalLeft = keyCode == KeyEvent.KEYCODE_DPAD_LEFT;
                int edge = 0;
                for (int i = 1; i < appRow.getChildCount(); i++) {
                    View candidate = appRow.getChildAt(i);
                    View current = appRow.getChildAt(edge);
                    if (physicalLeft ? candidate.getLeft() < current.getLeft() :
                            candidate.getRight() > current.getRight()) {
                        edge = i;
                    }
                }
                appRow.getChildAt(edge).requestFocus();
                return true;
            }
            if (tag instanceof String && ((String) tag).startsWith("page-dot:")) {
                int delta = keyCode == KeyEvent.KEYCODE_DPAD_LEFT ? (rtl ? 1 : -1) : (rtl ? -1 : 1);
                int target = page + delta;
                if (target >= 0 && target < pageCount()) {
                    page = target;
                    editMode = false;
                    showHome(false);
                    focusPageDot(target);
                }
                return true;
            }
            if (tag instanceof String && ((String) tag).startsWith("home:")) {
                try {
                    int index = Integer.parseInt(((String) tag).substring(5));
                    if (editMode) {
                        int logicalDelta = keyCode == KeyEvent.KEYCODE_DPAD_LEFT ? (rtl ? 1 : -1) :
                                (rtl ? -1 : 1);
                        moveSelectedApp(logicalDelta);
                        return true;
                    }
                    if (appRow != null && appRow.getChildCount() > 0) {
                        int leftmost = 0;
                        int rightmost = 0;
                        for (int i = 1; i < appRow.getChildCount(); i++) {
                            if (appRow.getChildAt(i).getLeft() < appRow.getChildAt(leftmost).getLeft()) leftmost = i;
                            if (appRow.getChildAt(i).getRight() > appRow.getChildAt(rightmost).getRight()) rightmost = i;
                        }
                        int slot = index % APPS_PER_PAGE;
                        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT && slot == leftmost) {
                            int delta = rtl ? 1 : -1;
                            if (page + delta >= 0 && page + delta < pageCount()) {
                                changePage(delta, rtl ? 0 : APPS_PER_PAGE - 1);
                            }
                            return true;
                        }
                        if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT && slot == rightmost) {
                            int delta = rtl ? -1 : 1;
                            if (page + delta >= 0 && page + delta < pageCount()) {
                                changePage(delta, rtl ? APPS_PER_PAGE - 1 : 0);
                            }
                            return true;
                        }
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
        if (folderOverlay != null) {
            closeHomeFolder();
        } else if (editMode) {
            editMode = false;
            selectedHomeIndex = -1;
            showHome(false);
        } else if (quickPanelVisible && quickPanelExpanded) {
            showCompactQuickPanel(true);
        } else if (drawerVisible || quickPanelVisible) {
            showHome(false);
        }
    }
}