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

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getProfit() {
        return profit;
    }

    public void setProfit(String profit) {
        this.profit = profit;
    }

    public String getXIRR() {
        return XIRR;
    }

    public void setXIRR(String xirr) {
        this.XIRR = xirr;
    }
}
