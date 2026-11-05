package com.qins.uistudio;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.content.Intent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

/** Displays the last uncaught crash report without requiring ADB or root. */
public class CrashReportActivity extends Activity {
    private int dp(float v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(14), dp(14), dp(14), dp(14));
        TextView title = new TextView(this);
        title.setText("QINS diagnostics");
        title.setTextSize(22);
        root.addView(title);
        ScrollView scroll = new ScrollView(this);
        TextView body = new TextView(this);
        body.setTextSize(12);
        body.setText(readReport());
        body.setTextIsSelectable(true);
        scroll.addView(body);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        Button share = new Button(this);
        share.setText("Share crash report");
        share.setOnClickListener(v -> {
            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("text/plain");
            send.putExtra(Intent.EXTRA_TEXT, body.getText().toString());
            startActivity(Intent.createChooser(send, "Share diagnostics"));
        });
        root.addView(share);
        Button close = new Button(this);
        close.setText("Back");
        close.setOnClickListener(v -> finish());
        root.addView(close);
        setContentView(root);
        share.requestFocus();
    }

    private String readReport() {
        File file = new File(getFilesDir(), "last-crash.txt");
        if (!file.exists()) return "No saved crash report yet.\n\n"
                + "Open Studio once to reproduce the problem, then return here.";
        StringBuilder text = new StringBuilder();
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream(file), "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null) text.append(line).append('\n');
            reader.close();
            return text.toString();
        } catch (Exception error) {
            return "Could not read report: " + error.toString();
        }
    }
}