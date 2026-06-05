package com.example.walletapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.walletapp.SupabaseClient
import com.example.walletapp.data.Account
import com.example.walletapp.data.Category
import com.example.walletapp.data.PreferencesManager
import com.example.walletapp.data.Transaction
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.abs

// --- CLASSI DTO PER L'INSERIMENTO SUL CLOUD (Senza campo ID) ---
@Serializable
data class AccountInsert(
    val name: String,
    @SerialName("initial_balance") val initialBalance: Double
)

@Serializable
data class CategoryInsert(
    val name: String,
    @SerialName("is_expense") val isExpense: Boolean
)

@Serializable
data class TransactionInsert(
    val title: String,
    val amount: Double,
    val date: Long,
    @SerialName("category_id") val categoryId: Long,
    @SerialName("account_id") val accountId: Long
)

class FinanceViewModel(application: Application) : AndroidViewModel(application) {

    private val db = SupabaseClient.client.postgrest

    private val prefs = PreferencesManager(application)
    private val _allAccounts = MutableStateFlow<List<Account>>(emptyList())
    val allAccounts: StateFlow<List<Account>> = _allAccounts.asStateFlow()

    private val _allCategories = MutableStateFlow<List<Category>>(emptyList())
    val allCategories: StateFlow<List<Category>> = _allCategories.asStateFlow()

    private val _allTransactions = MutableStateFlow<List<Transaction>>(emptyList())
    val allTransactions: StateFlow<List<Transaction>> = _allTransactions.asStateFlow()

    val appTheme: StateFlow<String> = prefs.themeFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "System Default"
    )
    val appCurrency: StateFlow<String> = prefs.currencyFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "€ (Euro)"
    )

    val appDateFormat: StateFlow<String> = prefs.dateFormatFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "DD/MM/YYYY"
    )

    init {
        fetchData()
    }

    private fun fetchData() {
        viewModelScope.launch {
            try {
                _allAccounts.value = db["accounts"].select().decodeList<Account>()
                _allCategories.value = db["categories"].select().decodeList<Category>()
                _allTransactions.value = db["transactions"].select().decodeList<Transaction>()
            } catch (e: Exception) {
                println("Errore di sincronizzazione: ${e.message}")
            }
        }
    }

    fun setAppTheme(theme: String) {
        viewModelScope.launch { prefs.saveTheme(theme)}
    }

    fun setAppCurrency(currency: String) {
        viewModelScope.launch {prefs.saveCurrency(currency)}
    }

    fun setAppDateFormat(format: String) {
        viewModelScope.launch {prefs.saveDateFormat(format)}
    }

    // --- GESTIONE CONTI ---
    fun createAccount(name: String, initialBalance: Double) {
        viewModelScope.launch {
            // Usiamo il nostro DTO sicuro invece della mappa
            val newAccount = AccountInsert(name = name, initialBalance = initialBalance)
            db["accounts"].insert(newAccount)
            fetchData()
        }
    }

    fun deleteAccount(account: Account) {
        viewModelScope.launch {
            db["accounts"].delete { filter { eq("id", account.id) } }
            fetchData()
        }
    }

    // --- GESTIONE TRANSAZIONI E CATEGORIE ---
    fun saveQuickTransaction(categoryName: String, amount: Double, isExpense: Boolean, accountId: Long) {
        viewModelScope.launch {
            var category = _allCategories.value.find {
                it.name.trim().equals(categoryName.trim(), ignoreCase = true) && it.isExpense == isExpense
            }

            if (category == null) {
                val newCat = CategoryInsert(name = categoryName.trim(), isExpense = isExpense)
                category = db["categories"].insert(newCat) { select() }.decodeSingle<Category>()
            }

            val newTx = TransactionInsert(
                title = category.name,
                amount = amount,
                date = System.currentTimeMillis(),
                categoryId = category.id,
                accountId = accountId
            )
            db["transactions"].insert(newTx)
            fetchData()
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
            var category = _allCategories.value.find {
                it.name.trim().equals(categoryName.trim(), ignoreCase = true) && it.isExpense == isExpense
            }

            if (category == null) {
                val newCat = CategoryInsert(name = categoryName.trim(), isExpense = isExpense)
                category = db["categories"].insert(newCat) { select() }.decodeSingle<Category>()
            }

            val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
            val parsedDate = try {
                sdf.parse(newDateString)?.time ?: transaction.date
            } catch (e: Exception) {
                transaction.date
            }

            val updatedTx = TransactionInsert(
                title = category.name,
                amount = newAmount,
                date = parsedDate,
                categoryId = category.id,
                accountId = newAccountId
            )

            db["transactions"].update(updatedTx) { filter { eq("id", transaction.id) } }
            fetchData()
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            db["transactions"].delete { filter { eq("id", transaction.id) } }
            fetchData()
        }
    }

    // --- ALLINEAMENTO SALDO ---
    fun adjustAccountBalance(account: Account, targetBalance: Double, currentTotal: Double) {
        val difference = targetBalance - currentTotal
        if (difference != 0.0) {
            val isExpense = difference < 0
            saveQuickTransaction(
                categoryName = "Balance Adjustment",
                amount = abs(difference),
                isExpense = isExpense,
                accountId = account.id
            )
        }
    }

    // --- CATEGORIES ---
    fun addCategory(name: String, isExpense: Boolean) {
        viewModelScope.launch {
            val newCat = CategoryInsert(name = name.trim(), isExpense = isExpense)
            db["categories"].insert(newCat)
            fetchData()
        }
    }

    fun updateCategory(category: Category, newName: String, newIsExpense: Boolean) {
        viewModelScope.launch {
            val updatedCat = CategoryInsert(name = newName.trim(), isExpense = newIsExpense)
            db["categories"].update(updatedCat) { filter { eq("id", category.id) } }
            fetchData()
        }
    }
}