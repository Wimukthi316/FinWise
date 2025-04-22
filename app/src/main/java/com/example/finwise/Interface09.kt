package com.example.finwise

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.finwise.databinding.ActivityInterface09Binding
import com.example.finwise.utils.PreferenceManager
import java.text.NumberFormat
import java.util.Locale

class Interface09 : AppCompatActivity() {
    private lateinit var binding: ActivityInterface09Binding
    private lateinit var preferenceManager: PreferenceManager
    private val CHANNEL_ID = "budget_alerts"
    private val NOTIFICATION_ID = 1001
    private var notificationShown = false

    // Notification permission launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) checkBudgetExceeded()
        else Toast.makeText(
            this,
            "Budget alerts require notification permission",
            Toast.LENGTH_LONG
        ).show()
    }

    private val expenseUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.example.finwise.EXPENSE_UPDATED") {
                loadBudgetData()
                checkBudgetExceeded()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInterface09Binding.inflate(layoutInflater)
        setContentView(binding.root)

        preferenceManager = PreferenceManager(this)
        createNotificationChannel()
        setupListeners()

        LocalBroadcastManager.getInstance(this).registerReceiver(
            expenseUpdateReceiver,
            IntentFilter("com.example.finwise.EXPENSE_UPDATED")
        )

        loadBudgetData()
        checkBudgetExceeded()
    }

    override fun onResume() {
        super.onResume()
        notificationShown = false
        loadBudgetData()
        checkBudgetExceeded()
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(expenseUpdateReceiver)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Budget Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when you reach or exceed your budget"
                enableLights(true)
                lightColor = android.graphics.Color.RED
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 250, 500)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }
        binding.btnSetBudget.setOnClickListener { saveMonthlyBudget() }
        binding.btnSaveCategoryBudgets.setOnClickListener { saveCategoryBudgets() }
    }

    private fun loadBudgetData() {
        val currency = NumberFormat.getCurrencyInstance(Locale.US)
        val monthlyBudget = preferenceManager.getMonthlyBudget()
        val totalExpenses = preferenceManager.getTotalExpenses()

        // Calculate percentage for overall budget
        val budgetPercentage = if (monthlyBudget > 0) {
            ((totalExpenses / monthlyBudget) * 100).toInt().coerceAtMost(100)
        } else 0

        binding.apply {
            etBudget.setText(if (monthlyBudget > 0) monthlyBudget.toString() else "")
            etFoodBudget.setText(if (preferenceManager.getCategoryBudget("food") > 0)
                preferenceManager.getCategoryBudget("food").toString() else "")
            etShoppingBudget.setText(if (preferenceManager.getCategoryBudget("shopping") > 0)
                preferenceManager.getCategoryBudget("shopping").toString() else "")

            tvBudgetProgress.text = "${currency.format(totalExpenses)} / ${currency.format(monthlyBudget)}"
            tvBudgetPercentage.text = "$budgetPercentage%"

            // Set circular progress for main budget
            progressBudget.progress = budgetPercentage
        }
        updateCategoryBreakdown()
    }

    private fun updateCategoryBreakdown() {
        val currency = NumberFormat.getCurrencyInstance(Locale.US)
        val total = preferenceManager.getTotalExpenses().takeIf { it > 0 } ?: 1.0

        listOf("food", "shopping").forEach { category ->
            val expenses = preferenceManager.getCategoryExpenses(category)
            val budget = preferenceManager.getCategoryBudget(category)

            // Calculate percentage for circular progress indicators
            val percentage = if (budget > 0) {
                ((expenses / budget) * 100).toInt().coerceAtMost(100)
            } else {
                ((expenses / total) * 100).toInt().coerceAtMost(100)
            }

            // Set values for circular progress indicators
            binding.getProgressView(category)?.progress = percentage
            binding.getAmountTextView(category)?.text = currency.format(expenses)
            binding.getBudgetTextView(category)?.text = "/ ${currency.format(budget)}"
        }
    }

    private fun checkBudgetExceeded() {
        val monthlyBudget = preferenceManager.getMonthlyBudget()
        val totalExpenses = preferenceManager.getTotalExpenses()

        if (monthlyBudget > 0 && totalExpenses >= monthlyBudget && !notificationShown) {
            if (canShowNotification()) {
                showBudgetNotification(totalExpenses, monthlyBudget)
                notificationShown = true
            }
        }
    }

    private fun canShowNotification(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> true
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    false
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    false
                }
            }
        } else true
    }

    private fun showBudgetNotification(expenses: Double, budget: Double) {
        val percentage = (expenses / budget * 100).toInt()
        val currency = NumberFormat.getCurrencyInstance(Locale.US)

        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_warning)
            .setContentTitle("Budget ${if (percentage >= 100) "Exceeded" else "Reached"}!")
            .setContentText("${currency.format(expenses)} (${percentage}%) of ${currency.format(budget)} budget has been spent.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .build().also {
                val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(NOTIFICATION_ID, it)
            }
    }

    private fun saveMonthlyBudget() {
        try {
            val budgetText = binding.etBudget.text.toString()
            if (budgetText.isNotEmpty()) {
                val budget = budgetText.toDouble()
                preferenceManager.saveMonthlyBudget(budget)
                Toast.makeText(this, "Monthly budget set successfully", Toast.LENGTH_SHORT).show()
                loadBudgetData()
                checkBudgetExceeded()
            } else {
                Toast.makeText(this, "Please enter a valid budget amount", Toast.LENGTH_SHORT).show()
            }
        } catch (e: NumberFormatException) {
            Toast.makeText(this, "Please enter a valid number", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveCategoryBudgets() {
        try {
            val categories = mapOf(
                "food" to binding.etFoodBudget.text.toString(),
                "shopping" to binding.etShoppingBudget.text.toString()
            )

            var success = true
            categories.forEach { (category, valueText) ->
                if (valueText.isNotEmpty()) {
                    try {
                        val budget = valueText.toDouble()
                        preferenceManager.saveCategoryBudget(category, budget)
                    } catch (e: NumberFormatException) {
                        success = false
                    }
                }
            }

            if (success) {
                Toast.makeText(this, "Category budgets saved successfully", Toast.LENGTH_SHORT).show()
                loadBudgetData()
            } else {
                Toast.makeText(this, "Please enter valid numbers for all categories", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error saving category budgets", Toast.LENGTH_SHORT).show()
        }
    }

    // Extension functions to access views based on category name
    private fun ActivityInterface09Binding.getProgressView(category: String) = when(category) {
        "food" -> progressFood
        "shopping" -> progressShopping
        else -> null
    }

    private fun ActivityInterface09Binding.getAmountTextView(category: String) = when(category) {
        "food" -> tvFoodAmount
        "shopping" -> tvShoppingAmount
        else -> null
    }

    private fun ActivityInterface09Binding.getBudgetTextView(category: String) = when(category) {
        "food" -> tvFoodBudget
        "shopping" -> tvShoppingBudget
        else -> null
    }
}