package com.qins.uistudio;

import android.app.Application;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

/** Captures uncaught app crashes for on-device diagnostics (API 19 compatible). */
public class QinsApplication extends Application {
    private static final String TAG = "QINS-Crash";
    private Thread.UncaughtExceptionHandler previousHandler;

    @Override public void onCreate() {
        super.onCreate();
        previousHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override public void uncaughtException(Thread thread, Throwable error) {
                StringWriter buffer = new StringWriter();
                error.printStackTrace(new PrintWriter(buffer));
                String report = "QINS UI Studio crash\n"
                        + "Thread: " + thread.getName() + "\n"
                        + "Time: " + System.currentTimeMillis() + "\n\n"
                        + buffer.toString();
                Log.e(TAG, report);
                try {
                    File file = new File(getFilesDir(), "last-crash.txt");
                    FileOutputStream stream = new FileOutputStream(file, false);
                    stream.write(report.getBytes("UTF-8"));
                    stream.close();
                } catch (Throwable writeError) {
                    Log.e(TAG, "Could not save crash report", writeError);
                }
                if (previousHandler != null) previousHandler.uncaughtException(thread, error);
            }
        });
        Log.i(TAG, "Crash logger initialized");
    }
}