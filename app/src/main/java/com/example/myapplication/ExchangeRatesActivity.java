package com.example.myapplication;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ExchangeRatesActivity extends AppCompatActivity {
    private TableLayout tableExchangeRates;
    private Spinner spinnerFrom;
    private Spinner spinnerTo;
    private String baseCurrency = "USD";
    private String token;
    private List<SupportedCurrency> supportedCurrencies = new ArrayList<>();
    private ArrayAdapter<String> fromAdapter;
    private ArrayAdapter<String> toAdapter;
    private String selectedFrom = null;
    private String selectedTo = null;
    private final String ANY = "— Любая —";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exchange_rates);
        setTitle("Курсы валют");

        tableExchangeRates = findViewById(R.id.tableExchangeRates);
        spinnerFrom = findViewById(R.id.spinnerFromCurrency);
        spinnerTo = findViewById(R.id.spinnerToCurrency);

        SharedPreferences prefs = getSharedPreferences("auth", MODE_PRIVATE);
        String rawToken = prefs.getString("token", null);
        if (rawToken == null) {
            Toast.makeText(this, "Требуется вход", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        token = "Bearer " + rawToken;

        fetchUserAndCurrencies();
    }

    private void fetchUserAndCurrencies() {
        AssetApiService api = ApiClient.getClient().create(AssetApiService.class);
        api.getMe(token).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    baseCurrency = response.body().base_currency != null ? response.body().base_currency : "USD";
                }
                fetchSupportedCurrencies();
            }
            @Override
            public void onFailure(Call<User> call, Throwable t) {
                fetchSupportedCurrencies();
            }
        });
    }

    private void fetchSupportedCurrencies() {
        AssetApiService api = ApiClient.getClient().create(AssetApiService.class);
        api.getSupportedCurrencies(token).enqueue(new Callback<List<SupportedCurrency>>() {
            @Override
            public void onResponse(Call<List<SupportedCurrency>> call, Response<List<SupportedCurrency>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    supportedCurrencies.clear();
                    supportedCurrencies.addAll(response.body());
                    setupSpinners();
                } else {
                    Toast.makeText(ExchangeRatesActivity.this, "Ошибка загрузки валют", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<List<SupportedCurrency>> call, Throwable t) {
                Toast.makeText(ExchangeRatesActivity.this, "Ошибка сети", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupSpinners() {
        List<String> codes = new ArrayList<>();
        codes.add(ANY);
        for (SupportedCurrency c : supportedCurrencies) {
            codes.add(c.code);
        }
        fromAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, codes);
        toAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, codes);
        spinnerFrom.setAdapter(fromAdapter);
        spinnerTo.setAdapter(toAdapter);

        // По умолчанию USD/EUR если есть, иначе первые
        int usdIdx = codes.indexOf("USD");
        int eurIdx = codes.indexOf("EUR");
        spinnerFrom.setSelection(usdIdx >= 0 ? usdIdx : 0);
        spinnerTo.setSelection(eurIdx >= 0 ? eurIdx : (codes.size() > 2 ? 2 : 0));
        selectedFrom = spinnerFrom.getSelectedItem().toString();
        selectedTo = spinnerTo.getSelectedItem().toString();

        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedFrom = spinnerFrom.getSelectedItem().toString();
                selectedTo = spinnerTo.getSelectedItem().toString();
                updateExchangeRatesTable();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };
        spinnerFrom.setOnItemSelectedListener(listener);
        spinnerTo.setOnItemSelectedListener(listener);
        updateExchangeRatesTable();
    }

    private void updateExchangeRatesTable() {
        // Удаляем все строки, кроме заголовка
        tableExchangeRates.post(() -> {
            while (tableExchangeRates.getChildCount() > 1) {
                tableExchangeRates.removeViewAt(1);
            }
        });
        // Если выбраны одинаковые валюты (и не ANY) — ничего не показываем
        if (!selectedFrom.equals(ANY) && selectedFrom.equals(selectedTo)) return;
        // Оба ANY — все пары
        if (selectedFrom.equals(ANY) && selectedTo.equals(ANY)) {
            for (SupportedCurrency from : supportedCurrencies) {
                for (SupportedCurrency to : supportedCurrencies) {
                    if (from.code.equals(to.code)) continue;
                    fetchAndAddRate(from.code, to.code);
                }
            }
            return;
        }
        // Только from выбран
        if (!selectedFrom.equals(ANY) && selectedTo.equals(ANY)) {
            for (SupportedCurrency to : supportedCurrencies) {
                if (selectedFrom.equals(to.code)) continue;
                fetchAndAddRate(selectedFrom, to.code);
            }
            return;
        }
        // Только to выбран
        if (selectedFrom.equals(ANY) && !selectedTo.equals(ANY)) {
            for (SupportedCurrency from : supportedCurrencies) {
                if (from.code.equals(selectedTo)) continue;
                fetchAndAddRate(from.code, selectedTo);
            }
            return;
        }
        // Оба выбраны — только одна пара
        if (!selectedFrom.equals(ANY) && !selectedTo.equals(ANY)) {
            fetchAndAddRate(selectedFrom, selectedTo);
        }
    }

    private void fetchAndAddRate(String from, String to) {
        AssetApiService api = ApiClient.getClient().create(AssetApiService.class);
        api.getExchangeRate(token, from, to).enqueue(new Callback<ExchangeRateResponse>() {
            @Override
            public void onResponse(Call<ExchangeRateResponse> call, Response<ExchangeRateResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ExchangeRateResponse rate = response.body();
                    runOnUiThread(() -> addRateRow(rate.from_currency, rate.to_currency, rate.rate));
                }
            }
            @Override
            public void onFailure(Call<ExchangeRateResponse> call, Throwable t) {
                Log.e("ExchangeRates", "Ошибка получения курса", t);
            }
        });
    }

    private void addRateRow(String from, String to, double rate) {
        TableRow row = new TableRow(this);
        TextView tvFrom = new TextView(this);
        TextView tvTo = new TextView(this);
        TextView tvRate = new TextView(this);
        tvFrom.setText(from);
        tvTo.setText(to);
        tvRate.setText(String.valueOf(rate));
        int pad = (int) (8 * getResources().getDisplayMetrics().density);
        tvFrom.setPadding(pad, pad, pad, pad);
        tvTo.setPadding(pad, pad, pad, pad);
        tvRate.setPadding(pad, pad, pad, pad);
        tvFrom.setTextColor(getResources().getColor(R.color.text_primary));
        tvTo.setTextColor(getResources().getColor(R.color.text_primary));
        tvRate.setTextColor(getResources().getColor(R.color.text_primary));
        tvFrom.setTextSize(16);
        tvTo.setTextSize(16);
        tvRate.setTextSize(16);
        row.addView(tvFrom);
        row.addView(tvTo);
        row.addView(tvRate);
        tableExchangeRates.addView(row);
    }
} 