package com.example.walletapp.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Category(
    val id: Long,
    val name: String,
    @SerialName("is_expense") val isExpense: Boolean,
    @SerialName("user_id") val userId: String? = null
)

@Serializable
data class Account(
    val id: Long,
    val name: String,
    @SerialName("initial_balance") val initialBalance: Double,
    @SerialName("user_id") val userId: String? = null
)

@Serializable
data class Transaction(
    val id: Long,
    @SerialName("account_id") val accountId: Long,
    @SerialName("category_id") val categoryId: Long,
    val amount: Double,
    val title: String,
    val date: Long,
    @SerialName("user_id") val userId: String? = null
)