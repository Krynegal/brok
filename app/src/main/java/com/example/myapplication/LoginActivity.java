package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {
    private EditText editTextEmail, editTextPassword;
    private Button buttonLogin, buttonRegister, buttonSwitchMode;
    private TextView textViewMode;
    private boolean isLoginMode = true;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences("auth", Context.MODE_PRIVATE);
        String token = prefs.getString("token", null);
        if (token != null) {
            // Уже залогинен — сразу в MainActivity
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonLogin = findViewById(R.id.buttonLogin);
        buttonRegister = findViewById(R.id.buttonRegister);
        buttonSwitchMode = findViewById(R.id.buttonSwitchMode);
        textViewMode = findViewById(R.id.textViewMode);

        updateMode();

        buttonLogin.setOnClickListener(v -> {
            if (isLoginMode) handleLogin();
        });
        buttonRegister.setOnClickListener(v -> {
            if (!isLoginMode) handleRegister();
        });
        buttonSwitchMode.setOnClickListener(v -> {
            isLoginMode = !isLoginMode;
            updateMode();
        });
    }

    private void updateMode() {
        if (isLoginMode) {
            textViewMode.setText("Вход");
            buttonLogin.setVisibility(View.VISIBLE);
            buttonRegister.setVisibility(View.GONE);
            buttonSwitchMode.setText("Нет аккаунта? Зарегистрироваться");
        } else {
            textViewMode.setText("Регистрация");
            buttonLogin.setVisibility(View.GONE);
            buttonRegister.setVisibility(View.VISIBLE);
            buttonSwitchMode.setText("Уже есть аккаунт? Войти");
        }
    }

    private void handleLogin() {
        String email = editTextEmail.getText().toString();
        String password = editTextPassword.getText().toString();
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Введите email и пароль", Toast.LENGTH_SHORT).show();
            return;
        }
        LoginRequest req = new LoginRequest();
        req.email = email;
        req.password = password;
        AssetApiService api = ApiClient.getClient().create(AssetApiService.class);
        api.login(req).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    saveToken(response.body().token);
                    goToMain();
                } else {
                    Toast.makeText(LoginActivity.this, "Ошибка входа", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                Log.d("error", t.toString());
                Toast.makeText(LoginActivity.this, "Ошибка сети", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleRegister() {
        String email = editTextEmail.getText().toString();
        String password = editTextPassword.getText().toString();
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Введите email и пароль", Toast.LENGTH_SHORT).show();
            return;
        }
        RegisterRequest req = new RegisterRequest();
        req.email = email;
        req.password = password;
        AssetApiService api = ApiClient.getClient().create(AssetApiService.class);
        api.register(req).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                Log.d("registration error", response.toString());
                if (response.isSuccessful() && response.body() != null) {
                    saveToken(response.body().token);
                    goToMain();
                } else {
                    Toast.makeText(LoginActivity.this, "Ошибка регистрации", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                Log.d("error", t.toString());
                Toast.makeText(LoginActivity.this, "Ошибка сети", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveToken(String token) {
        SharedPreferences prefs = getSharedPreferences("auth", Context.MODE_PRIVATE);
        prefs.edit().putString("token", token).apply();
    }

    private void goToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
} 