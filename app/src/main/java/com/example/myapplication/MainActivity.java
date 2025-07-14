package com.example.myapplication;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.example.myapplication.Models.Asset;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.View;
import android.view.animation.AnimationUtils;

import androidx.navigation.ui.AppBarConfiguration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.databinding.ActivityMainBinding;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Body;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.Callback;
import retrofit2.Response;

import android.widget.Spinner;
import android.widget.ArrayAdapter;

import com.example.myapplication.User;
import com.example.myapplication.ExchangeRateResponse;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    private ActivityResultLauncher<Intent> resultLauncher;
    private MyAdapter adapter;
    private List<Asset> assetList = new ArrayList<>();
    private User currentUser;
    private String baseCurrency;
    private java.util.Map<String, Double> exchangeRates = new java.util.HashMap<>(); // from_currency -> rate to base
    private boolean assetsLoaded = false;
    private int exchangeRatesToLoad = 0;
    private int exchangeRatesLoaded = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        SharedPreferences prefs = getSharedPreferences("auth", MODE_PRIVATE);
        String rawToken = prefs.getString("token", null);
        if (rawToken == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        final String token = "Bearer " + rawToken;

        setSupportActionBar(binding.toolbar);

//        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
//        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
//        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

//        binding.buttonAddAsset.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAnchorView(R.id.buttonAddAsset)
//                        .setAction("Action", null).show();
//            }
//        });

        TextView totalAssetsSum = findViewById(R.id.textViewAssetsSum);

        RecyclerView recyclerView = findViewById(R.id.recyclerViewAssets);

        assetList.add(new Asset(getString(R.string.broker_account_1), "type1", 100.0));

        assetList.add(new Asset(getString(R.string.broker_account_2), "type2", 200.0));

        recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));

        // Создаём обработчик для получения данных из нового Activity
        resultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        String assetId = result.getData().getStringExtra("assetId");
                        double newBalance = result.getData().getDoubleExtra("assetBalance", 0.0);
                        String newName = result.getData().getStringExtra("assetName");
                        
                        // Получаем обновленные значения функции
                        Double newXirr = result.getData().hasExtra("assetXirr") ? 
                            result.getData().getDoubleExtra("assetXirr", 0.0) : null;
                        Double newApy = result.getData().hasExtra("assetApy") ? 
                            result.getData().getDoubleExtra("assetApy", 0.0) : null;
                        Double newApr = result.getData().hasExtra("assetApr") ? 
                            result.getData().getDoubleExtra("assetApr", 0.0) : null;
                        Double newProfit = result.getData().hasExtra("assetProfit") ? 
                            result.getData().getDoubleExtra("assetProfit", 0.0) : null;
                        
                        // Обновляем только измененный ассет локально
                        for (int i = 0; i < assetList.size(); i++) {
                            Asset asset = assetList.get(i);
                            if (asset.getId() != null && asset.getId().equals(assetId)) {
                                asset.setBalance(newBalance);
                                asset.setName(newName);
                                if (newXirr != null) asset.setXirr(newXirr);
                                if (newApy != null) asset.setApy(newApy);
                                if (newApr != null) asset.setApr(newApr);
                                if (newProfit != null) asset.setProfit(newProfit);
                                adapter.notifyItemChanged(i);
                                break;
                            }
                        }
                        adapter.updateTotalSum();
                        updateTotalSumTextView();
                    }
                }
        );

        adapter = new MyAdapter(assetList, new MyAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                // Действия при клике на элемент
                Asset asset = assetList.get(position);

                Intent intent = new Intent(MainActivity.this, AssetLobbyActivity.class);

                intent.putExtra("position", position);
                intent.putExtra("assetId", asset.getId());
                intent.putExtra("assetName", asset.getName());
                intent.putExtra("assetBalance", asset.getBalance());
                intent.putExtra("assetType", asset.getType());
                //intent.putExtra("assetProfit", asset.getProfit());

                resultLauncher.launch(intent);
            }
        });

        // Устанавливаем слушатель для обновления метрик портфеля
        adapter.setOnPortfolioUpdateListener(new MyAdapter.OnPortfolioUpdateListener() {
            @Override
            public void onPortfolioUpdated() {
                updateTotalSumTextView();
            }
        });
        recyclerView.setAdapter(adapter);
        
        // Инициализируем состояние пустого списка
        updateEmptyState();

        // Добавляем обработку долгого нажатия для удаления ассета
        adapter.setOnItemLongClickListener(position -> {
            Asset asset = assetList.get(position);
            new AlertDialog.Builder(MainActivity.this)
                .setTitle("Удалить актив?")
                .setMessage("Вы действительно хотите удалить актив \"" + asset.getName() + "\"?")
                .setPositiveButton("Удалить", (dialog, which) -> {
                    if (rawToken != null) {
                        AssetApiService api = ApiClient.getClient().create(AssetApiService.class);
                        api.deleteAsset(token, asset.getId()).enqueue(new Callback<Void>() {
                            @Override
                            public void onResponse(Call<Void> call, Response<Void> response) {
                                if (response.isSuccessful()) {
                                    assetList.remove(position);
                                    adapter.notifyItemRemoved(position);
                                    adapter.updateTotalSum();
                                    updateTotalSumTextView();
                                    updateEmptyState();
                                    Toast.makeText(MainActivity.this, "Актив удалён", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(MainActivity.this, "Ошибка удаления", Toast.LENGTH_SHORT).show();
                                }
                            }
                            @Override
                            public void onFailure(Call<Void> call, Throwable t) {
                                Toast.makeText(MainActivity.this, "Ошибка сети", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
        });

        // Загрузка активов из API
        AssetApiService api = ApiClient.getClient().create(AssetApiService.class);
        api.getAssets(token).enqueue(new Callback<List<Asset>>() {
            @Override
            public void onResponse(Call<List<Asset>> call, Response<List<Asset>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    assetList.clear();
                    assetList.addAll(response.body());
                    assetsLoaded = true;
                    Log.d("DEBUG", "Активы загружены, курсы ещё не все получены: " + (exchangeRatesToLoad - exchangeRatesLoaded));
                    
                    // Удалено: добавление тестовых данных
                    // if (assetList.isEmpty()) {
                    //     Asset testAsset1 = new Asset("Тестовый актив 1", "stock", 10000.0);
                    //     testAsset1.setXirr(15.5);
                    //     testAsset1.setApy(12.3);
                    //     testAsset1.setApr(10.8);
                    //     testAsset1.setProfit(1500.0);
                    //     assetList.add(testAsset1);
                    //     Asset testAsset2 = new Asset("Тестовый актив 2", "bond", 5000.0);
                    //     testAsset2.setXirr(8.2);
                    //     testAsset2.setApy(7.1);
                    //     testAsset2.setApr(6.5);
                    //     testAsset2.setProfit(300.0);
                    //     assetList.add(testAsset2);
                    // }
                    
                    adapter.notifyDataSetChanged();
                    adapter.updateTotalSum();
                    fetchUserAndRecalculateAssetsSum(() -> updateTotalSumTextView());
                    updateEmptyState();
                    Log.d("MainActivity", "Активы загружены: " + assetList.size() + " шт.");
                    
                    // Принудительно обновляем метрики портфеля
                    updatePortfolioMetrics();
                } else {
                    // Обработка 401: невалидный или истекший токен
                    if (response.code() == 401) {
                        SharedPreferences prefs = getSharedPreferences("auth", MODE_PRIVATE);
                        prefs.edit().remove("token").apply();
                        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                        startActivity(intent);
                        finish();
                        return;
                    }
                    String errorBody = "";
                    try {
                        if (response.errorBody() != null) {
                            errorBody = response.errorBody().string();
                        }
                    } catch (Exception e) {
                        errorBody = "Не удалось прочитать тело ошибки";
                    }
                    Log.e("MainActivity", "Ошибка загрузки активов. HTTP: " + response.code() + 
                          ", URL: " + call.request().url() + 
                          ", Error: " + errorBody);
                    Toast.makeText(MainActivity.this, "Ошибка загрузки активов", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Asset>> call, Throwable t) {
                Log.e("MainActivity", "Ошибка сети при загрузке активов", t);
                Toast.makeText(MainActivity.this, "Ошибка сети", Toast.LENGTH_SHORT).show();
                
                // Показываем пустое состояние при ошибке сети
                updateEmptyState();
            }
        });

        binding.buttonAddAsset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showInputDialog(adapter, api, token, assetList);
            }
        });

        // Добавляем обработчик клика для карточки портфеля
        binding.portfolioCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Показываем детальную информацию о портфеле
                showPortfolioDetails();
            }
        });

        Spinner spinnerSort = findViewById(R.id.spinnerSortAssets);
        String[] sortOptions = {"Без сортировки", "Баланс по возрастанию", "Баланс по убыванию"};
        ArrayAdapter<String> sortAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, sortOptions);
        spinnerSort.setAdapter(sortAdapter);
        spinnerSort.setSelection(0);
        spinnerSort.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    // Без сортировки — можно восстановить исходный порядок, если нужно
                    // Пока ничего не делаем
                    adapter.notifyDataSetChanged();
                } else if (position == 1) {
                    // Баланс по возрастанию
                    assetList.sort(java.util.Comparator.comparingDouble(Asset::getBalance));
                    adapter.notifyDataSetChanged();
                } else if (position == 2) {
                    // Баланс по убыванию
                    assetList.sort((a, b) -> Double.compare(b.getBalance(), a.getBalance()));
                    adapter.notifyDataSetChanged();
                }
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    // Метод для обновления данных в адаптере
    private void updateItemInAdapter(Asset asset, int position) {
        if (position != -1) {
            // Обновляем данные в адаптере
            MyAdapter adapter = (MyAdapter) ((RecyclerView) findViewById(R.id.recyclerViewAssets)).getAdapter();
            if (adapter != null) {
                adapter.updateAsset(position, asset);
            }
        }
    }

    private void fetchUserAndRecalculateAssetsSum(final Runnable onDone) {
        AssetApiService api = ApiClient.getClient().create(AssetApiService.class);
        SharedPreferences prefs = getSharedPreferences("auth", MODE_PRIVATE);
        String rawToken = prefs.getString("token", null);
        if (rawToken == null) return;
        final String token = "Bearer " + rawToken;
        api.getMe(token).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentUser = response.body();
                    baseCurrency = currentUser.base_currency != null ? currentUser.base_currency : "USD";
                    recalculateAssetsSum(token, onDone);
                }
            }
            @Override
            public void onFailure(Call<User> call, Throwable t) {
                baseCurrency = "USD";
                recalculateAssetsSum(token, onDone);
            }
        });
    }
    private void recalculateAssetsSum(String token, final Runnable onDone) {
        // Собираем уникальные валюты, которых нет в кэше
        java.util.Set<String> currencies = new java.util.HashSet<>();
        for (Asset asset : assetList) {
            String cur = asset.getCurrency();
            if (cur != null && !cur.equals(baseCurrency) && !exchangeRates.containsKey(cur)) {
                currencies.add(cur);
            }
        }
        exchangeRatesToLoad = currencies.size();
        exchangeRatesLoaded = 0;
        if (currencies.isEmpty()) {
            if (onDone != null) onDone.run();
            return;
        }
        // Для каждой валюты делаем запрос курса
        final int[] remaining = {currencies.size()};
        for (String from : currencies) {
            Log.d("EXCHANGE_RATE", "Параметры запроса: from=" + from + ", to=" + baseCurrency);
            if (from == null || from.isEmpty() || baseCurrency == null || baseCurrency.isEmpty()) {
                Log.e("EXCHANGE_RATE", "ОШИБКА: пустые параметры from=" + from + ", to=" + baseCurrency + ". Курс не будет запрошен.");
                remaining[0]--;
                if (remaining[0] == 0 && onDone != null) onDone.run();
                continue;
            }
            Log.d("EXCHANGE_RATE", "Запрос курса: " + from + " -> " + baseCurrency);
            AssetApiService api = ApiClient.getClient().create(AssetApiService.class);
            api.getExchangeRate(token, from, baseCurrency).enqueue(new Callback<ExchangeRateResponse>() {
                @Override
                public void onResponse(Call<ExchangeRateResponse> call, Response<ExchangeRateResponse> response) {
                    Log.d("EXCHANGE_RATE", "Ответ курса: " + from + " -> " + baseCurrency + ", HTTP: " + response.code() + ", body: " + (response.body() != null ? response.body().rate : "null"));
                    exchangeRatesLoaded++;
                    Log.d("DEBUG", "Курс получен, осталось: " + (exchangeRatesToLoad - exchangeRatesLoaded));
                    if (response.isSuccessful() && response.body() != null) {
                        exchangeRates.put(from, response.body().rate);
                    } else {
                        exchangeRates.put(from, null);
                    }
                    remaining[0]--;
                    if (remaining[0] == 0 && onDone != null) onDone.run();
                }
                @Override
                public void onFailure(Call<ExchangeRateResponse> call, Throwable t) {
                    Log.e("EXCHANGE_RATE", "Ошибка запроса курса: " + from + " -> " + baseCurrency, t);
                    exchangeRatesLoaded++;
                    Log.d("DEBUG", "Курс получен (ошибка), осталось: " + (exchangeRatesToLoad - exchangeRatesLoaded));
                    exchangeRates.put(from, null);
                    remaining[0]--;
                    if (remaining[0] == 0 && onDone != null) onDone.run();
                }
            });
        }
    }
    private void updateTotalSumTextView() {
        SharedPreferences prefs = getSharedPreferences("auth", MODE_PRIVATE);
        String rawToken = prefs.getString("token", null);
        if (rawToken == null) return;
        final String token = "Bearer " + rawToken;
        if (baseCurrency == null) {
            fetchUserAndRecalculateAssetsSum(() -> updateTotalSumTextView());
            return;
        }
        double totalSum = 0.0;
        for (Asset asset : assetList) {
            double value = asset.getBalance();
            String assetCur = asset.getCurrency();
            if (assetCur != null && !assetCur.equals(baseCurrency)) {
                Double rate = exchangeRates.get(assetCur);
                if (rate != null) {
                    value = value * rate;
                }
            }
            totalSum += value;
        }
        TextView totalAssetsSum = findViewById(R.id.textViewAssetsSum);
        String symbol = getCurrencySymbol(baseCurrency);
        totalAssetsSum.setText(symbol + String.format("%,.0f", totalSum));
        // Удалено: отображение курсов
        // Обновляем общую прибыль и значения функции для портфеля
        updatePortfolioMetrics();
        // Управляем состоянием пустого списка
        updateEmptyState();
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

    private void updateEmptyState() {
        View emptyStateContainer = findViewById(R.id.emptyStateContainer);
        RecyclerView recyclerView = findViewById(R.id.recyclerViewAssets);
        
        if (assetList.isEmpty()) {
            emptyStateContainer.setVisibility(View.VISIBLE);
            emptyStateContainer.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyStateContainer.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void updatePortfolioMetrics() {
        if (assetList == null || assetList.isEmpty()) {
            Log.d("PortfolioMetrics", "AssetList is null or empty");
            return;
        }

        Log.d("PortfolioMetrics", "Updating portfolio metrics for " + assetList.size() + " assets");

        // Рассчитываем общую прибыль
        double totalProfit = 0.0;
        double totalBalance = 0.0;
        double totalXirr = 0.0;
        double totalApy = 0.0;
        double totalApr = 0.0;
        int assetsWithXirr = 0;
        int assetsWithApy = 0;
        int assetsWithApr = 0;

        for (Asset asset : assetList) {
            totalBalance += asset.getBalance();
            
            // Суммируем прибыль
            if (asset.getProfit() != null) {
                totalProfit += asset.getProfit();
            }
            
            // Суммируем значения функции (взвешенно по балансу)
            if (asset.getXirr() != null) {
                totalXirr += asset.getXirr() * asset.getBalance();
                assetsWithXirr++;
                Log.d("PortfolioMetrics", "Asset " + asset.getName() + " XIRR: " + asset.getXirr());
            }
            if (asset.getApy() != null) {
                totalApy += asset.getApy() * asset.getBalance();
                assetsWithApy++;
                Log.d("PortfolioMetrics", "Asset " + asset.getName() + " APY: " + asset.getApy());
            }
            if (asset.getApr() != null) {
                totalApr += asset.getApr() * asset.getBalance();
                assetsWithApr++;
                Log.d("PortfolioMetrics", "Asset " + asset.getName() + " APR: " + asset.getApr());
            }
        }

        // Обновляем отображение общей прибыли
        TextView portfolioProfitView = findViewById(R.id.textViewPortfolioProfit);
        String profitText = "Прибыль: " + (totalProfit >= 0 ? "+" : "") + "$" + String.format("%,.0f", totalProfit);
        portfolioProfitView.setText(profitText);
        
        // Устанавливаем цвет в зависимости от прибыли
        if (totalProfit >= 0) {
            portfolioProfitView.setTextColor(getResources().getColor(R.color.positive));
        } else {
            portfolioProfitView.setTextColor(getResources().getColor(R.color.negative));
        }

        // Обновляем отображение значений функции (взвешенное среднее)
        TextView portfolioXirrView = findViewById(R.id.textViewPortfolioXirr);
        TextView portfolioApyView = findViewById(R.id.textViewPortfolioApy);
        TextView portfolioAprView = findViewById(R.id.textViewPortfolioApr);

        // XIRR
        if (assetsWithXirr > 0 && totalBalance > 0) {
            double avgXirr = totalXirr / totalBalance;
            portfolioXirrView.setText("XIRR: " + String.format("%+.1f%%", avgXirr));
            portfolioXirrView.setTextColor(getResources().getColor(R.color.metric_xirr));
            portfolioXirrView.setTypeface(null, android.graphics.Typeface.BOLD);
        } else {
            portfolioXirrView.setText("XIRR: —");
            portfolioXirrView.setTextColor(getResources().getColor(R.color.text_hint));
            portfolioXirrView.setTypeface(null, android.graphics.Typeface.NORMAL);
        }
        portfolioXirrView.setPadding(0, 8, 0, 0);

        // APY
        if (assetsWithApy > 0 && totalBalance > 0) {
            double avgApy = totalApy / totalBalance;
            portfolioApyView.setText("APY: " + String.format("%+.1f%%", avgApy));
            portfolioApyView.setTextColor(getResources().getColor(R.color.metric_apy));
            portfolioApyView.setTypeface(null, android.graphics.Typeface.BOLD);
        } else {
            portfolioApyView.setText("APY: —");
            portfolioApyView.setTextColor(getResources().getColor(R.color.text_hint));
            portfolioApyView.setTypeface(null, android.graphics.Typeface.NORMAL);
        }
        portfolioApyView.setPadding(0, 8, 0, 0);

        // APR
        if (assetsWithApr > 0 && totalBalance > 0) {
            double avgApr = totalApr / totalBalance;
            portfolioAprView.setText("APR: " + String.format("%+.1f%%", avgApr));
            portfolioAprView.setTextColor(getResources().getColor(R.color.metric_apr));
            portfolioAprView.setTypeface(null, android.graphics.Typeface.BOLD);
        } else {
            portfolioAprView.setText("APR: —");
            portfolioAprView.setTextColor(getResources().getColor(R.color.text_hint));
            portfolioAprView.setTypeface(null, android.graphics.Typeface.NORMAL);
        }
        portfolioAprView.setPadding(0, 8, 0, 0);

        // Логируем финальные значения
        Log.d("PortfolioMetrics", "Final values - Profit: " + totalProfit + 
              ", XIRR assets: " + assetsWithXirr + 
              ", APY assets: " + assetsWithApy + 
              ", APR assets: " + assetsWithApr);
        Log.d("PortfolioMetrics", "APY assets: " + assetsWithApy + ", totalApy: " + totalApy);
        Log.d("PortfolioMetrics", "APR assets: " + assetsWithApr + ", totalApr: " + totalApr);
    }

    private void showPortfolioDetails() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Детали портфеля");
        
        // Создаем layout для диалога
        LinearLayout dialogLayout = new LinearLayout(this);
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        dialogLayout.setPadding(32, 24, 32, 24);
        
        // Общая сумма активов
        TextView totalAssetsView = new TextView(this);
        totalAssetsView.setText("Общая сумма: $" + String.format("%,.0f", adapter.getTotalSum()));
        totalAssetsView.setTextSize(18);
        totalAssetsView.setTextColor(getResources().getColor(android.R.color.black));
        totalAssetsView.setPadding(0, 0, 0, 16);
        dialogLayout.addView(totalAssetsView);
        
        // Количество активов
        TextView assetsCountView = new TextView(this);
        assetsCountView.setText("Количество активов: " + assetList.size());
        assetsCountView.setTextSize(16);
        assetsCountView.setTextColor(getResources().getColor(android.R.color.darker_gray));
        assetsCountView.setPadding(0, 0, 0, 16);
        dialogLayout.addView(assetsCountView);
        
        // Средний баланс на актив
        if (!assetList.isEmpty()) {
            double avgBalance = adapter.getTotalSum() / assetList.size();
            TextView avgBalanceView = new TextView(this);
            avgBalanceView.setText("Средний баланс: $" + String.format("%,.0f", avgBalance));
            avgBalanceView.setTextSize(16);
            avgBalanceView.setTextColor(getResources().getColor(android.R.color.darker_gray));
            avgBalanceView.setPadding(0, 0, 0, 16);
            dialogLayout.addView(avgBalanceView);
        }
        
        builder.setView(dialogLayout);
        builder.setPositiveButton("Закрыть", null);
        builder.show();
    }

    private void showInputDialog(MyAdapter adapter, AssetApiService api, String token, List<Asset> assetList) {
        // Маппинг: русский -> английский код
        final String[] typesRu = {"Выберите тип актива", "Брокерский счёт", "Депозит", "Недвижимость", "Криптовалюта", "Другое"};
        final String[] typesEn = {"", "broker_account", "deposit", "real_estate", "crypto", "other"};

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_asset, null);
        Spinner typeSpinner = dialogView.findViewById(R.id.spinnerAssetType);
        ArrayAdapter<String> adapterSpinner = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, typesRu);
        typeSpinner.setAdapter(adapterSpinner);
        final com.google.android.material.textfield.TextInputEditText editName = dialogView.findViewById(R.id.editTextAssetName);
        final com.google.android.material.textfield.TextInputEditText editDescription = dialogView.findViewById(R.id.editTextAssetDescription);
        Spinner currencySpinner = dialogView.findViewById(R.id.spinnerAssetCurrency);
        List<String> currencyCodes = new ArrayList<>();
        List<SupportedCurrency> supportedCurrencies = new ArrayList<>();
        ArrayAdapter<String> currencyAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, currencyCodes);
        currencySpinner.setAdapter(currencyAdapter);
        // Загрузка валют с бэкенда
        final List<String> popularOrder = java.util.Arrays.asList("USD", "EUR", "RUB", "GBP", "JPY", "CNY", "CHF", "CAD", "AUD", "KRW");
        final java.util.Map<String, Integer> popularityMap = new java.util.HashMap<>();
        for (int i = 0; i < popularOrder.size(); i++) popularityMap.put(popularOrder.get(i), i);
        api.getSupportedCurrencies(token).enqueue(new Callback<List<SupportedCurrency>>() {
            @Override
            public void onResponse(Call<List<SupportedCurrency>> call, Response<List<SupportedCurrency>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    supportedCurrencies.clear();
                    supportedCurrencies.addAll(response.body());
                    currencyCodes.clear();
                    java.util.List<SupportedCurrency> sorted = new java.util.ArrayList<>();
                    for (SupportedCurrency c : supportedCurrencies) {
                        if (c.is_supported) sorted.add(c);
                    }
                    // Сортировка: сначала популярные, потом остальные по алфавиту
                    sorted.sort((a, b) -> {
                        Integer ia = popularityMap.get(a.code);
                        Integer ib = popularityMap.get(b.code);
                        if (ia != null && ib != null) return ia - ib;
                        if (ia != null) return -1;
                        if (ib != null) return 1;
                        return a.code.compareTo(b.code);
                    });
                    for (SupportedCurrency c : sorted) {
                        currencyCodes.add(c.code + (c.symbol != null && !c.symbol.isEmpty() ? (" (" + c.symbol + ")") : ""));
                    }
                    currencyAdapter.notifyDataSetChanged();
                    if (!currencyCodes.isEmpty()) {
                        currencySpinner.setSelection(0);
                    }
                } else {
                    if (currencyCodes.isEmpty()) {
                        currencyCodes.add("USD");
                        currencyCodes.add("EUR");
                        currencyCodes.add("RUB");
                        currencyAdapter.notifyDataSetChanged();
                    }
                }
            }
            @Override
            public void onFailure(Call<List<SupportedCurrency>> call, Throwable t) {
                if (currencyCodes.isEmpty()) {
                    currencyCodes.add("USD");
                    currencyCodes.add("EUR");
                    currencyCodes.add("RUB");
                    currencyAdapter.notifyDataSetChanged();
                }
            }
        });
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setPositiveButton("Добавить", (dialog, which) -> {
                    int selectedIdx = typeSpinner.getSelectedItemPosition();
                    if (selectedIdx == 0) {
                        Toast.makeText(this, "Пожалуйста, выберите тип актива", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String type = typesEn[selectedIdx];
                    String name = editName.getText() != null ? editName.getText().toString() : "";
                    String description = editDescription.getText() != null ? editDescription.getText().toString() : "";
                    int currencyIdx = currencySpinner.getSelectedItemPosition();
                    String currencyCode = null;
                    if (currencyIdx >= 0 && currencyIdx < currencyCodes.size()) {
                        String codeWithSymbol = currencyCodes.get(currencyIdx);
                        currencyCode = codeWithSymbol.split(" ")[0]; // Берём только код
                    }
                    if (name.isEmpty()) {
                        Toast.makeText(this, "Введите название актива", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (currencyCode == null || currencyCode.isEmpty()) {
                        Toast.makeText(this, "Пожалуйста, выберите валюту", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    CreateAssetRequest req = new CreateAssetRequest();
                    req.name = name;
                    req.type = type;
                    req.currency = currencyCode;
                    // Можно добавить описание в модель, если поддерживается бэкендом
                    api.createAsset(token, req).enqueue(new Callback<Asset>() {
                        @Override
                        public void onResponse(Call<Asset> call, Response<Asset> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                Log.d("CREATE ASSET", "Asset created: " + response.body().toString());
                                // Вместо локального добавления — обновляем список с backend
                                api.getAssets(token).enqueue(new Callback<List<Asset>>() {
                                    @Override
                                    public void onResponse(Call<List<Asset>> call, Response<List<Asset>> response) {
                                        if (response.isSuccessful() && response.body() != null) {
                                            assetList.clear();
                                            assetList.addAll(response.body());
                                            adapter.notifyDataSetChanged();
                                            adapter.updateTotalSum();
                                            updateTotalSumTextView();
                                            updateEmptyState();
                                        }
                                    }
                                    @Override
                                    public void onFailure(Call<List<Asset>> call, Throwable t) {
                                        Log.e("MainActivity", "Ошибка обновления списка активов", t);
                                        Toast.makeText(MainActivity.this, "Ошибка обновления списка активов", Toast.LENGTH_SHORT).show();
                                        updateEmptyState();
                                    }
                                });
                                Toast.makeText(MainActivity.this, "Актив добавлен", Toast.LENGTH_SHORT).show();
                            } else {
                                String errorBody = "";
                                try {
                                    if (response.errorBody() != null) {
                                        errorBody = response.errorBody().string();
                                    }
                                } catch (Exception e) {
                                    errorBody = "Не удалось прочитать тело ошибки";
                                }
                                Log.e("CREATE ASSET", "Ошибка создания актива. HTTP: " + response.code() + 
                                      ", URL: " + call.request().url() + 
                                      ", Request: name=" + req.name + ", type=" + req.type + ", currency=" + req.currency +
                                      ", Error: " + errorBody);
                                Toast.makeText(MainActivity.this, "Ошибка добавления актива", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<Asset> call, Throwable t) {
                            Log.e("CREATE ASSET", "Ошибка сети при создании актива", t);
                            Toast.makeText(MainActivity.this, "Ошибка сети", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Отмена", (dialog, which) -> dialog.cancel())
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }
        if (id == R.id.action_logout) {
            // Удаляем токен и переходим на LoginActivity
            SharedPreferences prefs = getSharedPreferences("auth", MODE_PRIVATE);
            prefs.edit().remove("token").apply();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return true;
        }
        if (id == R.id.action_exchange_rates) {
            Intent intent = new Intent(this, ExchangeRatesActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

//    @Override
//    public boolean onSupportNavigateUp() {
//        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
//        return NavigationUI.navigateUp(navController, appBarConfiguration)
//                || super.onSupportNavigateUp();
//    }

    @Override
    protected void onResume() {
        super.onResume();
        
        // Просто обновляем отображение, если адаптер существует
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        updateTotalSumTextView();
    }
}