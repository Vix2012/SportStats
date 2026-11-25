package com.example.sportstats

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private data class Blob(
    val color: Color,
    val radiusFactor: Float,
    val speedX: Float,
    val speedY: Float,
    val phaseX: Float,
    val phaseY: Float
)

@Composable
fun Modifier.lavaBackground(
    baseColor: Color = Color(0xFF0A2E22) // vert foncé Roland Garros
): Modifier {
    val blobs = remember {
        listOf(
            Blob(Color(0xFFD9622B), 0.60f, 1f, 2f, 0.0f, 0.5f), // terre battue
            Blob(Color(0xFFE8814A), 0.50f, 2f, 1f, 1.5f, 3.0f), // terre battue claire
            Blob(Color(0xFF1F7A50), 0.60f, 1f, 1f, 3.0f, 1.0f), // vert court
            Blob(Color(0xFFB8471F), 0.45f, 2f, 3f, 4.5f, 2.0f), // brique foncée
            Blob(Color(0xFFF3E9D8), 0.35f, 3f, 2f, 2.0f, 5.0f)  // crème
        )
    }

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

    // drawBehind = dessine DERRIÈRE le contenu de l'élément
    return this.drawBehind {
        drawRect(baseColor)
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
}