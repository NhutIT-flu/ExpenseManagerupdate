module com.expensemanager {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires org.slf4j;
    requires java.desktop;

    opens com.expensemanager to javafx.fxml;
    opens com.expensemanager.controller to javafx.fxml;
    opens com.expensemanager.model to javafx.base;
    opens com.expensemanager.database to javafx.base;
    opens com.expensemanager.util to javafx.base;

    exports com.expensemanager;
    exports com.expensemanager.controller;
    exports com.expensemanager.model;
    exports com.expensemanager.database;
    exports com.expensemanager.util;
}