package com.example.sportstats

import android.content.Context
import android.os.Build
import android.os.PowerManager
import kotlinx.coroutines.delay

class ThermalGuard(context: Context) {

    private val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager

    // Le téléphone est jugé trop chaud à partir du niveau MODERATE
    fun tropChaud(): Boolean =
        Build.VERSION.SDK_INT >= 29 &&
                pm.currentThermalStatus >= PowerManager.THERMAL_STATUS_MODERATE

    suspend fun attendreRefroidissement(quandOnAttend: () -> Unit) {
        while (tropChaud()) {
            quandOnAttend()
            delay(10_000)
        }
    }
}