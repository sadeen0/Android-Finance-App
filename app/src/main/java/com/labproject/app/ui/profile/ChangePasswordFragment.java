package com.labproject.app.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.labproject.app.utils.ToastHelper;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.labproject.app.data.db.DBHelper;
import com.labproject.app.data.session.SessionManager;
import com.labproject.app.databinding.FragmentChangePasswordBinding;

public class ChangePasswordFragment extends Fragment {

    private FragmentChangePasswordBinding binding;
    private DBHelper db;
    private SessionManager session;

    // 6-12, at least 1 lower, 1 upper, 1 digit (letters/digits only)
    private static final String PASS_REGEX = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[A-Za-z\\d]{6,12}$";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        binding = FragmentChangePasswordBinding.inflate(inflater, container, false);

        db = new DBHelper(requireContext());
        session = new SessionManager(requireContext());

        binding.btnUpdatePassword.setOnClickListener(v -> {
            String email = session.getUserEmail();
            if (email == null) {
                toast("Session expired. Login again.");
                return;
            }

            String current = binding.etCurrentPass.getText().toString();
            String newPass = binding.etNewPass.getText().toString();
            String newPass2 = binding.etNewPass2.getText().toString();

            if (current.isEmpty() || newPass.isEmpty() || newPass2.isEmpty()) {
                toast("Fill all fields");
                return;
            }

            if (!db.checkPassword(email, current)) {
                toast("Current password is wrong");
                return;
            }

            if (!newPass.matches(PASS_REGEX)) {
                toast("New password invalid (6-12, upper+lower+number)");
                return;
            }

            if (!newPass.equals(newPass2)) {
                toast("Passwords do not match");
                return;
            }

            boolean ok = db.updatePassword(email, newPass);
            if (!ok) {
                toast("Failed to update password");
                return;
            }

            toast("Password updated");
            Navigation.findNavController(v).popBackStack();
        });

        return binding.getRoot();
    }

    private void toast(String msg) {
        if (msg.contains("failed") || msg.contains("Failed") || msg.contains("wrong") || msg.contains("invalid") || msg.contains("not match") || msg.contains("Fill") || msg.contains("expired")) {
            ToastHelper.showError(requireContext(), msg);
        } else if (msg.contains("updated") || msg.contains("success")) {
            ToastHelper.showSuccess(requireContext(), msg);
        } else {
            ToastHelper.showInfo(requireContext(), msg);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
