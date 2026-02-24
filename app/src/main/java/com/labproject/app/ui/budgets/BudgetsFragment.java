package com.labproject.app.ui.budgets;

import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.labproject.app.utils.ToastHelper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;
import com.labproject.app.R;
import com.labproject.app.data.db.DBHelper;
import com.labproject.app.data.prefs.SettingsManager;
import com.labproject.app.data.session.SessionManager;
import com.labproject.app.ui.Models.Budget;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;
import java.util.List;

public class BudgetsFragment extends Fragment {

    private RecyclerView rvBudgets;
    private TextView tvEmptyBudgets, tvBudgetFormTitle, tvResultsCount;
    private Spinner spBudgetCategory, spBudgetPeriod;
    private TextInputEditText etBudgetLimit, etAlertThreshold;
    private MaterialButton btnAddBudget, btnClearFilters;
    private EditText etBudgetSearch;
    private CheckBox cbShowAlertsOnly;

    // Collapse/expand views
    private LinearLayout layoutBudgetFormHeader, layoutBudgetFormContent;
    private LinearLayout layoutFilterToggle, layoutFilterOptions;
    private ImageView ivCollapseForm, ivExpandFilters;

    // Sort/filter chips
    private Chip chipPeriodAll, chipPeriodDaily, chipPeriodWeekly, chipPeriodMonthly;
    private Chip chipSortCategory, chipSortLimitDesc, chipSortLimitAsc, chipSortUsage;

    private DBHelper db;
    private SessionManager session;
    private SettingsManager settings;
    private String email;

    private BudgetAdapter adapter;
    private Budget editingBudget = null;

    // Collapse state
    private boolean isFormCollapsed = false;
    private boolean isFiltersExpanded = false;

    // Filter state
    private String searchQuery = "";
    private String filterPeriod = ""; // empty = all
    private int sortMode = 0; // 0=category, 1=limit desc, 2=limit asc, 3=usage
    private boolean showAlertsOnly = false;

    // All budgets list (unfiltered)
    private List<Budget> allBudgetsList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_budget, container, false);

        db = new DBHelper(requireContext());
        session = new SessionManager(requireContext());
        settings = new SettingsManager(requireContext());
        email = session.getUserEmail();

        if (email == null) {
            ToastHelper.showError(requireContext(), "Session expired");
            return root;
        }

        // Find views
        rvBudgets = root.findViewById(R.id.rvBudgets);
        tvEmptyBudgets = root.findViewById(R.id.tvEmptyBudgets);
        tvBudgetFormTitle = root.findViewById(R.id.tvBudgetFormTitle);
        tvResultsCount = root.findViewById(R.id.tvResultsCount);
        spBudgetCategory = root.findViewById(R.id.spBudgetCategory);
        spBudgetPeriod = root.findViewById(R.id.spBudgetPeriod);
        etBudgetLimit = root.findViewById(R.id.etBudgetLimit);
        etAlertThreshold = root.findViewById(R.id.etAlertThreshold);
        btnAddBudget = root.findViewById(R.id.btnAddBudget);
        btnClearFilters = root.findViewById(R.id.btnClearFilters);
        etBudgetSearch = root.findViewById(R.id.etBudgetSearch);
        cbShowAlertsOnly = root.findViewById(R.id.cbShowAlertsOnly);

        layoutBudgetFormHeader = root.findViewById(R.id.layoutBudgetFormHeader);
        layoutBudgetFormContent = root.findViewById(R.id.layoutBudgetFormContent);
        layoutFilterToggle = root.findViewById(R.id.layoutFilterToggle);
        layoutFilterOptions = root.findViewById(R.id.layoutFilterOptions);
        ivCollapseForm = root.findViewById(R.id.ivCollapseForm);
        ivExpandFilters = root.findViewById(R.id.ivExpandFilters);

        chipPeriodAll = root.findViewById(R.id.chipPeriodAll);
        chipPeriodDaily = root.findViewById(R.id.chipPeriodDaily);
        chipPeriodWeekly = root.findViewById(R.id.chipPeriodWeekly);
        chipPeriodMonthly = root.findViewById(R.id.chipPeriodMonthly);
        chipSortCategory = root.findViewById(R.id.chipSortCategory);
        chipSortLimitDesc = root.findViewById(R.id.chipSortLimitDesc);
        chipSortLimitAsc = root.findViewById(R.id.chipSortLimitAsc);
        chipSortUsage = root.findViewById(R.id.chipSortUsage);

        setupCategorySpinner();
        setupPeriodSpinner();

        setupFormCollapse();
        setupFilters();

        adapter = new BudgetAdapter(new BudgetAdapter.Listener() {
            @Override
            public void onClick(Budget budget) {
                enterEditMode(budget);
            }

            @Override
            public void onLongClick(Budget budget) {
                confirmDelete(budget);
            }
        });

        rvBudgets.setLayoutManager(new LinearLayoutManager(getContext()));
        rvBudgets.setAdapter(adapter);

        btnAddBudget.setOnClickListener(v -> saveBudget());

        loadBudgets();

        return root;
    }

    private void setupFormCollapse() {
        layoutBudgetFormHeader.setOnClickListener(v -> toggleFormCollapse());
        ivCollapseForm.setOnClickListener(v -> toggleFormCollapse());
    }

    private void toggleFormCollapse() {
        isFormCollapsed = !isFormCollapsed;
        
        if (isFormCollapsed) {
            layoutBudgetFormContent.setVisibility(View.GONE);
            ivCollapseForm.setRotation(180);
        } else {
            layoutBudgetFormContent.setVisibility(View.VISIBLE);
            ivCollapseForm.setRotation(0);
        }
    }

    private void setupFilters() {
        // Search with real time filtering
        etBudgetSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                searchQuery = s.toString().trim().toLowerCase();
                applyFiltersAndSort();
            }
        });

        // Toggle filters visibility
        layoutFilterToggle.setOnClickListener(v -> toggleFiltersExpanded());
        ivExpandFilters.setOnClickListener(v -> toggleFiltersExpanded());

        chipPeriodAll.setOnClickListener(v -> { filterPeriod = ""; applyFiltersAndSort(); });
        chipPeriodDaily.setOnClickListener(v -> { filterPeriod = "daily"; applyFiltersAndSort(); });
        chipPeriodWeekly.setOnClickListener(v -> { filterPeriod = "weekly"; applyFiltersAndSort(); });
        chipPeriodMonthly.setOnClickListener(v -> { filterPeriod = "monthly"; applyFiltersAndSort(); });

        chipSortCategory.setOnClickListener(v -> { sortMode = 0; applyFiltersAndSort(); });
        chipSortLimitDesc.setOnClickListener(v -> { sortMode = 1; applyFiltersAndSort(); });
        chipSortLimitAsc.setOnClickListener(v -> { sortMode = 2; applyFiltersAndSort(); });
        chipSortUsage.setOnClickListener(v -> { sortMode = 3; applyFiltersAndSort(); });

        // Show alerts only checkbox
        cbShowAlertsOnly.setOnCheckedChangeListener((buttonView, isChecked) -> {
            showAlertsOnly = isChecked;
            applyFiltersAndSort();
        });

        btnClearFilters.setOnClickListener(v -> clearFilters());
    }

    private void toggleFiltersExpanded() {
        isFiltersExpanded = !isFiltersExpanded;
        
        if (isFiltersExpanded) {
            layoutFilterOptions.setVisibility(View.VISIBLE);
            ivExpandFilters.setRotation(180);
        } else {
            layoutFilterOptions.setVisibility(View.GONE);
            ivExpandFilters.setRotation(0);
        }
    }

    private void clearFilters() {
        searchQuery = "";
        filterPeriod = "";
        sortMode = 0;
        showAlertsOnly = false;

        etBudgetSearch.setText("");
        chipPeriodAll.setChecked(true);
        chipSortCategory.setChecked(true);
        cbShowAlertsOnly.setChecked(false);

        applyFiltersAndSort();
        ToastHelper.showInfo(requireContext(), "Filters cleared");
    }

    private void applyFiltersAndSort() {
        List<Budget> filtered = new ArrayList<>();

        for (Budget b : allBudgetsList) {
            // Search filter
            if (!searchQuery.isEmpty()) {
                String category = b.category != null ? b.category.toLowerCase() : "";
                String limit = String.valueOf(b.limitAmount);
                
                if (!category.contains(searchQuery) && !limit.contains(searchQuery)) {
                    continue;
                }
            }

            if (!filterPeriod.isEmpty() && !filterPeriod.equals(b.period)) {
                continue;
            }

            // Alerts only filter
            if (showAlertsOnly && !b.alertTriggered) {
                continue;
            }

            filtered.add(b);
        }

        // sort
        Comparator<Budget> comparator;
        switch (sortMode) {
            case 1: // Limit descending
                comparator = (a, b) -> Double.compare(b.limitAmount, a.limitAmount);
                break;
            case 2: // Limit ascending
                comparator = (a, b) -> Double.compare(a.limitAmount, b.limitAmount);
                break;
            case 3: // Usage percentage
                comparator = (a, b) -> {
                    double usageA = a.limitAmount > 0 ? (a.spent / a.limitAmount) : 0;
                    double usageB = b.limitAmount > 0 ? (b.spent / b.limitAmount) : 0;
                    return Double.compare(usageB, usageA); // Highest usage first
                };
                break;
            default: // Category alphabet
                comparator = (a, b) -> {
                    String catA = a.category != null ? a.category : "";
                    String catB = b.category != null ? b.category : "";
                    return catA.compareToIgnoreCase(catB);
                };
                break;
        }
        Collections.sort(filtered, comparator);

        adapter.submitList(filtered);

        // Update UI
        tvEmptyBudgets.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        rvBudgets.setVisibility(filtered.isEmpty() ? View.GONE : View.VISIBLE);

        // Update results count
        if (filtered.size() == allBudgetsList.size()) {
            tvResultsCount.setText("Your Budgets (" + filtered.size() + ")");
        } else {
            tvResultsCount.setText("Showing " + filtered.size() + " of " + allBudgetsList.size());
        }
    }

    private void setupCategorySpinner() {
        List<String> categories = settings.getExpenseCategories();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                categories
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spBudgetCategory.setAdapter(adapter);
    }

    private void setupPeriodSpinner() {
        List<String> periods = Arrays.asList("monthly", "weekly", "daily");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                periods
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spBudgetPeriod.setAdapter(adapter);
    }

    private void saveBudget() {
        String limitStr = etBudgetLimit.getText().toString().trim();
        String thresholdStr = etAlertThreshold.getText().toString().trim();

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
            ToastHelper.showError(requireContext(), "Invalid input format");
            return;
        }

        if (limit <= 0) {
            ToastHelper.showError(requireContext(), "Budget limit must be greater than $0");
            return;
        }

        if (limit > 1000000) {
            ToastHelper.showError(requireContext(), "Budget limit too large (max $1,000,000)");
            return;
        }

        if (threshold < 0 || threshold > 100) {
            ToastHelper.showError(requireContext(), "Alert threshold must be 0-100%");
            return;
        }

        String category = spBudgetCategory.getSelectedItem().toString();
        String period = spBudgetPeriod.getSelectedItem().toString();

        if (editingBudget == null) {
            // ADD
            long id = db.insertBudget(email, category, limit, period, threshold);
            if (id > 0) {
                ToastHelper.showSuccess(requireContext(), "Budget added");
            } else {
                ToastHelper.showError(requireContext(), "Failed to add budget");
            }
        } else {
            // UPDATE
            boolean ok = db.updateBudget(email, editingBudget.id, category, limit, period, threshold);
            if (ok) {
                ToastHelper.showSuccess(requireContext(), "Budget updated");
            } else {
                ToastHelper.showError(requireContext(), "Failed to update");
            }
            exitEditMode();
        }

        clearInputs();
        loadBudgets();
    }

    private void enterEditMode(Budget b) {
        editingBudget = b;

        tvBudgetFormTitle.setText("Edit Budget");
        btnAddBudget.setText("Update Budget");

        etBudgetLimit.setText(String.format(Locale.US, "%.2f", b.limitAmount));
        etAlertThreshold.setText(String.valueOf(b.alertThreshold));

        // Set category
        ArrayAdapter<String> catAdapter = (ArrayAdapter<String>) spBudgetCategory.getAdapter();
        int catPos = catAdapter.getPosition(b.category);
        if (catPos >= 0) spBudgetCategory.setSelection(catPos);

        // Set period
        ArrayAdapter<String> periodAdapter = (ArrayAdapter<String>) spBudgetPeriod.getAdapter();
        int periodPos = periodAdapter.getPosition(b.period);
        if (periodPos >= 0) spBudgetPeriod.setSelection(periodPos);

        // Allow cancel by clicking title
        tvBudgetFormTitle.setOnClickListener(v -> {
            exitEditMode();
            clearInputs();
        });
    }

    private void exitEditMode() {
        editingBudget = null;
        tvBudgetFormTitle.setText("Add Budget");
        btnAddBudget.setText("Add Budget");
        tvBudgetFormTitle.setOnClickListener(null);
    }

    private void confirmDelete(Budget b) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Budget?")
                .setMessage("Are you sure you want to delete this budget for " + b.category + "?")
                .setPositiveButton("Delete", (d, w) -> {
                    boolean ok = db.deleteBudget(email, b.id);
                    if (ok) {
                        ToastHelper.showSuccess(requireContext(), "Budget deleted");
                        loadBudgets();
                    } else {
                        ToastHelper.showError(requireContext(), "Failed to delete");
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void clearInputs() {
        etBudgetLimit.setText("");
        etAlertThreshold.setText("50");
        spBudgetCategory.setSelection(0);
        spBudgetPeriod.setSelection(0);
    }

    private void loadBudgets() {
        allBudgetsList.clear();

        Cursor c = db.getBudgetsForUser(email);
        if (c != null && c.moveToFirst()) {
            do {
                int id = c.getInt(0);
                String category = c.getString(1);
                double limit = c.getDouble(2);
                String period = c.getString(3);
                int threshold = c.getInt(4);

                Budget b = new Budget(id, category, limit, period, threshold);

                // Calculate spent amount for this period
                long[] range = getPeriodRange(period);
                double spent = db.getSpentAmountForCategoryAndPeriod(email, category, range[0], range[1]);
                b.spent = spent;

                // Check if alert should trigger
                if (limit > 0) {
                    int percentUsed = (int) ((spent * 100.0) / limit);
                    b.alertTriggered = (percentUsed >= threshold);
                }

                allBudgetsList.add(b);

            } while (c.moveToNext());
            c.close();
        }

        applyFiltersAndSort();
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
        return new long[]{start, end};
    }

    @Override
    public void onResume() {
        super.onResume();
        loadBudgets();
    }
}
