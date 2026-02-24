package com.labproject.app.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import com.labproject.app.utils.ToastHelper;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.labproject.app.data.prefs.SettingsManager;
import com.labproject.app.databinding.FragmentSettingsBinding;

import java.util.Arrays;
import java.util.List;

public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;
    private SettingsManager settings;

    private boolean showingIncomeCats = true; // current tab
    private CategoryAdapter adapter;
    private boolean isInitialSetup = true; // flag to prevent toast on first load

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        settings = new SettingsManager(requireContext());

        // --- THEME
        if (settings.getTheme().equals("dark")) {
            binding.toggleTheme.check(binding.btnDark.getId());
        } else {
            binding.toggleTheme.check(binding.btnLight.getId());
        }

        binding.toggleTheme.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;

            if (checkedId == binding.btnDark.getId()) {
                settings.setTheme("dark");
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                settings.setTheme("light");
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }

            // apply instantly
            requireActivity().recreate();
        });

        // -- DEFAULT PERIOD
        List<String> periodLabels = Arrays.asList("Daily", "Weekly", "Monthly");
        ArrayAdapter<String> periodAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                periodLabels
        );
        periodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spDefaultPeriod.setAdapter(periodAdapter);

        String p = settings.getDefaultPeriod();
        int pos = p.equals("day") ? 0 : p.equals("week") ? 1 : 2;
        binding.spDefaultPeriod.setSelection(pos);

        // Set listener AFTER setting initial selection to avoid triggering toast on load
        binding.spDefaultPeriod.setOnItemSelectedListener(new SimpleItemSelectedListener(position -> {
            if (isInitialSetup) {
                isInitialSetup = false;
                return;
            }
            String value = position == 0 ? "day" : position == 1 ? "week" : "month";
            settings.setDefaultPeriod(value);
            ToastHelper.showSuccess(getContext(), "Saved");
        }));

        // --- CATEGORIES
        binding.toggleCatType.check(binding.btnCatIncome.getId());
        showingIncomeCats = true;

        binding.toggleCatType.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            showingIncomeCats = (checkedId == binding.btnCatIncome.getId());
            refreshCategories();
        });

        adapter = new CategoryAdapter(name -> {
            boolean ok = showingIncomeCats
                    ? settings.removeIncomeCategory(name)
                    : settings.removeExpenseCategory(name);

            if (!ok) ToastHelper.showError(getContext(), "Can't delete");
            refreshCategories();
        });

        binding.rvCategories.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvCategories.setAdapter(adapter);

        binding.btnAddCategory.setOnClickListener(v -> {
            String name = binding.etNewCategory.getText().toString().trim();
            if (name.isEmpty()) {
                ToastHelper.showError(getContext(), "Enter category name");
                return;
            }

            boolean ok = showingIncomeCats
                    ? settings.addIncomeCategory(name)
                    : settings.addExpenseCategory(name);

            if (!ok) {
                ToastHelper.showError(getContext(), "Already exists");
                return;
            }

            binding.etNewCategory.setText("");
            refreshCategories();
        });

        refreshCategories();
        return binding.getRoot();
    }

    private void refreshCategories() {
        List<String> list = showingIncomeCats
                ? settings.getIncomeCategories()
                : settings.getExpenseCategories();

        adapter.submitList(list);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
