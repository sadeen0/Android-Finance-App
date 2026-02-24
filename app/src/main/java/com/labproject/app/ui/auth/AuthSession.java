package com.labproject.app.ui.auth;

import android.content.Context;
import android.content.SharedPreferences;

public class AuthSession {

    private static final String PREF_NAME = "auth_pref";
    private static final String KEY_LOGGED_IN = "logged_in";

    public static void login(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sp.edit().putBoolean(KEY_LOGGED_IN, true).apply();
    }

    public static void logout(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sp.edit().clear().apply();
    }

    public static boolean isLoggedIn(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sp.getBoolean(KEY_LOGGED_IN, false);
    }
}
