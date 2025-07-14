package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.List;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {
    private List<Transaction> transactionList;

    public TransactionAdapter(List<Transaction> transactionList) {
        this.transactionList = transactionList;
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transaction, parent, false);
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        Transaction transaction = transactionList.get(position);
        // Преобразуем ISO-дату в человекочитаемый формат
        try {
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
            Date date = isoFormat.parse(transaction.timestamp);
            SimpleDateFormat displayFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
            holder.textViewTransactionDateTime.setText(displayFormat.format(date));
        } catch (Exception e) {
            holder.textViewTransactionDateTime.setText(transaction.timestamp); // fallback
        }
        holder.textViewTransactionType.setText(transaction.type);
        String currencySymbol = getCurrencySymbol(transaction.currency);
        holder.textViewTransactionAmount.setText(currencySymbol + String.format("%,.0f", transaction.amount));
        // Можно менять иконку в зависимости от типа операции
        // holder.imageViewTransactionIcon.setImageResource(...);
    }

    private String getCurrencySymbol(String code) {
        if (code == null) return "$";
        switch (code) {
            case "USD": return "$";
            case "EUR": return "€";
            case "RUB": return "₽";
            case "GBP": return "£";
            case "JPY": return "¥";
            case "CNY": return "¥";
            case "CHF": return "₣";
            case "CAD": return "$";
            case "AUD": return "$";
            case "KRW": return "₩";
            default: return code + " ";
        }
    }

    @Override
    public int getItemCount() {
        return transactionList.size();
    }

    static class TransactionViewHolder extends RecyclerView.ViewHolder {
        TextView textViewTransactionDateTime, textViewTransactionType, textViewTransactionAmount;
        ImageView imageViewTransactionIcon;
        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewTransactionDateTime = itemView.findViewById(R.id.textViewTransactionDateTime);
            textViewTransactionType = itemView.findViewById(R.id.textViewTransactionType);
            textViewTransactionAmount = itemView.findViewById(R.id.textViewTransactionAmount);
        }
    }
} 