package com.labproject.app.ui.statistics;

import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.labproject.app.R;
import com.labproject.app.data.db.DBHelper;
import com.labproject.app.data.session.SessionManager;
import com.labproject.app.databinding.FragmentStatisticsBinding;
import com.labproject.app.ui.Models.MainViewModel;
import com.labproject.app.ui.Models.Transaction;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StatisticsFragment extends Fragment {

    private FragmentStatisticsBinding binding;
    private MainViewModel vm;
    private DBHelper db;
    private SessionManager session;
    private DecimalFormat currencyFormat = new DecimalFormat("$#,##0.00");

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        binding = FragmentStatisticsBinding.inflate(inflater, container, false);

        vm = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        db = new DBHelper(requireContext());
        session = new SessionManager(requireContext());

        String email = session.getUserEmail();
        if (email != null) {
            vm.loadTransactions(db, email);
        }

        vm.getTransactions().observe(getViewLifecycleOwner(), list -> {
            if (list != null && !list.isEmpty()) {
                calculateStatistics(list);
                setupCharts(list);
                binding.tvEmptyStats.setVisibility(View.GONE);
                binding.scrollView.setVisibility(View.VISIBLE);
            } else {
                binding.tvEmptyStats.setVisibility(View.VISIBLE);
                binding.scrollView.setVisibility(View.GONE);
            }
        });

        return binding.getRoot();
    }

    private void calculateStatistics(List<Transaction> transactions) {
        Calendar cal = Calendar.getInstance();
        int currentMonth = cal.get(Calendar.MONTH);
        int currentYear = cal.get(Calendar.YEAR);

        cal.add(Calendar.MONTH, -1);
        int lastMonth = cal.get(Calendar.MONTH);
        int lastMonthYear = cal.get(Calendar.YEAR);

        double currentMonthIncome = 0.0, currentMonthExpense = 0.0;
        double lastMonthIncome = 0.0, lastMonthExpense = 0.0;
        double totalIncome = 0.0, totalExpense = 0.0;

        Map<String, Double> topExpenseCategories = new HashMap<>();

        for (Transaction t : transactions) {
            Calendar txCal = Calendar.getInstance();
            txCal.setTimeInMillis(t.dateMillis);
            int txMonth = txCal.get(Calendar.MONTH);
            int txYear = txCal.get(Calendar.YEAR);

            if (t.type == Transaction.INCOME) {
                totalIncome += t.amount;
                if (txMonth == currentMonth && txYear == currentYear) {
                    currentMonthIncome += t.amount;
                } else if (txMonth == lastMonth && txYear == lastMonthYear) {
                    lastMonthIncome += t.amount;
                }
            } else if (t.type == Transaction.EXPENSE) {
                totalExpense += t.amount;
                if (txMonth == currentMonth && txYear == currentYear) {
                    currentMonthExpense += t.amount;
                } else if (txMonth == lastMonth && txYear == lastMonthYear) {
                    lastMonthExpense += t.amount;
                }

                // Track top categories
                String cat = t.category == null || t.category.isEmpty() ? "Other" : t.category;
                topExpenseCategories.put(cat, topExpenseCategories.getOrDefault(cat, 0.0) + t.amount);
            }
        }

        // Display totals
        binding.tvTotalIncome.setText(currencyFormat.format(totalIncome));
        binding.tvTotalExpense.setText(currencyFormat.format(totalExpense));
        binding.tvTotalBalance.setText(currencyFormat.format(totalIncome - totalExpense));

        int balanceColor = (totalIncome - totalExpense) >= 0
                ? getResources().getColor(R.color.colorPrimaryDark)
                : getResources().getColor(R.color.red_negative);
        binding.tvTotalBalance.setTextColor(balanceColor);

        // Current month
        binding.tvCurrentMonthIncome.setText(currencyFormat.format(currentMonthIncome));
        binding.tvCurrentMonthExpense.setText(currencyFormat.format(currentMonthExpense));
        binding.tvCurrentMonthBalance.setText(currencyFormat.format(currentMonthIncome - currentMonthExpense));

        // Month- over-month comparison
        if (lastMonthIncome > 0) {
            double incomeChange = ((currentMonthIncome - lastMonthIncome) / (double) lastMonthIncome) * 100;
            binding.tvIncomeChange.setText(String.format(Locale.US, "%+.1f%%", incomeChange));
            binding.tvIncomeChange.setTextColor(incomeChange >= 0
                    ? getResources().getColor(R.color.colorPrimaryDark)
                    : getResources().getColor(R.color.red_negative));
        } else {
            binding.tvIncomeChange.setText("N/A");
        }

        if (lastMonthExpense > 0) {
            double expenseChange = ((currentMonthExpense - lastMonthExpense) / (double) lastMonthExpense) * 100;
            binding.tvExpenseChange.setText(String.format(Locale.US, "%+.1f%%", expenseChange));
            binding.tvExpenseChange.setTextColor(expenseChange <= 0
                    ? getResources().getColor(R.color.colorPrimaryDark)
                    : getResources().getColor(R.color.red_negative));
        } else {
            binding.tvExpenseChange.setText("N/A");
        }

        // Savings rate
        if (totalIncome > 0) {
            double savingsRate = ((totalIncome - totalExpense) / (double) totalIncome) * 100;
            binding.tvSavingsRate.setText(String.format(Locale.US, "%.1f%%", savingsRate));
        } else {
            binding.tvSavingsRate.setText("0%");
        }

        // Top spending category
        String topCategory = "N/A";
        double topAmount = 0.0;
        for (Map.Entry<String, Double> entry : topExpenseCategories.entrySet()) {
            if (entry.getValue() > topAmount) {
                topAmount = entry.getValue();
                topCategory = entry.getKey();
            }
        }
        binding.tvTopCategory.setText(topCategory);
        binding.tvTopCategoryAmount.setText(currencyFormat.format(topAmount));

        // Average daily spending
        if (!transactions.isEmpty()) {
            long firstDate = transactions.get(transactions.size() - 1).dateMillis;
            long lastDate = transactions.get(0).dateMillis;
            long daysDiff = (lastDate - firstDate) / (1000 * 60 * 60 * 24) + 1;
            if (daysDiff > 0) {
                double avgDaily = totalExpense / (double) daysDiff;
                binding.tvAvgDailySpending.setText(currencyFormat.format(avgDaily));
            }
        }
    }

    private void setupCharts(List<Transaction> transactions) {
        setupMonthlyTrendChart(transactions);
        setupCategoryBreakdownChart(transactions);
    }

    private void setupMonthlyTrendChart(List<Transaction> transactions) {
        Map<String, Double> monthlyIncome = new HashMap<>();
        Map<String, Double> monthlyExpense = new HashMap<>();

        SimpleDateFormat monthFormat = new SimpleDateFormat("MMM yy", Locale.US);

        for (Transaction t : transactions) {
            String monthKey = monthFormat.format(t.dateMillis);

            if (t.type == Transaction.INCOME) {
                monthlyIncome.put(monthKey, monthlyIncome.getOrDefault(monthKey, 0.0) + t.amount);
            } else {
                monthlyExpense.put(monthKey, monthlyExpense.getOrDefault(monthKey, 0.0) + t.amount);
            }
        }

        // Get last 6 months
        List<String> months = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        for (int i = 5; i >= 0; i--) {
            cal.add(Calendar.MONTH, i == 5 ? -5 : 1);
            months.add(monthFormat.format(cal.getTime()));
        }

        ArrayList<BarEntry> incomeEntries = new ArrayList<>();
        ArrayList<BarEntry> expenseEntries = new ArrayList<>();

        for (int i = 0; i < months.size(); i++) {
            String month = months.get(i);
            incomeEntries.add(new BarEntry(i, monthlyIncome.getOrDefault(month, 0.0).floatValue()));
            expenseEntries.add(new BarEntry(i, monthlyExpense.getOrDefault(month, 0.0).floatValue()));
        }

        BarDataSet incomeSet = new BarDataSet(incomeEntries, "Income");
        incomeSet.setColor(getResources().getColor(R.color.colorPrimaryDark));
        incomeSet.setValueTextColor(getResources().getColor(R.color.textPrimary));

        BarDataSet expenseSet = new BarDataSet(expenseEntries, "Expenses");
        expenseSet.setColor(getResources().getColor(R.color.red_negative));
        expenseSet.setValueTextColor(getResources().getColor(R.color.textPrimary));

        BarData barData = new BarData(incomeSet, expenseSet);
        barData.setBarWidth(0.35f);

        BarChart chart = binding.chartMonthlyTrend;
        chart.setData(barData);
        chart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(months));
        chart.getXAxis().setGranularity(1f);
        chart.getXAxis().setTextColor(getResources().getColor(R.color.textPrimary));
        chart.getAxisLeft().setTextColor(getResources().getColor(R.color.textPrimary));
        chart.getAxisRight().setEnabled(false);
        chart.getLegend().setTextColor(getResources().getColor(R.color.textPrimary));
        chart.groupBars(0f, 0.3f, 0f);
        chart.getDescription().setEnabled(false);
        chart.animateY(1000);
        chart.invalidate();
    }

    private void setupCategoryBreakdownChart(List<Transaction> transactions) {
        Map<String, Double> categoryTotals = new HashMap<>();

        for (Transaction t : transactions) {
            if (t.type == Transaction.EXPENSE) {
                String cat = t.category == null || t.category.isEmpty() ? "Other" : t.category;
                categoryTotals.put(cat, categoryTotals.getOrDefault(cat, 0.0) + t.amount);
            }
        }

        ArrayList<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Double> entry : categoryTotals.entrySet()) {
            entries.add(new PieEntry(entry.getValue().floatValue(), entry.getKey()));
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(getResources().getColor(R.color.textPrimary));

        PieData pieData = new PieData(dataSet);

        PieChart chart = binding.chartCategoryBreakdown;
        chart.setData(pieData);
        chart.getDescription().setEnabled(false);
        chart.setEntryLabelTextSize(10f);
        chart.setEntryLabelColor(getResources().getColor(R.color.textPrimary));
        chart.getLegend().setTextColor(getResources().getColor(R.color.textPrimary));
        chart.animateY(1000);
        chart.invalidate();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
