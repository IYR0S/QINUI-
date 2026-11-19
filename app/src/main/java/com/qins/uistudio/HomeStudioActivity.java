package com.qins.uistudio;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Keyboard-first home-layout editor. */
public class HomeStudioActivity extends StudioActivity {
    private int columns = 3, rows = 2;
    private LinearLayout preview;
    private TextView status;
    private Button[] columnButtons = new Button[3], rowButtons = new Button[2];

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        columns = getPreferences(MODE_PRIVATE).getInt("home_columns", 3);
        rows = getPreferences(MODE_PRIVATE).getInt("home_rows", 2);
        buildScreen();
    }

    private void buildScreen() {
        final ThemeManager.Appearance a = ThemeManager.getAppearance(this);
        ScrollView scroll = new ScrollView(this);
        scroll.setFocusable(false);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(18), dp(22), dp(18), dp(22));
        page.setBackgroundColor(a.background);
        scroll.addView(page);

        TextView title = new TextView(this);
        title.setText(R.string.home_studio);
        title.setTextSize(26);
        title.setTextColor(a.text);
        page.addView(title);

        TextView intro = new TextView(this);
        intro.setText(R.string.home_studio_intro);
        intro.setTextSize(14);
        intro.setTextColor(a.muted);
        LinearLayout.LayoutParams introParams = new LinearLayout.LayoutParams(-1, -2);
        introParams.bottomMargin = dp(12);
        page.addView(intro, introParams);

        addHeading(page, R.string.home_columns_heading, a.text);
        LinearLayout columnRow = new LinearLayout(this);
        columnRow.setOrientation(LinearLayout.HORIZONTAL);
        page.addView(columnRow);
        for (int i = 0; i < 3; i++) {
            final int value = i + 2;
            Button choice = choice(String.valueOf(value), a);
            columnButtons[i] = choice;
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(42), 1f);
            lp.setMargins(dp(3), dp(3), dp(3), dp(3));
            columnRow.addView(choice, lp);
            choice.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { columns = value; updatePreview(); }
            });
        }

        addHeading(page, R.string.home_rows_heading, a.text);
        LinearLayout rowRow = new LinearLayout(this);
        rowRow.setOrientation(LinearLayout.HORIZONTAL);
        page.addView(rowRow);
        for (int i = 0; i < 2; i++) {
            final int value = i + 2;
            Button choice = choice(String.valueOf(value), a);
            rowButtons[i] = choice;
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(42), 1f);
            lp.setMargins(dp(3), dp(3), dp(3), dp(3));
            rowRow.addView(choice, lp);
            choice.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { rows = value; updatePreview(); }
            });
        }

        addHeading(page, R.string.home_preview_heading, a.text);
        preview = new LinearLayout(this);
        preview.setOrientation(LinearLayout.VERTICAL);
        preview.setPadding(dp(8), dp(8), dp(8), dp(8));
        GradientDrawable previewSurface = new GradientDrawable();
        previewSurface.setColor(a.surface);
        previewSurface.setCornerRadius(dp(a.cardCornerDp));
        previewSurface.setStroke(dp(1), a.border);
        preview.setBackgroundDrawable(previewSurface);
        preview.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        page.addView(preview, new LinearLayout.LayoutParams(-1, -2));

        status = new TextView(this);
        status.setTextSize(13);
        status.setTextColor(a.muted);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(-1, -2);
        statusParams.topMargin = dp(10);
        statusParams.bottomMargin = dp(10);
        page.addView(status, statusParams);

        Button save = choice(getString(R.string.home_save_layout), a);
        page.addView(save, new LinearLayout.LayoutParams(-1, dp(44)));
        save.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                boolean ok = getPreferences(MODE_PRIVATE).edit()
                        .putInt("home_columns", columns)
                        .putInt("home_rows", rows).commit();
                status.setText(ok ? R.string.home_saved : R.string.home_save_failed);
            }
        });

        Button back = choice(getString(R.string.menu_dashboard), a);
        LinearLayout.LayoutParams backParams = new LinearLayout.LayoutParams(-1, dp(44));
        backParams.topMargin = dp(8);
        page.addView(back, backParams);
        back.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { finish(); }
        });

        setContentView(scroll);
        updatePreview();
        columnButtons[Math.max(0, Math.min(2, columns - 2))].requestFocus();
    }

    private void addHeading(LinearLayout page, int textId, int color) {
        TextView heading = new TextView(this);
        heading.setText(textId);
        heading.setTextSize(16);
        heading.setTextColor(color);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(14);
        lp.bottomMargin = dp(5);
        page.addView(heading, lp);
    }

    private Button choice(String label, ThemeManager.Appearance a) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        KeyboardSupport.makeFocusable(button, a.surface, a.accent, a.text, dp(a.tileCornerDp));
        return button;
    }

    private void updatePreview() {
        if (preview == null) return;
        ThemeManager.Appearance a = ThemeManager.getAppearance(this);
        preview.removeAllViews();
        String[] names = {"Phone", "Music", "Files", "Settings", "Photos", "Tools",
                "Browser", "Calendar", "Notes", "Radio", "Clock", "Apps"};
        String[] glyphs = {"home", "sound", "folder", "settings", "palette", "apps",
                "wifi", "done", "apps", "sound", "brightness", "apps"};

        for (int row = 0; row < rows; row++) {
            LinearLayout previewRow = new LinearLayout(this);
            previewRow.setOrientation(LinearLayout.HORIZONTAL);
            previewRow.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2);
            preview.addView(previewRow, rowParams);

            for (int column = 0; column < columns; column++) {
                final int index = row * columns + column;
                LinearLayout tile = new LinearLayout(this);
                tile.setOrientation(LinearLayout.VERTICAL);
                tile.setGravity(Gravity.CENTER);
                tile.setPadding(dp(2), dp(5), dp(2), dp(5));
                tile.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

                QinsGlyphView glyph = new QinsGlyphView(this, glyphs[index % glyphs.length], a.accent);
                glyph.setVisualStyle(a.id);
                tile.addView(glyph, new LinearLayout.LayoutParams(dp(23), dp(23)));

                TextView label = new TextView(this);
                label.setText(names[index % names.length]);
                label.setTextColor(a.text);
                label.setTextSize(9);
                label.setGravity(Gravity.CENTER);
                label.setSingleLine(true);
                tile.addView(label, new LinearLayout.LayoutParams(-1, -2));

                GradientDrawable tileSurface = new GradientDrawable();
                tileSurface.setColor(a.tile);
                tileSurface.setCornerRadius(dp(a.tileCornerDp));
                tileSurface.setStroke(dp(1), a.border);
                tile.setBackgroundDrawable(tileSurface);

                LinearLayout.LayoutParams tileParams = new LinearLayout.LayoutParams(0, dp(66), 1f);
                tileParams.setMargins(dp(3), dp(3), dp(3), dp(3));
                previewRow.addView(tile, tileParams);
            }
        }

        for (int i = 0; i < 3; i++) {
            columnButtons[i].setText((i + 2) + (columns == i + 2 ? "  ✓" : ""));
            columnButtons[i].setTextColor(columns == i + 2 ? 0xFFFFFFFF : a.text);
        }
        for (int i = 0; i < 2; i++) {
            rowButtons[i].setText((i + 2) + (rows == i + 2 ? "  ✓" : ""));
            rowButtons[i].setTextColor(rows == i + 2 ? 0xFFFFFFFF : a.text);
        }
        status.setText(getString(R.string.home_layout_summary, columns, rows));
    }
}
