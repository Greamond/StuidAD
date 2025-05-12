package com.example.stuid.models;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.stuid.R;

import java.util.List;

public class EmployeeAdapter extends RecyclerView.Adapter<EmployeeAdapter.ViewHolder> {
    private List<Employee> employees;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView tvName, tvEmail, tvAbout;

        public ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvEmployeeName);
            tvEmail = itemView.findViewById(R.id.tvEmployeeEmail);
            tvAbout = itemView.findViewById(R.id.tvEmployeeAbout);
        }
    }

    public EmployeeAdapter(List<Employee> employees) {
        this.employees = employees;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_employee, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Employee employee = employees.get(position);
        holder.tvName.setText(employee.getFullName());
        holder.tvEmail.setText(employee.getEmail());
        holder.tvAbout.setText(employee.getAbout());
    }

    @Override
    public int getItemCount() {
        return employees.size();
    }
}
