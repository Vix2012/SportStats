package com.example.sportstats

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Range
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.video.AudioConfig
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recording
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import java.io.File

@Composable
fun DirectScreen(
    modifier: Modifier = Modifier,
    viewModel: AnalysisViewModel = viewModel()
) {
    val context = LocalContext.current
    val proprietaire = context as LifecycleOwner
    val ecran = LocalView.current

    var autorise by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }
    val demandePermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { accorde -> autorise = accorde }

    val controleur = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.VIDEO_CAPTURE)
            setVideoCaptureTargetFrameRate(Range(30, 30))
        }
    }
    val supportVue = remember { arrayOfNulls<PreviewView>(1) }

    var qualite by remember { mutableStateOf("FHD") }
    var enregistrement by remember { mutableStateOf<Recording?>(null) }
    var dernierFichier by remember { mutableStateOf<File?>(null) }
    var info by remember { mutableStateOf("") }
    var calibre by remember { mutableStateOf(false) }
    var imageCalibration by remember { mutableStateOf<Bitmap?>(null) }

    DisposableEffect(Unit) {
        ecran.keepScreenOn = true
        onDispose { ecran.keepScreenOn = false }
    }

    LaunchedEffect(autorise) {
        if (autorise) controleur.bindToLifecycle(proprietaire)
    }

    // Full HD minimum : on ne descend jamais en dessous
    LaunchedEffect(qualite) {
        val selecteur = if (qualite == "UHD") {
            QualitySelector.from(
                Quality.UHD,
                FallbackStrategy.lowerQualityOrHigherThan(Quality.FHD)
            )
        } else {
            QualitySelector.from(
                Quality.FHD,
                FallbackStrategy.higherQualityOrLowerThan(Quality.FHD)
            )
        }
        controleur.setVideoCaptureQualitySelector(selecteur)
    }

    // Écran de calibrage, à la place de la caméra
    val aCalibrer = imageCalibration
    if (aCalibrer != null) {
        CalibrationScreen(
            videoUri = null,
            bitmapInitial = aCalibrer,
            modifier = modifier,
            onFini = { coins ->
                if (coins != null) {
                    viewModel.enregistrerTerrain(coins)
                    calibre = true
                }
                imageCalibration = null
            }
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!autorise) {
            Spacer(Modifier.weight(1f))
            Text("Mode direct", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(16.dp))
            Button(onClick = { demandePermission.launch(Manifest.permission.CAMERA) }) {
                Text("Autoriser la caméra")
            }
            Spacer(Modifier.weight(1f))
        } else {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        this.controller = controleur
                        supportVue[0] = this
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = qualite == "FHD",
                    onClick = { qualite = "FHD" },
                    enabled = enregistrement == null
                )
                Text("Full HD")
                Spacer(Modifier.width(16.dp))
                RadioButton(
                    selected = qualite == "UHD",
                    onClick = { qualite = "UHD" },
                    enabled = enregistrement == null
                )
                Text("4K")
            }
            Text("30 images par seconde", style = MaterialTheme.typography.bodySmall)

            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                onClick = {
                    val capture = supportVue[0]?.bitmap
                    if (capture != null) {
                        imageCalibration = capture
                    } else {
                        info = "L'aperçu n'est pas prêt, réessaie dans un instant."
                    }
                },
                enabled = enregistrement == null
            ) {
                Text(if (calibre) "Recalibrer le terrain" else "Calibrer le terrain")
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    if (enregistrement == null) {
                        val fichier = File(
                            context.filesDir,
                            "match_${System.currentTimeMillis()}.mp4"
                        )
                        val options = FileOutputOptions.Builder(fichier).build()
                        info = "Enregistrement en cours..."
                        enregistrement = controleur.startRecording(
                            options,
                            AudioConfig.AUDIO_DISABLED,
                            ContextCompat.getMainExecutor(context)
                        ) { evenement ->
                            if (evenement is VideoRecordEvent.Finalize) {
                                enregistrement = null
                                if (evenement.hasError()) {
                                    info = "Erreur d'enregistrement (code ${evenement.error})"
                                } else {
                                    dernierFichier = fichier
                                    info = "Enregistré : " + decrire(fichier)
                                }
                            }
                        }
                    } else {
                        enregistrement?.stop()
                    }
                },
                enabled = enregistrement != null || calibre
            ) {
                Text(if (enregistrement == null) "Démarrer l'enregistrement" else "Arrêter")
            }

            if (!calibre) {
                Text(
                    "Calibre le terrain avant de pouvoir filmer",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (info.isNotEmpty()) Text(info)

            val fichier = dernierFichier
            if (fichier != null && enregistrement == null) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { viewModel.analyze(Uri.fromFile(fichier)) },
                    enabled = !viewModel.isAnalyzing
                ) {
                    Text("Analyser cet enregistrement")
                }
                Text(
                    "La progression s'affiche dans l'onglet Accueil",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

// Lit la taille, la résolution et le nombre d'images par seconde réels du fichier
private fun decrire(fichier: File): String {
    val lecteur = MediaMetadataRetriever()
    return try {
        lecteur.setDataSource(fichier.absolutePath)
        val largeur = lecteur.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
        val hauteur = lecteur.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
        val duree = lecteur
            .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
        val images = if (Build.VERSION.SDK_INT >= 28) {
            lecteur.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_FRAME_COUNT)
                ?.toLongOrNull()
        } else null

        buildString {
            append("${fichier.length() / 1_000_000} Mo")
            if (largeur != null && hauteur != null) append(", ${largeur}x${hauteur}")
            if (images != null && duree != null && duree > 0) {
                append(", %.0f images/s".format(images * 1000f / duree))
            }
        }
    } catch (e: Exception) {
        "${fichier.length() / 1_000_000} Mo"
    } finally {
        lecteur.release()
    }
}