package com.example.biblefob

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.biblefob.navigation.BibleFobApp
import com.example.biblefob.ui.theme.BibleFobTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BibleFobTheme {
                BibleFobApp()
            }
        }
    }
}
