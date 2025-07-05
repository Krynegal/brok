package com.example.myapplication;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.view.animation.AnimationUtils;

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
        if (portfolioUpdateListener != null) {
            portfolioUpdateListener.onPortfolioUpdated();
        }
    }

    public void updateAsset(int position, Asset newAsset) {
        assetList.set(position, newAsset);
        //notifyItemInserted(assetList.size() - 1);
        updateTotalSum();
        notifyItemChanged(position);
        if (portfolioUpdateListener != null) {
            portfolioUpdateListener.onPortfolioUpdated();
        }
    }

    // Обновляем сумму всех элементов
    public void updateTotalSum() {
        totalAssetsSum = 0.0;
        for (Asset asset : assetList) {
            totalAssetsSum += asset.getBalance();
        }
    }

    // Интерфейс для уведомления об обновлении метрик портфеля
    public interface OnPortfolioUpdateListener {
        void onPortfolioUpdated();
    }

    private OnPortfolioUpdateListener portfolioUpdateListener;

    public void setOnPortfolioUpdateListener(OnPortfolioUpdateListener listener) {
        this.portfolioUpdateListener = listener;
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
        
        // Применяем анимацию для новых элементов
        if (holder.itemView.getTag() == null) {
            holder.itemView.setTag("animated");
            holder.itemView.startAnimation(AnimationUtils.loadAnimation(
                holder.itemView.getContext(), R.anim.item_animation_fall_down));
        }
        
        holder.textViewAssetName.setText(assetItem.getName());
        String assetType = assetItem.getType();
        holder.textViewAssetType.setText(Asset.getTypeRu(assetType));
        // Баланс
        String formattedBalance = "$" + String.format("%,.0f", assetItem.getBalance());
        holder.textViewAssetValue.setText(formattedBalance);
        // Профит
        Double profit = assetItem.getProfit();
        if (profit != null) {
            String profitStr = (profit >= 0 ? "+" : "") + "$" + String.format("%,.0f", profit);
            holder.textViewAssetProfit.setText(profitStr);
            if (profit >= 0) {
                holder.textViewAssetProfit.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.positive));
            } else {
                holder.textViewAssetProfit.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.negative));
            }
        } else {
            holder.textViewAssetProfit.setText("—");
            holder.textViewAssetProfit.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.text_hint));
        }
        // XIRR, APY, APR
        TextView xirrView = holder.textViewAssetXirr;
        TextView apyView = holder.textViewAssetApy;
        TextView aprView = holder.textViewAssetApr;
        Double xirr = assetItem.getXirr();
        Double apy = assetItem.getApy();
        Double apr = assetItem.getApr();
        // XIRR
        if (xirr != null) {
            xirrView.setText("XIRR: " + String.format("%+.1f%%", xirr));
            xirrView.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.metric_xirr));
            xirrView.setTypeface(null, android.graphics.Typeface.BOLD);
        } else {
            xirrView.setText("XIRR: —");
            xirrView.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.text_hint));
            xirrView.setTypeface(null, android.graphics.Typeface.NORMAL);
        }
        // APY
        if (apy != null) {
            apyView.setText("APY: " + String.format("%+.1f%%", apy));
            apyView.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.metric_apy));
            apyView.setTypeface(null, android.graphics.Typeface.BOLD);
        } else {
            apyView.setText("APY: —");
            apyView.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.text_hint));
            apyView.setTypeface(null, android.graphics.Typeface.NORMAL);
        }
        // APR
        if (apr != null) {
            aprView.setText("APR: " + String.format("%+.1f%%", apr));
            aprView.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.metric_apr));
            aprView.setTypeface(null, android.graphics.Typeface.BOLD);
        } else {
            aprView.setText("APR: —");
            aprView.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.text_hint));
            aprView.setTypeface(null, android.graphics.Typeface.NORMAL);
        }
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
        TextView textViewAssetName, textViewAssetType, textViewAssetValue, textViewAssetProfit;
        TextView textViewAssetXirr, textViewAssetApy, textViewAssetApr;
        
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewAssetName = itemView.findViewById(R.id.textViewAssetName);
            textViewAssetType = itemView.findViewById(R.id.textViewAssetType);
            textViewAssetValue = itemView.findViewById(R.id.textViewAssetValue);
            textViewAssetProfit = itemView.findViewById(R.id.textViewAssetProfit);
            textViewAssetXirr = itemView.findViewById(R.id.textViewAssetXirr);
            textViewAssetApy = itemView.findViewById(R.id.textViewAssetApy);
            textViewAssetApr = itemView.findViewById(R.id.textViewAssetApr);
        }
    }
}
