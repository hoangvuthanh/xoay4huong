package com.vn.rotate4way;

import android.content.*;
import android.os.Build;
import android.service.quicksettings.*;

public class RotateTileService extends TileService {

    public static void refresh(Context c) {
        try {
            TileService.requestListeningState(c, new ComponentName(c, RotateTileService.class));
        } catch (Exception ignored) {}
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateUi(RotateService.getState());
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onClick() {
        super.onClick();
        if (!RotateService.isEnabled()) {
            startActivityAndCollapse(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            return;
        }
        int next = (RotateService.getState() + 1) % 3;
        RotateService.setMode(this, next);
        updateUi(next);
    }

    private void updateUi(int state) {
        Tile t = getQsTile();
        if (t == null) return;
        if (state == RotateService.MODE_2WAY) {
            t.setState(Tile.STATE_ACTIVE);
            t.setLabel("Xoay 2 hướng");
            if (Build.VERSION.SDK_INT >= 29) t.setSubtitle("Dọc & Ngược");
        } else if (state == RotateService.MODE_4WAY) {
            t.setState(Tile.STATE_ACTIVE);
            t.setLabel("Xoay 4 hướng");
            if (Build.VERSION.SDK_INT >= 29) t.setSubtitle("Đủ 4 chiều");
        } else {
            t.setState(Tile.STATE_INACTIVE);
            t.setLabel("Xoay 1 hướng");
            if (Build.VERSION.SDK_INT >= 29) t.setSubtitle("Đang tắt");
        }
        t.updateTile();
    }
}
