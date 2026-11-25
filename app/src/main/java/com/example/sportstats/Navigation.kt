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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

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
    val onglets = Onglet.entries
    val nombre = onglets.size
    val scope = rememberCoroutineScope()
    val densite = LocalDensity.current

    // Le rebond : plus l'amortissement est bas, plus ça rebondit
    val rebond = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    // Position de la pastille, en "numéro d'onglet" (0 = premier, 1 = deuxième...)
    val curseur = remember { Animatable(actuel.ordinal.toFloat()) }
    var taille by remember { mutableStateOf(IntSize.Zero) }

    // Quand l'onglet change (appui ou glissement), la pastille y va en rebondissant
    LaunchedEffect(actuel) {
        curseur.animateTo(actuel.ordinal.toFloat(), rebond)
    }

    fun terminerGeste() {
        val cible = curseur.value.roundToInt().coerceIn(0, nombre - 1)
        onChoix(onglets[cible])
        scope.launch { curseur.animateTo(cible.toFloat(), rebond) }
    }

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
        Box(
            Modifier
                .padding(6.dp)
                .onSizeChanged { taille = it }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { changement, deplacement ->
                            changement.consume()
                            if (taille.width > 0) {
                                val largeurOnglet = taille.width / nombre.toFloat()
                                scope.launch {
                                    curseur.snapTo(
                                        (curseur.value + deplacement / largeurOnglet)
                                            .coerceIn(0f, (nombre - 1).toFloat())
                                    )
                                }
                            }
                        },
                        onDragEnd = { terminerGeste() },
                        onDragCancel = { terminerGeste() }
                    )
                }
        ) {
            val largeurOnglet = taille.width / nombre.toFloat()

            // La pastille qui glisse
            if (taille.width > 0) {
                Box(
                    Modifier
                        .offset { IntOffset((curseur.value * largeurOnglet).roundToInt(), 0) }
                        .size(
                            with(densite) { largeurOnglet.toDp() },
                            with(densite) { taille.height.toDp() }
                        )
                        .background(couleur.copy(alpha = 0.2f), RoundedCornerShape(50))
                )
            }

            // Les onglets, par-dessus
            Row(Modifier.fillMaxWidth()) {
                val proche = curseur.value.roundToInt()
                onglets.forEach { o ->
                    val actif = o.ordinal == proche
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50))
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
}