package com.example.finwise.utils

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.finwise.model.Expense
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

class PreferenceManager(private val context: Context) {
    companion object {
        private const val PREF_NAME = "FinWisePrefs"
        private const val KEY_EXPENSES = "expenses"
        private const val KEY_MONTHLY_BUDGET = "monthly_budget"
        private const val KEY_FOOD_BUDGET = "food_budget"
        private const val KEY_TRANSPORTATION_BUDGET = "transportation_budget"
        private const val KEY_SHOPPING_BUDGET = "shopping_budget"
        private const val KEY_ENTERTAINMENT_BUDGET = "entertainment_budget"

        val DATE_FORMAT = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    }

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    // Expense Management
    fun saveExpense(expense: Expense) {
        val expenses = getExpenses().toMutableList()
        expenses.add(expense)
        saveExpenses(expenses)
        checkAndNotifyBudget()
    }

    fun updateExpense(expense: Expense) {
        val expenses = getExpenses().toMutableList()
        val index = expenses.indexOfFirst { it.id == expense.id }
        if (index != -1) {
            expenses[index] = expense
            saveExpenses(expenses)
            checkAndNotifyBudget()
        }
    }

    fun deleteExpense(expenseId: String) {
        val expenses = getExpenses().toMutableList()
        expenses.removeIf { it.id == expenseId }
        saveExpenses(expenses)
        checkAndNotifyBudget()
    }

    private fun checkAndNotifyBudget() {
        val monthlyBudget = getMonthlyBudget()
        val totalExpenses = getTotalExpenses()
        if (monthlyBudget > 0 && totalExpenses >= monthlyBudget) {
            // Send both broadcasts - local for UI updates and system-wide for notifications
            LocalBroadcastManager.getInstance(context)
                .sendBroadcast(Intent("com.example.finwise.EXPENSE_UPDATED"))

            // This is the new broadcast that will trigger the notification even when app is not in foreground
            context.sendBroadcast(Intent("com.example.finwise.BUDGET_EXCEEDED").apply {
                // Add relevant data to the intent
                putExtra("monthlyBudget", monthlyBudget)
                putExtra("totalExpenses", totalExpenses)
                putExtra("percentage", (totalExpenses / monthlyBudget * 100).toInt())
            })
        }
    }

    private fun broadcastExpenseUpdate() {
        LocalBroadcastManager.getInstance(context)
            .sendBroadcast(Intent("com.example.finwise.EXPENSE_UPDATED"))
    }

    fun getExpense(expenseId: String): Expense? = getExpenses().find { it.id == expenseId }

    fun getExpenses(): List<Expense> {
        return sharedPreferences.getString(KEY_EXPENSES, null)?.let {
            gson.fromJson(it, object : TypeToken<List<Expense>>() {}.type)
        } ?: emptyList()
    }

    fun getExpensesByCategory(category: String?): List<Expense> {
        return if (category == null || category.equals("All", ignoreCase = true)) {
            getExpenses()
        } else {
            getExpenses().filter { it.category.equals(category, ignoreCase = true) }
        }
    }

    private fun saveExpenses(expenses: List<Expense>) {
        sharedPreferences.edit()
            .putString(KEY_EXPENSES, gson.toJson(expenses))
            .apply()
    }

    // Budget Management
    fun saveMonthlyBudget(budget: Double) {
        sharedPreferences.edit()
            .putFloat(KEY_MONTHLY_BUDGET, budget.toFloat())
            .apply()
        checkAndNotifyBudget() // Changed from broadcastExpenseUpdate to checkAndNotifyBudget
    }

    fun getMonthlyBudget(): Double =
        sharedPreferences.getFloat(KEY_MONTHLY_BUDGET, 0f).toDouble()

    fun saveCategoryBudget(category: String, budget: Double) {
        val key = when (category.lowercase()) {
            "food" -> KEY_FOOD_BUDGET
            "transportation", "transport" -> KEY_TRANSPORTATION_BUDGET
            "shopping" -> KEY_SHOPPING_BUDGET
            "entertainment" -> KEY_ENTERTAINMENT_BUDGET
            else -> return
        }
        sharedPreferences.edit().putFloat(key, budget.toFloat()).apply()
    }

    fun getCategoryBudget(category: String): Double {
        val key = when (category.lowercase()) {
            "food" -> KEY_FOOD_BUDGET
            "transportation", "transport" -> KEY_TRANSPORTATION_BUDGET
            "shopping" -> KEY_SHOPPING_BUDGET
            "entertainment" -> KEY_ENTERTAINMENT_BUDGET
            else -> return 0.0
        }
        return sharedPreferences.getFloat(key, 0f).toDouble()
    }

    fun getTotalExpenses(): Double = getExpenses().sumOf { it.amount }

    fun getCategoryExpenses(category: String): Double =
        getExpenses()
            .filter { it.category.equals(category, ignoreCase = true) }
            .sumOf { it.amount }

    // Date Utilities
    fun dateToTimestamp(dateString: String): Long =
        try {
            DATE_FORMAT.parse(dateString)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }

    fun timestampToDate(timestamp: Long): String =
        DATE_FORMAT.format(Date(timestamp))

    fun checkBudgetStatusOnAppStart() {
        checkAndNotifyBudget()
    }
}