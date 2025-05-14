package com.example.stuid.activity;

import android.content.Intent;
import android.os.Bundle;
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

public class RegInActivity extends AppCompatActivity {
    private ApiClient apiClient;
    private TextView tvLoginLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reg_in);

        apiClient = new ApiClient();
        Button registerButton = findViewById(R.id.btnRegister);

        tvLoginLink = findViewById(R.id.tvLoginLink);
        tvLoginLink.setOnClickListener(v -> {
            startActivity(new Intent(RegInActivity.this, SignInActivity.class));
        });

        registerButton.setOnClickListener(v -> {
            String firstName = ((EditText)findViewById(R.id.etFirstName)).getText().toString();
            String lastName = ((EditText)findViewById(R.id.etLastName)).getText().toString();
            String middleName = ((EditText)findViewById(R.id.etMiddleName)).getText().toString();
            String email = ((EditText)findViewById(R.id.etEmail)).getText().toString();
            String password = ((EditText)findViewById(R.id.etPassword)).getText().toString();

            apiClient.registerUser(firstName, lastName, middleName, email, password, new AuthCallback() {
                @Override
                public void onSuccess(Employee employee, String token) {
                    runOnUiThread(() -> {
                        // Save user data
                        apiClient.setAuthToken(token);
                        saveUserData(email, password, employee, token);

                        // Go to main activity
                        Intent intent = new Intent(RegInActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    });
                }

                @Override
                public void onFailure(String error) {
                    runOnUiThread(() -> {
                        Toast.makeText(RegInActivity.this, "Registration failed: " + error, Toast.LENGTH_LONG).show();
                    });
                }
            });
        });
    }

    private void saveUserData(String email, String password, Employee employee, String token) {
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("jwt_token", token);
        editor.putString("user_email", email);
        editor.putString("user_password", password);

        // Save employee data
        editor.putString("employee_fullName", employee.getFullName());
        editor.putString("employee_email", employee.getEmail());
        editor.putString("employee_description", employee.getDescription());

        editor.apply();
    }
}