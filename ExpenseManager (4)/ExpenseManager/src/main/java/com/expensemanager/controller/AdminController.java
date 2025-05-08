package com.expensemanager.controller;

import com.expensemanager.database.DataBaseManager;
import com.expensemanager.model.Expense;
import com.expensemanager.model.User;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class AdminController {
    @FXML
    private TableView<User> userTableView;
    @FXML
    private TableColumn<User, String> usernameColumn;
    @FXML
    private TableColumn<User, String> roleColumn;

    @FXML
    private TableView<Expense> expenseTableView;
    @FXML
    private TableColumn<Expense, String> dateColumn;
    @FXML
    private TableColumn<Expense, Double> amountColumn;
    @FXML
    private TableColumn<Expense, String> categoryColumn;

    @FXML
    public void initialize() {
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        roleColumn.setCellValueFactory(new PropertyValueFactory<>("role"));

        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));

        userTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldUser, newUser) -> {
            if (newUser != null) {
                loadExpensesForUser(newUser.getId());
            }
        });

        loadUsers();
    }

    private void loadUsers() {
        List<User> users = new ArrayList<>();
        try (Connection conn = DataBaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT id, username, role FROM users")) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                users.add(new User(rs.getInt("id"), rs.getString("username"), rs.getString("role")));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        userTableView.setItems(FXCollections.observableArrayList(users));
    }

    private void loadExpensesForUser(int userId) {
        List<Expense> expenses = new ArrayList<>();
        try (Connection conn = DataBaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM expenses WHERE user_id = ?")) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                expenses.add(new Expense(
                        rs.getInt("id"),
                        userId,
                        rs.getDouble("amount"),
                        rs.getString("category"),
                        rs.getString("date")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        expenseTableView.setItems(FXCollections.observableArrayList(expenses));
    }
} 