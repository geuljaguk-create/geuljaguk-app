package com.geuljaguk.app.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import org.json.JSONArray;
import org.json.JSONObject;

/** 앱이 저장한 files/widget_look.json(배경 스타일·색)을 읽어 위젯 배경을 그림 */
final class WidgetLook {
    String style = "default";
    int frame = 0xFF2E4A3B;      // 기본 초록
    int ink = 0xFF1D2A22;        // 글씨
    int ink2 = 0xFF6B7A70;       // 작은 글씨
    boolean dark = false;
    int base = 0xFFFBFAF6;
    JSONArray blobs = new JSONArray();

    static WidgetLook read(Context c) {
        WidgetLook w = new WidgetLook();
        try {
            File f = new File(c.getFilesDir(), "widget_look.json");
            if (!f.exists()) return w;
            byte[] b = new byte[(int) f.length()];
            try (FileInputStream in = new FileInputStream(f)) {
                int n = 0, r;
                while (n < b.length && (r = in.read(b, n, b.length - n)) >= 0) n += r;
            }
            JSONObject o = new JSONObject(new String(b, StandardCharsets.UTF_8));
            w.style = o.optString("style", "default");
            w.frame = col(o.optString("frame", ""), w.frame);
            w.dark = o.optBoolean("dark", false);
            w.base = col(o.optString("base", ""), w.base);
            JSONArray bl = o.optJSONArray("blobs");
            if (bl != null) w.blobs = bl;
            if (w.dark) { w.ink = 0xFFF1F5F9; w.ink2 = 0xFF9AA5B1; }
        } catch (Exception ignored) {}
        return w;
    }

    private static int col(String s, int def) {
        try {
            if (s != null && s.matches("#?[0-9a-fA-F]{6}")) return Color.parseColor(s.startsWith("#") ? s : "#" + s);
        } catch (Exception ignored) {}
        return def;
    }

    Bitmap draw(int w, int h, float dp) {
        Bitmap bm = Bitmap.createBitmap(Math.max(w, 10), Math.max(h, 10), Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(bm);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        float r = 22 * dp;
        RectF all = new RectF(0, 0, w, h);
        Path clip = new Path();
        clip.addRoundRect(all, r, r, Path.Direction.CW);
        cv.save();
        cv.clipPath(clip);
        switch (style) {
            case "book": {          // 기본: 테두리 색 + 안쪽 종이
                p.setColor(frame); cv.drawRect(all, p);
                float m = 5 * dp;
                p.setColor(0xFFFBFAF6);
                cv.drawRoundRect(new RectF(m, m, w - m, h - m), r - m, r - m, p);
                break;
            }
            case "notepad": {       // 메모장: 파란 줄 + 빨간 여백선
                p.setColor(0xFFF6F9FF); cv.drawRect(all, p);
                p.setColor(0x5A4880C8); p.setStrokeWidth(1.2f * dp);
                for (float y = 26 * dp; y < h; y += 22 * dp) cv.drawLine(0, y, w, y, p);
                p.setColor(0xCCE43C50);
                cv.drawLine(w * 0.1f, 0, w * 0.1f, h, p);
                break;
            }
            case "oldpaper": {      // 오래된 원고
                p.setShader(new RadialGradient(w / 2f, h / 2f, (float) Math.hypot(w, h) / 2f,
                        new int[] { 0xFFF3E7C9, 0xFFF3E7C9, 0xFFE2CF9F }, new float[] { 0f, 0.55f, 1f }, Shader.TileMode.CLAMP));
                cv.drawRect(all, p);
                p.setShader(null);
                p.setColor(0x26785A28); p.setStrokeWidth(dp);
                for (float y = 26 * dp; y < h; y += 22 * dp) cv.drawLine(0, y, w, y, p);
                break;
            }
            case "spring": {        // 스프링 수첩
                p.setColor(0xFFFDFCF8); cv.drawRect(all, p);
                p.setColor(0x382E4A3B); p.setStrokeWidth(dp);
                for (float y = 30 * dp; y < h; y += 22 * dp) cv.drawLine(0, y, w, y, p);
                p.setColor(frame);
                cv.drawRect(0, 0, w, 9 * dp, p);
                break;
            }
            case "gradient": {      // 그라데이션: 바탕색 + 둥근 빛 번짐
                p.setColor(base); cv.drawRect(all, p);
                float R0 = (float) Math.hypot(w, h);
                for (int i = 0; i < blobs.length(); i++) {
                    JSONArray b = blobs.optJSONArray(i);
                    if (b == null || b.length() < 4) continue;
                    int c1 = col(b.optString(2), base);
                    p.setShader(new RadialGradient((float) b.optDouble(0) * w, (float) b.optDouble(1) * h,
                            Math.max(1f, (float) b.optDouble(3) * R0 * 0.75f), c1, c1 & 0x00FFFFFF, Shader.TileMode.CLAMP));
                    cv.drawRect(all, p);
                }
                p.setShader(null);
                break;
            }
            case "system":
                p.setColor(dark ? 0xFF16181D : 0xFFF4F6F9); cv.drawRect(all, p);
                break;
            default:
                p.setColor(Color.WHITE); cv.drawRect(all, p);
        }
        cv.restore();
        if (!"book".equals(style)) {  // 얇은 테두리
            p.setShader(null);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(dp);
            p.setColor(dark ? 0x33FFFFFF : 0x1F1D2A22);
            cv.drawRoundRect(new RectF(dp * 0.5f, dp * 0.5f, w - dp * 0.5f, h - dp * 0.5f), r, r, p);
        }
        return bm;
    }
}
