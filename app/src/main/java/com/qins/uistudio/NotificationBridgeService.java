package com.qins.uistudio;

import android.app.Notification;
import android.app.PendingIntent;
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
        public final String[] actionTitles;
        public final PendingIntent[] actions;
        NotificationItem(String key, String packageName, String title, String text, long postedAt,
                         boolean missedCall, boolean media, String[] actionTitles, PendingIntent[] actions) {
            this.key = key; this.packageName = packageName; this.title = title; this.text = text;
            this.postedAt = postedAt; this.missedCall = missedCall; this.media = media;
            this.actionTitles = actionTitles; this.actions = actions;
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

    private NotificationItem toItem(StatusBarNotification sbn) {
        Notification notification = sbn.getNotification();
        Bundle extras = notification == null ? null : notification.extras;
        CharSequence title = extras == null ? null : extras.getCharSequence(Notification.EXTRA_TITLE);
        CharSequence text = extras == null ? null : extras.getCharSequence(Notification.EXTRA_BIG_TEXT);
        if (text == null && extras != null) text = extras.getCharSequence(Notification.EXTRA_TEXT);
        String packageName = sbn.getPackageName() == null ? "" : sbn.getPackageName();
        String titleValue = title == null ? "" : title.toString().trim();
        String textValue = text == null ? "" : text.toString().trim();
        if (titleValue.length() == 0) titleValue = packageName;
        String searchable = (titleValue + " " + textValue).toLowerCase(java.util.Locale.US);
        String pkg = packageName.toLowerCase(java.util.Locale.US);
        boolean missed = searchable.contains("missed call") || searchable.contains("שיחה שלא נענתה") || searchable.contains("שיחות שלא נענו");
        Notification.Action[] raw = notification == null ? null : notification.actions;
        int count = raw == null ? 0 : Math.min(3, raw.length);
        String[] actionTitles = new String[count]; PendingIntent[] actions = new PendingIntent[count];
        for (int i = 0; i < count; i++) { Notification.Action action = raw[i]; actionTitles[i] = action == null || action.title == null ? "" : action.title.toString(); actions[i] = action == null ? null : action.actionIntent; }
        boolean media = !missed && count >= 2 && (pkg.contains("music") || pkg.contains("spotify") || pkg.contains("media") || pkg.contains("youtube") || pkg.contains("vlc") || pkg.contains("audio"));
        return new NotificationItem(makeKey(sbn), packageName, titleValue, textValue, sbn.getPostTime(), missed, media, actionTitles, actions);
    }

    public static void sendAction(NotificationItem item, int index) {
        if (item == null || item.actions == null || index < 0 || index >= item.actions.length) return;
        PendingIntent action = item.actions[index];
        if (action != null) try { action.send(); } catch (PendingIntent.CanceledException ignored) { }
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
