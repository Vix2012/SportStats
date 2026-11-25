package com.example.sportstats

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// Une "bulle" de lave : sa couleur, sa taille et sa façon de bouger
private data class Blob(
    val color: Color,
    val radiusFactor: Float, // taille par rapport à l'écran
    val speedX: Float,       // vitesse horizontale (nombre entier pour une boucle parfaite)
    val speedY: Float,       // vitesse verticale
    val phaseX: Float,       // point de départ différent pour chaque bulle
    val phaseY: Float
)

@Composable
fun LavaBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {}
) {
    val blobs = remember {
        listOf(
            Blob(Color(0xFF4CAF50), 0.55f, 1f, 2f, 0.0f, 0.5f),
            Blob(Color(0xFFFFEB3B), 0.50f, 2f, 1f, 1.5f, 3.0f),
            Blob(Color(0xFFFF9800), 0.60f, 1f, 1f, 3.0f, 1.0f),
            Blob(Color(0xFFFF5722), 0.45f, 2f, 3f, 4.5f, 2.0f),
            Blob(Color(0xFFFFFFFF), 0.50f, 3f, 2f, 2.0f, 5.0f)
        )
    }

    // "time" va de 0 à 2π en boucle, 1 tour = 40 secondes (change 40000 pour accélérer/ralentir)
    val transition = rememberInfiniteTransition(label = "lava")
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 40000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )

    Box(modifier = modifier.fillMaxSize().background(Color(0xFF12002B))) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            blobs.forEach { b ->
                val cx = size.width * (0.5f + 0.38f * sin(time * b.speedX + b.phaseX))
                val cy = size.height * (0.5f + 0.42f * cos(time * b.speedY + b.phaseY))
                val r = size.minDimension * b.radiusFactor
                val center = Offset(cx, cy)

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(b.color.copy(alpha = 0.85f), b.color.copy(alpha = 0f)),
                        center = center,
                        radius = r
                    ),
                    radius = r,
                    center = center
                )
            }
        }
        content() // ton écran s'affiche par-dessus le fond
    }
}