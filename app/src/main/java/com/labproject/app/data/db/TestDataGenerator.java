package com.labproject.app.data.db;

import android.content.Context;
import java.util.Calendar;
import java.util.Random;

/**
 * Test data generator for populating the database with sample transactions
 * Run this once to create test data for months 3-12 of 2025
 * 
 * RECURRENCE TEST DATA:
 * ---------------------
 * This generator creates transactions with different recurrence types:
 * - ONE-TIME: Single occurrence expenses/income
 * - WEEKLY: Repeats every week (e.g., weekly groceries, weekly allowance)
 * - MONTHLY: Repeats every month (e.g., salary, rent, subscriptions)
 * - YEARLY: Repeats every year (e.g., annual bonus, insurance)
 * 
 * TEST DATE RANGES TO VERIFY RECURRENCE LOGIC:
 * ---------------------------------------------
 * 1. Check December 2025 (current month):
 *    - Monthly salary ($4500) should appear
 *    - Weekly groceries ($150/week) should show ~4 entries or calculated weekly amount
 *    - Monthly rent ($1200), Netflix ($15.99), Internet ($79.99) should appear
 *    
 * 2. Check Week view for any week in December:
 *    - Weekly income ($150 freelance) should show full amount
 *    - Monthly expenses should show ~1/4 of monthly amount
 *    
 * 3. Check Yearly view (2025):
 *    - Annual bonus ($5000) should appear once
 *    - Annual insurance ($1200) should appear once
 *    - Monthly salary should calculate as $4500 * 12 = $54,000
 *    - Weekly groceries should calculate as $150 * 52 = $7,800
 *    
 * 4. Specific test transactions added on December 15, 2025:
 *    - Income: $4500 Monthly Salary, $500 Weekly Freelance, $5000 Yearly Bonus, $100 One-time Gift
 *    - Expense: $1200 Monthly Rent, $150 Weekly Groceries, $1200 Yearly Insurance, $250 One-time Electronics
 */
public class TestDataGenerator {

    // Recurrence types
    public static final String ONCE = "once";
    public static final String WEEKLY = "weekly";
    public static final String MONTHLY = "monthly";
    public static final String YEARLY = "yearly";

    public static void generateTestData(Context context) {
        DBHelper db = new DBHelper(context);
        String testEmail = "test@finance.com";
        
        // Create test user if not exists
        if (!db.emailExists(testEmail)) {
            db.registerUser(testEmail, "Test", "User", "Test123");
        }

        Random random = new Random(12345); // Fixed seed for reproducible data

        // Income categories with typical recurrence
        String[] incomeCategories = {"Salary", "Freelance", "Investment", "Gift", "Refund"};
        
        // Expense categories with typical recurrence
        String[] expenseCategories = {"Foods", "Bills", "Transportation", "Education", 
                                      "Healthcare", "Entertainment", "Dining", "Shopping"};

        // ADD SPECIFIC TEST TRANSACTIONS FOR DECEMBER 15, 2025
        // These are the KEY transactions to verify recurrence logic
        long dec15 = getDateMillis(2025, Calendar.DECEMBER, 15);
        
        // --- INCOME with different recurrence types
        db.insertTransaction(testEmail, 1, 4500.00, dec15, "Salary", "Monthly salary from company", MONTHLY);
        db.insertTransaction(testEmail, 1, 500.00, dec15, "Freelance", "Weekly freelance work", WEEKLY);
        db.insertTransaction(testEmail, 1, 5000.00, dec15, "Investment", "Annual bonus / dividend", YEARLY);
        db.insertTransaction(testEmail, 1, 100.00, dec15, "Gift", "One-time birthday gift", ONCE);
        
        //  EXPENSES with different recurrence types
        db.insertTransaction(testEmail, 2, 1200.00, dec15, "Bills", "Monthly rent payment", MONTHLY);
        db.insertTransaction(testEmail, 2, 150.00, dec15, "Foods", "Weekly grocery shopping", WEEKLY);
        db.insertTransaction(testEmail, 2, 1200.00, dec15, "Healthcare", "Yearly health insurance", YEARLY);
        db.insertTransaction(testEmail, 2, 250.00, dec15, "Shopping", "One-time electronics purchase", ONCE);
        
        // More monthly recurring expenses (common bills)
        db.insertTransaction(testEmail, 2, 15.99, dec15, "Entertainment", "Netflix subscription", MONTHLY);
        db.insertTransaction(testEmail, 2, 79.99, dec15, "Bills", "Internet bill", MONTHLY);
        db.insertTransaction(testEmail, 2, 45.00, dec15, "Transportation", "Monthly bus pass", MONTHLY);
        
        // More weekly recurring expenses
        db.insertTransaction(testEmail, 2, 50.00, dec15, "Dining", "Weekly dining out", WEEKLY);
        db.insertTransaction(testEmail, 2, 30.00, dec15, "Transportation", "Weekly gas/fuel", WEEKLY);

        // GENERATE HISTORICAL DATA (March to November 2025)
        // Mix of recurrence types for realistic data
        for (int month = Calendar.MARCH; month <= Calendar.NOVEMBER; month++) {
            
            // --- INCOME TRANSACTIONS
            // Monthly salary (always monthly recurrence)
            double salaryAmount = 4000 + random.nextDouble() * 1500;
            int salaryDay = 1 + random.nextInt(5); // Typically early in month
            db.insertTransaction(testEmail, 1, salaryAmount, 
                getDateMillis(2025, month, salaryDay), 
                "Salary", "Monthly salary", MONTHLY);
            
            // Occasional freelance income (weekly or one-time)
            if (random.nextBoolean()) {
                double freelanceAmount = 200 + random.nextDouble() * 500;
                int freelanceDay = 10 + random.nextInt(15);
                String freelanceRecurrence = random.nextBoolean() ? WEEKLY : ONCE;
                db.insertTransaction(testEmail, 1, freelanceAmount,
                    getDateMillis(2025, month, freelanceDay),
                    "Freelance", freelanceRecurrence.equals(WEEKLY) ? "Weekly project" : "One-time project", 
                    freelanceRecurrence);
            }
            
            // Quarterly investment income (simulate yearly)
            if (month == Calendar.MARCH || month == Calendar.JUNE || 
                month == Calendar.SEPTEMBER) {
                double investmentAmount = 500 + random.nextDouble() * 1000;
                db.insertTransaction(testEmail, 1, investmentAmount,
                    getDateMillis(2025, month, 15),
                    "Investment", "Quarterly dividend", YEARLY);
            }

            // --- EXPENSE TRANSACTIONS
            
            // Monthly bills (rent, utilities - MONTHLY recurrence)
            db.insertTransaction(testEmail, 2, 1100 + random.nextDouble() * 200,
                getDateMillis(2025, month, 1),
                "Bills", "Monthly rent", MONTHLY);
            
            db.insertTransaction(testEmail, 2, 60 + random.nextDouble() * 40,
                getDateMillis(2025, month, 5),
                "Bills", "Electricity bill", MONTHLY);
            
            // Weekly groceries (WEEKLY recurrence)
            for (int week = 0; week < 4; week++) {
                int day = 1 + (week * 7) + random.nextInt(3);
                if (day > 28) day = 28;
                db.insertTransaction(testEmail, 2, 100 + random.nextDouble() * 80,
                    getDateMillis(2025, month, day),
                    "Foods", "Weekly groceries", WEEKLY);
            }
            
            // One-time expenses (various categories)
            int oneTimeCount = 3 + random.nextInt(5);
            for (int i = 0; i < oneTimeCount; i++) {
                double amount = 20 + random.nextDouble() * 200;
                int day = 1 + random.nextInt(28);
                String category = expenseCategories[random.nextInt(expenseCategories.length)];
                db.insertTransaction(testEmail, 2, amount,
                    getDateMillis(2025, month, day),
                    category, "One-time " + category.toLowerCase() + " expense", ONCE);
            }
            
            // Monthly subscriptions
            db.insertTransaction(testEmail, 2, 12.99 + random.nextDouble() * 5,
                getDateMillis(2025, month, 10),
                "Entertainment", "Streaming subscription", MONTHLY);
            
            // Weekly entertainment
            if (random.nextDouble() > 0.3) {
                db.insertTransaction(testEmail, 2, 30 + random.nextDouble() * 50,
                    getDateMillis(2025, month, random.nextInt(28) + 1),
                    "Entertainment", "Weekly movie/activity", WEEKLY);
            }
        }

        // YEARLY EXPENSES (added at specific months)
        // Car insurance in January
        db.insertTransaction(testEmail, 2, 800.00,
            getDateMillis(2025, Calendar.JANUARY, 15),
            "Transportation", "Annual car insurance", YEARLY);
        
        // Health insurance in March
        db.insertTransaction(testEmail, 2, 1500.00,
            getDateMillis(2025, Calendar.MARCH, 1),
            "Healthcare", "Annual health insurance premium", YEARLY);
        
        // Tax payment in April
        db.insertTransaction(testEmail, 2, 2500.00,
            getDateMillis(2025, Calendar.APRIL, 15),
            "Bills", "Annual tax payment", YEARLY);

        // BUDGETS
        db.insertBudget(testEmail, "Foods", 800.00, "monthly", 75);
        db.insertBudget(testEmail, "Bills", 1500.00, "monthly", 80);
        db.insertBudget(testEmail, "Entertainment", 300.00, "monthly", 70);
        db.insertBudget(testEmail, "Transportation", 400.00, "monthly", 85);
        db.insertBudget(testEmail, "Shopping", 500.00, "monthly", 60);
        db.insertBudget(testEmail, "Dining", 400.00, "monthly", 50);
        db.insertBudget(testEmail, "Healthcare", 200.00, "monthly", 90);
    }

    private static long getDateMillis(int year, int month, int day) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month);
        cal.set(Calendar.DAY_OF_MONTH, day);
        cal.set(Calendar.HOUR_OF_DAY, 12);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }
}
