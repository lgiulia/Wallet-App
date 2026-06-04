package com.example.walletapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.walletapp.data.Account
import com.example.walletapp.data.Category
import com.example.walletapp.data.FinanceDao
import com.example.walletapp.data.Transaction
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.abs

class FinanceViewModel(private val dao: FinanceDao) : ViewModel() {

    // --- LETTURA DATI ---
    val allTransactions: StateFlow<List<Transaction>> = dao.getAllTransactions()
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = emptyList())

    val allCategories: StateFlow<List<Category>> = dao.getAllCategories()
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = emptyList())

    val allAccounts: StateFlow<List<Account>> = dao.getAllAccounts()
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = emptyList())

    // --- GESTIONE CONTI ---

    // 2. Crea un nuovo conto
    fun createAccount(name: String, initialBalance: Double) {
        viewModelScope.launch {
            dao.insertAccount(Account(name = name, initialBalance = initialBalance))
        }
    }

    // 3. Modifica il valore iniziale di un conto esistente
    fun updateAccountInitialBalance(account: Account, newInitialBalance: Double) {
        viewModelScope.launch {
            dao.updateAccount(account.copy(initialBalance = newInitialBalance))
        }
    }

    // --- GESTIONE TRANSAZIONI ---

    // 4. Salva una spesa richiedendo esplicitamente l'ID del conto
    fun saveQuickTransaction(categoryName: String, amount: Double, isExpense: Boolean, accountId: Long) {
        viewModelScope.launch {
            // 1. Cerca se esiste già una categoria con quel nome esatto e quello stesso tipo (entrata/uscita)
            val existingCategory = allCategories.value.find {
                it.name.trim().equals(categoryName.trim(), ignoreCase = true) && it.isExpense == isExpense
            }

            // 2. Se esiste prende il suo ID, altrimenti la crea sul momento
            val categoryId = if (existingCategory != null) {
                existingCategory.id
            } else {
                val newCategory = Category(name = categoryName.trim(), isExpense = isExpense)
                dao.insertCategory(newCategory) // Restituisce il nuovo ID generato
            }

            // 3. Salva la transazione
            val transaction = Transaction(
                title = categoryName.trim(), // Usiamo il nome della categoria come titolo visibile
                amount = amount,
                date = System.currentTimeMillis(),
                categoryId = categoryId,
                accountId = accountId
            )
            dao.insertTransaction(transaction)
        }
    }

    fun deleteAccount(account: Account) {
        viewModelScope.launch {
            dao.deleteAccount(account)
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            dao.deleteTransaction(transaction)
        }
    }

    fun updateExistingTransaction(
        transaction: Transaction,
        categoryName: String,
        newAmount: Double,
        isExpense: Boolean,
        newAccountId: Long,
        newDateString: String
    ) {
        viewModelScope.launch {
            val existingCategory = allCategories.value.find {
                it.name.trim().equals(categoryName.trim(), ignoreCase = true) && it.isExpense == isExpense
            }

            val categoryId = if (existingCategory != null) {
                existingCategory.id
            } else {
                val newCategory = Category(name = categoryName.trim(), isExpense = isExpense)
                dao.insertCategory(newCategory)
            }

            val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
            val parsedDate = try {
                sdf.parse(newDateString)?.time ?: transaction.date
            } catch (e: Exception) {
                transaction.date
            }

            val updatedTx = transaction.copy(
                title = categoryName.trim(),
                amount = newAmount,
                date = parsedDate,
                categoryId = categoryId,
                accountId = newAccountId
            )
            dao.updateTransaction(updatedTx)
        }
    }

    // 5. Riconciliazione (Aggiustamento automatico) per un conto specifico
    fun adjustAccountBalance(account: Account, targetBalance: Double, currentTotal: Double) {
        val difference = targetBalance - currentTotal

        if (difference != 0.0) {
            val isExpense = difference < 0

            saveQuickTransaction(
                categoryName = "Balance Adjustment",
                amount = abs(difference),
                isExpense = isExpense,
                accountId = account.id,
            )
        }
    }
}

class FinanceViewModelFactory(private val dao: FinanceDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FinanceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FinanceViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}