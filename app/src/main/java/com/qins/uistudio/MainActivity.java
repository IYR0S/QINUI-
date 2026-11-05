package com.qins.uistudio;

import android.app.Activity;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** First runnable dashboard shell; modules are explicitly marked by maturity. */
public class MainActivity extends Activity {
    private int dp(float value) { return (int) (value * getResources().getDisplayMetrics().density + 0.5f); }
    @Override public void onCreate(Bundle state) { super.onCreate(state); showDashboard(); }
    private void showDashboard() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL); page.setPadding(dp(20), dp(28), dp(20), dp(24)); page.setBackgroundColor(0xFFF3F5F8); scroll.addView(page);
        TextView title = new TextView(this); title.setText(R.string.app_name); title.setTextSize(28); title.setTextColor(0xFF172033); page.addView(title);
        TextView subtitle = new TextView(this); subtitle.setText(R.string.dashboard_subtitle); subtitle.setTextSize(14); subtitle.setTextColor(0xFF586579);
        LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(-1, -2); subParams.bottomMargin = dp(22); page.addView(subtitle, subParams);
        addSection(page, getString(R.string.studio_heading), getString(R.string.studio_description));
        addModule(page, R.string.home_studio, R.string.home_studio_desc, R.string.status_foundation);
        addModule(page, R.string.quick_panel, R.string.quick_panel_desc, R.string.status_limited);
        addModule(page, R.string.edge_panel, R.string.edge_panel_desc, R.string.status_planned);
        addModule(page, R.string.theme_studio, R.string.theme_studio_desc, R.string.status_foundation);
        addModule(page, R.string.lock_studio, R.string.lock_studio_desc, R.string.status_limited);
        addSection(page, getString(R.string.project_status), getString(R.string.project_status_desc)); setContentView(scroll);
    }
    private void addSection(LinearLayout parent, String heading, String description) {
        TextView h = new TextView(this); h.setText(heading); h.setTextSize(18); h.setTextColor(0xFF172033);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, -2); hp.topMargin = dp(14); hp.bottomMargin = dp(5); parent.addView(h, hp);
        TextView d = new TextView(this); d.setText(description); d.setTextSize(13); d.setTextColor(0xFF586579);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2); params.bottomMargin = dp(10); parent.addView(d, params);
    }
    private void addModule(LinearLayout parent, int name, int detail, int status) {
        LinearLayout card = new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL); card.setPadding(dp(14), dp(12), dp(14), dp(12)); card.setBackgroundColor(0xFFFFFFFF);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2); cp.bottomMargin = dp(9); parent.addView(card, cp);
        TextView n = new TextView(this); n.setText(name); n.setTextSize(16); n.setTextColor(0xFF172033); card.addView(n);
        TextView desc = new TextView(this); desc.setText(detail); desc.setTextSize(13); desc.setTextColor(0xFF586579); card.addView(desc);
        TextView s = new TextView(this); s.setText(status); s.setTextSize(11); s.setTextColor(0xFF4263A5);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, -2); sp.topMargin = dp(6); card.addView(s, sp);
    }
}
