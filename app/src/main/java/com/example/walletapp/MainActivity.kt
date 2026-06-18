package com.example.walletapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.walletapp.ui.AccountDetailScreen
import com.example.walletapp.ui.AuthDialog
import com.example.walletapp.ui.DashboardScreen
import com.example.walletapp.ui.SettingsScreen
import com.example.walletapp.ui.theme.WalletAppTheme
import com.example.walletapp.viewmodel.FinanceViewModel
import androidx.activity.compose.BackHandler

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel: FinanceViewModel = viewModel()

            // Legge il tema scelto in tempo reale
            val currentTheme by viewModel.appTheme.collectAsState()

            // Calcola se si deve forzare il Dark Mode, il Light Mode, o seguire il sistema
            val isDarkTheme = when (currentTheme) {
                "Dark" -> true
                "Light" -> false
                else -> isSystemInDarkTheme()
            }

            // Passa il valore calcolato al tema
            WalletAppTheme(darkTheme = isDarkTheme) {
                var selectedAccountIdForDetail by remember { mutableStateOf<Long?>(null) }
                var showProfileDialog by remember { mutableStateOf(false) }
                var showSettingsScreen by remember { mutableStateOf(false) }

                // Smistamento Pagine
                if (showSettingsScreen) {
                    // Tasto indietro
                    BackHandler {
                        showSettingsScreen = false
                    }
                    SettingsScreen(
                        viewModel = viewModel,
                        onBack = { showSettingsScreen = false },
                        onOpenAccountManagement = { showProfileDialog = true } // Apre il popup AuthDialog da Settings
                    )
                    // Il popup delle impostazioni mostra il bottone delete account
                    if (showProfileDialog) {
                        AuthDialog(
                            viewModel = viewModel,
                            showDeleteOption = true,
                            onDismiss = { showProfileDialog = false }
                        )
                    }
                } else if (selectedAccountIdForDetail == null) {
                    DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToDetail = { id ->
                            selectedAccountIdForDetail = id ?: -1L
                        },
                        onNavigateToProfile = { showProfileDialog = true },
                        onNavigateToSettings = { showSettingsScreen = true } // Apre Settings
                    )
                    // Popup della dashboard nasconde il bottone detele account
                    if (showProfileDialog) {
                        AuthDialog(
                            viewModel = viewModel,
                            showDeleteOption = false,
                            onDismiss = { showProfileDialog = false }
                        )
                    }
                } else {
                    // Tasto indietro
                    BackHandler {
                        selectedAccountIdForDetail = null
                    }
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