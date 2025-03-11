package com.example.myapplication.Models;

public class Asset {
    private String name;
    private String value; // TODO тип для денег
    private String profit;
    private String XIRR;

    public Asset(String name, String value, String profit, String XIRR) {
        this.name = name;
        this.value = value;
        this.profit = profit;
        this.XIRR = XIRR;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public String getProfit() {
        return profit;
    }

    public String getXIRR() {
        return XIRR;
    }
}
