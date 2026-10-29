package com.elka.faceitwidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.RemoteViews;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class FaceitWidgetProvider extends AppWidgetProvider {
    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) { for (int id : ids) update(context, manager, id, "Загружаю статистику…"); }
    @Override public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if ("com.elka.faceitwidget.REFRESH".equals(intent.getAction())) {
            AppWidgetManager m = AppWidgetManager.getInstance(context);
            for (int id : m.getAppWidgetIds(new ComponentName(context, FaceitWidgetProvider.class))) update(context, m, id, "Обновляю…");
        }
    }
    private static void update(Context c, AppWidgetManager manager, int id, String initialStatus) {
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget_faceit);
        SharedPreferences p = c.getSharedPreferences("faceit", Context.MODE_PRIVATE);
        String nick = p.getString("nickname", "Elka390");
        v.setTextViewText(R.id.nickname, nick);
        v.setTextViewText(R.id.status, initialStatus);
        Intent refresh = new Intent(c, FaceitWidgetProvider.class).setAction("com.elka.faceitwidget.REFRESH");
        v.setOnClickPendingIntent(R.id.refresh, PendingIntent.getBroadcast(c, 42, refresh, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        Intent open = new Intent(c, MainActivity.class);
        v.setOnClickPendingIntent(R.id.nickname, PendingIntent.getActivity(c, 43, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        manager.updateAppWidget(id, v);
        String key = p.getString("api_key", "");
        if (key.isEmpty()) { v.setTextViewText(R.id.status, "Открой приложение и добавь API key"); manager.updateAppWidget(id, v); return; }
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                JSONObject profile = request("https://open.faceit.com/data/v4/players?nickname=" + java.net.URLEncoder.encode(nick, "UTF-8"), key);
                String playerId = profile.optString("player_id", "");
                if (playerId.isEmpty()) throw new Exception("Профиль не найден");
                int elo = profile.optInt("games", 0);
                JSONObject cs2 = profile.optJSONObject("games");
                if (cs2 != null) elo = cs2.optJSONObject("cs2") == null ? 0 : cs2.optJSONObject("cs2").optInt("faceit_elo", 0);
                JSONObject statsResponse = request("https://open.faceit.com/data/v4/players/" + playerId + "/stats/cs2", key);
                JSONObject lifetime = statsResponse.optJSONObject("lifetime");
                if (lifetime == null) lifetime = new JSONObject();
                String kd = metric(lifetime, "Average K/D Ratio");
                String wr = metric(lifetime, "Win Rate %");
                if (!wr.equals("—") && !wr.endsWith("%")) wr += "%";
                final String eloText = elo > 0 ? String.valueOf(elo) : "—";
                final String kdText = kd, wrText = wr;
                final String status = "Данные FACEIT · нажми ↻ для обновления";
                android.os.Handler h = new android.os.Handler(c.getMainLooper());
                h.post(() -> { RemoteViews out = new RemoteViews(c.getPackageName(), R.layout.widget_faceit); out.setTextViewText(R.id.nickname, nick); out.setTextViewText(R.id.elo, eloText); out.setTextViewText(R.id.kd, kdText); out.setTextViewText(R.id.winrate, wrText); out.setTextViewText(R.id.status, status); out.setOnClickPendingIntent(R.id.refresh, PendingIntent.getBroadcast(c, 42, refresh, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE)); out.setOnClickPendingIntent(R.id.nickname, PendingIntent.getActivity(c, 43, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE)); manager.updateAppWidget(id, out); });
            } catch (Exception e) {
                String message = e.getMessage() == null ? "Не удалось получить данные" : e.getMessage();
                if (message.contains("401") || message.contains("403")) message = "Проверь API key / права доступа";
                final String err = message.length() > 55 ? message.substring(0, 52) + "…" : message;
                new android.os.Handler(c.getMainLooper()).post(() -> { v.setTextViewText(R.id.status, "Ошибка: " + err); manager.updateAppWidget(id, v); });
            }
        }).start();
    }
    private static JSONObject request(String url, String key) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setRequestMethod("GET"); c.setConnectTimeout(12000); c.setReadTimeout(12000);
        c.setRequestProperty("Authorization", "Bearer " + key); c.setRequestProperty("Accept", "application/json");
        int code = c.getResponseCode();
        java.io.InputStream stream = code >= 200 && code < 300 ? c.getInputStream() : c.getErrorStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream)); StringBuilder body = new StringBuilder(); String line;
        while ((line = reader.readLine()) != null) body.append(line);
        reader.close(); c.disconnect();
        if (code < 200 || code >= 300) throw new Exception("HTTP " + code + " от FACEIT");
        return new JSONObject(body.toString());
    }
    private static String metric(JSONObject j, String key) { String s = j.optString(key, "—"); return s.equals("null") || s.isEmpty() ? "—" : s; }
}
