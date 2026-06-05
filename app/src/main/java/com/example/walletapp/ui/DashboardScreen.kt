package com.example.walletapp.ui

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.walletapp.data.Account
import com.example.walletapp.data.Category
import com.example.walletapp.data.Transaction
import com.example.walletapp.viewmodel.FinanceViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(
    viewModel: FinanceViewModel,
    onNavigateToDetail: (Long?) -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val transactions by viewModel.allTransactions.collectAsState()
    val categories by viewModel.allCategories.collectAsState()
    val accounts by viewModel.allAccounts.collectAsState()

    var showTransactionDialog by remember { mutableStateOf(false) }
    var showAccountDialog by remember { mutableStateOf(false) }
    var accountToAdjust by remember { mutableStateOf<Account?>(null) }

    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var accountToDelete by remember { mutableStateOf<Account?>(null) }

    var showDeleteTransactionDialog by remember { mutableStateOf(false) }
    var transactionToDelete by remember { mutableStateOf<Transaction?>(null) }

    var showEditTransactionDialog by remember { mutableStateOf(false) }
    var transactionToEdit by remember { mutableStateOf<Transaction?>(null) }

    // Calcolo dei saldi correnti per ciascun conto
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
            // SETTINGS + DASHBOARD + PROFILE
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateToSettings) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Settings,
                        contentDescription = "Settings"
                    )
                }

                Text(
                    text = "Dashboard",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                IconButton(onClick = onNavigateToProfile) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Person,
                        contentDescription = "Profile"
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            // --- GLOBAL TOTAL ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToDetail(null) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Total Amount", fontSize = 14.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = String.format("€ %.2f", totalAmount), fontSize = 36.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- SECTION 1: ACCOUNTS CAROUSEL (LAZYROW) ---
            Text(text = "My Accounts", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(accounts) { account ->
                    val currentBalance = accountBalances[account.id] ?: 0.0
                    val accountTransactions = transactions.filter { it.accountId == account.id }

                    Card(
                        modifier = Modifier
                            .width(165.dp)
                            .height(135.dp),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        // Il Box ci permette di lavorare a livelli sovrapposti
                        Box(modifier = Modifier.fillMaxSize()) {

                            // LIVELLO 1 (SOTTO): L'area sensibile per il click/long-click della pagina
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .combinedClickable(
                                        onClick = { onNavigateToDetail(account.id) },
                                        onLongClick = {
                                            accountToDelete = account
                                            showDeleteAccountDialog = true
                                        }
                                    )
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Titolo spostato un po' a sinistra per non accavallarsi alla matita
                                Text(
                                    text = account.name,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(end = 24.dp)
                                )

                                Text(
                                    text = String.format("€ %.2f", currentBalance),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                )

                                AccountSparkline(
                                    transactions = accountTransactions,
                                    categories = categories,
                                    initialBalance = account.initialBalance,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(35.dp)
                                        .padding(vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                // Bottone Add Account
                item {
                    OutlinedCard(
                        onClick = { showAccountDialog = true },
                        modifier = Modifier
                            .width(165.dp)
                            .height(135.dp),
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

            val visibleTransactions = transactions.filter { it.title != "Balance Adjustment" }

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
                                    onClick = {
                                        transactionToEdit = transaction
                                        showEditTransactionDialog = true
                                    },
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

    // --- POPUPS & DIALOGS (Invariati per preservare le funzionalità) ---
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
        var categoryName by remember { mutableStateOf("") }
        var amountString by remember { mutableStateOf("") }
        var isExpense by remember { mutableStateOf(true) }
        var expandedCategoryDropdown by remember { mutableStateOf(false) }

        var selectedAccount by remember { mutableStateOf(accounts.first()) }
        var expandedDropdown by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showTransactionDialog = false },
            title = { Text("New Transaction") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Selezione Conto
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

                    // Entrata o Uscita
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text(text = if (isExpense) "🔴 Expense" else "🟢 Income", fontWeight = FontWeight.Medium)
                        Switch(checked = isExpense, onCheckedChange = { isExpense = it; categoryName = "" }) // Resetta il nome se cambi tipo
                    }

                    // Selezione o Scrittura Categoria
                    val availableCategories = categories.filter { it.isExpense == isExpense && it.name != "Balance Adjustment" }
                    ExposedDropdownMenuBox(expanded = expandedCategoryDropdown, onExpandedChange = { expandedCategoryDropdown = !expandedCategoryDropdown }) {
                        OutlinedTextField(
                            value = categoryName,
                            onValueChange = { categoryName = it; expandedCategoryDropdown = true },
                            label = { Text("Category (e.g. Groceries)") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategoryDropdown) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            singleLine = true
                        )
                        val filteredCats = availableCategories.filter { it.name.contains(categoryName, ignoreCase = true) }
                        if (filteredCats.isNotEmpty() && expandedCategoryDropdown) {
                            ExposedDropdownMenu(expanded = expandedCategoryDropdown, onDismissRequest = { expandedCategoryDropdown = false }) {
                                filteredCats.forEach { cat ->
                                    DropdownMenuItem(text = { Text(cat.name) }, onClick = { categoryName = cat.name; expandedCategoryDropdown = false })
                                }
                            }
                        }
                    }

                    // Importo
                    OutlinedTextField(value = amountString, onValueChange = { amountString = it }, label = { Text("Amount (€)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    val amount = amountString.replace(",", ".").toDoubleOrNull()
                    if (amount != null && categoryName.isNotBlank()) {
                        viewModel.saveQuickTransaction(categoryName, amount, isExpense, selectedAccount.id)
                        showTransactionDialog = false
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showTransactionDialog = false }) { Text("Cancel") } }
        )
    }

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
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteAccountDialog = false; accountToDelete = null }) { Text("Cancel") } }
        )
    }

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
            dismissButton = { TextButton(onClick = { showDeleteTransactionDialog = false; transactionToDelete = null }) { Text("Cancel") } }
        )
    }

    // --- POPUP: MODIFICA TRANSAZIONE ESISTENTE ---
    if (showEditTransactionDialog && transactionToEdit != null) {
        val tx = transactionToEdit!!
        val cat = categories.find { it.id == tx.categoryId }

        var editCategoryName by remember { mutableStateOf(tx.title) }
        var editAmountStr by remember { mutableStateOf(tx.amount.toString()) }
        var editIsExpense by remember { mutableStateOf(cat?.isExpense ?: true) }
        var expandedEditCategoryDropdown by remember { mutableStateOf(false) }

        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        var editDateStr by remember { mutableStateOf(sdf.format(Date(tx.date))) }

        var editSelectedAccount by remember { mutableStateOf(accounts.find { it.id == tx.accountId } ?: accounts.first()) }
        var expandedEditDropdown by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showEditTransactionDialog = false; transactionToEdit = null },
            title = { Text("Edit Transaction") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (accounts.isNotEmpty()) {
                        ExposedDropdownMenuBox(expanded = expandedEditDropdown, onExpandedChange = { expandedEditDropdown = !expandedEditDropdown }) {
                            OutlinedTextField(
                                value = editSelectedAccount.name, onValueChange = {}, readOnly = true, label = { Text("Account") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedEditDropdown) }, modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(expanded = expandedEditDropdown, onDismissRequest = { expandedEditDropdown = false }) {
                                accounts.forEach { account ->
                                    DropdownMenuItem(text = { Text(account.name) }, onClick = { editSelectedAccount = account; expandedEditDropdown = false })
                                }
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text(text = if (editIsExpense) "🔴 Expense" else "🟢 Income", fontWeight = FontWeight.Medium)
                        Switch(checked = editIsExpense, onCheckedChange = { editIsExpense = it })
                    }

                    val availableCategories = categories.filter { it.isExpense == editIsExpense && it.name != "Balance Adjustment" }
                    ExposedDropdownMenuBox(expanded = expandedEditCategoryDropdown, onExpandedChange = { expandedEditCategoryDropdown = !expandedEditCategoryDropdown }) {
                        OutlinedTextField(
                            value = editCategoryName,
                            onValueChange = { editCategoryName = it; expandedEditCategoryDropdown = true },
                            label = { Text("Category") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedEditCategoryDropdown) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            singleLine = true
                        )
                        val filteredCats = availableCategories.filter { it.name.contains(editCategoryName, ignoreCase = true) }
                        if (filteredCats.isNotEmpty() && expandedEditCategoryDropdown) {
                            ExposedDropdownMenu(expanded = expandedEditCategoryDropdown, onDismissRequest = { expandedEditCategoryDropdown = false }) {
                                filteredCats.forEach { c ->
                                    DropdownMenuItem(text = { Text(c.name) }, onClick = { editCategoryName = c.name; expandedEditCategoryDropdown = false })
                                }
                            }
                        }
                    }

                    OutlinedTextField(value = editAmountStr, onValueChange = { editAmountStr = it }, label = { Text("Amount (€)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = editDateStr, onValueChange = { editDateStr = it }, label = { Text("Date (dd/MM/yyyy)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    val amount = editAmountStr.replace(",", ".").toDoubleOrNull()
                    if (amount != null && editCategoryName.isNotBlank()) {
                        viewModel.updateExistingTransaction(
                            transaction = tx,
                            categoryName = editCategoryName,
                            newAmount = amount,
                            isExpense = editIsExpense,
                            newAccountId = editSelectedAccount.id,
                            newDateString = editDateStr
                        )
                        showEditTransactionDialog = false
                        transactionToEdit = null
                    }
                }) { Text("Update") }
            },
            dismissButton = { TextButton(onClick = { showEditTransactionDialog = false; transactionToEdit = null }) { Text("Cancel") } }
        )
    }
}

// --- COMPONENTE SPARKLINE: MINI GRAFICO A CURVA FLUIDA SENZA PUNTI ---
@Composable
fun AccountSparkline(transactions: List<Transaction>, categories: List<Category>, initialBalance: Double, modifier: Modifier = Modifier) {
    val sortedTx = transactions.filter { it.title != "Balance Adjustment" }.sortedBy { it.date }

    var currentRunningBalance = initialBalance
    val balancePoints = mutableListOf<Double>()
    balancePoints.add(currentRunningBalance)

    for (tx in sortedTx) {
        val cat = categories.find { it.id == tx.categoryId }
        if (cat?.isExpense == true) {
            currentRunningBalance -= tx.amount
        } else {
            currentRunningBalance += tx.amount
        }
        balancePoints.add(currentRunningBalance)
    }

    // Prendiamo gli ultimi 6 punti storici per mantenere la linea leggibile ma reattiva
    val graphData = balancePoints.takeLast(6)
    val lineColor = MaterialTheme.colorScheme.primary

    Canvas(modifier = modifier) {
        // Disegnamo la curva solo se abbiamo almeno due punti distinti, altrimenti facciamo una linea piatta
        if (graphData.size >= 2) {
            val maxVal = graphData.maxOrNull() ?: 0.0
            val minVal = graphData.minOrNull() ?: 0.0
            val deltaY = if (maxVal == minVal) 1.0 else (maxVal - minVal) * 1.2

            val spaceX = size.width / (graphData.size - 1)
            val path = Path()
            val coordinates = mutableListOf<Offset>()

            graphData.forEachIndexed { index, balance ->
                val x = index * spaceX
                val y = size.height - (((balance - minVal) / deltaY) * size.height).toFloat()
                coordinates.add(Offset(x, y))
            }

            val firstPoint = coordinates.first()
            path.moveTo(firstPoint.x, firstPoint.y)

            for (i in 0 until coordinates.size - 1) {
                val p1 = coordinates[i]
                val p2 = coordinates[i + 1]

                // Calcolo dell'interpolazione cubica per ammorbidire la linea
                val controlPoint1 = Offset((p1.x + p2.x) / 2f, p1.y)
                val controlPoint2 = Offset((p1.x + p2.x) / 2f, p2.y)

                path.cubicTo(
                    controlPoint1.x, controlPoint1.y,
                    controlPoint2.x, controlPoint2.y,
                    p2.x, p2.y
                )
            }

            drawPath(
                path = path,
                color = lineColor.copy(alpha = 0.7f), // Un po' più opaco per integrarsi elegantemente
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )
        } else {
            // Se il conto è appena nato o non ha transazioni, tracciamo una linea retta a metà altezza
            val midY = size.height / 2f
            drawLine(
                color = lineColor.copy(alpha = 0.3f),
                start = Offset(0f, midY),
                end = Offset(size.width, midY),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}