package com.expensemanager.model;

import javafx.beans.property.*;

public class User {
    private final IntegerProperty id;
    private final StringProperty username;
    private final StringProperty role;

    public User(int id, String username, String role) {
        this.id = new SimpleIntegerProperty(id);
        this.username = new SimpleStringProperty(username);
        this.role = new SimpleStringProperty(role);
    }

    public int getId() { return id.get(); }
    public String getUsername() { return username.get(); }
    public String getRole() { return role.get(); }
    public StringProperty usernameProperty() { return username; }
    public StringProperty roleProperty() { return role; }
} 