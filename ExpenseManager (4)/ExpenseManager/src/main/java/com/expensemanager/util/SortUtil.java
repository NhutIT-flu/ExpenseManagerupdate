package com.expensemanager.util;

import com.expensemanager.model.Expense;

import java.util.Comparator;
import java.util.List;

public class SortUtil {
    public enum SortOption {
        DATE_ASC,
        DATE_DESC,
        AMOUNT_ASC,
        AMOUNT_DESC,
        CATEGORY_ASC,
        CATEGORY_DESC
    }

    public static void sortExpenses(List<Expense> expenses, SortOption option) {
        switch (option) {
            case DATE_ASC:
                expenses.sort(Comparator.comparing(Expense::getDate));
                break;
            case DATE_DESC:
                expenses.sort(Comparator.comparing(Expense::getDate).reversed());
                break;
            case AMOUNT_ASC:
                expenses.sort(Comparator.comparing(Expense::getAmount));
                break;
            case AMOUNT_DESC:
                expenses.sort(Comparator.comparing(Expense::getAmount).reversed());
                break;
            case CATEGORY_ASC:
                expenses.sort(Comparator.comparing(Expense::getCategory));
                break;
            case CATEGORY_DESC:
                expenses.sort(Comparator.comparing(Expense::getCategory).reversed());
                break;
        }
    }
}
