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
}