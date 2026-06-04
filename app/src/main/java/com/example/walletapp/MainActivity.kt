package com.example.walletapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.walletapp.data.AppDatabase
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
                DashboardScreen(viewModel = viewModel)
            }
        }
    }
}