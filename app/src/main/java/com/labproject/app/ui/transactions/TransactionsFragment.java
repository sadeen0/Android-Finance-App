package com.labproject.app.ui.transactions;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
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
import com.labproject.app.data.session.SessionManager;
import com.labproject.app.databinding.DialogEditTransactionBinding;
import com.labproject.app.databinding.FragmentTransactionsBinding;
import com.labproject.app.ui.Models.MainViewModel;
import com.labproject.app.ui.Models.Transaction;
import com.labproject.app.utils.ToastHelper;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TransactionsFragment extends Fragment {

    private FragmentTransactionsBinding binding;
    private MainViewModel vm;
    private DBHelper db;
    private String email;

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
        binding = FragmentTransactionsBinding.inflate(inflater, container, false);

        db = new DBHelper(requireContext());
        email = new SessionManager(requireContext()).getUserEmail();

        vm = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        TransactionsAdapter adapter = new TransactionsAdapter(new TransactionsAdapter.Listener() {
            @Override public void onClick(Transaction t) { openEditDialog(t); }
            @Override public void onLongClick(Transaction t) { showTransactionOptions(t); }
        });

        binding.rvTransactions.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvTransactions.setAdapter(adapter);

        vm.getTransactions().observe(getViewLifecycleOwner(), list -> {
            adapter.submitList(list);

            boolean empty = (list == null || list.isEmpty());
            binding.tvEmptyTransactions.setVisibility(empty ? View.VISIBLE : View.GONE);
            binding.rvTransactions.setVisibility(empty ? View.GONE : View.VISIBLE);
        });

        // Ensure list is loaded when opening this screen
        vm.loadTransactions(db, email);

        return binding.getRoot();
    }

    private void showTransactionOptions(Transaction t) {
        String typeLabel = (t.type == Transaction.INCOME) ? "Income" : "Expense";
        String[] options;
        if (t.isActive) {
            options = new String[]{"Edit", "Deactivate", "Delete Permanently"};
        } else {
            options = new String[]{"Edit", "Reactivate", "Delete Permanently"};
        }

        new AlertDialog.Builder(requireContext())
                .setTitle(typeLabel + " Options")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: // Edit
                            openEditDialog(t);
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
        String typeLabel = (t.type == Transaction.INCOME) ? "income" : "expense";
        String message = "Deactivating will stop this " + typeLabel + " from being counted in future calculations.\n\n" +
                "It will remain active for all periods up to now.\n\n" +
                "You can reactivate it later.";
        
        new AlertDialog.Builder(requireContext())
                .setTitle("Deactivate " + (t.type == Transaction.INCOME ? "Income" : "Expense") + "?")
                .setMessage(message)
                .setPositiveButton("Deactivate", (d, which) -> {
                    vm.deactivateTransaction(db, email, t.id);
                    ToastHelper.showInfo(requireContext(), typeLabel.substring(0, 1).toUpperCase() + typeLabel.substring(1) + " deactivated");
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmReactivate(Transaction t) {
        String typeLabel = (t.type == Transaction.INCOME) ? "Income" : "Expense";
        new AlertDialog.Builder(requireContext())
                .setTitle("Reactivate " + typeLabel + "?")
                .setMessage("This will make the " + typeLabel.toLowerCase() + " active again and remove any end date.")
                .setPositiveButton("Reactivate", (d, which) -> {
                    vm.reactivateTransaction(db, email, t.id);
                    ToastHelper.showSuccess(requireContext(), typeLabel + " reactivated");
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmDelete(Transaction t) {
        String typeLabel = (t.type == Transaction.INCOME) ? "income" : "expense";
        String message = "Are you sure you want to permanently delete this " + typeLabel + "?\n\n" +
                "This action cannot be undone and all references will be removed.\n\n" +
                "Tip: Consider deactivating instead if you want to keep historical data.";
        
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete " + (t.type == Transaction.INCOME ? "Income" : "Expense") + " Permanently?")
                .setMessage(message)
                .setPositiveButton("Delete", (d, which) -> {
                    vm.deleteTransaction(db, email, t.id);
                    ToastHelper.showInfo(requireContext(), typeLabel.substring(0, 1).toUpperCase() + typeLabel.substring(1) + " deleted");
                })
                .setNeutralButton("Deactivate Instead", (d, which) -> {
                    vm.deactivateTransaction(db, email, t.id);
                    ToastHelper.showInfo(requireContext(), typeLabel.substring(0, 1).toUpperCase() + typeLabel.substring(1) + " deactivated");
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void openEditDialog(Transaction t) {
        DialogEditTransactionBinding b =
                DialogEditTransactionBinding.inflate(LayoutInflater.from(requireContext()));

        // fill current values
        b.etAmount.setText(String.valueOf(t.amount));
        b.etDesc.setText(t.description == null ? "" : t.description);

        final long[] selectedDate = { t.dateMillis };
        b.etDate.setText(formatDate(selectedDate[0]));

        // Recurrence spinner
        ArrayAdapter<String> recurrenceAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                Arrays.asList(RECURRENCE_LABELS)
        );
        recurrenceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        b.spRecurrence.setAdapter(recurrenceAdapter);

        // Set current recurrence
        int recurrencePos = 0;
        if (t.recurrence != null) {
            for (int i = 0; i < RECURRENCE_VALUES.length; i++) {
                if (RECURRENCE_VALUES[i].equals(t.recurrence)) {
                    recurrencePos = i;
                    break;
                }
            }
        }
        b.spRecurrence.setSelection(recurrencePos);

        // categories based on type
        int arr = (t.type == Transaction.INCOME)
                ? R.array.income_categories
                : R.array.expense_categories;

        ArrayAdapter<CharSequence> catAdapter = ArrayAdapter.createFromResource(
                requireContext(), arr, android.R.layout.simple_spinner_item
        );
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        b.spCategory.setAdapter(catAdapter);

        int pos = catAdapter.getPosition(t.category);
        if (pos >= 0) b.spCategory.setSelection(pos);

        b.etDate.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(selectedDate[0]);

            new DatePickerDialog(
                    requireContext(),
                    (view, y, m, day) -> {
                        Calendar c = Calendar.getInstance();
                        c.set(Calendar.YEAR, y);
                        c.set(Calendar.MONTH, m);
                        c.set(Calendar.DAY_OF_MONTH, day);
                        c.set(Calendar.HOUR_OF_DAY, 12);
                        c.set(Calendar.MINUTE, 0);
                        c.set(Calendar.SECOND, 0);
                        selectedDate[0] = c.getTimeInMillis();
                        b.etDate.setText(formatDate(selectedDate[0]));
                    },
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
            ).show();
        });

        new AlertDialog.Builder(requireContext())
                .setTitle(t.type == Transaction.INCOME ? "Edit Income" : "Edit Expense")
                .setView(b.getRoot())
                .setPositiveButton("Save", (d, w) -> {
                    String aStr = b.etAmount.getText().toString().trim();
                    if (TextUtils.isEmpty(aStr)) return;

                    double amount = Double.parseDouble(aStr);
                    String desc = b.etDesc.getText().toString().trim();
                    String cat = b.spCategory.getSelectedItem().toString();
                    int recIdx = b.spRecurrence.getSelectedItemPosition();
                    String recurrence = RECURRENCE_VALUES[recIdx];

                    t.amount = amount;
                    t.dateMillis = selectedDate[0];
                    t.category = cat;
                    t.description = desc;
                    t.recurrence = recurrence;

                    vm.updateTransaction(db, email, t);
                    ToastHelper.showSuccess(requireContext(), "Transaction updated");
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private String formatDate(long millis) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date(millis));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
