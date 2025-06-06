package com.example.stuid.fragments;

import static android.app.Activity.RESULT_OK;
import static android.content.Context.MODE_PRIVATE;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Base64;
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
import com.example.stuid.activity.SignInActivity;
import com.example.stuid.api.ApiClient;
import com.example.stuid.api.ProfilePhotoCallback;
import com.example.stuid.api.ProfilePhotoDownloadCallback;
import com.example.stuid.api.ProfileUpdateCallback;
import com.example.stuid.classes.FileUtil;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

public class ProfileFragment extends Fragment {
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final long MAX_PHOTO_SIZE_BYTES = 1024 * 1024;
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

        Button btnLogout = view.findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> showLogoutConfirmationDialog());

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

    private void showLogoutConfirmationDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Выход из аккаунта")
                .setMessage("Вы уверены, что хотите выйти?")
                .setPositiveButton("Выйти", (dialog, which) -> logoutUser())
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void logoutUser() {
        // Анимация при выходе
        requireActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out);

        // Очистка данных
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();

        // Переход на экран входа
        startActivity(new Intent(requireActivity(), SignInActivity.class));
        requireActivity().finishAffinity(); // Закрывает все активности
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
        int employeeId = prefs.getInt("employee_id", 0);
        String token = prefs.getString("jwt_token", "");
        String localImagePath = prefs.getString("profile_image", null);

        // Сначала пробуем загрузить локальное изображение
        if (localImagePath != null) {
            File imageFile = new File(localImagePath);
            if (imageFile.exists()) {
                Glide.with(this)
                        .load(imageFile)
                        .circleCrop()
                        .into(ivPhoto);
                return;
            }
        }

        // Если локального нет, загружаем с сервера
        apiClient.getProfilePhoto(employeeId, token, new ProfilePhotoDownloadCallback() {
            @Override
            public void onSuccess(String base64Image) {
                requireActivity().runOnUiThread(() -> {
                    try {
                        byte[] imageData = Base64.decode(base64Image, Base64.DEFAULT);

                        // Сохраняем локально
                        File storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                        File imageFile = File.createTempFile("profile_", ".jpg", storageDir);
                        FileOutputStream fos = new FileOutputStream(imageFile);
                        fos.write(imageData);
                        fos.close();

                        // Обновляем SharedPreferences
                        editor.putString("profile_image", imageFile.getAbsolutePath());
                        editor.apply();

                        // Показываем изображение
                        Glide.with(ProfileFragment.this)
                                .load(imageFile)
                                .circleCrop()
                                .into(ivPhoto);
                    } catch (IOException e) {
                        Toast.makeText(requireContext(),
                                "Ошибка сохранения фото", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                // Можно показать заглушку
                requireActivity().runOnUiThread(() -> {
                    ivPhoto.setImageResource(R.drawable.ic_profile);
                });
            }
        });
    }

    private void showEditProfileDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Редактировать профиль");

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_edit_profile, null);
        builder.setView(dialogView);

        TextInputLayout tilLastName = dialogView.findViewById(R.id.tilLastName);
        TextInputLayout tilFirstName = dialogView.findViewById(R.id.tilFirstName);
        TextInputLayout tilMiddleName = dialogView.findViewById(R.id.tilMiddleName);
        TextInputLayout tilDescription = dialogView.findViewById(R.id.tilDescription);

        EditText etLastName = dialogView.findViewById(R.id.etLastName);
        EditText etFirstName = dialogView.findViewById(R.id.etFirstName);
        EditText etMiddleName = dialogView.findViewById(R.id.etMiddleName);
        EditText etDescription = dialogView.findViewById(R.id.etDescription);

        Button btnSave = dialogView.findViewById(R.id.btnSave);

        setRussianNameInputFilter(etLastName);
        setRussianNameInputFilter(etFirstName);
        setRussianNameInputFilter(etMiddleName);

        addClearErrorTextWatcher(etLastName, tilLastName);
        addClearErrorTextWatcher(etFirstName, tilFirstName);
        addClearErrorTextWatcher(etMiddleName, tilMiddleName);
        addClearErrorTextWatcher(etDescription, tilDescription);

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

            boolean isValid = true;

            // Очистка предыдущих ошибок
            tilLastName.setError(null);
            tilFirstName.setError(null);
            tilMiddleName.setError(null);

            // Проверка обязательных полей
            if (lastName.isEmpty()) {
                tilLastName.setError("Фамилия обязательна");
                isValid = false;
            } else if (!isValidRussianName(lastName)) {
                tilLastName.setError("Фамилия должна начинаться с заглавной буквы и содержать только русские символы");
                isValid = false;
            }

            if (firstName.isEmpty()) {
                tilFirstName.setError("Имя обязательно");
                isValid = false;
            } else if (!isValidRussianName(firstName)) {
                tilFirstName.setError("Имя должно начинаться с заглавной буквы и содержать только русские символы");
                isValid = false;
            }

            if (middleName.isEmpty()) {
                tilMiddleName.setError("Отчество обязательно");
                isValid = false;
            } else if (!isValidRussianName(middleName)) {
                tilMiddleName.setError("Отчество должно начинаться с заглавной буквы и содержать только русские символы");
                isValid = false;
            }

            if (!isValid) return;

            updateProfile(lastName, firstName, middleName, description, dialog);
        });

        dialog.show();
    }

    private boolean isValidRussianName(String name) {
        return name != null && name.matches("^[А-Я][а-я]+$");
    }

    private void addClearErrorTextWatcher(EditText editText, TextInputLayout textInputLayout) {
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                textInputLayout.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setRussianNameInputFilter(EditText editText) {
        InputFilter filter = (source, start, end, dest, dstart, dend) -> {
            for (int i = start; i < end; i++) {
                char c = source.charAt(i);
                if (!Character.isLetter(c) || !(c >= 'А' && c <= 'я') && c != 'ё' && c != 'Ё') {
                    return "";
                }
            }
            return null;
        };
        editText.setFilters(new InputFilter[]{filter});
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
                // 1. Сохраняем изображение локально
                File storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                File imageFile = File.createTempFile("profile_", ".jpg", storageDir);

                InputStream in = requireContext().getContentResolver().openInputStream(imageUri);
                OutputStream out = new FileOutputStream(imageFile);
                FileUtil.copy(in, out);

                // 2. Конвертируем в Base64 для отправки на сервер
                byte[] imageData = Files.readAllBytes(imageFile.toPath());
                String base64Image = Base64.encodeToString(imageData, Base64.DEFAULT);

                // 3. Отправляем на сервер
                int employeeId = prefs.getInt("employee_id", 0);
                String token = prefs.getString("jwt_token", "");

                apiClient.uploadProfilePhoto(employeeId, base64Image, token, new ProfilePhotoCallback() {
                    @Override
                    public void onSuccess() {
                        requireActivity().runOnUiThread(() -> {
                            // 4. Сохраняем локальный путь только после успешной загрузки на сервер
                            editor.putString("profile_image", imageFile.getAbsolutePath());
                            editor.apply();

                            progressDialog.dismiss();
                            Glide.with(ProfileFragment.this)
                                    .load(imageFile)
                                    .circleCrop()
                                    .into(ivPhoto);

                            Toast.makeText(requireContext(),
                                    "Фото профиля обновлено", Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onFailure(String error) {
                        requireActivity().runOnUiThread(() -> {
                            progressDialog.dismiss();
                            Toast.makeText(requireContext(),
                                    "Ошибка загрузки фото: " + error, Toast.LENGTH_LONG).show();
                        });
                    }
                });

            } catch (IOException e) {
                requireActivity().runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(requireContext(),
                            "Ошибка обработки изображения", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
}