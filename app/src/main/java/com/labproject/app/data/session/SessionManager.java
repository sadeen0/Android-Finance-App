package com.labproject.app.data.session;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREF = "session_pref";

    // session
    private static final String KEY_SESSION_EMAIL = "session_email";
    private static final String KEY_LOGGED_IN = "logged_in";

    // remember me (email only)
    private static final String KEY_REMEMBER = "remember";
    private static final String KEY_SAVED_EMAIL = "saved_email";

    private final SharedPreferences sp;

    public SessionManager(Context c) {
        sp = c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    // Session
    public void loginSession(String email) {
        sp.edit()
                .putString(KEY_SESSION_EMAIL, email.trim().toLowerCase())
                .putBoolean(KEY_LOGGED_IN, true)
                .apply();
    }

    public boolean isLoggedIn() {
        return sp.getBoolean(KEY_LOGGED_IN, false)
                && sp.getString(KEY_SESSION_EMAIL, null) != null;
    }

    public String getUserEmail() {
        return sp.getString(KEY_SESSION_EMAIL, null);
    }

    public void logout() {
        sp.edit()
                .remove(KEY_SESSION_EMAIL)
                .putBoolean(KEY_LOGGED_IN, false)
                .apply();
    }

    // Remember Me (email only)
    public void setRememberedEmail(String email) {
        sp.edit()
                .putBoolean(KEY_REMEMBER, true)
                .putString(KEY_SAVED_EMAIL, email.trim().toLowerCase())
                .apply();
    }

    public void clearRememberedEmail() {
        sp.edit()
                .putBoolean(KEY_REMEMBER, false)
                .remove(KEY_SAVED_EMAIL)
                .apply();
    }

    public boolean isRemembered() {
        return sp.getBoolean(KEY_REMEMBER, false)
                && sp.getString(KEY_SAVED_EMAIL, null) != null;
    }

    public String getRememberedEmail() {
        return sp.getString(KEY_SAVED_EMAIL, null);
    }
}
