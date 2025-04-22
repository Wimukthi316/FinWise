package com.example.finwise

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.finwise.adapter.ExpenseAdapter
import com.example.finwise.model.Expense
import com.example.finwise.utils.PreferenceManager
import com.example.finwise.databinding.ActivityInterface08Binding
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import java.text.NumberFormat
import java.util.*

class Interface08 : AppCompatActivity() {
    private lateinit var binding: ActivityInterface08Binding
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var adapter: ExpenseAdapter
    private var selectedCategory: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInterface08Binding.inflate(layoutInflater)
        setContentView(binding.root)

        preferenceManager = PreferenceManager(this)
        setupRecyclerView()
        setupListeners()
        loadExpenses()
    }

    override fun onResume() {
        super.onResume()
        loadExpenses()
    }

    private fun setupRecyclerView() {
        adapter = ExpenseAdapter(
            expenses = emptyList(),
            context = this,
            onItemClick = { expense ->
                showExpenseDetails(expense)
            },
            onDeleteClick = { expense ->
                showDeleteConfirmationDialog(expense)
            },
            onEditClick = { expense ->
                openEditExpenseActivity(expense)
            }
        )

        binding.rvExpenses.layoutManager = LinearLayoutManager(this)
        binding.rvExpenses.adapter = adapter
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.fabAddExpense.setOnClickListener {
            startActivity(Intent(this, Interface07::class.java))
        }

        binding.chipGroupFilter.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val chipId = checkedIds[0]
                val chip = group.findViewById<Chip>(chipId)
                selectedCategory = if (chip.id == R.id.chipAll) null else chip.text.toString()
                loadExpenses()
            }
        }
    }

    private fun loadExpenses() {
        val expenses = preferenceManager.getExpensesByCategory(selectedCategory)
        updateUI(expenses)
    }

    private fun updateUI(expenses: List<Expense>) {
        val totalExpenses = expenses.sumOf { it.amount }
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
        binding.tvTotalExpenses.text = currencyFormat.format(totalExpenses)

        adapter.updateExpenses(expenses)

        if (expenses.isEmpty()) {
            binding.emptyState.visibility = View.VISIBLE
            binding.rvExpenses.visibility = View.GONE
        } else {
            binding.emptyState.visibility = View.GONE
            binding.rvExpenses.visibility = View.VISIBLE
        }
    }

    private fun showExpenseDetails(expense: Expense) {
        // Implementation if needed
    }

    private fun showDeleteConfirmationDialog(expense: Expense) {
        AlertDialog.Builder(this)
            .setTitle("Delete Expense")
            .setMessage("Are you sure you want to delete '${expense.title}'?")
            .setPositiveButton("Delete") { _, _ ->
                deleteExpense(expense)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteExpense(expense: Expense) {
        preferenceManager.deleteExpense(expense.id)
        loadExpenses()
        showSnackbar("Expense deleted")
    }

    private fun openEditExpenseActivity(expense: Expense) {
        val intent = Intent(this, Interface07::class.java).apply {
            putExtra("EXPENSE_ID", expense.id)
        }
        startActivity(intent)
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }
}