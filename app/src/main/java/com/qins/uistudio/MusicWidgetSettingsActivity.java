package com.qins.uistudio;

import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.app.AlertDialog;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Music widget editor. All controls are focusable for physical D-pad navigation. */
public class MusicWidgetSettingsActivity extends StudioActivity {
    private static final String PREFS = "qins_music_widget";
    private String selectedPackage = "";
    private String selectedLabel = "Automatic (active player)";
    private CheckBox artwork, title, artist, controls;
    private TextView playerSummary;
    private int dp(float v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        android.content.SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);
        selectedPackage = p.getString("player_package", "");
        artwork = null;
        buildPage();
    }

    private void buildPage() {
        final ThemeManager.Palette palette = ThemeManager.getPalette(this);
        ScrollView scroll = new ScrollView(this);
        scroll.setFocusable(false);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(18), dp(18), dp(18), dp(22));
        page.setBackgroundColor(palette.background);
        scroll.addView(page);
        TextView heading = new TextView(this);
        heading.setText("Music Widget · Editor");
        heading.setTextSize(23);
        heading.setTextColor(palette.text);
        page.addView(heading);
        TextView hint = new TextView(this);
        hint.setText("Use the physical arrow keys to move between settings, OK to select, and Back to return.");
        hint.setTextColor(palette.muted);
        hint.setTextSize(13);
        page.addView(hint);

        playerSummary = new TextView(this);
        playerSummary.setTextColor(palette.text);
        playerSummary.setTextSize(15);
        playerSummary.setPadding(0, dp(14), 0, dp(5));
        page.addView(playerSummary);
        addButton(page, "Choose default music app", new View.OnClickListener() {
            @Override public void onClick(View v) { showPlayerPicker(); }
        });

        android.content.SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);
        artwork = addCheck(page, "Show album artwork", p.getBoolean("show_artwork", true));
        title = addCheck(page, "Show song title", p.getBoolean("show_title", true));
        artist = addCheck(page, "Show artist / track details", p.getBoolean("show_artist", true));
        controls = addCheck(page, "Show playback controls", p.getBoolean("show_controls", true));

        addButton(page, "Preview changes", new View.OnClickListener() {
            @Override public void onClick(View v) { showPreview(); }
        });
        addButton(page, "Save Music Widget settings", new View.OnClickListener() {
            @Override public void onClick(View v) { saveSettings(); }
        });
        addButton(page, "Back", new View.OnClickListener() {
            @Override public void onClick(View v) { finish(); }
        });
        setContentView(scroll);
        refreshPlayerSummary();
        if (page.getChildCount() > 2) page.getChildAt(2).requestFocus();
    }

    private CheckBox addCheck(LinearLayout page, String label, boolean checked) {
        CheckBox box = new CheckBox(this);
        box.setText(label);
        box.setTextSize(15);
        box.setChecked(checked);
        box.setFocusable(true);
        box.setFocusableInTouchMode(false);
        page.addView(box, new LinearLayout.LayoutParams(-1, dp(48)));
        return box;
    }

    private void addButton(LinearLayout page, String label, View.OnClickListener action) {
        Button button = new Button(this);
        button.setText(label);
        button.setFocusable(true);
        button.setFocusableInTouchMode(false);
        button.setOnClickListener(action);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(5);
        page.addView(button, lp);
    }

    private void showPlayerPicker() {
        final ArrayList<String> packages = new ArrayList<String>();
        final ArrayList<String> labels = new ArrayList<String>();
        packages.add("");
        labels.add("Automatic (active player)");
        Intent query = new Intent(Intent.ACTION_MAIN);
        query.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> apps = getPackageManager().queryIntentActivities(query, 0);
        Collections.sort(apps, new Comparator<ResolveInfo>() {
            @Override public int compare(ResolveInfo a, ResolveInfo b) {
                return String.valueOf(a.loadLabel(getPackageManager())).compareToIgnoreCase(
                        String.valueOf(b.loadLabel(getPackageManager())));
            }
        });
        for (ResolveInfo info : apps) {
            String pkg = info.activityInfo == null ? "" : info.activityInfo.packageName;
            if (pkg.length() == 0 || packages.contains(pkg)) continue;
            packages.add(pkg);
            labels.add(String.valueOf(info.loadLabel(getPackageManager())));
        }
        int selected = packages.indexOf(selectedPackage);
        new AlertDialog.Builder(this).setTitle("Default music app")
                .setSingleChoiceItems(labels.toArray(new String[labels.size()]), Math.max(0, selected),
                        new android.content.DialogInterface.OnClickListener() {
                    @Override public void onClick(android.content.DialogInterface dialog, int which) {
                        selectedPackage = packages.get(which);
                        selectedLabel = labels.get(which);
                        dialog.dismiss();
                        refreshPlayerSummary();
                    }
                }).setNegativeButton(android.R.string.cancel, null).show();
    }

    private void refreshPlayerSummary() {
        if (selectedPackage.length() == 0) selectedLabel = "Automatic (active player)";
        else if (selectedLabel.equals("Automatic (active player)")) {
            try {
                android.content.pm.ApplicationInfo info = getPackageManager().getApplicationInfo(selectedPackage, 0);
                selectedLabel = String.valueOf(getPackageManager().getApplicationLabel(info));
            } catch (Exception ignored) { selectedLabel = selectedPackage; }
        }
        playerSummary.setText("Selected player: " + selectedLabel);
    }

    private void showPreview() {
        LinearLayout preview = new LinearLayout(this);
        preview.setOrientation(LinearLayout.HORIZONTAL);
        preview.setPadding(dp(12), dp(12), dp(12), dp(12));
        if (artwork.isChecked()) {
            TextView art = new TextView(this);
            art.setText("♪");
            art.setTextSize(30);
            art.setGravity(android.view.Gravity.CENTER);
            preview.addView(art, new LinearLayout.LayoutParams(dp(58), dp(58)));
        }
        LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        if (title.isChecked()) {
            TextView song = new TextView(this); song.setText("Song title preview"); song.setTextSize(16);
            text.addView(song);
        }
        if (artist.isChecked()) {
            TextView performer = new TextView(this); performer.setText("Artist / track details"); performer.setTextSize(12);
            text.addView(performer);
        }
        preview.addView(text, new LinearLayout.LayoutParams(0, -2, 1));
        if (controls.isChecked()) {
            TextView buttons = new TextView(this); buttons.setText("‹     ▶ / Ⅱ     ›"); buttons.setTextSize(16);
            buttons.setGravity(android.view.Gravity.CENTER);
            preview.addView(buttons, new LinearLayout.LayoutParams(-2, -1));
        }
        new AlertDialog.Builder(this).setTitle("Widget preview").setView(preview)
                .setPositiveButton(android.R.string.ok, null).show();
    }

    private void saveSettings() {
        boolean saved = getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putString("player_package", selectedPackage)
                .putBoolean("show_artwork", artwork.isChecked())
                .putBoolean("show_title", title.isChecked())
                .putBoolean("show_artist", artist.isChecked())
                .putBoolean("show_controls", controls.isChecked())
                .commit();
        if (saved) {
            sendBroadcast(new Intent(NotificationBridgeService.ACTION_CHANGED).setPackage(getPackageName()));
            android.widget.Toast.makeText(this, "Music Widget settings saved.", android.widget.Toast.LENGTH_SHORT).show();
            finish();
        } else {
            android.widget.Toast.makeText(this, "Could not save settings.", android.widget.Toast.LENGTH_LONG).show();
        }
    }
}
