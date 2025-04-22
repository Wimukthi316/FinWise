package com.example.finwise.model

import java.util.*

data class Expense(
    val id: String = UUID.randomUUID().toString(),
    val amount: Double,
    val title: String,
    val category: String,
    val date: Long,
    val timestamp: Long = System.currentTimeMillis()
)