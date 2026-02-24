package com.labproject.app.ui.income;

import android.graphics.Color;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.labproject.app.R;
import com.labproject.app.ui.Models.Transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class IncomeAdapter extends RecyclerView.Adapter<IncomeAdapter.ViewHolder> {

    public interface Listener {
        void onClick(Transaction t);     // edit
        void onLongClick(Transaction t); // show options (delete/deactivate)
    }

    private final Listener listener;
    private List<Transaction> list = new ArrayList<>();

    public IncomeAdapter(Listener listener) {
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
                .inflate(R.layout.item_income, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Transaction t = list.get(position);

        holder.amount.setText(String.format(Locale.US, "+ $%.2f", t.amount));

        String desc = (t.description == null || t.description.trim().isEmpty())
                ? "—"
                : t.description;
        holder.desc.setText(desc);

        String date = DateFormat.format("yyyy-MM-dd", t.dateMillis).toString();
        String cat = (t.category == null || t.category.trim().isEmpty())
                ? "Other"
                : t.category;

        holder.meta.setText(cat + " • " + date);

        // Display recurrence info
        String recurrenceLabel = t.getRecurrenceLabel();
        if (holder.recurrence != null) {
            holder.recurrence.setText(recurrenceLabel);
            // Set visibility and color based on recurrence type
            if (Transaction.RECURRENCE_ONCE.equals(t.recurrence)) {
                holder.recurrence.setVisibility(View.GONE);
            } else {
                holder.recurrence.setVisibility(View.VISIBLE);
            }
        }

        // Handle inactive status
        if (holder.statusIndicator != null) {
            if (!t.isActive) {
                holder.statusIndicator.setVisibility(View.VISIBLE);
                holder.statusIndicator.setColorFilter(Color.parseColor("#FF9800")); // Orange for inactive
            } else if (t.hasEndDate()) {
                holder.statusIndicator.setVisibility(View.VISIBLE);
                holder.statusIndicator.setColorFilter(Color.parseColor("#2196F3")); // Blue for has end date
            } else {
                holder.statusIndicator.setVisibility(View.GONE);
            }
        }

        // Gray out inactive items
        float alpha = t.isActive ? 1.0f : 0.5f;
        holder.itemView.setAlpha(alpha);

        // Show end date if set
        if (holder.endDateText != null) {
            if (t.hasEndDate()) {
                String endDate = DateFormat.format("yyyy-MM-dd", t.endDateMillis).toString();
                holder.endDateText.setText("Ends: " + endDate);
                holder.endDateText.setVisibility(View.VISIBLE);
            } else {
                holder.endDateText.setVisibility(View.GONE);
            }
        }

        // Show inactive badge
        if (holder.inactiveBadge != null) {
            holder.inactiveBadge.setVisibility(t.isActive ? View.GONE : View.VISIBLE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(t);
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) listener.onLongClick(t);
            return true;
        });

        // Overflow menu click - same as long click
        if (holder.overflowMenu != null) {
            holder.overflowMenu.setOnClickListener(v -> {
                if (listener != null) listener.onLongClick(t);
            });
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView amount, desc, meta, recurrence, endDateText, inactiveBadge;
        ImageView statusIndicator, overflowMenu;

        ViewHolder(View itemView) {
            super(itemView);
            amount = itemView.findViewById(R.id.tvAmount);
            desc   = itemView.findViewById(R.id.tvDesc);
            meta   = itemView.findViewById(R.id.tvMeta);
            recurrence = itemView.findViewById(R.id.tvRecurrence);
            endDateText = itemView.findViewById(R.id.tvEndDate);
            inactiveBadge = itemView.findViewById(R.id.tvInactiveBadge);
            statusIndicator = itemView.findViewById(R.id.ivStatusIndicator);
            overflowMenu = itemView.findViewById(R.id.ivOverflowMenu);
        }
    }
}
