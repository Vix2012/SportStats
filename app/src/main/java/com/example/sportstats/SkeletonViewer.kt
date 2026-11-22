package com.example.sportstats

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

// Paires de points reliés par un trait (numéros des points MediaPipe)
private val LIAISONS = listOf(
    11 to 12,            // épaules
    11 to 13, 13 to 15,  // bras gauche
    12 to 14, 14 to 16,  // bras droit
    11 to 23, 12 to 24,  // torse
    23 to 24,            // hanches
    23 to 25, 25 to 27,  // jambe gauche
    24 to 26, 26 to 28   // jambe droite
)

private val COULEURS = listOf(Color.Red, Color.Cyan, Color.Yellow, Color.Green)

@Composable
fun SkeletonViewer(
    videoUri: Uri,
    frames: List<FramePoses>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var index by remember { mutableIntStateOf(0) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    val retriever = remember(videoUri) {
        MediaMetadataRetriever().apply { setDataSource(context, videoUri) }
    }
    val verrou = remember { Mutex() }

    DisposableEffect(retriever) {
        onDispose { retriever.release() }
    }

    val frame = frames.getOrNull(index)

    LaunchedEffect(index) {
        val f = frames.getOrNull(index) ?: return@LaunchedEffect
        bitmap = withContext(Dispatchers.IO) {
            verrou.withLock {
                retriever.getFrameAtTime(
                    f.timestampMs * 1000,
                    MediaMetadataRetriever.OPTION_CLOSEST
                )
            }
        }
    }

    Column(modifier) {
        bitmap?.let { bmp ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(bmp.width.toFloat() / bmp.height)
            ) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
                Canvas(Modifier.fillMaxSize()) {
                    frame?.players?.forEachIndexed { numero, points ->
                        val couleur = COULEURS[numero % COULEURS.size]

                        fun position(i: Int): Offset? {
                            val p = points.getOrNull(i) ?: return null
                            return Offset(p.x() * size.width, p.y() * size.height)
                        }

                        for ((a, b) in LIAISONS) {
                            val debut = position(a)
                            val fin = position(b)
                            if (debut != null && fin != null) {
                                drawLine(couleur, debut, fin, strokeWidth = 5f)
                            }
                        }
                        for (i in points.indices) {
                            position(i)?.let { drawCircle(couleur, 6f, it) }
                        }
                    }
                }
            }
        }

        if (frames.size > 1) {
            Slider(
                value = index.toFloat(),
                onValueChange = { index = it.roundToInt() },
                valueRange = 0f..(frames.size - 1).toFloat()
            )
        }
        Text("Image ${index + 1} / ${frames.size} - ${frame?.players?.size ?: 0} joueur(s) détecté(s)")
    }
}