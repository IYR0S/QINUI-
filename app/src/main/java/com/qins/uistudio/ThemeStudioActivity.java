package com.qins.uistudio;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Functional first Theme Studio: preview, persist and apply an app-wide palette. */
public class ThemeStudioActivity extends Activity {
    private ThemeManager.Palette selected;
    private LinearLayout page;
    private TextView previewTitle;
    private TextView previewBody;
    private TextView status;

    private int dp(float value) { return (int) (value * getResources().getDisplayMetrics().density + 0.5f); }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        selected = ThemeManager.getPalette(this);
        buildScreen();
        renderPreview();
    }

    private void buildScreen() {
        ScrollView scroll = new ScrollView(this);
        page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(20), dp(24), dp(20), dp(24));
        scroll.addView(page);

        TextView title = new TextView(this);
        title.setText(R.string.theme_studio);
        title.setTextSize(26);
        title.setTextColor(selected.text);
        page.addView(title);

        TextView intro = new TextView(this);
        intro.setText(R.string.theme_intro);
        intro.setTextSize(14);
        intro.setTextColor(selected.muted);
        LinearLayout.LayoutParams introParams = new LinearLayout.LayoutParams(-1, -2);
        introParams.bottomMargin = dp(18);
        page.addView(intro, introParams);

        TextView choices = new TextView(this);
        choices.setText(R.string.theme_choose);
        choices.setTextSize(17);
        choices.setTextColor(selected.text);
        page.addView(choices);

        addChoice("ocean", R.string.theme_ocean);
        addChoice("midnight", R.string.theme_midnight);
        addChoice("forest", R.string.theme_forest);

        TextView previewHeading = new TextView(this);
        previewHeading.setText(R.string.theme_preview);
        previewHeading.setTextSize(17);
        previewHeading.setTextColor(selected.text);
        LinearLayout.LayoutParams ph = new LinearLayout.LayoutParams(-1, -2);
        ph.topMargin = dp(18);
        ph.bottomMargin = dp(8);
        page.addView(previewHeading, ph);

        LinearLayout preview = new LinearLayout(this);
        preview.setOrientation(LinearLayout.VERTICAL);
        preview.setPadding(dp(16), dp(16), dp(16), dp(16));
        page.addView(preview, new LinearLayout.LayoutParams(-1, -2));
        previewTitle = new TextView(this);
        previewTitle.setText(R.string.preview_card_title);
        previewTitle.setTextSize(18);
        preview.addView(previewTitle);
        previewBody = new TextView(this);
        previewBody.setText(R.string.preview_card_body);
        previewBody.setTextSize(13);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, -2);
        bp.topMargin = dp(6);
        preview.addView(previewBody, bp);

        Button apply = new Button(this);
        apply.setText(R.string.theme_apply);
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(-1, -2);
        ap.topMargin = dp(16);
        page.addView(apply, ap);
        apply.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                boolean saved = ThemeManager.apply(ThemeStudioActivity.this, selected.id);
                status.setText(saved ? R.string.theme_saved : R.string.theme_save_failed);
                renderPreview();
            }
        });

        status = new TextView(this);
        status.setTextSize(13);
        status.setTextColor(selected.muted);
        LinearLayout.LayoutParams st = new LinearLayout.LayoutParams(-1, -2);
        st.topMargin = dp(8);
        page.addView(status, st);

        TextView note = new TextView(this);
        note.setText(R.string.theme_scope_note);
        note.setTextSize(12);
        note.setTextColor(selected.muted);
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(-1, -2);
        np.topMargin = dp(20);
        page.addView(note, np);
        setContentView(scroll);
    }

    private void addChoice(final String id, int labelId) {
        Button choice = new Button(this);
        choice.setText(labelId);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(6);
        page.addView(choice, params);
        choice.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                for (ThemeManager.Palette palette : ThemeManager.getPalettes()) {
                    if (palette.id.equals(id)) selected = palette;
                }
                status.setText("");
                renderPreview();
            }
        });
    }

    private void renderPreview() {
        page.setBackgroundColor(selected.background);
        previewTitle.setTextColor(selected.text);
        previewBody.setTextColor(selected.muted);
        View preview = (View) previewTitle.getParent();
        preview.setBackgroundColor(selected.surface);
        previewTitle.setTextColor(selected.primary);
        status.setTextColor(selected.muted);
    }
}
