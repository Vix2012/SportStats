package com.example.sportstats

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.example.sportstats.ui.theme.SportStatsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SportStatsTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SportStatsApp(modifier = Modifier.padding(innerPadding))
                }

            }
            MaterialTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    LavaBackground()   // 1er = tout derrière

                    // ton app ici, après = devant
                    // (ta navigation, tes écrans, etc.)
                }

                }
            Scaffold(
                containerColor = Color.Transparent,
                // ...le reste inchangé
            )
            }

        }
    }

private fun MainActivity.Scaffold(containerColor: Color) {}

annotation class LavaBackground


@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    SportStatsTheme {
        Greeting("Android")
    }
}
