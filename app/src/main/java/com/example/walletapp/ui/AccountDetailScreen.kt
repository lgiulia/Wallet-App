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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AccountDetailScreen(
    viewModel: FinanceViewModel,
    accountId: Long?,
    onBack: () -> Unit
) {
    val transactions by viewModel.allTransactions.collectAsState()
    val categories by viewModel.allCategories.collectAsState()
    val accounts by viewModel.allAccounts.collectAsState()

    var showTransactionDialog by remember { mutableStateOf(false) }
    var showAdjustDialog by remember { mutableStateOf(false) }
    var showDeleteTransactionDialog by remember { mutableStateOf(false) }
    var transactionToDelete by remember { mutableStateOf<Transaction?>(null) }
    var showEditTransactionDialog by remember { mutableStateOf(false) }
    var transactionToEdit by remember { mutableStateOf<Transaction?>(null) }

    val currentAccount = accounts.find { it.id == accountId }

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

    // --- LOGICA DEL TREND PERCENTUALE (Month over Month) ---
    val cal = Calendar.getInstance()

    // Inizio del mese corrente
    cal.set(Calendar.DAY_OF_MONTH, 1)
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    val currentMonthStart = cal.timeInMillis

    // Inizio del mese scorso
    cal.add(Calendar.MONTH, -1)
    val prevMonthStart = cal.timeInMillis

    // Calcoliamo il netto del mese corrente (Incomes - Expenses)
    val currentMonthNet = filteredTransactions.filter { it.date >= currentMonthStart && it.title != "Balance Adjustment" }.sumOf { tx ->
        val cat = categories.find { it.id == tx.categoryId }
        if (cat?.isExpense == true) -tx.amount else tx.amount
    }

    // Calcoliamo il netto del mese scorso
    val prevMonthNet = filteredTransactions.filter { it.date in prevMonthStart..<currentMonthStart && it.title != "Balance Adjustment" }.sumOf { tx ->
        val cat = categories.find { it.id == tx.categoryId }
        if (cat?.isExpense == true) -tx.amount else tx.amount
    }

    // Formula per calcolare la variazione percentuale
    val trendPercentage = if (prevMonthNet == 0.0) {
        if (currentMonthNet == 0.0) 0.0 else 100.0
    } else {
        ((currentMonthNet - prevMonthNet) / abs(prevMonthNet)) * 100
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- SALDO ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(text = "Balance", fontSize = 16.sp, color = Color.Gray)
                if (accountId != null) {
                    IconButton(onClick = { showAdjustDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Adjust Balance", modifier = Modifier.size(18.dp), tint = Color.Gray)
                    }
                }
            }
            Text(text = String.format("€ %.2f", specificBalance), fontSize = 32.sp, fontWeight = FontWeight.Bold)

            // --- VISUALIZZAZIONE PERCENTUALE TREND ---
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = String.format(Locale.getDefault(), "%+.1f%% month over month", trendPercentage),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = if (trendPercentage >= 0) Color(0xFF388E3C) else Color(0xFFD32F2F)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // --- GRAFICO A LINEE ---
            Text(
                text = "Balance History Trend",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Passiamo le transazioni e la base iniziale al widget della linea
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
                    modifier = Modifier.weight(1f).fillMaxWidth(),
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

    // --- DIALOGS ---
    if (showTransactionDialog && accounts.isNotEmpty()) {
        var title by remember { mutableStateOf("") }
        var amountString by remember { mutableStateOf("") }
        var isExpense by remember { mutableStateOf(true) }
        var selectedAccount by remember { mutableStateOf(currentAccount ?: accounts.first()) }
        var expandedDropdown by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showTransactionDialog = false },
            title = { Text("New Transaction") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (accountId == null) {
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

        var editTitle by remember { mutableStateOf(tx.title) }
        var editAmountStr by remember { mutableStateOf(tx.amount.toString()) }
        var editIsExpense by remember { mutableStateOf(cat?.isExpense ?: true) }

        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        var editDateStr by remember { mutableStateOf(sdf.format(Date(tx.date))) }

        var editSelectedAccount by remember {
            mutableStateOf(accounts.find { it.id == tx.accountId } ?: accounts.first())
        }
        var expandedEditDropdown by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showEditTransactionDialog = false; transactionToEdit = null },
            title = { Text("Edit Transaction") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Selezione del conto (solo se ci sono conti disponibili)
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
                    OutlinedTextField(value = editTitle, onValueChange = { editTitle = it }, label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = editAmountStr, onValueChange = { editAmountStr = it }, label = { Text("Amount (€)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = editDateStr, onValueChange = { editDateStr = it }, label = { Text("Date (dd/MM/yyyy)") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                        Text(text = if (editIsExpense) "🔴 Expense" else "🟢 Income")
                        Switch(checked = editIsExpense, onCheckedChange = { editIsExpense = it })
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val amount = editAmountStr.replace(",", ".").toDoubleOrNull()
                    if (amount != null && editTitle.isNotBlank()) {
                        viewModel.updateExistingTransaction(
                            transaction = tx,
                            newTitle = editTitle,
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

    val graphData = balancePoints.takeLast(6)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp), // Leggermente più alto per far respirare la curva
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

                // Aggiungiamo un po' di "padding" matematico per non far toccare alla linea i bordi estremi
                val deltaY = if (maxVal == minVal) 1.0 else (maxVal - minVal) * 1.2
                val yOffset = if (maxVal == minVal) 0.0 else (maxVal - minVal) * 0.1

                val spaceX = size.width / (graphData.size - 1)

                val strokePath = Path()
                val fillPath = Path()
                val coordinates = mutableListOf<Offset>()

                // 1. Calcoliamo tutte le coordinate X e Y
                graphData.forEachIndexed { index, balance ->
                    val x = index * spaceX
                    val y = size.height - (((balance - minVal + yOffset) / deltaY) * size.height).toFloat()
                    coordinates.add(Offset(x, y))
                }

                // 2. Disegniamo le curve fluide (Bézier)
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

                    // 3. Chiudiamo il tracciato per la sfumatura (gradient) sotto la linea
                    val lastPoint = coordinates.last()
                    fillPath.lineTo(lastPoint.x, size.height)
                    fillPath.lineTo(firstPoint.x, size.height)
                    fillPath.close()

                    // Disegniamo la sfumatura
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

                    // Disegniamo la linea solida curva
                    drawPath(
                        path = strokePath,
                        color = lineColor,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // 4. Disegniamo i classici puntini sopra la curva
                    coordinates.forEach { centerOffset ->
                        // Cerchio esterno del colore della linea
                        drawCircle(
                            color = lineColor,
                            radius = 5.dp.toPx(),
                            center = centerOffset
                        )
                        // Cerchio interno per creare l'effetto "ciambellina" o punto forato
                        drawCircle(
                            color = Color.White, // o il colore di sfondo della tua card
                            radius = 2.5.dp.toPx(),
                            center = centerOffset
                        )
                    }
                }
            }
        }
    }
}