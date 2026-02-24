package com.labproject.app.ui.settings;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.labproject.app.R;

import java.util.ArrayList;
import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.VH> {

    public interface Listener {
        void onDelete(String name);
    }

    private final Listener listener;
    private List<String> list = new ArrayList<>();

    public CategoryAdapter(Listener l) {
        listener = l;
    }

    public void submitList(List<String> newList) {
        list = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_setting_category, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        String name = list.get(position);
        h.tvName.setText(name);
        h.btnDelete.setOnClickListener(v -> listener.onDelete(name));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, btnDelete;
        VH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvCatName);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
