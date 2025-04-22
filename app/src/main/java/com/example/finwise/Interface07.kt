package com.example.finwise

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.finwise.databinding.ActivityInterface07Binding
import com.example.finwise.model.Expense
import com.example.finwise.utils.PreferenceManager
import java.util.*

class Interface07 : AppCompatActivity() {
    private lateinit var binding: ActivityInterface07Binding
    private lateinit var preferenceManager: PreferenceManager
    private var selectedDate: Long = System.currentTimeMillis()
    private var isEditMode = false
    private var expenseId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInterface07Binding.inflate(layoutInflater)
        setContentView(binding.root)

        preferenceManager = PreferenceManager(this)
        setupListeners()

        // Check if we're in edit mode
        expenseId = intent.getStringExtra("EXPENSE_ID")
        isEditMode = expenseId != null

        if (isEditMode) {
            loadExpenseForEdit()
            binding.tvTitle.text = "Edit Expense"
            binding.btnAddExpense.text = "Update Expense"
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            // Navigate to Interface06
            val intent = Intent(this, Interface06::class.java)
            startActivity(intent)
            finish() // Optional: removes current activity from stack
        }

        binding.btnAddExpense.setOnClickListener {
            if (validateInputs()) {
                saveExpense()
            }
        }

        binding.etDate.setOnClickListener {
            showDatePicker()
        }
    }

    private fun loadExpenseForEdit() {
        val expense = preferenceManager.getExpense(expenseId!!)
        if (expense != null) {
            binding.etTitle.setText(expense.title)
            binding.etAmount.setText(expense.amount.toString())
            binding.etDate.setText(preferenceManager.timestampToDate(expense.date))

            // Select the category radio button
            val radioButtonId = when (expense.category.lowercase()) {
                "rent" -> R.id.rbRent
                "bill" -> R.id.rbBill
                "food" -> R.id.rbFood
                "shopping" -> R.id.rbShopping
                "transport" -> R.id.rbTransport
                else -> -1
            }

            if (radioButtonId != -1) {
                binding.rgCategory.check(radioButtonId)
            }
        } else {
            Toast.makeText(this, "Expense not found", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun validateInputs(): Boolean {
        var isValid = true

        // Validate title
        if (binding.etTitle.text.toString().trim().isEmpty()) {
            binding.tilTitle.error = "Title is required"
            isValid = false
        } else {
            binding.tilTitle.error = null
        }

        // Validate amount
        val amountStr = binding.etAmount.text.toString().trim()
        if (amountStr.isEmpty()) {
            binding.tilAmount.error = "Amount is required"
            isValid = false
        } else {
            try {
                val amount = amountStr.toDouble()
                if (amount <= 0) {
                    binding.tilAmount.error = "Amount must be greater than 0"
                    isValid = false
                } else {
                    binding.tilAmount.error = null
                }
            } catch (e: NumberFormatException) {
                binding.tilAmount.error = "Invalid amount"
                isValid = false
            }
        }

        // Validate date
        if (binding.etDate.text.toString().trim().isEmpty()) {
            binding.tilDate.error = "Date is required"
            isValid = false
        } else {
            binding.tilDate.error = null
        }

        // Validate category
        if (binding.rgCategory.checkedRadioButtonId == -1) {
            Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        return isValid
    }

    private fun saveExpense() {
        val title = binding.etTitle.text.toString().trim()
        val amount = binding.etAmount.text.toString().toDouble()
        val dateStr = binding.etDate.text.toString().trim()
        val date = preferenceManager.dateToTimestamp(dateStr)

        // Get selected category
        val selectedCategoryId = binding.rgCategory.checkedRadioButtonId
        val categoryRadioButton = findViewById<RadioButton>(selectedCategoryId)
        val category = categoryRadioButton.text.toString()

        val expense = if (isEditMode) {
            // Update existing expense
            Expense(
                id = expenseId!!,
                title = title,
                amount = amount,
                category = category,
                date = date,
                timestamp = System.currentTimeMillis()
            )
        } else {
            // Create new expense
            Expense(
                title = title,
                amount = amount,
                category = category,
                date = date
            )
        }

        if (isEditMode) {
            preferenceManager.updateExpense(expense)
            Toast.makeText(this, "Expense updated", Toast.LENGTH_SHORT).show()
        } else {
            preferenceManager.saveExpense(expense)
            Toast.makeText(this, "Expense added", Toast.LENGTH_SHORT).show()
        }

        // Navigate back to Interface06 after saving
        val intent = Intent(this, Interface06::class.java)
        startActivity(intent)
        finish()
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(
            this,
            { _, year, month, day ->
                calendar.set(year, month, day)
                selectedDate = calendar.timeInMillis
                binding.etDate.setText(preferenceManager.timestampToDate(selectedDate))
            },
            currentYear,
            currentMonth,
            currentDay
        ).show()
    }
}