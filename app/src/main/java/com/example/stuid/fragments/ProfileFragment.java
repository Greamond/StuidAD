package com.example.stuid.fragments;

import static android.app.Activity.RESULT_OK;
import static android.content.Context.MODE_PRIVATE;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.stuid.R;
import com.example.stuid.api.ApiClient;
import com.example.stuid.api.ProfileUpdateCallback;
import com.example.stuid.classes.FileUtil;
import com.google.android.material.imageview.ShapeableImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ProfileFragment extends Fragment {
    private static final int PICK_IMAGE_REQUEST = 1;
    private ShapeableImageView ivPhoto;
    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;
    private ApiClient apiClient;
    private ProgressDialog progressDialog;
    private Uri selectedImageUri;

    @Override
    public View onCreateView(LayoutInflater inflator, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflator.inflate(R.layout.fragment_profile, container, false);

        // Инициализация элементов
        ivPhoto = view.findViewById(R.id.ivPhoto);
        TextView tvFullName = view.findViewById(R.id.tvFullName);
        TextView tvEmail = view.findViewById(R.id.tvEmail);
        TextView tvAbout = view.findViewById(R.id.tvDescription);
        Button btnEdit = view.findViewById(R.id.btnEditProfile);

        // Инициализация API клиента
        apiClient = new ApiClient();

        // Работа с SharedPreferences
        prefs = requireContext().getSharedPreferences("user_prefs", MODE_PRIVATE);
        editor = prefs.edit();

        // Загрузка данных
        loadProfileData(tvFullName, tvEmail, tvAbout);
        loadProfileImage();

        // Обработчики событий
        ivPhoto.setOnClickListener(v -> openImageChooser());
        btnEdit.setOnClickListener(v -> showEditProfileDialog());

        return view;
    }

    private void loadProfileData(TextView... textViews) {
        String fullName = prefs.getString("employee_fullName", "");
        String email = prefs.getString("employee_email", "");
        String description = prefs.getString("employee_description", "");

        textViews[0].setText(fullName);
        textViews[1].setText(email);
        textViews[2].setText(description);
    }

    private void loadProfileImage() {
        String imagePath = prefs.getString("profile_image", null);
        if (imagePath != null) {
            Glide.with(this)
                    .load(new File(imagePath))
                    .circleCrop()
                    .into(ivPhoto);
        }
    }

    private void showEditProfileDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Редактировать профиль");

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_edit_profile, null);
        builder.setView(dialogView);

        EditText etLastName = dialogView.findViewById(R.id.etLastName);
        EditText etFirstName = dialogView.findViewById(R.id.etFirstName);
        EditText etMiddleName = dialogView.findViewById(R.id.etMiddleName);
        EditText etDescription = dialogView.findViewById(R.id.etDescription);
        Button btnSave = dialogView.findViewById(R.id.btnSave);

        // Заполняем текущие данные
        String[] nameParts = prefs.getString("employee_fullName", "").split(" ");
        if (nameParts.length >= 3) {
            etLastName.setText(nameParts[0]);
            etFirstName.setText(nameParts[1]);
            etMiddleName.setText(nameParts[2]);
        }
        etDescription.setText(prefs.getString("employee_description", ""));

        AlertDialog dialog = builder.create();

        btnSave.setOnClickListener(v -> {
            String lastName = etLastName.getText().toString().trim();
            String firstName = etFirstName.getText().toString().trim();
            String middleName = etMiddleName.getText().toString().trim();
            String description = etDescription.getText().toString().trim();

            if (validateInput(lastName, firstName)) {
                updateProfile(lastName, firstName, middleName, description, dialog);
            }
        });

        dialog.show();
    }

    private void updateProfile(String lastName, String firstName, String middleName,
                               String description, AlertDialog dialog) {
        progressDialog = new ProgressDialog(requireContext());
        progressDialog.setMessage("Сохранение...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        int employeeId = prefs.getInt("employee_id", 0);
        String token = prefs.getString("jwt_token", "");

        apiClient.updateProfile(employeeId, lastName, firstName, middleName,
                description, token, new ProfileUpdateCallback() {
                    @Override
                    public void onSuccess() {
                        requireActivity().runOnUiThread(() -> {
                            progressDialog.dismiss();
                            dialog.dismiss();

                            // Обновляем локальные данные
                            String fullName = lastName + " " + firstName + " " + middleName;
                            editor.putString("employee_fullName", fullName);
                            editor.putString("employee_description", description);
                            editor.apply();

                            // Обновляем UI
                            TextView tvFullName = requireView().findViewById(R.id.tvFullName);
                            TextView tvDescription = requireView().findViewById(R.id.tvDescription);
                            tvFullName.setText(fullName);
                            tvDescription.setText(description);

                            Toast.makeText(requireContext(),
                                    "Профиль успешно обновлен", Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onFailure(String error) {
                        requireActivity().runOnUiThread(() -> {
                            progressDialog.dismiss();
                            Toast.makeText(requireContext(),
                                    "Ошибка обновления: " + error, Toast.LENGTH_LONG).show();
                        });
                    }
                });
    }

    private boolean validateInput(String lastName, String firstName) {
        if (lastName.isEmpty()) {
            Toast.makeText(requireContext(), "Введите фамилию", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (firstName.isEmpty()) {
            Toast.makeText(requireContext(), "Введите имя", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void openImageChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedImageUri = data.getData();
            saveImageToStorage(selectedImageUri);
        }
    }

    private void saveImageToStorage(Uri imageUri) {
        progressDialog = new ProgressDialog(requireContext());
        progressDialog.setMessage("Загрузка изображения...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        new Thread(() -> {
            try {
                File storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                File imageFile = File.createTempFile("profile_", ".jpg", storageDir);

                InputStream in = requireContext().getContentResolver().openInputStream(imageUri);
                OutputStream out = new FileOutputStream(imageFile);
                FileUtil.copy(in, out);

                // Сохраняем путь к изображению
                editor.putString("profile_image", imageFile.getAbsolutePath());
                editor.apply();

                requireActivity().runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Glide.with(this)
                            .load(imageFile)
                            .circleCrop()
                            .into(ivPhoto);
                    Toast.makeText(requireContext(),
                            "Фото профиля обновлено", Toast.LENGTH_SHORT).show();
                });
            } catch (IOException e) {
                requireActivity().runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(requireContext(),
                            "Ошибка сохранения изображения", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
}