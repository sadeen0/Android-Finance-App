package com.labproject.app.ui.home;

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

public class RecentTransactionsAdapter extends RecyclerView.Adapter<RecentTransactionsAdapter.VH> {

    private List<Transaction> list = new ArrayList<>();
    private final SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    public void submit(List<Transaction> newList) {
        list = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recent_tx, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Transaction t = list.get(position);

        String sign = (t.type == Transaction.INCOME) ? "+ " : "- ";
        h.tvAmount.setText(String.format(Locale.US, "%s$%.2f", sign, t.amount));

        h.tvDesc.setText(t.description == null ? "" : t.description);

        String date = fmt.format(new Date(t.dateMillis));
        String cat = (t.category == null ? "-" : t.category);
        h.tvMeta.setText(cat + " • " + date);

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
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvAmount, tvDesc, tvMeta, tvRecurrence, tvInactiveBadge;
        ImageView statusIndicator;

        VH(@NonNull View itemView) {
            super(itemView);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvDesc = itemView.findViewById(R.id.tvDesc);
            tvMeta = itemView.findViewById(R.id.tvMeta);
            tvRecurrence = itemView.findViewById(R.id.tvRecurrence);
            tvInactiveBadge = itemView.findViewById(R.id.tvInactiveBadge);
            statusIndicator = itemView.findViewById(R.id.ivStatusIndicator);
        }
    }
}
