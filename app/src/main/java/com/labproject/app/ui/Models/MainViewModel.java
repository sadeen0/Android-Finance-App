package com.labproject.app.ui.Models;

import android.database.Cursor;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.labproject.app.data.db.DBHelper;

import java.util.ArrayList;
import java.util.List;

public class MainViewModel extends ViewModel {

    // Loading
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    public LiveData<Boolean> isLoading() { return loading; }

    // Totals
    private final MutableLiveData<Double> income = new MutableLiveData<>(0.0);
    private final MutableLiveData<Double> expenses = new MutableLiveData<>(0.0);

    // Transactions
    private final MutableLiveData<List<Transaction>> transactions =
            new MutableLiveData<>(new ArrayList<>());

    public LiveData<List<Transaction>> getTransactions() { return transactions; }
    public LiveData<Double> getIncome() { return income; }
    public LiveData<Double> getExpenses() { return expenses; }

    // Balance = income - expenses
    private final MediatorLiveData<Double> totalBalance = new MediatorLiveData<>();
    public LiveData<Double> getTotalBalance() { return totalBalance; }

    public MainViewModel() {
        totalBalance.addSource(income, i ->
                totalBalance.setValue(
                        (i == null ? 0 : i) - (expenses.getValue() == null ? 0 : expenses.getValue())
                )
        );
        totalBalance.addSource(expenses, e ->
                totalBalance.setValue(
                        (income.getValue() == null ? 0 : income.getValue()) - (e == null ? 0 : e)
                )
        );
    }

    // DB Actions

    /**
     * Load all transactions for user, then calculate totals.
     * Uses small delay so you can SEE the loading progress bar.
     */
    public void loadTransactions(DBHelper db, String email) {
        if (email == null || email.trim().isEmpty()) return;

        loading.setValue(true);

        // demo delay
        new Handler(Looper.getMainLooper()).postDelayed(() -> {

            List<Transaction> list = new ArrayList<>();
            Cursor c = db.getTransactionsForUser(email);

            if (c != null && c.moveToFirst()) {
                do {
                    int id = c.getInt(c.getColumnIndexOrThrow("id"));
                    double amount = c.getDouble(c.getColumnIndexOrThrow("amount"));
                    long dateMillis = c.getLong(c.getColumnIndexOrThrow("date_millis"));
                    String category = c.getString(c.getColumnIndexOrThrow("category"));
                    String desc = c.getString(c.getColumnIndexOrThrow("description"));
                    int type = c.getInt(c.getColumnIndexOrThrow("type"));
                    
                    // Get recurrence, default to "once" if not found
                    String recurrence = Transaction.RECURRENCE_ONCE;
                    int recurrenceIndex = c.getColumnIndex("recurrence");
                    if (recurrenceIndex >= 0) {
                        String rec = c.getString(recurrenceIndex);
                        if (rec != null && !rec.isEmpty()) {
                            recurrence = rec;
                        }
                    }

                    // Get isActive, default to true
                    boolean isActive = true;
                    int isActiveIndex = c.getColumnIndex("is_active");
                    if (isActiveIndex >= 0) {
                        isActive = c.getInt(isActiveIndex) == 1;
                    }

                    // Get endDateMillis, default to -1 (no end date)
                    long endDateMillis = Transaction.NO_END_DATE;
                    int endDateIndex = c.getColumnIndex("end_date_millis");
                    if (endDateIndex >= 0) {
                        endDateMillis = c.getLong(endDateIndex);
                    }

                    list.add(new Transaction(id, amount, dateMillis, category, desc, type, recurrence, isActive, endDateMillis));
                } while (c.moveToNext());

                c.close();
            }

            transactions.setValue(list);
            recalcTotals(list);

            loading.setValue(false);

        }, 500);

    }

    private void recalcTotals(List<Transaction> list) {
        double in = 0.0, ex = 0.0;
        for (Transaction t : list) {
            if (t.type == Transaction.INCOME) in += t.amount;
            else if (t.type == Transaction.EXPENSE) ex += t.amount;
        }
        income.setValue(in);
        expenses.setValue(ex);
    }

    // Add income (legacy, no recurrence)
    public void addIncome(DBHelper db, String email, double amount, long dateMillis, String category, String desc) {
        addIncome(db, email, amount, dateMillis, category, desc, Transaction.RECURRENCE_ONCE, true, Transaction.NO_END_DATE);
    }

    // Add income with recurrence
    public void addIncome(DBHelper db, String email, double amount, long dateMillis, String category, String desc, String recurrence) {
        addIncome(db, email, amount, dateMillis, category, desc, recurrence, true, Transaction.NO_END_DATE);
    }

    // Add income with all fields
    public void addIncome(DBHelper db, String email, double amount, long dateMillis, String category, 
                          String desc, String recurrence, boolean isActive, long endDateMillis) {
        if (email == null) return;
        db.insertTransaction(email, Transaction.INCOME, amount, dateMillis, category, desc, recurrence, isActive, endDateMillis);
        loadTransactions(db, email);
    }

    //  Add expense (legacy, no recurrence)
    public void addExpense(DBHelper db, String email, double amount, long dateMillis, String category, String desc) {
        addExpense(db, email, amount, dateMillis, category, desc, Transaction.RECURRENCE_ONCE, true, Transaction.NO_END_DATE);
    }

    //  Add expense with recurrence
    public void addExpense(DBHelper db, String email, double amount, long dateMillis, String category, String desc, String recurrence) {
        addExpense(db, email, amount, dateMillis, category, desc, recurrence, true, Transaction.NO_END_DATE);
    }

    //  Add expense with all fields
    public void addExpense(DBHelper db, String email, double amount, long dateMillis, String category, 
                           String desc, String recurrence, boolean isActive, long endDateMillis) {
        if (email == null) return;
        db.insertTransaction(email, Transaction.EXPENSE, amount, dateMillis, category, desc, recurrence, isActive, endDateMillis);
        loadTransactions(db, email);
    }

    //  Update
    public void updateTransaction(DBHelper db, String email, Transaction t) {
        if (email == null || t == null) return;
        String recurrence = (t.recurrence == null) ? Transaction.RECURRENCE_ONCE : t.recurrence;
        db.updateTransaction(email, t.id, t.type, t.amount, t.dateMillis, t.category, t.description, 
                            recurrence, t.isActive, t.endDateMillis);
        loadTransactions(db, email);
    }

    //  Deactivate (marks as inactive with end date set to current time)
    public void deactivateTransaction(DBHelper db, String email, int txId) {
        if (email == null) return;
        long now = System.currentTimeMillis();
        db.deactivateTransaction(email, txId, now);
        loadTransactions(db, email);
    }

    //  Reactivate (marks as active, removes end date)
    public void reactivateTransaction(DBHelper db, String email, int txId) {
        if (email == null) return;
        db.reactivateTransaction(email, txId);
        loadTransactions(db, email);
    }

    //  Delete
    public void deleteTransaction(DBHelper db, String email, int txId) {
        if (email == null) return;
        db.deleteTransaction(email, txId);
        loadTransactions(db, email);
    }
}

