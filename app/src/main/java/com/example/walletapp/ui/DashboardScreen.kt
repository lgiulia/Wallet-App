package com.example.walletapp.ui

import android.widget.Toast
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
import io.github.jan.supabase.gotrue.SessionStatus
import androidx.compose.ui.platform.LocalContext
import com.example.walletapp.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.draw.blur

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(
    viewModel: FinanceViewModel,
    onNavigateToDetail: (Long?) -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val rawTransactions by viewModel.allTransactions.collectAsState()
    val transactions = rawTransactions.sortedWith(
        compareByDescending<Transaction> { it.date }
            .thenByDescending { it.id } // In caso di pareggio guarda l'id maggiore (quello creato dopo)
    )
    val categories by viewModel.allCategories.collectAsState()
    val accounts by viewModel.allAccounts.collectAsState()

    val appCurrency by viewModel.appCurrency.collectAsState()
    val appDateFormat by viewModel.appDateFormat.collectAsState()

    // Estrapola solo il simbolo della valuta
    val currencySymbol = appCurrency.substringBefore(" ")

    // Prepara il pattern per la data
    val datePattern = if (appDateFormat == "MM/DD/YYYY") "MM/dd/yyyy" else "dd/MM/yyyy"

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

    val context = LocalContext.current
    val sessionStatus by SupabaseClient.client.auth.sessionStatus.collectAsState()
    val isLoggedIn = sessionStatus is SessionStatus.Authenticated

    // --- GESTIONE SCORRIMENTO PER IL BOTTONE FAB ---
    val listState = rememberLazyListState()
    var isFabVisible by remember { mutableStateOf(true) }
    var previousIndex by remember { mutableIntStateOf(0) }
    var previousScrollOffset by remember { mutableIntStateOf(0) }

    var showNewCategoryDialog by remember { mutableStateOf(false) }
    var isExpense by remember { mutableStateOf(true) }
    var categoryName by remember { mutableStateOf("") }

    // Hide amounts
    val hideTotalBalance by viewModel.hideTotalBalance.collectAsState()
    val hiddenAccounts by viewModel.hiddenAccounts.collectAsState()

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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Total Amount", fontSize = 14.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { viewModel.toggleTotalBalanceVisibility() },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = if (hideTotalBalance) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle Global Visibility",
                            tint = Color.Gray
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = String.format("%s %,.2f", currencySymbol, totalAmount),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = if (hideTotalBalance) Modifier.blur(12.dp) else Modifier
                )
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
                        // Il Box permette di lavorare a livelli sovrapposti
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
                                val isAccountHidden = hideTotalBalance || hiddenAccounts.contains(account.id)

                                // Account Name and Eye Icon
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = account.name,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp
                                    )
                                    IconButton(
                                        onClick = { viewModel.toggleAccountVisibility(account.id) },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isAccountHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = "Toggle Account Visibility",
                                            tint = Color.Gray
                                        )
                                    }
                                }

                                Text(
                                    text = String.format("%s %,.2f", currencySymbol, currentBalance),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    // Sfocatura del singolo saldo se l'occhio è chiuso!
                                    modifier = if (isAccountHidden) Modifier.blur(12.dp) else Modifier
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
                        onClick = {
                            if (isLoggedIn) {
                                showAccountDialog = true
                            } else {
                                Toast.makeText(context, "You must be logged in to add an account", Toast.LENGTH_SHORT).show()
                            }
                        },
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

            // --- SECTION 2: INCOME/EXPENSE LIST ---
            Text(text = "Transaction History", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            val visibleTransactions = transactions.filter { it.title != "Balance Adjustment" }

            if (visibleTransactions.isEmpty()) {
                Box(modifier = Modifier.weight(1.0f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(text = "No transactions yet.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1.0f).fillMaxWidth(),
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

    // --- POPUPS & DIALOGS ---
    if (showAccountDialog) {
        var accountName by remember { mutableStateOf("") }
        var initBalanceStr by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAccountDialog = false },
            title = { Text("New Account") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = accountName, onValueChange = { accountName = it }, label = { Text("Account Name") }, singleLine = true)
                    OutlinedTextField(value = initBalanceStr, onValueChange = { initBalanceStr = it }, label = { Text("Initial Balance") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
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
        var amountString by remember { mutableStateOf("") }
        var expandedCategoryDropdown by remember { mutableStateOf(false) }

        var selectedAccount by remember { mutableStateOf(accounts.first()) }
        var expandedDropdown by remember { mutableStateOf(false) }

        var transactionMode by remember { mutableStateOf("EXPENSE") } // "EXPENSE", "INCOME", "TRANSFER"
        var selectedToAccount by remember { mutableStateOf(accounts.firstOrNull { it.id != selectedAccount.id } ?: accounts.first()) }
        var expandedToDropdown by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showTransactionDialog = false },
            title = { Text("New Transaction") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // SELETTORE A 3 VIE
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        FilterChip (
                            selected = transactionMode == "EXPENSE",
                            onClick = { transactionMode = "EXPENSE"; isExpense = true },
                            label = { Text("Expense") }
                        )
                        FilterChip (
                            selected = transactionMode == "INCOME",
                            onClick = { transactionMode = "INCOME"; isExpense = false },
                            label = { Text("Income") }
                        )
                        if (accounts.size > 1) {
                            FilterChip(
                                selected = transactionMode == "TRANSFER",
                                onClick = { transactionMode = "TRANSFER" },
                                label = { Text("Transfer") }
                            )
                        }
                    }

                    // FROM ACCOUNT
                    ExposedDropdownMenuBox(expanded = expandedDropdown, onExpandedChange = { expandedDropdown = !expandedDropdown }) {
                        OutlinedTextField(
                            value = selectedAccount.name, onValueChange = {}, readOnly = true,
                            label = { Text(if (transactionMode == "TRANSFER") "From Account" else "Account") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) }, modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = expandedDropdown, onDismissRequest = { expandedDropdown = false }) {
                            accounts.forEach { account ->
                                DropdownMenuItem(text = { Text(account.name) }, onClick = { selectedAccount = account; expandedDropdown = false })
                            }
                        }
                    }

                    // Logica dinamica dei campi centrali
                    if (transactionMode == "TRANSFER") {
                        // Mostra il selettore del conto di destinazione
                        ExposedDropdownMenuBox(expanded = expandedToDropdown, onExpandedChange = { expandedToDropdown = !expandedToDropdown }) {
                            OutlinedTextField(
                                value = selectedToAccount.name, onValueChange = {}, readOnly = true, label = { Text("To Account") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedToDropdown) }, modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(expanded = expandedToDropdown, onDismissRequest = { expandedToDropdown = false }) {
                                // Filtro per togliere lo stesso conto di partenza
                                accounts.filter { it.id != selectedAccount.id }.forEach { account ->
                                    DropdownMenuItem(text = { Text(account.name) }, onClick = { selectedToAccount = account; expandedToDropdown = false })
                                }
                            }
                        }
                    } else {
                        // Selezione o Scrittura Categoria
                        val availableCategories = categories.filter { it.isExpense == (transactionMode == "EXPENSE") && it.name != "Balance Adjustment" && it.name != "Transfer In" && it.name != "Transfer Out" }
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
                    }

                    // Importo
                    OutlinedTextField(value = amountString, onValueChange = { amountString = it }, label = { Text("Amount (€)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    val amount = amountString.replace(",", ".").toDoubleOrNull()
                    if (amount != null) {
                        if (transactionMode == "TRANSFER") {
                            viewModel.saveTransferTransaction(selectedAccount, selectedToAccount, amount)
                            showTransactionDialog = false
                        } else if (categoryName.isNotBlank()) {
                            viewModel.saveQuickTransaction(categoryName, amount, transactionMode == "EXPENSE", selectedAccount.id)
                            showTransactionDialog = false
                        }
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

                    OutlinedTextField(value = editAmountStr, onValueChange = { editAmountStr = it }, label = { Text("Amount") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
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

// --- COMPONENTE SPARKLINE: MINI GRAFICO SENZA PUNTI ---
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

    // Prende gli ultimi 6 punti storici
    val graphData = balancePoints.takeLast(6)
    val lineColor = MaterialTheme.colorScheme.primary

    Canvas(modifier = modifier) {
        // Disegna la curva solo se abbiamo almeno due punti distinti, altrimenti facciamo una linea piatta
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
                color = lineColor.copy(alpha = 0.7f),
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )
        } else {
            // Se il conto è appena nato o non ha transazioni, traccia una linea retta a metà altezza
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