package com.qins.uistudio;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Minimal API-19-safe Studio entry screen.
 * Keeps diagnostics visible on-device instead of silently crashing.
 */
public class MainActivity extends Activity {
    private static final String TAG = "QINS-Studio";

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        try {
            setTheme(R.style.AppTheme);
            showDashboard();
        } catch (Throwable error) {
            Log.e(TAG, "Dashboard creation failed", error);
            showFailure(error);
        }
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void showDashboard() {
        final LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(16), dp(18), dp(16), dp(18));
        page.setFocusable(false);

        TextView title = new TextView(this);
        title.setText("QINS UI Studio");
        title.setTextSize(24);
        page.addView(title, new LinearLayout.LayoutParams(-1, -2));

        TextView hint = new TextView(this);
        hint.setText("Use the physical arrow keys and OK / Enter.");
        hint.setTextSize(14);
        LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(-1, -2);
        hintParams.bottomMargin = dp(14);
        page.addView(hint, hintParams);

        addButton(page, "Home Studio", HomeStudioActivity.class);
        addButton(page, "Theme Studio", ThemeStudioActivity.class);
        addButton(page, "App Menu", AppMenuActivity.class);

        setContentView(page);
        if (page.getChildCount() > 2) page.getChildAt(2).requestFocus();
    }

    private void addButton(LinearLayout page, String label, final Class<?> target) {
        Button button = new Button(this);
        button.setText(label);
        button.setFocusable(true);
        button.setFocusableInTouchMode(false);
        button.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                try {
                    startActivity(new Intent(MainActivity.this, target));
                } catch (Throwable error) {
                    Log.e(TAG, "Could not open " + target.getSimpleName(), error);
                    showFailure(error);
                }
            }
        });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = dp(8);
        page.addView(button, params);
    }

    private void showFailure(Throwable error) {
        String detail = error.getClass().getName() + ": " + error.getMessage();
        TextView message = new TextView(this);
        message.setText("QINS Studio error\n\n" + detail
                + "\n\nSend this message to the developer.");
        message.setTextSize(16);
        message.setPadding(dp(16), dp(16), dp(16), dp(16));
        try {
            setContentView(message);
        } catch (Throwable ignored) {
            android.widget.Toast.makeText(this, detail, android.widget.Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            try {
                startActivity(new Intent(this, AppMenuActivity.class));
            } catch (Throwable error) {
                Log.e(TAG, "Could not open app menu", error);
                showFailure(error);
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
