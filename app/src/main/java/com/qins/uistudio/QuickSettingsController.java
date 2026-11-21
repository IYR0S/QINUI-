package com.qins.uistudio;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.media.AudioManager;
import android.net.wifi.WifiManager;
import android.provider.Settings;

/**
 * Centralized API-19-safe Quick Settings system bridge.
 *
 * UI state is always read from Android; callers must refresh after the
 * asynchronous Wi-Fi/Bluetooth state broadcasts rather than optimistically
 * changing a tile's appearance.
 */
public final class QuickSettingsController {
    public interface Listener {
        void onSystemStateChanged();
        void onOperationFailed(String operation);
    }

    private final Context app;
    private final WifiManager wifi;
    private final AudioManager audio;
    private final Listener listener;

    public QuickSettingsController(Context context, Listener listener) {
        app = context.getApplicationContext();
        wifi = (WifiManager) app.getSystemService(Context.WIFI_SERVICE);
        audio = (AudioManager) app.getSystemService(Context.AUDIO_SERVICE);
        this.listener = listener;
    }

    public boolean isWifiAvailable() { return wifi != null; }
    public boolean isWifiEnabled() {
        try { return wifi != null && wifi.isWifiEnabled(); }
        catch (RuntimeException e) { return false; }
    }

    public boolean toggleWifi() {
        try {
            if (wifi == null || !wifi.setWifiEnabled(!wifi.isWifiEnabled())) {
                fail("Wi-Fi");
                return false;
            }
            return true;
        } catch (SecurityException e) {
            fail("Wi-Fi permission");
            return false;
        } catch (RuntimeException e) {
            fail("Wi-Fi");
            return false;
        }
    }

    public boolean isBluetoothAvailable() { return BluetoothAdapter.getDefaultAdapter() != null; }
    public boolean isBluetoothEnabled() {
        try {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            return adapter != null && adapter.isEnabled();
        } catch (RuntimeException e) { return false; }
    }

    public boolean toggleBluetooth() {
        try {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter == null) { fail("Bluetooth"); return false; }
            boolean accepted = adapter.isEnabled() ? adapter.disable() : adapter.enable();
            if (!accepted) fail("Bluetooth");
            return accepted;
        } catch (SecurityException e) {
            fail("Bluetooth permission");
            return false;
        } catch (RuntimeException e) {
            fail("Bluetooth");
            return false;
        }
    }

    public int getBrightnessPercent() {
        int value = 128;
        try { value = Settings.System.getInt(app.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS); }
        catch (Exception ignored) { }
        return clamp(Math.round(value * 100f / 255f), 1, 100);
    }

    public boolean setBrightnessPercent(int percent) {
        int safe = clamp(percent, 1, 100);
        int raw = Math.max(1, Math.min(255, Math.round(safe * 255f / 100f)));
        try {
            Settings.System.putInt(app.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
            boolean written = Settings.System.putInt(app.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS, raw);
            if (!written) fail("Brightness");
            return written;
        } catch (SecurityException e) {
            fail("Brightness permission");
            return false;
        } catch (RuntimeException e) {
            fail("Brightness");
            return false;
        }
    }

    public int getVolumePercent() {
        if (audio == null) return 0;
        int max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        if (max <= 0) return 0;
        return clamp(Math.round(audio.getStreamVolume(AudioManager.STREAM_MUSIC) * 100f / max), 0, 100);
    }

    public boolean setVolumePercent(int percent) {
        if (audio == null) { fail("Volume"); return false; }
        try {
            int max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            int target = Math.round(clamp(percent, 0, 100) * max / 100f);
            audio.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0);
            return true;
        } catch (SecurityException e) {
            fail("Volume permission");
            return false;
        } catch (RuntimeException e) {
            fail("Volume");
            return false;
        }
    }

    public int getRingerMode() {
        return audio == null ? AudioManager.RINGER_MODE_NORMAL : audio.getRingerMode();
    }

    public boolean cycleRingerMode() {
        if (audio == null) { fail("Sound"); return false; }
        try {
            int current = audio.getRingerMode();
            int next = current == AudioManager.RINGER_MODE_NORMAL
                    ? AudioManager.RINGER_MODE_VIBRATE
                    : (current == AudioManager.RINGER_MODE_VIBRATE
                    ? AudioManager.RINGER_MODE_SILENT : AudioManager.RINGER_MODE_NORMAL);
            audio.setRingerMode(next);
            return true;
        } catch (SecurityException e) {
            fail("Sound permission");
            return false;
        } catch (RuntimeException e) {
            fail("Sound");
            return false;
        }
    }

    private void fail(String operation) {
        if (listener != null) listener.onOperationFailed(operation);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
