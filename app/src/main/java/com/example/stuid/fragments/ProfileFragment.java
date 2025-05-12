package com.example.stuid.fragments;

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
        TextView tvAbout = view.findViewById(R.id.tvAbout);
        Button btnEdit = view.findViewById(R.id.btnEditProfile);

        // Здесь можно установить данные из ViewModel или SharedPreferences
        tvFullName.setText("Петров Алексей Сергеевич");
        tvEmail.setText("alexey.petrov@example.com");
        tvAbout.setText("Full-stack разработчик с 5-летним опытом. Специализация: мобильные приложения и веб-разработка.");

        btnEdit.setOnClickListener(v -> {
            // Логика перехода к редактированию профиля
        });

        return view;
    }
}