package com.example.stuid.models;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.stuid.R;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.ArrayList;
import java.util.List;

public class EmployeeAdapter extends RecyclerView.Adapter<EmployeeAdapter.ViewHolder> {
    private List<Employee> employees;
    private Context context;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ShapeableImageView ivPhoto;
        public TextView tvName, tvEmail, tvDescription;

        public ViewHolder(View itemView) {
            super(itemView);
            ivPhoto = itemView.findViewById(R.id.ivEmployeePhoto);
            tvName = itemView.findViewById(R.id.tvEmployeeName);
            tvEmail = itemView.findViewById(R.id.tvEmployeeEmail);
            tvDescription = itemView.findViewById(R.id.tvEmployeeDescription);
        }
    }

    public EmployeeAdapter(List<Employee> employees, Context context) {
        this.employees = employees;
        this.context = context;
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
        holder.tvDescription.setText(employee.getDescription());

        // Загрузка фото
        if (employee.getPhoto() != null && !employee.getPhoto().isEmpty()) {
            try {
                byte[] imageBytes = Base64.decode(employee.getPhoto(), Base64.DEFAULT);
                Glide.with(holder.itemView.getContext())
                        .load(imageBytes)
                        .circleCrop()
                        .placeholder(R.drawable.ic_profile)
                        .error(R.drawable.ic_profile)
                        .into(holder.ivPhoto);
            } catch (Exception e) {
                Log.e("IMAGE_LOAD", "Error loading image", e);
                holder.ivPhoto.setImageResource(R.drawable.ic_profile);
            }
        } else {
            holder.ivPhoto.setImageResource(R.drawable.ic_profile);
        }
    }

    @Override
    public int getItemCount() {
        return employees.size();
    }

    public void updateEmployees(List<Employee> newEmployees) {
        this.employees = new ArrayList<>(newEmployees);
        notifyDataSetChanged();
    }
}
