package com.example.sequencemultiplayer;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatActivity;

public class SharedPreferencesHelper extends AppCompatActivity {

    SharedPreferences sharedpreferences;

    public SharedPreferencesHelper(Context context) {
        sharedpreferences = context.getSharedPreferences("PREFS", context.MODE_PRIVATE);
    }

    public void putString(String key, String value) {
        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.putString(key, value);
        editor.commit();
    }

    public void clear() {
        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.clear();
        editor.commit();
    }

    public String getString(String key, String def) {
        return sharedpreferences.getString(key, def);
    }
}
