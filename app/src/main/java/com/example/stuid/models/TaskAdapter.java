package com.example.stuid.models;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.stuid.R;

import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.ViewHolder> {
    private List<Task> tasks;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView tvNumber, tvName, tvAssignee, tvStatus;

        public ViewHolder(View itemView) {
            super(itemView);
            tvNumber = itemView.findViewById(R.id.tvTaskNumber);
            tvName = itemView.findViewById(R.id.tvTaskName);
            tvAssignee = itemView.findViewById(R.id.tvTaskAssignee);
            tvStatus = itemView.findViewById(R.id.tvTaskStatus);
        }
    }

    public TaskAdapter(List<Task> tasks) {
        this.tasks = tasks;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Task task = tasks.get(position);
        holder.tvNumber.setText("Задача #" + task.getNumber());
        holder.tvName.setText(task.getName());
        holder.tvAssignee.setText("Ответственный: " + task.getAssignee());
        holder.tvStatus.setText(task.getStatus());

        // Установка цвета статуса
        int statusColor = getStatusColor(task.getStatus());
        GradientDrawable drawable = (GradientDrawable) holder.tvStatus.getBackground();
        drawable.setColor(statusColor);
    }

    private int getStatusColor(String status) {
        switch (status.toLowerCase()) {
            case "в работе": return Color.parseColor("#FFA000"); // оранжевый
            case "завершена": return Color.parseColor("#4CAF50"); // зеленый
            case "новая": return Color.parseColor("#2196F3"); // голубой
            default: return Color.parseColor("#9E9E9E"); // серый
        }
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }
}
