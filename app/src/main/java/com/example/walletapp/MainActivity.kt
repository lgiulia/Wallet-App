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

        // 1. Inizializza il Database
        val database = AppDatabase.getDatabase(this)

        // 2. Prepara la Factory per costruire il ViewModel
        val factory = FinanceViewModelFactory(database.financeDao())

        setContent {
            WalletAppTheme {
                // 3. Crea l'istanza del ViewModel
                val viewModel: FinanceViewModel = viewModel(factory = factory)

                // 4. Mostra la nostra schermata passandogli il ViewModel
                DashboardScreen(viewModel = viewModel)
            }
        }
    }
}