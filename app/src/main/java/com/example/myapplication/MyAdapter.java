package com.example.myapplication;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.Models.Asset;

import java.util.List;

public class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyViewHolder> {

    private List<Asset> assetList;
    private Double totalAssetsSum;
    private OnItemClickListener onItemClickListener;
    private OnItemLongClickListener onItemLongClickListener;

    // Интерфейс для обработки кликов
    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(int position);
    }

    // Constructor
    public MyAdapter(List<Asset> assetList, OnItemClickListener listener) {
        this.assetList = assetList;
        this.onItemClickListener = listener;
        updateTotalSum();
    }

    // Метод для добавления нового элемента
    public void addAsset(Asset newAsset) {
        assetList.add(newAsset);
        updateTotalSum();
        // Уведомляем адаптер, что элемент был добавлен
        notifyItemInserted(assetList.size() - 1);
    }

    public void updateAsset(int position, Asset newAsset) {
        assetList.set(position, newAsset);
        //notifyItemInserted(assetList.size() - 1);
        updateTotalSum();
        notifyItemChanged(position);
    }

    // Обновляем сумму всех элементов
    public void updateTotalSum() {
        totalAssetsSum = 0.0;
        for (Asset asset : assetList) {
            totalAssetsSum += asset.getBalance();
        }
    }

    public Double getTotalSum() {
        return totalAssetsSum;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.onItemLongClickListener = listener;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_asset, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Asset assetItem = assetList.get(position);
        
        // Название актива
        holder.textViewAssetName.setText(assetItem.getName());
        
        // Баланс в долларах с форматированием
        String formattedBalance = "$" + String.format("%,.0f", assetItem.getBalance());
        holder.textViewAssetValue.setText(formattedBalance);
        
        // Тип актива (с заглавной буквы)
        String assetType = assetItem.getType();
        if (assetType != null && !assetType.isEmpty()) {
            String capitalizedType = assetType.substring(0, 1).toUpperCase() + assetType.substring(1).toLowerCase();
            holder.textViewAssetProfit.setText(capitalizedType);
        } else {
            holder.textViewAssetProfit.setText("Актив");
        }

        //holder.imageViewAssetIcon.setImageResource(...); // если нужно
        //holder.imageViewAssetStatus.setImageResource(...); // если нужно
        holder.itemView.setOnClickListener(v -> {
            onItemClickListener.onItemClick(position);
        });
        holder.itemView.setOnLongClickListener(v -> {
            if (onItemLongClickListener != null) {
                onItemLongClickListener.onItemLongClick(position);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return assetList.size();
    }

    // ViewHolder class
    static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView textViewAssetName, textViewAssetValue, textViewAssetProfit;
        
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewAssetName = itemView.findViewById(R.id.textViewAssetName);
            textViewAssetValue = itemView.findViewById(R.id.textViewAssetValue);
            textViewAssetProfit = itemView.findViewById(R.id.textViewAssetProfit);
        }
    }
}
