package com.labproject.app.ui.Models;

public class Transaction {

    public static final int INCOME = 1;
    public static final int EXPENSE = 2;
    public static final String RECURRENCE_ONCE = "once";
    public static final String RECURRENCE_WEEKLY = "weekly";
    public static final String RECURRENCE_MONTHLY = "monthly";
    public static final String RECURRENCE_YEARLY = "yearly";


    public static final long NO_END_DATE = -1;  // End date constant for "infinite" (no end date)

    public int id;
    public double amount;
    public long dateMillis;
    public String category;
    public String description;
    public int type;
    public String recurrence; // For income and expenses: "once", "weekly", "monthly", "yearly"
    public boolean isActive;  // Whether the transaction is active
    public long endDateMillis; // End date for recurring transactions (-1 means no end/infinite)

    public Transaction(int id, double amount, long dateMillis, String category, String description, int type) {
        this.id = id;
        this.amount = amount;
        this.dateMillis = dateMillis;
        this.category = category;
        this.description = description;
        this.type = type;
        this.recurrence = RECURRENCE_ONCE; // Default to one-time
        this.isActive = true;  // Default to active
        this.endDateMillis = NO_END_DATE; // Default to no end date
    }

    public Transaction(int id, double amount, long dateMillis, String category, String description, int type, String recurrence) {
        this.id = id;
        this.amount = amount;
        this.dateMillis = dateMillis;
        this.category = category;
        this.description = description;
        this.type = type;
        this.recurrence = (recurrence == null || recurrence.isEmpty()) ? RECURRENCE_ONCE : recurrence;
        this.isActive = true;  // Default to active
        this.endDateMillis = NO_END_DATE; // Default to no end date
    }

    public Transaction(int id, double amount, long dateMillis, String category, String description, 
                       int type, String recurrence, boolean isActive, long endDateMillis) {
        this.id = id;
        this.amount = amount;
        this.dateMillis = dateMillis;
        this.category = category;
        this.description = description;
        this.type = type;
        this.recurrence = (recurrence == null || recurrence.isEmpty()) ? RECURRENCE_ONCE : recurrence;
        this.isActive = isActive;
        this.endDateMillis = endDateMillis;
    }

    /**
     * Check if the transaction is currently active (considering both isActive flag and end date)
     * @param currentTimeMillis the current time in milliseconds
     * @return true if the transaction should be considered active
     */
    public boolean isActiveAt(long currentTimeMillis) {
        if (!isActive) return false;
        if (endDateMillis == NO_END_DATE) return true;
        return currentTimeMillis <= endDateMillis;
    }

    /**
     * Check if the transaction was active at a specific date
     * (the date is between start date and end date, and the transaction was active)
     * @param dateToCheck the date to check in milliseconds
     * @return true if the transaction was active at that date
     */
    public boolean wasActiveAt(long dateToCheck) {
        // Must be after or on start date
        if (dateToCheck < dateMillis) return false;
        // If no end date and is active, always true for dates after start
        if (endDateMillis == NO_END_DATE) return true;
        // Check if date is before or on end date
        return dateToCheck <= endDateMillis;
    }

    /**
     * Check if the transaction has an end date set
     */
    public boolean hasEndDate() {
        return endDateMillis != NO_END_DATE;
    }

    /**
     * Calculate the effective amount for a given period based on recurrence.
     * Works for both income and expenses.
     * @param periodType "day", "week", "month", "year"
     * @return The effective amount for that period
     */
    public double getEffectiveAmountForPeriod(String periodType) {
        if (recurrence == null) {
            return amount;
        }

        switch (periodType) {
            case "day":
                return getDailyAmount();
            case "week":
                return getWeeklyAmount();
            case "month":
                return getMonthlyAmount();
            case "year":
                return getYearlyAmount();
            default:
                return amount;
        }
    }

    /**
     * Get the daily equivalent of the income.
     */
    public double getDailyAmount() {
        if (recurrence == null) return amount;
        switch (recurrence) {
            case RECURRENCE_ONCE:
                return amount; // One-time income, counted fully on that day
            case RECURRENCE_WEEKLY:
                return amount / 7.0;
            case RECURRENCE_MONTHLY:
                return amount / 30.0;
            case RECURRENCE_YEARLY:
                return amount / 365.0;
            default:
                return amount;
        }
    }

    /**
     * Get the weekly equivalent of the income.
     */
    public double getWeeklyAmount() {
        if (recurrence == null) return amount;
        switch (recurrence) {
            case RECURRENCE_ONCE:
                return amount; // One-time income, counted fully in that week
            case RECURRENCE_WEEKLY:
                return amount;
            case RECURRENCE_MONTHLY:
                return amount / 4.33; // Average weeks per month
            case RECURRENCE_YEARLY:
                return amount / 52.0;
            default:
                return amount;
        }
    }

    /**
     * Get the monthly equivalent of the income.
     */
    public double getMonthlyAmount() {
        if (recurrence == null) return amount;
        switch (recurrence) {
            case RECURRENCE_ONCE:
                return amount; // One-time income, counted fully in that month
            case RECURRENCE_WEEKLY:
                return amount * 4.33;
            case RECURRENCE_MONTHLY:
                return amount;
            case RECURRENCE_YEARLY:
                return amount / 12.0;
            default:
                return amount;
        }
    }

    /**
     * Get the yearly equivalent of the income.
     */
    public double getYearlyAmount() {
        if (recurrence == null) return amount;
        switch (recurrence) {
            case RECURRENCE_ONCE:
                return amount; // One-time income, counted fully in that year
            case RECURRENCE_WEEKLY:
                return amount * 52.0;
            case RECURRENCE_MONTHLY:
                return amount * 12.0;
            case RECURRENCE_YEARLY:
                return amount;
            default:
                return amount;
        }
    }

    /**
     * Get a display label for the recurrence type.
     */
    public String getRecurrenceLabel() {
        if (recurrence == null) return "One-time";
        switch (recurrence) {
            case RECURRENCE_ONCE:
                return "One-time";
            case RECURRENCE_WEEKLY:
                return "Weekly";
            case RECURRENCE_MONTHLY:
                return "Monthly";
            case RECURRENCE_YEARLY:
                return "Yearly";
            default:
                return "One-time";
        }
    }
}
