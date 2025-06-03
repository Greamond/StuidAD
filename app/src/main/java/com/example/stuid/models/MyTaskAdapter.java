package com.example.stuid.models;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.stuid.R;
import com.example.stuid.api.ApiClient;

import java.util.ArrayList;
import java.util.List;

public class MyTaskAdapter extends RecyclerView.Adapter<MyTaskAdapter.ViewHolder> {

    private List<Task> tasks;
    private OnTaskClickListener listener;
    private List<Employee> employees;
    private int currentUserId;
    private ApiClient apiClient;
    private String authToken;

    public void updateData(List<Task> newTasks, List<Employee> newEmployees) {
        this.tasks = new ArrayList<>(newTasks);
        this.employees = new ArrayList<>(newEmployees);
        notifyDataSetChanged();
    }

    public interface OnTaskClickListener {
        void onTaskClick(Task task);
    }

    public MyTaskAdapter(List<Task> tasks,
                         List<Employee> employees,
                         int currentUserId,
                         ApiClient apiClient,
                         String authToken) {
        this.tasks = new ArrayList<>(tasks); // Создаём новый список
        this.employees = new ArrayList<>(employees); // Создаём новый список
        this.currentUserId = currentUserId;
        this.apiClient = apiClient;
        this.authToken = authToken;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTaskNumber, tvTaskName, tvCreatorOrAssignee, tvSubtasks;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTaskNumber = itemView.findViewById(R.id.tvTaskNumber);
            tvTaskName = itemView.findViewById(R.id.tvTaskName);
            tvCreatorOrAssignee = itemView.findViewById(R.id.tvTaskCreator);
            tvSubtasks = itemView.findViewById(R.id.tvSubtasks);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Task task = tasks.get(position);

        holder.tvTaskNumber.setText("№" + task.getId());
        holder.tvTaskName.setText(task.getName());

        String creatorName = "Неизвестный";
        for (Employee employee : employees) {
            if (employee.getEmployeeId() == task.getCreatorId()) {
                creatorName = employee.getFullName();
                break;
            }
        }

        if (task.getCreatorId() == currentUserId) {
            creatorName = "Вы";
        }

        holder.tvCreatorOrAssignee.setText("Создатель: " + creatorName);
        holder.tvSubtasks.setVisibility(View.GONE);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTaskClick(task);
            }
        });
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    public void updateTasks(List<Task> newTasks) {
        this.tasks.clear();
        this.tasks.addAll(newTasks);
        notifyDataSetChanged(); // Уведомляем об изменениях
    }
}
