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

        binding.apply {
            etBudget.setText(if (monthlyBudget > 0) monthlyBudget.toString() else "")
            etFoodBudget.setText(preferenceManager.getCategoryBudget("food").toString())
            etTransportationBudget.setText(preferenceManager.getCategoryBudget("transportation").toString())
            etShoppingBudget.setText(preferenceManager.getCategoryBudget("shopping").toString())
            etEntertainmentBudget.setText(preferenceManager.getCategoryBudget("entertainment").toString())

            tvBudgetProgress.text = "${currency.format(totalExpenses)} / ${currency.format(monthlyBudget)}"
            progressBudget.progress = if (monthlyBudget > 0) {
                ((totalExpenses / monthlyBudget) * 100).toInt().coerceAtMost(100)
            } else 0
        }
        updateCategoryBreakdown()
    }

    private fun updateCategoryBreakdown() {
        val currency = NumberFormat.getCurrencyInstance(Locale.US)
        val total = preferenceManager.getTotalExpenses().takeIf { it > 0 } ?: 1.0

        listOf("food", "transportation", "shopping", "entertainment").forEach { category ->
            val expenses = preferenceManager.getCategoryExpenses(category)
            val budget = preferenceManager.getCategoryBudget(category)

            binding.getProgressView(category)?.progress = if (budget > 0) {
                ((expenses / budget) * 100).toInt().coerceAtMost(100)
            } else {
                ((expenses / total) * 100).toInt().coerceAtMost(100)
            }

            binding.getAmountTextView(category)?.text = currency.format(expenses)
            binding.getBudgetTextView(category)?.text = currency.format(budget)
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
            .setContentText("${currency.format(expenses)} (${percentage}%) of ${currency.format(budget)}")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .build()
            .also { notification ->
                (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                    .notify(NOTIFICATION_ID, notification)
            }
    }

    private fun saveMonthlyBudget() {
        binding.etBudget.text.toString().takeIf { it.isNotEmpty() }?.let { budgetText ->
            try {
                val budget = budgetText.toDouble()
                if (budget <= 0) {
                    binding.tilBudget.error = "Budget must be greater than 0"
                    return
                }
                preferenceManager.saveMonthlyBudget(budget)
                binding.tilBudget.error = null
                Toast.makeText(this, "Budget set", Toast.LENGTH_SHORT).show()
                notificationShown = false
                loadBudgetData()
                checkBudgetExceeded()
            } catch (e: NumberFormatException) {
                binding.tilBudget.error = "Invalid amount"
            }
        } ?: run { binding.tilBudget.error = "Required" }
    }

    private fun saveCategoryBudgets() {
        try {
            listOf("food", "transportation", "shopping", "entertainment").forEach { category ->
                binding.getBudgetEditText(category)?.text?.toString()?.takeIf { it.isNotEmpty() }?.let { budgetText ->
                    val budget = budgetText.toDouble()
                    if (budget < 0) {
                        Toast.makeText(this, "$category budget must be positive", Toast.LENGTH_SHORT).show()
                        return
                    }
                    preferenceManager.saveCategoryBudget(category, budget)
                }
            }
            Toast.makeText(this, "Category budgets saved", Toast.LENGTH_SHORT).show()
            loadBudgetData()
        } catch (e: NumberFormatException) {
            Toast.makeText(this, "Invalid budget format", Toast.LENGTH_SHORT).show()
        }
    }

    // Extension functions for view binding
    private fun ActivityInterface09Binding.getBudgetEditText(category: String) = when (category) {
        "food" -> etFoodBudget
        "transportation" -> etTransportationBudget
        "shopping" -> etShoppingBudget
        "entertainment" -> etEntertainmentBudget
        else -> null
    }

    private fun ActivityInterface09Binding.getProgressView(category: String) = when (category) {
        "food" -> progressFood
        "transportation" -> progressTransportation
        "shopping" -> progressShopping
        "entertainment" -> progressEntertainment
        else -> null
    }

    private fun ActivityInterface09Binding.getAmountTextView(category: String) = when (category) {
        "food" -> tvFoodAmount
        "transportation" -> tvTransportationAmount
        "shopping" -> tvShoppingAmount
        "entertainment" -> tvEntertainmentAmount
        else -> null
    }

    private fun ActivityInterface09Binding.getBudgetTextView(category: String) = when (category) {
        "food" -> tvFoodBudget
        "transportation" -> tvTransportationBudget
        "shopping" -> tvShoppingBudget
        "entertainment" -> tvEntertainmentBudget
        else -> null
    }
}