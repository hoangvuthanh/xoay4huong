package com.vn.rotate4way;

import android.accessibilityservice.AccessibilityService;
import android.content.*;
import android.content.pm.ActivityInfo;
import android.graphics.PixelFormat;
import android.hardware.SensorManager;
import android.os.*;
import android.view.*;
import android.view.accessibility.AccessibilityEvent;

public class RotateService extends AccessibilityService {
    public static final int MODE_OFF = 0;
    public static final int MODE_2WAY = 1;
    public static final int MODE_4WAY = 2;

    private static volatile RotateService sInstance;
    private static volatile int sState = MODE_OFF;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private WindowManager wm;
    private View overlay;
    private WindowManager.LayoutParams lp;
    private OrientationEventListener orientationListener;
    private int current2WayOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
    private boolean screenReceiverRegistered = false;

    private final BroadcastReceiver screenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (sState != MODE_2WAY || orientationListener == null) return;
            String action = intent.getAction();
            if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                orientationListener.disable();
            } else if (Intent.ACTION_SCREEN_ON.equals(action) && orientationListener.canDetectOrientation()) {
                orientationListener.enable();
            }
        }
    };

    public static boolean isEnabled() {
        return sInstance != null;
    }

    public static int getState() {
        return sInstance != null ? sState : MODE_OFF;
    }

    public static void setMode(Context c, final int mode) {
        final RotateService svc = sInstance;
        if (svc == null) {
            sState = MODE_OFF;
            RotateTileService.refresh(c);
            return;
        }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            svc.applyMode(mode);
        } else {
            sState = mode;
            svc.mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    svc.applyMode(mode);
                }
            });
        }
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        sInstance = this;
        sState = MODE_OFF;
        RotateTileService.refresh(this);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
    }

    @Override
    public void onInterrupt() {
    }

    private void applyMode(int mode) {
        sState = mode;
        if (mode == MODE_OFF) {
            stop2WayListener();
            removeOverlay();
        } else if (mode == MODE_4WAY) {
            stop2WayListener();
            applyOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
        } else {
            current2WayOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
            applyOrientation(current2WayOrientation);
            start2WayListener();
        }
        RotateTileService.refresh(this);
    }

    private void start2WayListener() {
        if (orientationListener == null) {
            orientationListener = new OrientationEventListener(this, SensorManager.SENSOR_DELAY_NORMAL) {
                @Override
                public void onOrientationChanged(int angle) {
                    if (angle == OrientationEventListener.ORIENTATION_UNKNOWN || sState != MODE_2WAY) return;
                    int target = current2WayOrientation;
                    if (angle >= 145 && angle <= 215) {
                        target = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                    } else if (angle <= 35 || angle >= 325) {
                        target = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    }
                    if (target != current2WayOrientation) {
                        current2WayOrientation = target;
                        applyOrientation(target);
                    }
                }
            };
        }
        if (orientationListener.canDetectOrientation()) {
            orientationListener.enable();
        }
        if (!screenReceiverRegistered) {
            IntentFilter f = new IntentFilter();
            f.addAction(Intent.ACTION_SCREEN_OFF);
            f.addAction(Intent.ACTION_SCREEN_ON);
            registerReceiver(screenReceiver, f);
            screenReceiverRegistered = true;
        }
    }

    private void stop2WayListener() {
        if (orientationListener != null) {
            orientationListener.disable();
            orientationListener = null;
        }
        if (screenReceiverRegistered) {
            try { unregisterReceiver(screenReceiver); } catch (Exception ignored) {}
            screenReceiverRegistered = false;
        }
    }

    private void applyOrientation(int orientation) {
        if (wm == null) wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        if (wm == null) return;
        if (overlay == null) {
            overlay = new View(this);
            lp = new WindowManager.LayoutParams(
                    0, 0,
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
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

    private void removeOverlay() {
        if (wm != null && overlay != null) {
            try { wm.removeView(overlay); } catch (Exception ignored) {}
        }
        overlay = null;
        lp = null;
        wm = null;
    }

    private void cleanup() {
        stop2WayListener();
        removeOverlay();
        sState = MODE_OFF;
        if (sInstance == this) sInstance = null;
        RotateTileService.refresh(this);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        cleanup();
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        cleanup();
        super.onDestroy();
    }
}
