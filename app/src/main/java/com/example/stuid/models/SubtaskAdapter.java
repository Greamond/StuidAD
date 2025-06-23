package com.example.stuid.models;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipDescription;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.stuid.R;
import com.example.stuid.api.ApiClient;
import com.example.stuid.api.CreatorNameCallback;
import com.example.stuid.api.EmployeeCallback;
import com.example.stuid.fragments.SubtasksFragment;

import java.util.List;

public class SubtaskAdapter extends RecyclerView.Adapter<SubtaskAdapter.ViewHolder> {
    private List<Subtask> subtasks;
    private OnSubtaskClickListener subtaskClickListener;
    private RecyclerView recyclerView;
    private List<Employee> projectParticipants;
    private int currentUserId;
    private ApiClient apiClient;
    private String authToken;
    private ItemTouchHelper itemTouchHelper;

    public void setItemTouchHelper(ItemTouchHelper helper) {
        this.itemTouchHelper = helper;
    }

    public interface OnSubtaskClickListener {
        void onSubtaskClick(Subtask subtask);
    }

    public void setOnSubtaskClickListener(OnSubtaskClickListener listener) {
        this.subtaskClickListener = listener;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView tvNumber, tvName, tvResponsible, tvSubtasks;

        public ViewHolder(View itemView) {
            super(itemView);
            tvNumber = itemView.findViewById(R.id.tvTaskNumber);
            tvName = itemView.findViewById(R.id.tvTaskName);
            tvResponsible = itemView.findViewById(R.id.tvTaskCreator);
            tvSubtasks = itemView.findViewById(R.id.tvSubtasks);
        }

        public void bind(Subtask subtask) {
            tvNumber.setText("№" + subtask.getId());
            tvName.setText(subtask.getName());
            tvSubtasks.setVisibility(View.GONE);

            tvResponsible.setText("Ответственный: загрузка..." );
            // Получаем имя ответственного
            getResponsibleName(subtask.getResponsibleId(), new CreatorNameCallback() {
                @Override
                public void onNameLoaded(String responsibleName) {
                    tvResponsible.setText("Ответственный: " + responsibleName);
                }

                @Override
                public void onFailure(String error) {
                    tvResponsible.setText("Ответственный: неизвестный");
                }
            });
        }
    }

    public SubtaskAdapter(List<Subtask> subtasks,
                          List<Employee> participants,
                          int currentUserId,
                          ApiClient apiClient,
                          String authToken,
                          RecyclerView recyclerView,
                          ItemTouchHelper itemTouchHelper) {
        this.subtasks = subtasks;
        this.projectParticipants = participants;
        this.currentUserId = currentUserId;
        this.apiClient = apiClient;
        this.authToken = authToken;
        this.recyclerView = recyclerView;
        this.itemTouchHelper = itemTouchHelper;
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
        Subtask subtask = subtasks.get(position);
        holder.bind(subtask);

        holder.itemView.findViewById(R.id.dragHandle).setOnLongClickListener(v -> {
            SubtasksFragment.setDraggedSubtask(subtask);

            ClipData.Item item = new ClipData.Item((CharSequence) null);
            ClipData dragData = new ClipData("subtask", new String[]{ClipDescription.MIMETYPE_TEXT_PLAIN}, item);
            View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(holder.itemView);
            holder.itemView.startDrag(dragData, shadowBuilder, null, 0);
            return true;
        });

        holder.itemView.setOnClickListener(v -> {
            if (subtaskClickListener != null) {
                subtaskClickListener.onSubtaskClick(subtask);
            }
        });

        holder.itemView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (itemTouchHelper != null) {
                    itemTouchHelper.startDrag(holder);
                } else {
                    Log.e("TaskAdapter", "ItemTouchHelper is null");
                }
            }
            return false;
        });
    }

    private void getResponsibleName(int responsibleId, CreatorNameCallback callback) {
        for (Employee employee : projectParticipants) {
            if (employee.getEmployeeId() == responsibleId) {
                callback.onNameLoaded(employee.getFullName());
                return;
            }
        }

        if (responsibleId == currentUserId) {
            callback.onNameLoaded("Вы");
            return;
        }

        if (apiClient == null || authToken == null) {
            callback.onNameLoaded("неизвестный");
            return;
        }

        apiClient.getEmployeeInfo(authToken, responsibleId, new EmployeeCallback() {
            @Override
            public void onSuccess(Employee employee) {
                if (recyclerView != null && recyclerView.getContext() instanceof Activity) {
                    ((Activity) recyclerView.getContext()).runOnUiThread(() -> {
                        callback.onNameLoaded(employee.getFullName());
                    });
                }
            }

            @Override
            public void onFailure(String error) {
                if (recyclerView != null && recyclerView.getContext() instanceof Activity) {
                    ((Activity) recyclerView.getContext()).runOnUiThread(() -> {
                        callback.onNameLoaded("неизвестный");
                    });
                }
            }
        });
    }

    public void updateSubtasks(List<Subtask> newSubtasks) {
        this.subtasks.clear();
        this.subtasks.addAll(newSubtasks);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return subtasks != null ? subtasks.size() : 0;
    }
}
