package com.example.myapplication;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.activity.OnBackPressedCallback;

import com.example.myapplication.Models.Asset;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import android.widget.ArrayAdapter;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class AssetLobbyActivity extends AppCompatActivity {
    private List<Transaction> transactionList = new ArrayList<>();
    private TransactionAdapter transactionAdapter;
    private String assetId;
    private String token;
    private TextView balanceView;
    private TextView typeView;
    private double assetBalance;
    private Asset currentAsset;

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
        TextView xirrView = findViewById(R.id.textViewAssetXirr);
        TextView apyView = findViewById(R.id.textViewAssetApy);
        TextView aprView = findViewById(R.id.textViewAssetApr);
        typeView = findViewById(R.id.textViewAssetType);
        nameView.setText(assetName);
        // Отображаем тип актива из Intent (с переводом)
        String assetType = getIntent().getStringExtra("assetType");
        typeView.setText(Asset.getTypeRu(assetType));
        xirrView.setText("XIRR: —");
        apyView.setText("APY: —");
        aprView.setText("APR: —");

        // Настраиваем RecyclerView для операций
        RecyclerView recyclerView = findViewById(R.id.recyclerViewTransactions);
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

        getOnBackPressedDispatcher().addCallback((LifecycleOwner) this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("assetId", assetId);
                resultIntent.putExtra("assetBalance", assetBalance);
                TextView nameView = findViewById(R.id.textViewAssetName);
                resultIntent.putExtra("assetName", nameView.getText().toString());
                
                // Передаем обновленные значения функции, если они есть
                if (currentAsset != null) {
                    resultIntent.putExtra("assetXirr", currentAsset.getXirr());
                    resultIntent.putExtra("assetApy", currentAsset.getApy());
                    resultIntent.putExtra("assetApr", currentAsset.getApr());
                }
                
                setResult(RESULT_OK, resultIntent);
                finish();
            }
        });
    }

    private void loadAssetData() {
        // Загружаем все активы и находим нужный по ID
        AssetApiService api = ApiClient.getClient().create(AssetApiService.class);
        api.getAssets(token).enqueue(new Callback<List<Asset>>() {
            @SuppressLint({"SetTextI18n", "DefaultLocale"})
            @Override
            public void onResponse(Call<List<Asset>> call, Response<List<Asset>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Ищем нужный актив по ID
                    Asset targetAsset = null;
                    for (Asset asset : response.body()) {
                        if (asset.getId() != null && asset.getId().equals(assetId)) {
                            targetAsset = asset;
                            break;
                        }
                    }
                    
                    if (targetAsset != null) {
                        assetBalance = targetAsset.getBalance();
                        
                        // Обновляем название актива (на случай если оно изменилось)
                        TextView nameView = findViewById(R.id.textViewAssetName);
                        nameView.setText(targetAsset.getName());
                        
                        // Обновляем XIRR, APY, APR
                        TextView xirrView = findViewById(R.id.textViewAssetXirr);
                        TextView apyView = findViewById(R.id.textViewAssetApy);
                        TextView aprView = findViewById(R.id.textViewAssetApr);
                        Double xirr = targetAsset.getXirr();
                        Double apy = targetAsset.getApy();
                        Double apr = targetAsset.getApr();
                        xirrView.setText("XIRR: " + (xirr != null ? String.format("%+.1f%%", xirr) : "—"));
                        apyView.setText("APY: " + (apy != null ? String.format("%+.1f%%", apy) : "—"));
                        aprView.setText("APR: " + (apr != null ? String.format("%+.1f%%", apr) : "—"));
                        
                        // Обновляем тип актива
                        String typeEn = targetAsset.getType();
                        typeView.setText(Asset.getTypeRu(typeEn));
                        
                        Log.d("AssetLobby", "Актив найден. Баланс: " + assetBalance);
                        
                        // После загрузки актива загружаем транзакции
                        loadTransactionsFromBackend();
                        updateProfitAndBalance(targetAsset);
                        currentAsset = targetAsset;
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
                    updateProfitAndBalance(currentAsset);
                    
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

    private void updateProfitAndBalance(Asset asset) {
        Double profit = asset.getProfit();
        String currencySymbol = getCurrencySymbol(asset.getCurrency());
        String balanceStr = currencySymbol + String.format("%,.0f", assetBalance);
        if (profit != null) {
            String profitStr = (profit >= 0 ? "+" : "") + currencySymbol + String.format("%,.0f", profit);
            balanceStr += " (" + profitStr + ")";
        }
        balanceView.setText(balanceStr);
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

    private void showAddTransactionDialog() {
        // Маппинг: русский -> английский код
        final String[] typesRu = {"Выберите тип операции", "Пополнение", "Снятие", "Покупка", "Продажа", "Переоценка", "Дивиденд"};
        final String[] typesEn = {"", "deposit", "withdrawal", "buy", "sell", "revaluation", "dividend"};

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_transaction, null);
        Spinner typeSpinner = dialogView.findViewById(R.id.spinnerTransactionType);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, typesRu);
        typeSpinner.setAdapter(adapter);
        final com.google.android.material.textfield.TextInputEditText editAmount = dialogView.findViewById(R.id.editTextAmount);
        final com.google.android.material.textfield.TextInputEditText editDescription = dialogView.findViewById(R.id.editTextDescription);
        final com.google.android.material.textfield.TextInputEditText editDate = dialogView.findViewById(R.id.editTextDate);

        // Устанавливаем текущую дату по умолчанию
        SimpleDateFormat displayFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
        String currentDate = displayFormat.format(new Date());
        editDate.setText(currentDate);

        // Обработчик клика по полю даты
        editDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            
            // Показываем DatePickerDialog
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    
                    // После выбора даты показываем TimePickerDialog
                    TimePickerDialog timePickerDialog = new TimePickerDialog(
                        this,
                        (timeView, hourOfDay, minute) -> {
                            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                            calendar.set(Calendar.MINUTE, minute);
                            calendar.set(Calendar.SECOND, 0);
                            calendar.set(Calendar.MILLISECOND, 0);
                            
                            // Обновляем отображаемую дату
                            String selectedDate = displayFormat.format(calendar.getTime());
                            editDate.setText(selectedDate);
                        },
                        calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE),
                        true
                    );
                    timePickerDialog.show();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });

        // Удаляю Spinner валюты и всю логику, связанную с currencySpinner, currencyCodes, supportedCurrencies, currencyAdapter, getSupportedCurrencies, выбор валюты и req.currency.
        // Оставляю только выбор типа, суммы, описания, даты.
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setPositiveButton("Добавить", (dialog, which) -> {
                    int selectedIdx = typeSpinner.getSelectedItemPosition();
                    if (selectedIdx == 0) {
                        Toast.makeText(this, "Пожалуйста, выберите тип операции", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String type = typesEn[selectedIdx];
                    String amountStr = editAmount.getText() != null ? editAmount.getText().toString() : "";
                    String description = editDescription.getText() != null ? editDescription.getText().toString() : "";
                    String dateStr = editDate.getText() != null ? editDate.getText().toString() : "";
                    if (amountStr.isEmpty()) {
                        Toast.makeText(this, "Введите сумму", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    double amount = 0.0;
                    try {
                        amount = Double.parseDouble(amountStr.replace(",", ".").replace("$", ""));
                    } catch (Exception e) {
                        Toast.makeText(this, "Некорректная сумма", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String timestamp = null;
                    if (!dateStr.isEmpty()) {
                        try {
                            Date selectedDate = displayFormat.parse(dateStr);
                            timestamp = isoFormat.format(selectedDate);
                        } catch (Exception e) {
                            timestamp = isoFormat.format(new Date());
                        }
                    }
                    CreateTransactionRequest req = new CreateTransactionRequest();
                    req.amount = amount;
                    req.type = type;
                    req.description = description;
                    req.timestamp = timestamp;
                    if (currentAsset != null && currentAsset.getCurrency() != null) {
                        req.currency = currentAsset.getCurrency();
                    }
                    AssetApiService api = ApiClient.getClient().create(AssetApiService.class);
                    api.createTransaction(token, assetId, req).enqueue(new retrofit2.Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            if (response.isSuccessful()) {
                                loadAssetData();
                                Toast.makeText(AssetLobbyActivity.this, "Операция добавлена", Toast.LENGTH_SHORT).show();
                            } else {
                                String errorBody = "";
                                try {
                                    if (response.errorBody() != null) {
                                        errorBody = response.errorBody().string();
                                    }
                                } catch (Exception e) {
                                    errorBody = "Не удалось прочитать тело ошибки";
                                }
                                Log.e("AssetLobby", "Ошибка добавления операции. HTTP: " + response.code() +
                                        ", URL: " + call.request().url() +
                                        ", Error: " + errorBody);
                                Toast.makeText(AssetLobbyActivity.this, "Ошибка добавления операции", Toast.LENGTH_SHORT).show();
                            }
                        }
                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                            Toast.makeText(AssetLobbyActivity.this, "Ошибка сети", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Отмена", (dialog, which) -> dialog.cancel())
                .show();
    }
}
