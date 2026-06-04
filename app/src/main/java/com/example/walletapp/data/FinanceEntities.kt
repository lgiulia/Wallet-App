package com.example.walletapp.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isExpense: Boolean
)

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val initialBalance: Double = 0.0 // Ci permette di modificare il valore di partenza di ogni conto
)

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Account::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE // Se elimini un conto, si eliminano le sue transazioni
        )
    ],
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["accountId"]) // Ottimizza la ricerca delle transazioni per conto
    ]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amount: Double,
    val date: Long,
    val categoryId: Long,
    val accountId: Long,
    val note: String? = null
)