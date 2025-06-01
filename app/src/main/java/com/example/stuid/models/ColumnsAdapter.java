package com.example.stuid.models;

import android.content.ClipData;
import android.graphics.Color;
import android.util.Log;
import android.util.SparseArray;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.stuid.R;
import com.example.stuid.api.ApiClient;
import com.example.stuid.fragments.TasksFragment;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ColumnsAdapter extends RecyclerView.Adapter<ColumnsAdapter.ColumnViewHolder> {
    private List<TaskColumn> columns;
    private OnTaskClickListener taskClickListener;
    private OnAddTaskListener addTaskListener;
    private List<Employee> projectParticipants;
    private int currentUserId;
    private ApiClient apiClient;
    private String authToken;
    private SparseArray<TaskAdapter> taskAdapters = new SparseArray<>();
    private OnTaskMovedListener taskMovedListener;
    private SparseArray<ItemTouchHelper> itemTouchHelpers = new SparseArray<>();
    private final TasksFragment tasksFragment;

    public interface OnTaskMovedListener {
        void onTaskMoved(Task task, int newChapterId);
    }

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
                          OnAddTaskListener addTaskListener,
                          OnTaskMovedListener taskMovedListener,
                          TasksFragment fragment) {
        this.columns = columns;
        this.projectParticipants = participants;
        this.currentUserId = currentUserId;
        this.apiClient = apiClient;
        this.authToken = authToken;
        this.taskClickListener = taskClickListener;
        this.addTaskListener = addTaskListener;
        this.taskMovedListener = taskMovedListener;
        this.tasksFragment = fragment;
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

            tasksRecyclerView.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
            tasksRecyclerView.setTag(column);

            ItemTouchHelper helperTask = new ItemTouchHelper(createTaskTouchCallback(column));
            helperTask.attachToRecyclerView(tasksRecyclerView);

            TaskAdapter taskAdapter = new TaskAdapter(
                    column.getTasks(),
                    projectParticipants,
                    currentUserId,
                    apiClient,
                    authToken,
                    helperTask
            );

            ItemTouchHelper helper = itemTouchHelpers.get(column.getId());
            if (helper == null) {
                helper = new ItemTouchHelper(createTaskTouchCallback(column));
                itemTouchHelpers.put(column.getId(), helper);
            }

            helper.attachToRecyclerView(tasksRecyclerView);
            tasksRecyclerView.setAdapter(taskAdapter);

            taskAdapter.setOnTaskClickListener(task -> {
                if (taskClickListener != null) {
                    taskClickListener.onTaskClick(task);
                }
            });

            tasksRecyclerView.setAdapter(taskAdapter);
            taskAdapters.put(column.getId(), taskAdapter);

            tasksRecyclerView.setOnDragListener((v, event) -> {
                switch (event.getAction()) {
                    case DragEvent.ACTION_DRAG_ENTERED:
                        v.setBackgroundColor(Color.parseColor("#e0e0e0"));
                        break;
                    case DragEvent.ACTION_DRAG_EXITED:
                        v.setBackgroundColor(Color.TRANSPARENT);
                        break;
                    case DragEvent.ACTION_DROP:
                        ClipData dragData = event.getClipData();
                        if (dragData != null && dragData.getItemCount() > 0) {
                            Task droppedTask = TasksFragment.getDraggedTask();
                            if (droppedTask != null) {
                                int oldChapterId = droppedTask.getChapter();
                                int newChapterId = column.getId();

                                if (oldChapterId != newChapterId) {
                                    updateTaskChapter(droppedTask.getId(), newChapterId);

                                    // Перемещаем локально
                                    for (TaskColumn oldColumn : columns) {
                                        if (oldColumn.getId() == oldChapterId) {
                                            oldColumn.getTasks().remove(droppedTask);
                                            taskAdapters.get(oldChapterId).notifyDataSetChanged();
                                        }
                                    }

                                    column.getTasks().add(droppedTask);
                                    taskAdapter.notifyItemInserted(column.getTasks().size() - 1);
                                }
                                TasksFragment.setDraggedTask(null);
                            }
                        }
                        v.setBackgroundColor(Color.TRANSPARENT);
                        break;
                }
                return true;
            });

            // Кнопка добавления задачи
            addTaskButton.setOnClickListener(v -> {
                if (addTaskListener != null) {
                    addTaskListener.onAddTask(column.getId());
                }
            });
        }
    }

    private ItemTouchHelper.SimpleCallback createTaskTouchCallback(TaskColumn column) {
        return new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                int from = viewHolder.getAdapterPosition();
                int to = target.getAdapterPosition();

                List<Task> tasks = column.getTasks();
                Collections.swap(tasks, from, to);
                TaskAdapter adapter = (TaskAdapter) recyclerView.getAdapter();
                adapter.updateTasks(new ArrayList<>(tasks));

                // Отправляем новый порядок на сервер
                tasksFragment.sendNewOrderToServer(column.getProjectId(), column.getId(), tasks);

                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                // Не используется
            }
        };
    }

    public void removeTask(Task taskToRemove) {
        for (TaskColumn column : columns) {
            int index = -1;
            for (int i = 0; i < column.getTasks().size(); i++) {
                if (column.getTasks().get(i).getId() == taskToRemove.getId()) {
                    index = i;
                    break;
                }
            }
            if (index >= 0) {
                column.getTasks().remove(index);
                TaskAdapter adapter = taskAdapters.get(column.getId());
                if (adapter != null) {
                    adapter.updateTasks(column.getTasks());
                }
                return;
            }
        }
    }

    private void updateTaskChapter(int taskId, int newChapterId) {
        if (taskMovedListener != null) {
            for (TaskColumn column : columns) {
                for (Task task : column.getTasks()) {
                    if (task.getId() == taskId) {
                        task.setChapter(newChapterId);
                        taskMovedListener.onTaskMoved(task, newChapterId);
                        return;
                    }
                }
            }
        }
    }

}