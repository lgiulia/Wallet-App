package com.example.walletapp.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.walletapp.data.Category
import com.example.walletapp.data.Transaction
import com.example.walletapp.viewmodel.FinanceViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.draw.blur
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.draw.clip

enum class TrendPeriod(val label: String) {
    WEEK("last week"),
    MONTH("last month"),
    THREE_MONTHS("last 3 months"),
    SIX_MONTHS("last 6 months"),
    YEAR("last year")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AccountDetailScreen(
    viewModel: FinanceViewModel,
    accountId: Long?,
    onBack: () -> Unit
) {

    val rawTransactions by viewModel.allTransactions.collectAsState()
    val transactions = rawTransactions.sortedWith(
        compareByDescending<Transaction> { it.date }
            .thenByDescending { it.id }
    )
    val categories by viewModel.allCategories.collectAsState()
    val accounts by viewModel.allAccounts.collectAsState()

    val appCurrency by viewModel.appCurrency.collectAsState()
    val appDateFormat by viewModel.appDateFormat.collectAsState()

    val currencySymbol = appCurrency.substringBefore(" ")
    val datePattern = if (appDateFormat == "MM/DD/YYYY") "MM/dd/yyyy" else "dd/MM/yyyy"

    var showTransactionDialog by remember { mutableStateOf(false) }
    var showAdjustDialog by remember { mutableStateOf(false) }
    var showDeleteTransactionDialog by remember { mutableStateOf(false) }
    var transactionToDelete by remember { mutableStateOf<Transaction?>(null) }
    var showEditTransactionDialog by remember { mutableStateOf(false) }
    var transactionToEdit by remember { mutableStateOf<Transaction?>(null) }

    var categoryName by remember { mutableStateOf("") }
    var isExpense by remember { mutableStateOf(true) }
    var showNewCategoryDialog by remember { mutableStateOf(false) }

    var selectedPeriod by remember { mutableStateOf(TrendPeriod.MONTH) }
    var expandedPeriodDropdown by remember { mutableStateOf(false) }

    val currentAccount = accounts.find { it.id == accountId }

    val hideTotalBalance by viewModel.hideTotalBalance.collectAsState()
    val hiddenAccounts by viewModel.hiddenAccounts.collectAsState()

    val isHidden = if (accountId == null) hideTotalBalance else (hideTotalBalance || hiddenAccounts.contains(accountId))

    val filteredTransactions = if (accountId == null) {
        transactions
    } else {
        transactions.filter { it.accountId == accountId }
    }

    val displayTitle = currentAccount?.name ?: "All Accounts Total"

    // Base di partenza per il calcolo del saldo corrente
    val initialBalanceScope = if (accountId == null) accounts.sumOf { it.initialBalance } else (currentAccount?.initialBalance ?: 0.0)

    val specificBalance = initialBalanceScope + filteredTransactions.sumOf { tx ->
        val cat = categories.find { it.id == tx.categoryId }
        if (cat?.isExpense == true) -tx.amount else tx.amount
    }

    // --- LOGICA DEL TREND PERCENTUALE DINAMICA ---
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)

    val currentPeriodStart: Long
    val prevPeriodStart: Long

    // Calcolo degli intervalli di tempo in base alla scelta dell'utente
    when (selectedPeriod) {
        TrendPeriod.WEEK -> {
            cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
            currentPeriodStart = cal.timeInMillis
            cal.add(Calendar.WEEK_OF_YEAR, -1)
            prevPeriodStart = cal.timeInMillis
        }
        TrendPeriod.MONTH -> {
            cal.set(Calendar.DAY_OF_MONTH, 1)
            currentPeriodStart = cal.timeInMillis
            cal.add(Calendar.MONTH, -1)
            prevPeriodStart = cal.timeInMillis
        }
        TrendPeriod.THREE_MONTHS -> {
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.add(Calendar.MONTH, -2) // Include questo mese + i 2 precedenti
            currentPeriodStart = cal.timeInMillis
            cal.add(Calendar.MONTH, -3) // I 3 mesi ancora precedenti
            prevPeriodStart = cal.timeInMillis
        }
        TrendPeriod.SIX_MONTHS -> {
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.add(Calendar.MONTH, -5)
            currentPeriodStart = cal.timeInMillis
            cal.add(Calendar.MONTH, -6)
            prevPeriodStart = cal.timeInMillis
        }
        TrendPeriod.YEAR -> {
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.add(Calendar.MONTH, -11)
            currentPeriodStart = cal.timeInMillis
            cal.add(Calendar.MONTH, -12)
            prevPeriodStart = cal.timeInMillis
        }
    }

    // Calcola il netto del periodo corrente (Incomes - Expenses)
    val currentPeriodNet = filteredTransactions.filter { it.date >= currentPeriodStart && it.title != "Balance Adjustment" }.sumOf { tx ->
        val cat = categories.find { it.id == tx.categoryId }
        if (cat?.isExpense == true) -tx.amount else tx.amount
    }

    // Calcola il netto del periodo precedente
    val prevPeriodNet = filteredTransactions.filter { it.date in prevPeriodStart..<currentPeriodStart && it.title != "Balance Adjustment" }.sumOf { tx ->
        val cat = categories.find { it.id == tx.categoryId }
        if (cat?.isExpense == true) -tx.amount else tx.amount
    }

    // Formula per calcolare la variazione percentuale
    val trendPercentage = if (prevPeriodNet == 0.0) {
        if (currentPeriodNet == 0.0) 0.0 else 100.0
    } else {
        ((currentPeriodNet - prevPeriodNet) / abs(prevPeriodNet)) * 100
    }

    // --- GESTIONE SCORRIMENTO PER IL BOTTONE FAB ---
    val listState = rememberLazyListState()
    var isFabVisible by remember { mutableStateOf(true) }
    var previousIndex by remember { mutableIntStateOf(0) }
    var previousScrollOffset by remember { mutableIntStateOf(0) }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                if (index > previousIndex) {
                    isFabVisible = false // Scorrimento verso il basso -> Nascondi
                } else if (index < previousIndex) {
                    isFabVisible = true  // Scorrimento verso l'alto -> Mostra
                } else {
                    if (offset > previousScrollOffset) {
                        isFabVisible = false // Scorrimento lento in basso
                    } else if (offset < previousScrollOffset) {
                        isFabVisible = true  // Scorrimento lento in alto
                    }
                }
                previousIndex = index
                previousScrollOffset = offset
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = displayTitle, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = isFabVisible && accounts.isNotEmpty(),
                enter = scaleIn(),
                exit = scaleOut()
            ) {
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- SALDO ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(text = "Balance", fontSize = 16.sp, color = Color.Gray)

                Spacer(modifier = Modifier.width(8.dp))

                if (accountId != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    // Pencil Icon
                    IconButton(onClick = { showAdjustDialog = true }, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Adjust Balance", tint = Color.Gray)
                    }
                }
                Spacer(modifier = Modifier.width(4.dp))
                // Eye Icon
                IconButton(
                    onClick = {
                        if (accountId == null) viewModel.toggleTotalBalanceVisibility()
                        else viewModel.toggleAccountVisibility(accountId)
                    },
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = if (isHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Toggle Visibility",
                        tint = Color.Gray
                    )
                }
            }

            Text(
                text = String.format("%s %,.2f", currencySymbol, specificBalance),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                modifier = if (isHidden) Modifier.blur(12.dp) else Modifier
            )

            // --- VISUALIZZAZIONE PERCENTUALE TREND INLINE---
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = String.format(Locale.getDefault(), "%+.1f%% compared to", trendPercentage),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (trendPercentage >= 0) Color(0xFF388E3C) else Color(0xFFD32F2F)
                )
                Box {
                    Row(
                        modifier = Modifier
                            .clickable { expandedPeriodDropdown = true }
                            .padding(vertical = 2.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedPeriod.label,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = " ▾",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    DropdownMenu(
                        expanded = expandedPeriodDropdown,
                        onDismissRequest = { expandedPeriodDropdown = false }
                    ) {
                        TrendPeriod.values().forEach { period ->
                            DropdownMenuItem(
                                text = { Text(period.label) },
                                onClick = {
                                    selectedPeriod = period
                                    expandedPeriodDropdown = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- GRAFICO A LINEE ---
            Text(
                text = "Balance History Trend",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Passa le transazioni e la base iniziale al widget della linea
            FinanceLineChart(transactions = filteredTransactions, categories = categories, initialBalance = initialBalanceScope)

            Spacer(modifier = Modifier.height(24.dp))

            // --- STORICO TRANSAZIONI ---
            Text(
                text = "Transaction History",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(8.dp))

            val visibleTransactions = filteredTransactions.filter { it.title != "Balance Adjustment" }

            if (visibleTransactions.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(text = "No transactions found.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(visibleTransactions) { transaction ->
                        val account = accounts.find { it.id == transaction.accountId }
                        val category = categories.find { it.id == transaction.categoryId }
                        val isExpense = category?.isExpense == true

                        val sdf = SimpleDateFormat(datePattern, Locale.getDefault())
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
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = transaction.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text(text = account?.name ?: "Unknown", fontSize = 12.sp, color = Color.Gray)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = String.format(if (isExpense) "- %,.2f %s" else "+ %,.2f %s", transaction.amount, currencySymbol),
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

    // --- DIALOGS ---
    if (showTransactionDialog && accounts.isNotEmpty()) {
        var amountString by remember { mutableStateOf("") }
        var expandedCategoryDropdown by remember { mutableStateOf(false) }

        var selectedAccount by remember { mutableStateOf(currentAccount ?: accounts.first()) }
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

                    // Entrata o Uscita (Lo mettiamo prima così filtra le categorie sotto)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text(text = if (isExpense) "🔴 Expense" else "🟢 Income", fontWeight = FontWeight.Medium)
                        Switch(checked = isExpense, onCheckedChange = { isExpense = it; categoryName = "" }) // Resetta il nome se cambi tipo
                    }

                    // Selezione o Scrittura Categoria
                    val availableCategories = categories.filter { it.isExpense == isExpense && it.name != "Balance Adjustment" }
                    ExposedDropdownMenuBox(expanded = expandedCategoryDropdown, onExpandedChange = { expandedCategoryDropdown = !expandedCategoryDropdown }) {
                        OutlinedTextField(
                            value = categoryName,
                            onValueChange = {}, // vuoto perchè non appare più la tastiera
                            readOnly = true, // Blocca la tastiera
                            label = { Text("Category (e.g. Groceries)") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategoryDropdown) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            singleLine = true
                        )
                        ExposedDropdownMenu(expanded = expandedCategoryDropdown, onDismissRequest = { expandedCategoryDropdown = false }) {
                            // Mostra le categorie esistenti
                            availableCategories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.name) },
                                    onClick = {
                                        categoryName = cat.name;
                                        expandedCategoryDropdown = false
                                    }
                                )
                            }
                            // Linea di separazione
                            if (availableCategories.isNotEmpty()) {
                                HorizontalDivider()
                            }
                            // Bottone per aggiungere nuova categoria
                            DropdownMenuItem(
                                text = { Text("+ Add new category...", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) },
                                onClick = { expandedCategoryDropdown = false
                                    showNewCategoryDialog = true }
                            )
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

    if (showAdjustDialog && currentAccount != null) {
        var targetBalanceStr by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAdjustDialog = false },
            title = { Text("Adjust ${currentAccount.name} Balance") },
            text = {
                OutlinedTextField(value = targetBalanceStr, onValueChange = { targetBalanceStr = it }, label = { Text("Actual Balance (€)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
            },
            confirmButton = {
                Button(onClick = {
                    val targetBalance = targetBalanceStr.replace(",", ".").toDoubleOrNull()
                    if (targetBalance != null) {
                        viewModel.adjustAccountBalance(currentAccount, targetBalance, specificBalance)
                        showAdjustDialog = false
                    }
                }) { Text("Update") }
            },
            dismissButton = { TextButton(onClick = { showAdjustDialog = false }) { Text("Cancel") } }
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

                        // Si legge la data base inserita dall'utente dalla stringa
                        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        val parsedUserDate = sdf.parse(editDateStr) ?: Date(tx.date)
                        // Si estrae l'orario esatto dalla transazione originale
                        val oldTime = Calendar.getInstance().apply { timeInMillis = tx.date }
                        // Si crea la nuova data con l'orario originale
                        val finalCalendar = Calendar.getInstance().apply {
                            time = parsedUserDate
                            set(Calendar.HOUR_OF_DAY, oldTime.get(Calendar.HOUR_OF_DAY))
                            set(Calendar.MINUTE, oldTime.get(Calendar.MINUTE))
                            set(Calendar.SECOND, oldTime.get(Calendar.SECOND))
                            set(Calendar.MILLISECOND, oldTime.get(Calendar.MILLISECOND))
                        }
                        // Risultato finale in millisecondi
                        val newTimestamp = finalCalendar.timeInMillis

                        viewModel.updateExistingTransaction(
                            transaction = tx,
                            categoryName = editCategoryName,
                            newAmount = amount,
                            isExpense = editIsExpense,
                            newAccountId = editSelectedAccount.id,
                            newDate = newTimestamp
                        )
                        showEditTransactionDialog = false
                        transactionToEdit = null
                    }
                }) { Text("Update") }
            },
            dismissButton = { TextButton(onClick = { showEditTransactionDialog = false; transactionToEdit = null }) { Text("Cancel") } }
        )
    }

    // POPUP CREATE NEW CATEGORY
    if (showNewCategoryDialog) {
        var newCatName by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showNewCategoryDialog = false },
            title = { Text("New Category") },
            text = {
                OutlinedTextField(
                    value = newCatName,
                    onValueChange = { newCatName = it },
                    label = { Text("Category Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newCatName.isNotBlank()) {
                        viewModel.addCategory(newCatName, isExpense)
                        categoryName = newCatName
                        showNewCategoryDialog = false
                    }
                }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showNewCategoryDialog = false }) { Text("Cancel") }
            }
        )
    }
}

// --- WIDGET GRAFICO A LINEE CON CANVAS ---
@Composable
fun FinanceLineChart(transactions: List<Transaction>, categories: List<Category>, initialBalance: Double) {
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

    val graphData = balancePoints.takeLast(10)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
    ) {
        if (graphData.size < 2) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Add more transactions to view the line trend", color = Color.Gray, fontSize = 13.sp)
            }
        } else {
            val lineColor = MaterialTheme.colorScheme.primary

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                val maxVal = graphData.maxOrNull() ?: 0.0
                val minVal = graphData.minOrNull() ?: 0.0

                // "Padding" matematico per non far toccare alla linea i bordi estremi
                val deltaY = if (maxVal == minVal) 1.0 else (maxVal - minVal) * 1.2
                val yOffset = if (maxVal == minVal) 0.0 else (maxVal - minVal) * 0.1

                val spaceX = size.width / (graphData.size - 1)

                val strokePath = Path()
                val fillPath = Path()
                val coordinates = mutableListOf<Offset>()

                // 1. Calcola tutte le coordinate X e Y
                graphData.forEachIndexed { index, balance ->
                    val x = index * spaceX
                    val y = size.height - (((balance - minVal + yOffset) / deltaY) * size.height).toFloat()
                    coordinates.add(Offset(x, y))
                }

                // 2. Disegna le curve fluide (Bézier)
                if (coordinates.isNotEmpty()) {
                    val firstPoint = coordinates.first()
                    strokePath.moveTo(firstPoint.x, firstPoint.y)
                    fillPath.moveTo(firstPoint.x, firstPoint.y)

                    for (i in 0 until coordinates.size - 1) {
                        val p1 = coordinates[i]
                        val p2 = coordinates[i + 1]

                        // Punti di controllo per rendere la curva orizzontale in corrispondenza del nodo
                        val controlPoint1 = Offset((p1.x + p2.x) / 2f, p1.y)
                        val controlPoint2 = Offset((p1.x + p2.x) / 2f, p2.y)

                        strokePath.cubicTo(
                            controlPoint1.x, controlPoint1.y,
                            controlPoint2.x, controlPoint2.y,
                            p2.x, p2.y
                        )

                        fillPath.cubicTo(
                            controlPoint1.x, controlPoint1.y,
                            controlPoint2.x, controlPoint2.y,
                            p2.x, p2.y
                        )
                    }

                    // 3. Chiude il tracciato per la sfumatura sotto la linea
                    val lastPoint = coordinates.last()
                    fillPath.lineTo(lastPoint.x, size.height)
                    fillPath.lineTo(firstPoint.x, size.height)
                    fillPath.close()

                    // Disegna la sfumatura
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                lineColor.copy(alpha = 0.4f), // Molto visibile in alto
                                Color.Transparent             // Svanisce verso il basso
                            ),
                            startY = 0f,
                            endY = size.height
                        )
                    )

                    // Disegna la linea solida curva
                    drawPath(
                        path = strokePath,
                        color = lineColor,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // 4. Disegna i classici puntini sopra la curva
                    coordinates.forEach { centerOffset ->
                        // Cerchio esterno del colore della linea
                        drawCircle(
                            color = lineColor,
                            radius = 5.dp.toPx(),
                            center = centerOffset
                        )
                        // Cerchio interno
                        drawCircle(
                            color = Color.White,
                            radius = 2.5.dp.toPx(),
                            center = centerOffset
                        )
                    }
                }
            }
        }
    }
}