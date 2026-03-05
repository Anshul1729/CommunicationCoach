package com.communicationcoach.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.communicationcoach.ui.navigation.AppNavigation
import com.communicationcoach.ui.theme.CoachTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Let Compose draw edge-to-edge (status bar blends with TopAppBar)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            CoachTheme {
                AppNavigation()
            }
        }
    }
}
