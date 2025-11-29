package com.abzikel.yunus.utils;

import com.abzikel.yunus.pojos.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class UserManager {
    private static UserManager instance;
    private final FirebaseFirestore db;
    private FirebaseUser currentUser;
    private User currentUserData;
    private ListenerRegistration userListener;

    private UserManager() {
        // Initialize Firebase Firestore and FirebaseUser
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        startListeningToUser();
    }

    public static UserManager getInstance() {
        // Singleton pattern to ensure only one instance
        if (instance == null) {
            instance = new UserManager();
        }

        return instance;
    }

    private void startListeningToUser() {
        // Check if the user is logged in
        if (currentUser != null) {
            // Get the user's document reference and start listening
            String userId = currentUser.getUid();
            DocumentReference userDocRef = db.collection("users").document(userId);

            // Add a listener to the user's document
            userListener = userDocRef.addSnapshotListener((snapshot, e) -> {
                if (e != null) {
                    System.err.println("Listen failed: " + e);
                    return;
                }

                // Update the current user data
                if (snapshot != null && snapshot.exists()) {
                    currentUserData = snapshot.toObject(User.class);

                    // Notify the listener
                    if (userUpdateListener != null) {
                        userUpdateListener.onUserUpdated(currentUserData);
                    }
                }
            });
        }
    }

    public User getCurrentUserData() {
        return currentUserData;
    }

    public void stopListeningToUser() {
        // Stop listening to the user's document
        if (userListener != null) {
            userListener.remove();
        }
        resetUserData();
    }

    private void resetUserData() {
        // Reset the user data and FirebaseUser reference
        currentUser = null;
        currentUserData = null;
        userListener = null;
    }

    public static void clearInstance() {
        // Clear the singleton instance
        if (instance != null) {
            instance.stopListeningToUser();
            instance = null;
        }
    }

    public interface UserUpdateListener {
        // Callback method to be called when the user data is updated
        void onUserUpdated(User newUser);
    }

    private UserUpdateListener userUpdateListener;

    public void setUserUpdateListener(UserUpdateListener listener) {
        this.userUpdateListener = listener;
    }
}
