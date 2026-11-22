package com.example.sportstats

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Recording
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.camera.view.video.AudioConfig.AUDIO_DISABLED
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    val vue = LocalView.current

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
        }
    }
    var enregistrement by remember { mutableStateOf<Recording?>(null) }
    var dernierFichier by remember { mutableStateOf<File?>(null) }
    var info by remember { mutableStateOf("") }

    // L'écran reste allumé tant qu'on est sur cet onglet
    DisposableEffect(Unit) {
        vue.keepScreenOn = true
        onDispose { vue.keepScreenOn = false }
    }

    LaunchedEffect(autorise) {
        if (autorise) controleur.bindToLifecycle(proprietaire)
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
                    PreviewView(ctx).apply { this.controller = controleur }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            Spacer(Modifier.height(12.dp))

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
                            AUDIO_DISABLED,
                            ContextCompat.getMainExecutor(context)
                        ) { evenement ->
                            if (evenement is VideoRecordEvent.Finalize) {
                                enregistrement = null
                                if (evenement.hasError()) {
                                    info = "Erreur d'enregistrement (code ${evenement.error})"
                                } else {
                                    dernierFichier = fichier
                                    info = "Enregistré : ${fichier.length() / 1_000_000} Mo"
                                }
                            }
                        }
                    } else {
                        enregistrement?.stop()
                    }
                },
                enabled = !viewModel.isAnalyzing
            ) {
                Text(if (enregistrement == null) "Démarrer l'enregistrement" else "Arrêter")
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