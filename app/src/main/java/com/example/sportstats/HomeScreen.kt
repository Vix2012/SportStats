package com.example.sportstats

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: AnalysisViewModel = viewModel()
) {
    val context = LocalContext.current
    var afficherInfo by remember { mutableStateOf(false) }

    val videoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) viewModel.analyze(uri)
    }

    val permissionNotif = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Qu'il accepte ou non, on continue : l'analyse marche dans les deux cas
        videoPicker.launch("video/*")
    }
    var calibrationUri by remember { mutableStateOf<Uri?>(null) }
    val choixCalibration = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) calibrationUri = uri
    }

    if (afficherInfo) {
        AlertDialog(
            onDismissRequest = { afficherInfo = false },
            title = { Text("Analyse en arrière-plan") },
            text = {
                Text(
                    "L'analyse peut être longue et se fait en arrière-plan. " +
                            "Pendant ce temps, le téléphone et l'app peuvent être plus lents. " +
                            "Tu recevras une notification quand elle sera terminée."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    afficherInfo = false
                    val permissionManquante = Build.VERSION.SDK_INT >= 33 &&
                            ContextCompat.checkSelfPermission(
                                context, Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                    if (permissionManquante) {
                        permissionNotif.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        videoPicker.launch("video/*")
                    }
                }) { Text("Continuer") }
            },
            dismissButton = {
                TextButton(onClick = { afficherInfo = false }) { Text("Annuler") }
            }
        )
    }
    val aCalibrer = calibrationUri
    if (aCalibrer != null) {
        CalibrationScreen(
            videoUri = aCalibrer,
            modifier = modifier,
            onFini = { coins ->
                if (coins != null) viewModel.enregistrerTerrain(coins)
                calibrationUri = null
            }
        )
        return
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Analyse de match", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(32.dp))

        Button(
            onClick = { afficherInfo = true },
            enabled = !viewModel.isAnalyzing
        ) {
            Text("Importer une vidéo")
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { choixCalibration.launch("video/*") }) {
                Text("Calibrer le terrain")
            }
            Text(if (viewModel.terrain != null) "Terrain calibré ✓" else "Terrain non calibré")
        }

        Spacer(Modifier.height(24.dp))

        if (viewModel.isAnalyzing) CircularProgressIndicator()
        Spacer(Modifier.height(8.dp))
        Text(viewModel.message)

        viewModel.exchanges.forEachIndexed { i, e ->
            Text("Échange ${i + 1} : ${temps(e.startMs)} → ${temps(e.endMs)}")
        }

        val uri = viewModel.videoUri
        if (uri != null && viewModel.frames.isNotEmpty() && !viewModel.isAnalyzing) {
            Spacer(Modifier.height(24.dp))
            SkeletonViewer(videoUri = uri, frames = viewModel.frames)
        }
    }
}

private fun temps(ms: Long): String {
    val s = ms / 1000
    return "%d:%02d".format(s / 60, s % 60)
}