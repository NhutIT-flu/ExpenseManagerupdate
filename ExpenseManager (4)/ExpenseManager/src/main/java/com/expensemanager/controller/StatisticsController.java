package com.expensemanager.controller;

import com.expensemanager.model.Expense;
import com.expensemanager.database.ExpenseDAO;
import com.expensemanager.util.UserSession;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.*;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;
import java.util.HashMap;

public class StatisticsController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(StatisticsController.class);

    @FXML
    private BarChart<String, Number> barChart;
    @FXML
    private CategoryAxis barCategoryAxis;
    @FXML
    private NumberAxis barNumberAxis;
    @FXML
    private PieChart pieChart;
    @FXML
    private LineChart<String, Number> lineChart;
    @FXML
    private CategoryAxis lineCategoryAxis;
    @FXML
    private NumberAxis lineNumberAxis;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Dữ liệu mẫu, sau này sẽ truyền dữ liệu thực tế vào đây
        // BarChart
        XYChart.Series<String, Number> barSeries = new XYChart.Series<>();
        barSeries.setName("Chi tiêu");
        barSeries.getData().add(new XYChart.Data<>("Ăn uống", 500));
        barSeries.getData().add(new XYChart.Data<>("Đi lại", 200));
        barSeries.getData().add(new XYChart.Data<>("Mua sắm", 300));
        barChart.getData().add(barSeries);

        // PieChart
        pieChart.getData().add(new PieChart.Data("Ăn uống", 500));
        pieChart.getData().add(new PieChart.Data("Đi lại", 200));
        pieChart.getData().add(new PieChart.Data("Mua sắm", 300));

        // LineChart
        XYChart.Series<String, Number> lineSeries = new XYChart.Series<>();
        lineSeries.setName("Tổng chi tiêu");
        lineSeries.getData().add(new XYChart.Data<>("01/06", 200));
        lineSeries.getData().add(new XYChart.Data<>("02/06", 400));
        lineSeries.getData().add(new XYChart.Data<>("03/06", 300));
        lineChart.getData().add(lineSeries);
    }

    public void showStatistics(List<Expense> expenses, String title) {
        // Xóa dữ liệu cũ
        barChart.getData().clear();
        pieChart.getData().clear();
        lineChart.getData().clear();

        // Gom nhóm theo danh mục cho BarChart và PieChart
        HashMap<String, Double> categoryTotals = new HashMap<>();
        for (Expense expense : expenses) {
            categoryTotals.put(
                expense.getCategory(),
                categoryTotals.getOrDefault(expense.getCategory(), 0.0) + expense.getAmount()
            );
        }
        XYChart.Series<String, Number> barSeries = new XYChart.Series<>();
        barSeries.setName("Chi tiêu theo danh mục");
        for (String cat : categoryTotals.keySet()) {
            double value = categoryTotals.get(cat);
            barSeries.getData().add(new XYChart.Data<>(cat, value));
            pieChart.getData().add(new PieChart.Data(cat, value));
        }
        barChart.getData().add(barSeries);

        // Gom nhóm theo ngày cho LineChart
        HashMap<String, Double> dateTotals = new HashMap<>();
        for (Expense expense : expenses) {
            dateTotals.put(
                expense.getDate(),
                dateTotals.getOrDefault(expense.getDate(), 0.0) + expense.getAmount()
            );
        }
        XYChart.Series<String, Number> lineSeries = new XYChart.Series<>();
        lineSeries.setName("Tổng chi tiêu theo ngày");
        for (String date : dateTotals.keySet()) {
            lineSeries.getData().add(new XYChart.Data<>(date, dateTotals.get(date)));
        }
        lineChart.getData().add(lineSeries);

        barChart.setTitle(title);
        pieChart.setTitle(title);
        lineChart.setTitle(title);
    }

    public void startStatisticDaily(String date) {
        try {
            List<Expense> expenses = ExpenseDAO.getExpensesByDate(date);
            showStatistics(expenses, "Thống kê chi tiêu ngày " + date);
        } catch (SQLException e) {
            logger.error("Lỗi khi tải dữ liệu thống kê theo ngày", e);
            showError("Lỗi", "Không thể tải dữ liệu thống kê theo ngày!");
        }
    }

    public void startStatisticDateRange(String startDate, String endDate) {
        try {
            List<Expense> expenses = ExpenseDAO.getExpensesByDateRange(startDate, endDate);
            showStatistics(expenses, "Thống kê chi tiêu từ " + startDate + " đến " + endDate);
        } catch (SQLException e) {
            logger.error("Lỗi khi tải dữ liệu thống kê theo khoảng ngày", e);
            showError("Lỗi", "Không thể tải dữ liệu thống kê theo khoảng ngày!");
        }
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
} 