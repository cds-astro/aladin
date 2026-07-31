package com.aladin.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.aladin.android.ui.SkyScreen
import com.aladin.android.ui.theme.AladinTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val vm: SkyViewModel by viewModels()
        setContent {
            AladinTheme {
                SkyScreen(vm)
            }
        }
    }
}