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
            Date date = null;
            SimpleDateFormat isoFormatMs = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
            isoFormatMs.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            try {
                date = isoFormatMs.parse(transaction.timestamp);
            } catch (Exception e1) {
                SimpleDateFormat isoFormatNoMs = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
                isoFormatNoMs.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                date = isoFormatNoMs.parse(transaction.timestamp);
            }
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            holder.textViewTransactionDate.setText(dateFormat.format(date));
            holder.textViewTransactionTime.setText(timeFormat.format(date));
        } catch (Exception e) {
            android.util.Log.e("TransactionAdapter", "Ошибка парсинга даты: '" + transaction.timestamp + "'", e);
            holder.textViewTransactionDate.setText(transaction.timestamp != null ? transaction.timestamp : "");
            holder.textViewTransactionTime.setText("");
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
        TextView textViewTransactionDate, textViewTransactionTime, textViewTransactionType, textViewTransactionAmount;
        ImageView imageViewTransactionIcon;
        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewTransactionDate = itemView.findViewById(R.id.textViewTransactionDate);
            textViewTransactionTime = itemView.findViewById(R.id.textViewTransactionTime);
            textViewTransactionType = itemView.findViewById(R.id.textViewTransactionType);
            textViewTransactionAmount = itemView.findViewById(R.id.textViewTransactionAmount);
        }
    }
} 