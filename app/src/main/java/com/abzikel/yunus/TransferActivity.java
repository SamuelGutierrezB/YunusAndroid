package com.abzikel.yunus;

import static com.abzikel.yunus.utils.Methods.getBitmapFromDrawable;

import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.abzikel.yunus.pojos.User;
import com.abzikel.yunus.utils.DecimalDigitsInputFilter;
import com.github.leandroborgesferreira.loadingbutton.customViews.CircularProgressButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.hbb20.CountryCodePicker;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class TransferActivity extends AppCompatActivity {
    private FirebaseUser currentUser;
    private FirebaseFirestore db;
    private User user;
    private TextView tvName, tvPhone, tvAmount, tvContributionRate, tvRealAmount, tvConcept;
    private CircularProgressButton btnTransfer;
    private double transactionAmount = 0.00;
    private int greenColor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer);
        setup();
    }

    private void setup() {
        // Initialize views
        CardView cvUser = findViewById(R.id.cvUser);
        CardView cvInformation = findViewById(R.id.cvInformation);
        tvName = findViewById(R.id.tvName);
        tvPhone = findViewById(R.id.tvPhone);
        tvAmount = findViewById(R.id.tvAmount);
        tvContributionRate = findViewById(R.id.tvContributionRate);
        tvRealAmount = findViewById(R.id.tvRealAmount);
        tvConcept = findViewById(R.id.tvConcept);
        btnTransfer = findViewById(R.id.btnTransfer);

        // Get color from the resources
        greenColor = ContextCompat.getColor(this, R.color.fern_green);

        // FirebaseAuth and FirebaseFirestore instance
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();

        // Set click listener for the user card
        cvUser.setOnClickListener(v -> showUserDialog());

        // Set click listener for the information card
        cvInformation.setOnClickListener(v -> showInformationDialog());

        // Set click listener for the transfer button
        btnTransfer.setOnClickListener(v -> validateAndTransfer());
    }

    private void showUserDialog() {
        // Inflate custom content dialog and get context
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.content_dialog_user, null);

        // Link XML to Java
        CountryCodePicker ccp = dialogView.findViewById(R.id.ccp);
        EditText etvPhone = dialogView.findViewById(R.id.etvPhone);
        CircularProgressButton btnSearch = dialogView.findViewById(R.id.btnSearch);

        // Get context
        Context context = this;

        // Initialize Country Code Picker
        ccp.registerCarrierNumberEditText(etvPhone);
        if (user != null) ccp.setFullNumber(user.getPhone());

        // Create Alert Dialog to ask confirmation
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomAlertDialog);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        // Set click listener for the search button
        btnSearch.setOnClickListener(v -> {
            // Get full number with plus
            String fullPhoneNumber = ccp.getFullNumberWithPlus();

            // Check if the phone number is valid
            if (!ccp.isValidFullNumber()) {
                Toast.makeText(context, R.string.invalid_phone, Toast.LENGTH_SHORT).show();
                return;
            }

            // Check if the current user is the same as the one being searched
            if (currentUser != null && Objects.equals(currentUser.getPhoneNumber(), fullPhoneNumber)) {
                Toast.makeText(context, R.string.same_phone, Toast.LENGTH_SHORT).show();
                return;
            }

            // Start the animation
            btnSearch.startAnimation();

            // Query the Firestore collection for the user with the given phone number
            CollectionReference usersRef = db.collection("users");
            usersRef.whereEqualTo("phone", fullPhoneNumber)
                    .limit(1)
                    .get().addOnSuccessListener(queryDocumentSnapshots -> {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            // User found, update the user object
                            DocumentSnapshot userDoc = queryDocumentSnapshots.getDocuments().get(0);
                            user = userDoc.toObject(User.class);

                            // Verify if the user exists
                            if (user != null) {
                                // Update the UI with the user's information
                                String fullName = user.getFirstName() + " " + user.getLastName();
                                tvName.setText(fullName);
                                tvPhone.setText(user.getPhone());

                                // Finish the animation
                                btnSearch.setBackgroundTintList(ColorStateList.valueOf(greenColor));
                                btnSearch.doneLoadingAnimation(greenColor, Objects.requireNonNull(getBitmapFromDrawable(context, R.drawable.ic_check)));
                                dialog.dismiss();
                            }
                        } else {
                            // User not found
                            Toast.makeText(context, R.string.user_not_found, Toast.LENGTH_SHORT).show();

                            // Revert the animation
                            revertAnimation(btnSearch);
                        }
                    }).addOnFailureListener(e -> {
                        // User not found
                        Toast.makeText(context, R.string.user_not_found, Toast.LENGTH_SHORT).show();

                        // Revert the animation
                        revertAnimation(btnSearch);
                    });
        });

        // Show Alert Dialog
        dialog.show();
    }

    private void showInformationDialog() {
        // Inflate custom content dialog and get context
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.content_dialog_information, null);

        // Link XML to Java
        EditText etvAmount = dialogView.findViewById(R.id.etvAmount);
        EditText etvConcept = dialogView.findViewById(R.id.etvConcept);
        TextView tvAmount = dialogView.findViewById(R.id.tvAmount);
        //TextView tvContributionRate = dialogView.findViewById(R.id.tvContributionRate);
        TextView tvRealAmount = dialogView.findViewById(R.id.tvRealAmount);
        Button btnSave = dialogView.findViewById(R.id.btnSave);

        // Set views with information
        etvAmount.setText(String.valueOf(transactionAmount));
        if (tvConcept.getVisibility() == View.VISIBLE) {
            etvConcept.setText(tvConcept.getText());
        }

        // Get the "zero" string from resources
        String zeroString = getString(R.string.zero);

        // Add text change listener to the EditText
        etvAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Check if input is empty and update TextViews accordingly
                if (s.toString().isEmpty()) {
                    // Reset the TextViews
                    tvAmount.setText(zeroString);
                    //tvContributionRate.setText(zeroString);
                    tvRealAmount.setText(zeroString);
                } else {
                    try {
                        // Parse the amount and update the TextViews
                        double amount = Double.parseDouble(s.toString());
                        String formattedAmount = String.format(Locale.getDefault(), "%.2f", amount);
                        //String contributionRate = String.format(Locale.getDefault(), "%.2f", amount * 0.1);
                        String realAmount = String.format(Locale.getDefault(), "%.2f", amount * 1);

                        // Set the text amounts to the TextViews
                        tvAmount.setText(formattedAmount);
                        //tvContributionRate.setText(contributionRate);
                        tvRealAmount.setText(realAmount);
                    } catch (NumberFormatException e) {
                        // Handle invalid number input gracefully
                        tvAmount.setText(zeroString);
                        //tvContributionRate.setText(zeroString);
                        tvRealAmount.setText(zeroString);
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        // Set input filter to allow only two decimal places
        etvAmount.setFilters(new InputFilter[]{new DecimalDigitsInputFilter(2)});

        // Create Alert Dialog to ask confirmation
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomAlertDialog);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        // Set on click listener for save button
        btnSave.setOnClickListener(v -> {
            // Check if there is a valid number
            if (etvAmount.getText().toString().isEmpty()) {
                Toast.makeText(this, R.string.unfilled, Toast.LENGTH_SHORT).show();
                return;
            }

            // Save important information
            String concept = etvConcept.getText().toString();
            transactionAmount = Double.parseDouble(etvAmount.getText().toString());

            // Update TextViews
            this.tvAmount.setText(String.valueOf(transactionAmount));
            //this.tvContributionRate.setText(String.valueOf(transactionAmount * 0.1));
            this.tvRealAmount.setText(String.valueOf(transactionAmount * 1));

            // Set concept text
            if (!concept.isEmpty()) {
                this.tvConcept.setText(concept);
                this.tvConcept.setVisibility(View.VISIBLE);
            } else {
                this.tvConcept.setVisibility(View.GONE);
            }

            // Dismiss dialog
            dialog.dismiss();
        });

        // Show Alert Dialog
        dialog.show();
    }

    private void revertAnimation(CircularProgressButton button) {
        // Revert the animation to its original state
        button.revertAnimation();
        button.setBackgroundResource(R.drawable.custom_button);
    }

    private void validateAndTransfer() {
        // Check if user is valid
        if (user == null) {
            Toast.makeText(this, R.string.fill_user_information, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if transaction amount is valid
        if (transactionAmount == 0.00) {
            Toast.makeText(this, R.string.fill_transaction_information, Toast.LENGTH_SHORT).show();
            return;
        }

        // Perform the transfer
        performTransfer();
    }

    private void performTransfer() {
        // Start the animation
        btnTransfer.startAnimation();

        // Get receiver's id and define bank id
        String receiverId = user.getId();
        String bankId = "gIzw5hmt9hALqPy6hER9";

        // Calculate the commission and transfer amount
        double contribution = transactionAmount * 0.1;
        //double transferAmount = transactionAmount - contribution;
        double transferAmount = transactionAmount;

        // Get concept
        String concept = tvConcept.getText().toString();

        // Create a batch to perform multiple Firestore operations
        WriteBatch batch = db.batch();

        // Update the sender's Yunus balance
        batch.update(db.collection("users").document(currentUser.getUid()),
                "yunus", FieldValue.increment(-transactionAmount));

        // Update the receiver's Yunus balance
        batch.update(db.collection("users").document(receiverId),
                "yunus", FieldValue.increment(transferAmount));

        // Update the bank's Yunus balance with the commission
        //batch.update(db.collection("users").document(bankId),
        //        "yunus", FieldValue.increment(contribution));

        // Get transaction document
        DocumentReference transactionDoc = db.collection("transactions").document();

        // Create transaction map
        Map<String, Object> transactionMap = new HashMap<>();
        transactionMap.put("id", transactionDoc.getId());
        transactionMap.put("senderId", currentUser.getUid());
        transactionMap.put("receiverId", receiverId);
        transactionMap.put("senderPhone", currentUser.getPhoneNumber());
        transactionMap.put("receiverPhone", user.getPhone());
        transactionMap.put("senderName", currentUser.getDisplayName());
        transactionMap.put("receiverName", user.getFirstName() + " " + user.getLastName());
        transactionMap.put("concept", concept);
        transactionMap.put("yunus", transactionAmount);
        transactionMap.put("date", FieldValue.serverTimestamp());
        transactionMap.put("users", Arrays.asList(currentUser.getUid(), receiverId));

        // Add the transaction to Firestore
        batch.set(transactionDoc, transactionMap);

        // Commit the batch
        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    // Transaction successful
                    Toast.makeText(this, R.string.transaction_successful, Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    // Handle the error
                    Toast.makeText(this, R.string.error_transaction, Toast.LENGTH_SHORT).show();
                    revertAnimation(btnTransfer);
                });
    }

}
