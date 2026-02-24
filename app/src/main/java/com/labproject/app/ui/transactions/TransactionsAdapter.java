package com.labproject.app.ui.transactions;

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
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TransactionsAdapter extends RecyclerView.Adapter<TransactionsAdapter.VH> {

    public interface Listener {
        void onClick(Transaction t);
        void onLongClick(Transaction t);
    }

    private final Listener listener;
    private List<Transaction> list = new ArrayList<>();

    public TransactionsAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submitList(List<Transaction> newList) {
        list = (newList == null) ? new ArrayList<>() : newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transaction, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Transaction t = list.get(position);

        boolean isIncome = (t.type == Transaction.INCOME);
        String sign = isIncome ? "+ " : "- ";

        h.tvAmount.setText(String.format(Locale.US, "%s$%.2f", sign, t.amount));
        h.tvDesc.setText(t.description == null ? "" : t.description);

        String date = DateFormat.format("yyyy-MM-dd", new Date(t.dateMillis)).toString();
        h.tvMeta.setText(t.category + " • " + date);

        // Show recurrence badge for recurring transactions
        if (h.tvRecurrence != null) {
            if (t.recurrence != null && !Transaction.RECURRENCE_ONCE.equals(t.recurrence)) {
                h.tvRecurrence.setText(t.getRecurrenceLabel());
                h.tvRecurrence.setVisibility(View.VISIBLE);
            } else {
                h.tvRecurrence.setVisibility(View.GONE);
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

        // Show inactive badge
        if (h.tvInactiveBadge != null) {
            h.tvInactiveBadge.setVisibility(t.isActive ? View.GONE : View.VISIBLE);
        }

        // Show end date
        if (h.tvEndDate != null) {
            if (t.hasEndDate()) {
                String endDate = DateFormat.format("yyyy-MM-dd", new Date(t.endDateMillis)).toString();
                h.tvEndDate.setText("Ends: " + endDate);
                h.tvEndDate.setVisibility(View.VISIBLE);
            } else {
                h.tvEndDate.setVisibility(View.GONE);
            }
        }

        h.itemView.setOnClickListener(v -> listener.onClick(t));
        h.itemView.setOnLongClickListener(v -> {
            listener.onLongClick(t);
            return true;
        });

        // Overflow menu click - same as long click
        if (h.overflowMenu != null) {
            h.overflowMenu.setOnClickListener(v -> listener.onLongClick(t));
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvAmount, tvDesc, tvMeta, tvRecurrence, tvInactiveBadge, tvEndDate;
        ImageView statusIndicator, overflowMenu;

        VH(@NonNull View itemView) {
            super(itemView);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvDesc = itemView.findViewById(R.id.tvDesc);
            tvMeta = itemView.findViewById(R.id.tvMeta);
            tvRecurrence = itemView.findViewById(R.id.tvRecurrence);
            tvInactiveBadge = itemView.findViewById(R.id.tvInactiveBadge);
            tvEndDate = itemView.findViewById(R.id.tvEndDate);
            statusIndicator = itemView.findViewById(R.id.ivStatusIndicator);
            overflowMenu = itemView.findViewById(R.id.ivOverflowMenu);
        }
    }
}
