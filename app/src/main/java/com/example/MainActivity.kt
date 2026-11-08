package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.navigation.ConsistAppNavigation
import com.example.ui.theme.ConsistTheme
import com.example.ui.viewmodel.ConsistViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: ConsistViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ConsistTheme {
                ConsistAppNavigation(viewModel = viewModel)
            }
        }
    }
}
