package com.vn.rotate4way;

import android.content.*;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.provider.Settings;
import android.service.quicksettings.*;

public class RotateTileService extends TileService {
    public static void requestTileUpdate(Context c) {
        try { TileService.requestListeningState(c, new ComponentName(c, RotateTileService.class)); } catch (Exception ignored) {}
    }
    @Override public void onStartListening() { super.onStartListening(); update(RotateService.isRunning(), RotateService.getCurrentMode()); }
    @Override @SuppressWarnings("deprecation")
    public void onClick() {
        super.onClick();
        if (!Settings.canDrawOverlays(this)) {
            startActivityAndCollapse(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            return;
        }
        if (RotateService.isRunning()) { update(false, RotateService.getCurrentMode()); RotateService.stop(this); }
        else { int m = RotateService.getSavedMode(this); update(true, m); RotateService.start(this, m); }
    }
    private void update(boolean on, int m) {
        Tile t = getQsTile(); if (t == null) return;
        t.setState(on ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        t.setLabel(getString(R.string.tile_label));
        if (Build.VERSION.SDK_INT >= 29) {
            t.setSubtitle(on ? (m == ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT ? "Ngược 180°" : "Tự động 4 chiều") : "Đang tắt");
        }
        t.updateTile();
    }
}