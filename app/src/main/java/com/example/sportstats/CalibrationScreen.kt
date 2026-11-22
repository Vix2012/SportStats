package com.example.sportstats

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CalibrationScreen(
    videoUri: Uri,
    onFini: (List<Offset>?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var image by remember { mutableStateOf<Bitmap?>(null) }
    var taille by remember { mutableStateOf(IntSize.Zero) }
    var enCours by remember { mutableStateOf(false) }
    var info by remember { mutableStateOf("Touche le sol du terrain pour le détecter.") }
    val coins = remember { mutableStateListOf<Offset>() }
    var coinActif by remember { mutableIntStateOf(-1) }

    // On prend une image de la vidéo, vers la première seconde
    LaunchedEffect(videoUri) {
        image = withContext(Dispatchers.IO) {
            val lecteur = MediaMetadataRetriever()
            try {
                lecteur.setDataSource(context, videoUri)
                lecteur.getFrameAtTime(1_000_000, MediaMetadataRetriever.OPTION_CLOSEST)
                    ?: lecteur.getFrameAtTime(0)
            } finally {
                lecteur.release()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Calibrage du terrain", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(info)
        Spacer(Modifier.height(12.dp))

        val bmp = image
        if (bmp == null) {
            CircularProgressIndicator()
        } else {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(bmp.width.toFloat() / bmp.height)
                    .onSizeChanged { taille = it }
                    .pointerInput(Unit) {
                        detectTapGestures { position ->
                            if (coins.isEmpty() && !enCours && taille.width > 0) {
                                val xn = position.x / taille.width
                                val yn = position.y / taille.height
                                scope.launch {
                                    enCours = true
                                    info = "Détection en cours..."
                                    val trouve = withContext(Dispatchers.Default) {
                                        try {
                                            CourtDetector.detect(context, bmp, xn, yn)
                                        } catch (e: Exception) {
                                            null
                                        }
                                    }
                                    if (trouve != null) {
                                        coins.addAll(trouve)
                                        info = "Fais glisser les 4 coins pour les ajuster, puis valide."
                                    } else {
                                        info = "Détection impossible. Touche ailleurs, ou place les coins à la main."
                                    }
                                    enCours = false
                                }
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { debut ->
                                var meilleur = -1
                                var distanceMin = 90f
                                for (i in coins.indices) {
                                    val pos = Offset(
                                        coins[i].x * taille.width,
                                        coins[i].y * taille.height
                                    )
                                    val d = (pos - debut).getDistance()
                                    if (d < distanceMin) {
                                        distanceMin = d
                                        meilleur = i
                                    }
                                }
                                coinActif = meilleur
                            },
                            onDrag = { changement, deplacement ->
                                if (coinActif >= 0 && taille.width > 0) {
                                    changement.consume()
                                    val c = coins[coinActif]
                                    coins[coinActif] = Offset(
                                        (c.x + deplacement.x / taille.width).coerceIn(0f, 1f),
                                        (c.y + deplacement.y / taille.height).coerceIn(0f, 1f)
                                    )
                                }
                            },
                            onDragEnd = { coinActif = -1 },
                            onDragCancel = { coinActif = -1 }
                        )
                    }
            ) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
                Canvas(Modifier.fillMaxSize()) {
                    if (coins.size == 4) {
                        val points = coins.map { Offset(it.x * size.width, it.y * size.height) }
                        for (i in 0..3) {
                            drawLine(Color.Yellow, points[i], points[(i + 1) % 4], strokeWidth = 4f)
                        }
                        points.forEach {
                            drawCircle(Color.Yellow, 22f, it)
                            drawCircle(Color.Black, 8f, it)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {
                coins.clear()
                coins.addAll(
                    listOf(
                        Offset(0.30f, 0.35f), Offset(0.70f, 0.35f),
                        Offset(0.90f, 0.80f), Offset(0.10f, 0.80f)
                    )
                )
                info = "Fais glisser les 4 coins sur les coins du terrain."
            }) { Text("À la main") }

            OutlinedButton(onClick = {
                coins.clear()
                info = "Touche le sol du terrain pour le détecter."
            }) { Text("Recommencer") }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { onFini(null) }) { Text("Annuler") }
            Button(
                onClick = { onFini(coins.toList()) },
                enabled = coins.size == 4
            ) { Text("Valider") }
        }
    }
}