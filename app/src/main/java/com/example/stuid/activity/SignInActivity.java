package com.example.stuid.activity;

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

public class SignInActivity extends AppCompatActivity {
    private ApiClient apiClient;
    TextView tvCreateAccount;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);
        tvCreateAccount = findViewById(R.id.tvCreateAccount);
        tvCreateAccount.setOnClickListener(v -> {
            startActivity(new Intent(SignInActivity.this, RegInActivity.class));
        });

        apiClient = new ApiClient();
        Button loginButton = findViewById(R.id.btnLogin);
        loginButton.setOnClickListener(v -> {
            String email = ((EditText)findViewById(R.id.etLogin)).getText().toString();
            String password = ((EditText)findViewById(R.id.etPassword)).getText().toString();

            apiClient.loginUser(email, password, new AuthCallback() {
                @Override
                public void onSuccess(Employee employee) {
                    runOnUiThread(() -> {
                        // Сохраняем данные пользователя
                        saveUserData(email, password, employee);

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

    private void saveUserData(String email, String password, Employee employee) {
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("user_email", email);
        editor.putString("user_password", password);

        // Сохраняем данные employee (пример для простых полей)
        editor.putString("employee_fullName", employee.getFullName());
        editor.putString("employee_email", employee.getEmail());
        editor.putString("employee_description", employee.getDescription());

        editor.apply();
    }
}