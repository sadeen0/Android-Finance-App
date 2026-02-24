package com.labproject.app.ui.home;

import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.labproject.app.R;
import com.labproject.app.ui.Models.Budget;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BudgetAlertAdapter extends RecyclerView.Adapter<BudgetAlertAdapter.VH> {

    private List<Budget> alerts = new ArrayList<>();

    public void submitList(List<Budget> list) {
        alerts = (list == null) ? new ArrayList<>() : new ArrayList<>(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_budget_alert, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Budget b = alerts.get(position);
        int percent = b.getPercentageUsed();
        
        // Set category name
        h.tvCategory.setText(b.category);
        
        // Set percentage badge
        h.tvPercentage.setText(String.format(Locale.US, "%d%%", percent));
        
        // Determine styling based on budget status
        int badgeColor;
        int iconTint;
        int bgResource;
        
        if (b.isOverBudget()) {
            // Over budget - critical
            h.tvMessage.setText(String.format(Locale.US, "Over budget! $%.0f / $%.0f", b.spent, b.limitAmount));
            badgeColor = ContextCompat.getColor(h.itemView.getContext(), R.color.budget_danger);
            iconTint = ContextCompat.getColor(h.itemView.getContext(), R.color.budget_danger);
            bgResource = R.drawable.bg_budget_alert_item;
        } else if (percent >= 90) {
            // Critical threshold
            h.tvMessage.setText(String.format(Locale.US, "Critical! $%.0f / $%.0f", b.spent, b.limitAmount));
            badgeColor = ContextCompat.getColor(h.itemView.getContext(), R.color.budget_danger);
            iconTint = ContextCompat.getColor(h.itemView.getContext(), R.color.budget_danger);
            bgResource = R.drawable.bg_budget_alert_item;
        } else {
            // Warning threshold
            h.tvMessage.setText(String.format(Locale.US, "%d%% used • $%.0f / $%.0f", percent, b.spent, b.limitAmount));
            badgeColor = ContextCompat.getColor(h.itemView.getContext(), android.R.color.holo_orange_dark);
            iconTint = ContextCompat.getColor(h.itemView.getContext(), android.R.color.holo_orange_dark);
            bgResource = R.drawable.bg_budget_alert_item;
        }
        
        //  badge background color
        Drawable badgeBg = ContextCompat.getDrawable(h.itemView.getContext(), R.drawable.bg_alert_badge);
        if (badgeBg != null) {
            badgeBg = DrawableCompat.wrap(badgeBg.mutate());
            DrawableCompat.setTint(badgeBg, badgeColor);
            h.tvPercentage.setBackground(badgeBg);
        }
        
        // Apply icon tint
        h.ivIcon.setColorFilter(iconTint);
    }

    @Override
    public int getItemCount() {
        return alerts.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvCategory;
        TextView tvMessage;
        TextView tvPercentage;
        ImageView ivIcon;
        LinearLayout alertContainer;

        public VH(@NonNull View v) {
            super(v);
            tvCategory = v.findViewById(R.id.tvAlertCategory);
            tvMessage = v.findViewById(R.id.tvBudgetAlertMessage);
            tvPercentage = v.findViewById(R.id.tvAlertPercentage);
            ivIcon = v.findViewById(R.id.ivAlertIcon);
            alertContainer = v.findViewById(R.id.alertContainer);
        }
    }
}
