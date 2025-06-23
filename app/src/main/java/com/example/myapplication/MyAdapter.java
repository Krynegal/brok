package com.example.myapplication;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.Models.Asset;

import java.util.List;

public class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyViewHolder> {

    private List<Asset> assetList;
    private Double totalAssetsSum;
    private OnItemClickListener onItemClickListener;

    // Интерфейс для обработки кликов
    public interface OnItemClickListener {
        void onItemClick(int position);
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
    private void updateTotalSum() {
        totalAssetsSum = 0.0;
        for (Asset asset : assetList) {
            totalAssetsSum += asset.getBalance();
        }
    }

    public Double getTotalSum() {
        return totalAssetsSum;
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
        // holder.textViewAssetName.setText(assetItem.getName());
        // holder.textViewAssetValue.setText(assetItem.getValue());
        // holder.textViewAssetProfit.setText(assetItem.getProfit());
        holder.textViewAssetName.setText(assetItem.getName());
        holder.textViewAssetValue.setText(String.valueOf(assetItem.getBalance()));
        holder.textViewAssetProfit.setText(assetItem.getType()); // или другое поле, если нужно

        //holder.imageViewAssetIcon.setImageResource(...); // если нужно
        //holder.imageViewAssetStatus.setImageResource(...); // если нужно
        holder.itemView.setOnClickListener(v -> {
            onItemClickListener.onItemClick(position);
        });
    }

    @Override
    public int getItemCount() {
        return assetList.size();
    }

    // ViewHolder class
    static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView textViewAssetName, textViewAssetValue, textViewAssetProfit;
        ImageView imageViewAssetIcon, imageViewAssetStatus;
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewAssetName = itemView.findViewById(R.id.textViewAssetName);
            textViewAssetValue = itemView.findViewById(R.id.textViewAssetValue);
            textViewAssetProfit = itemView.findViewById(R.id.textViewAssetProfit);
            imageViewAssetIcon = itemView.findViewById(R.id.imageViewAssetIcon);
            imageViewAssetStatus = itemView.findViewById(R.id.imageViewAssetStatus);
        }
    }
}
