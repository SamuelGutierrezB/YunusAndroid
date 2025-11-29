package com.abzikel.yunus.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.abzikel.yunus.R;
import com.abzikel.yunus.TransferActivity;
import com.abzikel.yunus.adapters.TransactionAdapter;
import com.abzikel.yunus.pojos.Transaction;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TransactionsFragment extends Fragment {
    private static final int PAGE_SIZE = 10;
    private RecyclerView rvTransactions;
    private LinearLayout linearLayoutNoTransactions;
    private FirebaseFirestore db;
    private TransactionAdapter transactionAdapter;
    private DocumentSnapshot lastVisibleTransaction;
    private List<Transaction> transactionsList;
    String currentUserId;
    private boolean isLoading = false;

    public TransactionsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_transactions, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Firebase Firestore and other variables
        rvTransactions = view.findViewById(R.id.rvTransactions);
        linearLayoutNoTransactions = view.findViewById(R.id.linearLayoutNoTransactions);
        db = FirebaseFirestore.getInstance();
        transactionsList = new ArrayList<>();
        lastVisibleTransaction = null;
        isLoading = false;
        currentUserId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();

        // Set up the RecyclerView with a LinearLayoutManager
        rvTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));
        transactionAdapter = new TransactionAdapter(transactionsList);
        rvTransactions.setAdapter(transactionAdapter);

        // Load the first page of transactions
        loadTransactions();

        // Set up infinite scroll listener
        rvTransactions.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (!recyclerView.canScrollVertically(1) && !isLoading) {
                    loadTransactions();
                }
            }
        });

        // Set up the Transfer button
        Button btnGoToTransfer = view.findViewById(R.id.btnGoToTransfer);
        btnGoToTransfer.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), TransferActivity.class);
            startActivity(intent);
        });
    }

    private void loadTransactions() {
        // Prevent loading if already loading
        if (isLoading) return;

        // Set loading state
        isLoading = true;

        // Create a query to retrieve transactions for the current user
        Query query = db.collection("transactions")
                .whereArrayContains("users", currentUserId)
                .orderBy("date", Query.Direction.DESCENDING)
                .limit(PAGE_SIZE);

        // Use last visible document for pagination if exists
        if (lastVisibleTransaction != null) {
            query = query.startAfter(lastVisibleTransaction);
        }

        // Execute the query and handle the results
        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                // Get the documents from the query result
                List<DocumentSnapshot> documents = task.getResult().getDocuments();

                if (!documents.isEmpty()) {
                    // Store the last visible document for the next query
                    lastVisibleTransaction = documents.get(documents.size() - 1);

                    // Clear the list and add new transactions
                    int startPosition = transactionsList.size();
                    for (DocumentSnapshot document : documents) {
                        Transaction transaction = document.toObject(Transaction.class);
                        transactionsList.add(transaction);
                    }

                    // Notify only the inserted range
                    transactionAdapter.notifyItemRangeInserted(startPosition, documents.size());
                }
            }

            // Set loading state
            isLoading = false;
            changeVisibility();
        }).addOnFailureListener(e -> {
            // Handle failure
            isLoading = false;
            changeVisibility();
        });
    }

    private void changeVisibility() {
        // Check if the list is empty and update the visibility of the views accordingly
        boolean isEmpty = transactionsList.isEmpty();
        rvTransactions.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        linearLayoutNoTransactions.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }

}

