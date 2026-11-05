package com.qins.uistudio;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Keyboard-first home-layout editor. This milestone previews and saves settings inside Studio. */
public class HomeStudioActivity extends StudioActivity {
    private int columns = 3, rows = 2;
    private LinearLayout preview;
    private TextView status;
    private Button[] columnButtons = new Button[3], rowButtons = new Button[2];
    private int dp(float v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        columns = getPreferences(MODE_PRIVATE).getInt("home_columns", 3);
        rows = getPreferences(MODE_PRIVATE).getInt("home_rows", 2);
        buildScreen();
    }
    private void buildScreen() {
        final ThemeManager.Palette p = ThemeManager.getPalette(this);
        ScrollView scroll = new ScrollView(this); scroll.setFocusable(false);
        LinearLayout page = new LinearLayout(this); page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(18),dp(22),dp(18),dp(22)); scroll.addView(page);
        TextView title = new TextView(this); title.setText(R.string.home_studio);
        title.setTextSize(26); title.setTextColor(p.text); page.addView(title);
        TextView intro = new TextView(this); intro.setText(R.string.home_studio_intro);
        intro.setTextSize(14); intro.setTextColor(p.muted);
        LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(-1,-2); ip.bottomMargin=dp(12); page.addView(intro,ip);
        heading(page,R.string.home_columns_heading,p.text);
        LinearLayout cr=new LinearLayout(this); cr.setOrientation(LinearLayout.HORIZONTAL); page.addView(cr);
        for(int i=0;i<3;i++){ final int value=i+2; Button b=choice(String.valueOf(value),p); columnButtons[i]=b;
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1); lp.setMargins(dp(3),dp(3),dp(3),dp(3)); cr.addView(b,lp);
            b.setOnClickListener(new View.OnClickListener(){public void onClick(View v){columns=value;updatePreview();}});
        }
        heading(page,R.string.home_rows_heading,p.text);
        LinearLayout rr=new LinearLayout(this); rr.setOrientation(LinearLayout.HORIZONTAL); page.addView(rr);
        for(int i=0;i<2;i++){ final int value=i+2; Button b=choice(String.valueOf(value),p); rowButtons[i]=b;
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1); lp.setMargins(dp(3),dp(3),dp(3),dp(3)); rr.addView(b,lp);
            b.setOnClickListener(new View.OnClickListener(){public void onClick(View v){rows=value;updatePreview();}});
        }
        heading(page,R.string.home_preview_heading,p.text);
        preview=new LinearLayout(this); preview.setOrientation(LinearLayout.VERTICAL); preview.setBackgroundColor(p.surface); preview.setPadding(dp(6),dp(6),dp(6),dp(6));
        page.addView(preview,new LinearLayout.LayoutParams(-1,-2));
        status=new TextView(this); status.setTextSize(13); status.setTextColor(p.muted);
        LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,-2); sp.topMargin=dp(10); sp.bottomMargin=dp(10); page.addView(status,sp);
        Button save=choice(getString(R.string.home_save_layout),p); page.addView(save,new LinearLayout.LayoutParams(-1,-2));
        save.setOnClickListener(new View.OnClickListener(){public void onClick(View v){
            boolean ok=getPreferences(MODE_PRIVATE).edit().putInt("home_columns",columns).putInt("home_rows",rows).commit();
            status.setText(ok?R.string.home_saved:R.string.home_save_failed);
        }});
        Button back=choice(getString(R.string.menu_dashboard),p); LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,-2);
        bp.topMargin=dp(8); page.addView(back,bp); back.setOnClickListener(new View.OnClickListener(){public void onClick(View v){finish();}});
        setContentView(scroll); updatePreview(); columnButtons[0].requestFocus();
    }
    private void heading(LinearLayout page,int id,int color){TextView t=new TextView(this);t.setText(id);t.setTextSize(16);t.setTextColor(color);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(14);lp.bottomMargin=dp(5);page.addView(t,lp);}
    private Button choice(String label,ThemeManager.Palette p){Button b=new Button(this);b.setText(label);
        KeyboardSupport.makeFocusable(b,p.surface,p.accent,p.text,dp(8));return b;}
    private void updatePreview(){
        if(preview==null)return; ThemeManager.Palette p=ThemeManager.getPalette(this);
        preview.removeAllViews();
        String[] names={"Phone","Music","Files","Settings","Photos","Tools","Browser","Calendar","Notes","Radio","Clock","Apps"};
        for(int row=0;row<rows;row++){
            LinearLayout previewRow=new LinearLayout(this);
            previewRow.setOrientation(LinearLayout.HORIZONTAL);
            preview.addView(previewRow,new LinearLayout.LayoutParams(-1,-2));
            for(int column=0;column<columns;column++){
                int i=row*columns+column;
                TextView tile=new TextView(this);tile.setText(names[i%names.length]);tile.setTextColor(p.text);
                tile.setTextSize(12);tile.setGravity(17);android.graphics.drawable.GradientDrawable bg=new android.graphics.drawable.GradientDrawable();
                bg.setColor(p.background);bg.setCornerRadius(dp(6));bg.setStroke(dp(1),p.accent);tile.setBackgroundDrawable(bg);
                LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(58),1f);
                lp.setMargins(dp(3),dp(3),dp(3),dp(3));previewRow.addView(tile,lp);
            }
        }
        for(int i=0;i<3;i++)columnButtons[i].setText((i+2)+(columns==i+2?"  *":""));
        for(int i=0;i<2;i++)rowButtons[i].setText((i+2)+(rows==i+2?"  *":""));
        status.setText(getString(R.string.home_layout_summary,columns,rows));
    }
}