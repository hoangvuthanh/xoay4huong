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
    public static final int MODE_OFF = 0;
    public static final int MODE_2WAY = 1;
    public static final int MODE_4WAY = 2;

    private static final String ACTION_SET = "com.vn.rotate4way.SET";
    private static final String ACTION_STOP = "com.vn.rotate4way.STOP";
    private static final String EXTRA_MODE = "m";
    private static final String CH = "r4_min";

    private static volatile int sState = MODE_OFF;
    private WindowManager wm;
    private View overlay;
    private WindowManager.LayoutParams lp;

    public static int getState() {
        return sState;
    }

    public static void setMode(Context c, int mode) {
        if (mode == MODE_OFF) {
            boolean wasOn = (sState != MODE_OFF);
            sState = MODE_OFF;
            if (wasOn) {
                try {
                    c.startService(new Intent(c, RotateService.class).setAction(ACTION_STOP));
                } catch (Exception ignored) {}
            }
            RotateTileService.refresh(c);
            return;
        }
        boolean alreadyRunning = (sState != MODE_OFF);
        sState = mode;
        Intent i = new Intent(c, RotateService.class).setAction(ACTION_SET).putExtra(EXTRA_MODE, mode);
        if (Build.VERSION.SDK_INT >= 26 && !alreadyRunning) {
            c.startForegroundService(i);
        } else {
            c.startService(i);
        }
        RotateTileService.refresh(c);
    }

    @Override
    public IBinder onBind(Intent i) {
        return null;
    }

    @Override
    public int onStartCommand(Intent i, int flags, int startId) {
        if (i != null && ACTION_STOP.equals(i.getAction())) {
            cleanup();
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }
        if (!Settings.canDrawOverlays(this)) {
            cleanup();
            stopSelf();
            return START_NOT_STICKY;
        }
        int mode = (i != null) ? i.getIntExtra(EXTRA_MODE, MODE_2WAY) : sState;
        if (mode == MODE_OFF) {
            cleanup();
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }
        sState = mode;
        startForeground(1, buildNotif(mode));
        applyOrientation(mode == MODE_4WAY
                ? ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
                : ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
        RotateTileService.refresh(this);
        return START_NOT_STICKY;
    }

    private void applyOrientation(int orientation) {
        if (wm == null) wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        if (wm == null) return;
        if (overlay == null) {
            overlay = new View(this);
            int type = (Build.VERSION.SDK_INT >= 26)
                    ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    : WindowManager.LayoutParams.TYPE_PHONE;
            lp = new WindowManager.LayoutParams(
                    0, 0, type,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                            | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT);
            lp.screenOrientation = orientation;
            try { wm.addView(overlay, lp); } catch (Exception e) { overlay = null; }
        } else {
            lp.screenOrientation = orientation;
            try { wm.updateViewLayout(overlay, lp); } catch (Exception ignored) {}
        }
    }

    private void cleanup() {
        if (wm != null && overlay != null) {
            try { wm.removeView(overlay); } catch (Exception ignored) {}
        }
        overlay = null;
        lp = null;
        wm = null;
        sState = MODE_OFF;
        RotateTileService.refresh(this);
    }

    private Notification buildNotif(int mode) {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (nm != null && nm.getNotificationChannel(CH) == null) {
                NotificationChannel ch = new NotificationChannel(CH, "Xoay màn hình", NotificationManager.IMPORTANCE_MIN);
                ch.setShowBadge(false);
                nm.createNotificationChannel(ch);
            }
        }
        PendingIntent stopPi = PendingIntent.getService(
                this, 1,
                new Intent(this, RotateService.class).setAction(ACTION_STOP),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b = (Build.VERSION.SDK_INT >= 26)
                ? new Notification.Builder(this, CH)
                : new Notification.Builder(this).setPriority(Notification.PRIORITY_MIN);
        String txt = (mode == MODE_4WAY)
                ? "Đang bật: Xoay 4 hướng"
                : "Đang bật: Xoay 2 hướng (Dọc & Ngược)";
        return b.setSmallIcon(R.drawable.ic_rotate_4way)
                .setContentTitle("Xoay màn hình")
                .setContentText(txt)
                .setOngoing(true)
                .addAction(new Notification.Action.Builder((Icon) null, "Tắt", stopPi).build())
                .build();
    }

    @Override
    public void onDestroy() {
        cleanup();
        super.onDestroy();
    }
}
