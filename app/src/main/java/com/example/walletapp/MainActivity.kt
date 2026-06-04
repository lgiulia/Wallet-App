package com.example.walletapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.walletapp.data.AppDatabase
import com.example.walletapp.ui.AccountDetailScreen
import com.example.walletapp.ui.DashboardScreen
import com.example.walletapp.ui.theme.WalletAppTheme
import com.example.walletapp.viewmodel.FinanceViewModel
import com.example.walletapp.viewmodel.FinanceViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(this)
        val factory = FinanceViewModelFactory(database.financeDao())

        setContent {
            WalletAppTheme {
                val viewModel: FinanceViewModel = viewModel(factory = factory)

                // Stato di navigazione: null = Dashboard, -1 = Total Detail, ID positivo = Dettaglio Conto
                var selectedAccountIdForDetail by remember { mutableStateOf<Long?>(null) }

                if (selectedAccountIdForDetail == null) {
                    DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToDetail = { id ->
                            // Se l'id è null (clic su Total Amount), usiamo -1L come convenzione per il totale globale
                            selectedAccountIdForDetail = id ?: -1L
                        }
                    )
                } else {
                    AccountDetailScreen(
                        viewModel = viewModel,
                        // Se è -1L passiamo null (indica il totale globale), altrimenti passiamo l'id del conto reale
                        accountId = if (selectedAccountIdForDetail == -1L) null else selectedAccountIdForDetail,
                        onBack = { selectedAccountIdForDetail = null }
                    )
                }
            }
        }
    }
}