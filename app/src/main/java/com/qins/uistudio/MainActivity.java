package com.qins.uistudio;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Keyboard-only dashboard for devices without a touch screen. */
public class MainActivity extends StudioActivity {
    private ThemeManager.Palette palette;
    private boolean firstDisplay = true;
    private int dp(float value) { return (int) (value * getResources().getDisplayMetrics().density + 0.5f); }

    @Override public void onCreate(Bundle state) { super.onCreate(state); showDashboard(); }
    @Override protected void onResume() { super.onResume(); if (!firstDisplay) showDashboard(); firstDisplay = false; }

    private void showDashboard() {
        palette=ThemeManager.getPalette(this);
        ScrollView scroll=new ScrollView(this);scroll.setFocusable(false);scroll.setFocusableInTouchMode(false);
        LinearLayout page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(20),dp(28),dp(20),dp(24));page.setBackgroundColor(palette.background);scroll.addView(page);
        TextView title=new TextView(this);title.setText(R.string.app_name);title.setTextSize(28);title.setTextColor(palette.text);page.addView(title);
        Button menu=new Button(this);menu.setText(R.string.open_app_menu);KeyboardSupport.makeFocusable(menu);
        menu.setOnClickListener(new View.OnClickListener(){public void onClick(View v){startActivity(new Intent(MainActivity.this,AppMenuActivity.class));}});
        LinearLayout.LayoutParams mp=new LinearLayout.LayoutParams(-1,-2);mp.topMargin=dp(8);mp.bottomMargin=dp(12);page.addView(menu,mp);
        TextView subtitle=new TextView(this);subtitle.setText(R.string.dashboard_subtitle);subtitle.setTextSize(14);subtitle.setTextColor(palette.muted);
        LinearLayout.LayoutParams sub=new LinearLayout.LayoutParams(-1,-2);sub.bottomMargin=dp(22);page.addView(subtitle,sub);
        addSection(page,getString(R.string.studio_heading),getString(R.string.studio_description));
        addModule(page,R.string.home_studio,R.string.home_studio_desc,R.string.status_active,1);
        addModule(page,R.string.quick_panel,R.string.quick_panel_desc,R.string.status_limited,0);
        addModule(page,R.string.edge_panel,R.string.edge_panel_desc,R.string.status_planned,0);
        addModule(page,R.string.theme_studio,R.string.theme_studio_desc,R.string.status_active,2);
        addModule(page,R.string.lock_studio,R.string.lock_studio_desc,R.string.status_limited,0);
        addSection(page,getString(R.string.project_status),getString(R.string.project_status_desc));
        setContentView(scroll);
        if(page.getChildCount()>2)page.getChildAt(2).requestFocus();
    }
    private void addSection(LinearLayout parent,String heading,String description){
        TextView h=new TextView(this);h.setText(heading);h.setTextSize(18);h.setTextColor(palette.text);
        LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,-2);hp.topMargin=dp(14);hp.bottomMargin=dp(5);parent.addView(h,hp);
        TextView d=new TextView(this);d.setText(description);d.setTextSize(13);d.setTextColor(palette.muted);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(10);parent.addView(d,lp);
    }
    private void addModule(LinearLayout parent,int name,int detail,int status,int target){
        final int destination=target;final LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14),dp(12),dp(14),dp(12));LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);
        cp.bottomMargin=dp(9);parent.addView(card,cp);
        TextView n=new TextView(this);n.setText(name);n.setTextSize(16);n.setTextColor(palette.primary);card.addView(n);
        TextView d=new TextView(this);d.setText(detail);d.setTextSize(13);d.setTextColor(palette.muted);card.addView(d);
        TextView s=new TextView(this);s.setText(status);s.setTextSize(11);s.setTextColor(palette.accent);
        LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,-2);sp.topMargin=dp(6);card.addView(s,sp);
        KeyboardSupport.makeFocusable(card,palette.surface,palette.background,palette.accent,dp(10));
        card.setClickable(destination!=0);
        if(destination!=0)card.setOnClickListener(new View.OnClickListener(){public void onClick(View v){
            startActivity(new Intent(MainActivity.this,destination==1?HomeStudioActivity.class:ThemeStudioActivity.class));
        }});
    }
}