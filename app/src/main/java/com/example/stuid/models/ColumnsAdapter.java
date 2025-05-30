package com.example.stuid.models;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.stuid.R;
import com.example.stuid.api.ApiClient;

import java.util.List;

public class ColumnsAdapter extends RecyclerView.Adapter<ColumnsAdapter.ColumnViewHolder> {
    private List<TaskColumn> columns;
    private OnTaskClickListener taskClickListener;
    private OnAddTaskListener addTaskListener;
    private List<Employee> projectParticipants;
    private int currentUserId;
    private ApiClient apiClient;
    private String authToken;

    public interface OnTaskClickListener {
        void onTaskClick(Task task);
    }

    public interface OnAddTaskListener {
        void onAddTask(int columnId);
    }

    public ColumnsAdapter(List<TaskColumn> columns,
                          List<Employee> participants,
                          int currentUserId,
                          ApiClient apiClient,
                          String authToken,
                          OnTaskClickListener taskClickListener,
                          OnAddTaskListener addTaskListener) {
        this.columns = columns;
        this.projectParticipants = participants;
        this.currentUserId = currentUserId;
        this.apiClient = apiClient;
        this.authToken = authToken;
        this.taskClickListener = taskClickListener;
        this.addTaskListener = addTaskListener;
    }

    @NonNull
    @Override
    public ColumnViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_column, parent, false);
        return new ColumnViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ColumnViewHolder holder, int position) {
        holder.bind(columns.get(position));
    }

    @Override
    public int getItemCount() {
        return columns.size();
    }

    class ColumnViewHolder extends RecyclerView.ViewHolder {
        private TextView columnTitle;
        private RecyclerView tasksRecyclerView;
        private Button addTaskButton;

        public ColumnViewHolder(@NonNull View itemView) {
            super(itemView);
            columnTitle = itemView.findViewById(R.id.tvColumnTitle);
            tasksRecyclerView = itemView.findViewById(R.id.rvTasks);
            addTaskButton = itemView.findViewById(R.id.btnAddTaskToColumn);
        }

        public void bind(TaskColumn column) {
            columnTitle.setText(column.getName());

            // Настройка списка задач в колонке
            tasksRecyclerView.setLayoutManager(new LinearLayoutManager(itemView.getContext()));

            // Создаем TaskAdapter с правильными параметрами
            TaskAdapter taskAdapter = new TaskAdapter(
                    column.getTasks(),
                    projectParticipants, // нужно добавить это поле в ColumnsAdapter
                    currentUserId,      // нужно добавить это поле в ColumnsAdapter
                    apiClient,          // нужно добавить это поле в ColumnsAdapter
                    authToken           // нужно добавить это поле в ColumnsAdapter
            );

            taskAdapter.setOnTaskClickListener(task -> {
                if (taskClickListener != null) {
                    taskClickListener.onTaskClick(task);
                }
            });

            tasksRecyclerView.setAdapter(taskAdapter);

            // Кнопка добавления задачи в колонку
            addTaskButton.setOnClickListener(v -> {
                if (addTaskListener != null) {
                    addTaskListener.onAddTask(column.getId());
                }
            });
        }
    }
}
