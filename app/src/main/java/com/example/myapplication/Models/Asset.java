package com.example.myapplication.Models;

public class Asset {
    private String id;
    private String user_id;
    private String name;
    private String type;
    private double balance;
    private String created_at;
    private Double xirr;
    private Double apy;
    private Double apr;
    private Double profit;

    public Asset(String name, String type, Double balance) {
        this.name = name;
        this.type = type;
        this.balance = balance;
    }

    // Геттеры и сеттеры
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUser_id() {
        return user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public String getCreated_at() {
        return created_at;
    }

    public void setCreated_at(String created_at) {
        this.created_at = created_at;
    }

    public Double getXirr() {
        return xirr;
    }

    public void setXirr(Double xirr) {
        this.xirr = xirr;
    }

    public Double getApy() {
        return apy;
    }

    public void setApy(Double apy) {
        this.apy = apy;
    }

    public Double getApr() {
        return apr;
    }

    public void setApr(Double apr) {
        this.apr = apr;
    }

    public Double getProfit() {
        return profit;
    }

    public void setProfit(Double profit) {
        this.profit = profit;
    }

    public static String getTypeRu(String typeEn) {
        if (typeEn == null) return "Тип не задан";
        switch (typeEn) {
            case "broker_account": return "Брокерский счёт";
            case "deposit": return "Депозит";
            case "real_estate": return "Недвижимость";
            case "crypto": return "Криптовалюта";
            case "other": return "Другое";
            default: return "Тип не задан";
        }
    }
}

// public class Asset {
//     private String name;
//     private String value; // TODO тип для денег
//     private String profit;
//     private String XIRR;

//     public Asset(String name, String value, String profit, String XIRR) {
//         this.name = name;
//         this.value = value;
//         this.profit = profit;
//         this.XIRR = XIRR;
//     }

//     public String getName() {
//         return name;
//     }

//     public void setName(String name) {
//         this.name = name;
//     }

//     public String getValue() {
//         return value;
//     }

//     public void setValue(String value) {
//         this.value = value;
//     }

//     public String getProfit() {
//         return profit;
//     }

//     public void setProfit(String profit) {
//         this.profit = profit;
//     }

//     public String getXIRR() {
//         return XIRR;
//     }

//     public void setXIRR(String xirr) {
//         this.XIRR = xirr;
//     }
// }
