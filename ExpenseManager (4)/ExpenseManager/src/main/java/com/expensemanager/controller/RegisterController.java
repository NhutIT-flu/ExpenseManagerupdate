package com.expensemanager.controller;

import com.expensemanager.database.DataBaseManager;
import com.expensemanager.util.PasswordUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class RegisterController {
    private static final Logger logger = LoggerFactory.getLogger(RegisterController.class);

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private Label errorLabel;

    @FXML
    private void handleRegister() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();
        String confirmPassword = confirmPasswordField.getText().trim();

        if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showError("Vui lòng nhập đầy đủ thông tin!");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showError("Mật khẩu xác nhận không khớp!");
            return;
        }

        if (password.length() < 6) {
            showError("Mật khẩu phải có ít nhất 6 ký tự!");
            return;
        }

        try {
            if (isUsernameExists(username)) {
                showError("Tên đăng nhập đã tồn tại!");
                return;
            }

            registerUser(username, password);
            showSuccessAndNavigateToLogin();
        } catch (SQLException e) {
            logger.error("Lỗi khi đăng ký tài khoản", e);
            showError("Có lỗi xảy ra, vui lòng thử lại sau!");
        }
    }

    @FXML
    private void handleBackToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/expensemanager/LoginView.fxml"));
            Scene scene = new Scene(loader.load());
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(scene);
        } catch (IOException e) {
            logger.error("Lỗi khi quay lại trang đăng nhập", e);
            showError("Không thể mở trang đăng nhập!");
        }
    }

    private boolean isUsernameExists(String username) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
        
        try (Connection conn = DataBaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    private void registerUser(String username, String password) throws SQLException {
        String sql = "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)";
        String salt = PasswordUtils.generateSalt();
        String hashedPassword = PasswordUtils.hashPassword(password, salt);
        
        try (Connection conn = DataBaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            pstmt.setString(2, hashedPassword);
            pstmt.setString(3, salt);
            pstmt.executeUpdate();
        }
    }

    private void showSuccessAndNavigateToLogin() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Đăng ký thành công");
        alert.setHeaderText(null);
        alert.setContentText("Tài khoản đã được tạo thành công!");
        alert.showAndWait();
        handleBackToLogin();
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
} 