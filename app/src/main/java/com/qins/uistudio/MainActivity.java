package com.qins.uistudio;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Standalone Studio dashboard. Kept independent from the shared Activity
 * subclass so opening Studio from the HOME launcher has a minimal lifecycle.
 */
public class MainActivity extends Activity {
    private ThemeManager.Palette palette;
    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override public void onCreate(Bundle state) {
        setTheme(R.style.AppTheme);
        super.onCreate(state);
        showDashboard();
    }

    private void showDashboard() {
        palette = ThemeManager.getPalette(this);
        ScrollView scroll = new ScrollView(this);
        scroll.setFocusable(false);
        scroll.setFocusableInTouchMode(false);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(20), dp(28), dp(20), dp(24));
        page.setBackgroundColor(palette.background);
        scroll.addView(page);

        TextView title = new TextView(this);
        title.setText(R.string.app_name);
        title.setTextSize(28);
        title.setTextColor(palette.text);
        page.addView(title);

        Button menu = new Button(this);
        menu.setText(R.string.open_app_menu);
        KeyboardSupport.makeFocusable(menu);
        menu.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, AppMenuActivity.class));
            }
        });
        LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(-1, -2);
        mp.topMargin = dp(8);
        mp.bottomMargin = dp(12);
        page.addView(menu, mp);

        TextView subtitle = new TextView(this);
        subtitle.setText(R.string.dashboard_subtitle);
        subtitle.setTextSize(14);
        subtitle.setTextColor(palette.muted);
        LinearLayout.LayoutParams sub = new LinearLayout.LayoutParams(-1, -2);
        sub.bottomMargin = dp(22);
        page.addView(subtitle, sub);

        addSection(page, getString(R.string.studio_heading), getString(R.string.studio_description));
        addModule(page, R.string.home_studio, R.string.home_studio_desc, R.string.status_active, 1);
        addModule(page, R.string.quick_panel, R.string.quick_panel_desc, R.string.status_limited, 0);
        addModule(page, R.string.edge_panel, R.string.edge_panel_desc, R.string.status_planned, 0);
        addModule(page, R.string.theme_studio, R.string.theme_studio_desc, R.string.status_active, 2);
        addModule(page, R.string.lock_studio, R.string.lock_studio_desc, R.string.status_limited, 0);
        addSection(page, getString(R.string.project_status), getString(R.string.project_status_desc));

        setContentView(scroll);
        if (menu.isFocusable()) menu.requestFocus();
    }

    private void addSection(LinearLayout parent, String heading, String description) {
        TextView h = new TextView(this);
        h.setText(heading);
        h.setTextSize(18);
        h.setTextColor(palette.text);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, -2);
        hp.topMargin = dp(14);
        hp.bottomMargin = dp(5);
        parent.addView(h, hp);

        TextView d = new TextView(this);
        d.setText(description);
        d.setTextSize(13);
        d.setTextColor(palette.muted);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = dp(10);
        parent.addView(d, lp);
    }

    private void addModule(LinearLayout parent, int name, int detail, int status, int target) {
        final int destination = target;
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
        cp.bottomMargin = dp(9);
        parent.addView(card, cp);

        TextView n = new TextView(this);
        n.setText(name);
        n.setTextSize(16);
        n.setTextColor(palette.primary);
        card.addView(n);

        TextView d = new TextView(this);
        d.setText(detail);
        d.setTextSize(13);
        d.setTextColor(palette.muted);
        card.addView(d);

        TextView s = new TextView(this);
        s.setText(status);
        s.setTextSize(11);
        s.setTextColor(palette.accent);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, -2);
        sp.topMargin = dp(6);
        card.addView(s, sp);

        if (destination != 0) {
            KeyboardSupport.makeFocusable(card, palette.surface, palette.accent,
                    palette.text, dp(10));
            card.setClickable(true);
            card.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    Class<?> screen = destination == 1 ? HomeStudioActivity.class : ThemeStudioActivity.class;
                    startActivity(new Intent(MainActivity.this, screen));
                }
            });
        }
    }

    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            startActivity(new Intent(this, AppMenuActivity.class));
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
