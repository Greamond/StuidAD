package com.example.stuid.models;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.stuid.R;
import com.example.stuid.api.ApiClient;
import com.example.stuid.api.CreatorNameCallback;
import com.example.stuid.api.EmployeeCallback;
import com.example.stuid.fragments.TasksFragment;

import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.ViewHolder> {
    private List<Task> tasks;
    private OnTaskClickListener taskClickListener;
    private RecyclerView recyclerView;
    private List<Employee> projectParticipants;
    private int currentUserId;
    private ApiClient apiClient;
    private String authToken;

    public interface OnTaskClickListener {
        void onTaskClick(Task task);
    }

    public void setOnTaskClickListener(OnTaskClickListener listener) {
        this.taskClickListener = listener;
    }


    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView tvNumber, tvName, tvCreator, tvChapter;
        private Task task;

        public ViewHolder(View itemView) {
            super(itemView);
            tvNumber = itemView.findViewById(R.id.tvTaskNumber);
            tvName = itemView.findViewById(R.id.tvTaskName);
            tvCreator = itemView.findViewById(R.id.tvTaskCreator);
            tvChapter = itemView.findViewById(R.id.tvTaskChapter);
        }

        // Метод для получения задачи
        public Task getTask() {
            return task;
        }

        // Метод для установки задачи
        public void bind(Task task) {
            this.task = task;
        }
    }

    public TaskAdapter(List<Task> tasks,
                       List<Employee> participants,
                       int currentUserId,
                       ApiClient apiClient,
                       String authToken) {
        this.tasks = tasks;
        this.projectParticipants = participants;
        this.currentUserId = currentUserId;
        this.apiClient = apiClient;
        this.authToken = authToken;
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
        holder.bind(task); // Теперь используем bind

        holder.tvNumber.setText("№" + task.getId());
        holder.tvName.setText(task.getName());
        holder.tvChapter.setText("Статус: " + getStatusText(task.getChapter()));

        getCreatorName(task.getCreatorId(), new CreatorNameCallback() {
            @Override
            public void onNameLoaded(String creatorName) {
                holder.tvCreator.setText("Создатель: " + creatorName);
            }

            @Override
            public void onFailure(String error) {
                holder.tvCreator.setText("Создатель: неизвестный");
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            TasksFragment.setDraggedTask(task);

            ClipData.Item item = new ClipData.Item((CharSequence) null);
            ClipData dragData = new ClipData("task", new String[]{ClipDescription.MIMETYPE_TEXT_PLAIN}, item);
            View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(holder.itemView);
            holder.itemView.startDrag(dragData, shadowBuilder, null, 0);
            return true;
        });

        holder.itemView.setOnClickListener(v -> {
            if (taskClickListener != null) {
                taskClickListener.onTaskClick(task);
            }
        });
    }

    // Метод для получения имени создателя
    private void getCreatorName(int creatorId, CreatorNameCallback callback) {
        // Проверяем локальный список участников (синхронно)
        for (Employee employee : projectParticipants) {
            if (employee.getEmployeeId() == creatorId) {
                callback.onNameLoaded(employee.getFullName());
                return;
            }
        }

        // Проверяем, не текущий ли это пользователь (синхронно)
        if (creatorId == currentUserId) {
            callback.onNameLoaded("Вы");
            return;
        }

        // Если API клиент не инициализирован (синхронно)
        if (apiClient == null || authToken == null) {
            callback.onNameLoaded("неизвестный");
            return;
        }

        // Запрос к серверу (асинхронно)
        apiClient.getEmployeeInfo(authToken, creatorId, new EmployeeCallback() {
            @Override
            public void onSuccess(Employee employee) {
                // Используем runOnUiThread для гарантии работы в UI потоке
                if (recyclerView != null && recyclerView.getContext() instanceof Activity) {
                    ((Activity) recyclerView.getContext()).runOnUiThread(() -> {
                        callback.onNameLoaded(employee.getFullName());
                    });
                }
            }

            @Override
            public void onFailure(String error) {
                // Аналогично для ошибки
                if (recyclerView != null && recyclerView.getContext() instanceof Activity) {
                    ((Activity) recyclerView.getContext()).runOnUiThread(() -> {
                        callback.onNameLoaded("неизвестный");
                    });
                }
            }
        });
    }

    public String getStatusText(int status) {
        switch (status) {
            case 1: return "Новая";
            case 2: return "В работе";
            case 3: return "На проверке";
            case 4: return "Завершена";
            default: return "Неизвестно";
        }
    }

    @Override
    public int getItemCount() {
        return tasks != null ? tasks.size() : 0;
    }

    public void updateTasks(List<Task> newTasks) {
        this.tasks = newTasks;
        notifyDataSetChanged();
    }
}
