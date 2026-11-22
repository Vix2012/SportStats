package com.example.sportstats

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun StatsScreen(
    modifier: Modifier = Modifier,
    viewModel: AnalysisViewModel = viewModel()
) {
    val matchs = viewModel.matches
    val nbEchanges = matchs.sumOf { it.echanges.size }
    val dureeJeuMs = matchs.sumOf { m -> m.echanges.sumOf { it.endMs - it.startMs } }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text("Stats totales", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        if (matchs.isEmpty()) {
            Text("Aucun match analysé pour l'instant")
        } else {
            Text("${matchs.size} match(s) analysé(s)")
            Text("$nbEchanges échange(s) au total")
            Text("Temps de jeu cumulé : ${formatDuree(dureeJeuMs)}")

            Spacer(Modifier.height(24.dp))
            Text("Historique", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            matchs.reversed().forEach { m ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(m.titre, style = MaterialTheme.typography.titleSmall)
                        Text(
                            "${m.echanges.size} échange(s), " +
                                    formatDuree(m.echanges.sumOf { it.endMs - it.startMs })
                        )
                        TextButton(onClick = { viewModel.supprimerMatch(m.id) }) {
                            Text("Supprimer")
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun ProfilScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Profil", style = MaterialTheme.typography.headlineMedium)
    }
}

private fun formatDuree(ms: Long): String {
    val s = ms / 1000
    return "%d min %02d s".format(s / 60, s % 60)
}