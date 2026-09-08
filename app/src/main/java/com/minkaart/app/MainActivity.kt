package com.minkaart2.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.minkaart2.app.navigation.AppNavigation
import com.minkaart2.app.ui.theme.MinkaArt_btaTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MinkaArt_btaTheme {
                AppNavigation()
            }
        }
    }
}
