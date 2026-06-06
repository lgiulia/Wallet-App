package com.example.walletapp.ui

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.walletapp.SupabaseClient
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.launch
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState

@Composable
fun AuthDialog(viewModel: com.example.walletapp.viewmodel.FinanceViewModel,
               showDeleteOption: Boolean = false,
               onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Ascolta in tempo reale se sei loggato o no
    val sessionStatus by SupabaseClient.client.auth.sessionStatus.collectAsState()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Se sei già loggato mostra il profilo
                if (sessionStatus is SessionStatus.Authenticated) {
                    val currentUser = SupabaseClient.client.auth.currentUserOrNull()

                    Text(
                        text = "My Profile",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    Text(text = "Logged in as:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = currentUser?.email ?: "Unknown User",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 32.dp)
                    )

                    Button(
                        onClick = {
                            scope.launch {
                                try {
                                    // Tenta il logout sul server
                                    SupabaseClient.client.auth.signOut()
                                } catch (e: Exception) {
                                    // Se il server dà errore (es. account già eliminato), lo ignoriamo!
                                } finally {
                                    // IN OGNI CASO:
                                    // 1. Pialla la memoria fisica del telefono forzatamente
                                    try { SupabaseClient.client.auth.clearSession() } catch (e: Exception) {}

                                    // 2. Azzera i dati della Dashboard dietro al popup
                                    viewModel.clearAllData()

                                    // 3. Mostra il messaggio e chiudi la finestra
                                    Toast.makeText(context, "Signed out successfully", Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Sign Out")
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    if (showDeleteOption) {
                        // Bottone elimina account
                        var showDeleteConfirm by remember { mutableStateOf(false) }

                        OutlinedButton(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Delete Account")
                        }

                        if (showDeleteConfirm) {
                            AlertDialog(
                                onDismissRequest = { showDeleteConfirm = false },
                                title = { Text("Are you sure?") },
                                text = { Text("This will permanently delete your account.") },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                try {
                                                    // 1. Lancia la cancellazione a catena sul server
                                                    SupabaseClient.client.postgrest.rpc("delete_my_account")
                                                } catch (e: Exception) {
                                                    // Ignoriamo l'errore se l'account era già sparito
                                                } finally {
                                                    // IN OGNI CASO:
                                                    // 2. Distruggi la sessione locale senza chiedere permesso
                                                    try {
                                                        SupabaseClient.client.auth.clearSession()
                                                    } catch (e: Exception) {
                                                    }

                                                    // 3. Azzera la Dashboard
                                                    viewModel.clearAllData()

                                                    // 4. Chiudi le finestre
                                                    Toast.makeText(
                                                        context,
                                                        "Account deleted",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                    showDeleteConfirm = false
                                                    onDismiss()
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                    ) { Text("Delete") }
                                },
                                dismissButton = {
                                    TextButton(onClick = {
                                        showDeleteConfirm = false
                                    }) { Text("Cancel") }
                                }
                            )
                        }
                    }

                    TextButton(onClick = onDismiss) {
                        Text("Close")
                    }

                } else {
                    // Se non sei loggato mostra sign in/ sign up
                    var email by remember { mutableStateOf("") }
                    var password by remember { mutableStateOf("") }
                    var isSignUp by remember { mutableStateOf(false) }
                    var isLoading by remember { mutableStateOf(false) }

                    Text(
                        text = if (isSignUp) "Create Account" else "Welcome Back",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            if (email.isBlank() || password.isBlank()) return@Button
                            isLoading = true
                            scope.launch {
                                try {
                                    if (isSignUp) {
                                        SupabaseClient.client.auth.signUpWith(Email) {
                                            this.email = email
                                            this.password = password
                                        }
                                        Toast.makeText(context, "Account created! You can now sign in.", Toast.LENGTH_LONG).show()
                                        isSignUp = false
                                    } else {
                                        SupabaseClient.client.auth.signInWith(Email) {
                                            this.email = email
                                            this.password = password
                                        }
                                        Toast.makeText(context, "Signed in successfully!", Toast.LENGTH_SHORT).show()
                                        onDismiss() // Chiude il popup appena si entra
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    ) {
                        Text(if (isSignUp) "Sign Up" else "Sign In")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    TextButton(onClick = { isSignUp = !isSignUp }) {
                        Text(if (isSignUp) "Already have an account? Sign In" else "Don't have an account? Sign Up")
                    }

                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}