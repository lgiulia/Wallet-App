package com.example.walletapp.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.walletapp.data.Category
import com.example.walletapp.viewmodel.FinanceViewModel
import androidx.compose.material.icons.filled.Delete

@Composable
fun SettingsScreen(
    viewModel: FinanceViewModel,
    onBack: () -> Unit,
    onOpenAccountManagement: () -> Unit
) {
    var showPreferencesDialog by remember { mutableStateOf(false) }
    var showCategoriesDialog by remember { mutableStateOf(false) }

    var appearanceDropdownExpanded by remember { mutableStateOf(false) }
    val selectedTheme by viewModel.appTheme.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp, top = 16.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Text("Settings", fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
            }

            // Appearance
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Appearance", fontSize = 18.sp)
                Box {
                    TextButton(onClick = { appearanceDropdownExpanded = true }) {
                        Text(selectedTheme)
                    }
                    DropdownMenu(
                        expanded = appearanceDropdownExpanded,
                        onDismissRequest = { appearanceDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(text = { Text("Light") }, onClick = { viewModel.setAppTheme("Light"); appearanceDropdownExpanded = false })
                        DropdownMenuItem(text = { Text("Dark") }, onClick = { viewModel.setAppTheme("Dark"); appearanceDropdownExpanded = false })
                        DropdownMenuItem(text = { Text("System Default") }, onClick = { viewModel.setAppTheme("System Default"); appearanceDropdownExpanded = false })
                    }
                }
            }
            HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray)

            // Preferences
            SettingsMenuItem(title = "Preferences", onClick = { showPreferencesDialog = true })

            // Manage Categories
            SettingsMenuItem(title = "Manage Categories", onClick = { showCategoriesDialog = true })

            // Account Management
            SettingsMenuItem(title = "Account Management", onClick = onOpenAccountManagement)

            // Data & Backup
            // SettingsMenuItem(title = "Data & Backup", onClick = { /* TODO: Implement Export CSV */ })

            // Security
            // SettingsMenuItem(title = "Security", onClick = { /* TODO: Implement App Lock */ })
        }
    }

    // --- POPUPS ---
    if (showPreferencesDialog) {
        PreferencesDialog(viewModel = viewModel, onDismiss = { showPreferencesDialog = false })
    }

    if (showCategoriesDialog) {
        ManageCategoriesDialog(viewModel = viewModel, onDismiss = { showCategoriesDialog = false })
    }
}

@Composable
fun SettingsMenuItem(title: String, onClick: () -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 20.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, fontSize = 18.sp)
            Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = Color.Gray)
        }
        HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferencesDialog(viewModel: FinanceViewModel, onDismiss: () -> Unit) {
    // Legge i valori dal ViewModel in tempo reale
    val currency by viewModel.appCurrency.collectAsState()
    var currencyExpanded by remember { mutableStateOf(false) }

    val dateFormat by viewModel.appDateFormat.collectAsState()
    var dateExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Preferences", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                // Currency Dropdown
                ExposedDropdownMenuBox(expanded = currencyExpanded, onExpandedChange = { currencyExpanded = !currencyExpanded }) {
                    OutlinedTextField(
                        value = currency, onValueChange = {}, readOnly = true, label = { Text("Currency") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = currencyExpanded, onDismissRequest = { currencyExpanded = false }) {
                        listOf("€ (Euro)", "$ (USD)", "£ (GBP)").forEach {
                            // Salva il valore nel ViewModel al click
                            DropdownMenuItem(text = { Text(it) }, onClick = { viewModel.setAppCurrency(it); currencyExpanded = false })
                        }
                    }
                }

                // Date Format Dropdown
                ExposedDropdownMenuBox(expanded = dateExpanded, onExpandedChange = { dateExpanded = !dateExpanded }) {
                    OutlinedTextField(
                        value = dateFormat, onValueChange = {}, readOnly = true, label = { Text("Date Format") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dateExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = dateExpanded, onDismissRequest = { dateExpanded = false }) {
                        listOf("DD/MM/YYYY", "MM/DD/YYYY").forEach {
                            DropdownMenuItem(text = { Text(it) }, onClick = { viewModel.setAppDateFormat(it); dateExpanded = false })
                        }
                    }
                }

                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Save & Close") }
            }
        }
    }
}

@Composable
fun ManageCategoriesDialog(viewModel: FinanceViewModel, onDismiss: () -> Unit) {
    val categories by viewModel.allCategories.collectAsState()
    var categoryToEdit by remember { mutableStateOf<Category?>(null) }

    var editName by remember { mutableStateOf("") }
    var editIsExpense by remember { mutableStateOf(true) }

    var categoryToDelete by remember { mutableStateOf<Category?>(null) }
    var showDeleteCategoryDialog by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().fillMaxHeight(0.8f)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Manage Categories", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))

                // Lista delle categorie esistenti
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(categories.filter { it.name != "Balance Adjustment" }, key = { cat -> cat.id }) { cat ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(if (cat.isExpense) "🔴 " else "🟢 ", fontSize = 12.sp)
                                Text(cat.name, fontSize = 16.sp)
                            }
                            // ICONS PENCIL AND TRASH
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = {
                                    categoryToEdit = cat
                                    editName = cat.name
                                    editIsExpense = cat.isExpense
                                }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Gray)
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                IconButton(onClick = {
                                    categoryToDelete = cat
                                    showDeleteCategoryDialog = true
                                }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                        HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Sezione Aggiungi/Modifica
                Text(if (categoryToEdit == null) "Add New Category" else "Edit Category", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Name") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (editIsExpense) "🔴" else "🟢", fontSize = 12.sp)
                        Switch(checked = editIsExpense, onCheckedChange = { editIsExpense = it })
                    }
                }

                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
                    if (categoryToEdit != null) {
                        TextButton(onClick = { categoryToEdit = null; editName = ""; editIsExpense = true }) {
                            Text("Cancel Edit", color = Color.Gray)
                        }
                    }
                    Button(onClick = {
                        if (editName.isNotBlank()) {
                            if (categoryToEdit == null) {
                                viewModel.addCategory(editName, editIsExpense)
                            } else {
                                viewModel.updateCategory(categoryToEdit!!, editName, editIsExpense)
                                categoryToEdit = null
                            }
                            editName = ""
                            editIsExpense = true
                        }
                    }) {
                        Text(if (categoryToEdit == null) "Add" else "Update")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Close") }
            }
        }

        // --- POPUP: CONFIRM CATEGORY DELETE ---
        if (showDeleteCategoryDialog && categoryToDelete != null) {
            AlertDialog(
                onDismissRequest = { showDeleteCategoryDialog = false; categoryToDelete = null },
                title = { Text("Delete Category") },
                text = { Text("Are you sure you want to delete \"${categoryToDelete?.name}\"?") },
                confirmButton = {
                    Button(
                        onClick = {
                            categoryToDelete?.let { viewModel.deleteCategory(it) }
                            showDeleteCategoryDialog = false
                            categoryToDelete = null

                            if (categoryToEdit?.id == categoryToDelete?.id) {
                                categoryToEdit = null
                                editName = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteCategoryDialog = false; categoryToDelete = null }) { Text("Cancel") }
                }
            )
        }
    }
}