package com.example.stuid.models;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.stuid.R;

import java.util.List;

public class ProjectAdapter extends RecyclerView.Adapter<ProjectAdapter.ViewHolder> {
    private List<Project> projects;
    private OnTaskButtonClickListener listener;

    public interface OnTaskButtonClickListener {
        void onTaskButtonClick(int position);
    }

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

    public ProjectAdapter(List<Project> projects, OnTaskButtonClickListener listener) {
        this.projects = projects;
        this.listener = listener;
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
        holder.tvCreator.setText("Создатель: " + project.getCreator());

        holder.btnTasks.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTaskButtonClick(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return projects.size();
    }

    public List<Project> getProjects() {
        return projects;
    }

    public void updateProjects(List<Project> newProjects) {
        projects.clear();
        projects.addAll(newProjects);
        notifyDataSetChanged();
    }

    public void addProject(Project project) {
        // Вставляем в начало списка
        projects.add(0, project);
        // Уведомляем адаптер о вставке в позицию 0
        notifyItemInserted(0);
    }
}
