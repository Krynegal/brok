package com.example.myapplication;

import com.example.myapplication.Models.Asset;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface AssetApiService {
    // Регистрация
    @POST("/auth/register")
    Call<LoginResponse> register(@Body RegisterRequest body);

    // Логин
    @POST("/auth/login")
    Call<LoginResponse> login(@Body LoginRequest body);

    // Получить текущего пользователя
    @GET("/api/me")
    Call<User> getMe(@Header("Authorization") String token);

    // Получить активы пользователя
    @GET("/api/assets")
    Call<List<Asset>> getAssets(@Header("Authorization") String token);

    // Создать актив
    @POST("/api/assets")
    Call<Asset> createAsset(@Header("Authorization") String token, @Body CreateAssetRequest body);

    // Обновить актив
    @PATCH("/api/assets/{id}")
    Call<Void> updateAsset(@Header("Authorization") String token, @Path("id") String id, @Body UpdateAssetRequest body);

    // Удалить актив
    @DELETE("/api/assets/{id}")
    Call<Void> deleteAsset(@Header("Authorization") String token, @Path("id") String id);

    // Получить транзакции по активу
    @GET("/api/assets/{id}/transactions")
    Call<List<Transaction>> getAssetTransactions(@Header("Authorization") String token, @Path("id") String assetId);

    // Создать транзакцию по активу
    @POST("/api/assets/{id}/transactions")
    Call<Void> createTransaction(@Header("Authorization") String token, @Path("id") String assetId, @Body CreateTransactionRequest body);

    // Удалить транзакцию
    @DELETE("/api/transactions/{id}")
    Call<Void> deleteTransaction(@Header("Authorization") String token, @Path("id") String transactionId);

    // Получить поддерживаемые валюты
    @GET("/api/currencies")
    Call<List<SupportedCurrency>> getSupportedCurrencies(@Header("Authorization") String token);

    // Получить курс между двумя валютами (НОВЫЙ GET-эндпоинт)
    @GET("/api/exchange-rates")
    Call<ExchangeRateResponse> getExchangeRate(
        @Header("Authorization") String token,
        @Query("from_currency") String fromCurrency,
        @Query("to_currency") String toCurrency
    );
} 