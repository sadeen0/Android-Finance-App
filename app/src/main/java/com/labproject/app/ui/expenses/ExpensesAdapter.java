package com.labproject.app.ui.expenses;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.labproject.app.R;
import com.labproject.app.ui.Models.Transaction;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ExpensesAdapter extends RecyclerView.Adapter<ExpensesAdapter.ViewHolder> {

    public interface Listener {
        void onClick(Transaction t);
        void onLongClick(Transaction t);
    }

    private final Listener listener;
    private List<Transaction> list = new ArrayList<>();

    public ExpensesAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submitList(List<Transaction> newList) {
        list = (newList == null) ? new ArrayList<>() : newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_expense, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Transaction t = list.get(position);

        h.amount.setText(String.format(Locale.US, "- $%.2f", t.amount));
        
        String desc = (t.description == null || t.description.trim().isEmpty())
                ? "—"
                : t.description;
        h.desc.setText(desc);

        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.US)
                .format(new Date(t.dateMillis));
        h.meta.setText((t.category == null ? "Other" : t.category) + " • " + date);

        // Display recurrence info
        String recurrenceLabel = t.getRecurrenceLabel();
        if (h.recurrence != null) {
            h.recurrence.setText(recurrenceLabel);
            // Set visibility based on recurrence type
            if (Transaction.RECURRENCE_ONCE.equals(t.recurrence)) {
                h.recurrence.setVisibility(View.GONE);
            } else {
                h.recurrence.setVisibility(View.VISIBLE);
            }
        }

        // Handle inactive status
        if (h.statusIndicator != null) {
            if (!t.isActive) {
                h.statusIndicator.setVisibility(View.VISIBLE);
                h.statusIndicator.setColorFilter(Color.parseColor("#FF9800")); // Orange for inactive
            } else if (t.hasEndDate()) {
                h.statusIndicator.setVisibility(View.VISIBLE);
                h.statusIndicator.setColorFilter(Color.parseColor("#2196F3")); // Blue for has end date
            } else {
                h.statusIndicator.setVisibility(View.GONE);
            }
        }

        // Gray out inactive items
        float alpha = t.isActive ? 1.0f : 0.5f;
        h.itemView.setAlpha(alpha);

        // Show end date if set
        if (h.endDateText != null) {
            if (t.hasEndDate()) {
                String endDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US)
                        .format(new Date(t.endDateMillis));
                h.endDateText.setText("Ends: " + endDate);
                h.endDateText.setVisibility(View.VISIBLE);
            } else {
                h.endDateText.setVisibility(View.GONE);
            }
        }

        // Show inactive badge
        if (h.inactiveBadge != null) {
            h.inactiveBadge.setVisibility(t.isActive ? View.GONE : View.VISIBLE);
        }

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(t);
        });

        h.itemView.setOnLongClickListener(v -> {
            if (listener != null) listener.onLongClick(t);
            return true;
        });

        // Overflow menu click - same as long click
        if (h.overflowMenu != null) {
            h.overflowMenu.setOnClickListener(v -> {
                if (listener != null) listener.onLongClick(t);
            });
        }
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView amount, desc, meta, recurrence, endDateText, inactiveBadge;
        ImageView statusIndicator, overflowMenu;

        ViewHolder(View itemView) {
            super(itemView);
            amount = itemView.findViewById(R.id.tvAmount);
            desc = itemView.findViewById(R.id.tvDesc);
            meta = itemView.findViewById(R.id.tvMeta);
            recurrence = itemView.findViewById(R.id.tvRecurrence);
            endDateText = itemView.findViewById(R.id.tvEndDate);
            inactiveBadge = itemView.findViewById(R.id.tvInactiveBadge);
            statusIndicator = itemView.findViewById(R.id.ivStatusIndicator);
            overflowMenu = itemView.findViewById(R.id.ivOverflowMenu);
        }
    }
}
