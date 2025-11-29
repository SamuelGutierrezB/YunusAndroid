package com.abzikel.yunus.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.abzikel.yunus.LoginActivity;
import com.abzikel.yunus.ProfileActivity;
import com.abzikel.yunus.R;
import com.abzikel.yunus.pojos.User;
import com.abzikel.yunus.utils.UserManager;
import com.google.firebase.auth.FirebaseAuth;

import java.text.DateFormat;
import java.util.Locale;

public class ProfileFragment extends Fragment {

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views
        TextView tvName = view.findViewById(R.id.tvName);
        TextView tvCreation = view.findViewById(R.id.tvCreation);
        TextView tvEditProfile = view.findViewById(R.id.tvEditProfile);
        TextView tvLogout = view.findViewById(R.id.tvLogout);

        // Get user data from the UserManager
        User currentUser = UserManager.getInstance().getCurrentUserData();

        if (currentUser != null) {
            // Set the full name (firstName + lastName)
            String fullName = currentUser.getFirstName() + " " + currentUser.getLastName();
            tvName.setText(fullName);

            // Format and set the creation date
            DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault());
            String creationDate = getString(R.string.member_since) + " " + dateFormat.format(currentUser.getCreation());
            tvCreation.setText(creationDate);
        }

        // Set click listener for the edit profile button
        tvEditProfile.setOnClickListener(v -> redirectToActivity(ProfileActivity.class));

        // Set click listener for the logout button
        tvLogout.setOnClickListener(v -> logout());
    }

    private void logout() {
        // Inflate custom content dialog and get context
        Context context = requireContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.content_dialog_confirmation, null);

        // Link XML to Java
        TextView tvMessage = dialogView.findViewById(R.id.tvMessage);
        Button btnPositive = dialogView.findViewById(R.id.btnPositive);
        Button btnNegative = dialogView.findViewById(R.id.btnNegative);

        // Initialize views
        tvMessage.setText(getResources().getString(R.string.ask_for_sign_out));
        btnPositive.setText(getResources().getString(R.string.cancel));
        btnNegative.setText(getResources().getString(R.string.logout));

        // Create Alert Dialog to ask confirmation
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.CustomAlertDialog);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        // Add listeners
        btnNegative.setOnClickListener(v -> {
            // Clear the singleton instance
            UserManager.clearInstance();

            // SignOut and go to SignIn Activity
            FirebaseAuth.getInstance().signOut();
            Toast.makeText(context, getResources().getString(R.string.session_closed), Toast.LENGTH_SHORT).show();
            redirectToActivity(LoginActivity.class);
            requireActivity().finish();

            dialog.dismiss();
        });
        btnPositive.setOnClickListener(v -> dialog.dismiss());

        // Show Alert Dialog
        dialog.show();
    }

    private void redirectToActivity(Class<?> activityClass) {
        // Redirect to the desired activity
        Intent intent = new Intent(requireContext(), activityClass);
        startActivity(intent);
    }

}
