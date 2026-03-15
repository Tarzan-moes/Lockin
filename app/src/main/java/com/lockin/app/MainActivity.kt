package com.lockin.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import com.lockin.app.presentation.components.LockinApp
import com.lockin.app.ui.theme.LockinTheme

/**
 * MainActivity – The single Activity that hosts the entire Compose UI.
 *
 * Annotated with @AndroidEntryPoint so Hilt can inject dependencies
 * into ViewModels created within this Activity's scope.
 *
 * The Activity does almost nothing itself — it just sets up edge-to-edge
 * display and hands off to [LockinTheme] → [LockinApp] which manages
 * all navigation and UI rendering via Compose.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LockinTheme {
                LockinApp()
            }
        }
    }
}

