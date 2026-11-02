package com.arjunvision.arjunfitai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.arjunvision.arjunfitai.ui.dashboard.DashboardScreen
import com.arjunvision.arjunfitai.ui.theme.ArjunFitTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ArjunFitTheme {
                DashboardScreen()
            }
        }
    }
}
