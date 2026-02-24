package com.labproject.app.ui.home;

import android.app.DatePickerDialog;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.labproject.app.utils.ToastHelper;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.labproject.app.R;
import com.labproject.app.data.db.DBHelper;
import com.labproject.app.data.prefs.SettingsManager;
import com.labproject.app.data.session.SessionManager;
import com.labproject.app.databinding.FragmentHomeBinding;
import com.labproject.app.ui.Models.Budget;
import com.labproject.app.ui.Models.MainViewModel;
import com.labproject.app.ui.Models.Transaction;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private MainViewModel vm;

    private DBHelper db;
    private SessionManager session;

    private long startMillis;
    private long endMillis;

    private final SimpleDateFormat rangeFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    private CategoryBreakdownAdapter incAdapter;
    private RecentTransactionsAdapter recentAdapter;
    private BudgetAlertAdapter budgetAlertAdapter;
    private com.labproject.app.ui.budgets.BudgetAdapter budgetSummaryAdapter;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);

        vm = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        db = new DBHelper(requireContext());
        session = new SessionManager(requireContext());

        // adapters
        incAdapter = new CategoryBreakdownAdapter();
        recentAdapter = new RecentTransactionsAdapter();
        budgetAlertAdapter = new BudgetAlertAdapter();
        budgetSummaryAdapter = new com.labproject.app.ui.budgets.BudgetAdapter(
                new com.labproject.app.ui.budgets.BudgetAdapter.Listener() {
                    @Override
                    public void onClick(Budget budget) {
                        // Navigate to budgets fragment or show details
                    }

                    @Override
                    public void onLongClick(Budget budget) {
                        // Do nothing on long click from home
                    }
                });

        binding.rvIncomeBreakdown.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvIncomeBreakdown.setAdapter(incAdapter);

        binding.rvRecent.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvRecent.setAdapter(recentAdapter);

        binding.rvBudgetAlerts.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvBudgetAlerts.setAdapter(budgetAlertAdapter);

        binding.rvBudgetSummary.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvBudgetSummary.setAdapter(budgetSummaryAdapter);

        String p = new SettingsManager(requireContext()).getDefaultPeriod();

        if (p.equals("day")) {
            binding.chipDay.setChecked(true);
            setPeriodDay();
        } else if (p.equals("week")) {
            binding.chipWeek.setChecked(true);
            setPeriodWeek();
        } else {
            binding.chipMonth.setChecked(true);
            setPeriodMonth();
        }

        // chips listeners
        binding.chipDay.setOnClickListener(v -> {
            setPeriodDay();
            renderFromVm();
        });
        binding.chipWeek.setOnClickListener(v -> {
            setPeriodWeek();
            renderFromVm();
        });
        binding.chipMonth.setOnClickListener(v -> {
            setPeriodMonth();
            renderFromVm();
        });
        binding.chipCustom.setOnClickListener(v -> openCustomRangePicker());

        // swipe refresh
        binding.swipeRefresh.setOnRefreshListener(() -> {
            String email = session.getUserEmail();
            if (email != null)
                vm.loadTransactions(db, email);
            else
                binding.swipeRefresh.setRefreshing(false);
        });

        // loading state
        vm.isLoading().observe(getViewLifecycleOwner(), isLoading -> {
            binding.progressLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            if (!isLoading)
                binding.swipeRefresh.setRefreshing(false);
        });

        // when transactions change -> re-render for selected period
        vm.getTransactions().observe(getViewLifecycleOwner(), list -> render(list));

        // ensure data loaded once (if MainActivity already loads, this is safe)
        String email = session.getUserEmail();
        if (email != null)
            vm.loadTransactions(db, email);

        // Quick Add FAB
        binding.fabQuickAdd.setOnClickListener(v -> openQuickAddDialog());

        renderRangeText();
        return binding.getRoot();
    }

    private void openQuickAddDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_quick_add, null);
        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        // Initialize views
        com.google.android.material.chip.ChipGroup chipGroupType = dialogView.findViewById(R.id.chipGroupType);
        com.google.android.material.chip.Chip chipIncome = dialogView.findViewById(R.id.chipIncome);
        com.google.android.material.chip.Chip chipExpense = dialogView.findViewById(R.id.chipExpense);
        com.google.android.material.chip.Chip chipBudget = dialogView.findViewById(R.id.chipBudget);

        android.widget.LinearLayout layoutTransactionFields = dialogView.findViewById(R.id.layoutTransactionFields);
        android.widget.LinearLayout layoutBudgetFields = dialogView.findViewById(R.id.layoutBudgetFields);

        com.google.android.material.textfield.TextInputEditText etAmount = dialogView.findViewById(R.id.etAmount);
        com.google.android.material.textfield.TextInputEditText etDate = dialogView.findViewById(R.id.etDate);
        com.google.android.material.textfield.MaterialAutoCompleteTextView actCategory = dialogView.findViewById(R.id.actCategory);
        com.google.android.material.textfield.TextInputEditText etDescription = dialogView.findViewById(R.id.etDescription);
        
        // Recurrence dropdown for income
        com.google.android.material.textfield.TextInputLayout tilRecurrence = dialogView.findViewById(R.id.tilRecurrence);
        com.google.android.material.textfield.MaterialAutoCompleteTextView actRecurrence = dialogView.findViewById(R.id.actRecurrence);

        com.google.android.material.textfield.TextInputEditText etBudgetLimit = dialogView.findViewById(R.id.etBudgetLimit);
        com.google.android.material.textfield.MaterialAutoCompleteTextView actBudgetCategory = dialogView.findViewById(R.id.actBudgetCategory);
        com.google.android.material.textfield.MaterialAutoCompleteTextView actBudgetPeriod = dialogView.findViewById(R.id.actBudgetPeriod);
        com.google.android.material.textfield.TextInputEditText etAlertThreshold = dialogView.findViewById(R.id.etAlertThreshold);

        com.google.android.material.button.MaterialButton btnAdd = dialogView.findViewById(R.id.btnAdd);
        com.google.android.material.button.MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);

        // Date setup
        final long[] selectedDateMillis = {System.currentTimeMillis()};
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        etDate.setText(sdf.format(new Date(selectedDateMillis[0])));

        etDate.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(selectedDateMillis[0]);
            new DatePickerDialog(requireContext(), (view, year, month, day) -> {
                Calendar selected = Calendar.getInstance();
                selected.set(year, month, day, 0, 0, 0);
                selected.set(Calendar.MILLISECOND, 0);
                selectedDateMillis[0] = selected.getTimeInMillis();
                etDate.setText(sdf.format(new Date(selectedDateMillis[0])));
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
        });

        // Setup category adapters
        SettingsManager settings = new SettingsManager(requireContext());
        android.widget.ArrayAdapter<String> incomeAdapter = new android.widget.ArrayAdapter<>(
                requireContext(), android.R.layout.simple_dropdown_item_1line, settings.getIncomeCategories());
        android.widget.ArrayAdapter<String> expenseAdapter = new android.widget.ArrayAdapter<>(
                requireContext(), android.R.layout.simple_dropdown_item_1line, settings.getExpenseCategories());
        android.widget.ArrayAdapter<String> budgetCatAdapter = new android.widget.ArrayAdapter<>(
                requireContext(), android.R.layout.simple_dropdown_item_1line, settings.getExpenseCategories());
        android.widget.ArrayAdapter<String> periodAdapter = new android.widget.ArrayAdapter<>(
                requireContext(), android.R.layout.simple_dropdown_item_1line, 
                java.util.Arrays.asList("monthly", "weekly", "daily"));

        // Recurrence options for income
        final String[] RECURRENCE_LABELS = {"One-time", "Weekly", "Monthly", "Yearly"};
        final String[] RECURRENCE_VALUES = {
                Transaction.RECURRENCE_ONCE,
                Transaction.RECURRENCE_WEEKLY,
                Transaction.RECURRENCE_MONTHLY,
                Transaction.RECURRENCE_YEARLY
        };
        android.widget.ArrayAdapter<String> recurrenceAdapter = new android.widget.ArrayAdapter<>(
                requireContext(), android.R.layout.simple_dropdown_item_1line, 
                java.util.Arrays.asList(RECURRENCE_LABELS));

        actCategory.setAdapter(incomeAdapter);
        actBudgetCategory.setAdapter(budgetCatAdapter);
        actBudgetPeriod.setAdapter(periodAdapter);
        actBudgetPeriod.setText("monthly", false);
        actRecurrence.setAdapter(recurrenceAdapter);
        actRecurrence.setText(RECURRENCE_LABELS[0], false); // Default to "One-time"

        // Type selection logic - show recurrence for both income and expense
        chipGroupType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipIncome) {
                layoutTransactionFields.setVisibility(View.VISIBLE);
                layoutBudgetFields.setVisibility(View.GONE);
                actCategory.setAdapter(incomeAdapter);
                tilRecurrence.setVisibility(View.VISIBLE); // Show recurrence for income
            } else if (checkedId == R.id.chipExpense) {
                layoutTransactionFields.setVisibility(View.VISIBLE);
                layoutBudgetFields.setVisibility(View.GONE);
                actCategory.setAdapter(expenseAdapter);
                tilRecurrence.setVisibility(View.VISIBLE); // Show recurrence for expense
            } else if (checkedId == R.id.chipBudget) {
                layoutTransactionFields.setVisibility(View.GONE);
                layoutBudgetFields.setVisibility(View.VISIBLE);
            }
        });

        // Add button logic
        btnAdd.setOnClickListener(v -> {
            String email = session.getUserEmail();
            if (email == null) {
                ToastHelper.showError(requireContext(), "Session expired");
                return;
            }

            if (chipIncome.isChecked() || chipExpense.isChecked()) {
                // Add transaction
                String amountStr = etAmount.getText().toString().trim();
                String desc = etDescription.getText().toString().trim();
                String category = actCategory.getText().toString().trim();

                if (amountStr.isEmpty()) {
                    ToastHelper.showError(requireContext(), "Please enter an amount");
                    return;
                }

                double amount;
                try {
                    amount = Double.parseDouble(amountStr);
                } catch (Exception e) {
                    ToastHelper.showError(requireContext(), "Invalid amount");
                    return;
                }

                if (amount <= 0 || amount > 1000000) {
                    ToastHelper.showError(requireContext(), "Amount must be between $0 and $1,000,000");
                    return;
                }

                if (category.isEmpty()) category = "Other";

                if (chipIncome.isChecked()) {
                    // Get recurrence value for income
                    String selectedRecurrence = actRecurrence.getText().toString().trim();
                    String recurrenceValue = Transaction.RECURRENCE_ONCE;
                    for (int i = 0; i < RECURRENCE_LABELS.length; i++) {
                        if (RECURRENCE_LABELS[i].equals(selectedRecurrence)) {
                            recurrenceValue = RECURRENCE_VALUES[i];
                            break;
                        }
                    }
                    vm.addIncome(db, email, amount, selectedDateMillis[0], category, desc, recurrenceValue);
                    ToastHelper.showSuccess(requireContext(), "Income added");
                } else {
                    // Get recurrence value for expense
                    String selectedRecurrence = actRecurrence.getText().toString().trim();
                    String recurrenceValue = Transaction.RECURRENCE_ONCE;
                    for (int i = 0; i < RECURRENCE_LABELS.length; i++) {
                        if (RECURRENCE_LABELS[i].equals(selectedRecurrence)) {
                            recurrenceValue = RECURRENCE_VALUES[i];
                            break;
                        }
                    }
                    vm.addExpense(db, email, amount, selectedDateMillis[0], category, desc, recurrenceValue);
                    ToastHelper.showSuccess(requireContext(), "Expense added");
                }

            } else if (chipBudget.isChecked()) {
                // Add budget
                String limitStr = etBudgetLimit.getText().toString().trim();
                String thresholdStr = etAlertThreshold.getText().toString().trim();
                String category = actBudgetCategory.getText().toString().trim();
                String period = actBudgetPeriod.getText().toString().trim();

                if (limitStr.isEmpty()) {
                    ToastHelper.showError(requireContext(), "Please enter a budget limit");
                    return;
                }

                double limit;
                int threshold;
                try {
                    limit = Double.parseDouble(limitStr);
                    threshold = thresholdStr.isEmpty() ? 50 : Integer.parseInt(thresholdStr);
                } catch (Exception e) {
                    ToastHelper.showError(requireContext(), "Invalid input");
                    return;
                }

                if (limit <= 0 || limit > 1000000) {
                    ToastHelper.showError(requireContext(), "Budget limit must be between $0 and $1,000,000");
                    return;
                }

                if (threshold < 0 || threshold > 100) {
                    ToastHelper.showError(requireContext(), "Alert threshold must be 0-100%");
                    return;
                }

                if (category.isEmpty()) category = "Other";
                if (period.isEmpty()) period = "monthly";

                long id = db.insertBudget(email, category, limit, period, threshold);
                if (id > 0) {
                    ToastHelper.showSuccess(requireContext(), "Budget added");
                } else {
                    ToastHelper.showError(requireContext(), "Failed to add budget");
                }
            }

            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void renderFromVm() {
        List<Transaction> list = vm.getTransactions().getValue();
        if (list != null)
            render(list);
        renderRangeText();
    }

    private void renderRangeText() {
        binding.tvPeriodRange.setText(
                rangeFmt.format(new Date(startMillis)) + "  →  " + rangeFmt.format(new Date(endMillis)));
    }

    private void render(List<Transaction> all) {
        // Determine the current period type based on the date range
        String periodType = getCurrentPeriodType();
        
        // filter by period and only include active transactions (or those that were active during this period)
        List<Transaction> filtered = new ArrayList<>();
        for (Transaction t : all) {
            if (t.dateMillis >= startMillis && t.dateMillis <= endMillis) {
                // Include if active or if it was active during this period
                if (t.isActive || t.wasActiveAt(startMillis)) {
                    filtered.add(t);
                }
            }
        }

        // Also include recurring income that should apply to this period
        // even if the transaction date is outside the period
        List<Transaction> recurringIncomes = new ArrayList<>();
        for (Transaction t : all) {
            if (t.type == Transaction.INCOME && 
                t.recurrence != null && 
                !Transaction.RECURRENCE_ONCE.equals(t.recurrence)) {
                // Check if this recurring income is active and should be counted in this period
                if (t.isActive && shouldIncludeRecurringIncome(t, startMillis, endMillis)) {
                    if (!filtered.contains(t)) {
                        recurringIncomes.add(t);
                    }
                }
            }
        }

        // totals - calculate income and expenses considering recurrence
        double income = 0.0;
        double expenses = 0.0;
        
        for (Transaction t : filtered) {
            if (t.type == Transaction.INCOME) {
                // For recurring income, calculate the effective amount for this period
                income += calculateEffectiveAmountForPeriod(t, periodType);
            } else if (t.type == Transaction.EXPENSE) {
                // For recurring expenses, calculate the effective amount for this period
                expenses += calculateEffectiveAmountForPeriod(t, periodType);
            }
        }
        
        // Add recurring incomes that weren't in the filtered list but apply to this period
        for (Transaction t : recurringIncomes) {
            income += calculateEffectiveAmountForPeriod(t, periodType);
        }
        
        double balance = income - expenses;

        binding.tvIncomeValue.setText(String.format(Locale.US, "$%.2f", income));
        binding.tvExpensesValue.setText(String.format(Locale.US, "$%.2f", expenses));
        binding.tvBalanceValue.setText(String.format(Locale.US, "$%.2f", balance));

        int color = balance < 0
                ? getResources().getColor(R.color.red_negative)
                : getResources().getColor(R.color.colorPrimaryDark);
        binding.tvBalanceValue.setTextColor(color);

        // Calculate and display all-time totals (raw amounts, not adjusted for recurrence)
        calculateAndDisplayAllTimeTotals(all);

        // breakdown maps - use effective amounts for income
        List<CategoryStat> incStats = buildCategoryStatsWithRecurrence(filtered, recurringIncomes, periodType);

        binding.tvEmptyIncomeBreakdown.setVisibility(incStats.isEmpty() ? View.VISIBLE : View.GONE);
        binding.rvIncomeBreakdown.setVisibility(incStats.isEmpty() ? View.GONE : View.VISIBLE);
        incAdapter.submit(incStats, income);

        // Build expense category stats with recurrence
        List<CategoryStat> expStats = buildExpenseCategoryStatsWithRecurrence(filtered, periodType);

        // recent (sorted by date desc, limit 5)
        Collections.sort(filtered, (a, b) -> Long.compare(b.dateMillis, a.dateMillis));
        List<Transaction> recent = filtered.size() > 5 ? filtered.subList(0, 5) : filtered;

        binding.tvEmptyRecent.setVisibility(recent.isEmpty() ? View.VISIBLE : View.GONE);
        binding.rvRecent.setVisibility(recent.isEmpty() ? View.GONE : View.VISIBLE);
        recentAdapter.submit(new ArrayList<>(recent));

        // Load and display budgets
        loadBudgets();

        // Update charts with effective amounts
        updateCharts(filtered, income, expenses, periodType);
    }

    /**
     * Determine the current period type based on the date range.
     */
    private String getCurrentPeriodType() {
        long diff = endMillis - startMillis;
        long dayMillis = 24L * 60 * 60 * 1000;
        long days = diff / dayMillis;
        
        if (days <= 1) {
            return "day";
        } else if (days <= 7) {
            return "week";
        } else if (days <= 31) {
            return "month";
        } else {
            return "year";
        }
    }

    /**
     * Check if a recurring income should be included in the given period.
     */
    private boolean shouldIncludeRecurringIncome(Transaction t, long periodStart, long periodEnd) {
        if (t.recurrence == null || Transaction.RECURRENCE_ONCE.equals(t.recurrence)) {
            return false; // One-time income only counted once
        }
        
        // For recurring income, check if the income started before or during this period
        // and would recur into this period
        return t.dateMillis <= periodEnd;
    }

    /**
     * Calculate the effective amount for a transaction based on period (works for both income and expenses).
     */
    private double calculateEffectiveAmountForPeriod(Transaction t, String periodType) {
        if (t.recurrence == null || Transaction.RECURRENCE_ONCE.equals(t.recurrence)) {
            return t.amount; // One-time transaction
        }
        
        // Use the Transaction's built-in calculation methods
        return t.getEffectiveAmountForPeriod(periodType);
    }

    /**
     * Calculate and display all-time totals (raw amounts from the beginning).
     * Only counts active transactions.
     */
    private void calculateAndDisplayAllTimeTotals(List<Transaction> all) {
        double totalIncome = 0.0;
        double totalExpenses = 0.0;
        
        for (Transaction t : all) {
            // Only count active transactions for all-time totals
            if (!t.isActive) continue;
            
            if (t.type == Transaction.INCOME) {
                totalIncome += t.amount;
            } else if (t.type == Transaction.EXPENSE) {
                totalExpenses += t.amount;
            }
        }
        
        double totalBalance = totalIncome - totalExpenses;
        
        binding.tvAllTimeIncome.setText(String.format(Locale.US, "$%.2f", totalIncome));
        binding.tvAllTimeExpenses.setText(String.format(Locale.US, "$%.2f", totalExpenses));
        binding.tvAllTimeBalance.setText(String.format(Locale.US, "$%.2f", totalBalance));
        
        // Set color for all-time balance
        int balanceColor = totalBalance < 0
                ? getResources().getColor(R.color.red_negative)
                : getResources().getColor(R.color.colorPrimaryDark);
        binding.tvAllTimeBalance.setTextColor(balanceColor);
    }

    /**
     * Build category stats considering recurrence for income.
     */
    private List<CategoryStat> buildCategoryStatsWithRecurrence(List<Transaction> filtered, 
                                                                 List<Transaction> recurringIncomes,
                                                                 String periodType) {
        Map<String, Double> map = new HashMap<>();
        
        // Process filtered transactions
        for (Transaction t : filtered) {
            if (t.type != Transaction.INCOME)
                continue;
            String cat = (t.category == null || t.category.trim().isEmpty()) ? "Other" : t.category;
            double old = map.containsKey(cat) ? map.get(cat) : 0.0;
            double effectiveAmount = calculateEffectiveAmountForPeriod(t, periodType);
            map.put(cat, old + effectiveAmount);
        }
        
        // Process recurring incomes not in filtered list
        for (Transaction t : recurringIncomes) {
            String cat = (t.category == null || t.category.trim().isEmpty()) ? "Other" : t.category;
            double old = map.containsKey(cat) ? map.get(cat) : 0.0;
            double effectiveAmount = calculateEffectiveAmountForPeriod(t, periodType);
            map.put(cat, old + effectiveAmount);
        }

        List<CategoryStat> stats = new ArrayList<>();
        for (String k : map.keySet())
            stats.add(new CategoryStat(k, map.get(k)));

        // sort desc
        stats.sort((a, b) -> Double.compare(b.total, a.total));
        return stats;
    }

    /**
     *   Build category stats considering recurrence for expenses.
     */
    private List<CategoryStat> buildExpenseCategoryStatsWithRecurrence(List<Transaction> filtered, 
                                                                        String periodType) {
        Map<String, Double> map = new HashMap<>();
        
        for (Transaction t : filtered) {
            if (t.type != Transaction.EXPENSE)
                continue;
            String cat = (t.category == null || t.category.trim().isEmpty()) ? "Other" : t.category;
            double old = map.containsKey(cat) ? map.get(cat) : 0.0;
            double effectiveAmount = calculateEffectiveAmountForPeriod(t, periodType);
            map.put(cat, old + effectiveAmount);
        }

        List<CategoryStat> stats = new ArrayList<>();
        for (String k : map.keySet())
            stats.add(new CategoryStat(k, map.get(k)));

        stats.sort((a, b) -> Double.compare(b.total, a.total));
        return stats;
    }

    private void loadBudgets() {
        String email = session.getUserEmail();
        if (email == null)
            return;

        List<Budget> allBudgets = new ArrayList<>();
        List<Budget> alertBudgets = new ArrayList<>();

        Cursor c = db.getBudgetsForUser(email);
        if (c != null && c.moveToFirst()) {
            do {
                int id = c.getInt(0);
                String category = c.getString(1);
                double limit = c.getDouble(2);
                String period = c.getString(3);
                int threshold = c.getInt(4);

                Budget b = new Budget(id, category, limit, period, threshold);

                // Calculate spent amount for current period
                long[] range = getPeriodRange(period);
                double spent = db.getSpentAmountForCategoryAndPeriod(email, category, range[0], range[1]);
                b.spent = spent;

                // Check if alert should trigger
                if (limit > 0) {
                    int percentUsed = (int) ((spent * 100.0) / limit);
                    b.alertTriggered = (percentUsed >= threshold);
                }

                allBudgets.add(b);

                // Add to alerts if triggered or over budget
                if (b.alertTriggered || b.isOverBudget()) {
                    alertBudgets.add(b);
                }

            } while (c.moveToNext());
            c.close();
        }

        // Display budget alerts
        if (!alertBudgets.isEmpty()) {
            binding.cardBudgetAlerts.setVisibility(View.VISIBLE);
            binding.tvBudgetAlertsCount.setText(String.valueOf(alertBudgets.size()));
            budgetAlertAdapter.submitList(alertBudgets);
        } else {
            binding.cardBudgetAlerts.setVisibility(View.GONE);
        }

        // Display budget summary
        if (!allBudgets.isEmpty()) {
            binding.cardBudgetSummary.setVisibility(View.VISIBLE);
            binding.tvEmptyBudgets.setVisibility(View.GONE);
            binding.rvBudgetSummary.setVisibility(View.VISIBLE);
            binding.tvBudgetOverviewCount.setText(String.valueOf(allBudgets.size()));
            budgetSummaryAdapter.submitList(allBudgets);
        } else {
            binding.cardBudgetSummary.setVisibility(View.GONE);
        }
    }

    private long[] getPeriodRange(String period) {
        Calendar cal = Calendar.getInstance();
        long end = cal.getTimeInMillis();

        if (period.equals("daily")) {
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
        } else if (period.equals("weekly")) {
            cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
        } else { // monthly
            cal.set(Calendar.DAY_OF_MONTH, 1);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
        }

        long start = cal.getTimeInMillis();
        return new long[] { start, end };
    }

    private void updateCharts(List<Transaction> transactions, double totalIncome, double totalExpenses, String periodType) {
        // Pie Chart - Income vs Expenses
        PieChart pieChart = binding.pieChart;

        if (totalIncome == 0 && totalExpenses == 0) {
            pieChart.clear();
            pieChart.setNoDataText("No data available");
            pieChart.setNoDataTextColor(getResources().getColor(R.color.textSecondary));
            pieChart.invalidate();
        } else {
            ArrayList<PieEntry> pieEntries = new ArrayList<>();
            if (totalIncome > 0)
                pieEntries.add(new PieEntry((float)totalIncome, "Income"));
            if (totalExpenses > 0)
                pieEntries.add(new PieEntry((float)totalExpenses, "Expenses"));

            PieDataSet pieDataSet = new PieDataSet(pieEntries, "");

            ArrayList<Integer> colors = new ArrayList<>();
            colors.add(getResources().getColor(R.color.colorPrimary));
            colors.add(getResources().getColor(R.color.red_negative));
            pieDataSet.setColors(colors);

            pieDataSet.setValueTextSize(14f);
            pieDataSet.setValueTextColor(getResources().getColor(R.color.textPrimary));

            PieData pieData = new PieData(pieDataSet);
            pieChart.setData(pieData);
            pieChart.getDescription().setEnabled(false);
            pieChart.setDrawHoleEnabled(true);
            pieChart.setHoleRadius(40f);
            pieChart.setTransparentCircleRadius(45f);
            pieChart.getLegend().setTextColor(getResources().getColor(R.color.textPrimary));
            pieChart.animateY(1000);
            pieChart.invalidate();
        }

        // Bar Chart - Expenses by Category (using effective amounts)
        BarChart barChart = binding.barChart;

        // Build category totals for expenses using effective amounts
        Map<String, Double> categoryTotals = new HashMap<>();
        for (Transaction t : transactions) {
            if (t.type == Transaction.EXPENSE) {
                String cat = (t.category == null || t.category.trim().isEmpty()) ? "Other" : t.category;
                double current = categoryTotals.containsKey(cat) ? categoryTotals.get(cat) : 0.0;
                double effectiveAmount = calculateEffectiveAmountForPeriod(t, periodType);
                categoryTotals.put(cat, current + effectiveAmount);
            }
        }

        if (categoryTotals.isEmpty()) {
            barChart.clear();
            barChart.setNoDataText("No expense data");
            barChart.setNoDataTextColor(getResources().getColor(R.color.textSecondary));            barChart.invalidate();
        } else {
            ArrayList<BarEntry> barEntries = new ArrayList<>();
            ArrayList<String> labels = new ArrayList<>();

            int index = 0;
            for (Map.Entry<String, Double> entry : categoryTotals.entrySet()) {
                barEntries.add(new BarEntry(index, entry.getValue().floatValue()));
                labels.add(entry.getKey());
                index++;
            }

            BarDataSet barDataSet = new BarDataSet(barEntries, "Expenses");
            barDataSet.setColors(ColorTemplate.MATERIAL_COLORS);
            barDataSet.setValueTextSize(12f);
            barDataSet.setValueTextColor(getResources().getColor(R.color.textPrimary));

            BarData barData = new BarData(barDataSet);
            barChart.setData(barData);
            barChart.getDescription().setEnabled(false);
            barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
            barChart.getXAxis().setGranularity(1f);
            barChart.getXAxis().setTextColor(getResources().getColor(R.color.textPrimary));
            barChart.getAxisLeft().setTextColor(getResources().getColor(R.color.textPrimary));
            barChart.getAxisRight().setEnabled(false);
            barChart.getLegend().setTextColor(getResources().getColor(R.color.textPrimary));
            barChart.animateY(1000);
            barChart.invalidate();
        }
    }

    // Period logic

    private void setPeriodDay() {
        Calendar c = Calendar.getInstance();
        setStartOfDay(c);
        startMillis = c.getTimeInMillis();
        endMillis = endOfDayMillis(startMillis);
        renderRangeText();
    }

    private void setPeriodWeek() {
        Calendar c = Calendar.getInstance();
        // week start: Monday
        c.setFirstDayOfWeek(Calendar.MONDAY);
        c.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        setStartOfDay(c);
        startMillis = c.getTimeInMillis();
        endMillis = startMillis + (7L * 24 * 60 * 60 * 1000) - 1;
        renderRangeText();
    }

    private void setPeriodMonth() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.DAY_OF_MONTH, 1);
        setStartOfDay(c);
        startMillis = c.getTimeInMillis();

        Calendar end = Calendar.getInstance();
        end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH));
        end.set(Calendar.HOUR_OF_DAY, 23);
        end.set(Calendar.MINUTE, 59);
        end.set(Calendar.SECOND, 59);
        end.set(Calendar.MILLISECOND, 999);
        endMillis = end.getTimeInMillis();

        renderRangeText();
    }

    private void openCustomRangePicker() {

        MaterialDatePicker<Pair<Long, Long>> picker = MaterialDatePicker.Builder.dateRangePicker()
                .setTheme(R.style.AppMaterialDatePicker)
                .setSelection(
                        new Pair<>(startMillis, endMillis))
                .build();

        picker.show(getParentFragmentManager(), "range");

        picker.addOnPositiveButtonClickListener(selection -> {
            if (selection.first == null || selection.second == null)
                return;

            startMillis = selection.first;
            endMillis = selection.second + (24L * 60 * 60 * 1000) - 1;

            renderFromVm();
        });
    }

    private void setStartOfDay(Calendar c) {
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
    }

    private long endOfDayMillis(long startOfDay) {
        return startOfDay + (24L * 60 * 60 * 1000) - 1;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
