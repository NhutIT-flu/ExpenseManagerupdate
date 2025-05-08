package com.expensemanager.controller;

import com.expensemanager.database.DataBaseManager;
import com.expensemanager.model.Expense;
import com.expensemanager.util.UserSession;
import com.expensemanager.database.ExpenseDAO;
import com.expensemanager.util.AlertUtils;
import com.expensemanager.util.DateUtil;
import com.expensemanager.util.SortUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javafx.scene.layout.GridPane;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.layout.StackPane;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import com.expensemanager.util.PasswordUtils;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Map;
import java.util.TreeMap;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;

import static com.expensemanager.util.InputUtil.validateInput;
import static com.expensemanager.util.SortUtil.SortOption.*;
import javafx.scene.layout.HBox;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import javafx.animation.FadeTransition;
import javafx.scene.Node;

public class HomeController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(HomeController.class);
    @FXML
    public Menu menuFile;
    @FXML
    public Menu menuView;
    @FXML
    public Menu menuStatistics;
    @FXML
    public TableView<Expense> expenseTableView;
    @FXML
    public TableColumn<Expense, Double> amountColumn;
    @FXML
    public TableColumn<Expense, String> categoryColumn;
    @FXML
    public TableColumn<Expense, String> dateColumn;
    @FXML
    public TextField textFiel_Amount;
    @FXML
    public TextField textFiel_Category;
    public DatePicker DatePicker;
    @FXML
    private Label greetingLabel;
    @FXML
    private Button addExpenseButton;
    @FXML
    private StackPane avatarCircleModern;
    @FXML
    private Button refreshButton;
    @FXML
    private HBox inputBox;
    @FXML
    private Button saveButton;
    @FXML
    private Button cancelButton;
    @FXML
    private AreaChart<String, Number> trafficChart;
    @FXML
    private CategoryAxis trafficXAxis;
    @FXML
    private NumberAxis trafficYAxis;
    @FXML
    private Label compareLabel;

    //sắp xếp lần đầu mở ứng dụng
    private SortUtil.SortOption currentSortOption = SortUtil.SortOption.DATE_DESC;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Thiết lập các cột cho TableView
        amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));

        // Định dạng cột Số tiền: bỏ .0 nếu là số nguyên, thêm chữ Đ
        amountColumn.setCellFactory(col -> new TableCell<Expense, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    if (item == Math.floor(item)) {
                        setText(String.format("%,.0f Đ", item));
        } else {
                        setText(String.format("%,.2f Đ", item));
                    }
                }
            }
        });

        // Hiệu ứng chạy chữ cho greeting label
        String greetingText = "Xin chào, " + UserSession.getCurrentUsername();
        playGreetingTypingEffect(greetingText);

        // Tắt animation cho AreaChart để tránh co bóp khi ít dữ liệu
        trafficChart.setAnimated(false);

        // Tải dữ liệu chi tiêu
        loadExpenses();

        // Thiết lập sự kiện cho các nút và trường nhập liệu
        setupEventHandlers();

        // Ẩn inputBox khi khởi động
        inputBox.setVisible(false);
        inputBox.setManaged(false);
    }

    private void playGreetingTypingEffect(String text) {
        greetingLabel.setText("");
        greetingLabel.setVisible(true);
        Timeline timeline = new Timeline();
        for (int i = 0; i <= text.length(); i++) {
            final int idx = i;
            KeyFrame keyFrame = new KeyFrame(Duration.millis(50 * i), e -> {
                greetingLabel.setText(text.substring(0, idx));
            });
            timeline.getKeyFrames().add(keyFrame);
        }
        timeline.play();
    }

    private void loadExpenses() {
        try {
            String sql;
            boolean isAdmin = "admin".equals(UserSession.getCurrentUserRole());
            if (isAdmin) {
                sql = "SELECT * FROM expenses ORDER BY date DESC";
            } else {
                sql = "SELECT * FROM expenses WHERE user_id = ? ORDER BY date DESC";
            }
            
            try (Connection conn = DataBaseManager.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                
                if (!isAdmin) {
                    pstmt.setInt(1, UserSession.getCurrentUserId());
                }
                ResultSet rs = pstmt.executeQuery();

                List<Expense> expenses = new ArrayList<>();
                while (rs.next()) {
                    expenses.add(new Expense(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getDouble("amount"),
                        rs.getString("category"),
                        rs.getString("date")
                    ));
                }

                SortUtil.sortExpenses(expenses, currentSortOption);
                loadExpenses(expenses);

                logger.info("Đã tải {} chi tiêu", expenses.size());
            }
        } catch (SQLException e) {
            logger.error("Lỗi khi tải dữ liệu", e);
            AlertUtils.showError("Lỗi", "Không thể tải dữ liệu!");
        }
    }

    private void loadExpenses(List<Expense> expenses) {
        ObservableList<Expense> expenseList = FXCollections.observableArrayList(expenses);
        expenseTableView.setItems(expenseList);
        updateTrafficChart(expenses);
    }

    private void updateTrafficChart(List<Expense> expenses) {
        // Gom nhóm chi tiêu theo ngày
        Map<String, Double> dailyTotals = new TreeMap<>();
        for (Expense expense : expenses) {
            String date = expense.getDate();
            dailyTotals.put(date, dailyTotals.getOrDefault(date, 0.0) + expense.getAmount());
        }
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Tổng chi tiêu");
        for (Map.Entry<String, Double> entry : dailyTotals.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }
        trafficChart.getData().clear();
        trafficChart.getData().add(series);

        // Animation: biểu đồ cao lên từ từ
        animateAreaChart(series, dailyTotals);

        // Hiệu ứng fade-in cho vùng fill và line
        trafficChart.applyCss();
        trafficChart.layout();
        Node fill = trafficChart.lookup(".chart-series-area-fill");
        Node line = trafficChart.lookup(".chart-series-area-line");
        if (fill != null) {
            FadeTransition ft = new FadeTransition(Duration.millis(900), fill);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.play();
        }
        if (line != null) {
            FadeTransition ft2 = new FadeTransition(Duration.millis(900), line);
            ft2.setFromValue(0);
            ft2.setToValue(1);
            ft2.play();
        }

        // So sánh % giữa 2 ngày gần nhất
        if (dailyTotals.size() >= 2) {
            List<String> dates = new ArrayList<>(dailyTotals.keySet());
            String lastDate = dates.get(dates.size() - 1);
            String prevDate = dates.get(dates.size() - 2);
            double lastValue = dailyTotals.get(lastDate);
            double prevValue = dailyTotals.get(prevDate);
            double percent = prevValue == 0 ? 100 : ((lastValue - prevValue) / prevValue) * 100;
            String arrow, color;
            if (percent > 0) {
                arrow = "↑";
                color = "#00c853";
            } else if (percent < 0) {
                arrow = "↓";
                color = "#d50000";
            } else {
                arrow = "=";
                color = "#757575";
            }
            String text = String.format("%s %.2f%% so với %s", arrow, Math.abs(percent), prevDate);
            compareLabel.setText(text);
            compareLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
        } else {
            compareLabel.setText("");
        }
    }

    // Thêm hàm animation cho AreaChart
    private void animateAreaChart(XYChart.Series<String, Number> series, Map<String, Double> dailyTotals) {
        // Đặt tất cả giá trị về 0 ban đầu
        for (XYChart.Data<String, Number> data : series.getData()) {
            data.setYValue(0);
        }
        Timeline timeline = new Timeline();
        int steps = 30; // số bước animation
        for (int i = 1; i <= steps; i++) {
            final int step = i;
            KeyFrame kf = new KeyFrame(Duration.millis(i * 20), e -> {
                int idx = 0;
                for (Map.Entry<String, Double> entry : dailyTotals.entrySet()) {
                    double target = entry.getValue();
                    double value = target * step / steps;
                    series.getData().get(idx).setYValue(value);
                    idx++;
                }
            });
            timeline.getKeyFrames().add(kf);
        }
        timeline.play();
    }

    private void setupEventHandlers() {
        // Sự kiện cho nút +
        addExpenseButton.setOnAction(e -> showAddExpenseDialog());
        // Sự kiện cho nút Lưu
        saveButton.setOnAction(e -> handleSaveExpense());
        // Sự kiện cho nút Hủy
        cancelButton.setOnAction(e -> handleCancelInput());

        // Thêm sự kiện cho các trường nhập liệu
        textFiel_Amount.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                handleAddExpense();
            }
        });

        textFiel_Category.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                handleAddExpense();
            }
        });

        // Thêm menu ngữ cảnh cho TableView
        ContextMenu contextMenu = new ContextMenu();
        MenuItem deleteItem = new MenuItem("Xóa");
        deleteItem.setOnAction(e -> {
            Expense selectedExpense = expenseTableView.getSelectionModel().getSelectedItem();
            if (selectedExpense != null) {
                handleDeleteExpense(selectedExpense);
            }
        });
        contextMenu.getItems().add(deleteItem);
        expenseTableView.setContextMenu(contextMenu);

        // Thêm sự kiện double click cho TableView
        expenseTableView.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                Expense selectedExpense = expenseTableView.getSelectionModel().getSelectedItem();
                if (selectedExpense != null) {
                    showEditDialog(selectedExpense);
                }
            }
        });

        // Avatar menu
        ContextMenu avatarMenu = new ContextMenu();
        MenuItem editProfileItem = new MenuItem("Chỉnh sửa thông tin cá nhân");
        MenuItem changePasswordItem = new MenuItem("Đổi mật khẩu");
        MenuItem logoutItem = new MenuItem("Đăng xuất");
        avatarMenu.getItems().addAll(editProfileItem, changePasswordItem, logoutItem);
        avatarCircleModern.setOnMouseClicked(e -> {
            if (e.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                avatarMenu.show(avatarCircleModern, e.getScreenX(), e.getScreenY());
            }
        });
        editProfileItem.setOnAction(e -> showEditProfileDialog());
        changePasswordItem.setOnAction(e -> showChangePasswordDialog());
        logoutItem.setOnAction(e -> handleLogout());
    }

    private void handleAddExpense() {
        String amountStr = textFiel_Amount.getText().trim();
        String category = textFiel_Category.getText().trim();
        LocalDate date = DatePicker.getValue();

        if (amountStr.isEmpty() || category.isEmpty() || date == null) {
            AlertUtils.showError("Lỗi", "Vui lòng nhập đầy đủ thông tin!");
            return;
        }

        try {
            double amount = Double.parseDouble(amountStr);
            Expense expense = new Expense(
                0, // id sẽ được tự động tạo bởi database
                UserSession.getCurrentUserId(),
                amount,
                category,
                date.toString()
            );

            ExpenseDAO.addExpense(expense);
            loadExpenses(); // Tải lại dữ liệu

            // Xóa dữ liệu trong các trường nhập liệu
            textFiel_Amount.clear();
            textFiel_Category.clear();
            DatePicker.setValue(null);

            logger.info("Đã thêm chi tiêu mới: {} - {} - {}", amount, category, date);
        } catch (NumberFormatException e) {
            AlertUtils.showError("Lỗi", "Số tiền không hợp lệ!");
        } catch (SQLException e) {
            logger.error("Lỗi khi thêm chi tiêu", e);
            AlertUtils.showError("Lỗi", "Không thể thêm chi tiêu!");
        }
    }

    private void handleDeleteExpense(Expense expense) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Xác nhận xóa");
        alert.setHeaderText(null);
        alert.setContentText("Bạn có chắc chắn muốn xóa chi tiêu này?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                ExpenseDAO.deleteExpenseById(expense.getId());
                loadExpenses(); // Tải lại dữ liệu
                logger.info("Đã xóa chi tiêu: {}", expense.getId());
            } catch (SQLException e) {
                logger.error("Lỗi khi xóa chi tiêu", e);
                AlertUtils.showError("Lỗi", "Không thể xóa chi tiêu!");
            }
        }
    }

    private void showEditDialog(Expense expense) {
        Dialog<Expense> dialog = new Dialog<>();
        dialog.setTitle("Sửa chi tiêu");
        dialog.setHeaderText(null);

        // Tạo form nhập liệu
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

        TextField amountField = new TextField(String.valueOf(expense.getAmount()));
        TextField categoryField = new TextField(expense.getCategory());
        DatePicker datePicker = new DatePicker(LocalDate.parse(expense.getDate()));

        grid.add(new Label("Số tiền:"), 0, 0);
        grid.add(amountField, 1, 0);
        grid.add(new Label("Danh mục:"), 0, 1);
        grid.add(categoryField, 1, 1);
        grid.add(new Label("Ngày:"), 0, 2);
        grid.add(datePicker, 1, 2);

        dialog.getDialogPane().setContent(grid);

        // Thêm nút OK và Cancel
        ButtonType saveButtonType = new ButtonType("Lưu", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Xử lý kết quả
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    double amount = Double.parseDouble(amountField.getText().trim());
                    String category = categoryField.getText().trim();
                    LocalDate date = datePicker.getValue();

                    if (amount <= 0 || category.isEmpty() || date == null) {
                        AlertUtils.showError("Lỗi", "Vui lòng nhập đầy đủ thông tin!");
                        return null;
                    }

                    return new Expense(
                        expense.getId(),
                        UserSession.getCurrentUserId(),
                        amount,
                        category,
                        date.toString()
                    );
                } catch (NumberFormatException e) {
                    AlertUtils.showError("Lỗi", "Số tiền không hợp lệ!");
                }
            }
            return null;
        });

        Optional<Expense> result = dialog.showAndWait();
        result.ifPresent(updatedExpense -> {
            try {
                // Cập nhật chi tiêu trong database
                String sql = "UPDATE expenses SET amount = ?, category = ?, date = ? WHERE id = ? AND user_id = ?";
                try (Connection conn = DataBaseManager.getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setDouble(1, updatedExpense.getAmount());
                    pstmt.setString(2, updatedExpense.getCategory());
                    pstmt.setString(3, updatedExpense.getDate());
                    pstmt.setInt(4, updatedExpense.getId());
                    pstmt.setInt(5, UserSession.getCurrentUserId());
                    pstmt.executeUpdate();
                }

                loadExpenses(); // Tải lại dữ liệu
                logger.info("Đã cập nhật chi tiêu: {}", updatedExpense.getId());
            } catch (SQLException e) {
                logger.error("Lỗi khi cập nhật chi tiêu", e);
                AlertUtils.showError("Lỗi", "Không thể cập nhật chi tiêu!");
            }
        });
    }

    @FXML
    private void handleLogout() {
        try {
            UserSession.clearSession();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/expensemanager/LoginView.fxml"));
            Scene scene = new Scene(loader.load());
            Stage stage = (Stage) expenseTableView.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Quản lý chi tiêu - Đăng nhập");
        } catch (IOException e) {
            logger.error("Lỗi khi đăng xuất", e);
            AlertUtils.showError("Lỗi", "Không thể đăng xuất!");
        }
    }

    @FXML
    private void handleExportToExcel() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Lưu file Excel");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Excel Files", "*.xlsx")
        );
        fileChooser.setInitialFileName("expenses.xlsx");

        File file = fileChooser.showSaveDialog(expenseTableView.getScene().getWindow());
        if (file != null) {
            try {
                // TODO: Implement Excel export
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Thông báo");
                alert.setHeaderText(null);
                alert.setContentText("Tính năng đang được phát triển!");
                alert.showAndWait();
            } catch (Exception e) {
                logger.error("Lỗi khi xuất file Excel", e);
                AlertUtils.showError("Lỗi", "Không thể xuất file Excel!");
            }
        }
    }

    @FXML
    private void handleImportFromExcel() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn file Excel");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Excel Files", "*.xlsx")
        );

        File file = fileChooser.showOpenDialog(expenseTableView.getScene().getWindow());
        if (file != null) {
            try {
                // TODO: Implement Excel import
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Thông báo");
                alert.setHeaderText(null);
                alert.setContentText("Tính năng đang được phát triển!");
                alert.showAndWait();
            } catch (Exception e) {
                logger.error("Lỗi khi nhập file Excel", e);
                AlertUtils.showError("Lỗi", "Không thể nhập file Excel!");
            }
        }
    }

    @FXML
    private void handleSortByDate() {
        currentSortOption = currentSortOption == DATE_ASC ? DATE_DESC : DATE_ASC;
        loadExpenses();
    }

    @FXML
    private void handleSortByAmount() {
        currentSortOption = currentSortOption == AMOUNT_ASC ? AMOUNT_DESC : AMOUNT_ASC;
        loadExpenses();
                        }

    @FXML
    private void handleSortByCategory() {
        currentSortOption = currentSortOption == SortUtil.SortOption.CATEGORY_ASC ? SortUtil.SortOption.CATEGORY_DESC : SortUtil.SortOption.CATEGORY_ASC;
                loadExpenses();
    }

    @FXML
    private void handleShowStatistics() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/expensemanager/StatisticsView.fxml"));
            Scene scene = new Scene(loader.load());
            Stage stage = new Stage();
            stage.setTitle("Thống kê chi tiêu");
            stage.setScene(scene);
            stage.show();
            } catch (IOException e) {
            logger.error("Lỗi khi mở thống kê", e);
            AlertUtils.showError("Lỗi", "Không thể mở thống kê!");
        }
    }

    @FXML
    private void handleViewAll() {
        loadExpenses();
    }

    @FXML
    private void handleViewByDate() {
        String date = DateUtil.chooseDate("Chọn ngày");
        if (date != null) {
            try {
                List<Expense> expenses = ExpenseDAO.getExpensesByDate(date);
                loadExpenses(expenses);
            } catch (SQLException e) {
                logger.error("Lỗi khi tải dữ liệu theo ngày", e);
                AlertUtils.showError("Lỗi", "Không thể tải dữ liệu theo ngày!");
            }
        }
    }

    @FXML
    private void handleViewByDateRange() {
        String startDate = DateUtil.chooseDate("Chọn ngày bắt đầu");
        if (startDate != null) {
            String endDate = DateUtil.chooseDate("Chọn ngày kết thúc");
            if (endDate != null) {
                try {
                    List<Expense> expenses = ExpenseDAO.getExpensesByDateRange(startDate, endDate);
                    loadExpenses(expenses);
                    } catch (SQLException e) {
                    logger.error("Lỗi khi tải dữ liệu theo khoảng ngày", e);
                    AlertUtils.showError("Lỗi", "Không thể tải dữ liệu theo khoảng ngày!");
                    }
                }
            }
    }

    @FXML
    private void handleViewByCategory() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Xem theo danh mục");
        dialog.setHeaderText(null);
        dialog.setContentText("Nhập tên danh mục:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(category -> {
            try {
                List<Expense> expenses = ExpenseDAO.getExpensesByCategory(category);
                loadExpenses(expenses);
        } catch (SQLException e) {
                logger.error("Lỗi khi tải dữ liệu theo danh mục", e);
                AlertUtils.showError("Lỗi", "Không thể tải dữ liệu theo danh mục!");
        }
        });
    }

    @FXML
    private void handleSortByDateDesc() {
        currentSortOption = SortUtil.SortOption.DATE_DESC;
        loadExpenses();
    }

    @FXML
    private void handleSortByDateAsc() {
        currentSortOption = SortUtil.SortOption.DATE_ASC;
        loadExpenses();
    }

    @FXML
    private void handleSortByAmountDesc() {
        currentSortOption = SortUtil.SortOption.AMOUNT_DESC;
        loadExpenses();
    }

    @FXML
    private void handleSortByAmountAsc() {
        currentSortOption = SortUtil.SortOption.AMOUNT_ASC;
        loadExpenses();
    }

    @FXML
    private void handleShowDailyChart() {
        String date = DateUtil.chooseDate("Chọn ngày");
        if (date != null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/expensemanager/StatisticsView.fxml"));
                Scene scene = new Scene(loader.load());
                Stage stage = new Stage();
                stage.setTitle("Thống kê chi tiêu ngày " + date);
                stage.setScene(scene);
                
                StatisticsController controller = loader.getController();
                controller.startStatisticDaily(date);
                
                stage.show();
            } catch (IOException e) {
                logger.error("Lỗi khi mở thống kê", e);
                AlertUtils.showError("Lỗi", "Không thể mở thống kê!");
            }
        }
    }

    @FXML
    private void handleShowMonthlyChart() {
        String startDate = DateUtil.chooseDate("Chọn ngày bắt đầu");
        if (startDate != null) {
        String endDate = DateUtil.chooseDate("Chọn ngày kết thúc");
            if (endDate != null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/expensemanager/StatisticsView.fxml"));
                    Scene scene = new Scene(loader.load());
                    Stage stage = new Stage();
                    stage.setTitle("Thống kê chi tiêu từ " + startDate + " đến " + endDate);
                    stage.setScene(scene);
                    
                StatisticsController controller = loader.getController();
                controller.startStatisticDateRange(startDate, endDate);
                    
                stage.show();
                } catch (IOException e) {
                    logger.error("Lỗi khi mở thống kê", e);
                    AlertUtils.showError("Lỗi", "Không thể mở thống kê!");
            }
        }
    }
    }

    @FXML
    private void deleteAllExpense() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Xác nhận xóa");
        alert.setHeaderText(null);
        alert.setContentText("Bạn có chắc chắn muốn xóa tất cả chi tiêu?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                ExpenseDAO.deleteAllExpenses();
                loadExpenses();
                logger.info("Đã xóa tất cả chi tiêu");
        } catch (SQLException e) {
                logger.error("Lỗi khi xóa tất cả chi tiêu", e);
                AlertUtils.showError("Lỗi", "Không thể xóa tất cả chi tiêu!");
        }
    }
    }

    @FXML
    private void handleRefresh() {
        loadExpenses();
    }

    @FXML
    private void handleSaveExpense() {
        String amountStr = textFiel_Amount.getText().trim();
        String category = textFiel_Category.getText().trim();
        LocalDate date = DatePicker.getValue();
        if (amountStr.isEmpty() || category.isEmpty() || date == null) {
            AlertUtils.showError("Lỗi", "Vui lòng nhập đầy đủ thông tin!");
            return;
        }
        try {
            double amount = Double.parseDouble(amountStr);
            Expense expense = new Expense(
                0,
                UserSession.getCurrentUserId(),
                amount,
                category,
                date.toString()
            );
            ExpenseDAO.addExpense(expense);
            loadExpenses();
            // Xóa dữ liệu nhập
            textFiel_Amount.clear();
            textFiel_Category.clear();
            DatePicker.setValue(null);
            // Ẩn inputBox
            inputBox.setVisible(false);
            inputBox.setManaged(false);
            logger.info("Đã thêm chi tiêu mới: {} - {} - {}", amount, category, date);
        } catch (NumberFormatException e) {
            AlertUtils.showError("Lỗi", "Số tiền không hợp lệ!");
        } catch (SQLException e) {
            logger.error("Lỗi khi thêm chi tiêu", e);
            AlertUtils.showError("Lỗi", "Không thể thêm chi tiêu!");
        }
    }

    @FXML
    private void handleCancelInput() {
        textFiel_Amount.clear();
        textFiel_Category.clear();
        DatePicker.setValue(null);
        inputBox.setVisible(false);
        inputBox.setManaged(false);
    }

    private void showAddExpenseDialog() {
        Dialog<Expense> dialog = new Dialog<>();
        dialog.setTitle("Thêm chi tiêu mới");
        dialog.setHeaderText(null);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

        TextField amountField = new TextField();
        amountField.setPromptText("Số tiền");
        TextField categoryField = new TextField();
        categoryField.setPromptText("Danh mục");
        DatePicker datePicker = new DatePicker();

        grid.add(new Label("Số tiền:"), 0, 0);
        grid.add(amountField, 1, 0);
        grid.add(new Label("Danh mục:"), 0, 1);
        grid.add(categoryField, 1, 1);
        grid.add(new Label("Ngày:"), 0, 2);
        grid.add(datePicker, 1, 2);

        dialog.getDialogPane().setContent(grid);

        ButtonType saveButtonType = new ButtonType("Lưu", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    double amount = Double.parseDouble(amountField.getText().trim());
                    String category = categoryField.getText().trim();
                    LocalDate date = datePicker.getValue();
                    if (amount <= 0 || category.isEmpty() || date == null) {
                        AlertUtils.showError("Lỗi", "Vui lòng nhập đầy đủ thông tin!");
                        return null;
                    }
                    return new Expense(
                        0,
                        UserSession.getCurrentUserId(),
                        amount,
                        category,
                        date.toString()
                    );
                } catch (NumberFormatException e) {
                    AlertUtils.showError("Lỗi", "Số tiền không hợp lệ!");
                }
            }
            return null;
        });

        Optional<Expense> result = dialog.showAndWait();
        result.ifPresent(expense -> {
            try {
                ExpenseDAO.addExpense(expense);
        loadExpenses();
                logger.info("Đã thêm chi tiêu mới: {} - {} - {}", expense.getAmount(), expense.getCategory(), expense.getDate());
            } catch (SQLException e) {
                logger.error("Lỗi khi thêm chi tiêu", e);
                AlertUtils.showError("Lỗi", "Không thể thêm chi tiêu!");
            }
        });
    }

    private void showEditProfileDialog() {
        // TODO: Hiện dialog cho phép sửa tên/email (hoặc các trường bạn muốn)
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Chỉnh sửa thông tin cá nhân");
        alert.setHeaderText(null);
        alert.setContentText("Tính năng đang phát triển!");
        alert.showAndWait();
    }

    private void showChangePasswordDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Đổi mật khẩu");
        dialog.setHeaderText("Vui lòng nhập thông tin để đổi mật khẩu");
        dialog.getDialogPane().setPrefWidth(420);

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(18);
        grid.setPadding(new javafx.geometry.Insets(24, 32, 16, 32));

        Label oldLabel = new Label("Mật khẩu cũ:");
        oldLabel.setStyle("-fx-font-weight: bold;");
        PasswordField oldPasswordField = new PasswordField();
        oldPasswordField.setPromptText("Nhập mật khẩu cũ");
        oldPasswordField.setPrefWidth(220);

        Label newLabel = new Label("Mật khẩu mới:");
        newLabel.setStyle("-fx-font-weight: bold;");
        PasswordField newPasswordField = new PasswordField();
        newPasswordField.setPromptText("Nhập mật khẩu mới");
        newPasswordField.setPrefWidth(220);

        Label confirmLabel = new Label("Xác nhận mật khẩu mới:");
        confirmLabel.setStyle("-fx-font-weight: bold;");
        PasswordField confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Nhập lại mật khẩu mới");
        confirmPasswordField.setPrefWidth(220);

        grid.add(oldLabel, 0, 0);
        grid.add(oldPasswordField, 1, 0);
        grid.add(newLabel, 0, 1);
        grid.add(newPasswordField, 1, 1);
        grid.add(confirmLabel, 0, 2);
        grid.add(confirmPasswordField, 1, 2);

        dialog.getDialogPane().setContent(grid);

        ButtonType changeButtonType = new ButtonType("Đổi mật khẩu", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(changeButtonType, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == changeButtonType) {
                String oldPass = oldPasswordField.getText();
                String newPass = newPasswordField.getText();
                String confirmPass = confirmPasswordField.getText();

                if (oldPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
                    AlertUtils.showError("Lỗi", "Vui lòng nhập đầy đủ thông tin!");
                    return null;
                }
                if (!newPass.equals(confirmPass)) {
                    AlertUtils.showError("Lỗi", "Mật khẩu mới không khớp!");
                    return null;
                }
                if (newPass.length() < 6) {
                    AlertUtils.showError("Lỗi", "Mật khẩu mới phải có ít nhất 6 ký tự!");
                    return null;
                }

                // Kiểm tra mật khẩu cũ
                try (Connection conn = DataBaseManager.getConnection();
                     PreparedStatement pstmt = conn.prepareStatement("SELECT password_hash, salt FROM users WHERE id = ?")) {
                    pstmt.setInt(1, UserSession.getCurrentUserId());
                    ResultSet rs = pstmt.executeQuery();
                    if (rs.next()) {
                        String storedHash = rs.getString("password_hash");
                        String salt = rs.getString("salt");
                        if (!PasswordUtils.verifyPassword(oldPass, storedHash, salt)) {
                            AlertUtils.showError("Lỗi", "Mật khẩu cũ không đúng!");
                            return null;
                        }
                        // Kiểm tra mật khẩu mới có trùng mật khẩu cũ không
                        if (PasswordUtils.verifyPassword(newPass, storedHash, salt)) {
                            AlertUtils.showError("Lỗi", "Mật khẩu mới không được trùng với mật khẩu cũ!");
                            return null;
                        }
                    } else {
                        AlertUtils.showError("Lỗi", "Không tìm thấy người dùng!");
                        return null;
                    }
                } catch (Exception e) {
                    AlertUtils.showError("Lỗi", "Có lỗi xảy ra khi kiểm tra mật khẩu cũ!");
                    return null;
                }

                // Cập nhật mật khẩu mới
                try (Connection conn = DataBaseManager.getConnection();
                     PreparedStatement pstmt = conn.prepareStatement("UPDATE users SET password_hash = ?, salt = ? WHERE id = ?")) {
                    String newSalt = PasswordUtils.generateSalt();
                    String newHash = PasswordUtils.hashPassword(newPass, newSalt);
                    pstmt.setString(1, newHash);
                    pstmt.setString(2, newSalt);
                    pstmt.setInt(3, UserSession.getCurrentUserId());
                    pstmt.executeUpdate();
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Thành công");
                    alert.setHeaderText(null);
                    alert.setContentText("Đổi mật khẩu thành công!");
                    alert.showAndWait();
                } catch (Exception e) {
                    AlertUtils.showError("Lỗi", "Không thể cập nhật mật khẩu!");
                }
            }
            return null;
        });

        dialog.showAndWait();
    }
}
