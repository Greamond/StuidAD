package com.example.stuid.models;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.stuid.R;
import com.example.stuid.fragments.TasksFragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ProjectAdapter extends RecyclerView.Adapter<ProjectAdapter.ViewHolder> {
    public interface OnTaskButtonClickListener {
        void onTaskButtonClick(int position);
        void onProjectClick(int position); // Добавляем новый метод
    }
    private List<Project> projects;
    private List<Employee> employees = new ArrayList<>();;
    private OnTaskButtonClickListener listener;
    private int currentUserId;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView tvNumber, tvName, tvCreator;
        public Button btnTasks;

        public ViewHolder(View itemView) {
            super(itemView);
            tvNumber = itemView.findViewById(R.id.tvProjectNumber);
            tvName = itemView.findViewById(R.id.tvProjectName);
            tvCreator = itemView.findViewById(R.id.tvProjectCreator);
            btnTasks = itemView.findViewById(R.id.btnProjectTasks);
        }
    }

    public ProjectAdapter(List<Project> projects, OnTaskButtonClickListener listener, int currentUserId) {
        this.projects = projects != null ? projects : new ArrayList<>();
        this.listener = listener;
        this.currentUserId = currentUserId;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_project, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Project project = projects.get(position);
        holder.tvNumber.setText("Проект #" + project.getId());
        holder.tvName.setText(project.getName());

        // Безопасная проверка сотрудников
        String creatorName = "Неизвестный";
        if (employees != null) {  // Добавляем проверку на null
            for (Employee employee : employees) {
                if (employee.getEmployeeId() == project.getCreator()) {
                    creatorName = employee.getFullName();
                    break;
                }
            }
        }

        holder.tvCreator.setText("Создатель: " +
                (project.getCreator() == currentUserId ? "Вы" : creatorName));

        // Обработка клика на кнопке задач
        holder.btnTasks.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTaskButtonClick(position);
            }
        });

        // Обработка клика на всем элементе
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onProjectClick(position);
            }
        });
    }

    public void setEmployees(List<Employee> employees) {
        this.employees = employees != null ? employees : new ArrayList<>();
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return projects.size();
    }

    public List<Project> getProjects() {
        return projects;
    }

    public void updateProjects(List<Project> projects) {
        this.projects.clear();

        // Сортируем: сначала проекты пользователя, затем остальные
        Collections.sort(projects, (p1, p2) -> {
            if (p1.getCreator() == currentUserId && p2.getCreator() != currentUserId) return -1;
            if (p1.getCreator() != currentUserId && p2.getCreator() == currentUserId) return 1;
            return p1.getName().compareToIgnoreCase(p2.getName());
        });

        this.projects.addAll(projects);
        notifyDataSetChanged();
    }

    public void addProject(Project project) {
        // Вставляем в начало списка
        projects.add(0, project);
        // Уведомляем адаптер о вставке в позицию 0
        notifyItemInserted(0);
    }
}
