package com.example.walletapp.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isExpense: Boolean // true if outcome (es. food), false if income (es. wage)
)

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE // Se elimini una categoria, elimina anche le spese collegate
        )
    ],
    indices = [Index(value = ["categoryId"])] // Optimize search queries performance
)
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amount: Double,
    val date: Long,
    val categoryId: Long, // Chiave esterna collegata alla tabella Category
    val note: String? = null // Nota opzionale, può essere null
)