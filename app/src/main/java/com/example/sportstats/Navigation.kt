package com.example.sportstats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class Onglet(val emoji: String, val titre: String) {
    Accueil("🏠", "Accueil"),
    Direct("🎥", "Direct"),
    Stats("📊", "Stats"),
    Profil("👤", "Profil")
}

@Composable
fun SportStatsApp(modifier: Modifier = Modifier) {
    var onglet by remember { mutableStateOf(Onglet.Accueil) }
    val espaceBarre = Modifier.padding(bottom = 96.dp)

    Box(modifier.fillMaxSize()) {
        when (onglet) {
            Onglet.Accueil -> HomeScreen(espaceBarre)
            Onglet.Stats -> StatsScreen(espaceBarre)
            Onglet.Profil -> ProfilScreen(espaceBarre)
            Onglet.Direct -> DirectScreen(espaceBarre)
        }
        BarreNavigation(
            actuel = onglet,
            onChoix = { onglet = it },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
fun BarreNavigation(
    actuel: Onglet,
    onChoix: (Onglet) -> Unit,
    modifier: Modifier = Modifier
) {
    val couleur = MaterialTheme.colorScheme.primary
    Surface(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .widthIn(max = 520.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            Modifier.padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Onglet.entries.forEach { o ->
                val actif = o == actuel
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (actif) couleur.copy(alpha = 0.2f) else Color.Transparent
                        )
                        .clickable { onChoix(o) }
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(o.emoji, fontSize = 20.sp)
                    Text(
                        o.titre,
                        fontSize = 11.sp,
                        maxLines = 1,
                        color = if (actif) couleur else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}