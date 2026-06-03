package com.example.walletapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.walletapp.viewmodel.FinanceViewModel

@Composable
fun DashboardScreen(viewModel: FinanceViewModel) {
    // 1. "Ascoltiamo" i dati dal ViewModel
    val transactions by viewModel.allTransactions.collectAsState()
    val categories by viewModel.allCategories.collectAsState()

    // 2. Calcoliamo il saldo totale (Entrate - Uscite)
    val totalBalance = transactions.sumOf { transaction ->
        // Troviamo la categoria associata a questa transazione
        val category = categories.find { it.id == transaction.categoryId }

        // Se è un'uscita sottraiamo, altrimenti sommiamo
        if (category?.isExpense == true) -transaction.amount else transaction.amount
    }

    // 3. Disegniamo l'interfaccia (UI)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center // Mette tutto al centro dello schermo
    ) {
        Text(
            text = "Saldo Totale",
            fontSize = 20.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(8.dp)) // Spazio vuoto

        // Mostriamo il saldo formattato con 2 decimali
        Text(
            text = String.format("€ %.2f", totalBalance),
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold
        )
    }
}