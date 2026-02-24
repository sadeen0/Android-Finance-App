package com.labproject.app.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.labproject.app.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CategoryBreakdownAdapter extends RecyclerView.Adapter<CategoryBreakdownAdapter.VH> {

    private List<CategoryStat> list = new ArrayList<>();
    private double grandTotal = 0.0;

    public void submit(List<CategoryStat> newList, double total) {
        list = newList;
        grandTotal = Math.max(0.0, total);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category_breakdown, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        CategoryStat s = list.get(position);

        h.tvCategory.setText(s.category);
        h.tvAmount.setText(String.format(Locale.US, "$%.2f", s.total));

        int pct = 0;
        if (grandTotal > 0) {
            pct = (int) Math.round((s.total * 100.0) / grandTotal);
        }
        h.tvPercent.setText(String.format(Locale.US, "%d%%", pct));
        h.progress.setProgress(pct);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvCategory, tvAmount, tvPercent;
        LinearProgressIndicator progress;

        VH(@NonNull View itemView) {
            super(itemView);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvPercent = itemView.findViewById(R.id.tvPercent);
            progress = itemView.findViewById(R.id.progress);
        }
    }
}
