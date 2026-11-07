package com.qins.uistudio;

import android.app.Notification;
import android.app.PendingIntent;
import android.media.AudioManager;
import android.net.Uri;
import android.view.KeyEvent;
import android.graphics.Bitmap;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Read-only notification bridge for the QINS Quick Settings notification page.
 * Requires the user to grant Notification Access explicitly in Android Settings.
 */
public final class NotificationBridgeService extends NotificationListenerService {
    public static final String ACTION_CHANGED = "com.qins.uistudio.NOTIFICATIONS_CHANGED";
    private static final Object LOCK = new Object();
    private static final ArrayList<NotificationItem> CACHE = new ArrayList<NotificationItem>();

    public static final class NotificationItem {
        public final String key;
        public final String packageName;
        public final String title;
        public final String text;
        public final long postedAt;
        public final boolean missedCall, media;
        public final Bitmap artwork;
        public final String[] actionTitles;
        public final PendingIntent[] actions;
        NotificationItem(String key, String packageName, String title, String text, long postedAt,
                         boolean missedCall, boolean media, Bitmap artwork, String[] actionTitles, PendingIntent[] actions) {
            this.key = key; this.packageName = packageName; this.title = title; this.text = text;
            this.postedAt = postedAt; this.missedCall = missedCall; this.media = media;
            this.artwork = artwork; this.actionTitles = actionTitles; this.actions = actions;
        }
    }

    public static List<NotificationItem> snapshot() {
        synchronized (LOCK) {
            return new ArrayList<NotificationItem>(CACHE);
        }
    }

    public static boolean isAccessEnabled(Context context) {
        try {
            String enabled = Settings.Secure.getString(context.getContentResolver(),
                    "enabled_notification_listeners");
            if (enabled == null) return false;
            return enabled.contains(context.getPackageName() + "/") &&
                    enabled.contains("NotificationBridgeService");
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    @Override public IBinder onBind(Intent intent) {
        IBinder binder = super.onBind(intent);
        // API 19 does not expose onListenerConnected(); wait briefly after bind
        // before requesting the initial active-notification snapshot.
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override public void run() { refreshActiveNotifications(); }
        }, 800L);
        return binder;
    }

    @Override public void onListenerConnected() {
        super.onListenerConnected();
        refreshActiveNotifications();
    }

    @Override public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null) return;
        NotificationItem item = toItem(sbn);
        synchronized (LOCK) {
            removeByKey(item.key);
            CACHE.add(item);
            sortCache();
        }
        notifyChanged();
    }

    @Override public void onNotificationRemoved(StatusBarNotification sbn) {
        if (sbn == null) return;
        synchronized (LOCK) {
            removeByKey(makeKey(sbn));
        }
        notifyChanged();
    }

    private void refreshActiveNotifications() {
        try {
            StatusBarNotification[] active = getActiveNotifications();
            synchronized (LOCK) {
                CACHE.clear();
                if (active != null) {
                    for (StatusBarNotification sbn : active) {
                        if (sbn != null) CACHE.add(toItem(sbn));
                    }
                }
                sortCache();
            }
        } catch (RuntimeException ignored) { }
        notifyChanged();
    }

    /**
     * Prefer an actual media filename when the player includes a file path or URI
     * in its notification extras. Android 4.4 does not expose a universal
     * currently-playing file-path API, so callers may not provide one.
     */
    private static String fileNameFromExtras(Bundle extras) {
        if (extras == null) return null;
        String[] keys = new String[] {
                "filename", "file_name", "fileName", "file_path", "filePath",
                "path", "file_uri", "fileUri", "media_uri", "mediaUri",
                "android.media.metadata.MEDIA_URI", "android.media.metadata.DATA",
                Notification.EXTRA_TITLE, Notification.EXTRA_TEXT
        };
        for (String key : keys) {
            try {
                Object value = extras.get(key);
                if (value == null) continue;
                String raw = value instanceof Uri ? value.toString() : value.toString();
                if (raw == null) continue;
                raw = raw.trim();
                if (raw.length() == 0) continue;

                boolean explicitName = "filename".equals(key) || "file_name".equals(key) ||
                        "fileName".equals(key);
                String candidate = raw;
                if (raw.startsWith("file:") || raw.startsWith("content:")) {
                    String segment = Uri.parse(raw).getLastPathSegment();
                    if (segment != null) candidate = segment;
                } else {
                    int slash = candidate.lastIndexOf('/');
                    if (slash >= 0) candidate = candidate.substring(slash + 1);
                }
                int query = candidate.indexOf('?');
                if (query >= 0) candidate = candidate.substring(0, query);
                candidate = Uri.decode(candidate).trim();
                if (candidate.length() == 0) continue;

                boolean audioFile = candidate.matches("(?i).+\\.(mp3|flac|wav|m4a|aac|ogg|opus|wma|aiff|alac|mp4)$");
                if (explicitName || audioFile) return candidate;
            } catch (RuntimeException ignored) { }
        }
        return null;
    }

    private NotificationItem toItem(StatusBarNotification sbn) {
        Notification notification = sbn.getNotification();
        Bundle extras = notification == null ? null : notification.extras;
        CharSequence title = extras == null ? null : extras.getCharSequence(Notification.EXTRA_TITLE);
        CharSequence text = extras == null ? null : extras.getCharSequence(Notification.EXTRA_BIG_TEXT);
        if (text == null && extras != null) text = extras.getCharSequence(Notification.EXTRA_TEXT);
        String packageName = sbn.getPackageName() == null ? "" : sbn.getPackageName();
        String titleValue = title == null ? "" : title.toString().trim();
        String textValue = text == null ? "" : text.toString().trim();
        if (titleValue.length() == 0 && extras != null) {
            CharSequence alternate = extras.getCharSequence("android.title.big");
            if (alternate == null) alternate = extras.getCharSequence("android.media.metadata.TITLE");
            if (alternate == null) alternate = extras.getCharSequence("android.media.metadata.DISPLAY_TITLE");
            if (alternate == null) alternate = extras.getCharSequence(Notification.EXTRA_SUB_TEXT);
            if (alternate != null) titleValue = alternate.toString().trim();
        }
        if (titleValue.length() == 0 && notification != null && notification.tickerText != null)
            titleValue = notification.tickerText.toString().trim();
        if (titleValue.length() == 0 && textValue.length() > 0) titleValue = textValue;
        if (titleValue.length() == 0) titleValue = packageName;
        String searchable = (titleValue + " " + textValue).toLowerCase(java.util.Locale.US);
        String pkg = packageName.toLowerCase(java.util.Locale.US);
        boolean missed = searchable.contains("missed call") || searchable.contains("שיחה שלא נענתה") || searchable.contains("שיחות שלא נענו");
        // Read notification transport actions reflectively: QINS targets API 19,
        // where newer Notification.Action types are not guaranteed to exist.
        Object[] raw = null;
        try { raw = (Object[]) Notification.class.getField("actions").get(notification); }
        catch (Exception ignored) { }
        int count = raw == null ? 0 : Math.min(3, raw.length);
        String[] actionTitles = new String[count];
        PendingIntent[] actions = new PendingIntent[count];
        boolean transportAction = false;
        for (int i = 0; i < count; i++) {
            Object action = raw[i];
            try {
                Object titleObject = action == null ? null : action.getClass().getField("title").get(action);
                actionTitles[i] = titleObject == null ? "" : titleObject.toString();
                Object intentObject = action == null ? null : action.getClass().getField("actionIntent").get(action);
                actions[i] = intentObject instanceof PendingIntent ? (PendingIntent) intentObject : null;
            } catch (Exception ignored) {
                actionTitles[i] = "";
                actions[i] = null;
            }
            String actionLabel = actionTitles[i].toLowerCase(java.util.Locale.US);
            if (actionLabel.contains("play") || actionLabel.contains("pause") ||
                    actionLabel.contains("next") || actionLabel.contains("previous") ||
                    actionLabel.contains("skip") || actionLabel.contains("הפעל") ||
                    actionLabel.contains("השהה") || actionLabel.contains("הבא") ||
                    actionLabel.contains("הקודם")) transportAction = true;
        }
        boolean mediaPackage = pkg.contains("music") || pkg.contains("spotify") ||
                pkg.contains("media") || pkg.contains("youtube") || pkg.contains("vlc") ||
                pkg.contains("audio") || pkg.contains("podcast") || pkg.contains("deezer") ||
                pkg.contains("soundcloud") || pkg.contains("poweramp") || pkg.contains("foobar");
        boolean mediaSession = extras != null && extras.containsKey("android.mediaSession");
        Bitmap artwork = notification == null ? null : notification.largeIcon;
        if (artwork == null && extras != null) {
            try { artwork = (Bitmap) extras.getParcelable("android.picture"); } catch (RuntimeException ignored) { }
            if (artwork == null) try { artwork = (Bitmap) extras.getParcelable("android.largeIcon"); } catch (RuntimeException ignored) { }
        }
        boolean ongoing = notification != null &&
                (notification.flags & (Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR)) != 0;
        boolean media = !missed && (transportAction || mediaSession ||
                (mediaPackage && (count > 0 || ongoing)));
        if (media) {
            // If the media player exposes a path/URI, show its basename rather than
            // attempting to infer or identify the song from its audio content.
            String fileName = fileNameFromExtras(extras);
            if (fileName != null) {
                titleValue = fileName;
            } else if (extras != null) {
                CharSequence trackTitle = extras.getCharSequence("android.media.metadata.TITLE");
                if (trackTitle == null) trackTitle = extras.getCharSequence("android.media.metadata.DISPLAY_TITLE");
                if (trackTitle != null && trackTitle.toString().trim().length() > 0)
                    titleValue = trackTitle.toString().trim();
                else if ((titleValue.equals(packageName) || titleValue.length() == 0) && textValue.length() > 0)
                    titleValue = textValue;
            }
        }
        return new NotificationItem(makeKey(sbn), packageName, titleValue, textValue, sbn.getPostTime(), missed, media, artwork, actionTitles, actions);
    }

    public static void sendAction(NotificationItem item, int index) {
        if (item == null || item.actions == null || index < 0 || index >= item.actions.length) return;
        PendingIntent action = item.actions[index];
        if (action != null) try { action.send(); } catch (PendingIntent.CanceledException ignored) { }
    }

    /** API 19 fallback for music apps that expose transport controls via AudioManager. */
    public static void sendMediaKey(Context context, int keyCode) {
        try {
            AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (audio == null) return;
            audio.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, keyCode));
            audio.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, keyCode));
        } catch (RuntimeException ignored) { }
    }

    private static String makeKey(StatusBarNotification sbn) {
        return sbn.getPackageName() + ":" + sbn.getId() + ":" +
                String.valueOf(sbn.getTag()) + ":" + sbn.getPostTime();
    }

    private void removeByKey(String key) {
        for (int i = CACHE.size() - 1; i >= 0; i--) {
            if (CACHE.get(i).key.equals(key)) CACHE.remove(i);
        }
    }

    private void sortCache() {
        Collections.sort(CACHE, new Comparator<NotificationItem>() {
            @Override public int compare(NotificationItem left, NotificationItem right) {
                return left.postedAt == right.postedAt ? 0 :
                        (left.postedAt > right.postedAt ? -1 : 1);
            }
        });
    }

    private void notifyChanged() {
        Intent changed = new Intent(ACTION_CHANGED);
        changed.setPackage(getPackageName());
        sendBroadcast(changed);
    }
}
