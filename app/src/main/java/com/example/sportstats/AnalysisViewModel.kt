package com.example.sportstats

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.compose.ui.geometry.Offset

class AnalysisViewModel(app: Application) : AndroidViewModel(app) {

    init {
        AnalysisState.matches = MatchStore.charger(app)
        AnalysisState.terrain = CalibrationStore.charger(app)
    }

    val isAnalyzing: Boolean get() = AnalysisState.isAnalyzing
    val message: String get() = AnalysisState.message
    val videoUri: Uri? get() = AnalysisState.videoUri
    val frames: List<FramePoses> get() = AnalysisState.frames
    val exchanges: List<Exchange> get() = AnalysisState.exchanges
    val matches: List<MatchRecord> get() = AnalysisState.matches
    val terrain: List<Offset>? get() = AnalysisState.terrain
    fun analyze(uri: Uri) {
        if (AnalysisState.isAnalyzing) return

        val context = getApplication<Application>()
        val intent = Intent(context, AnalysisService::class.java).apply {
            data = uri
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            AnalysisState.isAnalyzing = true
            ContextCompat.startForegroundService(context, intent)
        } catch (e: Exception) {
            AnalysisState.isAnalyzing = false
            AnalysisState.message = "Impossible de lancer l'analyse : ${e.message}"
        }
    }

    fun supprimerMatch(id: Long) {
        val context = getApplication<Application>()
        MatchStore.supprimer(context, id)
        AnalysisState.matches = MatchStore.charger(context)
    }
    fun enregistrerTerrain(coins: List<Offset>) {
        CalibrationStore.sauver(getApplication<Application>(), coins)
        AnalysisState.terrain = coins
    }
}