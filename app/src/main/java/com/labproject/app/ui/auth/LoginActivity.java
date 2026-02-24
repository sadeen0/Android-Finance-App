package com.labproject.app.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.labproject.app.MainActivity;
import com.labproject.app.data.db.DBHelper;
import com.labproject.app.data.db.TestDataGenerator;
import com.labproject.app.data.session.SessionManager;
import com.labproject.app.databinding.ActivityLoginBinding;
import com.labproject.app.utils.ToastHelper;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private DBHelper db;
    private SessionManager session;
    private int tapCount = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = new DBHelper(this);
        session = new SessionManager(this);

        // If already logged in -> go main
        if (session.isLoggedIn()) {
            goMain();
            return;
        }

        // Hidden test data generator (tap title 5 times)
        binding.tvTitle.setOnClickListener(v -> {
            tapCount++;
            if (tapCount >= 5) {
                tapCount = 0;
                generateTestData();
            }
        });

        // Remember me: rem. email only
        if (session.isRemembered()) {
            binding.etEmail.setText(session.getRememberedEmail());
            binding.cbRemember.setChecked(true);
        }

        binding.btnLogin.setOnClickListener(v -> login());
        binding.tvCreateAccount.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class))
        );
    }

    private void login() {
        String email = binding.etEmail.getText().toString().trim();
        String pass  = binding.etPassword.getText().toString();

        binding.tvError.setVisibility(View.GONE);

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("Enter a valid email");
            return;
        }

        if (pass.length() < 6 || pass.length() > 12) {
            showError("Password must be 6-12 characters");
            return;
        }

        if (!db.loginValid(email, pass)) {
            showError("Wrong email or password");
            return;
        }

        // session
        session.loginSession(email);

        // remember email only
        if (binding.cbRemember.isChecked()) {
            session.setRememberedEmail(email);
        } else {
            session.clearRememberedEmail();
        }

        goMain();
    }

    private void showError(String msg) {
        binding.tvError.setText(msg);
        binding.tvError.setVisibility(View.VISIBLE);
    }
    private void generateTestData() {
        ToastHelper.showInfo(this, "Generating test data...");
        TestDataGenerator.generateTestData(this);
        ToastHelper.showSuccess(this, "Test data created!\nEmail: test@finance.com\nPassword: Test123");
        // Pre fill login with test credentials
        binding.etEmail.setText("test@finance.com");
        binding.etPassword.setText("Test123");
    }
    private void goMain() {
        Intent i = new Intent(this, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }
}
