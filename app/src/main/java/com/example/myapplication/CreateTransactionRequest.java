package com.example.myapplication;

public class CreateTransactionRequest {
    public double amount;
    public String type;
    public String description;
    public String timestamp; // ISO-строка, например "2025-06-28T07:23:24.036Z"
    public String currency;
} 