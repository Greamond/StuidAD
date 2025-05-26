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
    private RecyclerView recyclerView;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView tvNumber, tvName, tvDescription, tvChapter;

        public ViewHolder(View itemView) {
            super(itemView);
            tvNumber = itemView.findViewById(R.id.tvTaskNumber);
            tvName = itemView.findViewById(R.id.tvTaskName);
            tvDescription = itemView.findViewById(R.id.tvTaskDescription);
            tvChapter = itemView.findViewById(R.id.tvTaskChapter);
        }
    }

    public TaskAdapter(List<Task> tasks, RecyclerView recyclerView) {
        this.tasks = tasks;
        this.recyclerView = recyclerView;
    }

    public void updateTasks(List<Task> newTasks) {
        this.tasks = newTasks;
        notifyDataSetChanged();
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
        holder.tvNumber.setText("№" + task.getId());
        holder.tvName.setText(task.getName());
        holder.tvDescription.setText(task.getDescription());
        holder.tvChapter.setText("Статус: " + getStatusText(task.getChapter()));

        holder.itemView.setOnClickListener(v -> {
            if (taskClickListener != null) {
                taskClickListener.onTaskClick(position, task);
            }
        });
    }

    public String getStatusText(int status) {
        switch (status) {
            case 1: return "Новая";
            case 2: return "В работе";
            case 3: return "Завершена";
            default: return "Неизвестно";
        }
    }

    @Override
    public int getItemCount() {
        return tasks != null ? tasks.size() : 0;
    }

    public void addTask(Task task) {
        tasks.add(0, task);
        notifyItemInserted(0);
        recyclerView.smoothScrollToPosition(0);
    }

    public interface OnTaskClickListener {
        void onTaskClick(int position, Task task);
    }

    private OnTaskClickListener taskClickListener;

    public void setOnTaskClickListener(OnTaskClickListener listener) {
        this.taskClickListener = listener;
    }
}
