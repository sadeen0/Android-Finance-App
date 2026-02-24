package com.labproject.app.ui.budgets;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.labproject.app.R;
import com.labproject.app.ui.Models.Budget;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BudgetAdapter extends RecyclerView.Adapter<BudgetAdapter.BudgetViewHolder> {

    private List<Budget> budgets = new ArrayList<>();
    private final Listener listener;

    public interface Listener {
        void onClick(Budget budget);
        void onLongClick(Budget budget);
    }

    public BudgetAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submitList(List<Budget> list) {
        budgets = (list == null) ? new ArrayList<>() : new ArrayList<>(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BudgetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_budget, parent, false);
        return new BudgetViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull BudgetViewHolder h, int position) {
        Budget b = budgets.get(position);

        h.tvCategory.setText(b.category);
        h.tvPeriod.setText(b.period);
        h.tvLimit.setText(String.format(Locale.US, "Budget: $%.2f", b.limitAmount));
        h.tvSpent.setText(String.format(Locale.US, "Spent: $%.2f", b.spent));
        h.tvRemaining.setText(String.format(Locale.US, "Remaining: $%.2f", b.getRemaining()));

        int percentage = b.getPercentageUsed();
        h.progressBar.setProgress(Math.min(percentage, 100));
        h.tvProgress.setText(percentage + "%");

        int color;
        if (b.isOverBudget()) {
            color = h.itemView.getContext().getResources().getColor(R.color.red_negative);
        } else if (b.alertTriggered) {
            color = h.itemView.getContext().getResources().getColor(android.R.color.holo_orange_dark);
        } else {
            color = h.itemView.getContext().getResources().getColor(R.color.colorPrimary);
        }

        h.progressBar.setProgressTintList(android.content.res.ColorStateList.valueOf(color));
        h.tvProgress.setTextColor(color);

        if (b.alertTriggered && !b.isOverBudget()) {
            h.tvAlert.setVisibility(View.VISIBLE);
            h.tvAlert.setText("⚠ " + b.alertThreshold + "% threshold reached!");
        } else if (b.isOverBudget()) {
            h.tvAlert.setVisibility(View.VISIBLE);
            h.tvAlert.setText("❌ Over budget!");
        } else {
            h.tvAlert.setVisibility(View.GONE);
        }

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(b);
        });

        h.itemView.setOnLongClickListener(v -> {
            if (listener != null) listener.onLongClick(b);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return budgets.size();
    }

    static class BudgetViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategory, tvPeriod, tvLimit, tvSpent, tvRemaining, tvProgress, tvAlert;
        ProgressBar progressBar;

        public BudgetViewHolder(@NonNull View v) {
            super(v);
            tvCategory = v.findViewById(R.id.tvBudgetCategory);
            tvPeriod = v.findViewById(R.id.tvBudgetPeriod);
            tvLimit = v.findViewById(R.id.tvBudgetLimit);
            tvSpent = v.findViewById(R.id.tvBudgetSpent);
            tvRemaining = v.findViewById(R.id.tvBudgetRemaining);
            tvProgress = v.findViewById(R.id.tvBudgetProgress);
            tvAlert = v.findViewById(R.id.tvBudgetAlert);
            progressBar = v.findViewById(R.id.progressBudget);
        }
    }
}
