package com.labproject.app.ui.expenses;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.labproject.app.R;
import com.labproject.app.data.db.DBHelper;
import com.labproject.app.data.prefs.SettingsManager;
import com.labproject.app.data.session.SessionManager;
import com.labproject.app.databinding.FragmentExpensesBinding;
import com.labproject.app.ui.Models.MainViewModel;
import com.labproject.app.ui.Models.Transaction;
import com.labproject.app.utils.ToastHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ExpensesFragment extends Fragment {

    private FragmentExpensesBinding binding;
    private MainViewModel vm;
    private DBHelper db;
    private SettingsManager settings;
    private String email;

    private long selectedDateMillis = System.currentTimeMillis();
    private Transaction editingTx = null;

    private ArrayAdapter<String> categoryAdapter;
    private ArrayAdapter<String> recurrenceAdapter;

    // Form collapse state
    private boolean isFormCollapsed = false;
    private boolean isFiltersExpanded = false;

    // Filter state
    private String searchQuery = "";
    private Long filterDateFrom = null;
    private Long filterDateTo = null;
    private int sortMode = 0; // 0=date desc, 1=date asc, 2=amount desc, 3=amount asc

    // All expenses list (unfiltered)
    private List<Transaction> allExpensesList = new ArrayList<>();
    private ExpensesAdapter adapter;

    // Recurrence options
    private static final String[] RECURRENCE_LABELS = {"One-time", "Weekly", "Monthly", "Yearly"};
    private static final String[] RECURRENCE_VALUES = {
            Transaction.RECURRENCE_ONCE,
            Transaction.RECURRENCE_WEEKLY,
            Transaction.RECURRENCE_MONTHLY,
            Transaction.RECURRENCE_YEARLY
    };

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentExpensesBinding.inflate(inflater, container, false);

        db = new DBHelper(requireContext());
        settings = new SettingsManager(requireContext());
        email = new SessionManager(requireContext()).getUserEmail();

        if (email == null) {
            ToastHelper.showError(requireContext(), "Session expired. Login again.");
            return binding.getRoot();
        }

        vm = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        updateDateText();
        binding.etExpenseDate.setOnClickListener(v -> openDatePicker());

        setupCategoryDropdown();
        setupRecurrenceDropdown();

        // Setup collapse/expand form
        setupFormCollapse();

        // Setup filters
        setupFilters();

        adapter = new ExpensesAdapter(new ExpensesAdapter.Listener() {
            @Override public void onClick(Transaction t) { enterEditMode(t); }
            @Override public void onLongClick(Transaction t) { showTransactionOptions(t); }
        });

        binding.rvExpenses.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvExpenses.setAdapter(adapter);

        vm.getTransactions().observe(getViewLifecycleOwner(), list -> {
            allExpensesList.clear();
            if (list != null) {
                for (Transaction t : list) {
                    if (t.type == Transaction.EXPENSE) allExpensesList.add(t);
                }
            }
            applyFiltersAndSort();
        });

        vm.loadTransactions(db, email);

        binding.btnAddExpense.setOnClickListener(v -> saveExpense());

        return binding.getRoot();
    }

    private void setupFormCollapse() {
        binding.layoutExpenseFormHeader.setOnClickListener(v -> toggleFormCollapse());
        binding.ivCollapseForm.setOnClickListener(v -> toggleFormCollapse());
    }

    private void toggleFormCollapse() {
        isFormCollapsed = !isFormCollapsed;
        
        if (isFormCollapsed) {
            binding.layoutExpenseFormContent.setVisibility(View.GONE);
            binding.ivCollapseForm.setRotation(180);
        } else {
            binding.layoutExpenseFormContent.setVisibility(View.VISIBLE);
            binding.ivCollapseForm.setRotation(0);
        }
    }

    private void setupFilters() {
        // Search with real-time filtering
        binding.etExpenseSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                searchQuery = s.toString().trim().toLowerCase();
                applyFiltersAndSort();
            }
        });

        // Toggle filters visibility
        binding.layoutFilterToggle.setOnClickListener(v -> toggleFiltersExpanded());
        binding.ivExpandFilters.setOnClickListener(v -> toggleFiltersExpanded());

        // Date filters
        binding.etFilterDateFrom.setOnClickListener(v -> openFilterDatePicker(true));
        binding.etFilterDateTo.setOnClickListener(v -> openFilterDatePicker(false));

        // Sort chips
        binding.chipSortDateDesc.setOnClickListener(v -> { sortMode = 0; applyFiltersAndSort(); });
        binding.chipSortDateAsc.setOnClickListener(v -> { sortMode = 1; applyFiltersAndSort(); });
        binding.chipSortAmountDesc.setOnClickListener(v -> { sortMode = 2; applyFiltersAndSort(); });
        binding.chipSortAmountAsc.setOnClickListener(v -> { sortMode = 3; applyFiltersAndSort(); });

        // Clear filters
        binding.btnClearFilters.setOnClickListener(v -> clearFilters());
    }

    private void toggleFiltersExpanded() {
        isFiltersExpanded = !isFiltersExpanded;
        
        if (isFiltersExpanded) {
            binding.layoutFilterOptions.setVisibility(View.VISIBLE);
            binding.ivExpandFilters.setRotation(180);
        } else {
            binding.layoutFilterOptions.setVisibility(View.GONE);
            binding.ivExpandFilters.setRotation(0);
        }
    }

    private void openFilterDatePicker(boolean isFromDate) {

        Long preselect = isFromDate ? filterDateFrom : filterDateTo;

        com.google.android.material.datepicker.MaterialDatePicker<Long> picker =
                com.google.android.material.datepicker.MaterialDatePicker.Builder.datePicker()
                        .setTheme(R.style.AppMaterialDatePicker)
                        .setSelection(
                                preselect != null
                                        ? preselect
                                        : com.google.android.material.datepicker.MaterialDatePicker.todayInUtcMilliseconds()
                        )
                        .build();

        picker.show(
                getParentFragmentManager(),
                isFromDate ? "expense_filter_from" : "expense_filter_to"
        );

        picker.addOnPositiveButtonClickListener(selection -> {

            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(selection);

            if (isFromDate) {
                c.set(Calendar.HOUR_OF_DAY, 0);
                c.set(Calendar.MINUTE, 0);
                c.set(Calendar.SECOND, 0);
                filterDateFrom = c.getTimeInMillis();
                binding.etFilterDateFrom.setText(formatDate(filterDateFrom));
            } else {
                c.set(Calendar.HOUR_OF_DAY, 23);
                c.set(Calendar.MINUTE, 59);
                c.set(Calendar.SECOND, 59);
                filterDateTo = c.getTimeInMillis();
                binding.etFilterDateTo.setText(formatDate(filterDateTo));
            }

            applyFiltersAndSort();
        });
    }
    private void openDatePicker() {

        com.google.android.material.datepicker.MaterialDatePicker<Long> picker =
                com.google.android.material.datepicker.MaterialDatePicker.Builder.datePicker()
                        .setTheme(R.style.AppMaterialDatePicker)
                        .setSelection(selectedDateMillis)
                        .build();

        picker.show(getParentFragmentManager(), "expense_date");

        picker.addOnPositiveButtonClickListener(selection -> {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(selection);
            c.set(Calendar.HOUR_OF_DAY, 12);
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);

            selectedDateMillis = c.getTimeInMillis();
            updateDateText();
        });
    }
//    private void openFilterDatePicker(boolean isFromDate) {
//        Calendar cal = Calendar.getInstance();
//        if (isFromDate && filterDateFrom != null) {
//            cal.setTimeInMillis(filterDateFrom);
//        } else if (!isFromDate && filterDateTo != null) {
//            cal.setTimeInMillis(filterDateTo);
//        }
//
//        DatePickerDialog dp = new DatePickerDialog(
//                requireContext(),
//                (view, y, m, d) -> {
//                    Calendar c = Calendar.getInstance();
//                    c.set(Calendar.YEAR, y);
//                    c.set(Calendar.MONTH, m);
//                    c.set(Calendar.DAY_OF_MONTH, d);
//
//                    if (isFromDate) {
//                        c.set(Calendar.HOUR_OF_DAY, 0);
//                        c.set(Calendar.MINUTE, 0);
//                        c.set(Calendar.SECOND, 0);
//                        filterDateFrom = c.getTimeInMillis();
//                        binding.etFilterDateFrom.setText(formatDate(filterDateFrom));
//                    } else {
//                        c.set(Calendar.HOUR_OF_DAY, 23);
//                        c.set(Calendar.MINUTE, 59);
//                        c.set(Calendar.SECOND, 59);
//                        filterDateTo = c.getTimeInMillis();
//                        binding.etFilterDateTo.setText(formatDate(filterDateTo));
//                    }
//                    applyFiltersAndSort();
//                },
//                cal.get(Calendar.YEAR),
//                cal.get(Calendar.MONTH),
//                cal.get(Calendar.DAY_OF_MONTH)
//        );
//        dp.show();
//    }

    private void clearFilters() {
        searchQuery = "";
        filterDateFrom = null;
        filterDateTo = null;
        sortMode = 0;

        binding.etExpenseSearch.setText("");
        binding.etFilterDateFrom.setText("");
        binding.etFilterDateTo.setText("");
        binding.chipSortDateDesc.setChecked(true);

        applyFiltersAndSort();
        ToastHelper.showInfo(requireContext(), "Filters cleared");
    }

    private void applyFiltersAndSort() {
        List<Transaction> filtered = new ArrayList<>();

        for (Transaction t : allExpensesList) {
            // Search filter
            if (!searchQuery.isEmpty()) {
                String desc = t.description != null ? t.description.toLowerCase() : "";
                String amount = String.valueOf(t.amount);
                String category = t.category != null ? t.category.toLowerCase() : "";
                
                if (!desc.contains(searchQuery) && !amount.contains(searchQuery) && !category.contains(searchQuery)) {
                    continue;
                }
            }

            // Date from filter
            if (filterDateFrom != null && t.dateMillis < filterDateFrom) {
                continue;
            }

            // Date to filter
            if (filterDateTo != null && t.dateMillis > filterDateTo) {
                continue;
            }

            filtered.add(t);
        }

        // Sort
        Comparator<Transaction> comparator;
        switch (sortMode) {
            case 1: // Date ascending
                comparator = (a, b) -> Long.compare(a.dateMillis, b.dateMillis);
                break;
            case 2: // Amount descending
                comparator = (a, b) -> Double.compare(b.amount, a.amount);
                break;
            case 3: // Amount ascending
                comparator = (a, b) -> Double.compare(a.amount, b.amount);
                break;
            default: // Date descending
                comparator = (a, b) -> Long.compare(b.dateMillis, a.dateMillis);
                break;
        }
        Collections.sort(filtered, comparator);

        adapter.submitList(filtered);

        // Update UI
        binding.tvEmptyExpenses.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        binding.rvExpenses.setVisibility(filtered.isEmpty() ? View.GONE : View.VISIBLE);

        // Update results count
        if (filtered.size() == allExpensesList.size()) {
            binding.tvResultsCount.setText("All expenses (" + filtered.size() + ")");
        } else {
            binding.tvResultsCount.setText("Showing " + filtered.size() + " of " + allExpensesList.size());
        }
    }

    private String formatDate(long millis) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date(millis));
    }

    @Override
    public void onResume() {
        super.onResume();
        setupCategoryDropdown();
    }

    private void setupCategoryDropdown() {
        List<String> cats = settings.getExpenseCategories();
        categoryAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                cats
        );
        binding.actExpenseCategory.setAdapter(categoryAdapter);
    }

    private void setupRecurrenceDropdown() {
        recurrenceAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                Arrays.asList(RECURRENCE_LABELS)
        );
        binding.actExpenseRecurrence.setAdapter(recurrenceAdapter);
        // Set default to "One-time"
        binding.actExpenseRecurrence.setText(RECURRENCE_LABELS[0], false);
    }

    private String getSelectedRecurrenceValue() {
        String selected = binding.actExpenseRecurrence.getText().toString().trim();
        for (int i = 0; i < RECURRENCE_LABELS.length; i++) {
            if (RECURRENCE_LABELS[i].equals(selected)) {
                return RECURRENCE_VALUES[i];
            }
        }
        return Transaction.RECURRENCE_ONCE;
    }

    private void setRecurrenceDropdownByValue(String value) {
        if (value == null) value = Transaction.RECURRENCE_ONCE;
        for (int i = 0; i < RECURRENCE_VALUES.length; i++) {
            if (RECURRENCE_VALUES[i].equals(value)) {
                binding.actExpenseRecurrence.setText(RECURRENCE_LABELS[i], false);
                return;
            }
        }
        binding.actExpenseRecurrence.setText(RECURRENCE_LABELS[0], false);
    }

    private void saveExpense() {
        String amountStr = binding.etExpenseAmount.getText().toString().trim();
        String desc = binding.etExpenseDescription.getText().toString().trim();
        String category = binding.actExpenseCategory.getText().toString().trim();
        String recurrence = getSelectedRecurrenceValue();

        if (amountStr.isEmpty()) {
            ToastHelper.showError(requireContext(), "Please enter an amount");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (Exception e) {
            ToastHelper.showError(requireContext(), "Invalid amount format");
            return;
        }

        if (amount <= 0) {
            ToastHelper.showError(requireContext(), "Amount must be greater than $0");
            return;
        }

        if (amount > 1_000_000) {
            ToastHelper.showError(requireContext(), "Amount too large");
            return;
        }

        if (category.isEmpty()) category = "Other";

        if (editingTx == null) {
            vm.addExpense(db, email, amount, selectedDateMillis, category, desc, recurrence);
            ToastHelper.showSuccess(requireContext(), "Expense added successfully");
        } else {
            editingTx.amount = amount;
            editingTx.dateMillis = selectedDateMillis;
            editingTx.category = category;
            editingTx.description = desc;
            editingTx.recurrence = recurrence;

            vm.updateTransaction(db, email, editingTx);
            ToastHelper.showSuccess(requireContext(), "Expense updated successfully");
            exitEditMode();
        }

        clearInputs();
    }

    private void enterEditMode(Transaction t) {
        editingTx = t;

        binding.tvExpenseFormTitle.setText("Edit Expense");
        binding.btnAddExpense.setText("UPDATE EXPENSE");

        binding.etExpenseAmount.setText(String.valueOf(t.amount));
        binding.etExpenseDescription.setText(t.description == null ? "" : t.description);

        selectedDateMillis = t.dateMillis;
        updateDateText();

        binding.actExpenseCategory.setText(t.category == null ? "" : t.category, false);
        setRecurrenceDropdownByValue(t.recurrence);

        binding.tvExpenseFormTitle.setOnClickListener(v -> {
            exitEditMode();
            clearInputs();
        });
    }

    private void exitEditMode() {
        editingTx = null;
        binding.tvExpenseFormTitle.setText("Add Expense");
        binding.btnAddExpense.setText("ADD EXPENSE");
        binding.tvExpenseFormTitle.setOnClickListener(null);
    }

    private void showTransactionOptions(Transaction t) {
        String[] options;
        if (t.isActive) {
            options = new String[]{"Edit", "Deactivate", "Delete Permanently"};
        } else {
            options = new String[]{"Edit", "Reactivate", "Delete Permanently"};
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Expense Options")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: // Edit
                            enterEditMode(t);
                            break;
                        case 1: // Deactivate or Reactivate
                            if (t.isActive) {
                                confirmDeactivate(t);
                            } else {
                                confirmReactivate(t);
                            }
                            break;
                        case 2: // Delete Permanently
                            confirmDelete(t);
                            break;
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmDeactivate(Transaction t) {
        String message = "Deactivating will stop this expense from being counted in future calculations.\n\n" +
                "It will remain active for all periods up to now.\n\n" +
                "You can reactivate it later.";
        
        new AlertDialog.Builder(requireContext())
                .setTitle("Deactivate Expense?")
                .setMessage(message)
                .setPositiveButton("Deactivate", (d, which) -> {
                    vm.deactivateTransaction(db, email, t.id);
                    ToastHelper.showInfo(requireContext(), "Expense deactivated");
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmReactivate(Transaction t) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Reactivate Expense?")
                .setMessage("This will make the expense active again and remove any end date.")
                .setPositiveButton("Reactivate", (d, which) -> {
                    vm.reactivateTransaction(db, email, t.id);
                    ToastHelper.showSuccess(requireContext(), "Expense reactivated");
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmDelete(Transaction t) {
        String message = "Are you sure you want to permanently delete this expense?\n\n" +
                "This action cannot be undone and all references to this expense will be removed.\n\n" +
                "Tip: Consider deactivating instead if you want to keep historical data.";
        
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Expense Permanently?")
                .setMessage(message)
                .setPositiveButton("Delete", (d, which) -> {
                    vm.deleteTransaction(db, email, t.id);
                    ToastHelper.showInfo(requireContext(), "Expense deleted");
                })
                .setNeutralButton("Deactivate Instead", (d, which) -> {
                    vm.deactivateTransaction(db, email, t.id);
                    ToastHelper.showInfo(requireContext(), "Expense deactivated");
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

//    private void openDatePicker() {
//        Calendar cal = Calendar.getInstance();
//        cal.setTimeInMillis(selectedDateMillis);
//
//        new DatePickerDialog(
//                requireContext(),
//                (view, y, m, d) -> {
//                    Calendar c = Calendar.getInstance();
//                    c.set(y, m, d, 12, 0, 0);
//                    selectedDateMillis = c.getTimeInMillis();
//                    updateDateText();
//                },
//                cal.get(Calendar.YEAR),
//                cal.get(Calendar.MONTH),
//                cal.get(Calendar.DAY_OF_MONTH)
//        ).show();
//    }

    private void updateDateText() {
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.US)
                .format(new Date(selectedDateMillis));
        binding.etExpenseDate.setText(date);
    }

    private void clearInputs() {
        binding.etExpenseAmount.setText("");
        binding.etExpenseDescription.setText("");
        binding.actExpenseCategory.setText("", false);
        binding.actExpenseRecurrence.setText(RECURRENCE_LABELS[0], false);

        selectedDateMillis = System.currentTimeMillis();
        updateDateText();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}