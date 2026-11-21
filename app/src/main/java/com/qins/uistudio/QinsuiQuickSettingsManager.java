package com.qins.uistudio;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ClipDrawable;
import android.media.AudioManager;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;

/**
 * Three OEM-inspired Quick Settings layouts for the 240x320 QINS display.
 * Pure framework widgets and Java; safe for Android 4.4 / API 19.
 * All system state is read from QuickSettingsController, never guessed.
 */
public final class QinsuiQuickSettingsManager {
    public enum ThemeType { ANDROID_17, ONE_UI_8, NOTHING_OS_5 }

    public interface Listener {
        void onAction(String action);
        void onOpenNotifications();
    }

    private final Context context;
    private final QuickSettingsController controller;
    private final Listener listener;
    private final AudioManager audio;

    private static final int FOREST = Color.rgb(14, 20, 17);
    private static final int MINT = Color.rgb(184, 230, 216);
    private static final int FOREST_TILE = Color.rgb(29, 38, 34);
    private static final int NAVY = Color.rgb(11, 15, 23);
    private static final int NAVY_CARD = Color.rgb(24, 34, 50);
    private static final int BLUE = Color.rgb(96, 165, 250);
    private static final int WHITE = Color.rgb(248, 250, 252);
    private static final int NOTHING_BLACK = Color.BLACK;
    private static final int NOTHING_CARD = Color.rgb(20, 20, 22);
    private static final int RED = Color.rgb(215, 25, 33);

    public QinsuiQuickSettingsManager(Context context, QuickSettingsController controller,
                                      Listener listener) {
        this.context = context;
        this.controller = controller;
        this.listener = listener;
        this.audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    public View build(ThemeType theme) {
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(8), dp(7), dp(8), dp(8));
        root.setClipChildren(false);
        root.setClipToPadding(false);
        if (theme == ThemeType.ANDROID_17) {
            root.setBackgroundColor(FOREST);
            buildAndroid17(root);
        } else if (theme == ThemeType.ONE_UI_8) {
            root.setBackgroundColor(NAVY);
            buildOneUI8(root);
        } else {
            root.setBackgroundColor(NOTHING_BLACK);
            buildNothing(root);
        }
        return root;
    }

    private void buildAndroid17(LinearLayout root) {
        SeekBar brightness = horizontalSlider(MINT, FOREST_TILE, controller.getBrightnessPercent());
        brightness.setContentDescription("Brightness");
        brightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                if (fromUser) controller.setBrightnessPercent(progress);
            }
            @Override public void onStartTrackingTouch(SeekBar bar) { }
            @Override public void onStopTrackingTouch(SeekBar bar) { }
        });
        add(root, brightness, -1, 32, 0, 0, 0, 0, 8);

        LinearLayout pills = row();
        add(pills, dualPill("Internet", controller.isWifiEnabled(), "מחובר", "כבוי", MINT, FOREST_TILE,
                new Runnable() { @Override public void run() { toggleWifi(); } }), 0, 42, 1f, 0, 3, 0, 3);
        add(pills, dualPill("Bluetooth", controller.isBluetoothEnabled(), "On", "Off", MINT, FOREST_TILE,
                new Runnable() { @Override public void run() { toggleBluetooth(); } }), 0, 42, 1, 0, 3, 0, 3);
        add(root, pills, -1, 42, 0, 0, 8, 0, 0);

        String[][] glyphs = new String[][] {
                {"◉", "ᛒ", "◷", "⌖"},
                {"✈", "☼", "⚙", "▶"}
        };
        String[][] actions = new String[][] {
                {"wifi", "bluetooth", "clock", "location"},
                {"airplane", "brightness", "settings", "media"}
        };
        for (int r = 0; r < 2; r++) {
            LinearLayout grid = row();
            grid.setGravity(Gravity.CENTER);
            for (int c = 0; c < 4; c++) {
                final String action = actions[r][c];
                final boolean active = r == 1 && c == 3;
                TextView tile = circle(glyphs[r][c], active ? MINT : FOREST_TILE,
                        active ? FOREST : MINT, 38, 17);
                tile.setContentDescription(action);
                tile.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) { dispatch(action); }
                });
                LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dp(38), dp(38));
                p.setMargins(dp(5), 0, dp(5), 0);
                grid.addView(tile, p);
            }
            add(root, grid, -1, 42, 0, 0, 3, 0, 0);
        }

        addMediaBar(root, FOREST_TILE, MINT, 34, false);
    }

    private void buildOneUI8(LinearLayout root) {
        LinearLayout top = row();
        add(top, topPill("Wi-Fi", controller.isWifiEnabled(), true,
                new Runnable() { @Override public void run() { toggleWifi(); } }), 0, 38, 1, 0, 3, 0, 3);
        add(top, topPill("Bluetooth", controller.isBluetoothEnabled(), false,
                new Runnable() { @Override public void run() { toggleBluetooth(); } }), 0, 38, 1, 0, 3, 0, 3);
        add(root, top, -1, 38, 0, 0, 8, 0, 0);

        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(5), dp(5), dp(5), dp(5));
        card.setBackground(round(NAVY_CARD, 20, 0));
        String[][] glyphs = {{"◉", "◷", "✉", "☼"}, {"⌂", "♧", "▦", "···"}};
        String[][] actions = {{"wifi", "clock", "notifications", "brightness"},
                {"home", "sound", "apps", "settings"}};
        for (int r = 0; r < 2; r++) {
            LinearLayout grid = row();
            grid.setGravity(Gravity.CENTER);
            for (int c = 0; c < 4; c++) {
                final String action = actions[r][c];
                boolean active = (r == 0 && (c == 0 || c == 3)) || (r == 1 && c == 2);
                TextView icon = circle(glyphs[r][c], active ? WHITE : Color.rgb(51, 65, 85),
                        active ? NAVY : WHITE, 32, 15);
                icon.setContentDescription(action);
                icon.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) { dispatch(action); }
                });
                LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(34), 1f);
                p.setMargins(dp(2), dp(1), dp(2), dp(1));
                grid.addView(icon, p);
            }
            card.addView(grid, new LinearLayout.LayoutParams(-1, dp(36)));
        }
        add(root, card, -1, 82, 0, 0, 9, 0, 0);

        addSplitSlider(root, true, "☾", "Brightness");
        addSplitSlider(root, false, "◖", "Volume");
    }

    private void buildNothing(LinearLayout root) {
        LinearLayout top = row();
        String[] glyphs = {"◉", "ᛒ", "⚙", "◖"};
        String[] actions = {"wifi", "bluetooth", "settings", "sound"};
        for (int i = 0; i < glyphs.length; i++) {
            final String action = actions[i];
            final boolean red = i == 3;
            TextView tile = circle(glyphs[i], red ? RED : NOTHING_CARD, WHITE, 38, 17);
            tile.setContentDescription(action);
            tile.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { dispatch(action); }
            });
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(38), 1f);
            p.setMargins(dp(5), 0, dp(5), 0);
            top.addView(tile, p);
        }
        add(root, top, -1, 42, 0, 0, 7, 0, 0);

        LinearLayout middle = row();
        middle.setGravity(Gravity.TOP);
        LinearLayout media = new LinearLayout(context);
        media.setOrientation(LinearLayout.VERTICAL);
        media.setGravity(Gravity.CENTER);
        media.setPadding(dp(5), dp(5), dp(5), dp(4));
        media.setBackground(round(NOTHING_CARD, 16, 0));
        TextView note = new TextView(context);
        note.setText("●
♫");
        note.setTextColor(RED);
        note.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        note.setTextSize(22);
        note.setGravity(Gravity.CENTER);
        media.addView(note, new LinearLayout.LayoutParams(-1, 0, 1f));
        LinearLayout controls = row();
        addMediaControl(controls, "‹", KeyEvent.KEYCODE_MEDIA_PREVIOUS);
        addMediaControl(controls, "▶", KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
        addMediaControl(controls, "›", KeyEvent.KEYCODE_MEDIA_NEXT);
        media.addView(controls, new LinearLayout.LayoutParams(-1, dp(24)));
        LinearLayout.LayoutParams mediaParams = new LinearLayout.LayoutParams(0, dp(114), 1.15f);
        mediaParams.rightMargin = dp(5);
        middle.addView(media, mediaParams);

        LinearLayout sliders = row();
        DotSlider volume = new DotSlider(context, controller.getVolumePercent(), "VOL", false);
        DotSlider bright = new DotSlider(context, controller.getBrightnessPercent(), "BRI", true);
        volume.setListener(new DotSlider.Listener() {
            @Override public void onValueChanged(int value) { controller.setVolumePercent(value); }
        });
        bright.setListener(new DotSlider.Listener() {
            @Override public void onValueChanged(int value) { controller.setBrightnessPercent(value); }
        });
        LinearLayout.LayoutParams v = new LinearLayout.LayoutParams(0, dp(114), 1f);
        v.rightMargin = dp(3);
        sliders.addView(volume, v);
        LinearLayout.LayoutParams b = new LinearLayout.LayoutParams(0, dp(114), 1f);
        b.leftMargin = dp(3);
        sliders.addView(bright, b);
        middle.addView(sliders, new LinearLayout.LayoutParams(0, dp(114), 0.85f));
        add(root, middle, -1, 114, 0, 0, 7, 0, 0);

        addNotificationDock(root);
    }

    private void addSplitSlider(LinearLayout root, final boolean isBrightness,
                                String sideGlyph, String label) {
        LinearLayout row = row();
        final int value = isBrightness ? controller.getBrightnessPercent() : controller.getVolumePercent();
        SeekBar seek = horizontalSlider(WHITE, Color.rgb(30, 41, 59), value);
        seek.setContentDescription(label);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                if (!fromUser) return;
                if (isBrightness) controller.setBrightnessPercent(progress);
                else controller.setVolumePercent(progress);
            }
            @Override public void onStartTrackingTouch(SeekBar bar) { }
            @Override public void onStopTrackingTouch(SeekBar bar) { }
        });
        add(row, seek, 0, 32, 1, 0, 0, 0, 6);
        TextView side = circle(sideGlyph, Color.rgb(51, 65, 85), WHITE, 32, 15);
        side.setContentDescription(isBrightness ? "Sleep mode" : "Mute");
        side.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { dispatch(isBrightness ? "sleep" : "sound"); }
        });
        add(row, side, 32, 32, 0, 0, 0, 0, 0);
        add(root, row, -1, 32, 0, 0, 7, 0, 0);
    }

    private void addNotificationDock(LinearLayout root) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(7), dp(5), dp(7), dp(5));
        card.setBackground(round(NOTHING_CARD, 15, 0));
        TextView title = new TextView(context);
        title.setText("NOTIFICATIONS");
        title.setTextColor(WHITE);
        title.setTextSize(9);
        title.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        card.addView(title, new LinearLayout.LayoutParams(-1, dp(15)));

        LinearLayout icons = row();
        icons.setGravity(Gravity.CENTER);
        List<NotificationBridgeService.NotificationItem> items = NotificationBridgeService.snapshot();
        int count = Math.min(5, items.size());
        for (int i = 0; i < 5; i++) {
            FrameLayout slot = new FrameLayout(context);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(30), 1f);
            p.setMargins(dp(2), 0, dp(2), 0);
            icons.addView(slot, p);
            if (i < count) {
                final NotificationBridgeService.NotificationItem item = items.get(i);
                ImageView appIcon = new ImageView(context);
                try { appIcon.setImageDrawable(context.getPackageManager().getApplicationIcon(item.packageName)); }
                catch (Exception ignored) { appIcon.setImageResource(android.R.drawable.sym_def_app_icon); }
                appIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);
                slot.addView(appIcon, new FrameLayout.LayoutParams(dp(25), dp(25), Gravity.CENTER));
                View dot = new View(context);
                dot.setBackground(round(RED, 5, 0));
                FrameLayout.LayoutParams dotParams = new FrameLayout.LayoutParams(dp(6), dp(6), Gravity.RIGHT | Gravity.TOP);
                slot.addView(dot, dotParams);
                slot.setContentDescription(item.title);
                slot.setFocusable(true);
                slot.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) { if (listener != null) listener.onOpenNotifications(); }
                });
            } else {
                TextView empty = circle("+", NOTHING_CARD, Color.rgb(110, 110, 114), 25, 12);
                slot.addView(empty, new FrameLayout.LayoutParams(dp(25), dp(25), Gravity.CENTER));
            }
        }
        card.addView(icons, new LinearLayout.LayoutParams(-1, dp(31)));
        card.setFocusable(true);
        card.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { if (listener != null) listener.onOpenNotifications(); }
        });
        add(root, card, -1, 56, 0, 0, 0, 0, 0);
    }

    private void addMediaBar(LinearLayout root, int background, int accent, int height, boolean mono) {
        LinearLayout bar = row();
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(9), dp(3), dp(8), dp(3));
        bar.setBackground(round(background, 12, 0));
        TextView title = new TextView(context);
        title.setText("נגן מוזיקה");
        title.setTextColor(Color.rgb(216, 230, 223));
        title.setTextSize(10);
        bar.addView(title, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView play = new TextView(context);
        play.setText("▶");
        play.setTextColor(accent);
        play.setTextSize(17);
        play.setGravity(Gravity.CENTER);
        bar.addView(play, new LinearLayout.LayoutParams(dp(24), -1));
        bar.setFocusable(true);
        bar.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { dispatch("media"); }
        });
        add(root, bar, -1, height, 0, 0, 4, 0, 0);
    }

    private void addMediaControl(LinearLayout row, String glyph, final int key) {
        TextView button = new TextView(context);
        button.setText(glyph);
        button.setTextColor(WHITE);
        button.setTextSize(15);
        button.setGravity(Gravity.CENTER);
        button.setFocusable(true);
        button.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                NotificationBridgeService.sendMediaKey(context, key);
            }
        });
        row.addView(button, new LinearLayout.LayoutParams(0, -1, 1f));
    }

    private View dualPill(String title, boolean active, String on, String off,
                          int activeColor, int inactiveColor, final Runnable action) {
        LinearLayout pill = new LinearLayout(context);
        pill.setOrientation(LinearLayout.VERTICAL);
        pill.setGravity(Gravity.CENTER_VERTICAL);
        pill.setPadding(dp(7), dp(3), dp(5), dp(3));
        pill.setBackground(round(active ? activeColor : inactiveColor, 21, 0));
        TextView heading = new TextView(context);
        heading.setText(title);
        heading.setTextSize(9);
        heading.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        heading.setTextColor(active ? FOREST : WHITE);
        TextView state = new TextView(context);
        state.setText(active ? on : off);
        state.setTextSize(8);
        state.setTextColor(active ? FOREST : WHITE);
        pill.addView(heading);
        pill.addView(state);
        pill.setFocusable(true);
        pill.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { action.run(); }
        });
        return pill;
    }

    private View topPill(String label, boolean active, boolean wifi, final Runnable action) {
        LinearLayout pill = new LinearLayout(context);
        pill.setOrientation(LinearLayout.HORIZONTAL);
        pill.setGravity(Gravity.CENTER_VERTICAL);
        pill.setPadding(dp(4), dp(3), dp(6), dp(3));
        pill.setBackground(round(Color.rgb(30, 41, 59), 20, 0));
        TextView icon = circle(wifi ? "◉" : "ᛒ", active ? WHITE : Color.rgb(51, 65, 85),
                active ? NAVY : WHITE, 28, 14);
        pill.addView(icon, new LinearLayout.LayoutParams(dp(28), dp(28)));
        TextView text = new TextView(context);
        text.setText(label);
        text.setTextColor(WHITE);
        text.setTextSize(9);
        text.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        LinearLayout.LayoutParams t = new LinearLayout.LayoutParams(-2, -2);
        t.leftMargin = dp(5);
        pill.addView(text, t);
        pill.setFocusable(true);
        pill.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { action.run(); }
        });
        return pill;
    }

    private SeekBar horizontalSlider(int fillColor, int trackColor, int value) {
        SeekBar seek = new SeekBar(context);
        seek.setMax(100);
        seek.setProgress(Math.max(0, Math.min(100, value)));
        seek.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        seek.setPadding(dp(7), 0, dp(7), 0);
        seek.setFocusable(true);
        seek.setFocusableInTouchMode(false);
        GradientDrawable track = new GradientDrawable();
        track.setColor(trackColor);
        track.setCornerRadius(dp(20));
        track.setSize(dp(1), dp(7));
        GradientDrawable fill = new GradientDrawable();
        fill.setColor(fillColor);
        fill.setCornerRadius(dp(20));
        fill.setSize(dp(1), dp(7));
        LayerDrawable layers = new LayerDrawable(new android.graphics.drawable.Drawable[] {
                track, new ClipDrawable(fill, Gravity.LEFT, ClipDrawable.HORIZONTAL)
        });
        layers.setId(0, android.R.id.background);
        layers.setId(1, android.R.id.progress);
        seek.setProgressDrawable(layers);
        GradientDrawable thumb = new GradientDrawable();
        thumb.setShape(GradientDrawable.OVAL);
        thumb.setColor(fillColor);
        thumb.setSize(dp(12), dp(12));
        seek.setThumb(thumb);
        seek.setThumbOffset(dp(6));
        return seek;
    }

    private TextView circle(String glyph, int background, int foreground, int size, int textSize) {
        TextView view = new TextView(context);
        view.setText(glyph);
        view.setTextColor(foreground);
        view.setTextSize(textSize);
        view.setGravity(Gravity.CENTER);
        view.setFocusable(true);
        view.setFocusableInTouchMode(false);
        view.setBackground(round(background, size / 2, 0));
        view.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override public void onFocusChange(View v, boolean focused) {
                v.setBackground(round(background, size / 2, focused ? BLUE : 0));
            }
        });
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private LinearLayout row() {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        return row;
    }

    private void add(LinearLayout parent, View child, int width, int height,
                     float weight, int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                width == 0 ? 0 : (width < 0 ? width : dp(width)),
                height == 0 ? 0 : (height < 0 ? height : dp(height)),
                weight);
        p.setMargins(dp(left), dp(top), dp(right), dp(bottom));
        parent.addView(child, p);
    }

    private GradientDrawable round(int color, int radius, int stroke) {
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE);
        shape.setColor(color);
        shape.setCornerRadius(dp(radius));
        if (stroke != 0) shape.setStroke(dp(1), stroke);
        return shape;
    }

    private void toggleWifi() {
        controller.toggleWifi();
        if (listener != null) listener.onAction("refresh");
    }

    private void toggleBluetooth() {
        controller.toggleBluetooth();
        if (listener != null) listener.onAction("refresh");
    }

    private void dispatch(String action) {
        if ("wifi".equals(action)) toggleWifi();
        else if ("bluetooth".equals(action)) toggleBluetooth();
        else if ("notifications".equals(action)) {
            if (listener != null) listener.onOpenNotifications();
        } else if (listener != null) listener.onAction(action);
    }

    private int dp(float value) {
        return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    /** D-pad friendly vertical Nothing-style slider, with a dotted active fill. */
    private static final class DotSlider extends View {
        interface Listener { void onValueChanged(int value); }
        private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint dots = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint focus = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final String label;
        private final boolean brightness;
        private int value;
        private Listener listener;

        DotSlider(Context context, int value, String label, boolean brightness) {
            super(context);
            this.value = Math.max(0, Math.min(100, value));
            this.label = label;
            this.brightness = brightness;
            setFocusable(true);
            setFocusableInTouchMode(false);
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            track.setColor(Color.rgb(36, 36, 39));
            fill.setColor(Color.rgb(229, 229, 234));
            dots.setColor(NOTHING_CARD);
            focus.setColor(RED);
            focus.setStyle(Paint.Style.STROKE);
            focus.setStrokeWidth(dp(context, 2));
            labelPaint.setColor(WHITE);
            labelPaint.setTextSize(dp(context, 8));
            labelPaint.setTypeface(Typeface.MONOSPACE);
        }

        void setListener(Listener listener) { this.listener = listener; }

        @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
            if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                setValue(value + 5); return true;
            }
            if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                setValue(value - 5); return true;
            }
            return super.onKeyDown(keyCode, event);
        }

        @Override public boolean onTouchEvent(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
                float pct = 1f - (event.getY() / Math.max(1f, getHeight()));
                setValue(Math.round(pct * 100f));
                return true;
            }
            return event.getAction() == MotionEvent.ACTION_UP || super.onTouchEvent(event);
        }

        private void setValue(int next) {
            value = Math.max(0, Math.min(100, next));
            if (listener != null) listener.onValueChanged(value);
            invalidate();
        }

        @Override protected void onDraw(Canvas canvas) {
            float w = getWidth(), h = getHeight();
            float inset = dp(getContext(), 2);
            RectF all = new RectF(inset, inset, w - inset, h - inset);
            float radius = w / 2f;
            canvas.drawRoundRect(all, radius, radius, track);
            float top = h * (1f - value / 100f);
            canvas.save();
            canvas.clipRect(0, top, w, h);
            canvas.drawRoundRect(all, radius, radius, fill);
            for (float y = top + dp(getContext(), 5); y < h - dp(getContext(), 12); y += dp(getContext(), 6)) {
                for (float x = dp(getContext(), 5); x < w - dp(getContext(), 3); x += dp(getContext(), 6)) {
                    canvas.drawCircle(x, y, dp(getContext(), 1), dots);
                }
            }
            canvas.restore();
            canvas.drawText(label, (w - labelPaint.measureText(label)) / 2f,
                    h - dp(getContext(), 4), labelPaint);
            if (isFocused()) canvas.drawRoundRect(all, radius, radius, focus);
        }

        private static int dp(Context context, float value) {
            return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
        }
    }
}
