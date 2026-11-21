package com.vn.rotate4way;

import android.app.*;
import android.content.*;
import android.content.pm.ActivityInfo;
import android.graphics.PixelFormat;
import android.graphics.drawable.Icon;
import android.os.*;
import android.provider.Settings;
import android.view.*;

public class RotateService extends Service {
    public static final String ACTION_START = "com.vn.rotate4way.START";
    public static final String ACTION_STOP = "com.vn.rotate4way.STOP";
    public static final String ACTION_STATE = "com.vn.rotate4way.STATE";
    public static final String EXTRA_MODE = "mode";
    private static final String PREF = "r4_prefs", KEY = "mode", CH = "r4_ch";
    private static volatile boolean sRunning = false;
    private static volatile int sMode = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR;
    private WindowManager wm;
    private View overlay;
    private WindowManager.LayoutParams lp;

    public static boolean isRunning() { return sRunning; }
    public static int getCurrentMode() { return sMode; }
    public static int getSavedMode(Context c) {
        return c.getSharedPreferences(PREF, MODE_PRIVATE).getInt(KEY, ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
    }
    public static void saveMode(Context c, int m) {
        c.getSharedPreferences(PREF, MODE_PRIVATE).edit().putInt(KEY, m).apply();
    }
    public static void start(Context c, int m) {
        saveMode(c, m);
        Intent i = new Intent(c, RotateService.class).setAction(ACTION_START).putExtra(EXTRA_MODE, m);
        if (Build.VERSION.SDK_INT >= 26) c.startForegroundService(i); else c.startService(i);
    }
    public static void stop(Context c) {
        c.startService(new Intent(c, RotateService.class).setAction(ACTION_STOP));
    }

    @Override public IBinder onBind(Intent i) { return null; }

    @Override
    public int onStartCommand(Intent i, int f, int id) {
        if (i != null && ACTION_STOP.equals(i.getAction())) {
            removeOverlay(); sRunning = false; notifyState();
            stopForeground(true); stopSelf(); return START_NOT_STICKY;
        }
        if (!Settings.canDrawOverlays(this)) {
            sRunning = false; notifyState(); stopSelf(); return START_NOT_STICKY;
        }
        int mode = (i != null && i.hasExtra(EXTRA_MODE)) ? i.getIntExtra(EXTRA_MODE, ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR) : getSavedMode(this);
        sMode = mode; saveMode(this, mode);
        startForeground(101, buildNotif(mode));
        applyOverlay(mode);
        sRunning = true; notifyState();
        return START_STICKY;
    }

    private void applyOverlay(int mode) {
        if (wm == null) wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        if (wm == null) return;
        if (overlay == null) {
            overlay = new View(this);
            int type = (Build.VERSION.SDK_INT >= 26) ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE;
            lp = new WindowManager.LayoutParams(0, 0, type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
            lp.screenOrientation = mode;
            try { wm.addView(overlay, lp); } catch (Exception e) { overlay = null; }
        } else {
            lp.screenOrientation = mode;
            try { wm.updateViewLayout(overlay, lp); } catch (Exception ignored) {}
        }
    }

    private void removeOverlay() {
        if (wm != null && overlay != null) {
            try { wm.removeView(overlay); } catch (Exception ignored) {}
            overlay = null;
        }
    }

    private Notification buildNotif(int mode) {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (nm != null && nm.getNotificationChannel(CH) == null) {
                NotificationChannel ch = new NotificationChannel(CH, "Xoay 4 hướng", NotificationManager.IMPORTANCE_MIN);
                ch.setShowBadge(false); nm.createNotificationChannel(ch);
            }
        }
        PendingIntent openPi = PendingIntent.getActivity(this, 0,
            new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent stopPi = PendingIntent.getService(this, 1,
            new Intent(this, RotateService.class).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b = (Build.VERSION.SDK_INT >= 26)
            ? new Notification.Builder(this, CH)
            : new Notification.Builder(this).setPriority(Notification.PRIORITY_MIN);
        String txt = (mode == ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT) ? "Đang khóa ngược 180°" : "Đang bật tự động 4 hướng";
        return b.setSmallIcon(R.drawable.ic_rotate_4way).setContentTitle("Xoay 4 hướng").setContentText(txt)
            .setOngoing(true).setContentIntent(openPi)
            .addAction(new Notification.Action.Builder((Icon) null, "Tắt", stopPi).build()).build();
    }

    private void notifyState() {
        RotateTileService.requestTileUpdate(this);
        sendBroadcast(new Intent(ACTION_STATE).setPackage(getPackageName()));
    }

    @Override public void onDestroy() { removeOverlay(); sRunning = false; notifyState(); super.onDestroy(); }
}