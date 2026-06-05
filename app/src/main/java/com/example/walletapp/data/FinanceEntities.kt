package com.example.walletapp.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Category(
    val id: Long = 0,
    val name: String,
    @SerialName("is_expense") val isExpense: Boolean
)

@Serializable
data class Account(
    val id: Long = 0,
    val name: String,
    @SerialName("initial_balance") val initialBalance: Double = 0.0
)

@Serializable
data class Transaction(
    val id: Long = 0,
    val title: String,
    val amount: Double,
    val date: Long,
    @SerialName("category_id") val categoryId: Long,
    @SerialName("account_id") val accountId: Long
)