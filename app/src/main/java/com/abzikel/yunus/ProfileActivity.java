package com.abzikel.yunus;

import static com.abzikel.yunus.utils.Methods.getBitmapFromDrawable;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.abzikel.yunus.pojos.User;
import com.abzikel.yunus.utils.UserManager;
import com.github.leandroborgesferreira.loadingbutton.customViews.CircularProgressButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ProfileActivity extends AppCompatActivity {
    private FirebaseUser currentUser;
    private FirebaseFirestore db;
    private EditText etvFirstName, etvLastName;
    private CircularProgressButton btnSave;
    private Button btnDelete;
    private int purpleColor, greenColor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        setup();
    }

    private void setup() {
        // Initialize views
        etvFirstName = findViewById(R.id.etvFirstName);
        etvLastName = findViewById(R.id.etvLastName);
        btnDelete = findViewById(R.id.btnDelete);
        btnSave = findViewById(R.id.btnSave);

        // Get color from the resources
        purpleColor = ContextCompat.getColor(this, R.color.grape);
        greenColor = ContextCompat.getColor(this, R.color.fern_green);

        // FirebaseFirestore instance
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) loadUserData();

        // Set click listener for the delete button
        btnSave.setOnClickListener(v -> updateUserProfile());
        btnDelete.setOnClickListener(v -> deleteUserProfile());
    }

    private void loadUserData() {
        // Get user data from UserManager
        User user = UserManager.getInstance().getCurrentUserData();
        if (user != null) {
            // Update EditText fields with user data
            etvFirstName.setText(user.getFirstName());
            etvLastName.setText(user.getLastName());

            // Check if the user has a display name
            if (currentUser.getDisplayName() != null && !currentUser.getDisplayName().isEmpty()) {
                showDeleteButton();
            }
        }
    }

    private void showDeleteButton() {
        // Show the delete button and animate it
        btnDelete.setVisibility(View.VISIBLE);
        Animation slideIn = AnimationUtils.loadAnimation(this, R.anim.slide_in);
        btnDelete.startAnimation(slideIn);
    }

    private void updateUserProfile() {
        // Get user input
        String firstName = etvFirstName.getText().toString().trim();
        String lastName = etvLastName.getText().toString().trim();

        if (firstName.isEmpty() || lastName.isEmpty()) {
            Toast.makeText(this, R.string.unfilled, Toast.LENGTH_SHORT).show();
            return;
        }

        // Start the animation
        btnSave.startAnimation();


        // Check if the user already exists in the database
        if (currentUser != null) {
            // Check if the user has a display name
            if (currentUser.getDisplayName() != null && !currentUser.getDisplayName().isEmpty()) {
                // User already exists, update their profile
                updateExistingUser(currentUser, firstName, lastName);
            } else {
                // User doesn't exist, create a new user
                createNewUser(currentUser, firstName, lastName);
            }
        }
    }

    private void deleteUserProfile() {
        // Inflate custom content dialog and get context
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.content_dialog_confirmation, null);

        // Link XML to Java
        TextView tvMessage = dialogView.findViewById(R.id.tvMessage);
        Button btnPositive = dialogView.findViewById(R.id.btnPositive);
        Button btnNegative = dialogView.findViewById(R.id.btnNegative);

        // Initialize views
        tvMessage.setText(getResources().getString(R.string.ask_for_delete_account));
        btnPositive.setText(getResources().getString(R.string.cancel));
        btnNegative.setText(getResources().getString(R.string.delete));

        // Create Alert Dialog to ask confirmation
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomAlertDialog);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        // Add listeners
        btnNegative.setOnClickListener(v -> {
            DocumentReference userRef = db.collection("users").document(currentUser.getUid());
            userRef.delete().addOnSuccessListener(unused -> {
                // Clear the singleton instance and sign out
                UserManager.clearInstance();
                FirebaseAuth.getInstance().signOut();

                // Start the LoginActivity and clear the back stack
                Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);

                // Dismiss the dialog
                dialog.dismiss();
            }).addOnFailureListener(e -> {
                // Handle the error
                Toast.makeText(ProfileActivity.this, R.string.error + " " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        });
        btnPositive.setOnClickListener(v -> dialog.dismiss());

        // Show Alert Dialog
        dialog.show();
    }

    private void updateExistingUser(FirebaseUser currentUser, String firstName, String lastName) {
        // Update user data in the database
        Map<String, Object> updates = new HashMap<>();
        updates.put("firstName", firstName);
        updates.put("lastName", lastName);

        // Update the user's display name
        db.collection("users")
                .document(currentUser.getUid())
                .update(updates)
                .addOnSuccessListener(aVoid -> onProfileUpdated())
                .addOnFailureListener(this::onProfileUpdateFailed);
    }

    private void createNewUser(FirebaseUser currentUser, String firstName, String lastName) {
        // Create a new user document in the database
        User newUser = new User();
        newUser.setId(currentUser.getUid());
        newUser.setPhone(currentUser.getPhoneNumber());
        newUser.setFirstName(firstName);
        newUser.setLastName(lastName);
        newUser.setYunus(0.0);
        newUser.setCreation(new Date());
        newUser.setLastLogin(new Date());

        // Save the new user document
        db.collection("users")
                .document(currentUser.getUid())
                .set(newUser)
                .addOnSuccessListener(aVoid -> onProfileUpdated())
                .addOnFailureListener(this::onProfileUpdateFailed);
    }

    private void onProfileUpdated() {
        // Update the user's display name
        btnSave.setBackgroundTintList(ColorStateList.valueOf(greenColor));
        btnSave.doneLoadingAnimation(greenColor, Objects.requireNonNull(getBitmapFromDrawable(this, R.drawable.ic_check)));
        Toast.makeText(this, R.string.profile_updated, Toast.LENGTH_SHORT).show();
        startActivity(new Intent(ProfileActivity.this, MainActivity.class));
        finish();
    }

    private void onProfileUpdateFailed(Exception e) {
        // Handle the error
        btnSave.setBackgroundTintList(ColorStateList.valueOf(purpleColor));
        btnSave.revertAnimation();
        btnSave.setBackgroundResource(R.drawable.custom_edittext);
        Toast.makeText(this, R.string.error + " " + e.getMessage(), Toast.LENGTH_SHORT).show();
    }
}
