package com.example.stuid.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.InputType;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.stuid.R;
import com.example.stuid.api.ApiClient;
import com.example.stuid.api.AuthCallback;
import com.example.stuid.api.AuthPasswordResetCallback;
import com.example.stuid.models.Employee;

import org.json.JSONObject;

import java.util.Random;

public class SignInActivity extends AppCompatActivity {
    private ApiClient apiClient;
    private TextView tvCreateAccount;
    private TextView tvForgotPassword;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);
        tvCreateAccount = findViewById(R.id.tvCreateAccount);
        tvCreateAccount.setOnClickListener(v -> {
            startActivity(new Intent(SignInActivity.this, RegInActivity.class));
        });
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());

        apiClient = new ApiClient();
        Button loginButton = findViewById(R.id.btnLogin);
        loginButton.setOnClickListener(v -> {
            String email = ((EditText)findViewById(R.id.etLogin)).getText().toString();
            String password = ((EditText)findViewById(R.id.etPassword)).getText().toString();

            apiClient.loginUser(email, password, new AuthCallback() {
                @Override
                public void onSuccess(Employee employee, String token) {
                    runOnUiThread(() -> {
                        // Сохраняем данные пользователя
                        apiClient.setAuthToken(token);
                        saveUserData(employee, token);

                        // Переходим на главный экран
                        Intent intent = new Intent(SignInActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    });
                }

                @Override
                public void onFailure(String error) {
                    runOnUiThread(() -> {
                        Toast.makeText(SignInActivity.this, "Login failed: " + error, Toast.LENGTH_LONG).show();
                    });
                }
            });
        });
    }

    private void saveUserData(Employee employee, String token) {
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("jwt_token", token);

        // Сохраняем данные employee (пример для простых полей)
        editor.putInt("employee_id", employee.getEmployeeId());
        editor.putString("employee_fullName", employee.getFullName());
        editor.putString("employee_email", employee.getEmail());
        editor.putString("employee_description", employee.getDescription());

        editor.apply();
    }

    private void showForgotPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Восстановление пароля");

        // Создаем EditText для ввода email
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        input.setHint("Введите ваш email");
        builder.setView(input);

        builder.setPositiveButton("Отправить", (dialog, which) -> {
            String email = input.getText().toString().trim();
            if (!email.isEmpty() && isValidEmail(email)) {
                sendPasswordResetEmail(email);
            } else {
                Toast.makeText(this, "Введите email", Toast.LENGTH_SHORT).show();
            }

            tvForgotPassword.setEnabled(false);
            new CountDownTimer(60000, 1000) {
                public void onTick(long millisUntilFinished) {
                    tvForgotPassword.setText("Повторить через " + millisUntilFinished / 1000);
                }
                public void onFinish() {
                    tvForgotPassword.setEnabled(true);
                    tvForgotPassword.setText("Забыли пароль?");
                }
            }.start();
        });

        builder.setNegativeButton("Отмена", null);
        builder.show();
    }

    private boolean isValidEmail(String email) {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private void sendPasswordResetEmail(String email) {
        // Показываем прогресс
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Отправка запроса...");
        progressDialog.show();

        Random random = new Random();
        int code = 100000 + random.nextInt(900000);

        apiClient.sendPasswordResetEmail(email, code, new AuthPasswordResetCallback() {
            @Override
            public void onSuccess(String message,JSONObject data) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(SignInActivity.this,
                            "Код подтверждения отправлен на " + email,
                            Toast.LENGTH_LONG).show();

                    showVerificationCodeDialog(email, code);
                });
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(SignInActivity.this,
                            "Ошибка: " + error,
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void showVerificationCodeDialog(String email, int code) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Подтверждение кода");
        builder.setMessage("Введите код, отправленный на " + email);

        // Создаем EditText для ввода кода
        final EditText codeInput = new EditText(this);
        codeInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        codeInput.setHint("6-значный код");
        builder.setView(codeInput);

        builder.setPositiveButton("Подтвердить", (dialog, which) -> {
            String enteredCode = codeInput.getText().toString().trim();
            if (enteredCode.equals(String.valueOf(code))) {
                // Код верный, переходим к смене пароля
                showNewPasswordDialog(email);
            } else {
                Toast.makeText(this, "Неверный код", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Отмена", null);
        builder.show();
    }

    private void showNewPasswordDialog(String email) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Новый пароль");

        // Создаем EditText для ввода нового пароля
        final EditText newPasswordInput = new EditText(this);
        newPasswordInput.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
        newPasswordInput.setHint("Введите новый пароль");

        // Создаем EditText для подтверждения пароля
        final EditText confirmPasswordInput = new EditText(this);
        confirmPasswordInput.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
        confirmPasswordInput.setHint("Подтвердите новый пароль");

        // Добавляем оба поля в диалог
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(newPasswordInput);
        layout.addView(confirmPasswordInput);
        builder.setView(layout);

        builder.setPositiveButton("Сохранить", (dialog, which) -> {
            String newPassword = newPasswordInput.getText().toString();
            String confirmPassword = confirmPasswordInput.getText().toString();

            if (newPassword.equals(confirmPassword)) {
                // Здесь можно вызвать метод API для смены пароля
                changePassword(email, newPassword);
            } else {
                Toast.makeText(this, "Пароли не совпадают", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Отмена", null);
        builder.show();
    }

    private void changePassword(String email, String newPassword) {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Смена пароля...");
        progressDialog.show();

        apiClient.resetPassword(email, newPassword, new AuthPasswordResetCallback() {
            @Override
            public void onSuccess(String message, JSONObject data) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(SignInActivity.this,
                            "Пароль успешно изменен",
                            Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(SignInActivity.this,
                            "Ошибка: " + error,
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }
}