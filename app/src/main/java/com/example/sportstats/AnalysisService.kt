package com.example.sportstats

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AnalysisService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    companion object {
        private const val CANAL_PROGRES = "analyse_progres"
        private const val CANAL_FIN = "analyse_fin"
        private const val ID_PROGRES = 1
        private const val ID_FIN = 2
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val uri = intent?.data
        if (uri == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        creerCanaux()
        val notif = notifProgres("Préparation de l'analyse...", 0, 0)
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(ID_PROGRES, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(ID_PROGRES, notif)
        }

        scope.launch {
            lancerAnalyse(uri)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        AnalysisState.isAnalyzing = false
        super.onDestroy()
    }

    private suspend fun lancerAnalyse(uri: Uri) {
        val etat = AnalysisState
        etat.isAnalyzing = true
        etat.videoUri = null
        etat.frames = emptyList()
        etat.exchanges = emptyList()
        try {
            etat.message = "Recherche des échanges..."
            majProgres("Recherche des échanges...", 0, 0)

            val trouves = ExchangeDetector(this).detect(uri)
            etat.exchanges = trouves

            if (trouves.isEmpty()) {
                etat.message = "Aucun échange détecté"
                notifierFin("Analyse terminée : aucun échange détecté")
                return
            }

            val garde = ThermalGuard(this)
            val analyzer = PoseAnalyzer(this)
            try {
                val tout = mutableListOf<FramePoses>()
                trouves.forEachIndexed { i, echange ->
                    garde.attendreRefroidissement {
                        etat.message = "Téléphone trop chaud, pause..."
                        majProgres("Téléphone trop chaud, pause...", trouves.size, i)
                    }
                    val branche = PowerGuard.enCharge(this)
                    val texte = "Analyse de l'échange ${i + 1}/${trouves.size}" +
                            if (branche) " (branché, rapide)" else " (sur batterie, économe)"
                    etat.message = "$texte..."
                    majProgres(texte, trouves.size, i)
                    val pas = if (branche) 50L else 100L
                    tout.addAll(analyzer.analyze(uri, echange.startMs, echange.endMs, pas))
                }

                etat.videoUri = uri
                etat.frames = tout
                val avecJoueurs = tout.count { it.players.isNotEmpty() }
                val resume = "${trouves.size} échange(s) analysé(s)"
                etat.message = "$resume, joueurs détectés dans $avecJoueurs images sur ${tout.size}"
                val match = MatchRecord(
                    id = System.currentTimeMillis(),
                    dateMs = System.currentTimeMillis(),
                    titre = "Match du " + SimpleDateFormat("d MMM yyyy, HH:mm", Locale.FRANCE).format(Date()),
                    echanges = trouves,
                    images = tout.size,
                    imagesAvecJoueurs = avecJoueurs
                )
                MatchStore.ajouter(this, match)
                etat.matches = MatchStore.charger(this)
                notifierFin("$resume. Touche pour voir le résultat.")
            } finally {
                analyzer.close()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            etat.message = "Erreur : ${e.message}"
            notifierFin("L'analyse a échoué")
        } finally {
            etat.isAnalyzing = false
        }
    }

    private fun creerCanaux() {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CANAL_PROGRES, "Analyse en cours", NotificationManager.IMPORTANCE_LOW)
        )
        nm.createNotificationChannel(
            NotificationChannel(CANAL_FIN, "Analyse terminée", NotificationManager.IMPORTANCE_DEFAULT)
        )
    }

    private fun ouvrirApp(): PendingIntent {
        val intent = packageManager.getLaunchIntentForPackage(packageName) ?: Intent()
        return PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }

    private fun notifProgres(texte: String, max: Int, actuel: Int): Notification =
        NotificationCompat.Builder(this, CANAL_PROGRES)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle("SportStats")
            .setContentText(texte)
            .setProgress(max, actuel, max == 0)
            .setOngoing(true)
            .setContentIntent(ouvrirApp())
            .build()

    private fun majProgres(texte: String, max: Int, actuel: Int) {
        getSystemService(NotificationManager::class.java)
            .notify(ID_PROGRES, notifProgres(texte, max, actuel))
    }

    private fun notifierFin(texte: String) {
        val notif = NotificationCompat.Builder(this, CANAL_FIN)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle("SportStats")
            .setContentText(texte)
            .setContentIntent(ouvrirApp())
            .setAutoCancel(true)
            .build()
        getSystemService(NotificationManager::class.java).notify(ID_FIN, notif)
    }
}