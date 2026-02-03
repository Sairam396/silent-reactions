package com.example.silentreactions

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.silentreactions.ui.theme.SilentReactionsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SilentReactionsTheme {
                Surface {
                    val vm: HandViewModel = viewModel()
                    MainScreen(viewModel = vm)
                }
            }
        }
    }
}
