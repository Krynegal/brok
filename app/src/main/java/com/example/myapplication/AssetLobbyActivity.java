package com.example.myapplication;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.activity.OnBackPressedCallback;

import com.example.myapplication.Models.Asset;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AssetLobbyActivity extends AppCompatActivity {
    private List<Transaction> transactionList = new ArrayList<>();
    private TransactionAdapter transactionAdapter;
    private String assetId;
    private String token;
    private TextView balanceView;
    private double assetBalance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_asset_lobby);

        // Получаем данные актива из Intent
        assetId = getIntent().getStringExtra("assetId");
        String assetName = getIntent().getStringExtra("assetName");
        assetBalance = getIntent().getDoubleExtra("assetBalance", 0.0);

        // Получаем токен из SharedPreferences
        SharedPreferences prefs = getSharedPreferences("auth", MODE_PRIVATE);
        String rawToken = prefs.getString("token", null);
        if (rawToken == null) {
            Toast.makeText(this, "Требуется вход", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        token = "Bearer " + rawToken;

        // Заполняем инфо о счёте
        TextView nameView = findViewById(R.id.textViewAssetName);
        balanceView = findViewById(R.id.textViewAssetBalance);
        TextView xirrView = findViewById(R.id.textViewXIRR);
        TextView apyView = findViewById(R.id.textViewAPY);
        TextView aprView = findViewById(R.id.textViewAPR);
        nameView.setText(assetName);
        xirrView.setText("+150% XIRR"); // Мок-данные, можно заменить на реальные
        apyView.setText("+130% APY");
        aprView.setText("+140% APR");

        // Настраиваем RecyclerView для операций
        RecyclerView recyclerView = findViewById(R.id.transactionsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        transactionAdapter = new TransactionAdapter(transactionList);
        recyclerView.setAdapter(transactionAdapter);

        // Кнопка добавления операции
        findViewById(R.id.buttonAddTransaction).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddTransactionDialog();
            }
        });

        // Загружаем актуальные данные актива и транзакции
        loadAssetData();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("assetId", assetId);
                resultIntent.putExtra("assetBalance", assetBalance);
                TextView nameView = findViewById(R.id.textViewAssetName);
                resultIntent.putExtra("assetName", nameView.getText().toString());
                setResult(RESULT_OK, resultIntent);
                finish();
            }
        });
    }

    private void loadAssetData() {
        // Загружаем все активы и находим нужный по ID
        AssetApiService api = ApiClient.getClient().create(AssetApiService.class);
        api.getAssets(token).enqueue(new Callback<List<Asset>>() {
            @Override
            public void onResponse(Call<List<Asset>> call, Response<List<Asset>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Ищем нужный актив по ID
                    Asset targetAsset = null;
                    for (Asset asset : response.body()) {
                        if (assetId.equals(asset.getId())) {
                            targetAsset = asset;
                            break;
                        }
                    }
                    
                    if (targetAsset != null) {
                        assetBalance = targetAsset.getBalance();
                        
                        // Обновляем название актива (на случай если оно изменилось)
                        TextView nameView = findViewById(R.id.textViewAssetName);
                        nameView.setText(targetAsset.getName());
                        
                        Log.d("AssetLobby", "Актив найден. Баланс: " + assetBalance);
                        
                        // После загрузки актива загружаем транзакции
                        loadTransactionsFromBackend();
                    } else {
                        Log.e("AssetLobby", "Актив с ID " + assetId + " не найден");
                        Toast.makeText(AssetLobbyActivity.this, "Актив не найден", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    String errorBody = "";
                    try {
                        if (response.errorBody() != null) {
                            errorBody = response.errorBody().string();
                        }
                    } catch (Exception e) {
                        errorBody = "Не удалось прочитать тело ошибки";
                    }
                    Log.e("AssetLobby", "Ошибка загрузки активов. HTTP: " + response.code() + 
                          ", URL: " + call.request().url() + 
                          ", Error: " + errorBody);
                    Toast.makeText(AssetLobbyActivity.this, "Ошибка загрузки актива", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<List<Asset>> call, Throwable t) {
                Log.e("AssetLobby", "Ошибка сети при загрузке активов", t);
                Toast.makeText(AssetLobbyActivity.this, "Ошибка сети", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadTransactionsFromBackend() {
        AssetApiService api = ApiClient.getClient().create(AssetApiService.class);
        api.getAssetTransactions(token, assetId).enqueue(new Callback<List<Transaction>>() {
            @Override
            public void onResponse(Call<List<Transaction>> call, Response<List<Transaction>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    transactionList.clear();
                    transactionList.addAll(response.body());
                    // Сортируем транзакции по дате (новые сверху)
                    sortTransactionsByDate();
                    transactionAdapter.notifyDataSetChanged();
                    
                    // Показываем баланс с backend и рассчитываем прибыль
                    updateProfitAndBalance();
                    
                    Log.d("AssetLobby", "Транзакции загружены: " + response.body().size() + " шт.");
                } else {
                    String errorBody = "";
                    try {
                        if (response.errorBody() != null) {
                            errorBody = response.errorBody().string();
                        }
                    } catch (Exception e) {
                        errorBody = "Не удалось прочитать тело ошибки";
                    }
                    Log.e("AssetLobby", "Ошибка загрузки транзакций. HTTP: " + response.code() + 
                          ", URL: " + call.request().url() + 
                          ", Error: " + errorBody);
                    Toast.makeText(AssetLobbyActivity.this, "Ошибка загрузки операций", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<List<Transaction>> call, Throwable t) {
                Log.e("AssetLobby", "Ошибка сети при загрузке транзакций", t);
                Toast.makeText(AssetLobbyActivity.this, "Ошибка сети", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sortTransactionsByDate() {
        // Сортируем по timestamp в обратном порядке (новые сверху)
        transactionList.sort((t1, t2) -> {
            try {
                SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
                Date date1 = isoFormat.parse(t1.timestamp);
                Date date2 = isoFormat.parse(t2.timestamp);
                return date2.compareTo(date1); // Обратный порядок
            } catch (Exception e) {
                return 0;
            }
        });
    }

    private void updateProfitAndBalance() {
        double profit = calculateProfit(transactionList);
        String profitStr = (profit >= 0 ? "+" : "") + "$" + String.format("%,.0f", profit);
        balanceView.setText("$" + String.format("%,.0f", assetBalance) + " (" + profitStr + ")");
    }

    private double calculateProfit(List<Transaction> transactions) {
        double totalIncome = 0;
        double totalExpense = 0;
        for (Transaction t : transactions) {
            if ("income".equals(t.type)) {
                totalIncome += t.amount;
            } else if ("expense".equals(t.type)) {
                totalExpense += t.amount;
            }
        }
        return totalIncome - totalExpense;
    }

    private double calculateNewBalance() {
        double balance = 0;
        for (Transaction t : transactionList) {
            if ("income".equals(t.type)) {
                balance += t.amount;
            } else if ("expense".equals(t.type)) {
                balance -= t.amount;
            }
        }
        return balance;
    }

    private void showAddTransactionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Добавить операцию");

        LinearLayout dialogLayout = new LinearLayout(this);
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        final EditText editType = new EditText(this);
        editType.setHint("Тип операции (income/expense)");
        final EditText editAmount = new EditText(this);
        editAmount.setHint("Сумма");
        final EditText editDescription = new EditText(this);
        editDescription.setHint("Описание (необязательно)");
        dialogLayout.addView(editType);
        dialogLayout.addView(editAmount);
        dialogLayout.addView(editDescription);
        builder.setView(dialogLayout);

        builder.setPositiveButton("Готово", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String type = editType.getText().toString();
                String amountStr = editAmount.getText().toString();
                String description = editDescription.getText().toString();
                if (type.isEmpty() || amountStr.isEmpty()) {
                    Toast.makeText(AssetLobbyActivity.this, "Заполните все поля", Toast.LENGTH_SHORT).show();
                    return;
                }
                double amount = 0.0;
                try {
                    amount = Double.parseDouble(amountStr.replace(",", "").replace("$", ""));
                } catch (Exception e) {
                    Toast.makeText(AssetLobbyActivity.this, "Некорректная сумма", Toast.LENGTH_SHORT).show();
                    return;
                }
                // Отправляем транзакцию на backend
                CreateTransactionRequest req = new CreateTransactionRequest();
                req.amount = amount;
                req.type = type;
                req.description = description;
                AssetApiService api = ApiClient.getClient().create(AssetApiService.class);
                api.createTransaction(token, assetId, req).enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {
                            loadAssetData();
                            Toast.makeText(AssetLobbyActivity.this, "Операция добавлена", Toast.LENGTH_SHORT).show();
                            Log.d("AssetLobby", "Транзакция успешно добавлена");
                            // Передаём обновлённые данные актива назад
                            Intent resultIntent = new Intent();
                            resultIntent.putExtra("assetId", assetId);
                            resultIntent.putExtra("assetBalance", assetBalance); // assetBalance обновляется в loadAssetData
                            TextView nameView = findViewById(R.id.textViewAssetName);
                            resultIntent.putExtra("assetName", nameView.getText().toString());
                            setResult(RESULT_OK, resultIntent);
                            setResult(RESULT_OK);
                        } else {
                            String errorBody = "";
                            try {
                                if (response.errorBody() != null) {
                                    errorBody = response.errorBody().string();
                                }
                            } catch (Exception e) {
                                errorBody = "Не удалось прочитать тело ошибки";
                            }
                            Log.e("AssetLobby", "Ошибка добавления транзакции. HTTP: " + response.code() + 
                                  ", URL: " + call.request().url() + 
                                  ", Request: amount=" + req.amount + ", type=" + req.type + ", description=" + req.description +
                                  ", Error: " + errorBody);
                            Toast.makeText(AssetLobbyActivity.this, "Ошибка добавления операции", Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        Log.e("AssetLobby", "Ошибка сети при добавлении транзакции", t);
                        Toast.makeText(AssetLobbyActivity.this, "Ошибка сети", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        builder.setNegativeButton("Отмена", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }
}
