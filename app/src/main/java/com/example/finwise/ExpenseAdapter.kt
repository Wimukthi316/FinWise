package com.example.finwise.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.finwise.databinding.ItemExpenseBinding
import com.example.finwise.model.Expense
import com.example.finwise.utils.PreferenceManager
import java.text.NumberFormat
import java.util.*

class ExpenseAdapter(
    private var expenses: List<Expense>,
    private val context: Context,
    private val onItemClick: (Expense) -> Unit,
    private val onDeleteClick: (Expense) -> Unit,
    private val onEditClick: (Expense) -> Unit
) : RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder>() {

    fun updateExpenses(newExpenses: List<Expense>) {
        expenses = newExpenses.sortedByDescending { it.timestamp }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val binding = ItemExpenseBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ExpenseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        holder.bind(expenses[position])
    }

    override fun getItemCount(): Int = expenses.size

    inner class ExpenseViewHolder(private val binding: ItemExpenseBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(expense: Expense) {
            val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)

            binding.tvExpenseTitle.text = expense.title
            binding.tvExpenseAmount.text = currencyFormat.format(expense.amount)
            binding.tvExpenseCategory.text = expense.category
            binding.tvExpenseDate.text = PreferenceManager.DATE_FORMAT.format(Date(expense.date))

            binding.root.setOnClickListener {
                onItemClick(expense)
            }

            binding.btnEditExpense.setOnClickListener {
                onEditClick(expense)
            }

            binding.btnDeleteExpense.setOnClickListener {
                onDeleteClick(expense)
            }
        }
    }
}