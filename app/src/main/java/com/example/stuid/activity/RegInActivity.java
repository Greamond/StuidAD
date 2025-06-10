package com.example.stuid.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Patterns;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.example.stuid.R;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.stuid.R;
import com.example.stuid.api.ApiClient;
import com.example.stuid.api.AuthCallback;
import com.example.stuid.models.Employee;
import com.google.android.material.textfield.TextInputLayout;

public class RegInActivity extends AppCompatActivity {
    private ApiClient apiClient;
    private TextView tvLoginLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reg_in);

        apiClient = new ApiClient();

        // Находим TextInputLayout
        TextInputLayout tilLastName = findViewById(R.id.tilLastName);
        TextInputLayout tilFirstName = findViewById(R.id.tilFirstName);
        TextInputLayout tilMiddleName = findViewById(R.id.tilMiddleName);
        TextInputLayout tilEmail = findViewById(R.id.tilEmail);
        TextInputLayout tilPassword = findViewById(R.id.tilPassword);
        TextInputLayout tilConfirmPassword = findViewById(R.id.tilConfirmPassword);

        // Находим EditText
        EditText etLastName = findViewById(R.id.etLastName);
        EditText etFirstName = findViewById(R.id.etFirstName);
        EditText etMiddleName = findViewById(R.id.etMiddleName);
        EditText etEmail = findViewById(R.id.etEmail);
        EditText etPassword = findViewById(R.id.etPassword);
        EditText etConfirmPassword = findViewById(R.id.etConfirmPassword);

        setRussianNameInputFilter(etLastName);
        setRussianNameInputFilter(etFirstName);
        setRussianNameInputFilter(etMiddleName);

        addClearErrorTextWatcher(etLastName, tilLastName);
        addClearErrorTextWatcher(etFirstName, tilFirstName);
        addClearErrorTextWatcher(etMiddleName, tilMiddleName);
        addClearErrorTextWatcher(etEmail, tilEmail);
        addClearErrorTextWatcher(etPassword, tilPassword);
        addClearErrorTextWatcher(etConfirmPassword, tilConfirmPassword);

        Button registerButton = findViewById(R.id.btnRegister);
        TextView tvLoginLink = findViewById(R.id.tvLoginLink);

        tvLoginLink.setOnClickListener(v -> {
            startActivity(new Intent(RegInActivity.this, SignInActivity.class));
        });

        registerButton.setOnClickListener(v -> {

            String lastName = etLastName.getText().toString().trim();
            String firstName = etFirstName.getText().toString().trim();
            String middleName = etMiddleName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String confirmPassword = etConfirmPassword.getText().toString().trim();

            boolean isValid = true;

            // Очистка предыдущих ошибок
            tilLastName.setError(null);
            tilFirstName.setError(null);
            tilMiddleName.setError(null);
            tilEmail.setError(null);
            tilPassword.setError(null);
            tilConfirmPassword.setError(null);

            String NAME_REGEX = "^[А-Я][а-я]+$";

            if (lastName.isEmpty()) {
                tilLastName.setError("Фамилия обязательна");
                isValid = false;
            } else if (!lastName.matches(NAME_REGEX)) {
                tilLastName.setError("Фамилия должна начинаться с заглавной буквы и содержать только русские буквы");
                isValid = false;
            }

            if (firstName.isEmpty()) {
                tilFirstName.setError("Имя обязательно");
                isValid = false;
            } else if (!firstName.matches(NAME_REGEX)) {
                tilFirstName.setError("Имя должно начинаться с заглавной буквы и содержать только русские буквы");
                isValid = false;
            }

            if (middleName.isEmpty()){
                tilMiddleName.setError("Отчество обязательно");
                isValid = false;
            } else if (!middleName.matches(NAME_REGEX)) {
                tilMiddleName.setError("Отчество должно начинаться с заглавной буквы и содержать только русские буквы");
                isValid = false;
            }

            if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                tilEmail.setError("Введите корректную почту");
                isValid = false;
            }

            if (password.isEmpty()) {
                tilPassword.setError("Пароль обязателен");
                isValid = false;
            } else if (password.length() < 6) {
                tilPassword.setError("Пароль должен быть не менее 6 символов");
                isValid = false;
            }

            if (!password.equals(confirmPassword)) {
                tilConfirmPassword.setError("Пароли не совпадают");
                isValid = false;
            }

            if (!isValid) return;

            // Все проверки пройдены — регистрируем пользователя
            apiClient.registerUser(firstName, lastName, middleName, email, password, new AuthCallback() {
                @Override
                public void onSuccess(Employee employee, String token) {
                    runOnUiThread(() -> {
                        apiClient.setAuthToken(token);
                        saveUserData(employee, token);

                        Intent intent = new Intent(RegInActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    });
                }

                @Override
                public void onFailure(String error) {
                    runOnUiThread(() -> Toast.makeText(RegInActivity.this, "Ошибка регистрации: " + error, Toast.LENGTH_LONG).show());
                }
            });
        });
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
                // Проверяем, является ли символ русской буквой или пробелом
                if (!Character.isLetter(c) || !(c >= 'А' && c <= 'я') || c == 'ё' || c == 'Ё') {
                    return "";
                }
            }
            return null;
        };
        editText.setFilters(new InputFilter[]{filter});
    }

    private void saveUserData(Employee employee, String token) {
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("jwt_token", token);

        // Save employee data
        editor.putInt("employee_id", employee.getEmployeeId());
        editor.putString("employee_fullName", employee.getFullName());
        editor.putString("employee_email", employee.getEmail());
        editor.putString("employee_description", employee.getDescription());

        editor.apply();
    }
}