package com.vn.rotate4way;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.*;
import android.widget.*;

public class MainActivity extends Activity implements View.OnClickListener {
    private TextView st;
    private Button pBtn;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setBackgroundColor(Color.parseColor("#121212"));
        root.setPadding(dp(20), dp(40), dp(20), dp(20));

        TextView title = new TextView(this);
        title.setText("Xoay Màn Hình (Hỗ Trợ Khóa)");
        title.setTextColor(Color.WHITE);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        root.addView(title);

        st = new TextView(this);
        st.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        st.setGravity(Gravity.CENTER);
        st.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(-1, -2);
        slp.topMargin = dp(16);
        slp.bottomMargin = dp(20);
        st.setLayoutParams(slp);
        root.addView(st);

        pBtn = btn(1, "Bật quyền Hỗ trợ (Trợ năng)", "#E65100");
        root.addView(pBtn);
        root.addView(btn(2, "Xoay 2 hướng (Dọc & Ngược 180°)", "#00897B"));
        root.addView(btn(3, "Xoay 4 hướng (Đủ 360°)", "#1E88E5"));
        root.addView(btn(4, "Tắt (1 hướng dọc mặc định)", "#424242"));

        TextView note = new TextView(this);
        note.setText("Hướng dẫn bật quyền trên Samsung:\nBấm nút cam ở trên -> Dịch vụ đã cài đặt (hoặc Ứng dụng đã cài đặt) -> Xoay 4 Hướng -> Gạt Bật.\n\nApp không đọc nội dung màn hình, không cần quyền lớp phủ cũ và không hiện thông báo thừa.");
        note.setTextColor(Color.parseColor("#B0BEC5"));
        note.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(-1, -2);
        nlp.topMargin = dp(16);
        note.setLayoutParams(nlp);
        root.addView(note);

        setContentView(root);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == 1) {
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            return;
        }
        if (id == 4) {
            RotateService.setMode(this, RotateService.MODE_OFF);
            refresh();
            return;
        }
        if (!RotateService.isEnabled()) {
            Toast.makeText(this, "Vui lòng bật quyền Hỗ trợ (Trợ năng) trước!", Toast.LENGTH_LONG).show();
            return;
        }
        RotateService.setMode(this, id == 2 ? RotateService.MODE_2WAY : RotateService.MODE_4WAY);
        refresh();
    }

    private Button btn(int id, String t, String hex) {
        Button b = new Button(this);
        b.setId(id);
        b.setText(t);
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor(hex));
        bg.setCornerRadius(dp(12));
        b.setBackground(bg);
        b.setOnClickListener(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(50));
        lp.bottomMargin = dp(10);
        b.setLayoutParams(lp);
        return b;
    }

    private void refresh() {
        boolean ok = RotateService.isEnabled();
        pBtn.setVisibility(ok ? View.GONE : View.VISIBLE);
        int s = RotateService.getState();
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(10));
        if (!ok) {
            st.setText("Chưa bật dịch vụ Hỗ trợ (Trợ năng)");
            st.setTextColor(Color.parseColor("#FFCC80"));
            bg.setColor(Color.parseColor("#3E2723"));
        } else if (s == RotateService.MODE_2WAY) {
            st.setText("ĐANG BẬT: Xoay 2 hướng (Dọc & Ngược)");
            st.setTextColor(Color.parseColor("#A5D6A7"));
            bg.setColor(Color.parseColor("#004D40"));
        } else if (s == RotateService.MODE_4WAY) {
            st.setText("ĐANG BẬT: Xoay 4 hướng (360°)");
            st.setTextColor(Color.parseColor("#90CAF9"));
            bg.setColor(Color.parseColor("#0D47A1"));
        } else {
            st.setText("ĐANG TẮT: 1 hướng dọc mặc định");
            st.setTextColor(Color.parseColor("#CFD8DC"));
            bg.setColor(Color.parseColor("#263238"));
        }
        st.setBackground(bg);
    }

    private int dp(int v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }
}
