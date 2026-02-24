package com.labproject.app.data.prefs;

import android.content.Context;
import android.content.SharedPreferences;

import com.labproject.app.R;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

public class SettingsManager {

    private static final String PREF = "settings_pref";

    private static final String KEY_THEME = "theme";                 // "light" or "dark"
    private static final String KEY_DEFAULT_PERIOD = "default_period"; // "day" "week" "month"

    private static final String KEY_INCOME_CATS = "income_categories_json";
    private static final String KEY_EXPENSE_CATS = "expense_categories_json";

    private final SharedPreferences sp;
    private final Context ctx;

    public SettingsManager(Context context) {
        ctx = context.getApplicationContext();
        sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        ensureDefaults();
    }

    //  THEME

    public String getTheme() {
        return sp.getString(KEY_THEME, "light");
    }

    public void setTheme(String theme) {
        if (!"dark".equals(theme)) theme = "light";
        sp.edit().putString(KEY_THEME, theme).apply();
    }

    //  DEFAULT PERIOD

    public String getDefaultPeriod() {
        String p = sp.getString(KEY_DEFAULT_PERIOD, "month");
        if (!p.equals("day") && !p.equals("week") && !p.equals("month")) p = "month";
        return p;
    }

    public void setDefaultPeriod(String period) {
        if (!period.equals("day") && !period.equals("week") && !period.equals("month")) period = "month";
        sp.edit().putString(KEY_DEFAULT_PERIOD, period).apply();
    }

    //  CATEGORIES

    public List<String> getIncomeCategories() {
        return readJsonList(KEY_INCOME_CATS);
    }

    public List<String> getExpenseCategories() {
        return readJsonList(KEY_EXPENSE_CATS);
    }

    public boolean addIncomeCategory(String name) {
        return addCategory(KEY_INCOME_CATS, name);
    }

    public boolean addExpenseCategory(String name) {
        return addCategory(KEY_EXPENSE_CATS, name);
    }

    public boolean removeIncomeCategory(String name) {
        return removeCategory(KEY_INCOME_CATS, name);
    }

    public boolean removeExpenseCategory(String name) {
        return removeCategory(KEY_EXPENSE_CATS, name);
    }

    public void resetCategoriesToDefaults() {
        sp.edit().remove(KEY_INCOME_CATS).remove(KEY_EXPENSE_CATS).apply();
        ensureDefaults();
    }

    //  INTERNAL

    private void ensureDefaults() {
        // if categories not saved yet -> load from arrays.xml
        if (!sp.contains(KEY_INCOME_CATS)) {
            String[] arr = ctx.getResources().getStringArray(R.array.income_categories);
            writeJsonList(KEY_INCOME_CATS, toList(arr));
        }
        if (!sp.contains(KEY_EXPENSE_CATS)) {
            String[] arr = ctx.getResources().getStringArray(R.array.expense_categories);
            writeJsonList(KEY_EXPENSE_CATS, toList(arr));
        }

        // theme / period defaults
        if (!sp.contains(KEY_THEME)) sp.edit().putString(KEY_THEME, "light").apply();
        if (!sp.contains(KEY_DEFAULT_PERIOD)) sp.edit().putString(KEY_DEFAULT_PERIOD, "month").apply();
    }

    private boolean addCategory(String key, String raw) {
        String name = normalizeName(raw);
        if (name.isEmpty()) return false;

        List<String> list = readJsonList(key);

        // prevent duplicates
        for (String s : list) {
            if (s.equalsIgnoreCase(name)) return false;
        }

        list.add(name);
        writeJsonList(key, list);
        return true;
    }

    private boolean removeCategory(String key, String raw) {
        String name = normalizeName(raw);
        if (name.isEmpty()) return false;

        List<String> list = readJsonList(key);

        //  keep at least 1 category
        if (list.size() <= 1) return false;

        // optional: protect "Other"
        if ("Other".equalsIgnoreCase(name)) return false;

        int idx = -1;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).equalsIgnoreCase(name)) {
                idx = i;
                break;
            }
        }
        if (idx == -1) return false;

        list.remove(idx);
        writeJsonList(key, list);
        return true;
    }

    private List<String> readJsonList(String key) {
        String json = sp.getString(key, "[]");
        List<String> out = new ArrayList<>();

        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                String s = arr.optString(i, "").trim();
                if (!s.isEmpty()) out.add(s);
            }
        } catch (Exception ignored) {}

        // if empty, fallback to ensure defaults
        if (out.isEmpty()) {
            ensureDefaults();
            json = sp.getString(key, "[]");
            try {
                JSONArray arr = new JSONArray(json);
                for (int i = 0; i < arr.length(); i++) {
                    String s = arr.optString(i, "").trim();
                    if (!s.isEmpty()) out.add(s);
                }
            } catch (Exception ignored) {}
        }
        return out;
    }

    private void writeJsonList(String key, List<String> list) {
        JSONArray arr = new JSONArray();
        HashSet<String> seen = new HashSet<>();

        for (String s : list) {
            if (s == null) continue;
            String v = s.trim();
            if (v.isEmpty()) continue;

            // avoid exact duplicates
            String low = v.toLowerCase(Locale.US);
            if (seen.contains(low)) continue;
            seen.add(low);

            arr.put(v);
        }

        sp.edit().putString(key, arr.toString()).apply();
    }

    private static String normalizeName(String raw) {
        if (raw == null) return "";
        String s = raw.trim();
        s = s.replaceAll("\\s+", " ");
        return s;
    }

    private static List<String> toList(String[] arr) {
        List<String> list = new ArrayList<>();
        if (arr == null) return list;
        for (String s : arr) {
            if (s != null && !s.trim().isEmpty()) list.add(s.trim());
        }
        return list;
    }
}
