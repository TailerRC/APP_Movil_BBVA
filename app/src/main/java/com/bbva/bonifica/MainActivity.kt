package com.bbva.bonifica

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.bbva.bonifica.ui.navigation.AppNavHost
import com.bbva.bonifica.ui.theme.BonificaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BonificaTheme {
                AppNavHost()
            }
        }
    }
}
