package com.labproject.app.data.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class DBHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "labproject.db";
    private static final int DB_VERSION = 7;

    // USERS TABLE
    public static final String T_USERS = "users";
    public static final String C_EMAIL = "email";
    public static final String C_FIRST = "first_name";
    public static final String C_LAST = "last_name";
    public static final String C_PASSWORD = "password";
    
    // TRANSACTIONS TABLE
    public static final String T_TX = "transactions";
    public static final String TX_ID = "id";
    public static final String TX_EMAIL = "email";
    public static final String TX_TYPE = "type";           // 1 income, 2 expense
    public static final String TX_AMOUNT = "amount";
    public static final String TX_DATE = "date_millis";
    public static final String TX_CATEGORY = "category";
    public static final String TX_DESC = "description";
    public static final String TX_RECURRENCE = "recurrence"; // "once", "weekly", "monthly", "yearly"
    public static final String TX_IS_ACTIVE = "is_active";   // 1 = active, 0 = inactive
    public static final String TX_END_DATE = "end_date_millis"; // -1 = no end date (infinite)
    
    // BUDGETS TABLE
    public static final String T_BUDGETS = "budgets";
    public static final String B_ID = "id";
    public static final String B_EMAIL = "email";
    public static final String B_CATEGORY = "category";
    public static final String B_LIMIT = "limit_amount";
    public static final String B_PERIOD = "period";        // "monthly", "weekly", "daily"
    public static final String B_ALERT_THRESHOLD = "alert_threshold";  // percentage

    public DBHelper(@Nullable Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        String createUsers =
                "CREATE TABLE " + T_USERS + " (" +
                        C_EMAIL + " TEXT PRIMARY KEY, " +
                        C_FIRST + " TEXT NOT NULL, " +
                        C_LAST + " TEXT NOT NULL, " +
                        C_PASSWORD + " TEXT NOT NULL" +
                        ");";
        db.execSQL(createUsers);

        String createTransactions =
                "CREATE TABLE " + T_TX + " (" +
                        TX_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        TX_EMAIL + " TEXT NOT NULL, " +
                        TX_TYPE + " INTEGER NOT NULL, " +
                        TX_AMOUNT + " REAL NOT NULL, " +
                        TX_DATE + " INTEGER NOT NULL, " +
                        TX_CATEGORY + " TEXT NOT NULL, " +
                        TX_DESC + " TEXT, " +
                        TX_RECURRENCE + " TEXT DEFAULT 'once', " +
                        TX_IS_ACTIVE + " INTEGER DEFAULT 1, " +
                        TX_END_DATE + " INTEGER DEFAULT -1" +
                        ");";
        db.execSQL(createTransactions);

        String createBudgets =
                "CREATE TABLE " + T_BUDGETS + " (" +
                        B_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        B_EMAIL + " TEXT NOT NULL, " +
                        B_CATEGORY + " TEXT NOT NULL, " +
                        B_LIMIT + " REAL NOT NULL, " +
                        B_PERIOD + " TEXT NOT NULL, " +
                        B_ALERT_THRESHOLD + " INTEGER DEFAULT 50" +
                        ");";
        db.execSQL(createBudgets);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + T_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + T_TX);
        db.execSQL("DROP TABLE IF EXISTS " + T_BUDGETS);
        onCreate(db);
    }

    //  AUTH

    public boolean registerUser(String email, String first, String last, String password) {
        if (emailExists(email)) return false;

        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(C_EMAIL, email.trim().toLowerCase());
        cv.put(C_FIRST, first.trim());
        cv.put(C_LAST, last.trim());
        cv.put(C_PASSWORD, password);

        long res = db.insert(T_USERS, null, cv);
        return res != -1;
    }

    public boolean emailExists(String email) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT " + C_EMAIL + " FROM " + T_USERS + " WHERE " + C_EMAIL + "=?",
                new String[]{email.trim().toLowerCase()}
        );
        boolean exists = c.moveToFirst();
        c.close();
        return exists;
    }

    public boolean loginValid(String email, String password) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT " + C_EMAIL + " FROM " + T_USERS +
                        " WHERE " + C_EMAIL + "=? AND " + C_PASSWORD + "=?",
                new String[]{email.trim().toLowerCase(), password}
        );
        boolean ok = c.moveToFirst();
        c.close();
        return ok;
    }

    //  PROFILE

    public Cursor getUserByEmail(String email) {
        SQLiteDatabase db = getReadableDatabase();
        return db.rawQuery(
                "SELECT " + C_FIRST + ", " + C_LAST + ", " + C_EMAIL +
                        " FROM " + T_USERS + " WHERE " + C_EMAIL + "=?",
                new String[]{email.trim().toLowerCase()}
        );
    }

    public boolean updateUserName(String email, String first, String last) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues cv = new ContentValues();
        cv.put(C_FIRST, first.trim());
        cv.put(C_LAST, last.trim());

        int rows = db.update(
                T_USERS,
                cv,
                C_EMAIL + "=?",
                new String[]{email.trim().toLowerCase()}
        );

        return rows > 0;
    }

    //  TRANSACTIONS CRUD

    public long insertTransaction(String email, int type, double amount, long dateMillis, String category, String desc) {
        return insertTransaction(email, type, amount, dateMillis, category, desc, "once", true, -1);
    }

    public long insertTransaction(String email, int type, double amount, long dateMillis, String category, String desc, String recurrence) {
        return insertTransaction(email, type, amount, dateMillis, category, desc, recurrence, true, -1);
    }

    public long insertTransaction(String email, int type, double amount, long dateMillis, String category, 
                                  String desc, String recurrence, boolean isActive, long endDateMillis) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues cv = new ContentValues();
        cv.put(TX_EMAIL, email.trim().toLowerCase());
        cv.put(TX_TYPE, type);
        cv.put(TX_AMOUNT, amount);
        cv.put(TX_DATE, dateMillis);
        cv.put(TX_CATEGORY, category);
        cv.put(TX_DESC, desc);
        cv.put(TX_RECURRENCE, recurrence == null ? "once" : recurrence);
        cv.put(TX_IS_ACTIVE, isActive ? 1 : 0);
        cv.put(TX_END_DATE, endDateMillis);

        return db.insert(T_TX, null, cv);
    }

    public boolean updateTransaction(String email, int id, int type, double amount, long dateMillis, String category, String desc) {
        return updateTransaction(email, id, type, amount, dateMillis, category, desc, "once", true, -1);
    }

    public boolean updateTransaction(String email, int id, int type, double amount, long dateMillis, String category, String desc, String recurrence) {
        return updateTransaction(email, id, type, amount, dateMillis, category, desc, recurrence, true, -1);
    }

    public boolean updateTransaction(String email, int id, int type, double amount, long dateMillis, 
                                     String category, String desc, String recurrence, boolean isActive, long endDateMillis) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues cv = new ContentValues();
        cv.put(TX_TYPE, type);
        cv.put(TX_AMOUNT, amount);
        cv.put(TX_DATE, dateMillis);
        cv.put(TX_CATEGORY, category);
        cv.put(TX_DESC, desc);
        cv.put(TX_RECURRENCE, recurrence == null ? "once" : recurrence);
        cv.put(TX_IS_ACTIVE, isActive ? 1 : 0);
        cv.put(TX_END_DATE, endDateMillis);

        int rows = db.update(
                T_TX,
                cv,
                TX_ID + "=? AND " + TX_EMAIL + "=?",
                new String[]{String.valueOf(id), email.trim().toLowerCase()}
        );

        return rows > 0;
    }

    /**
     * Deactivate a transaction (set is_active to 0 and optionally set end date)
     */
    public boolean deactivateTransaction(String email, int id, long endDateMillis) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues cv = new ContentValues();
        cv.put(TX_IS_ACTIVE, 0);
        cv.put(TX_END_DATE, endDateMillis);

        int rows = db.update(
                T_TX,
                cv,
                TX_ID + "=? AND " + TX_EMAIL + "=?",
                new String[]{String.valueOf(id), email.trim().toLowerCase()}
        );

        return rows > 0;
    }

    /**
     * Reactivate a transaction (set is_active to 1)
     */
    public boolean reactivateTransaction(String email, int id) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues cv = new ContentValues();
        cv.put(TX_IS_ACTIVE, 1);
        cv.put(TX_END_DATE, -1); // Remove end date when reactivating

        int rows = db.update(
                T_TX,
                cv,
                TX_ID + "=? AND " + TX_EMAIL + "=?",
                new String[]{String.valueOf(id), email.trim().toLowerCase()}
        );

        return rows > 0;
    }

    public boolean deleteTransaction(String email, int id) {
        SQLiteDatabase db = getWritableDatabase();
        int rows = db.delete(
                T_TX,
                TX_ID + "=? AND " + TX_EMAIL + "=?",
                new String[]{String.valueOf(id), email.trim().toLowerCase()}
        );
        return rows > 0;
    }

    public Cursor getTransactionsForUser(String email) {
        SQLiteDatabase db = getReadableDatabase();
        return db.rawQuery(
                "SELECT " + TX_ID + ", " + TX_AMOUNT + ", " + TX_DATE + ", " + TX_CATEGORY + ", " + 
                        TX_DESC + ", " + TX_TYPE + ", " + TX_RECURRENCE + ", " + TX_IS_ACTIVE + ", " + TX_END_DATE +
                        " FROM " + T_TX +
                        " WHERE " + TX_EMAIL + "=? ORDER BY " + TX_DATE + " DESC",
                new String[]{email.trim().toLowerCase()}
        );
    }

    //  PASSWORD

    public boolean checkPassword(String email, String currentPassword) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT " + C_EMAIL + " FROM " + T_USERS +
                        " WHERE " + C_EMAIL + "=? AND " + C_PASSWORD + "=?",
                new String[]{email.trim().toLowerCase(), currentPassword}
        );
        boolean ok = c.moveToFirst();
        c.close();
        return ok;
    }

    public boolean updatePassword(String email, String newPassword) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues cv = new ContentValues();
        cv.put(C_PASSWORD, newPassword);

        int rows = db.update(
                T_USERS,
                cv,
                C_EMAIL + "=?",
                new String[]{email.trim().toLowerCase()}
        );
        return rows > 0;
    }

    //  BUDGETS CRUD

    public long insertBudget(String email, String category, double limitAmount, String period, int alertThreshold) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues cv = new ContentValues();
        cv.put(B_EMAIL, email.trim().toLowerCase());
        cv.put(B_CATEGORY, category);
        cv.put(B_LIMIT, limitAmount);
        cv.put(B_PERIOD, period);
        cv.put(B_ALERT_THRESHOLD, alertThreshold);

        return db.insert(T_BUDGETS, null, cv);
    }

    public boolean updateBudget(String email, int id, String category, double limitAmount, String period, int alertThreshold) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues cv = new ContentValues();
        cv.put(B_CATEGORY, category);
        cv.put(B_LIMIT, limitAmount);
        cv.put(B_PERIOD, period);
        cv.put(B_ALERT_THRESHOLD, alertThreshold);

        int rows = db.update(
                T_BUDGETS,
                cv,
                B_ID + "=? AND " + B_EMAIL + "=?",
                new String[]{String.valueOf(id), email.trim().toLowerCase()}
        );

        return rows > 0;
    }

    public boolean deleteBudget(String email, int id) {
        SQLiteDatabase db = getWritableDatabase();
        int rows = db.delete(
                T_BUDGETS,
                B_ID + "=? AND " + B_EMAIL + "=?",
                new String[]{String.valueOf(id), email.trim().toLowerCase()}
        );
        return rows > 0;
    }

    public Cursor getBudgetsForUser(String email) {
        SQLiteDatabase db = getReadableDatabase();
        return db.rawQuery(
                "SELECT " + B_ID + ", " + B_CATEGORY + ", " + B_LIMIT + ", " + B_PERIOD + ", " + B_ALERT_THRESHOLD +
                        " FROM " + T_BUDGETS +
                        " WHERE " + B_EMAIL + "=?",
                new String[]{email.trim().toLowerCase()}
        );
    }

    public double getSpentAmountForCategoryAndPeriod(String email, String category, long startMillis, long endMillis) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT SUM(" + TX_AMOUNT + ") FROM " + T_TX +
                        " WHERE " + TX_EMAIL + "=? AND " + TX_CATEGORY + "=? AND " + TX_TYPE + "=? AND " +
                        TX_DATE + " >= ? AND " + TX_DATE + " <= ?",
                new String[]{
                        email.trim().toLowerCase(),
                        category,
                        String.valueOf(2), // EXPENSE
                        String.valueOf(startMillis),
                        String.valueOf(endMillis)
                }
        );

        double total = 0.0;
        if (c != null && c.moveToFirst()) {
            total = c.getDouble(0);
            c.close();
        }
        return total;
    }

}
