package com.elka.faceitwidget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
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
        SharedPreferences p = getSharedPreferences("faceit", Context.MODE_PRIVATE);
        EditText nick = findViewById(R.id.nickname_input);
        EditText key = findViewById(R.id.key_input);
        nick.setText(p.getString("nickname", "Elka390"));
        key.setText(p.getString("api_key", ""));
        Button save = findViewById(R.id.save_button);
        save.setOnClickListener(v -> {
            String n = nick.getText().toString().trim();
            String k = key.getText().toString().trim();
            if (n.isEmpty() || k.isEmpty()) { Toast.makeText(this, "Укажи ник и API key", Toast.LENGTH_LONG).show(); return; }
            p.edit().putString("nickname", n).putString("api_key", k).apply();
            Intent i = new Intent(this, FaceitWidgetProvider.class).setAction("com.elka.faceitwidget.REFRESH");
            sendBroadcast(i);
            Toast.makeText(this, "Сохранено. Если виджет уже добавлен, он обновится.", Toast.LENGTH_LONG).show();
        });
    }
}
