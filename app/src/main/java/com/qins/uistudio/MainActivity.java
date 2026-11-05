package com.qins.uistudio;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** First runnable dashboard shell; all modules are clearly marked as foundations. */
public class MainActivity extends Activity {
    private int dp(float value) { return (int) (value * getResources().getDisplayMetrics().density + 0.5f); }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        showDashboard();
    }

    private void showDashboard() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(20), dp(28), dp(20), dp(24));
        page.setBackgroundColor(0xFFF3F5F8);
        scroll.addView(page);

        TextView title = new TextView(this);
        title.setText("QINS UI Studio");
        title.setTextSize(28);
        title.setTextColor(0xFF172033);
        page.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Modular interface customization • Android 4.4+");
        subtitle.setTextSize(14);
        subtitle.setTextColor(0xFF586579);
        LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(-1, -2);
        subParams.bottomMargin = dp(22);
        page.addView(subtitle, subParams);

        addSection(page, "Studio", "A safe, no-root workspace for personalizing the device.");
        addModule(page, "Home Studio", "Launcher concepts and shortcuts", "Foundation only");
        addModule(page, "Quick Panel Studio", "A companion panel; cannot replace protected System UI", "Capability-limited");
        addModule(page, "Edge Panel", "Optional edge launcher overlay (permission/device checks required)", "Planned");
        addModule(page, "Theme Studio", "Shared colors, typography and preview tokens", "Foundation only");
        addModule(page, "Lock Studio", "Lock-screen companion concepts; secure keyguard replacement is out of scope", "Capability-limited");
        addSection(page, "Project status", "This is an early scaffold. No feature is represented as complete until implemented and tested on the QINS1+.");
        setContentView(scroll);
    }

    private void addSection(LinearLayout parent, String heading, String description) {
        TextView h = new TextView(this);
        h.setText(heading); h.setTextSize(18); h.setTextColor(0xFF172033);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, -2);
        hp.topMargin = dp(14); hp.bottomMargin = dp(5); parent.addView(h, hp);
        TextView d = new TextView(this);
        d.setText(description); d.setTextSize(13); d.setTextColor(0xFF586579);
        LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(-1, -2);
        dp.bottomMargin = dp(10); parent.addView(d, dp);
    }

    private void addModule(LinearLayout parent, String name, String detail, String status) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.setBackgroundColor(0xFFFFFFFF);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
        cp.bottomMargin = dp(9); parent.addView(card, cp);
        TextView n = new TextView(this); n.setText(name); n.setTextSize(16); n.setTextColor(0xFF172033); card.addView(n);
        TextView desc = new TextView(this); desc.setText(detail); desc.setTextSize(13); desc.setTextColor(0xFF586579); card.addView(desc);
        TextView s = new TextView(this); s.setText(status); s.setTextSize(11); s.setTextColor(0xFF4263A5);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, -2); sp.topMargin = dp(6); card.addView(s, sp);
    }
}
