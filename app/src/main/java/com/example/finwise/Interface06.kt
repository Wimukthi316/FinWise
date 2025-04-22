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
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.finwise.utils.PreferenceManager
import com.google.android.material.button.MaterialButton
import java.text.NumberFormat
import java.util.Locale

class Interface06 : AppCompatActivity() {

    private lateinit var preferenceManager: PreferenceManager
    private lateinit var tvBudgetProgress: TextView
    private lateinit var progressBudget: ProgressBar
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

    // Local BroadcastReceiver to listen for expense updates
    private val expenseUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            updateBudgetUI()
            checkBudgetExceeded()
        }
    }

    // System-wide BroadcastReceiver to listen for budget exceeded notifications
    private val budgetExceededReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == "com.example.finwise.BUDGET_EXCEEDED") {
                val monthlyBudget = intent.getDoubleExtra("monthlyBudget", 0.0)
                val totalExpenses = intent.getDoubleExtra("totalExpenses", 0.0)
                val percentage = intent.getIntExtra("percentage", 0)

                if (canShowNotification() && !notificationShown) {
                    showBudgetNotification(totalExpenses, monthlyBudget)
                    notificationShown = true
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_interface06)

        preferenceManager = PreferenceManager(this)
        createNotificationChannel()

        // Initialize UI components
        tvBudgetProgress = findViewById(R.id.tvBudgetProgress)
        progressBudget = findViewById(R.id.progressBudget)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Add Expense button - navigates to Interface07
        val btnAddExpense = findViewById<MaterialButton>(R.id.btnAddExpense)
        btnAddExpense.setOnClickListener {
            navigateToActivity(Interface07::class.java)
        }

        // View Details button - navigates to Interface08
        val btnViewSummary = findViewById<MaterialButton>(R.id.btnViewSummary)
        btnViewSummary.setOnClickListener {
            navigateToActivity(Interface08::class.java)
        }

        // Manage Budget button - navigates to Interface09
        val btnBudgetPlan = findViewById<MaterialButton>(R.id.btnBudgetPlan)
        btnBudgetPlan.setOnClickListener {
            navigateToActivity(Interface09::class.java)
        }

        // Initialize the budget UI
        updateBudgetUI()
        checkBudgetExceeded()
    }

    override fun onResume() {
        super.onResume()
        notificationShown = false

        // Register the BroadcastReceiver to listen for expense updates
        LocalBroadcastManager.getInstance(this).registerReceiver(
            expenseUpdateReceiver,
            IntentFilter("com.example.finwise.EXPENSE_UPDATED")
        )

        // Register the system-wide BroadcastReceiver for budget exceeded notifications
        registerReceiver(
            budgetExceededReceiver,
            IntentFilter("com.example.finwise.BUDGET_EXCEEDED"),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Context.RECEIVER_NOT_EXPORTED
            } else {
                Context.RECEIVER_EXPORTED
            }
        )

        // Update the UI in case changes were made while away from this activity
        updateBudgetUI()
        checkBudgetExceeded()
    }

    override fun onPause() {
        super.onPause()
        // Unregister the BroadcastReceivers when the activity is not visible
        LocalBroadcastManager.getInstance(this).unregisterReceiver(expenseUpdateReceiver)
        try {
            unregisterReceiver(budgetExceededReceiver)
        } catch (e: IllegalArgumentException) {
            // Receiver not registered
        }
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

    private fun updateBudgetUI() {
        val monthlyBudget = preferenceManager.getMonthlyBudget()
        val totalExpenses = preferenceManager.getTotalExpenses()

        // Format currency for display
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)

        // Update the budget progress text
        tvBudgetProgress.text = "${currencyFormat.format(totalExpenses)} / ${currencyFormat.format(monthlyBudget)}"

        // Calculate and set the progress percentage (capped at 100)
        val progressPercentage = if (monthlyBudget > 0) {
            ((totalExpenses / monthlyBudget) * 100).toInt().coerceAtMost(100)
        } else {
            0
        }
        progressBudget.progress = progressPercentage

        // Optional: Change color based on budget status
        if (totalExpenses > monthlyBudget) {
            tvBudgetProgress.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
        } else {
            tvBudgetProgress.setTextColor(resources.getColor(R.color.green, null))
        }
    }

    private fun navigateToActivity(activityClass: Class<*>) {
        val intent = Intent(this, activityClass)
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}