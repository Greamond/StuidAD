package com.example.stuid.activity;

import static androidx.core.content.ContentProviderCompat.requireContext;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Patterns;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.stuid.R;
import com.example.stuid.api.ApiClient;
import com.example.stuid.api.AuthCallback;
import com.example.stuid.api.AuthPasswordResetCallback;
import com.example.stuid.classes.CheckInternet;
import com.example.stuid.models.Employee;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.messaging.FirebaseMessaging;

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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.white));
        }

        tvCreateAccount = findViewById(R.id.tvCreateAccount);
        tvCreateAccount.setOnClickListener(v -> {
            startActivity(new Intent(SignInActivity.this, RegInActivity.class));
        });

        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());

        apiClient = new ApiClient();
        Button loginButton = findViewById(R.id.btnLogin);

        TextInputLayout emailInputLayout = findViewById(R.id.emailInputLayout);
        TextInputLayout passwordInputLayout = findViewById(R.id.passwordInputLayout);

        EditText etLogin = findViewById(R.id.etLogin);
        EditText etPassword = findViewById(R.id.etPassword);

        addClearErrorTextWatcher(etLogin, emailInputLayout);
        addClearErrorTextWatcher(etPassword, passwordInputLayout);


        loginButton.setOnClickListener(v -> {
            String email = etLogin.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            // Валидация email
            if (email.isEmpty()) {
                if (emailInputLayout != null) {
                    emailInputLayout.setError("Введите email");
                } else {
                    etLogin.setError("Введите email");
                }
                return;
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                if (emailInputLayout != null) {
                    emailInputLayout.setError("Введите корректный email");
                } else {
                    etLogin.setError("Введите корректный email");
                }
                return;
            }

            // Валидация пароля
            if (password.isEmpty()) {
                passwordInputLayout.setError("Введите пароль");
                return;
            }

            // Сбрасываем ошибки если они были
            if (emailInputLayout != null) {
                emailInputLayout.setError(null);
            }
            etPassword.setError(null);

            FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    String FCMToken = task.getResult();

                    if (!CheckInternet.isNetworkConnected(SignInActivity.this)) {
                        Toast.makeText(SignInActivity.this, "Нет подключения к интернету", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (!CheckInternet.isNetworkConnected(SignInActivity.this)) {
                        Toast.makeText(SignInActivity.this, "Нет подключения к интернету", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    apiClient.loginUser(email, password, FCMToken, new AuthCallback() {
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
                                Toast.makeText(SignInActivity.this, "Ошибка авторизации: " + error, Toast.LENGTH_LONG).show();
                            });
                        }
                    });
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

        // Создаем контейнер
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        container.setLayoutParams(containerParams);

        // Настраиваем отступы
        int padding = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                16,
                getResources().getDisplayMetrics()
        );
        container.setPadding(padding, padding, padding, 0);

        // Создаем TextInputLayout
        final TextInputLayout textInputLayout = new TextInputLayout(this);
        LinearLayout.LayoutParams inputLayoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        textInputLayout.setLayoutParams(inputLayoutParams);
        textInputLayout.setHint("Введите ваш email");
        textInputLayout.setErrorEnabled(true);
        textInputLayout.setBoxStrokeColor(ContextCompat.getColor(this, R.color.blue));
        textInputLayout.setHintTextColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.blue)));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            textInputLayout.setCursorColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.blue)));
        }

        // Создаем EditText
        final TextInputEditText editText = new TextInputEditText(textInputLayout.getContext());
        editText.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        LinearLayout.LayoutParams editTextParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        editText.setLayoutParams(editTextParams);
        textInputLayout.addView(editText);

        container.addView(textInputLayout);
        builder.setView(container);

        // Устанавливаем кнопки
        builder.setPositiveButton("Отправить", null); // Обработчик установим позже
        builder.setNegativeButton("Отмена", (dialogEmail, which) -> dialogEmail.dismiss());

        // Создаем и показываем диалог
        final AlertDialog dialog = builder.create();

        // Переопределяем обработчик для кнопки "Отправить"
        dialog.setOnShowListener(dialogInterface -> {
            Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            negativeButton.setTextColor(getResources().getColor(R.color.red));

            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setTextColor(getResources().getColor(R.color.blue));
            positiveButton.setOnClickListener(view -> {
                String email = editText.getText().toString().trim();

                if (email.isEmpty()) {
                    textInputLayout.setError("Поле не может быть пустым");
                    return;
                }

                if (!isValidEmail(email)) {
                    textInputLayout.setError("Введите корректный email");
                    return;
                }

                // Если все проверки пройдены
                textInputLayout.setError(null);
                sendPasswordResetEmail(email);

                dialog.dismiss();
            });
        });

        // Сбрасываем ошибку при изменении текста
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

        dialog.show();
    }

    private boolean isValidEmail(String email) {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private void sendPasswordResetEmail(String email) {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Отправка запроса...");
        progressDialog.show();

        Random random = new Random();
        int code = 100000 + random.nextInt(900000);

        if (!CheckInternet.isNetworkConnected(SignInActivity.this)) {
            Toast.makeText(SignInActivity.this, "Нет подключения к интернету", Toast.LENGTH_SHORT).show();
            return;
        }

        apiClient.sendPasswordResetEmail(email, code, new AuthPasswordResetCallback() {
            @Override
            public void onSuccess(String message, JSONObject data) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(SignInActivity.this,
                            "Код подтверждения отправлен на " + email,
                            Toast.LENGTH_LONG).show();

                    tvForgotPassword.setEnabled(false); // Блокируем на 2 минуты
                    tvForgotPassword.setText("Повторить через 02:00");

                    startResendCountdown(); // Запускаем обратный отсчёт
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

    private void startResendCountdown() {
        new CountDownTimer(120000, 1000) {
            public void onTick(long millisUntilFinished) {
                int seconds = (int) (millisUntilFinished / 1000);
                int minutes = seconds / 60;
                int remainingSeconds = seconds % 60;
                tvForgotPassword.setText(String.format("Повторить через %02d:%02d", minutes, remainingSeconds));
            }

            public void onFinish() {
                tvForgotPassword.setEnabled(true);
                tvForgotPassword.setText("Забыли пароль?");
            }
        }.start();
    }

    private void showVerificationCodeDialog(String email, int code) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Подтверждение кода");
        builder.setMessage("Введите код, отправленный на " + email);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        int padding = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                16,
                getResources().getDisplayMetrics()
        );
        container.setPadding(padding, 0, padding, 0);

        TextInputLayout codeInputLayout = new TextInputLayout(this);
        codeInputLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        codeInputLayout.setHint("6-значный код");
        codeInputLayout.setErrorEnabled(true);
        codeInputLayout.setBoxStrokeColor(ContextCompat.getColor(this, R.color.blue));
        codeInputLayout.setHintTextColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.blue)));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            codeInputLayout.setCursorColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.blue)));
        }

        TextInputEditText codeInput = new TextInputEditText(codeInputLayout.getContext());
        codeInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        codeInput.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        codeInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(6)});
        codeInputLayout.addView(codeInput);
        container.addView(codeInputLayout);

        // TextView для отображения времени
        TextView tvTimer = new TextView(this);
        tvTimer.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        tvTimer.setGravity(View.TEXT_ALIGNMENT_CENTER);
        tvTimer.setTextSize(14);
        tvTimer.setTextColor(ContextCompat.getColor(this, R.color.gray));
        tvTimer.setText("Осталось времени: 02:00");
        container.addView(tvTimer);

        builder.setView(container);

        builder.setPositiveButton("Подтвердить", null);
        builder.setNegativeButton("Отмена", (dialogEmail, which) -> dialogEmail.dismiss());

        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(dialogInterface -> {
            Button negtiveButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            negtiveButton.setTextColor(getResources().getColor(R.color.red));

            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setTextColor(getResources().getColor(R.color.blue));
            positiveButton.setOnClickListener(view -> {
                String enteredCode = codeInput.getText().toString().trim();
                if (enteredCode.isEmpty()) {
                    codeInputLayout.setError("Введите код подтверждения");
                    return;
                }
                if (!enteredCode.equals(String.valueOf(code))) {
                    codeInputLayout.setError("Неверный код");
                    return;
                }

                codeInputLayout.setError(null);
                showNewPasswordDialog(email);
                dialog.dismiss();
            });
        });

        // Запуск таймера
        CountDownTimer timer = new CountDownTimer(120000, 1000) { // 2 минуты
            public void onTick(long millisUntilFinished) {
                int seconds = (int) (millisUntilFinished / 1000);
                int minutes = seconds / 60;
                int remainingSeconds = seconds % 60;

                String timeLeft = String.format("Осталось времени: %02d:%02d", minutes, remainingSeconds);
                tvTimer.setText(timeLeft);
            }

            public void onFinish() {
                tvTimer.setText("Время истекло. Код недействителен.");
                Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                positiveButton.setEnabled(false);
                positiveButton.setText("Код просрочен");

                // Разрешаем повторную отправку кода
                tvForgotPassword.setEnabled(true);
                tvForgotPassword.setText("Повторить отправку");
            }
        };

        // Привязываем таймер к диалогу
        dialog.setOnCancelListener(dialogInterface -> timer.cancel());

        dialog.show();

        // Включаем таймер
        timer.start();
    }

    private void showNewPasswordDialog(String email) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Новый пароль");

        // Создаем контейнер
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        // Настраиваем отступы
        int padding = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                16,
                getResources().getDisplayMetrics()
        );
        container.setPadding(padding, padding, padding, 0);

        // Поле для нового пароля
        final TextInputLayout newPasswordLayout = new TextInputLayout(this);
        newPasswordLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        newPasswordLayout.setHint("Новый пароль");
        newPasswordLayout.setErrorEnabled(true);
        newPasswordLayout.setBoxStrokeColor(ContextCompat.getColor(this, R.color.blue));
        newPasswordLayout.setHintTextColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.blue)));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            newPasswordLayout.setCursorColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.blue)));
        }

        final TextInputEditText newPasswordInput = new TextInputEditText(newPasswordLayout.getContext());
        newPasswordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        newPasswordInput.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        newPasswordLayout.addView(newPasswordInput);

        // Поле для подтверждения пароля
        final TextInputLayout confirmPasswordLayout = new TextInputLayout(this);
        confirmPasswordLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        confirmPasswordLayout.setHint("Подтвердите пароль");
        confirmPasswordLayout.setErrorEnabled(true);
        confirmPasswordLayout.setBoxStrokeColor(ContextCompat.getColor(this, R.color.blue));
        confirmPasswordLayout.setHintTextColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.blue)));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            confirmPasswordLayout.setCursorColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.blue)));
        }

        final TextInputEditText confirmPasswordInput = new TextInputEditText(confirmPasswordLayout.getContext());
        confirmPasswordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        confirmPasswordInput.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        confirmPasswordLayout.addView(confirmPasswordInput);

        container.addView(newPasswordLayout);
        container.addView(confirmPasswordLayout);
        builder.setView(container);

        // Устанавливаем кнопки (сначала с пустыми обработчиками)
        builder.setPositiveButton("Сохранить", null);
        builder.setNegativeButton("Отмена", null);

        // Создаем диалог
        final AlertDialog dialog = builder.create();

        // Переопределяем обработчик для кнопки "Сохранить"
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
                negativeButton.setTextColor(getResources().getColor(R.color.red));

                Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                positiveButton.setTextColor(getResources().getColor(R.color.blue));
                positiveButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String newPassword = newPasswordInput.getText().toString().trim();
                        String confirmPassword = confirmPasswordInput.getText().toString().trim();

                        // Сбрасываем предыдущие ошибки
                        newPasswordLayout.setError(null);
                        confirmPasswordLayout.setError(null);

                        // Валидация
                        if (newPassword.isEmpty()) {
                            newPasswordLayout.setError("Введите пароль");
                            return;
                        }

                        if (newPassword.length() < 6) {
                            newPasswordLayout.setError("Минимум 6 символов");
                            return;
                        }

                        if (confirmPassword.isEmpty()) {
                            confirmPasswordLayout.setError("Подтвердите пароль");
                            return;
                        }

                        if (!newPassword.equals(confirmPassword)) {
                            confirmPasswordLayout.setError("Пароли не совпадают");
                            return;
                        }

                        // Все проверки пройдены
                        changePassword(email, newPassword);
                        dialog.dismiss();
                    }
                });
            }
        });

        // Сбрасываем ошибки при изменении текста
        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                newPasswordLayout.setError(null);
                confirmPasswordLayout.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        newPasswordInput.addTextChangedListener(textWatcher);
        confirmPasswordInput.addTextChangedListener(textWatcher);

        dialog.show();
    }

    private void changePassword(String email, String newPassword) {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Смена пароля...");
        progressDialog.show();

        if (!CheckInternet.isNetworkConnected(SignInActivity.this)) {
            Toast.makeText(SignInActivity.this, "Нет подключения к интернету", Toast.LENGTH_SHORT).show();
            return;
        }

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