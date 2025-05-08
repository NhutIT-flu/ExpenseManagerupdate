package com.expensemanager.database;

import com.expensemanager.model.Expense;
import com.expensemanager.util.UserSession;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ExpenseDAO {
    public static void addExpense(Expense expense) throws SQLException {
        String sql = "INSERT INTO expenses (user_id, amount, category, date) VALUES (?, ?, ?, ?)";
        try (Connection conn = DataBaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, expense.getUserId());
            pstmt.setDouble(2, expense.getAmount());
            pstmt.setString(3, expense.getCategory());
            pstmt.setString(4, expense.getDate());
            pstmt.executeUpdate();
        }
    }

    public static void addExpenseList(List<Expense> expenses) throws SQLException {
        String sql = "INSERT INTO expenses (user_id, amount, category, date) VALUES (?, ?, ?, ?)";
        try (Connection conn = DataBaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (Expense expense : expenses) {
                pstmt.setInt(1, expense.getUserId());
                pstmt.setDouble(2, expense.getAmount());
                pstmt.setString(3, expense.getCategory());
                pstmt.setString(4, expense.getDate());
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }
    }

    public static List<Expense> getExpensesByDate(String date) throws SQLException {
        String sql = "SELECT * FROM expenses WHERE date = ? AND user_id = ?";
        return getExpenses(sql, date);
    }

    public static List<Expense> getExpensesByDateRange(String startDate, String endDate) throws SQLException {
        String sql = "SELECT * FROM expenses WHERE date BETWEEN ? AND ? AND user_id = ?";
        return getExpenses(sql, startDate, endDate);
    }

    public static List<Expense> getExpensesByCategory(String category) throws SQLException {
        String sql = "SELECT * FROM expenses WHERE category = ? AND user_id = ?";
        return getExpenses(sql, category);
    }

    public static void deleteExpenseById(int id) throws SQLException {
        String sql = "DELETE FROM expenses WHERE id = ? AND user_id = ?";
        
        try (Connection conn = DataBaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            pstmt.setInt(2, UserSession.getCurrentUserId());
            pstmt.executeUpdate();
        }
    }

    public static void deleteAllExpenses() throws SQLException {
        String sql = "DELETE FROM expenses WHERE user_id = ?";
        
        try (Connection conn = DataBaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, UserSession.getCurrentUserId());
            pstmt.executeUpdate();
        }
    }

    public static int countExpenses() throws SQLException {
        String sql = "SELECT COUNT(*) FROM expenses WHERE user_id = ?";
        
        try (Connection conn = DataBaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, UserSession.getCurrentUserId());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    private static List<Expense> getExpenses(String sql, String... params) throws SQLException {
        List<Expense> expenses = new ArrayList<>();
        
        try (Connection conn = DataBaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            for (int i = 0; i < params.length; i++) {
                pstmt.setString(i + 1, params[i]);
            }
            pstmt.setInt(params.length + 1, UserSession.getCurrentUserId());
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    expenses.add(new Expense(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getDouble("amount"),
                        rs.getString("category"),
                        rs.getString("date")
                    ));
                }
            }
        }
        return expenses;
    }
} 