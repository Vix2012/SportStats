package com.example.sportstats

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset

object AnalysisState {
    var isAnalyzing by mutableStateOf(false)
    var message by mutableStateOf("Aucune vidéo importée")
    var videoUri by mutableStateOf<Uri?>(null)
    var frames by mutableStateOf<List<FramePoses>>(emptyList())
    var exchanges by mutableStateOf<List<Exchange>>(emptyList())
    var matches by mutableStateOf<List<MatchRecord>>(emptyList())
    var terrain by mutableStateOf<List<Offset>?>(null)
}