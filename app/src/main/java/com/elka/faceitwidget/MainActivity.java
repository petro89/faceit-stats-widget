package com.elka.faceitwidget;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);
        SharedPreferences prefs = getSharedPreferences("faceit", Context.MODE_PRIVATE);
        EditText nick = findViewById(R.id.nickname_input);
        EditText steamId = findViewById(R.id.steam_id_input);
        EditText key = findViewById(R.id.key_input);
        nick.setText(prefs.getString("nickname", "Elka390"));
        steamId.setText(prefs.getString("steam_id", "76561199274700343"));
        key.setText(prefs.getString("api_key", ""));
        Button save = findViewById(R.id.save_button);
        save.setOnClickListener(view -> {
            String n = nick.getText().toString().trim();
            String s = steamId.getText().toString().trim();
            String k = key.getText().toString().trim();
            if (n.isEmpty() || s.isEmpty() || k.isEmpty()) {
                Toast.makeText(this, "Укажи ник, SteamID64 и ключ WOK", Toast.LENGTH_LONG).show();
                return;
            }
            if (!s.matches("\\d{17}")) {
                Toast.makeText(this, "SteamID64 должен состоять из 17 цифр", Toast.LENGTH_LONG).show();
                return;
            }
            prefs.edit().putString("nickname", n).putString("steam_id", s).putString("api_key", k).apply();
            Intent refresh = new Intent(this, FaceitWidgetProvider.class).setAction("com.elka.faceitwidget.REFRESH");
            sendBroadcast(refresh);
            Toast.makeText(this, "Сохранено. Виджет обновится; ключ хранится на этом устройстве.", Toast.LENGTH_LONG).show();
        });
    }
}
