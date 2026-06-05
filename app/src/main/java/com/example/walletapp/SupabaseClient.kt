package com.example.walletapp

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseClient {
    val client = createSupabaseClient(
        supabaseUrl = "https://xxlrmoigglfrumhgjgek.supabase.co",
        supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Inh4bHJtb2lnZ2xmcnVtaGdqZ2VrIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODA1ODI4OTUsImV4cCI6MjA5NjE1ODg5NX0.j42SxuBoISAsrb30KwK4Wq5CGfw5grMqBzxecEwyzl0"
    ) {
        install(Postgrest) // Abilita il database
    }
}