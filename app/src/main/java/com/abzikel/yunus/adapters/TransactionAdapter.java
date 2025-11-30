package com.abzikel.yunus.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.abzikel.yunus.R;
import com.abzikel.yunus.pojos.Transaction;
import com.google.firebase.auth.FirebaseAuth;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {
    private final List<Transaction> transactionList;

    public TransactionAdapter(List<Transaction> transactionList) {
        this.transactionList = transactionList;
    }

    @NonNull
    @Override
    public TransactionAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the layout for each item and create the ViewHolder
        View view = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.item_transaction, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionAdapter.ViewHolder holder, int position) {
        // Bind the data to the view holder
        holder.setup(transactionList.get(position));
    }

    @Override
    public int getItemCount() {
        return transactionList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final CardView cvTransaction;
        private final ImageView ivTransaction;
        private final TextView tvYunus, tvDate;
        private final SimpleDateFormat dateFormat = new SimpleDateFormat(
                "dd MMM yyyy, hh:mm a", Locale.getDefault());
        private final Context context;
        private final String uid;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            // Link XML to Java
            cvTransaction = itemView.findViewById(R.id.cvInformation);
            ivTransaction = itemView.findViewById(R.id.ivTransaction);
            tvYunus = itemView.findViewById(R.id.tvYunus);
            tvDate = itemView.findViewById(R.id.tvDate);

            // Get context
            context = itemView.getContext();

            // Get current user ID
            uid = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        }

        public void setup(Transaction transaction) {
            // Get displayed yunus based on the transaction type
            DecimalFormat decimalFormat = new DecimalFormat("0.00");
            double displayedYunus = transaction.getSenderId().equals(uid)
                    ? transaction.getYunus()
                    : transaction.getYunus() * 1;
            // transaction.getYunus() * 0.9;
            String displayedYunusText = decimalFormat.format(displayedYunus);

            // Set the image based on the transaction type
            ivTransaction.setImageResource(
                    transaction.getSenderId().equals(uid)
                            ? R.drawable.ic_send
                            : R.drawable.ic_receive);

            // Initialize views
            tvYunus.setText(displayedYunusText);
            tvDate.setText(dateFormat.format(transaction.getDate()));

            // Set click listener for the card view
            cvTransaction.setOnClickListener(v -> showTransactionInformation(transaction, displayedYunusText));
        }

        private void showTransactionInformation(Transaction transaction, String displayedYunus) {
            // Create Alert Dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.CustomAlertDialog);
            LayoutInflater inflater = LayoutInflater.from(context);
            View view = inflater.inflate(R.layout.content_dialog_transaction, null);

            // Link XML to Java
            TextView tvSenderName = view.findViewById(R.id.tvSenderName);
            TextView tvSenderPhone = view.findViewById(R.id.tvSenderPhone);
            TextView tvReceiverName = view.findViewById(R.id.tvReceiverName);
            TextView tvReceiverPhone = view.findViewById(R.id.tvReceiverPhone);
            TextView tvAmount = view.findViewById(R.id.tvAmount);
            TextView tvDate = view.findViewById(R.id.tvDate);
            TextView tvConcept = view.findViewById(R.id.tvConcept);

            // Hide concept if it's empty
            if (transaction.getConcept().isEmpty())
                tvConcept.setVisibility(View.GONE);

            // Initialize views
            tvSenderName.setText(transaction.getSenderName());
            tvSenderPhone.setText(transaction.getSenderPhone());
            tvReceiverName.setText(transaction.getReceiverName());
            tvReceiverPhone.setText(transaction.getReceiverPhone());
            tvAmount.setText(displayedYunus);
            tvDate.setText(dateFormat.format(transaction.getDate()));
            tvConcept.setText(transaction.getConcept());

            // Set click listener for the close button
            builder.setView(view).setPositiveButton(R.string.close, (dialog, which) -> dialog.dismiss());

            // Show the dialog
            AlertDialog dialog = builder.create();
            dialog.show();

            // Change the color of the positive button
            int nightColor = ContextCompat.getColor(context, R.color.night);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(nightColor);
        }

    }

}
