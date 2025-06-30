package com.example.myapplication;

public class Transaction {
    public String id;
    public String asset_id;
    public double amount;
    public String type;         // "income" или "expense"
    public String description;
    public String timestamp;    // ISO-строка, например "2025-06-28T07:23:24.036Z"

    public Transaction() {}

    public Transaction(String id, String asset_id, double amount, String type, String description, String timestamp) {
        this.id = id;
        this.asset_id = asset_id;
        this.amount = amount;
        this.type = type;
        this.description = description;
        this.timestamp = timestamp;
    }
} 