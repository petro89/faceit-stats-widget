package com.elka.faceitwidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.RemoteViews;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class FaceitWidgetProvider extends AppWidgetProvider {
    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        for (int id : ids) update(context, manager, id, "Загружаю статистику…");
    }

    @Override public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if ("com.elka.faceitwidget.REFRESH".equals(intent.getAction())) {
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            for (int id : manager.getAppWidgetIds(new ComponentName(context, FaceitWidgetProvider.class)))
                update(context, manager, id, "Обновляю…");
        }
    }

    private static void update(Context context, AppWidgetManager manager, int id, String initialStatus) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_faceit);
        SharedPreferences prefs = context.getSharedPreferences("faceit", Context.MODE_PRIVATE);
        String nickname = prefs.getString("nickname", "Elka390");
        String steamId = prefs.getString("steam_id", "76561199274700343");
        views.setTextViewText(R.id.nickname, nickname);
        views.setTextViewText(R.id.status, initialStatus);
        Intent refresh = new Intent(context, FaceitWidgetProvider.class).setAction("com.elka.faceitwidget.REFRESH");
        Intent open = new Intent(context, MainActivity.class);
        views.setOnClickPendingIntent(R.id.refresh, PendingIntent.getBroadcast(context, 42, refresh, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        views.setOnClickPendingIntent(R.id.nickname, PendingIntent.getActivity(context, 43, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        manager.updateAppWidget(id, views);

        String key = prefs.getString("api_key", "");
        if (key.isEmpty()) {
            views.setTextViewText(R.id.status, "Открой приложение и добавь ключ WOK");
            manager.updateAppWidget(id, views);
            return;
        }

        final String savedNickname = nickname;
        final String savedSteamId = steamId;
        new Thread(() -> {
            try {
                String encodedId = URLEncoder.encode(savedSteamId, "UTF-8");
                JSONObject profile = request(
                    "https://woksteamapi.com/faceit/data/v4/players?game=cs2&game_player_id=" + encodedId, key);
                String playerId = profile.optString("player_id", "");
                if (playerId.isEmpty()) throw new Exception("FACEIT-профиль не найден для этого SteamID");
                JSONObject games = profile.optJSONObject("games");
                JSONObject cs2 = games == null ? null : games.optJSONObject("cs2");
                int elo = cs2 == null ? 0 : cs2.optInt("faceit_elo", 0);
                String actualNickname = profile.optString("nickname", savedNickname);
                if (actualNickname.isEmpty()) actualNickname = savedNickname;

                JSONObject stats = request(
                    "https://woksteamapi.com/faceit/data/v4/players/" + playerId + "/stats/cs2", key);
                JSONObject lifetime = stats.optJSONObject("lifetime");
                if (lifetime == null) lifetime = new JSONObject();
                String kd = metric(lifetime, "Average K/D Ratio");
                String winRate = metric(lifetime, "Win Rate %");
                if (!winRate.equals("—") && !winRate.endsWith("%")) winRate += "%";

                final String eloText = elo > 0 ? String.valueOf(elo) : "—";
                final String kdText = kd;
                final String wrText = winRate;
                final String nameText = actualNickname;
                final String status = "Обновлено через WOK · нажми ↻";
                new android.os.Handler(context.getMainLooper()).post(() -> {
                    RemoteViews out = new RemoteViews(context.getPackageName(), R.layout.widget_faceit);
                    out.setTextViewText(R.id.nickname, nameText);
                    out.setTextViewText(R.id.elo, eloText);
                    out.setTextViewText(R.id.kd, kdText);
                    out.setTextViewText(R.id.winrate, wrText);
                    out.setTextViewText(R.id.status, status);
                    out.setOnClickPendingIntent(R.id.refresh, PendingIntent.getBroadcast(context, 42, refresh, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
                    out.setOnClickPendingIntent(R.id.nickname, PendingIntent.getActivity(context, 43, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
                    manager.updateAppWidget(id, out);
                });
            } catch (Exception e) {
                String message = e.getMessage() == null ? "Не удалось получить данные" : e.getMessage();
                if (message.contains("HTTP 401")) message = "Ключ WOK неверный или неактивен";
                else if (message.contains("HTTP 402")) message = "Лимит запросов WOK исчерпан";
                else if (message.contains("HTTP 404")) message = "FACEIT-профиль по этому SteamID не найден";
                else if (message.contains("HTTP 429")) message = "Слишком много запросов; попробуй позже";
                final String error = message.length() > 55 ? message.substring(0, 52) + "…" : message;
                new android.os.Handler(context.getMainLooper()).post(() -> {
                    views.setTextViewText(R.id.status, "Ошибка: " + error);
                    manager.updateAppWidget(id, views);
                });
            }
        }).start();
    }

    private static JSONObject request(String url, String key) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        try {
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);
            connection.setRequestProperty("Authorization", "Bearer " + key);
            connection.setRequestProperty("Accept", "application/json");
            int code = connection.getResponseCode();
            InputStream stream = code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream();
            StringBuilder body = new StringBuilder();
            if (stream != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
                String line;
                while ((line = reader.readLine()) != null) body.append(line);
                reader.close();
            }
            if (code < 200 || code >= 300) throw new Exception("HTTP " + code + " от WOK");
            return new JSONObject(body.toString());
        } finally {
            connection.disconnect();
        }
    }

    private static String metric(JSONObject json, String key) {
        String value = json.optString(key, "—");
        return value.equals("null") || value.isEmpty() ? "—" : value;
    }
}
