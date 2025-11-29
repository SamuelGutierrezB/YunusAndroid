package com.abzikel.yunus;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.Toast;

import com.github.leandroborgesferreira.loadingbutton.customViews.CircularProgressButton;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.hbb20.CountryCodePicker;

import java.util.Objects;

public class LoginActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private CountryCodePicker ccp;
    private EditText etvOTP;
    private CircularProgressButton btnLogin, btnVerify;
    private String verificationId, sentPhoneNumber;
    private boolean isPhoneNumberCorrect = false;
    private int purpleColor, greenColor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        setup();
        checkUserSession();
    }

    private void setup() {
        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Get colors from the resources
        purpleColor = ContextCompat.getColor(this, R.color.grape);
        greenColor = ContextCompat.getColor(this, R.color.fern_green);

        // Initialize views
        ccp = findViewById(R.id.ccp);
        etvOTP = findViewById(R.id.etvOTP);
        EditText etvPhone = findViewById(R.id.etvPhone);
        btnLogin = findViewById(R.id.btnLogin);
        btnVerify = findViewById(R.id.btnVerify);

        // Register carrier
        ccp.registerCarrierNumberEditText(etvPhone);

        // Set click listener for the login button
        btnLogin.setOnClickListener(v -> {
            btnLogin.startAnimation();
            sendOTP();
        });

        // Set click listener for the verify button
        btnVerify.setOnClickListener(v -> {
            btnVerify.startAnimation();
            verifyOTP();
        });

        // Set text watcher for the phone number field
        etvPhone.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // Get the current phone number from the input field
                String currentPhoneNumber = ccp.getFullNumberWithPlus();

                // Check if the current phone number matches the one to which OTP was sent
                boolean isCorrect = currentPhoneNumber.equals(sentPhoneNumber);

                // Only trigger animation if the phone number correctness state has changed
                if (isCorrect != isPhoneNumberCorrect) {
                    if (isCorrect) {
                        // Phone number is correct, show OTP field and hide the login button
                        showOTPField();
                        hideButtonWithAnimation(btnLogin);
                    } else {
                        // Phone number is incorrect, hide OTP and verify button, show login button
                        hideButtonWithAnimation(etvOTP);
                        hideButtonWithAnimation(btnVerify);

                        // Restore the login button with the correct style and color
                        revertAnimation(btnLogin);
                        btnLogin.setVisibility(View.VISIBLE);
                    }

                    // Update the state to reflect the new phone number correctness status
                    isPhoneNumberCorrect = isCorrect;
                }
                // No action needed if the state has not changed (to avoid redundant animations)
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

    }

    private void checkUserSession() {
        // Check if the user is already authenticated
        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {
            // Verify if displayName is set initially
            String displayName = user.getDisplayName();
            if (displayName != null && !displayName.isEmpty()) {
                // User has a displayName, redirect to MainActivity
                redirectToActivity(MainActivity.class);
            } else {
                // Reload user data to ensure up-to-date information
                user.reload().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Check displayName again after reload
                        String updatedDisplayName = user.getDisplayName();
                        if (updatedDisplayName != null && !updatedDisplayName.isEmpty()) {
                            // User now has a displayName, redirect to MainActivity
                            redirectToActivity(MainActivity.class);
                        } else {
                            // No displayName found after reload, redirect to ProfileActivity
                            redirectToActivity(ProfileActivity.class);
                        }
                    } else {
                        // Handle reload failure
                        Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }

    private void redirectToActivity(Class<?> activityClass) {
        Intent intent = new Intent(this, activityClass);
        startActivity(intent);
        finish();
    }

    private void sendOTP() {
        if (ccp.isValidFullNumber()) {
            // Save the original phone number temporarily
            sentPhoneNumber = ccp.getFullNumberWithPlus();

            // Send OTP
            PhoneAuthOptions options =
                    PhoneAuthOptions.newBuilder(mAuth)
                            .setPhoneNumber(sentPhoneNumber)
                            .setTimeout(60L, java.util.concurrent.TimeUnit.SECONDS)
                            .setActivity(this)
                            .setCallbacks(mCallbacks)
                            .build();
            PhoneAuthProvider.verifyPhoneNumber(options);
        } else {
            // Invalid phone number
            Toast.makeText(this, R.string.invalid_phone, Toast.LENGTH_SHORT).show();
            btnLogin.revertAnimation();
        }
    }

    private void verifyOTP() {
        // Get the OTP code from the EditText
        String otpCode = etvOTP.getText().toString().trim();

        if (!otpCode.isEmpty() && verificationId != null) {
            // Create a PhoneAuthCredential with the verification ID and the OTP code
            PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, otpCode);
            signInWithPhoneAuthCredential(credential);
        } else {
            // Invalid OTP code
            Toast.makeText(this, R.string.invalid_otp, Toast.LENGTH_SHORT).show();
            revertAnimation(btnVerify);
        }
    }

    private void revertAnimation(CircularProgressButton button) {
        // Revert the animation to its original state
        button.setBackgroundTintList(ColorStateList.valueOf(purpleColor));
        button.revertAnimation();
        button.setBackgroundResource(R.drawable.custom_edittext);
    }

    private void finishAnimation(CircularProgressButton button) {
        // Finish the animation
        button.setBackgroundTintList(ColorStateList.valueOf(greenColor));
        button.doneLoadingAnimation(greenColor, Objects.requireNonNull(getBitmapFromDrawable(R.drawable.ic_check)));
    }

    private void showOTPField() {
        // Show the OTP field and the verify button
        etvOTP.setVisibility(View.VISIBLE);
        btnVerify.setVisibility(View.VISIBLE);
        Animation slideIn = AnimationUtils.loadAnimation(this, R.anim.slide_in);
        etvOTP.startAnimation(slideIn);
        btnVerify.startAnimation(slideIn);
    }

    private void hideButtonWithAnimation(final View view) {
        // Hide the button with a slide-out animation
        Animation slideOut = AnimationUtils.loadAnimation(this, R.anim.slide_out);

        // Start the animation
        view.startAnimation(slideOut);

        // Set the animation listener
        slideOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                view.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Authentication successful
                        finishAnimation(btnLogin);
                        Toast.makeText(LoginActivity.this, R.string.welcome, Toast.LENGTH_SHORT).show();

                        // Check if the user has a displayName
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null && user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
                            // User has a displayName, redirect to MainActivity
                            redirectToActivity(MainActivity.class);
                        } else {
                            // No displayName, redirect to CreateUserActivity to complete user info
                            redirectToActivity(ProfileActivity.class);
                        }
                    } else {
                        // Error during authentication
                        revertAnimation(btnVerify);
                        Toast.makeText(LoginActivity.this, R.string.error_verifying_otp, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private Bitmap getBitmapFromDrawable(int drawableId) {
        // Convert a drawable resource to a Bitmap
        Drawable drawable = ResourcesCompat.getDrawable(getResources(), drawableId, null);
        if (drawable == null) return null;
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }


    private final PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        @Override
        public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
            // If the verification is completed automatically
            signInWithPhoneAuthCredential(phoneAuthCredential);
            finishAnimation(btnLogin);
            Toast.makeText(LoginActivity.this, R.string.welcome, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onVerificationFailed(@NonNull FirebaseException e) {
            // If the verification fails
            btnLogin.revertAnimation();
            Toast.makeText(LoginActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onCodeSent(@NonNull String verificationId, @NonNull PhoneAuthProvider.ForceResendingToken token) {
            // If the OTP is sent successfully
            isPhoneNumberCorrect = true;
            LoginActivity.this.verificationId = verificationId;
            Toast.makeText(LoginActivity.this, R.string.otp_sent, Toast.LENGTH_SHORT).show();

            // Show the OTP field and the verify button
            finishAnimation(btnLogin);
            showOTPField();

            // Delay the hiding of the button
            new Handler().postDelayed(() -> hideButtonWithAnimation(btnLogin), 1000);
        }
    };

}