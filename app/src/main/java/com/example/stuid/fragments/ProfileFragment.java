package com.example.stuid.fragments;

import static android.content.Context.MODE_PRIVATE;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.example.stuid.R;

public class ProfileFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        TextView tvFullName = view.findViewById(R.id.tvFullName);
        TextView tvEmail = view.findViewById(R.id.tvEmail);
        TextView tvAbout = view.findViewById(R.id.tvDescription);
        Button btnEdit = view.findViewById(R.id.btnEditProfile);

        // Здесь можно установить данные из ViewModel или SharedPreferences
        SharedPreferences prefs = requireContext().getSharedPreferences("user_prefs", MODE_PRIVATE);

        String fullName = prefs.getString("employee_fullName", "");
        String email = prefs.getString("employee_email", "");
        String description = prefs.getString("employee_description", "");

        tvFullName.setText(fullName);
        tvEmail.setText(email);
        tvAbout.setText(description);

        btnEdit.setOnClickListener(v -> {
            // Логика перехода к редактированию профиля
        });

        return view;
    }
}