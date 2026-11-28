package com.example.sportstats

import android.content.Context
import android.os.Build
import android.os.PowerManager
import kotlinx.coroutines.delay

class ThermalGuard(private val context: Context) {

    private val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager

    fun tropChaud(): Boolean {
        if (Build.VERSION.SDK_INT < 29) return false
        // Branché, on tolère plus de chaleur avant de mettre en pause
        val seuil = if (PowerGuard.enCharge(context)) {
            PowerManager.THERMAL_STATUS_SEVERE
        } else {
            PowerManager.THERMAL_STATUS_MODERATE
        }
        return pm.currentThermalStatus >= seuil
    }

    suspend fun attendreRefroidissement(quandOnAttend: () -> Unit) {
        while (tropChaud()) {
            quandOnAttend()
            delay(10_000)
        }
    }
}