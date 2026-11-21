package com.vn.rotate4way;

import android.app.Activity;
import android.content.*;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.*;
import android.widget.*;

public class MainActivity extends Activity implements View.OnClickListener {
    private TextView st;
    private Button pBtn;
    private final BroadcastReceiver rcv = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) { refresh(); }
    };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        ScrollView sv = new ScrollView(this);
        sv.setFillViewport(true); sv.setBackgroundColor(Color.parseColor("#121212"));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL); root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(20), dp(36), dp(20), dp(24));

        TextView title = new TextView(this);
        title.setText("Xoay 4 Hướng (Siêu Nhẹ)"); title.setTextColor(Color.WHITE);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22); title.setGravity(Gravity.CENTER);
        root.addView(title);

        st = new TextView(this);
        st.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15); st.setGravity(Gravity.CENTER);
        st.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(-1, -2);
        slp.topMargin = dp(16); slp.bottomMargin = dp(20); st.setLayoutParams(slp);
        root.addView(st);

        pBtn = btn(1, "1. Cấp quyền Xuất hiện trên cùng", "#E65100");
        root.addView(pBtn);
        root.addView(btn(2, "Bật Xoay Tự Động 4 Hướng (360°)", "#1E88E5"));
        root.addView(btn(3, "Khóa Xoay Ngược 180° (Chân sạc lên trên)", "#00897B"));
        root.addView(btn(4, "Tắt (Trả về mặc định máy)", "#424242"));

        TextView g = new TextView(this);
        g.setText("Gắn phím tắt Samsung:\n1. Kéo thanh thông báo xuống 2 lần.\n2. Bấm 3 chấm (⋮) -> Thứ tự phím (hoặc +).\n3. Kéo \"Xoay 4 hướng\" xuống -> Hoàn tất.\n\n• Chạm 1 lần để Bật/Tắt nhanh.\n• Nhấn giữ để mở bảng chọn chế độ này.");
        g.setTextColor(Color.parseColor("#B0BEC5")); g.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        LinearLayout.LayoutParams glp = new LinearLayout.LayoutParams(-1, -2); glp.topMargin = dp(20); g.setLayoutParams(glp);
        root.addView(g);
        sv.addView(root); setContentView(sv);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == 1) startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())));
        else if (id == 2) go(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
        else if (id == 3) go(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
        else if (id == 4) { RotateService.stop(this); refresh(); }
    }

    private void go(int mode) {
        if (!Settings.canDrawOverlays(this)) { Toast.makeText(this, "Cấp quyền 'Xuất hiện trên cùng' trước!", Toast.LENGTH_LONG).show(); return; }
        RotateService.start(this, mode); refresh();
    }

    private Button btn(int id, String t, String hex) {
        Button b = new Button(this); b.setId(id); b.setText(t); b.setAllCaps(false); b.setTextColor(Color.WHITE);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        GradientDrawable bg = new GradientDrawable(); bg.setColor(Color.parseColor(hex)); bg.setCornerRadius(dp(12));
        b.setBackground(bg); b.setOnClickListener(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(52)); lp.bottomMargin = dp(12); b.setLayoutParams(lp);
        return b;
    }

    private void refresh() {
        boolean ok = Settings.canDrawOverlays(this);
        pBtn.setVisibility(ok ? View.GONE : View.VISIBLE);
        GradientDrawable bg = new GradientDrawable(); bg.setCornerRadius(dp(10));
        if (!ok) { st.setText("Chưa cấp quyền lớp phủ"); st.setTextColor(Color.parseColor("#FFCC80")); bg.setColor(Color.parseColor("#3E2723")); }
        else if (RotateService.isRunning()) {
            boolean rev = RotateService.getCurrentMode() == ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
            st.setText(rev ? "Trạng thái: ĐANG KHÓA NGƯỢC 180°" : "Trạng thái: ĐANG BẬT XOAY 4 HƯỚNG");
            st.setTextColor(Color.parseColor("#A5D6A7")); bg.setColor(Color.parseColor("#1B5E20"));
        } else { st.setText("Trạng thái: ĐANG TẮT"); st.setTextColor(Color.parseColor("#CFD8DC")); bg.setColor(Color.parseColor("#263238")); }
        st.setBackground(bg);
    }

    private int dp(int v) { return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics()); }
    @Override protected void onResume() { super.onResume(); registerReceiver(rcv, new IntentFilter(RotateService.ACTION_STATE)); refresh(); }
    @Override protected void onPause() { super.onPause(); try { unregisterReceiver(rcv); } catch (Exception ignored) {} }
}