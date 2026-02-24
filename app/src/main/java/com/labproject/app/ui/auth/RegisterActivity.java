package com.labproject.app.ui.auth;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.text.Editable;
import android.text.TextWatcher;

import androidx.appcompat.app.AppCompatActivity;

import com.labproject.app.data.db.DBHelper;
import com.labproject.app.databinding.ActivityRegisterBinding;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private DBHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = new DBHelper(this);

        binding.btnRegister.setOnClickListener(v -> register());
        binding.tvBackToLogin.setOnClickListener(v -> finish());

        //  Remove red highlight immediately when field becomes valid
        setupLiveValidation();
    }

    // Password: 6–12 chars, 1 uppercase, 1 lowercase, 1 digit
    private boolean isValidPassword(String password) {
        String regex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[A-Za-z\\d]{6,12}$";
        return password.matches(regex);
    }

    private void register() {
        String first = binding.etFirstName.getText().toString().trim();
        String last  = binding.etLastName.getText().toString().trim();
        String email = binding.etEmail.getText().toString().trim();
        String pass  = binding.etPassword.getText().toString();
        String pass2 = binding.etConfirmPassword.getText().toString();

        binding.tvError.setVisibility(View.GONE);

        if (email.isEmpty() || first.isEmpty() || last.isEmpty() || pass.isEmpty() || pass2.isEmpty()) {
            showError("Fill all Fields");
            return;
        }
        // First name
        if (first.isEmpty()) {
            markError(binding.etFirstName);
            showError("First Name is empty");
            return;
        }

        if (first.length() < 3 || first.length() > 10) {
            markError(binding.etFirstName);
            showError("First Name must be 3-10 characters");
            return;
        }

        // Last name
        if (last.isEmpty()) {
            markError(binding.etLastName);
            showError("Last Name is empty");
            return;
        }

        if (last.length() < 3 || last.length() > 10) {
            markError(binding.etLastName);
            showError("Last Name must be 3-10 characters");
            return;
        }

        // Email (PRIMARY KEY)
        if (email.isEmpty()) {
            markError(binding.etEmail);
            showError("Email is required");
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            markError(binding.etEmail);
            showError("Enter a valid email");
            return;
        }

        // Password
        if (!isValidPassword(pass)) {
            markError(binding.etPassword);
            showError("Password must be 6-12 chars and include uppercase, lowercase, and a number");
            return;
        }

        // Confirm password
        if (!pass.equals(pass2)) {
            markError(binding.etConfirmPassword);
            showError("Passwords Don't Match");
            return;
        }

        boolean created = db.registerUser(email, first, last, pass);
        if (!created) {
            markError(binding.etEmail);
            showError("Email already exists, Try another one");
            return;
        }

        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    private void showError(String msg) {
        binding.tvError.setText(msg);
        binding.tvError.setVisibility(View.VISIBLE);
    }

    // Highlight field
    private void markError(View view) {
        view.setBackgroundTintList(
                ColorStateList.valueOf(
                        getResources().getColor(android.R.color.holo_red_dark)
                )
        );
    }

    //  Clear highlight
    private void clearError(View view) {
        view.setBackgroundTintList(null);
    }

    // Live validation logic
    private void setupLiveValidation() {

        binding.etFirstName.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String v = s.toString().trim();
                if (v.length() >= 3 && v.length() <= 10) {
                    clearError(binding.etFirstName);
                }
            }
        });

        binding.etLastName.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String v = s.toString().trim();
                if (v.length() >= 3 && v.length() <= 10) {
                    clearError(binding.etLastName);
                }
            }
        });

        binding.etEmail.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (Patterns.EMAIL_ADDRESS.matcher(s.toString().trim()).matches()) {
                    clearError(binding.etEmail);
                }
            }
        });

        binding.etPassword.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isValidPassword(s.toString())) {
                    clearError(binding.etPassword);
                }
            }
        });

        binding.etConfirmPassword.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().equals(binding.etPassword.getText().toString())) {
                    clearError(binding.etConfirmPassword);
                }
            }
        });
    }

    // Cleaner TextWatcher
    abstract class SimpleTextWatcher implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void afterTextChanged(Editable s) {}
    }
}
