package com.geuljaguk.app.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.RemoteViews;
import com.geuljaguk.app.MainActivity;
import com.geuljaguk.app.R;
import java.util.Calendar;

/** 홈 화면 위젯 공통: '오늘의 글귀'(quoteMode=true) / '오늘의 한 줄'(false) */
public abstract class BaseWidget extends AppWidgetProvider {
    protected abstract int layout();
    protected abstract boolean quoteMode();

    @Override
    public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        for (int id : ids) m.updateAppWidget(id, build(c, m, id));
    }

    @Override
    public void onAppWidgetOptionsChanged(Context c, AppWidgetManager m, int id, Bundle o) {
        m.updateAppWidget(id, build(c, m, id));
    }

    /** 모든 위젯 새로 그리기 (앱이 화면에서 내려갈 때 호출) */
    public static void refreshAll(Context c) {
        AppWidgetManager m = AppWidgetManager.getInstance(c);
        Class<?>[] all = { LineSmallWidget.class, LineWideWidget.class, QuoteSmallWidget.class, QuoteWideWidget.class };
        for (Class<?> k : all) {
            int[] ids = m.getAppWidgetIds(new ComponentName(c, k));
            if (ids == null || ids.length == 0) continue;
            Intent u = new Intent(c, k);
            u.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            u.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
            c.sendBroadcast(u);
        }
    }

    @Override
    public void onReceive(Context c, Intent i) {
        super.onReceive(c, i);
        String a = i.getAction();
        if (Intent.ACTION_TIME_CHANGED.equals(a) || Intent.ACTION_TIMEZONE_CHANGED.equals(a) || Intent.ACTION_DATE_CHANGED.equals(a)) {
            AppWidgetManager m = AppWidgetManager.getInstance(c);
            onUpdate(c, m, m.getAppWidgetIds(new ComponentName(c, getClass())));
        }
    }

    RemoteViews build(Context c, AppWidgetManager m, int wid) {
        RemoteViews v = new RemoteViews(c.getPackageName(), layout());
        // 배경: 앱의 읽기 설정(files/widget_look.json)을 따라 그림
        try {
            WidgetLook look = WidgetLook.read(c);
            float dp = c.getResources().getDisplayMetrics().density;
            Bundle o = m.getAppWidgetOptions(wid);
            int wDp = o != null ? o.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 0) : 0;
            int hDp = o != null ? o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0) : 0;
            if (wDp <= 0) wDp = 300;
            if (hDp <= 0) hDp = 150;
            float sc = Math.min(1f, 700f / (wDp * dp));
            v.setImageViewBitmap(R.id.w_bg, look.draw((int) (wDp * dp * sc), (int) (hDp * dp * sc), dp * sc));
            v.setTextColor(R.id.w_text, look.ink);
            v.setTextColor(R.id.w_label, look.ink2);
            v.setTextColor(R.id.w_sub, look.ink2);
        } catch (Exception ignored) {}

        WidgetData d = WidgetData.get(c);
        Calendar now = Calendar.getInstance();
        int qid = 0;
        if (quoteMode()) {
            WidgetData.Quote q = d.quoteFor(now);
            qid = q.id;
            v.setTextViewText(R.id.w_label, q.cat.isEmpty() ? "오늘의 글귀" : "오늘의 글귀 · #" + q.cat);
            v.setTextViewText(R.id.w_text, q.text);
            v.setTextViewText(R.id.w_sub, "- " + q.author + " -");
        } else {
            String[] r = d.lineFor(now);
            v.setTextViewText(R.id.w_label, r[0] + "의 한 줄");
            v.setTextViewText(R.id.w_text, r[1]);
            v.setTextViewText(R.id.w_sub, "글자국");
        }
        // 누르면 앱 열기 — 글귀 위젯은 그 글귀로 바로 이동
        Intent open = new Intent(c, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if (qid > 0) {
            open.setAction(Intent.ACTION_VIEW);
            open.setData(Uri.parse("https://geuljaguk-e0920.web.app/q/" + qid + "/"));
        }
        PendingIntent pi = PendingIntent.getActivity(c, layout(), open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        v.setOnClickPendingIntent(R.id.w_root, pi);
        return v;
    }
}
