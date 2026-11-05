package com.qins.uistudio;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Dashboard shell with a working entry point into Theme Studio. */
public class MainActivity extends Activity {
    private ThemeManager.Palette palette;
    private int dp(float value) { return (int) (value * getResources().getDisplayMetrics().density + 0.5f); }

    @Override public void onCreate(Bundle state) { super.onCreate(state); showDashboard(); }
    @Override protected void onResume() { super.onResume(); showDashboard(); }

    private void showDashboard() {
        palette = ThemeManager.getPalette(this);
        ScrollView scroll = new ScrollView(this);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(20), dp(28), dp(20), dp(24));
        page.setBackgroundColor(palette.background);
        scroll.addView(page);

        TextView title = new TextView(this);
        title.setText(R.string.app_name); title.setTextSize(28); title.setTextColor(palette.text); page.addView(title);
        TextView subtitle = new TextView(this);
        subtitle.setText(R.string.dashboard_subtitle); subtitle.setTextSize(14); subtitle.setTextColor(palette.muted);
        LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(-1, -2);
        subParams.bottomMargin = dp(22); page.addView(subtitle, subParams);

        addSection(page, getString(R.string.studio_heading), getString(R.string.studio_description));
        addModule(page, R.string.home_studio, R.string.home_studio_desc, R.string.status_foundation, false);
        addModule(page, R.string.quick_panel, R.string.quick_panel_desc, R.string.status_limited, false);
        addModule(page, R.string.edge_panel, R.string.edge_panel_desc, R.string.status_planned, false);
        addModule(page, R.string.theme_studio, R.string.theme_studio_desc, R.string.status_active, true);
        addModule(page, R.string.lock_studio, R.string.lock_studio_desc, R.string.status_limited, false);
        addSection(page, getString(R.string.project_status), getString(R.string.project_status_desc));
        setContentView(scroll);
    }

    private void addSection(LinearLayout parent, String heading, String description) {
        TextView h = new TextView(this); h.setText(heading); h.setTextSize(18); h.setTextColor(palette.text);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, -2);
        hp.topMargin = dp(14); hp.bottomMargin = dp(5); parent.addView(h, hp);
        TextView d = new TextView(this); d.setText(description); d.setTextSize(13); d.setTextColor(palette.muted);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = dp(10); parent.addView(d, params);
    }

    private void addModule(LinearLayout parent, int name, int detail, int status, boolean active) {
        final LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.setBackgroundColor(palette.surface);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
        cp.bottomMargin = dp(9); parent.addView(card, cp);
        TextView n = new TextView(this); n.setText(name); n.setTextSize(16); n.setTextColor(palette.primary); card.addView(n);
        TextView desc = new TextView(this); desc.setText(detail); desc.setTextSize(13); desc.setTextColor(palette.muted); card.addView(desc);
        TextView s = new TextView(this); s.setText(status); s.setTextSize(11); s.setTextColor(palette.accent);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, -2);
        sp.topMargin = dp(6); card.addView(s, sp);
        if (active) {
            card.setFocusable(true);
            card.setClickable(true);
            card.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    startActivity(new Intent(MainActivity.this, ThemeStudioActivity.class));
                }
            });
        }
    }
}
