package com.example.walletapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.walletapp.ui.AccountDetailScreen
import com.example.walletapp.ui.DashboardScreen
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

                if (selectedAccountIdForDetail == null) {
                    DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToDetail = { id ->
                            selectedAccountIdForDetail = id ?: -1L
                        }
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