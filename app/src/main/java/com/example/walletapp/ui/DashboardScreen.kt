package com.example.walletapp.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.walletapp.data.Account
import com.example.walletapp.data.Transaction
import com.example.walletapp.viewmodel.FinanceViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(viewModel: FinanceViewModel) {
    val transactions by viewModel.allTransactions.collectAsState()
    val categories by viewModel.allCategories.collectAsState()
    val accounts by viewModel.allAccounts.collectAsState()

    // Stati per i popup standard
    var showTransactionDialog by remember { mutableStateOf(false) }
    var showAccountDialog by remember { mutableStateOf(false) }
    var showAdjustDialog by remember { mutableStateOf(false) }
    var accountToAdjust by remember { mutableStateOf<Account?>(null) }

    // NUOVI STATI: Per gestire le eliminazioni con la conferma
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var accountToDelete by remember { mutableStateOf<Account?>(null) }

    var showDeleteTransactionDialog by remember { mutableStateOf(false) }
    var transactionToDelete by remember { mutableStateOf<Transaction?>(null) }

    // Calcolo dei saldi
    val accountBalances = accounts.associate { account ->
        val accountTransactions = transactions.filter { it.accountId == account.id }
        val netAmount = accountTransactions.sumOf { tx ->
            val category = categories.find { it.id == tx.categoryId }
            if (category?.isExpense == true) -tx.amount else tx.amount
        }
        account.id to (account.initialBalance + netAmount)
    }

    val totalAmount = accountBalances.values.sum()

    Scaffold(
        floatingActionButton = {
            if (accounts.isNotEmpty()) {
                FloatingActionButton(
                    onClick = { showTransactionDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Transaction", tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(top = 16.dp, start = 16.dp, end = 16.dp)
        ) {
            // --- HEADER ---
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Wallet App", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Total Amount", fontSize = 14.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = String.format("€ %.2f", totalAmount), fontSize = 36.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- SECTION 1: ACCOUNTS CAROUSEL ---
            Text(text = "My Accounts", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(accounts) { account ->
                    val currentBalance = accountBalances[account.id] ?: 0.0
                    Card(
                        modifier = Modifier
                            .width(160.dp)
                            .height(110.dp)
                            // Utilizziamo combinedClickable per catturare il clic prolungato sul conto
                            .combinedClickable(
                                onClick = { /* Clic normale, implementabile in futuro */ },
                                onLongClick = {
                                    accountToDelete = account
                                    showDeleteAccountDialog = true
                                }
                            ),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = account.name, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Adjust",
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable {
                                            accountToAdjust = account
                                            showAdjustDialog = true
                                        },
                                    tint = Color.Gray
                                )
                            }
                            Text(text = String.format("€ %.2f", currentBalance), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                    }
                }

                item {
                    OutlinedCard(
                        onClick = { showAccountDialog = true },
                        modifier = Modifier
                            .width(160.dp)
                            .height(110.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Account", tint = Color.Gray)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "Add Account", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- SECTION 2: INCOME/OUTCOME LIST ---
            Text(text = "Income/Outcome List", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            // Creiamo una lista "visiva" che esclude gli aggiustamenti automatici
            val visibleTransactions = transactions.filter { it.title != "Balance Adjustment" }

            // Usiamo visibleTransactions invece di transactions per disegnare la lista
            if (visibleTransactions.isEmpty()) {
                Box(modifier = Modifier.weight(1.0f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(text = "No transactions yet.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1.0f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(visibleTransactions) { transaction ->
                        val account = accounts.find { it.id == transaction.accountId }
                        val category = categories.find { it.id == transaction.categoryId }
                        val isExpense = category?.isExpense == true

                        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        val dateString = sdf.format(Date(transaction.date))

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = { /* Clic normale */ },
                                    onLongClick = {
                                        transactionToDelete = transaction
                                        showDeleteTransactionDialog = true
                                    }
                                ),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = transaction.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text(text = account?.name ?: "Unknown Account", fontSize = 12.sp, color = Color.Gray)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = String.format(if (isExpense) "- %.2f €" else "+ %.2f €", transaction.amount),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = if (isExpense) Color(0xFFD32F2F) else Color(0xFF388E3C)
                                    )
                                    Text(text = dateString, fontSize = 11.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- DIALOGS DI CREAZIONE E MODIFICA (Invariati) ---
    if (showAccountDialog) {
        var accountName by remember { mutableStateOf("") }
        var initBalanceStr by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAccountDialog = false },
            title = { Text("New Account") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = accountName, onValueChange = { accountName = it }, label = { Text("Account Name") }, singleLine = true)
                    OutlinedTextField(value = initBalanceStr, onValueChange = { initBalanceStr = it }, label = { Text("Initial Balance (€)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = {
                    val initialBalance = initBalanceStr.replace(",", ".").toDoubleOrNull() ?: 0.0
                    if (accountName.isNotBlank()) {
                        viewModel.createAccount(accountName, initialBalance)
                        showAccountDialog = false
                    }
                }) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showAccountDialog = false }) { Text("Cancel") } }
        )
    }

    if (showTransactionDialog && accounts.isNotEmpty()) {
        var title by remember { mutableStateOf("") }
        var amountString by remember { mutableStateOf("") }
        var isExpense by remember { mutableStateOf(true) }
        var selectedAccount by remember { mutableStateOf(accounts.first()) }
        var expandedDropdown by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showTransactionDialog = false },
            title = { Text("New Transaction") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExposedDropdownMenuBox(expanded = expandedDropdown, onExpandedChange = { expandedDropdown = !expandedDropdown }) {
                        OutlinedTextField(
                            value = selectedAccount.name, onValueChange = {}, readOnly = true, label = { Text("Select Account") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) }, modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = expandedDropdown, onDismissRequest = { expandedDropdown = false }) {
                            accounts.forEach { account ->
                                DropdownMenuItem(text = { Text(account.name) }, onClick = { selectedAccount = account; expandedDropdown = false })
                            }
                        }
                    }
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = amountString, onValueChange = { amountString = it }, label = { Text("Amount (€)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                        Text(text = if (isExpense) "🔴 Expense" else "🟢 Income")
                        Switch(checked = isExpense, onCheckedChange = { isExpense = it })
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val amount = amountString.replace(",", ".").toDoubleOrNull()
                    if (amount != null && title.isNotBlank()) {
                        viewModel.saveQuickTransaction(title, amount, isExpense, selectedAccount.id)
                        showTransactionDialog = false
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showTransactionDialog = false }) { Text("Cancel") } }
        )
    }

    accountToAdjust?.let { account ->
        if (showAdjustDialog) {
            var targetBalanceStr by remember { mutableStateOf("") }
            val currentCalculatedBalance = accountBalances[account.id] ?: 0.0

            AlertDialog(
                onDismissRequest = { showAdjustDialog = false },
                title = { Text("Adjust ${account.name} Balance") },
                text = {
                    Column {
                        Text("Enter the actual amount in this account.", fontSize = 13.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = targetBalanceStr, onValueChange = { targetBalanceStr = it }, label = { Text("Actual Balance (€)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val targetBalance = targetBalanceStr.replace(",", ".").toDoubleOrNull()
                        if (targetBalance != null) {
                            viewModel.adjustAccountBalance(account, targetBalance, currentCalculatedBalance)
                            showAdjustDialog = false
                            accountToAdjust = null
                        }
                    }) { Text("Update") }
                },
                dismissButton = { TextButton(onClick = { showAdjustDialog = false }) { Text("Cancel") } }
            )
        }
    }

    // --- CONFERMA ELIMINAZIONE CONTO ---
    if (showDeleteAccountDialog && accountToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false; accountToDelete = null },
            title = { Text("Delete Account") },
            text = { Text("Are you sure you want to delete \"${accountToDelete?.name}\"? This will also permanently delete all its associated transactions.") },
            confirmButton = {
                Button(
                    onClick = {
                        accountToDelete?.let { viewModel.deleteAccount(it) }
                        showDeleteAccountDialog = false
                        accountToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error) // Rosso per evidenziare il pericolo
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountDialog = false; accountToDelete = null }) { Text("Cancel") }
            }
        )
    }

    // --- CONFERMA ELIMINAZIONE TRANSAZIONE ---
    if (showDeleteTransactionDialog && transactionToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteTransactionDialog = false; transactionToDelete = null },
            title = { Text("Delete Transaction") },
            text = { Text("Are you sure you want to delete \"${transactionToDelete?.title}\"?") },
            confirmButton = {
                Button(
                    onClick = {
                        transactionToDelete?.let { viewModel.deleteTransaction(it) }
                        showDeleteTransactionDialog = false
                        transactionToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteTransactionDialog = false; transactionToDelete = null }) { Text("Cancel") }
            }
        )
    }
}