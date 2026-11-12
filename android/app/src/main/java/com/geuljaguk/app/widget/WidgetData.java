package com.geuljaguk.app.widget;

import android.content.Context;
import com.geuljaguk.app.R;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import org.json.JSONArray;
import org.json.JSONObject;

/** res/raw/widget_data.json — 시간대별 '오늘의 한 줄'과 위젯용 글귀 목록 */
final class WidgetData {
    static final class Slot { int from, to; String name; String[] lines, weekend; }
    static final class Quote { int id; String text, author, cat; }

    private static WidgetData cache;
    Slot[] slots = new Slot[0];
    Quote[] quotes = new Quote[0];

    static synchronized WidgetData get(Context c) {
        if (cache != null) return cache;
        WidgetData d = new WidgetData();
        try (InputStream in = c.getResources().openRawResource(R.raw.widget_data)) {
            byte[] buf = new byte[in.available()];
            int n = 0, r;
            while (n < buf.length && (r = in.read(buf, n, buf.length - n)) >= 0) n += r;
            JSONObject o = new JSONObject(new String(buf, 0, n, StandardCharsets.UTF_8));
            JSONArray s = o.getJSONArray("slots");
            d.slots = new Slot[s.length()];
            for (int i = 0; i < s.length(); i++) {
                JSONObject j = s.getJSONObject(i);
                Slot t = new Slot();
                t.from = j.getInt("from"); t.to = j.getInt("to"); t.name = j.getString("name");
                t.lines = arr(j.getJSONArray("lines")); t.weekend = arr(j.optJSONArray("weekend"));
                d.slots[i] = t;
            }
            JSONArray q = o.getJSONArray("quotes");
            d.quotes = new Quote[q.length()];
            for (int i = 0; i < q.length(); i++) {
                JSONObject j = q.getJSONObject(i);
                Quote t = new Quote();
                t.id = j.optInt("id", 0); t.text = j.getString("text"); t.author = j.getString("author"); t.cat = j.optString("cat", "");
                d.quotes[i] = t;
            }
        } catch (Exception ignored) {}
        cache = d;
        return d;
    }

    private static String[] arr(JSONArray a) {
        if (a == null) return new String[0];
        String[] r = new String[a.length()];
        for (int i = 0; i < a.length(); i++) r[i] = a.optString(i, "");
        return r;
    }

    /** 앱(자바스크립트)과 같은 날짜 번호 — 같은 날에는 앱과 위젯이 같은 글을 보여줌 */
    static long dayIndex(Calendar now) {
        Calendar d = (Calendar) now.clone();
        d.set(Calendar.HOUR_OF_DAY, 0); d.set(Calendar.MINUTE, 0); d.set(Calendar.SECOND, 0); d.set(Calendar.MILLISECOND, 0);
        return Math.floorDiv(d.getTimeInMillis(), 86400000L);
    }

    private static long mod(long a, long b) { long r = a % b; return r < 0 ? r + b : r; }

    /** {시간대 이름, 한 줄} */
    String[] lineFor(Calendar now) {
        if (slots.length == 0) return new String[] { "오늘", "오늘도 당신에게 어울리는 한 줄이 있어요" };
        int h = now.get(Calendar.HOUR_OF_DAY), si = 0;
        for (int i = 0; i < slots.length; i++) if (h >= slots[i].from && h < slots[i].to) { si = i; break; }
        Slot s = slots[si];
        int dow = now.get(Calendar.DAY_OF_WEEK);
        boolean weekend = dow == Calendar.SATURDAY || dow == Calendar.SUNDAY;
        String[] pool = weekend && s.weekend.length > 0 ? s.weekend : s.lines;
        if (pool.length == 0) return new String[] { s.name, "오늘도 당신에게 어울리는 한 줄이 있어요" };
        long seed = dayIndex(now) * 31 + si * 7L;
        return new String[] { s.name, pool[(int) mod(2654435761L * seed, pool.length)] };
    }

    Quote quoteFor(Calendar now) {
        if (quotes.length > 0) return quotes[(int) mod(dayIndex(now), quotes.length)];
        Quote q = new Quote();
        q.text = "이 또한 지나가리라"; q.author = "페르시아 우화"; q.cat = "위로";
        return q;
    }
}
