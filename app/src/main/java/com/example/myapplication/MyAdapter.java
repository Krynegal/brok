package com.example.myapplication;

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
    private OnItemClickListener onItemClickListener;

    // Интерфейс для обработки кликов
    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    // Constructor
    public MyAdapter(List<Asset> examList, OnItemClickListener listener) {
        this.assetList = examList;
        this.onItemClickListener = listener;
    }

    // Метод для добавления нового элемента
    public void addAsset(Asset newAsset) {
        assetList.add(newAsset);
        // Уведомляем адаптер, что элемент был добавлен
        notifyItemInserted(assetList.size() - 1);
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.exam_card, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Asset examItem = assetList.get(position);

        holder.examName.setText(examItem.getName());
        holder.examDate.setText(examItem.getValue());
        holder.examMessage.setText(examItem.getProfit());
//        holder.examPic.setImageResource(examItem.getImage1());
//        holder.examPic2.setImageResource(examItem.getImage2());

        //holder.examName.setText(data.get(position));

        holder.itemView.setOnClickListener(v -> {
            // Вызываем метод onItemClickListener, чтобы сообщить о клике на элемент
            onItemClickListener.onItemClick(position);
        });
    }

    @Override
    public int getItemCount() {
        return assetList.size();
    }

    // ViewHolder class
    static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView examName, examDate, examMessage;
        ImageView examPic, examPic2;

        public MyViewHolder(@NonNull View itemView) {

            super(itemView);

            examName = itemView.findViewById(R.id.examName);
            examDate = itemView.findViewById(R.id.examDate);
            examMessage = itemView.findViewById(R.id.examMessage);
            examPic = itemView.findViewById(R.id.examPic);
            examPic2 = itemView.findViewById(R.id.examPic2);
        }
    }
}
