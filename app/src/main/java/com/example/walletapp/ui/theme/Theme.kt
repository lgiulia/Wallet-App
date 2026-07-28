package com.example.walletapp.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// I COLORI STANDARD DI BASE (VIOLA)
private val PurpleDarkColorScheme = darkColorScheme(
    primary = Purple80, secondary = PurpleGrey80, tertiary = Pink80
)
private val PurpleLightColorScheme = lightColorScheme(
    primary = Purple40, secondary = PurpleGrey40, tertiary = Pink40
)

// NUOVO COLORE: BLU
private val BlueLightColorScheme = lightColorScheme(
    primary = Color(0xFF1976D2), secondary = Color(0xFF64B5F6), tertiary = Color(0xFF1565C0)
)
private val BlueDarkColorScheme = darkColorScheme(
    primary = Color(0xFF90CAF9), secondary = Color(0xFF1976D2), tertiary = Color(0xFF64B5F6)
)

// NUOVO COLORE: VERDE
private val GreenLightColorScheme = lightColorScheme(
    primary = Color(0xFF388E3C), secondary = Color(0xFF81C784), tertiary = Color(0xFF2E7D32)
)
private val GreenDarkColorScheme = darkColorScheme(
    primary = Color(0xFFA5D6A7), secondary = Color(0xFF388E3C), tertiary = Color(0xFF81C784)
)


@Composable
fun WalletAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    colorPalette: String = "Dynamic",
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    // Lo switch che decide quale tavolozza usare
    val colorScheme = when (colorPalette) {
        "Blue" -> if (darkTheme) BlueDarkColorScheme else BlueLightColorScheme
        "Green" -> if (darkTheme) GreenDarkColorScheme else GreenLightColorScheme
        "Purple" -> if (darkTheme) PurpleDarkColorScheme else PurpleLightColorScheme
        else -> {
            // "System Default" (Usa i colori dinamici del dispositivo Android 12+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                // Fallback se il telefono è troppo vecchio per i colori dinamici
                if (darkTheme) PurpleDarkColorScheme else PurpleLightColorScheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}