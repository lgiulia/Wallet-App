package com.example.walletapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.walletapp.data.Category
import com.example.walletapp.data.FinanceDao
import com.example.walletapp.data.Transaction
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FinanceViewModel(private val dao: FinanceDao) : ViewModel() {

    // 1. DATA READING: Trasformiamo i Flow di Room in StateFlow perfetti per Compose
    val allTransactions: StateFlow<List<Transaction>> = dao.getAllTransactions()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000), // Ottimizzazione della batteria
            initialValue = emptyList()
        )

    val allCategories: StateFlow<List<Category>> = dao.getAllCategories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // 2. DATA WRITING: Usiamo le Coroutines per non bloccare lo schermo
    fun insertTransaction(transaction: Transaction) {
        viewModelScope.launch {
            dao.insertTransaction(transaction)
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            dao.deleteTransaction(transaction)
        }
    }

    fun insertCategory(category: Category) {
        viewModelScope.launch {
            dao.insertCategory(category)
        }
    }
}

// 3. FACTORY: Insegna ad Android come creare questo ViewModel
class FinanceViewModelFactory(private val dao: FinanceDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FinanceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FinanceViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}