package com.expensemanager.controller;

import com.expensemanager.database.DataBaseManager;
import com.expensemanager.util.PasswordUtils;
import com.expensemanager.util.UserSession;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LoginController {
    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private TextField passwordTextField;

    @FXML
    private Button togglePasswordBtn;

    @FXML
    private Label errorLabel;

    @FXML
    private ImageView logoImage;

    private boolean isPasswordVisible = false;

    @FXML
    public void initialize() {
        // Đồng bộ dữ liệu giữa 2 field
        passwordTextField.textProperty().bindBidirectional(passwordField.textProperty());
        togglePasswordBtn.setOnAction(e -> togglePasswordVisibility());
        java.net.URL imgUrl = getClass().getResource("/com/expensemanager/login_logo.png");
        System.out.println("Logo image URL: " + imgUrl);
        if (imgUrl != null) {
            logoImage.setImage(new Image(imgUrl.toExternalForm()));
        } else {
            logoImage.setImage(null);
        }
    }

    private void togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible;
        passwordTextField.setVisible(isPasswordVisible);
        passwordTextField.setManaged(isPasswordVisible);
        passwordField.setVisible(!isPasswordVisible);
        passwordField.setManaged(!isPasswordVisible);
        togglePasswordBtn.setText(isPasswordVisible ? "🙈" : "👁");
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = isPasswordVisible ? passwordTextField.getText().trim() : passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Vui lòng nhập đầy đủ thông tin!");
            return;
        }

        try {
            if (authenticateUser(username, password)) {
                loadMainApplication();
            } else {
                showError("Tên đăng nhập hoặc mật khẩu không đúng!");
            }
        } catch (SQLException e) {
            logger.error("Lỗi khi đăng nhập", e);
            showError("Có lỗi xảy ra, vui lòng thử lại sau!");
        }
    }

    @FXML
    private void handleRegister() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/expensemanager/RegisterView.fxml"));
            Scene scene = new Scene(loader.load());
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(scene);
        } catch (IOException e) {
            logger.error("Lỗi khi mở form đăng ký", e);
            showError("Không thể mở form đăng ký!");
        }
    }

    private boolean authenticateUser(String username, String password) throws SQLException {
        String sql = "SELECT id, password_hash, salt, role FROM users WHERE username = ?";
        
        try (Connection conn = DataBaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String storedHash = rs.getString("password_hash");
                String salt = rs.getString("salt");
                boolean isValid = PasswordUtils.verifyPassword(password, storedHash, salt);
                
                if (isValid) {
                    // Lưu user_id và role vào biến static để sử dụng trong ứng dụng
                    UserSession.setCurrentUserId(rs.getInt("id"));
                    String role = rs.getString("role");
                    UserSession.setCurrentUserRole(role != null ? role : "user");
                    UserSession.setCurrentUsername(username);
                    return true;
                }
            }
        }
        return false;
    }

    private void loadMainApplication() {
        try {
            if ("admin".equals(UserSession.getCurrentUserRole())) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/expensemanager/AdminView.fxml"));
                Scene scene = new Scene(loader.load(), 900, 600);
                Stage stage = (Stage) usernameField.getScene().getWindow();
                stage.setTitle("Quản lý chi tiêu (Admin)");
                stage.setScene(scene);
                stage.show();
            } else {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/expensemanager/HomeExpenseManagerView.fxml"));
                Scene scene = new Scene(loader.load(), 800, 600);
                Stage stage = (Stage) usernameField.getScene().getWindow();
                stage.setTitle("Quản lý chi tiêu");
                stage.setScene(scene);
                stage.show();
            }
        } catch (IOException e) {
            logger.error("Lỗi khi mở ứng dụng chính", e);
            showError("Không thể mở ứng dụng!");
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
} 