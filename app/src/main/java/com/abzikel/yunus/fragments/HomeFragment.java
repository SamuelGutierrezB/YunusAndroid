package com.abzikel.yunus.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.abzikel.yunus.R;
import com.abzikel.yunus.pojos.User;
import com.abzikel.yunus.utils.UserManager;

import java.util.Locale;

public class HomeFragment extends Fragment {
    private TextView tvBalance;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Initialize the TextView
        tvBalance = view.findViewById(R.id.tvBalance);

        // Set the current user data immediately when the view is created
        User currentUser = UserManager.getInstance().getCurrentUserData();
        if (currentUser != null) {
            tvBalance.setText(String.format(Locale.getDefault(), "%.2f", currentUser.getYunus()));
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Register the user update listener in onResume
        UserManager.getInstance().setUserUpdateListener(newUser -> {
            // Update the TextView with the new balance in real-time
            tvBalance.setText(String.format(Locale.getDefault(), "%.2f", newUser.getYunus()));
        });
    }

    @Override
    public void onPause() {
        super.onPause();

        // Remove the user listener in onPause to avoid memory leaks
        UserManager.getInstance().setUserUpdateListener(null);
    }
}
