package com.labproject.app.ui.Models;

public class Budget {

    public int id;
    public String category;
    public double limitAmount;
    public String period;          // "monthly", "weekly", "daily"
    public int alertThreshold;     // percentage

    // runtime data
    public double spent;           // calculated from transactions
    public boolean alertTriggered; // true if spent >= limit * (alertThreshold / 100)

    public Budget(int id, String category, double limitAmount, String period, int alertThreshold) {
        this.id = id;
        this.category = category;
        this.limitAmount = limitAmount;
        this.period = period;
        this.alertThreshold = alertThreshold;
        this.spent = 0.0;
        this.alertTriggered = false;
    }

    public double getRemaining() {
        return limitAmount - spent;
    }

    public int getPercentageUsed() {
        if (limitAmount == 0) return 0;
        return (int) ((spent * 100.0) / limitAmount);
    }

    public boolean isOverBudget() {
        return spent > limitAmount;
    }
}
