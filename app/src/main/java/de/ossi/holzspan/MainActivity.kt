package de.ossi.holzspan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import de.ossi.holzspan.ui.HolzspanTheme
import de.ossi.holzspan.ui.RechnerScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("maschine", MODE_PRIVATE)
        setContent {
            HolzspanTheme {
                RechnerScreen(prefs)
            }
        }
    }
}
