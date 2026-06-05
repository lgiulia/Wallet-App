package com.example.walletapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.walletapp.ui.AccountDetailScreen
import com.example.walletapp.ui.AuthDialog
import com.example.walletapp.ui.DashboardScreen
import com.example.walletapp.ui.SettingsScreen
import com.example.walletapp.ui.theme.WalletAppTheme
import com.example.walletapp.viewmodel.FinanceViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            WalletAppTheme {
                val viewModel: FinanceViewModel = viewModel()
                var selectedAccountIdForDetail by remember { mutableStateOf<Long?>(null) }
                var showProfileDialog by remember { mutableStateOf(false) }
                var showSettingsScreen by remember { mutableStateOf(false) }

                // Se la variabile è true, disegna il popup in sovrimpressione
                if (showProfileDialog) {
                    AuthDialog(
                        onDismiss = { showProfileDialog = false } // Permette di chiuderlo premendo fuori o su Cancel
                    )
                }

                // Smistamento Pagine
                if (showSettingsScreen) {
                    SettingsScreen(
                        viewModel = viewModel,
                        onBack = { showSettingsScreen = false },
                        onOpenAccountManagement = { showProfileDialog = true } // Apre il popup AuthDialog da Settings
                    )
                } else if (selectedAccountIdForDetail == null) {
                    DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToDetail = { id ->
                            selectedAccountIdForDetail = id ?: -1L
                        },
                        onNavigateToProfile = { showProfileDialog = true },
                        onNavigateToSettings = { showSettingsScreen = true } // Apre Settings
                    )
                } else {
                    AccountDetailScreen(
                        viewModel = viewModel,
                        accountId = if (selectedAccountIdForDetail == -1L) null else selectedAccountIdForDetail,
                        onBack = { selectedAccountIdForDetail = null }
                    )
                }
            }
        }
    }
}