package com.expensemanager.model;

public class Expense {
    private int id;
    private int userId;
    private final double amount;
    private final String category;
    private final String date;

    public Expense(int id, int userId, double amount, String category, String date) {
        this.id = id;
        this.userId = userId;
        this.amount = amount;
        this.category = category;
        this.date = date;
    }

    public Expense(double amount, String category, String date) {
        this(0, 0, amount, category, date);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public double getAmount() {
        return amount;
    }

    public String getCategory() {
        return category;
    }

    public String getDate() {
        return date;
    }
} 