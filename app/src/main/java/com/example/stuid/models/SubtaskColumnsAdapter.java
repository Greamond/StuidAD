package com.example.stuid.models;

import android.content.ClipData;
import android.graphics.Color;
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
import com.example.stuid.fragments.SubtasksFragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class SubtaskColumnsAdapter extends RecyclerView.Adapter<SubtaskColumnsAdapter.ColumnViewHolder> {
    private List<SubtaskColumn> columns;
    private OnSubtaskClickListener subtaskClickListener;
    private OnAddSubtaskListener addSubtaskListener;
    private List<Employee> projectParticipants;
    private int currentUserId;
    private ApiClient apiClient;
    private String authToken;
    private SparseArray<SubtaskAdapter> taskAdapters = new SparseArray<>();
    private OnSubtaskMovedListener subtaskMovedListener;
    private SparseArray<ItemTouchHelper> itemTouchHelpers = new SparseArray<>();
    private final SubtasksFragment subtasksFragment;

    public interface OnSubtaskMovedListener {
        void onSubtaskMoved(Subtask subtask, int newChapterId);
    }

    public interface OnSubtaskClickListener {
        void onSubtaskClick(Subtask subtask);
    }

    public interface OnAddSubtaskListener {
        void onAddSubtask(int chapterId);
    }

    public SubtaskColumnsAdapter(List<SubtaskColumn> columns,
                                 List<Employee> participants,
                                 int currentUserId,
                                 ApiClient apiClient,
                                 String authToken,
                                 OnSubtaskClickListener listener,
                                 OnAddSubtaskListener addListener,
                                 OnSubtaskMovedListener movedListener,
                                 SubtasksFragment fragment) {
        this.columns = columns;
        this.projectParticipants = participants;
        this.currentUserId = currentUserId;
        this.apiClient = apiClient;
        this.authToken = authToken;
        this.subtaskClickListener = listener;
        this.addSubtaskListener = addListener;
        this.subtaskMovedListener = movedListener;
        this.subtasksFragment = fragment;
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

        public void bind(SubtaskColumn column) {
            columnTitle.setText(column.getName());

            columnTitle.setOnClickListener(v -> {
                if (subtasksFragment != null && subtasksFragment instanceof SubtasksFragment) {
                    ((SubtasksFragment) subtasksFragment).showAddColumnDialog(column);
                }
            });

            tasksRecyclerView.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
            tasksRecyclerView.setTag(column);

            ItemTouchHelper helper = itemTouchHelpers.get(column.getId());
            if (helper == null) {
                helper = new ItemTouchHelper(createSubtaskTouchCallback(column));
                itemTouchHelpers.put(column.getId(), helper);
            }

            helper.attachToRecyclerView(tasksRecyclerView);

            SubtaskAdapter adapter = new SubtaskAdapter(
                    column.getSubtasks(),
                    projectParticipants,
                    currentUserId,
                    apiClient,
                    authToken,
                    tasksRecyclerView,
                    helper
            );

            tasksRecyclerView.setAdapter(adapter);
            taskAdapters.put(column.getId(), adapter);

            adapter.setOnSubtaskClickListener(task -> {
                if (subtaskClickListener != null) {
                    subtaskClickListener.onSubtaskClick(task);
                }
            });

            // Кнопка добавления подзадачи
            addTaskButton.setOnClickListener(v -> {
                if (addSubtaskListener != null) {
                    addSubtaskListener.onAddSubtask(column.getId());
                }
            });

            // Обработка перетаскивания подзадач
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
                            Subtask droppedSubtask = SubtasksFragment.getDraggedSubtask();
                            if (droppedSubtask != null) {
                                int oldChapterId = droppedSubtask.getChapterId();
                                int newChapterId = column.getId();

                                if (oldChapterId != newChapterId) {
                                    updateSubtaskChapter(droppedSubtask.getId(), newChapterId);

                                    // Удаляем из старой колонки
                                    for (SubtaskColumn oldColumn : columns) {
                                        if (oldColumn.getId() == oldChapterId) {
                                            oldColumn.getSubtasks().remove(droppedSubtask);
                                            taskAdapters.get(oldChapterId).notifyDataSetChanged();
                                        }
                                    }

                                    // Добавляем в новую
                                    column.getSubtasks().add(droppedSubtask);
                                    adapter.notifyItemInserted(column.getSubtasks().size() - 1);
                                }

                                SubtasksFragment.setDraggedSubtask(null);
                            }
                        }
                        v.setBackgroundColor(Color.TRANSPARENT);
                        break;
                }
                return true;
            });
        }
    }

    private ItemTouchHelper.SimpleCallback createSubtaskTouchCallback(SubtaskColumn column) {
        return new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                int from = viewHolder.getAdapterPosition();
                int to = target.getAdapterPosition();

                List<Subtask> subtasks = column.getSubtasks();
                Collections.swap(subtasks, from, to);
                SubtaskAdapter adapter = (SubtaskAdapter) recyclerView.getAdapter();
                adapter.updateSubtasks(new ArrayList<>(subtasks));

                // Отправляем новый порядок на сервер
                subtasksFragment.sendNewOrderToServer(column.getTaskId(), column.getId(), subtasks);

                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                // Не используется
            }
        };
    }

    public void removeSubtask(Subtask subtaskToRemove) {
        boolean taskRemoved = false;

        for (SubtaskColumn column : columns) {
            List<Subtask> tasks = column.getSubtasks();
            Iterator<Subtask> iterator = tasks.iterator();

            while (iterator.hasNext()) {
                Subtask task = iterator.next();
                if (task.getId() == subtaskToRemove.getId()) {
                    iterator.remove(); // Удаляем из списка
                    taskRemoved = true;
                    break;
                }
            }

            if (taskRemoved) {
                SubtaskAdapter adapter = taskAdapters.get(column.getId());
                if (adapter != null) {
                    adapter.updateSubtasks(new ArrayList<>(tasks)); // Обновляем адаптер
                }
                return; // Выходим, если задача найдена и удалена
            }
        }
    }

    private void updateSubtaskChapter(int taskId, int newChapterId) {
        if (subtaskMovedListener != null) {
            for (SubtaskColumn column : columns) {
                for (Subtask subtask : column.getSubtasks()) {
                    if (subtask.getId() == taskId) {
                        subtask.setChapterId(newChapterId);
                        subtaskMovedListener.onSubtaskMoved(subtask, newChapterId);
                        return;
                    }
                }
            }
        }
    }
}