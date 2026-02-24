package com.labproject.app.ui.profile;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.labproject.app.utils.ToastHelper;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.labproject.app.R;
import com.labproject.app.data.db.DBHelper;
import com.labproject.app.data.session.SessionManager;
import com.labproject.app.databinding.FragmentProfileBinding;
import com.labproject.app.ui.auth.LoginActivity;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private DBHelper db;
    private SessionManager session;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        binding = FragmentProfileBinding.inflate(inflater, container, false);

        db = new DBHelper(requireContext());
        session = new SessionManager(requireContext());

        String email = session.getUserEmail();
        if (email == null) {
            forceLogout();
            return binding.getRoot();
        }

        loadUser(email);

        binding.btnSave.setOnClickListener(v -> {
            String first = binding.etFirstName.getText().toString().trim();
            String last  = binding.etLastName.getText().toString().trim();

            if (email.isEmpty() || first.isEmpty() || last.isEmpty()) {
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

            boolean ok = db.updateUserName(email, first, last);
            toast(ok ? "Profile updated" : "Update failed");

            if (ok && getActivity() instanceof com.labproject.app.MainActivity) {
                ((com.labproject.app.MainActivity) requireActivity()).refreshDrawerHeader();
            }
        });


        binding.btnChangePassword.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_nav_profile_to_changePasswordFragment)
        );

        binding.btnLogout.setOnClickListener(v -> forceLogout());

        return binding.getRoot();
    }

    private void loadUser(String email) {
        Cursor c = db.getUserByEmail(email);
        if (c != null && c.moveToFirst()) {
            binding.etFirstName.setText(c.getString(0));
            binding.etLastName.setText(c.getString(1));
            binding.tvEmail.setText(c.getString(2));
        }
        if (c != null) c.close();
    }

    private void forceLogout() {
        session.logout();
        Intent i = new Intent(requireContext(), LoginActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        requireActivity().finish();
    }

    private void toast(String msg) {
        if (msg.contains("failed") || msg.contains("Failed") || msg.contains("can't") || msg.contains("empty")) {
            ToastHelper.showError(requireContext(), msg);
        } else if (msg.contains("updated") || msg.contains("success")) {
            ToastHelper.showSuccess(requireContext(), msg);
        } else {
            ToastHelper.showInfo(requireContext(), msg);
        }
    }

    private void showError(String msg) {
        binding.tvError.setText(msg);
        binding.tvError.setVisibility(View.VISIBLE);
    }

    private void markError(View view) {
        view.setBackgroundTintList(
                ColorStateList.valueOf(
                        getResources().getColor(android.R.color.holo_red_dark)
                )
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}