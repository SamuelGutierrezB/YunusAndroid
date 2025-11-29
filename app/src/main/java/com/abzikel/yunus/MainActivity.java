package com.abzikel.yunus;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.abzikel.yunus.fragments.HomeFragment;
import com.abzikel.yunus.fragments.ProfileFragment;
import com.abzikel.yunus.fragments.TransactionsFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    private final HomeFragment homeFragment = new HomeFragment();
    private final TransactionsFragment transactionsFragment = new TransactionsFragment();
    private final ProfileFragment profileFragment = new ProfileFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Update lastLogin on user document
        updateLastLogin();

        // Initialize BottomNavigationView
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Set default fragment (HomeFragment)
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.nav_host_fragment, new HomeFragment())
                    .commit();
        }

        // Handle navigation item clicks
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            int itemId = item.getItemId();
            if (itemId == R.id.navigation_home) selectedFragment = homeFragment;
            else if (itemId == R.id.navigation_transactions)
                selectedFragment = transactionsFragment;
            else if (itemId == R.id.navigation_profile) selectedFragment = profileFragment;

            // Replace the fragment
            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.nav_host_fragment, selectedFragment)
                        .commit();
            }

            return true;
        });
    }

    private void updateLastLogin() {
        // Get current user
        String userId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();

        // Get a reference to the user's document
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference userDocRef = db.collection("users").document(userId);

        // Update the lastLogin field with server timestamp
        userDocRef.update("lastLogin", FieldValue.serverTimestamp())
                .addOnSuccessListener(aVoid -> {
                    // Success, lastLogin updated
                    System.out.println("User lastLogin updated successfully.");
                })
                .addOnFailureListener(e -> {
                    // Failure, handle the error
                    System.err.println("Error updating lastLogin: " + e.getMessage());
                });
    }

}
